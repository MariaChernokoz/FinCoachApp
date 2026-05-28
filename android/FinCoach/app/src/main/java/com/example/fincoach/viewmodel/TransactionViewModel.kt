package com.example.fincoach.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fincoach.data.model.Budget
import com.example.fincoach.data.model.Category
import com.example.fincoach.data.model.Transaction
import com.example.fincoach.data.repository.BudgetRepository
import com.example.fincoach.data.repository.CategoryRepository
import com.example.fincoach.data.repository.TransactionRepository
import com.example.fincoach.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder { NEWEST, OLDEST, EXPENSIVE, CHEAPEST }

class TransactionViewModel : ViewModel() {

    private val transactionRepo = TransactionRepository()
    private val categoryRepo    = CategoryRepository()
    private val budgetRepo      = BudgetRepository()
    private val userRepo        = UserRepository()

    private val _isLoading      = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error          = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()

    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate = _endDate.asStateFlow()

    private val allTransactions: StateFlow<List<Transaction>> = transactionRepo
        .observeTransactions()
        .catch { e -> _error.value = "Ошибка загрузки: ${e.localizedMessage}"; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepo
        .observeCategories()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = budgetRepo
        .observeBudgets()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Поиск и Тип фильтра
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // 0 - Все, 1 - Доходы, 2 - Расходы
    private val _typeFilter = MutableStateFlow(0)
    val typeFilter = _typeFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    val sortOrder = _sortOrder.asStateFlow()

    // Фильтр по категориям (пустое множество = все категории)
    private val _categoryFilter = MutableStateFlow<Set<String>>(emptySet())
    val categoryFilter = _categoryFilter.asStateFlow()

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions, _startDate, _endDate, _searchQuery, _typeFilter
    ) { transactions, start, end, query, type ->
        transactions.filter { tx ->
            // 1. Фильтр по дате и типу
            val dateMatch = if (start == null || end == null) true else tx.timestamp in start..(end + 86399999L)
            val typeMatch = when (type) { 1 -> tx.isIncome; 2 -> !tx.isIncome; else -> true }

            // 2. Fuzzy Search (Левенштейн)
            val searchMatch = if (query.isBlank()) true else {
                val wordsInTx = (tx.title + " " + tx.categoryTitle).split(" ")
                wordsInTx.any { word ->
                    if (word.contains(query, ignoreCase = true)) return@any true
                    val distance = levenshteinDistance(query.lowercase(), word.lowercase())
                    val threshold = if (query.length > 4) 2 else 1
                    distance <= threshold
                }
            }

            dateMatch && typeMatch && searchMatch
        }
    }.combine(_categoryFilter) { list, catFilter ->
        if (catFilter.isEmpty()) list else list.filter { it.categoryId in catFilter }
    }.combine(_sortOrder) { list, order ->
        when (order) {
            SortOrder.NEWEST    -> list.sortedByDescending { it.timestamp }
            SortOrder.OLDEST    -> list.sortedBy { it.timestamp }
            SortOrder.EXPENSIVE -> list.sortedByDescending { it.amount }
            SortOrder.CHEAPEST  -> list.sortedBy { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //Алгоритм Левенштейна для поиска с опечатками

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1].lowercaseChar() == s2[j - 1].lowercaseChar()) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,        // удаление
                    dp[i][j - 1] + 1,        // вставка
                    dp[i - 1][j - 1] + cost  // замена
                )
            }
        }
        return dp[len1][len2]
    }

    val recentTransactions: StateFlow<List<Transaction>> = allTransactions
        .map { it.take(6) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> = allTransactions
        .map { list -> list.sumOf { if (it.isIncome) it.amount else -it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = allTransactions
        .map { list -> list.filter { it.isIncome }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = allTransactions
        .map { list -> list.filter { !it.isIncome }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val transactions: StateFlow<List<Transaction>> = allTransactions

    fun addTransaction(
        title: String,
        amount: Double,
        categoryId: String,
        categoryTitle: String,
        isIncome: Boolean,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (title.isBlank() || amount <= 0 || categoryId.isBlank()) {
            _error.value = "Пожалуйста, заполните все поля"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                transactionRepo.addTransaction(
                    Transaction(
                        title         = title.trim(),
                        amount        = amount,
                        categoryId    = categoryId,
                        categoryTitle = categoryTitle,
                        isIncome      = isIncome,
                        timestamp     = timestamp
                    )
                )
                // Обновляем баланс пользователя
                val delta = if (isIncome) amount else -amount
                userRepo.updateBalance(totalBalance.value + delta)

                // Если расход — обновляем currentSpent в бюджете этой категории
                if (!isIncome) {
                    val budget = budgets.value.find { it.categoryId == categoryId }
                    if (budget != null) {
                        budgetRepo.updateSpent(budget.id, budget.currentSpent + amount)
                    }
                }

                _successMessage.value = "Операция успешно сохранена"
            }.onFailure { _error.value = "Ошибка при сохранении: ${it.localizedMessage}" }
            _isLoading.value = false
        }
    }

    fun updateTransaction(
        original: Transaction,
        title: String,
        amount: Double,
        categoryId: String,
        categoryTitle: String,
        isIncome: Boolean,
        timestamp: Long
    ) {
        if (title.isBlank() || amount <= 0 || categoryId.isBlank()) {
            _error.value = "Пожалуйста, заполните все поля"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val updated = original.copy(
                    title         = title.trim(),
                    amount        = amount,
                    categoryId    = categoryId,
                    categoryTitle = categoryTitle,
                    isIncome      = isIncome,
                    timestamp     = timestamp
                )
                transactionRepo.updateTransaction(updated)

                // Пересчёт баланса: откатить старую сумму, добавить новую
                val oldDelta = if (original.isIncome) original.amount else -original.amount
                val newDelta = if (isIncome) amount else -amount
                userRepo.updateBalance(totalBalance.value - oldDelta + newDelta)

                // Откат бюджета по старой категории (если расход)
                if (!original.isIncome) {
                    budgets.value.find { it.categoryId == original.categoryId }?.let { b ->
                        budgetRepo.updateSpent(b.id, (b.currentSpent - original.amount).coerceAtLeast(0.0))
                    }
                }
                // Начисление в бюджет новой категории (если расход)
                if (!isIncome) {
                    budgets.value.find { it.categoryId == categoryId }?.let { b ->
                        budgetRepo.updateSpent(b.id, b.currentSpent + amount)
                    }
                }

                _successMessage.value = "Операция обновлена"
            }.onFailure { _error.value = "Ошибка при обновлении: ${it.localizedMessage}" }
            _isLoading.value = false
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            runCatching {
                val delta      = if (transaction.isIncome) -transaction.amount else transaction.amount
                val newBalance = totalBalance.value + delta
                transactionRepo.deleteTransaction(transaction.id)
                userRepo.updateBalance(newBalance)

                // Если расход — откатываем currentSpent в бюджете
                if (!transaction.isIncome) {
                    val budget = budgets.value.find { it.categoryId == transaction.categoryId }
                    if (budget != null) {
                        val newSpent = (budget.currentSpent - transaction.amount).coerceAtLeast(0.0)
                        budgetRepo.updateSpent(budget.id, newSpent)
                    }
                }
            }.onFailure { _error.value = "Ошибка удаления: ${it.localizedMessage}" }
        }
    }

    fun setDateRange(start: Long?, end: Long?) {
        _startDate.value = start
        _endDate.value   = end
    }

    fun clearDateFilter() {
        _startDate.value = null
        _endDate.value   = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: Int) {
        _typeFilter.value = type
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun toggleCategoryFilter(categoryId: String) {
        val current = _categoryFilter.value
        _categoryFilter.value = if (categoryId in current) current - categoryId else current + categoryId
    }

    fun clearCategoryFilter() {
        _categoryFilter.value = emptySet()
    }

    fun getCategoryTitle(categoryId: String): String =
        categories.value.find { it.id == categoryId }?.title ?: "Без категории"

    fun clearError()   { _error.value = null }
    fun clearSuccess() { _successMessage.value = null }
    fun clearFilter() {
        _startDate.value = null
        _endDate.value = null
        _searchQuery.value = ""
        _typeFilter.value = 0
        _sortOrder.value = SortOrder.NEWEST
        _categoryFilter.value = emptySet()
    }}
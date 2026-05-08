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

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions, _startDate, _endDate
    ) { transactions, start, end ->
        if (start == null || end == null) transactions
        else {
            val endOfDay = end + 86399999L
            transactions.filter { it.timestamp in start..endOfDay }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<Transaction>> = allTransactions
        .map { it.take(5) }
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
                val categoryTitle = categories.value.find { it.id == categoryId }?.title ?: ""
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

    fun getCategoryTitle(categoryId: String): String =
        categories.value.find { it.id == categoryId }?.title ?: "Без категории"

    fun clearError()   { _error.value = null }
    fun clearSuccess() { _successMessage.value = null }
    fun clearFilter()  = clearDateFilter()
}
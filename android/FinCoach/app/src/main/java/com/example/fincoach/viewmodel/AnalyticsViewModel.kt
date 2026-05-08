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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnalyticsViewModel : ViewModel() {

    private val transactionRepo = TransactionRepository()
    private val categoryRepo    = CategoryRepository()
    private val budgetRepo      = BudgetRepository()
    private val userRepo        = UserRepository()

    private val _isLoading      = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error          = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = transactionRepo
        .observeTransactions()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepo
        .observeCategories()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = budgetRepo
        .observeBudgets()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = transactions
        .map { list -> list.filter { it.isIncome }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = transactions
        .map { list -> list.filter { !it.isIncome }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Пересчитываем бюджеты каждый раз когда меняются транзакции или бюджеты
    init {
        viewModelScope.launch {
            combine(transactions, budgets) { txs, bdgs -> Pair(txs, bdgs) }
                .filter { (txs, bdgs) -> txs.isNotEmpty() && bdgs.isNotEmpty() }
                .collect { (txs, bdgs) ->
                    recalculateBudgets(txs, bdgs)
                }
        }
    }

    // Пересчитывает currentSpent для каждого бюджета на основе реальных транзакций
    private suspend fun recalculateBudgets(
        transactions: List<Transaction>,
        budgets: List<Budget>
    ) {
        budgets.forEach { budget ->
            // Определяем начало периода для этого бюджета
            val periodStart = budget.periodStart.takeIf { it > 0 }
                ?: Budget.currentPeriodStart(budget.period)

            // Считаем сумму всех расходов по этой категории за текущий период
            val actualSpent = transactions
                .filter { tx ->
                    !tx.isIncome &&
                            tx.categoryId == budget.categoryId &&
                            tx.timestamp >= periodStart
                }
                .sumOf { it.amount }

            // Обновляем только если значение отличается (избегаем лишних запросов)
            if (kotlin.math.abs(actualSpent - budget.currentSpent) > 0.001) {
                runCatching {
                    budgetRepo.updateSpent(budget.id, actualSpent)
                }
            }
        }
    }

    fun setBudget(categoryId: String, limitAmount: Double, period: String = "month") {
        if (limitAmount <= 0) { _error.value = "Лимит должен быть больше 0"; return }
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val existing = budgets.value.find { it.categoryId == categoryId }
                budgetRepo.setBudget(
                    Budget(
                        id           = existing?.id ?: "",
                        categoryId   = categoryId,
                        limitAmount  = limitAmount,
                        currentSpent = existing?.currentSpent ?: 0.0,
                        period       = period
                    )
                )
                _successMessage.value = "Бюджет установлен"
                // После создания — сразу пересчитываем
                recalculateBudgets(transactions.value, budgets.value)
            }.onFailure { _error.value = "Ошибка: ${it.localizedMessage}" }
            _isLoading.value = false
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            runCatching { budgetRepo.deleteBudget(budgetId) }
                .onFailure { _error.value = "Ошибка удаления" }
        }
    }

    fun clearError()   { _error.value = null }
    fun clearSuccess() { _successMessage.value = null }
}
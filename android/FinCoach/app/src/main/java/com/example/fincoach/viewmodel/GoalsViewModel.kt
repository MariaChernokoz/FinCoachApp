package com.example.fincoach.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fincoach.data.model.Goal
import com.example.fincoach.data.repository.GoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel : ViewModel() {

    private val repository = GoalRepository()

    private val _isLoading      = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error          = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    val goals: StateFlow<List<Goal>> = repository
        .observeGoals()
        .catch { e -> _error.value = "Ошибка загрузки: ${e.localizedMessage}"; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Активные цели (не завершённые)
    val activeGoals: StateFlow<List<Goal>> = goals
        .map { list -> list.filter { !it.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Завершённые цели
    val completedGoals: StateFlow<List<Goal>> = goals
        .map { list -> list.filter { it.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGoal(title: String, targetAmount: Double, deadline: Long?) {
        if (title.isBlank())       { _error.value = "Введите название цели"; return }
        if (targetAmount <= 0)     { _error.value = "Сумма должна быть больше 0"; return }

        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                repository.addGoal(
                    Goal(
                        title        = title.trim(),
                        targetAmount = targetAmount,
                        deadline     = deadline
                    )
                )
                _successMessage.value = "Цель создана!"
            }.onFailure { _error.value = "Ошибка: ${it.localizedMessage}" }
            _isLoading.value = false
        }
    }

    fun addSaving(goal: Goal, amount: Double) {
        if (amount <= 0) { _error.value = "Сумма должна быть больше 0"; return }

        viewModelScope.launch {
            runCatching {
                repository.addSaving(goal.id, amount, goal.savedAmount, goal.targetAmount)
                val newSaved = goal.savedAmount + amount
                if (newSaved >= goal.targetAmount) {
                    _successMessage.value = "🎉 Цель «${goal.title}» достигнута!"
                }
            }.onFailure { _error.value = "Ошибка: ${it.localizedMessage}" }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteGoal(goalId) }
                .onFailure { _error.value = "Ошибка удаления" }
        }
    }

    fun clearError()   { _error.value = null }
    fun clearSuccess() { _successMessage.value = null }
}
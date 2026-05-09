package com.example.fincoach.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fincoach.data.model.Transaction
import com.example.fincoach.data.repository.TransactionRepository
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AssistantViewModel : ViewModel() {

    private val transactionRepo = TransactionRepository()
    private val functions = Firebase.functions("us-central1")

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Привет! Я ваш финансовый AI коуч. Задавайте мне вопросы о ваших тратах, целях и бюджете!",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val transactions: StateFlow<List<Transaction>> = transactionRepo
        .observeTransactions()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        _messages.value = _messages.value + ChatMessage(text = text, isUser = true)

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            runCatching {
                // Формируем список транзакций как у подруги
                val txList = transactions.value.map { tx ->
                    hashMapOf(
                        "title"         to tx.title,
                        "amount"        to tx.amount,
                        "categoryTitle" to tx.categoryTitle,
                        "isIncome"      to tx.isIncome,
                        "timestamp"     to tx.timestamp
                    )
                }

                // Данные для функции — точно такой же формат как в iOS
                val data = hashMapOf(
                    "question"     to text,
                    "transactions" to txList
                )

                // Вызываем ту же Cloud Function что и подруга
                val result = functions
                    .getHttpsCallable("analyzeFinances")
                    .call(data)
                    .await()

                @Suppress("UNCHECKED_CAST")
                val resultData = result.data as? Map<String, Any>
                val answer = resultData?.get("answer") as? String
                    ?: "Не удалось получить ответ. Попробуйте ещё раз."

                _messages.value = _messages.value + ChatMessage(text = answer, isUser = false)

            }.onFailure { e ->
                val errorText = when {
                    "unauthenticated" in (e.message ?: "").lowercase() ->
                        "Необходимо войти в аккаунт"
                    "unavailable" in (e.message ?: "").lowercase() ->
                        "Сервер недоступен. Проверьте интернет."
                    else ->
                        "Произошла ошибка. Попробуйте ещё раз."
                }
                _error.value = errorText
                _messages.value = _messages.value + ChatMessage(
                    text = "Извините, произошла ошибка. Попробуйте ещё раз.",
                    isUser = false
                )
            }

            _isLoading.value = false
        }
    }

    fun clearError() { _error.value = null }
}
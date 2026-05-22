package com.example.fincoach.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fincoach.data.model.ChatMessage
import com.example.fincoach.data.repository.ChatRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AssistantViewModel : ViewModel() {

    private val chatRepo  = ChatRepository()
    private val functions = Firebase.functions("us-central1")

    // Приветствие — показываем всегда первым, в базе не храним
    private val greeting = ChatMessage(
        text = "Привет! Я ваш финансовый ассистент. Задавайте мне вопросы о ваших тратах, целях и бюджете!",
        isUser = false
    )

    // История чата из Firestore, приветствие добавляем сверху
    val messages: StateFlow<List<ChatMessage>> = chatRepo
        .observeMessages()
        .map { saved -> listOf(greeting) + saved }
        .catch { emit(listOf(greeting)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(greeting))

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // false на время запроса и 3 секунды после — чтобы не частить (rate limiting)
    private val _canSend = MutableStateFlow(true)
    val canSend: StateFlow<Boolean> = _canSend.asStateFlow()

    fun sendMessage(text: String) {
        val message = text.trim().take(500)
        if (message.isBlank() || !_canSend.value) return

        // Cloud Function требует авторизации
        if (Firebase.auth.currentUser == null) {
            _error.value = "Необходимо войти в аккаунт"
            return
        }

        viewModelScope.launch {
            _canSend.value = false
            _isLoading.value = true
            _error.value = null

            // Сохраняем вопрос пользователя
            runCatching { chatRepo.addMessage(message, isUser = true) }

            runCatching {
                // Сервер сам читает транзакции из Firestore — отправляем только текст
                val data = hashMapOf("message" to message)
                val result = functions
                    .getHttpsCallable("analyzeFinances")
                    .call(data)
                    .await()

                val response = result.data as? Map<*, *>
                val answer = response?.get("answer") as? String
                    ?: "Не удалось получить ответ. Попробуйте ещё раз."

                // Ответ ассистента (в том числе заглушка при success = false)
                chatRepo.addMessage(answer, isUser = false)

            }.onFailure { e ->
                _error.value = when ((e as? FirebaseFunctionsException)?.code) {
                    FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                        "Необходимо войти в аккаунт"
                    FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                        "Сообщение слишком длинное (макс. 500 символов) или пустое"
                    FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                        "Подождите несколько секунд перед следующим вопросом"
                    else ->
                        "Произошла ошибка, попробуйте позже"
                }
            }

            _isLoading.value = false
            // Пауза 3 секунды между запросами
            delay(3000)
            _canSend.value = true
        }
    }

    // Очистить переписку
    fun clearChat() {
        viewModelScope.launch {
            runCatching { chatRepo.clearChat() }
                .onFailure { _error.value = "Не удалось очистить чат" }
        }
    }

    fun clearError() { _error.value = null }
}

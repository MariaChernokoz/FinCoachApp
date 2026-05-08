package com.example.fincoach.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fincoach.data.model.Transaction
import com.example.fincoach.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class AssistantViewModel : ViewModel() {

    private val transactionRepo = TransactionRepository()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage(
            "Привет! Я твой финансовый помощник 👋\n\nМогу проанализировать твои траты, подсказать где можно сэкономить или ответить на любые вопросы о личных финансах. Спрашивай!",
            isUser = false
        ))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Загружаем транзакции для контекста
    private val transactions: StateFlow<List<Transaction>> = transactionRepo
        .observeTransactions()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        // Добавляем сообщение пользователя
        _messages.value = _messages.value + ChatMessage(userText, isUser = true)

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            runCatching {
                val reply = callAnthropicApi(userText, transactions.value)
                _messages.value = _messages.value + ChatMessage(reply, isUser = false)
            }.onFailure {
                _error.value = "Ошибка соединения. Проверьте интернет."
                _messages.value = _messages.value + ChatMessage(
                    "Извини, не удалось получить ответ. Попробуй ещё раз 🙏",
                    isUser = false
                )
            }

            _isLoading.value = false
        }
    }

    private suspend fun callAnthropicApi(
        userMessage: String,
        transactions: List<Transaction>
    ): String = withContext(Dispatchers.IO) {

        // Формируем краткий контекст транзакций для ИИ
        val txContext = buildTransactionContext(transactions)

        // Системный промпт — объясняем ИИ кто он и что знает
        val systemPrompt = """
            Ты финансовый помощник в мобильном приложении FinCoach. 
            Ты помогаешь пользователю анализировать его личные финансы, давать советы по экономии и финансовому планированию.
            Отвечай кратко, по делу, дружелюбно. Используй эмодзи умеренно.
            Отвечай на русском языке.
            
            Вот данные о транзакциях пользователя:
            $txContext
        """.trimIndent()

        // Строим историю сообщений для API (без первого приветствия от бота)
        val history = _messages.value
            .drop(1) // убираем приветствие
            .dropLast(1) // убираем только что добавленное сообщение пользователя
            .map { msg ->
                JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "assistant")
                    put("content", msg.text)
                }
            }

        // Добавляем текущее сообщение
        val allMessages = JSONArray().apply {
            history.forEach { put(it) }
            put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })
        }

        val body = JSONObject().apply {
            put("model", "claude-haiku-4-5-20251001")
            put("max_tokens", 1024)
            put("system", systemPrompt)
            put("messages", allMessages)
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("x-api-key", "ТВОЙ_API_КЛЮЧ")  // <- вставь сюда ключ
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("API error ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        json.getJSONArray("content")
            .getJSONObject(0)
            .getString("text")
    }

    private fun buildTransactionContext(transactions: List<Transaction>): String {
        if (transactions.isEmpty()) return "Транзакций пока нет."

        val totalIncome  = transactions.filter { it.isIncome }.sumOf { it.amount }
        val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
        val balance      = totalIncome - totalExpense

        val byCategory = transactions
            .filter { !it.isIncome }
            .groupBy { it.categoryTitle.ifBlank { "Другое" } }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .take(5)
            .joinToString("\n") { (cat, sum) -> "  - $cat: ${String.format("%,.0f", sum)} ₽" }

        val recentTx = transactions.take(5)
            .joinToString("\n") { tx ->
                "  - ${if (tx.isIncome) "+" else "-"}${String.format("%,.0f", tx.amount)} ₽ (${tx.categoryTitle.ifBlank { "Другое" }}): ${tx.title}"
            }

        return """
            Баланс: ${String.format("%,.0f", balance)} ₽
            Всего доходов: ${String.format("%,.0f", totalIncome)} ₽
            Всего расходов: ${String.format("%,.0f", totalExpense)} ₽
            
            Топ категорий расходов:
            $byCategory
            
            Последние операции:
            $recentTx
            
            Всего транзакций: ${transactions.size}
        """.trimIndent()
    }

    fun clearError() { _error.value = null }
}
package com.example.fincoach.ui.screens.assistant

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fincoach.data.model.ChatMessage
import com.example.fincoach.ui.FinCoachTopBar
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.LocalAppColors
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.TextPlaceholder
import com.example.fincoach.viewmodel.AssistantViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssistantScreen() {
    val c = LocalAppColors.current
    val vm: AssistantViewModel = viewModel()
    val messages  by vm.messages.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val canSendVm by vm.canSend.collectAsState()
    val error     by vm.error.collectAsState()
    val context = LocalContext.current

    // Показываем ошибки ассистента всплывающим сообщением
    LaunchedEffect(error) {
        error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            vm.clearError()
        }
    }

    var inputText by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        FinCoachTopBar(
            title = "Финансовый ассистент",
            actionIcon = Icons.Default.Delete,
            onActionClick = { showClearDialog = true },
            verticalPadding = 4.dp
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { message -> MessageBubble(message = message) }
            if (isLoading) {
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) { TypingIndicator() } }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }

        if (messages.size == 1 && !isLoading) {
            ExampleQuestions(
                questions = listOf("Дай общую статистику моих трат", "Почему я трачу так много?", "Как оптимизировать мой бюджет?", "Успею ли я накопить на цель?"),
                onQuestionClick = { vm.sendMessage(it) }
            )
        }

        HorizontalDivider(color = c.divider)

        Row(modifier = Modifier.fillMaxWidth().background(c.cardBg).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = inputText, onValueChange = { inputText = it },
                placeholder = { Text("Задайте вопрос...", color = TextPlaceholder) },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = c.inputBg, unfocusedContainerColor = c.inputBg, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (inputText.isNotBlank() && !isLoading && canSendVm) { vm.sendMessage(inputText.trim()); inputText = "" } }),
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            val canSend = inputText.isNotBlank() && !isLoading && canSendVm
            Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(if (canSend) DeepGreen else c.inputBg), contentAlignment = Alignment.Center) {
                IconButton(onClick = { if (canSend) { vm.sendMessage(inputText.trim()); inputText = "" } }, enabled = canSend) {
                    Icon(Icons.Default.Send, contentDescription = "Отправить", tint = if (canSend) c.cardBg else TextPlaceholder, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    // Подтверждение очистки истории
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистить историю?") },
            text = { Text("Вся переписка с ассистентом будет удалена без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = { vm.clearChat(); showClearDialog = false }) {
                    Text("Очистить", color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Отмена", color = TextGray)
                }
            }
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val c = LocalAppColors.current
    val isUser = message.isUser
    val timeLabel = remember(message.timestamp) { SimpleDateFormat("HH:mm", Locale("ru")).format(Date(message.timestamp)) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        if (!isUser) { Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(DeepGreen), contentAlignment = Alignment.Center) { Text("✦", color = c.cardBg, fontSize = 10.sp) }; Spacer(Modifier.width(6.dp)) }
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Box(modifier = Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (isUser) 18.dp else 4.dp, bottomEnd = if (isUser) 4.dp else 18.dp)).background(if (isUser) DeepGreen else Color.Gray.copy(alpha = 0.15f)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(text = message.text, color = if (isUser) c.cardBg else c.textPrimary, fontSize = 14.sp, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text(text = timeLabel, fontSize = 10.sp, color = TextPlaceholder, modifier = Modifier.padding(horizontal = 4.dp))
        }
        if (isUser) Spacer(Modifier.width(6.dp))
    }
}

@Composable
private fun TypingIndicator() {
    Row(modifier = Modifier.padding(start = 34.dp)) {
        Box(modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(Color.Gray.copy(alpha = 0.15f)).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index ->
                    val inf = rememberInfiniteTransition(label = "d$index")
                    val scale by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse, StartOffset(index * 200)), label = "s$index")
                    Box(modifier = Modifier.size(8.dp).scale(scale).clip(CircleShape).background(TextGray))
                }
            }
        }
    }
}

@Composable
private fun ExampleQuestions(questions: List<String>, onQuestionClick: (String) -> Unit) {
    val c = LocalAppColors.current
    Column(
        modifier = Modifier.fillMaxWidth().background(c.cardBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Примеры вопросов:", color = TextGray, fontSize = 12.sp)
        questions.forEach { question ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = c.divider,
                onClick = { onQuestionClick(question) }) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 14.sp); Spacer(Modifier.width(10.dp))
                    Text(
                        text = question,
                        fontSize = 13.sp,
                        color = c.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text("›", color = TextGray, fontSize = 18.sp)
                }
            }
        }
    }
}
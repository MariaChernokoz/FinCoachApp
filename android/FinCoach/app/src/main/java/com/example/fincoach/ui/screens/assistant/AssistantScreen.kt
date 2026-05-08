package com.example.fincoach.ui.screens.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fincoach.ui.theme.BgLightGray
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.TextBlack
import com.example.fincoach.ui.theme.TextDarkGray
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.TextPlaceholder
import com.example.fincoach.ui.theme.White
import com.example.fincoach.viewmodel.AssistantViewModel
import com.example.fincoach.viewmodel.ChatMessage

@Composable
fun AssistantScreen() {
    val vm: AssistantViewModel = viewModel()
    val messages  by vm.messages.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Прокручиваем вниз когда появляется новое сообщение
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLightGray)
    ) {
        // Шапка
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Аватар ИИ
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DeepGreen),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = White, fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Финансовый ассистент",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextBlack
                )
                Text(
                    if (isLoading) "печатает..." else "онлайн",
                    fontSize = 12.sp,
                    color = if (isLoading) DeepGreen else TextGray
                )
            }
        }

        HorizontalDivider(color = BgLightGray)

        //Сообщения
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
            }

            // Индикатор загрузки
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        TypingIndicator()
                    }
                }
            }
        }

        // ── Быстрые вопросы (показываем только если 1 сообщение — приветствие) ──
        if (messages.size == 1 && !isLoading) {
            QuickQuestions(
                questions = listOf(
                    "На что я трачу больше всего?",
                    "Как мне сэкономить?",
                    "Проанализируй мои расходы",
                    "Как правильно вести бюджет?"
                ),
                onQuestionClick = { vm.sendMessage(it) }
            )
        }

        //Поле ввода
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Напишите вопрос...", color = TextPlaceholder) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = BgLightGray,
                    unfocusedContainerColor = BgLightGray,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank() && !isLoading) {
                        vm.sendMessage(inputText)
                        inputText = ""
                    }
                }),
                maxLines = 4
            )

            Spacer(Modifier.width(8.dp))

            // Кнопка отправки
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (!isLoading && inputText.isNotBlank()) DeepGreen else BgLightGray),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            vm.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = !isLoading && inputText.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Отправить",
                        tint = if (!isLoading && inputText.isNotBlank()) White else TextPlaceholder,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Пузырь сообщения

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Аватар бота
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DeepGreen),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = White, fontSize = 10.sp)
            }
            Spacer(Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart    = 18.dp,
                        topEnd      = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd   = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(if (isUser) DeepGreen else White)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) White else TextBlack,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        if (isUser) {
            Spacer(Modifier.width(6.dp))
        }
    }
}

//Индикатор печати

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(start = 34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(TextGray)
                    )
                }
            }
        }
    }
}

// Быстрые вопросы

@Composable
private fun QuickQuestions(
    questions: List<String>,
    onQuestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Попробуй спросить:",
            color = TextGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        questions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { question ->
                    Surface(
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = White,
                        shadowElevation = 1.dp,
                        onClick = { onQuestionClick(question) }
                    ) {
                        Text(
                            text = question,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 12.sp,
                            color = TextDarkGray,
                            lineHeight = 16.sp
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
package com.example.fincoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.White

@Composable
fun FinCoachTopBar(
    title: String,
    navigationIcon: ImageVector? = null,
    onNavigationClick: () -> Unit = {},
    actionIcon: ImageVector? = null,
    onActionClick: () -> Unit = {},
    customAction: @Composable (() -> Unit)? = null,
    verticalPadding: androidx.compose.ui.unit.Dp = 14.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepGreen)
            .statusBarsPadding()
            .heightIn(min = 48.dp)
            .padding(horizontal = 4.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кнопка назад / левая иконка
        if (navigationIcon != null) {
            IconButton(onClick = onNavigationClick) {
                Icon(navigationIcon, contentDescription = null, tint = White)
            }
        } else {
            Spacer(Modifier.width(16.dp))
        }

        // Заголовок
        Text(
            text = title,
            color = White,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.weight(1f)
        )

        // Правая часть — кастомный контент или иконка
        when {
            customAction != null -> customAction()
            actionIcon != null -> {
                IconButton(onClick = onActionClick) {
                    Icon(actionIcon, contentDescription = null, tint = White)
                }
            }
            else -> Spacer(Modifier.width(48.dp))
        }
    }
}
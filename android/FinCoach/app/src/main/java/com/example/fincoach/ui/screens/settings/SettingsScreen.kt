package com.example.fincoach.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fincoach.ui.theme.BgLightGray
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.TextDarkGray
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.White
import com.example.fincoach.viewmodel.AuthViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val authViewModel: AuthViewModel = viewModel()
    val context = LocalContext.current

    val userEmail   = authViewModel.getCurrentUserEmail()
    val userName    = authViewModel.getCurrentUserName()
    val firstLetter = if (userName.isNotEmpty()) userName.take(1).uppercase() else "?"

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    val errorMessage by authViewModel.errorMessage.collectAsState()

    // Слушаем сообщения об успехе / ошибках из ViewModel
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearError()
        }
    }

    // Диалог выхода
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Выйти?") },
            text  = { Text("Вы уверены, что хотите выйти из аккаунта?") },
            confirmButton = {
                TextButton(onClick = { authViewModel.logout(); onLogout() }) {
                    Text("Выйти", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Отмена", color = TextGray)
                }
            }
        )
    }

    // Диалог подтверждения смены пароля
    if (showResetPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showResetPasswordDialog = false },
            title = { Text("Сброс пароля") },
            text  = { Text("Мы отправим ссылку для создания нового пароля на вашу почту $userEmail. Продолжить?") },
            confirmButton = {
                TextButton(onClick = {
                    showResetPasswordDialog = false
                    if (userEmail.isNotEmpty() && userEmail != "Email не указан") {
                        authViewModel.resetPassword(userEmail)
                    } else {
                        Toast.makeText(context, "Не удалось определить Email", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Отправить", color = DeepGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPasswordDialog = false }) {
                    Text("Отмена", color = TextGray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLightGray)
            .verticalScroll(rememberScrollState())
    ) {
        // Шапка
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepGreen)
                .padding(top = 48.dp, bottom = 32.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = White)
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(Color(0xFFB4EC51), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(firstLetter, color = White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(userName, color = White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(userEmail, color = White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Данные аккаунта
        SectionLabel("ДАННЫЕ АККАУНТА")
        SettingsCard {
            SettingsRow(Icons.Default.Person, "Имя пользователя", userName)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BgLightGray)
            SettingsRow(Icons.Default.Email, "Email", userEmail)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BgLightGray)

            // Кликабельная строка для сброса пароля
            SettingsRowClickable(
                icon = Icons.Default.LockReset,
                label = "Безопасность",
                value = "Сбросить текущий пароль",
                onClick = {
                    showResetPasswordDialog = true
                }
            )
        }

        Spacer(Modifier.height(32.dp))

        // Кнопка выхода
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { showLogoutDialog = true },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ExitToApp, null, tint = Color(0xFFFF5252), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Выйти из системы", color = Color(0xFFFF5252), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// Вспомогательные компоненты

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextGray,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 28.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp),
        content = content
    )
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(DeepGreen.copy(0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = DeepGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, color = TextGray, fontSize = 12.sp)
            Text(value, color = TextDarkGray, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }
}

@Composable
private fun SettingsRowClickable(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(DeepGreen.copy(0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = DeepGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = TextGray, fontSize = 12.sp)
            Text(value, color = TextDarkGray, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextGray,
            modifier = Modifier.size(20.dp)
        )
    }
}
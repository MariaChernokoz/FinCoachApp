package com.example.fincoach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fincoach.auth.AuthViewModel
import com.example.fincoach.ui.theme.BgLightGray
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.White

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val authViewModel: AuthViewModel = viewModel()
    val userEmail = authViewModel.getCurrentUserEmail()
    val userName = authViewModel.getCurrentUserName()
    val firstLetter = if (userName.isNotEmpty()) userName.take(1).uppercase() else "?"

    Scaffold(
        bottomBar = {
            // Внешний контейнер для отступов
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp), // Отступы от краев
                contentAlignment = Alignment.Center
            ) {
                // "белый остров"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(36.dp),
                    color = White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Иконка 1
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(androidx.compose.material.icons.Icons.Default.List, null, modifier = Modifier.size(26.dp), tint = Color.Black.copy(alpha = 0.6f))
                        }
                        // Иконка 2
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(androidx.compose.material.icons.Icons.Default.DateRange, null, modifier = Modifier.size(26.dp), tint = Color.Black.copy(alpha = 0.6f))
                        }
                        // Иконка 3
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(androidx.compose.material.icons.Icons.Default.Email, null, modifier = Modifier.size(26.dp), tint = Color.Black.copy(alpha = 0.6f))
                        }
                        // Иконка 4
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(androidx.compose.material.icons.Icons.Default.Info, null, modifier = Modifier.size(26.dp), tint = Color.Black.copy(alpha = 0.6f))
                        }

                        // АКТИВНАЯ ИКОНКА (Настройки)
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .padding(4.dp)
                                .background(Color(0xFFEFEFF4), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color(0xFF007AFF), // синий цвет
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgLightGray)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "ПРОФИЛЬ",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )

            // Карточка пользователя
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFB4EC51), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(firstLetter, color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(userName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(userEmail, color = TextGray, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Кнопка выхода
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        authViewModel.logout()
                        onLogout()
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Выйти из аккаунта", color = Color(0xFFFF5252), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
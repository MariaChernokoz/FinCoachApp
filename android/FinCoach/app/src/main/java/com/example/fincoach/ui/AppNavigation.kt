package com.example.fincoach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.White

@Composable
fun FinCoachBottomBar(
    activeTab: Int,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(36.dp),
            color = White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Цели (tab 0)
                NavigationIcon(Icons.Default.Star, activeTab == 0) { onNavigate("goals") }
                // Аналитика (tab 2)
                NavigationIcon(Icons.Default.Analytics, activeTab == 2) { onNavigate("analytics") }
                // Центр: Транзакции (tab 1)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (activeTab == 1) DeepGreen else Color.Transparent)
                        .clickable { onNavigate("transactions") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Wallet, "Главная",
                        tint = if (activeTab == 1) White else TextGray,
                        modifier = Modifier.size(26.dp)
                    )
                }
                // Ассистент (tab 3)
                NavigationIcon(Icons.Rounded.AutoAwesome, activeTab == 3) { onNavigate("assistant") }
                // Настройки (tab 4)
                NavigationIcon(Icons.Default.Person, activeTab == 4) { onNavigate("settings") }
            }
        }
    }
}

@Composable
fun NavigationIcon(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) DeepGreen else TextGray,
            modifier = Modifier.size(24.dp)
        )
    }
}
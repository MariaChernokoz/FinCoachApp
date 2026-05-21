package com.example.fincoach.ui.screens.transactions

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fincoach.data.model.Transaction
import com.example.fincoach.ui.theme.BgLightGray
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.TextDarkGray
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.White
import com.example.fincoach.viewmodel.TransactionViewModel

@Composable
fun TransactionsScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val txViewModel: TransactionViewModel = viewModel()

    val totalBalance by txViewModel.totalBalance.collectAsState()
    val totalIncome by txViewModel.totalIncome.collectAsState()
    val totalExpense by txViewModel.totalExpense.collectAsState()
    val recentTxs by txViewModel.recentTransactions.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLightGray)
            .padding(16.dp)
    ) {
        Spacer(Modifier.height(40.dp))

        Text(
            text = "МОИ ФИНАНСЫ",
            style = MaterialTheme.typography.labelMedium,
            color = TextGray,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Общий баланс", color = TextGray, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${String.format("%,.2f", totalBalance)} ₽",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalBalance >= 0) DeepGreen else Color(0xFFE74C3C)
                )

                Spacer(Modifier.height(20.dp))

                Row(Modifier.fillMaxWidth()) {
                    SummaryItem(
                        label = "Доходы",
                        amount = totalIncome,
                        color = Color(0xFF27AE60),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryItem(
                        label = "Расходы",
                        amount = totalExpense,
                        color = Color(0xFFE74C3C),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onNavigateToAdd,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = White)
            Spacer(Modifier.width(8.dp))
            Text("Добавить операцию", color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Последние операции", color = TextDarkGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "Все",
                color = DeepGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigateToHistory() }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (recentTxs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Операций пока нет", color = TextGray)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    recentTxs.forEachIndexed { index, tx ->
                        // Вызываем нашу ОДНУ функцию
                        TransactionRow(tx)

                        if (index < recentTxs.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = BgLightGray,
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = TextGray, fontSize = 12.sp)
        Text(
            text = "${if (amount > 0 && label == "Доходы") "+" else ""}${String.format("%,.2f", amount)} ₽",
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun TransactionRow(tx: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кружок с иконкой стрелочки
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (tx.isIncome) Color(0xFF27AE60).copy(0.1f) else Color(0xFFE74C3C).copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (tx.isIncome) "↑" else "↓",
                color = if (tx.isIncome) Color(0xFF27AE60) else Color(0xFFE74C3C),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(tx.title.ifBlank { "Без названия" }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            // Берем название категории сразу из транзакции
            Text(tx.categoryTitle, color = TextGray, fontSize = 12.sp)
        }

        Text(
            text = "${if (tx.isIncome) "+" else "−"} ${String.format("%,.2f", tx.amount)} ₽",
            fontWeight = FontWeight.Bold,
            color = if (tx.isIncome) Color(0xFF27AE60) else Color(0xFFE74C3C),
            fontSize = 14.sp
        )
    }
}
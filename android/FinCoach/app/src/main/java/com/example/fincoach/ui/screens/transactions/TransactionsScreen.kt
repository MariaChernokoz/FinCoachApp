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
import com.example.fincoach.ui.FinCoachTopBar
import com.example.fincoach.ui.categoryEmoji
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.LocalAppColors
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.White
import com.example.fincoach.viewmodel.TransactionViewModel

@Composable
fun TransactionsScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val vm: TransactionViewModel = viewModel()
    val totalBalance by vm.totalBalance.collectAsState()
    val totalIncome  by vm.totalIncome.collectAsState()
    val totalExpense by vm.totalExpense.collectAsState()
    val recentTxs    by vm.recentTransactions.collectAsState()

    val limitedTxs = recentTxs.take(7)
    val c = LocalAppColors.current

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        FinCoachTopBar(title = "Мои финансы")

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = c.cardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Общий баланс", color = TextGray, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(text = "${String.format("%,.2f", totalBalance)} ₽", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = if (totalBalance >= 0) DeepGreen else Color(0xFFE74C3C))
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = c.divider)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        SummaryItem("Доходы", totalIncome, DeepGreen, Modifier.weight(1f))
                        SummaryItem("Расходы", totalExpense, Color(0xFFE74C3C), Modifier.weight(1f))
                    }
                }
            }

            Button(onClick = onNavigateToAdd, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = White)
                Spacer(Modifier.width(8.dp))
                Text("Добавить операцию", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Последние операции", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Все →", color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onNavigateToHistory() })
            }

            if (limitedTxs.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = c.cardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💸", fontSize = 32.sp); Spacer(Modifier.height(8.dp))
                            Text("Операций пока нет", color = TextGray, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = c.cardBg),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column {
                        limitedTxs.forEachIndexed { index, tx ->
                            TransactionRow(tx)
                            if (index < limitedTxs.lastIndex) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = c.divider)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, amount: Double, color: Color, modifier: Modifier) {
    val c = LocalAppColors.current
    Column(modifier = modifier) {
        Text(label, color = c.textSecondary, fontSize = 12.sp)
        Text("${if (label == "Доходы") "+" else "−"} ${String.format("%,.2f", amount)} ₽", color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun TransactionRow(tx: Transaction) {
    val c = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (tx.isIncome) DeepGreen.copy(0.12f) else Color(0xFFE74C3C).copy(0.12f)), contentAlignment = Alignment.Center) {
            Text(categoryEmoji(tx.categoryTitle, tx.isIncome), fontSize = 22.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.title.ifBlank { "Без названия" }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = c.textPrimary)
            Text(tx.categoryTitle, color = c.textSecondary, fontSize = 12.sp)
        }

        Text(
            text = "${if (tx.isIncome) "+" else "−"} ${String.format("%,.2f", tx.amount)} ₽",
            fontWeight = FontWeight.Bold,
            color = if (tx.isIncome) DeepGreen else c.textPrimary,
            fontSize = 14.sp
        )
    }
}
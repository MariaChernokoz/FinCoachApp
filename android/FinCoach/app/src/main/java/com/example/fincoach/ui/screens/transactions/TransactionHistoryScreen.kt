package com.example.fincoach.ui.screens.transactions

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fincoach.data.model.Transaction
import com.example.fincoach.ui.theme.BgLightGray
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.TextBlack
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.TextPlaceholder
import com.example.fincoach.ui.theme.White
import com.example.fincoach.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(onBack: () -> Unit) {
    val vm: TransactionViewModel = viewModel()
    val transactions by vm.filteredTransactions.collectAsState()
    val startDate    by vm.startDate.collectAsState()
    val endDate      by vm.endDate.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    // Группируем по дате
    val grouped = remember(transactions) {
        transactions
            .groupBy { formatDateGroup(it.timestamp) }
            .entries
            .sortedByDescending { entry -> entry.value.maxOf { it.timestamp } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Фильтр", tint = DeepGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White,
                    scrolledContainerColor = White,
                    navigationIconContentColor = TextBlack,
                    titleContentColor = TextBlack,
                    actionIconContentColor = DeepGreen
                ),
                modifier = Modifier.height(52.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(BgLightGray)
        ) {
            // Плашка активного фильтра
            if (startDate != null && endDate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatDate(startDate!!)} — ${formatDate(endDate!!)}",
                        fontSize = 14.sp,
                        color = DeepGreen,
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(onClick = { vm.clearFilter() }) {
                        Text("Сбросить", color = Color(0xFFE74C3C))
                    }
                }
            }

            when {
                transactions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💸", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (startDate != null) "Операций за этот период нет"
                                else "Операций пока нет",
                                color = TextGray,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        grouped.forEach { (dateLabel, txList) ->
                            // Заголовок группы
                            item(key = "header_$dateLabel") {
                                Text(
                                    text = dateLabel,
                                    color = TextGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp)
                                )
                            }
                            // Карточка с транзакциями группы
                            item(key = "group_$dateLabel") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = White),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column {
                                        txList.forEachIndexed { index, tx ->
                                            HistoryTxItem(
                                                transaction = tx,
                                                onDelete    = { vm.deleteTransaction(tx) }
                                            )
                                            if (index < txList.lastIndex) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = BgLightGray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // Выбор диапазона дат
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setDateRange(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                    showDatePicker = false
                }) { Text("ОК", color = DeepGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена", color = TextGray)
                }
            }
        ) {
            DateRangePicker(
                state    = dateRangePickerState,
                modifier = Modifier
                    .height(450.dp)
                    .padding(16.dp),
                title    = { Text("Выберите период", modifier = Modifier.padding(16.dp)) }
            )
        }
    }
}

//Строка транзакции

@Composable
private fun HistoryTxItem(transaction: Transaction, onDelete: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Удалить операцию?") },
            text  = { Text("«${transaction.title.ifBlank { "Без названия" }}» будет удалена.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDialog = false
                }) { Text("Удалить", color = Color(0xFFE74C3C)) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Отмена", color = TextGray)
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Иконка
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (transaction.isIncome) Color(0xFF27AE60).copy(0.12f)
                    else Color(0xFFE74C3C).copy(0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (transaction.isIncome) "↑" else "↓",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (transaction.isIncome) Color(0xFF27AE60) else Color(0xFFE74C3C)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Название и категория
        Column(Modifier.weight(1f)) {
            Text(
                transaction.title.ifBlank { "Без названия" },
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TextBlack
            )
            Text(
                transaction.categoryTitle.ifBlank { "—" },
                color = TextGray,
                fontSize = 12.sp
            )
        }

        // Сумма
        Text(
            text = "${if (transaction.isIncome) "+" else "−"} ${
                String.format("%,.2f", transaction.amount)
            } ₽",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (transaction.isIncome) Color(0xFF27AE60) else Color(0xFFE74C3C)
        )

        // Кнопка удаления
        IconButton(
            onClick = { showDialog = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Удалить",
                tint = TextPlaceholder,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Хелперы дат

private fun formatDateGroup(timestamp: Long): String {
    val txCal     = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today     = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
    return when {
        isSameDay(txCal, today)     -> "Сегодня"
        isSameDay(txCal, yesterday) -> "Вчера"
        else -> SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date(timestamp))
    }
}

private fun isSameDay(a: Calendar, b: Calendar) =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd.MM.yy", Locale("ru")).format(Date(millis))
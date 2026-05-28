package com.example.fincoach.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.fincoach.ui.FinCoachTopBar
import com.example.fincoach.ui.categoryEmoji
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.LocalAppColors
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.TextPlaceholder
import com.example.fincoach.viewmodel.SortOrder
import com.example.fincoach.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(onBack: () -> Unit, onNavigateToEdit: (String) -> Unit = {}) {
    val vm: TransactionViewModel = viewModel()
    val transactions by vm.filteredTransactions.collectAsState()
    val startDate    by vm.startDate.collectAsState()
    val endDate        by vm.endDate.collectAsState()
    val searchQuery    by vm.searchQuery.collectAsState()
    val typeFilter     by vm.typeFilter.collectAsState()
    val sortOrder      by vm.sortOrder.collectAsState()

    val c = LocalAppColors.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showSortMenu   by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    val grouped = remember(transactions, sortOrder) {
        if (sortOrder == SortOrder.NEWEST || sortOrder == SortOrder.OLDEST) {
            // При сортировке по дате — группируем по дням
            transactions
                .groupBy { formatDateGroup(it.timestamp) }
                .entries
                .let { entries ->
                    if (sortOrder == SortOrder.NEWEST)
                        entries.sortedByDescending { it.value.maxOfOrNull { tx -> tx.timestamp } ?: 0L }
                    else
                        entries.sortedBy { it.value.minOfOrNull { tx -> tx.timestamp } ?: 0L }
                }
        } else {
            // При сортировке по сумме — один список «Все операции»
            listOf(
                object : Map.Entry<String, List<com.example.fincoach.data.model.Transaction>> {
                    override val key = "Все операции"
                    override val value = transactions
                }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        FinCoachTopBar(
            title = "История",
            navigationIcon = Icons.Default.ArrowBack,
            onNavigationClick = onBack,
            actionIcon = Icons.Default.DateRange,
            onActionClick = { showDatePicker = true }
        )

        // БЛОК ПОИСКА И ФИЛЬТРОВ
        Column(modifier = Modifier.fillMaxWidth().background(c.cardBg).padding(bottom = 6.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                placeholder = { Text("Поиск по названию или категории", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextGray, modifier = Modifier.size(20.dp)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = c.inputBg,
                    unfocusedContainerColor = c.inputBg,
                    focusedBorderColor = DeepGreen.copy(0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = DeepGreen
                ),
                singleLine = true
            )

            // Фильтр типа + кнопка сортировки — в одной строке
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Все" to 0, "Доходы" to 1, "Расходы" to 2).forEach { (label, type) ->
                        FilterChip(
                            selected = typeFilter == type,
                            onClick = { vm.setTypeFilter(type) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = if (typeFilter == type) DeepGreen else TextGray
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Transparent,
                                labelColor = TextGray,
                                selectedContainerColor = DeepGreen.copy(0.1f),
                                selectedLabelColor = DeepGreen
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = typeFilter == type,
                                borderColor = if (typeFilter == type) DeepGreen else TextPlaceholder,
                                borderWidth = 1.dp
                            )
                        )
                    }
                }

                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(DeepGreen.copy(alpha = 0.08f))
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Sort, null, tint = DeepGreen, modifier = Modifier.size(15.dp))
                        Text(
                            text = when (sortOrder) {
                                SortOrder.NEWEST    -> "Сначала новые"
                                SortOrder.OLDEST    -> "Сначала старые"
                                SortOrder.EXPENSIVE -> "Сначала дорогие"
                                SortOrder.CHEAPEST  -> "Сначала дешёвые"
                            },
                            fontSize = 13.sp,
                            color = DeepGreen
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        listOf(
                            "Сначала новые"   to SortOrder.NEWEST,
                            "Сначала старые"  to SortOrder.OLDEST,
                            "Сначала дорогие" to SortOrder.EXPENSIVE,
                            "Сначала дешёвые" to SortOrder.CHEAPEST
                        ).forEach { (label, order) ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 14.sp,
                                            color = if (sortOrder == order) DeepGreen else c.textPrimary
                                        )
                                        if (sortOrder == order) {
                                            Spacer(Modifier.width(24.dp))
                                            Text("✓", color = DeepGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                },
                                onClick = { vm.setSortOrder(order); showSortMenu = false }
                            )
                        }
                    }
                }
            }
        }

        // Плашка активного фильтра дат
        if (startDate != null && endDate != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${formatDate(startDate!!)} — ${formatDate(endDate!!)}", fontSize = 13.sp, color = DeepGreen)
                TextButton(onClick = { vm.clearFilter() }) { Text("Сбросить", color = Color(0xFFE74C3C), fontSize = 13.sp) }
            }
        }

        when {
            transactions.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💸", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(if (startDate != null) "Операций за этот период нет" else "Операций пока нет", color = TextGray, fontSize = 15.sp)
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
                        item(key = "header_$dateLabel") {
                            Text(dateLabel, color = TextGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp))
                        }
                        item(key = "group_$dateLabel") {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = c.cardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                                Column {
                                    txList.forEachIndexed { index, tx ->
                                        HistoryTxItem(transaction = tx, onDelete = { vm.deleteTransaction(tx) }, onEdit = { onNavigateToEdit(tx.id) })
                                        if (index < txList.lastIndex) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = c.divider)
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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { vm.setDateRange(dateRangePickerState.selectedStartDateMillis, dateRangePickerState.selectedEndDateMillis); showDatePicker = false }) { Text("ОК", color = DeepGreen) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Отмена", color = TextGray) } }
        ) {
            DateRangePicker(state = dateRangePickerState, modifier = Modifier.height(450.dp).padding(16.dp), title = { Text("Выберите период", modifier = Modifier.padding(16.dp)) })
        }
    }
}

@Composable
private fun HistoryTxItem(transaction: Transaction, onDelete: () -> Unit, onEdit: () -> Unit = {}) {
    val c = LocalAppColors.current
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Удалить операцию?") },
            text  = { Text("«${transaction.title.ifBlank { "Без названия" }}» будет удалена.") },
            confirmButton = { TextButton(onClick = { onDelete(); showDialog = false }) { Text("Удалить", color = Color(0xFFE74C3C)) } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена", color = TextGray) } }
        )
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (transaction.isIncome) DeepGreen.copy(0.12f) else Color(0xFFE74C3C).copy(0.12f)), contentAlignment = Alignment.Center) {
            Text(categoryEmoji(transaction.categoryTitle, transaction.isIncome), fontSize = 22.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(transaction.title.ifBlank { "Без названия" }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = c.textPrimary)
            Text(transaction.categoryTitle.ifBlank { "—" }, color = c.textSecondary, fontSize = 12.sp)
        }
        Text("${if (transaction.isIncome) "+" else "−"} ${String.format("%,.2f", transaction.amount)} ₽", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (transaction.isIncome) DeepGreen else c.textPrimary)
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, null, tint = TextPlaceholder, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = { showDialog = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, null, tint = TextPlaceholder, modifier = Modifier.size(16.dp))
        }
    }
}

private fun formatDateGroup(timestamp: Long): String {
    val txCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
    return when {
        isSameDay(txCal, today)     -> "Сегодня"
        isSameDay(txCal, yesterday) -> "Вчера"
        else -> SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date(timestamp))
    }
}

private fun isSameDay(a: Calendar, b: Calendar) =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd.MM.yy", Locale("ru")).format(Date(millis))
package com.example.fincoach.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fincoach.data.model.Budget
import com.example.fincoach.data.model.Category
import com.example.fincoach.ui.theme.BgLightGray
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.TextBlack
import com.example.fincoach.ui.theme.TextDarkGray
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.TextPlaceholder
import com.example.fincoach.ui.theme.White
import com.example.fincoach.viewmodel.AnalyticsViewModel
import java.util.Calendar

private val chartColors = listOf(
    Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFF45B7D1),
    Color(0xFF96CEB4), Color(0xFFF7DC6F), Color(0xFFBB8FCE),
    Color(0xFFF0A500), Color(0xFF58D68D)
)

@Composable
fun AnalyticsScreen() {
    val vm: AnalyticsViewModel = viewModel()

    val transactions  by vm.transactions.collectAsState()
    val categories    by vm.categories.collectAsState()
    val budgets       by vm.budgets.collectAsState()
    val totalIncome   by vm.totalIncome.collectAsState()
    val totalExpense  by vm.totalExpense.collectAsState()
    val isLoading     by vm.isLoading.collectAsState()
    val error         by vm.error.collectAsState()
    val successMsg    by vm.successMessage.collectAsState()

    // Диалог добавления бюджета
    var showAddBudget        by remember { mutableStateOf(false) }
    var budgetCategory       by remember { mutableStateOf<Category?>(null) }
    var budgetLimit          by remember { mutableStateOf("") }
    var budgetPeriod         by remember { mutableStateOf("month") }
    var dropdownExpanded     by remember { mutableStateOf(false) }

    // Toast при успехе/ошибке
    LaunchedEffect(successMsg) { if (successMsg != null) vm.clearSuccess() }
    LaunchedEffect(error) { if (error != null) vm.clearError() }

    // Вычисления для аналитики

    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    val currentYear  = Calendar.getInstance().get(Calendar.YEAR)

    val thisMonthExpenses = remember(transactions) {
        transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            !tx.isIncome
                    && cal.get(Calendar.MONTH) == currentMonth
                    && cal.get(Calendar.YEAR)  == currentYear
        }
    }

    val lastMonthExpenses = remember(transactions) {
        val lastMonth = if (currentMonth == 0) 11 else currentMonth - 1
        val lastYear  = if (currentMonth == 0) currentYear - 1 else currentYear
        transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            !tx.isIncome
                    && cal.get(Calendar.MONTH) == lastMonth
                    && cal.get(Calendar.YEAR)  == lastYear
        }
    }

    val expenseByCategory = remember(thisMonthExpenses) {
        thisMonthExpenses
            .groupBy { it.categoryTitle.ifBlank { "Другое" } }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }

    val totalThisMonth = thisMonthExpenses.sumOf { it.amount }
    val totalLastMonth = lastMonthExpenses.sumOf { it.amount }
    val diffPercent    = if (totalLastMonth > 0)
        ((totalThisMonth - totalLastMonth) / totalLastMonth * 100).toInt() else 0

    // UI

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLightGray)
            .verticalScroll(rememberScrollState())
    ) {
        // Шапка
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Аналитика и бюджеты",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextBlack
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── 1. Сравнение с прошлым месяцем ───────────────────────────────────
        SectionLabel("СРАВНЕНИЕ С ПРОШЛЫМ МЕСЯЦЕМ")

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MonthStat(label = "Прошлый месяц", amount = totalLastMonth, color = TextGray)
                    MonthStat(label = "Этот месяц",    amount = totalThisMonth, color = TextDarkGray)
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = BgLightGray)
                Spacer(Modifier.height(12.dp))

                if (totalLastMonth == 0.0 && totalThisMonth == 0.0) {
                    Text("Нет данных для сравнения", color = TextGray, fontSize = 13.sp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isMore = diffPercent > 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isMore) Color(0xFFE74C3C).copy(0.1f)
                                    else Color(0xFF27AE60).copy(0.1f)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${if (isMore) "▲" else "▼"} ${kotlin.math.abs(diffPercent)}%",
                                color = if (isMore) Color(0xFFE74C3C) else Color(0xFF27AE60),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = when {
                                diffPercent > 0  -> "больше чем в прошлом месяце"
                                diffPercent < 0  -> "меньше чем в прошлом месяце"
                                else             -> "столько же, как в прошлом месяце"
                            },
                            color = TextGray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── 2. Круговая диаграмма расходов ───────────────────────────────────
        SectionLabel("РАСХОДЫ ПО КАТЕГОРИЯМ")

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            if (expenseByCategory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Расходов в этом месяце нет", color = TextGray, fontSize = 14.sp)
                    }
                }
            } else {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Donut диаграмма
                    DonutChart(
                        data   = expenseByCategory.map { it.value.toFloat() },
                        colors = chartColors.take(expenseByCategory.size),
                        total  = totalThisMonth,
                        modifier = Modifier
                            .size(180.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(20.dp))

                    // Легенда
                    expenseByCategory.forEachIndexed { index, (name, amount) ->
                        val color   = chartColors.getOrElse(index) { TextGray }
                        val percent = if (totalThisMonth > 0)
                            (amount / totalThisMonth * 100).toInt() else 0

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(name, modifier = Modifier.weight(1f), fontSize = 13.sp, color = TextDarkGray)
                            Text("$percent%", color = TextGray, fontSize = 12.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "${String.format("%,.0f", amount)} ₽",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextDarkGray
                            )
                        }

                        if (index < expenseByCategory.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp), color = BgLightGray)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        //3. Бюджеты
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabelInline("БЮДЖЕТЫ")
            IconButton(
                onClick = { showAddBudget = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить бюджет", tint = DeepGreen)
            }
        }

        if (budgets.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎯", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Бюджеты не заданы", color = TextGray, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Нажмите + чтобы добавить лимит",
                            color = TextPlaceholder,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    budgets.forEachIndexed { index, budget ->
                        val catTitle = categories.find { it.id == budget.categoryId }?.title ?: "—"
                        BudgetProgressRow(
                            budget    = budget,
                            catTitle  = catTitle,
                            onDelete  = { vm.deleteBudget(budget.id) }
                        )
                        if (index < budgets.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = BgLightGray
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // 4. Итого за всё время
        SectionLabel("ВСЕГО ЗА ВСЁ ВРЕМЯ")

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TotalCard(Modifier.weight(1f), "Доходы",  totalIncome,  Color(0xFF27AE60))
            TotalCard(Modifier.weight(1f), "Расходы", totalExpense, Color(0xFFE74C3C))
        }

        Spacer(Modifier.height(100.dp))
    }

    // Диалог добавления бюджета
    if (showAddBudget) {
        val expenseCategories = categories.filter { it.type == "expense" }

        AlertDialog(
            onDismissRequest = { showAddBudget = false },
            title = { Text("Новый бюджет", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Выбор категории
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, DeepGreen, RoundedCornerShape(12.dp))
                                .clickable { dropdownExpanded = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Категория", fontSize = 11.sp, color = DeepGreen, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = budgetCategory?.title ?: "Выберите категорию",
                                        fontSize = 15.sp,
                                        color = if (budgetCategory != null) TextBlack else TextPlaceholder
                                    )
                                }
                                Text("▾", color = DeepGreen, fontSize = 16.sp)
                            }
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .width(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(White)
                        ) {
                            if (expenseCategories.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Нет категорий", color = TextGray, fontSize = 14.sp) },
                                    onClick = {}
                                )
                            } else {
                                expenseCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                cat.title,
                                                fontSize = 14.sp,
                                                color = if (budgetCategory?.id == cat.id) DeepGreen else TextDarkGray,
                                                fontWeight = if (budgetCategory?.id == cat.id) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = { budgetCategory = cat; dropdownExpanded = false },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (budgetCategory?.id == cat.id) DeepGreen.copy(0.07f) else White
                                            )
                                    )
                                }
                            }
                        }
                    }

                    // Лимит
                    OutlinedTextField(
                        value = budgetLimit,
                        onValueChange = { budgetLimit = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Лимит (₽)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepGreen,
                            focusedLabelColor  = DeepGreen
                        )
                    )

                    // Период
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("month" to "Месяц", "week" to "Неделя").forEach { (value, label) ->
                            val selected = budgetPeriod == value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) DeepGreen.copy(0.1f) else BgLightGray)
                                    .border(
                                        width = if (selected) 1.5.dp else 0.dp,
                                        color = if (selected) DeepGreen else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { budgetPeriod = value }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) DeepGreen else TextGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val limit = budgetLimit.toDoubleOrNull() ?: 0.0
                        val cat   = budgetCategory
                        if (cat != null && limit > 0) {
                            vm.setBudget(cat.id, limit, budgetPeriod)
                            showAddBudget  = false
                            budgetCategory = null
                            budgetLimit    = ""
                            budgetPeriod   = "month"
                        }
                    }
                ) { Text("Добавить", color = DeepGreen, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddBudget  = false
                    budgetCategory = null
                    budgetLimit    = ""
                }) { Text("Отмена", color = TextGray) }
            }
        )
    }
}

//Компоненты

@Composable
private fun DonutChart(
    data: List<Float>,
    colors: List<Color>,
    total: Double,
    modifier: Modifier = Modifier
) {
    val sum = data.sum().takeIf { it > 0f } ?: 1f
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 44.dp.toPx()
            val radius = (size.minDimension - stroke) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            var startAngle = -90f
            data.forEachIndexed { i, value ->
                val sweep = (value / sum) * 360f
                drawArc(
                    color      = colors.getOrElse(i) { Color.Gray },
                    startAngle = startAngle,
                    sweepAngle = sweep - 2f,
                    useCenter  = false,
                    topLeft    = Offset(center.x - radius, center.y - radius),
                    size       = Size(radius * 2f, radius * 2f),
                    style      = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
        // Сумма в центре
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${String.format("%,.0f", total)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = TextDarkGray
            )
            Text("₽", fontSize = 12.sp, color = TextGray)
        }
    }
}

@Composable
private fun BudgetProgressRow(budget: Budget, catTitle: String, onDelete: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Удалить бюджет?") },
            text  = { Text("Лимит для «$catTitle» будет удалён.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDialog = false }) {
                    Text("Удалить", color = Color(0xFFE74C3C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Отмена", color = TextGray) }
            }
        )
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(catTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextDarkGray)
                Text(
                    "${String.format("%,.0f", budget.currentSpent)} / ${String.format("%,.0f", budget.limitAmount)} ₽",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Остаток или превышение
                Text(
                    text = if (budget.isOverLimit) "Превышен!"
                    else "осталось ${String.format("%,.0f", budget.remaining)} ₽",
                    color = if (budget.isOverLimit) Color(0xFFE74C3C) else Color(0xFF27AE60),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = TextPlaceholder,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Полоска прогресса
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BgLightGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(budget.spentFraction.toFloat())
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            budget.spentFraction >= 1.0  -> Color(0xFFE74C3C)
                            budget.spentFraction >= 0.75 -> Color(0xFFF39C12)
                            else                          -> DeepGreen
                        }
                    )
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "${(budget.spentFraction * 100).toInt()}% использовано · ${
                if (budget.period == "month") "в месяц" else "в неделю"
            }",
            color = TextPlaceholder,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextGray,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SectionLabelInline(text: String) {
    Text(
        text = text,
        color = TextGray,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
private fun MonthStat(label: String, amount: Double, color: Color) {
    Column {
        Text(label, color = TextGray, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "${String.format("%,.2f", amount)} ₽",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = color
        )
    }
}

@Composable
private fun TotalCard(modifier: Modifier, label: String, amount: Double, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = TextGray, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "${String.format("%,.2f", amount)} ₽",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = color
            )
        }
    }
}
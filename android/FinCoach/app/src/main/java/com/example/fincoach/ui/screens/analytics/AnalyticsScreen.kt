package com.example.fincoach.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fincoach.ui.FinCoachTopBar
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.LocalAppColors
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.viewmodel.AnalyticsViewModel
import java.util.Calendar

private val chartColors = listOf(
    Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFF45B7D1),
    Color(0xFF96CEB4), Color(0xFFF7DC6F), Color(0xFFBB8FCE),
    Color(0xFFF0A500), Color(0xFF58D68D)
)

private val monthNames = listOf("Янв", "Февр", "Март", "Апр", "Май", "Июн", "Июл", "Авг", "Сент", "Окт", "Нояб", "Дек")

private fun niceMax(value: Double): Double {
    if (value <= 0) return 1000.0
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(value)))
    val normalized = value / magnitude
    return when {
        normalized <= 1 -> magnitude
        normalized <= 2 -> 2 * magnitude
        normalized <= 5 -> 5 * magnitude
        else -> 10 * magnitude
    }
}

private fun formatYLabel(value: Double): String = when {
    value >= 1_000_000 -> "${(value / 1_000_000).toInt()}М"
    value >= 1_000     -> "${(value / 1_000).toInt()}К"
    else               -> "${value.toInt()}"
}

@Composable
fun AnalyticsScreen() {
    val vm: AnalyticsViewModel = viewModel()

    val transactions by vm.transactions.collectAsState()
    val totalIncome  by vm.totalIncome.collectAsState()
    val totalExpense by vm.totalExpense.collectAsState()

    val c = LocalAppColors.current

    var selectedPeriod by remember { mutableStateOf("month") }
    var showExpenses   by remember { mutableStateOf(true) }

    // ── Границы текущего периода ─────────────────────────────────────────────
    val periodStart = remember(selectedPeriod) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            "week"    -> cal.apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
            "month"   -> cal.apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            "quarter" -> cal.apply { add(Calendar.MONTH, -3) }.timeInMillis
            "year"    -> cal.apply {
                set(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            else -> 0L
        }
    }


    // ── Фильтрация транзакций ────────────────────────────────────────────────
    val curExpenses = remember(transactions, periodStart) {
        transactions.filter { !it.isIncome && it.timestamp >= periodStart }
    }
    val curIncome = remember(transactions, periodStart) {
        transactions.filter { it.isIncome && it.timestamp >= periodStart }
    }

    val expenseByCategory = remember(curExpenses) {
        curExpenses.groupBy { it.categoryTitle.ifBlank { "Другое" } }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }
    val incomeByCategory = remember(curIncome) {
        curIncome.groupBy { it.categoryTitle.ifBlank { "Другое" } }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }

    val totalCur    = curExpenses.sumOf { it.amount }
    val totalCurInc = curIncome.sumOf { it.amount }

    // ── Данные для столбчатой диаграммы (последние 6 месяцев) ────────────────
    val monthlyData = remember(transactions) {
        (5 downTo 0).map { monthsAgo ->
            val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -monthsAgo) }
            val m = cal.get(Calendar.MONTH)
            val y = cal.get(Calendar.YEAR)
            val income  = transactions.filter { tx ->
                val tc = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                tx.isIncome && tc.get(Calendar.MONTH) == m && tc.get(Calendar.YEAR) == y
            }.sumOf { it.amount }
            val expense = transactions.filter { tx ->
                val tc = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                !tx.isIncome && tc.get(Calendar.MONTH) == m && tc.get(Calendar.YEAR) == y
            }.sumOf { it.amount }
            Triple(monthNames[m], income, expense)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {

        FinCoachTopBar(title = "Аналитика")

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Переключатель периода ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.cardBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("week" to "Неделя", "month" to "Месяц", "quarter" to "Квартал", "year" to "Год")
                    .forEach { (period, label) ->
                        val selected = selectedPeriod == period
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) DeepGreen.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { selectedPeriod = period }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) DeepGreen else c.textSecondary
                            )
                        }
                    }
            }

            // ── Круговая диаграмма ────────────────────────────────────────────
            SectionLabel("ПО КАТЕГОРИЯМ")

            // Переключатель Расходы / Доходы
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.cardBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(true to "Расходы", false to "Доходы").forEach { (isExpense, label) ->
                    val selected = showExpenses == isExpense
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected && isExpense) Color(0xFFE74C3C).copy(alpha = 0.12f)
                                else if (selected) DeepGreen.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable { showExpenses = isExpense }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                selected && isExpense -> Color(0xFFE74C3C)
                                selected             -> DeepGreen
                                else                 -> c.textSecondary
                            }
                        )
                    }
                }
            }

            val activeByCategory = if (showExpenses) expenseByCategory else incomeByCategory
            val activeTotal      = if (showExpenses) totalCur else totalCurInc
            val activeAccent     = if (showExpenses) Color(0xFFE74C3C) else DeepGreen
            val emptyText        = if (showExpenses) "Расходов за этот период нет" else "Доходов за этот период нет"

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = c.cardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                if (activeByCategory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📊", fontSize = 36.sp); Spacer(Modifier.height(8.dp))
                            Text(emptyText, color = c.textSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    Column(modifier = Modifier.padding(20.dp)) {
                        DonutChart(
                            data        = activeByCategory.map { it.value.toFloat() },
                            colors      = chartColors.take(activeByCategory.size),
                            total       = activeTotal,
                            accentColor = activeAccent,
                            modifier    = Modifier.size(180.dp).align(Alignment.CenterHorizontally)
                        )
                        Spacer(Modifier.height(20.dp))
                        activeByCategory.forEachIndexed { index, (name, amount) ->
                            val color   = chartColors.getOrElse(index) { TextGray }
                            val percent = if (activeTotal > 0) (amount / activeTotal * 100).toInt() else 0
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                Spacer(Modifier.width(10.dp))
                                Text(name, modifier = Modifier.weight(1f), fontSize = 13.sp, color = c.textPrimary)
                                Text("$percent%", color = c.textSecondary, fontSize = 12.sp)
                                Spacer(Modifier.width(12.dp))
                                Text("${String.format("%,.0f", amount)} ₽", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = c.textPrimary)
                            }
                            if (index < activeByCategory.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp), color = c.divider)
                        }
                    }
                }
            }

            // ── Динамика по месяцам ───────────────────────────────────────────
            SectionLabel("ДИНАМИКА ПО МЕСЯЦАМ")
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = c.cardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp)) {
                    MonthlyBarChart(monthlyData = monthlyData)
                }
            }

            // ── Итого ─────────────────────────────────────────────────────────
            SectionLabel("ВСЕГО ЗА ВСЁ ВРЕМЯ")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TotalCard(Modifier.weight(1f), "Доходы",  totalIncome,  Color(0xFF27AE60))
                TotalCard(Modifier.weight(1f), "Расходы", totalExpense, Color(0xFFE74C3C))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MonthlyBarChart(monthlyData: List<Triple<String, Double, Double>>) {
    val c = LocalAppColors.current
    val incomeColor  = DeepGreen
    val expenseColor = Color(0xFFE74C3C)
    val gridColor    = c.divider

    val hasData = monthlyData.any { it.second > 0 || it.third > 0 }
    if (!hasData) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
            Text("Нет данных за последние 6 месяцев", color = c.textSecondary, fontSize = 13.sp)
        }
        return
    }

    val maxValue = monthlyData.maxOf { maxOf(it.second, it.third) }
    val yMax = niceMax(maxValue)
    val yLevels = listOf(1.0, 0.75, 0.5, 0.25, 0.0)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Область графика
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            // Метки по оси Y
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                yLevels.forEach { fraction ->
                    Text(
                        text = formatYLabel(yMax * fraction),
                        fontSize = 9.sp,
                        color = TextGray,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Столбчатый график
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val h = size.height
                val w = size.width
                val n = monthlyData.size
                val slotW = w / n
                val gap = slotW * 0.12f
                val barW = (slotW - gap * 2f) / 2f - 2f
                val radius = CornerRadius(4.dp.toPx())

                // Сетка
                yLevels.forEach { fraction ->
                    val y = h * (1f - fraction).toFloat()
                    drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1.dp.toPx())
                }

                // Столбцы
                monthlyData.forEachIndexed { i, (_, income, expense) ->
                    val slotX = i * slotW + gap

                    val incH = ((income / yMax) * h).toFloat().coerceAtLeast(0f)
                    if (incH > 2f) {
                        drawRoundRect(
                            color = incomeColor,
                            topLeft = Offset(slotX, h - incH),
                            size = Size(barW, incH),
                            cornerRadius = radius
                        )
                    }

                    val expX = slotX + barW + 2f
                    val expH = ((expense / yMax) * h).toFloat().coerceAtLeast(0f)
                    if (expH > 2f) {
                        drawRoundRect(
                            color = expenseColor,
                            topLeft = Offset(expX, h - expH),
                            size = Size(barW, expH),
                            cornerRadius = radius
                        )
                    }
                }
            }
        }

        // Подписи месяцев
        Row(modifier = Modifier.fillMaxWidth().padding(start = 44.dp, top = 4.dp)) {
            monthlyData.forEach { (label, _, _) ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    fontSize = 9.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Легенда
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)).background(DeepGreen))
            Spacer(Modifier.width(4.dp))
            Text("Доходы", fontSize = 11.sp, color = TextGray)
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)).background(Color(0xFFE74C3C)))
            Spacer(Modifier.width(4.dp))
            Text("Расходы", fontSize = 11.sp, color = TextGray)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextGray,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun TotalCard(modifier: Modifier, label: String, amount: Double, color: Color) {
    val c = LocalAppColors.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = c.cardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = c.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                "${String.format("%,.0f", amount)} ₽",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun DonutChart(
    data: List<Float>,
    colors: List<Color>,
    total: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val total_f = data.sum()
            if (total_f == 0f) return@Canvas
            val stroke = size.minDimension * 0.18f
            val radius = (size.minDimension - stroke) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            var startAngle = -90f
            data.forEachIndexed { i, value ->
                val sweep = 360f * value / total_f
                drawArc(
                    color = colors.getOrElse(i) { Color.Gray },
                    startAngle = startAngle,
                    sweepAngle = sweep - 1.5f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%,.0f", total),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text("₽", fontSize = 12.sp, color = c.textSecondary)
        }
    }
}

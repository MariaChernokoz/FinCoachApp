package com.example.fincoach.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fincoach.data.model.Budget
import com.example.fincoach.data.model.Category
import com.example.fincoach.data.model.Goal
import com.example.fincoach.ui.FinCoachTopBar
import com.example.fincoach.ui.theme.BrightGreen
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.LocalAppColors
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.TextPlaceholder
import com.example.fincoach.viewmodel.AnalyticsViewModel
import com.example.fincoach.viewmodel.GoalsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoalsScreen() {
    val vm: GoalsViewModel = viewModel()
    val analyticsVm: AnalyticsViewModel = viewModel()
    val context = LocalContext.current

    val activeGoals    by vm.activeGoals.collectAsState()
    val completedGoals by vm.completedGoals.collectAsState()
    val error          by vm.error.collectAsState()
    val successMsg     by vm.successMessage.collectAsState()

    val budgets    by analyticsVm.budgets.collectAsState()
    val categories by analyticsVm.categories.collectAsState()

    val c = LocalAppColors.current

    var showAddGoalDialog   by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var budgetCategory      by remember { mutableStateOf<Category?>(null) }
    var budgetLimit         by remember { mutableStateOf("") }
    var budgetPeriod        by remember { mutableStateOf("month") }
    var dropdownExpanded    by remember { mutableStateOf(false) }

    LaunchedEffect(successMsg) {
        if (successMsg != null) {
            android.widget.Toast.makeText(context, successMsg, android.widget.Toast.LENGTH_SHORT).show()
            vm.clearSuccess()
        }
    }
    LaunchedEffect(error) {
        if (error != null) {
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
            vm.clearError()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {

        FinCoachTopBar(title = "Цели и бюджеты")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Секция: Цели ──────────────────────────────────────────────
            item(key = "goals_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ЦЕЛИ",
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepGreen.copy(0.08f))
                            .clickable { showAddGoalDialog = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Add, null, tint = DeepGreen, modifier = Modifier.size(14.dp))
                            Text("Добавить", fontSize = 12.sp, color = DeepGreen, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            if (activeGoals.isEmpty() && completedGoals.isEmpty()) {
                item(key = "goals_empty") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = c.cardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎯", fontSize = 36.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Целей пока нет", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = c.textPrimary)
                                Spacer(Modifier.height(4.dp))
                                TextButton(onClick = { showAddGoalDialog = true }) {
                                    Text("Создать первую цель", color = DeepGreen, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            } else {
                if (activeGoals.isNotEmpty()) {
                    item(key = "active_label") {
                        Text("АКТИВНЫЕ", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                    }
                    items(activeGoals, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            onDelete = { vm.deleteGoal(goal.id) },
                            onAddSaving = { amount -> vm.addSaving(goal, amount) },
                            onWithdraw = { amount -> vm.withdrawSaving(goal, amount) }
                        )
                    }
                }
                if (completedGoals.isNotEmpty()) {
                    item(key = "completed_label") {
                        Spacer(Modifier.height(2.dp))
                        Text("ДОСТИГНУТО 🎉", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                    }
                    items(completedGoals, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            onDelete = { vm.deleteGoal(goal.id) },
                            onAddSaving = { amount -> vm.addSaving(goal, amount) },
                            isCompleted = true
                        )
                    }
                }
            }

            // ── Секция: Бюджеты ───────────────────────────────────────────
            item(key = "budgets_header") {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "БЮДЖЕТЫ",
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepGreen.copy(0.08f))
                            .clickable { showAddBudgetDialog = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Add, null, tint = DeepGreen, modifier = Modifier.size(14.dp))
                            Text("Добавить", fontSize = 12.sp, color = DeepGreen, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            if (budgets.isEmpty()) {
                item(key = "budgets_empty") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = c.cardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💸", fontSize = 36.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Бюджеты не заданы", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = c.textPrimary)
                                Spacer(Modifier.height(4.dp))
                                TextButton(onClick = { showAddBudgetDialog = true }) {
                                    Text("Задать первый бюджет", color = DeepGreen, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            } else {
                item(key = "budgets_list") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = c.cardBg),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            budgets.forEachIndexed { index, budget ->
                                val catTitle = categories.find { it.id == budget.categoryId }?.title ?: "—"
                                BudgetProgressRow(
                                    budget = budget,
                                    catTitle = catTitle,
                                    onDelete = { analyticsVm.deleteBudget(budget.id) }
                                )
                                if (index < budgets.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = c.divider)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "bottom_spacer") { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Диалог добавления цели
    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { title, amount, deadline ->
                vm.addGoal(title, amount, deadline)
                showAddGoalDialog = false
            }
        )
    }

    // Диалог добавления бюджета
    if (showAddBudgetDialog) {
        val expenseCategories = categories.filter { it.type == "expense" }
        AlertDialog(
            onDismissRequest = { showAddBudgetDialog = false; budgetCategory = null; budgetLimit = "" },
            title = { Text("Новый бюджет", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, DeepGreen, RoundedCornerShape(12.dp))
                                .clickable { dropdownExpanded = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Категория", fontSize = 11.sp, color = DeepGreen, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        budgetCategory?.title ?: "Выберите категорию",
                                        fontSize = 15.sp,
                                        color = if (budgetCategory != null) c.textPrimary else TextPlaceholder
                                    )
                                }
                                Text("▾", color = DeepGreen, fontSize = 16.sp)
                            }
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.width(220.dp).clip(RoundedCornerShape(16.dp)).background(c.cardBg)
                        ) {
                            if (expenseCategories.isEmpty()) {
                                DropdownMenuItem(text = { Text("Нет категорий", color = c.textSecondary, fontSize = 14.sp) }, onClick = {})
                            } else {
                                expenseCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                cat.title,
                                                fontSize = 14.sp,
                                                color = c.textPrimary
                                            )
                                        },
                                        onClick = {
                                            budgetCategory = cat
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = budgetLimit,
                        onValueChange = { budgetLimit = it },
                        label = { Text("Лимит, ₽") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepGreen,
                            focusedLabelColor = DeepGreen
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cat = budgetCategory
                        val limit = budgetLimit.toDoubleOrNull()
                        if (cat != null && limit != null && limit > 0) {
                            analyticsVm.setBudget(cat.id, limit, budgetPeriod)
                            showAddBudgetDialog = false
                            budgetCategory = null
                            budgetLimit = ""
                        }
                    }
                ) { Text("Добавить", color = DeepGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showAddBudgetDialog = false; budgetCategory = null; budgetLimit = "" }) {
                    Text("Отмена", color = TextGray)
                }
            }
        )
    }
}

// ── GoalCard ──────────────────────────────────────────────────────────────────
@Composable
private fun GoalCard(
    goal: Goal,
    onDelete: () -> Unit,
    onAddSaving: (Double) -> Unit,
    onWithdraw: ((Double) -> Unit)? = null,
    isCompleted: Boolean = false
) {
    val c = LocalAppColors.current
    var showAmountDialog by remember { mutableStateOf(false) }
    var isWithdrawMode   by remember { mutableStateOf(false) }

    val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).coerceIn(0.0, 1.0) else 0.0
    val remaining = goal.targetAmount - goal.savedAmount

    val deadlineText = goal.deadline?.let {
        val daysLeft = ((it - System.currentTimeMillis()) / 86_400_000).toInt()
        val formatted = SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date(it))
        if (daysLeft >= 0) "до $formatted · $daysLeft дн." else "до $formatted · просрочено"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) DeepGreen.copy(0.06f) else c.cardBg
        ),
        elevation = CardDefaults.cardElevation(if (isCompleted) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(if (isCompleted) DeepGreen.copy(0.15f) else DeepGreen.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isCompleted) "✅" else "🎯", fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(goal.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = c.textPrimary)
                    if (deadlineText != null) {
                        Text(deadlineText, fontSize = 12.sp, color = TextGray)
                    }
                }
                if (!isCompleted) {
                    IconButton(onClick = { isWithdrawMode = false; showAmountDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.AddCircle, null, tint = DeepGreen, modifier = Modifier.size(22.dp))
                    }
                    if (onWithdraw != null) {
                        IconButton(onClick = { isWithdrawMode = true; showAmountDialog = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.RemoveCircle, null, tint = Color(0xFFE74C3C), modifier = Modifier.size(22.dp))
                        }
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, tint = TextGray, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Накоплено", fontSize = 11.sp, color = TextGray)
                    Text("${String.format("%,.0f", goal.savedAmount)} ₽", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Цель", fontSize = 11.sp, color = TextGray)
                    Text("${String.format("%,.0f", goal.targetAmount)} ₽", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.textPrimary)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Прогресс-бар
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(DeepGreen.copy(0.12f))) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress.toFloat()).clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(BrightGreen, DeepGreen))))
            }

            Spacer(Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepGreen)
                if (isCompleted) {
                    Text("Цель достигнута! 🎉", fontSize = 12.sp, color = DeepGreen, fontWeight = FontWeight.Medium)
                } else {
                    Text("осталось ${String.format("%,.0f", remaining.coerceAtLeast(0.0))} ₽", fontSize = 12.sp, color = TextGray)
                }
            }
        }
    }

    if (showAmountDialog) {
        var inputAmount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAmountDialog = false; inputAmount = "" },
            title = { Text(if (isWithdrawMode) "Снять средства" else "Пополнить цель", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = inputAmount,
                    onValueChange = { inputAmount = it },
                    label = { Text("Сумма, ₽") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepGreen, focusedLabelColor = DeepGreen)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = inputAmount.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        if (isWithdrawMode) onWithdraw?.invoke(amount) else onAddSaving(amount)
                        showAmountDialog = false; inputAmount = ""
                    }
                }) { Text(if (isWithdrawMode) "Снять" else "Пополнить", color = DeepGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showAmountDialog = false; inputAmount = "" }) { Text("Отмена", color = TextGray) }
            }
        )
    }
}

// ── BudgetProgressRow ─────────────────────────────────────────────────────────
@Composable
private fun BudgetProgressRow(budget: Budget, catTitle: String, onDelete: () -> Unit) {
    val c = LocalAppColors.current
    val spent    = budget.currentSpent
    val limit    = budget.limitAmount
    val progress = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0) else 0.0
    val exceeded = spent > limit
    val remaining = limit - spent

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(catTitle, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = c.textPrimary)
                if (exceeded) {
                    Text("Превышен!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE74C3C))
                } else {
                    Text("осталось ${String.format("%,.0f", remaining)} ₽", fontSize = 12.sp, color = DeepGreen)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${String.format("%,.0f", spent)} / ${String.format("%,.0f", limit)} ₽",
                fontSize = 12.sp, color = c.textSecondary
            )
            Spacer(Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.divider)) {
                Box(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(progress.toFloat()).clip(RoundedCornerShape(3.dp))
                        .background(if (exceeded) Color(0xFFE74C3C) else DeepGreen)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text("${(progress * 100).toInt()}% · в месяц", fontSize = 11.sp, color = TextGray)
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, null, tint = TextGray, modifier = Modifier.size(16.dp))
        }
    }
}

// ── AddGoalDialog ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (String, Double, Long?) -> Unit) {
    val c = LocalAppColors.current
    var title       by remember { mutableStateOf("") }
    var amount      by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val selectedDeadline = datePickerState.selectedDateMillis

    val deadlineLabel = selectedDeadline?.let {
        SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date(it))
    } ?: "Не задан"

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("ОК", color = DeepGreen) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Отмена", color = TextGray) } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая цель", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepGreen, focusedLabelColor = DeepGreen)
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Целевая сумма, ₽") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepGreen, focusedLabelColor = DeepGreen)
                )
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, DeepGreen, RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Дедлайн (опционально)", fontSize = 11.sp, color = DeepGreen, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text(deadlineLabel, fontSize = 15.sp, color = if (selectedDeadline != null) c.textPrimary else TextGray)
                        }
                        Icon(Icons.Default.CalendarMonth, null, tint = DeepGreen, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.toDoubleOrNull()
                if (title.isNotBlank() && a != null && a > 0) onConfirm(title, a, selectedDeadline)
            }) { Text("Создать", color = DeepGreen) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = TextGray) } }
    )
}
                                               
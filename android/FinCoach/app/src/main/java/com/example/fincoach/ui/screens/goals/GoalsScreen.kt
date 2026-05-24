package com.example.fincoach.ui.screens.goals

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.fincoach.data.model.Goal
import com.example.fincoach.ui.FinCoachTopBar
import com.example.fincoach.ui.theme.BgLightGray
import com.example.fincoach.ui.theme.BrightGreen
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.TextBlack
import com.example.fincoach.ui.theme.TextDarkGray
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.TextPlaceholder
import com.example.fincoach.ui.theme.White
import com.example.fincoach.viewmodel.GoalsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen() {
    val vm: GoalsViewModel = viewModel()
    val context = LocalContext.current

    val activeGoals    by vm.activeGoals.collectAsState()
    val completedGoals by vm.completedGoals.collectAsState()
    val isLoading      by vm.isLoading.collectAsState()
    val error          by vm.error.collectAsState()
    val successMsg     by vm.successMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(successMsg) { if (successMsg != null) { android.widget.Toast.makeText(context, successMsg, android.widget.Toast.LENGTH_SHORT).show(); vm.clearSuccess() } }
    LaunchedEffect(error) { if (error != null) { android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show(); vm.clearError() } }

    Column(modifier = Modifier.fillMaxSize().background(BgLightGray)) {

        FinCoachTopBar(
            title = "Мои цели",
            actionIcon = Icons.Default.Add,
            onActionClick = { showAddDialog = true }
        )

        if (activeGoals.isEmpty() && completedGoals.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🎯", fontSize = 64.sp)
                    Text("Целей пока нет", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDarkGray)
                    Text("Создайте первую цель-накопление", color = TextGray, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(BrightGreen, DeepGreen))).clickable { showAddDialog = true }.padding(horizontal = 28.dp, vertical = 14.dp)
                    ) { Text("Создать цель", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeGoals.isNotEmpty()) {
                    item { Text("АКТИВНЫЕ", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) }
                    items(activeGoals, key = { it.id }) { goal ->
                        GoalCard(goal = goal, onDelete = { vm.deleteGoal(goal.id) }, onAddSaving = { amount -> vm.addSaving(goal, amount) })
                    }
                }
                if (completedGoals.isNotEmpty()) {
                    item { Spacer(Modifier.height(4.dp)); Text("ДОСТИГНУТО 🎉", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) }
                    items(completedGoals, key = { it.id }) { goal ->
                        GoalCard(goal = goal, onDelete = { vm.deleteGoal(goal.id) }, onAddSaving = { amount -> vm.addSaving(goal, amount) }, isCompleted = true)
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(onDismiss = { showAddDialog = false }, onConfirm = { title, amount, deadline -> vm.addGoal(title, amount, deadline); showAddDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalCard(goal: Goal, onDelete: () -> Unit, onAddSaving: (Double) -> Unit, isCompleted: Boolean = false) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSavingDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text("Удалить цель?") }, text = { Text("«${goal.title}» будет удалена.") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Удалить", color = Color(0xFFE74C3C)) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена", color = TextGray) } })
    }
    if (showSavingDialog) {
        AddSavingDialog(goalTitle = goal.title, remaining = goal.remaining, onDismiss = { showSavingDialog = false }, onConfirm = { amount -> onAddSaving(amount); showSavingDialog = false })
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (isCompleted) Color(0xFFF0FFF4) else White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(if (isCompleted) Color(0xFF27AE60).copy(0.15f) else DeepGreen.copy(0.1f)), contentAlignment = Alignment.Center) {
                    Text(if (isCompleted) "✅" else "🎯", fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(goal.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextBlack)
                    goal.deadline?.let { deadline ->
                        val dateStr = SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date(deadline))
                        val daysLeft = ((deadline - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                        Text(text = if (daysLeft > 0) "до $dateStr · $daysLeft дн." else "Срок истёк", color = if (daysLeft < 7 && daysLeft >= 0) Color(0xFFF39C12) else if (daysLeft < 0) Color(0xFFE74C3C) else TextGray, fontSize = 12.sp)
                    }
                }
                if (!isCompleted) {
                    IconButton(onClick = { showSavingDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.AddCircle, null, tint = DeepGreen, modifier = Modifier.size(22.dp))
                    }
                }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, tint = TextPlaceholder, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Накоплено", color = TextGray, fontSize = 11.sp)
                    Text("${String.format("%,.0f", goal.savedAmount)} ₽", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isCompleted) Color(0xFF27AE60) else DeepGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Цель", color = TextGray, fontSize = 11.sp)
                    Text("${String.format("%,.0f", goal.targetAmount)} ₽", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDarkGray)
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(BgLightGray)) {
                if (isCompleted) {
                    Box(modifier = Modifier.fillMaxWidth(goal.progress.toFloat()).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Color(0xFF27AE60)))
                } else {
                    Box(modifier = Modifier.fillMaxWidth(goal.progress.toFloat()).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(listOf(BrightGreen, DeepGreen))))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(goal.progress * 100).toInt()}%", color = if (isCompleted) Color(0xFF27AE60) else DeepGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (!isCompleted) Text("осталось ${String.format("%,.0f", goal.remaining)} ₽", color = TextGray, fontSize = 12.sp)
                else Text("Цель достигнута! 🎉", color = Color(0xFF27AE60), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (String, Double, Long?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    val dateLabel = selectedDate?.let { SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date(it)) } ?: "Без срока"

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { selectedDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("ОК", color = DeepGreen) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Без срока", color = TextGray) } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая цель", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название цели") }, placeholder = { Text("Например: Отпуск, MacBook...", color = TextPlaceholder) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepGreen, focusedLabelColor = DeepGreen))
                OutlinedTextField(value = amountText, onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Сумма (₽)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepGreen, focusedLabelColor = DeepGreen))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, if (selectedDate != null) DeepGreen else Color.Gray.copy(0.4f), RoundedCornerShape(12.dp)).clickable { showDatePicker = true }.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, tint = if (selectedDate != null) DeepGreen else TextGray, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Срок", fontSize = 11.sp, color = if (selectedDate != null) DeepGreen else TextGray)
                            Text(dateLabel, fontSize = 14.sp, color = if (selectedDate != null) TextBlack else TextPlaceholder)
                        }
                    }
                }
                AnimatedVisibility(visible = selectedDate != null) {
                    TextButton(onClick = { selectedDate = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Убрать срок", color = Color(0xFFE74C3C), fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { val amount = amountText.toDoubleOrNull() ?: 0.0; if (title.isNotBlank() && amount > 0) onConfirm(title, amount, selectedDate) }) { Text("Создать", color = DeepGreen, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = TextGray) } }
    )
}

@Composable
private fun AddSavingDialog(goalTitle: String, remaining: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пополнить цель", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("«$goalTitle»", color = TextGray, fontSize = 13.sp)
                Text("Осталось: ${String.format("%,.0f", remaining)} ₽", color = DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = amountText, onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Сумма пополнения (₽)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DeepGreen, focusedLabelColor = DeepGreen))
            }
        },
        confirmButton = { TextButton(onClick = { val amount = amountText.toDoubleOrNull() ?: 0.0; if (amount > 0) onConfirm(amount) }) { Text("Добавить", color = DeepGreen, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = TextGray) } }
    )
}
package com.example.fincoach.ui.screens.transactions

// ПРОВЕРЬ ЭТИ ИМПОРТЫ (должны совпадать с твоей структурой папок)

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.fincoach.data.model.Category
import com.example.fincoach.ui.theme.BrightGreen
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.LocalAppColors
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.White
import com.example.fincoach.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val vm: TransactionViewModel = viewModel()

    // Состояния из ViewModel
    val categories by vm.categories.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val successMsg by vm.successMessage.collectAsState()

    // Состояния формы
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    // Дата
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateLabel = remember(selectedDate) {
        SimpleDateFormat("dd MMMM yyyy", Locale("ru")).format(Date(selectedDate))
    }

    // Слушатели событий
    LaunchedEffect(successMsg) {
        if (successMsg != null) {
            android.widget.Toast.makeText(context, successMsg, android.widget.Toast.LENGTH_SHORT).show()
            vm.clearSuccess()
            onSuccess()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            vm.clearError()
        }
    }

    // Фильтр категорий по типу (доход/расход)
    val filteredCategories = categories.filter {
        it.type == if (isIncome) "income" else "expense"
    }

    LaunchedEffect(isIncome) { selectedCategory = null }

    val c = LocalAppColors.current
    val buttonGradient = Brush.verticalGradient(listOf(BrightGreen, DeepGreen))

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Новая операция", color = White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepGreen
                )
            )
        },
        // Закрепляем кнопку в самом низу экрана вне зоны прокрутки
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.bg)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        vm.addTransaction(title, amountText.toDoubleOrNull() ?: 0.0, selectedCategory?.id ?: "", selectedCategory?.title ?: "", isIncome, selectedDate)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepGreen,
                        disabledContainerColor = DeepGreen.copy(alpha = 0.5f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("СОХРАНИТЬ", fontWeight = FontWeight.Bold, color = White, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(c.bg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Тип операции
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = c.cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(c.bg, RoundedCornerShape(16.dp))
                ) {
                    listOf(false to "Расход", true to "Доход").forEach { (income, label) ->
                        val selected = isIncome == income
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) c.cardBg else Color.Transparent)
                                .clickable { isIncome = income },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) (if (income) DeepGreen else Color(0xFFE74C3C)) else TextGray
                            )
                        }
                    }
                }
            }

            // Выбор даты
            Card(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = c.cardBg)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarMonth, null, tint = DeepGreen)
                    Spacer(Modifier.width(12.dp))
                    Text(dateLabel, fontWeight = FontWeight.Bold, color = c.textPrimary)
                }
            }

            // Поле суммы
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = c.cardBg)) {
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Сумма", color = c.textSecondary) },
                    prefix = { Text("₽ ", fontWeight = FontWeight.Bold, color = c.textPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = c.cardBg,
                        unfocusedContainerColor = c.cardBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = c.textPrimary,
                        unfocusedTextColor = c.textPrimary
                    )
                )
            }

            // Поле описания
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = c.cardBg)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Описание", color = c.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = c.cardBg,
                        unfocusedContainerColor = c.cardBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = c.textPrimary,
                        unfocusedTextColor = c.textPrimary
                    )
                )
            }

            // Категории
            Text("Категория", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            filteredCategories.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { cat ->
                        val isSelected = selectedCategory?.id == cat.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) DeepGreen.copy(0.1f) else c.cardBg)
                                .border(1.dp, if (isSelected) DeepGreen else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = cat }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat.title, fontSize = 12.sp, color = if (isSelected) DeepGreen else c.textPrimary)
                        }
                    }
                    if (row.size < 3) Spacer(Modifier.weight((3 - row.size).toFloat()))
                }
            }
        }
    }

    // Календарь
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: selectedDate
                    showDatePicker = false
                }) { Text("ОК", color = DeepGreen, fontWeight = FontWeight.Bold) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
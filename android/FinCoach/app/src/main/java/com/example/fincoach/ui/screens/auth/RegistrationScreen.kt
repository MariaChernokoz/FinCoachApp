package com.example.fincoach

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fincoach.ui.screens.auth.AuthViewModel
import com.example.fincoach.ui.theme.BgLightGray
import com.example.fincoach.ui.theme.BorderGray
import com.example.fincoach.ui.theme.BrightGreen
import com.example.fincoach.ui.theme.DeepGreen
import com.example.fincoach.ui.theme.TextGray
import com.example.fincoach.ui.theme.TextPlaceholder
import com.example.fincoach.ui.theme.White

@Composable
fun RegistrationScreen(onSuccess: () -> Unit) {
    // 1. Системные переменные
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    val isAuthSuccess by authViewModel.isAuthSuccess.collectAsState()

// Этот блок сработает автоматически, когда флаг в ViewModel станет true
    LaunchedEffect(isAuthSuccess) {
        if (isAuthSuccess) {
            onSuccess()
            authViewModel.resetAuthStatus() // Сбрасываем флаг для следующего раза
        }
    }

    val errorMessage by authViewModel.errorMessage.collectAsState()

// Слушаем ошибки - используем уже существующий context
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            authViewModel.clearError()
        }
    }

    // Настройка клиента Google
    val gso = remember {
        com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("909169891407-4vkc6ap9hgtusg5ou83o1ovqh2ltjmig.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember {
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
    }

    // Лаунчер для выбора аккаунта Google
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            account.idToken?.let { token ->
                authViewModel.signInWithGoogle(token)
            }
        } catch (e: Exception) {
            // Ошибка или отмена входа
        }
    }

    // 2. Состояние полей ввода
    var selectedTab by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Дизайн (градиенты)
    val backgroundGradient = Brush.verticalGradient(colors = listOf(BrightGreen.copy(alpha = 0.8f), DeepGreen))
    val buttonGradient = Brush.verticalGradient(colors = listOf(BrightGreen, DeepGreen))

    // 3. Верстка интерфейса
    Box(
        modifier = Modifier.fillMaxSize().background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Welcome to FinCoach AI!",
                style = MaterialTheme.typography.headlineMedium.copy(color = White, fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text = "Your personal finance coach",
                style = MaterialTheme.typography.titleMedium.copy(color = White.copy(alpha = 0.9f))
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Иконка доллара
            Card(
                modifier = Modifier.size(110.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("$", style = TextStyle(fontSize = 68.sp, fontWeight = FontWeight.Black, color = DeepGreen))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Форма ввода
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(32.dp),
                color = White,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CustomSegmentedControl(selectedTab) { selectedTab = it }

                    Spacer(modifier = Modifier.height(28.dp))

                    if (selectedTab == 1) {
                        FinCoachTextField(name, { name = it }, "Имя", Icons.Default.Person)
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    FinCoachTextField(email, { email = it }, "Email", Icons.Default.Email)
                    Spacer(modifier = Modifier.height(14.dp))
                    FinCoachTextField(password, { password = it }, "Пароль", Icons.Default.Lock, true)

                    if (selectedTab == 0) {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            androidx.compose.material3.Text(
                                text = "Забыли пароль?",
                                modifier = Modifier
                                    .align(androidx.compose.ui.Alignment.CenterEnd)
                                    .clickable { /* TODO: Логика восстановления */ },
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    color = TextGray,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Кнопка Вход / Регистрация
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(buttonGradient)
                            .clickable {
                                if (selectedTab == 0) {
                                    authViewModel.loginUser(email, password)
                                } else {
                                    authViewModel.registerUser(email, password, name)
                                }

                                // ВАЖНО: Вызываем переход.
                                // Позже мы обернем это в проверку успешного входа из Firebase,
                                // но для теста перехода вызываем сразу.
                                //onSuccess()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTab == 0) "ВОЙТИ" else "ЗАРЕГИСТРИРОВАТЬСЯ",
                            color = White,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("или", color = TextPlaceholder, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    // Кнопка GOOGLE
                    SocialButton(
                        text = "Войти через Google",
                        iconResId = R.drawable.ic_google,
                        backgroundColor = Color.White,
                        textColor = Color.Black,
                        showBorder = true,
                        onClick = {
                            // Просто выводим сообщение в логи вместо запуска окна
                            println("Google Login clicked: заглушка")
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

//ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ (Ниже основного экрана)

@Composable
fun FinCoachTextField(
    value: String, onValueChange: (String) -> Unit, label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, isPassword: Boolean = false
) {
    TextField(
        value = value, onValueChange = onValueChange,
        placeholder = { Text(text = label, color = TextPlaceholder) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = TextGray) },
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = BgLightGray, unfocusedContainerColor = BgLightGray,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun SocialButton(text: String, iconResId: Int, backgroundColor: Color, textColor: Color, showBorder: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp).then(
            if (showBorder) Modifier.border(1.dp, BorderGray, RoundedCornerShape(16.dp)) else Modifier
        ),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = iconResId), contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, color = textColor, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CustomSegmentedControl(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(48.dp).background(BgLightGray, RoundedCornerShape(24.dp)).padding(2.dp)) {
        Row(modifier = Modifier.fillMaxSize()) {
            listOf("Вход", "Регистрация").forEachIndexed { index, title ->
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(22.dp))
                        .background(if (selectedIndex == index) White else Color.Transparent)
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = title, fontWeight = FontWeight.Bold, color = if (selectedIndex == index) DeepGreen else TextGray)
                }
            }
        }
    }
}
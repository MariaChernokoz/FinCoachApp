import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fincoach.ui.theme.*
import com.example.fincoach.R

@Composable
fun RegistrationScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Градиент фона (от светлого центра к чуть более темным краям)
    val backgroundGradient = remember {
        Brush.verticalGradient(colors = listOf(BrightGreen.copy(alpha = 0.8f), DeepGreen))
    }

    // Градиент для кнопки
    val buttonGradient = Brush.verticalGradient(
        colors = listOf(BrightGreen, DeepGreen)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient) // Вот он, живой фон!
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Текстовый блок
            Text(
                text = "Welcome to FinCoach AI!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = White,
                    fontWeight = FontWeight.ExtraBold,
                    shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black.copy(alpha = 0.1f), blurRadius = 8f)
                )
            )
            Text(
                text = "Your personal finance coach",
                style = MaterialTheme.typography.titleMedium.copy(color = White.copy(alpha = 0.9f))
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 1. Тот самый белый квадрат (Floating Box)
            Card(
                modifier = Modifier.size(110.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "$",
                        style = TextStyle(fontSize = 68.sp, fontWeight = FontWeight.Black, color = DeepGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 2. Белая карточка с контентом
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(32.dp),
                color = White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ПЕРЕКЛЮЧАТЕЛЬ (Segmented Control)
                    // (Код переключателя оставляем тот же, он у нас уже отличный!)
                    CustomSegmentedControl(selectedTab) { selectedTab = it }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Поля ввода
                    if (selectedTab == 1) {
                        FinCoachTextField(name, { name = it }, "Имя", Icons.Default.Person)
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    FinCoachTextField(email, { email = it }, "Email", Icons.Default.Email)
                    Spacer(modifier = Modifier.height(14.dp))
                    FinCoachTextField(password, { password = it }, "Пароль", Icons.Default.Lock, true)

                    if (selectedTab == 0) {
                        Text(
                            "Забыли пароль?",
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clickable { /* TODO */ },
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextGray)
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // ГЛАВНАЯ КНОПКА
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(buttonGradient)
                            .clickable { /* Action */ },
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

                    // Google Button
                    SocialButton(
                        text = "Войти через Google",
                        iconResId = R.drawable.ic_google,
                        backgroundColor = NearBlack
                    ) { }
                }
            }

            Spacer(modifier = Modifier.height(50.dp)) // Чтобы карточка "дышала" снизу
        }
    }
}

@Composable
fun FinCoachTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = label,
                color = TextPlaceholder // Используем #CED0D1
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextGray // Используем #7C7C7C
            )
        },
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = BgLightGray, // #EEEEEF
            unfocusedContainerColor = BgLightGray,
            disabledContainerColor = BgLightGray,
            focusedTextColor = TextBlack, // #000000
            unfocusedTextColor = TextBlack,
            cursorColor = DeepGreen,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun SocialButton(
    text: String,
    iconResId: Int,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun CustomSegmentedControl(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(BgLightGray, RoundedCornerShape(24.dp)) // Наш серый фон #EEEEEF
            .padding(2.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Кнопка Вход
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selectedIndex == 0) White else Color.Transparent)
                    .clickable { onTabSelected(0) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Вход",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIndex == 0) DeepGreen else TextGray,
                        fontSize = 15.sp
                    )
                )
            }
            // Кнопка Регистрация
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selectedIndex == 1) White else Color.Transparent)
                    .clickable { onTabSelected(1) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Регистрация",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIndex == 1) DeepGreen else TextGray,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}
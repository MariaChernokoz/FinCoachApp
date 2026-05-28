package com.example.fincoach.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fincoach.data.repository.UserRepository
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val userRepository = UserRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isAuthSuccess = MutableStateFlow(false)
    val isAuthSuccess = _isAuthSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // true = только что зарегистрировались, письмо отправлено — показать диалог
    private val _showVerificationDialog = MutableStateFlow(false)
    val showVerificationDialog = _showVerificationDialog.asStateFlow()

    fun dismissVerificationDialog() { _showVerificationDialog.value = false }

    fun clearError() { _errorMessage.value = null }

    // Регистрация

    fun registerUser(email: String, pass: String, name: String) {
        if (email.isEmpty() || pass.isEmpty() || name.isEmpty()) {
            _errorMessage.value = "Пожалуйста, заполните все поля"
            return
        }
        if (pass.length < 6) {
            _errorMessage.value = "Пароль должен быть не менее 6 символов"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Создаём аккаунт в Firebase Auth
                val result = auth.createUserWithEmailAndPassword(email, pass).await()

                // 2. Обновляем displayName
                val profileUpdates = userProfileChangeRequest { displayName = name }
                result.user?.updateProfile(profileUpdates)?.await()

                // 3. Создаём документ в коллекции users в Firestore
                userRepository.createUserProfile(name = name, email = email)

                // 4. Отправляем письмо с подтверждением email
                runCatching { result.user?.sendEmailVerification()?.await() }
                _showVerificationDialog.value = true
                // Не переходим сразу — ждём пока пользователь закроет диалог
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка регистрации: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Вход

    fun loginUser(email: String, pass: String) {
        if (email.isEmpty() || pass.isEmpty()) {
            _errorMessage.value = "Введите email и пароль"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Авторизуемся в Firebase Auth
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val user = result.user

                // 2. ПРОВЕРКА: существует ли профиль в Firestore?
                // Это спасет ситуацию, если ты удалила базу вручную.
                val profile = userRepository.getUserProfile()
                if (profile == null && user != null) {
                    userRepository.createUserProfile(
                        name = user.displayName ?: "Пользователь",
                        email = user.email ?: email
                    )
                }

                _isAuthSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Неверный email или пароль"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isEmpty()) {
            _errorMessage.value = "Введите email"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.sendPasswordResetEmail(email).await()

                // Передаем сообщение об успехе в errorMessage, чтобы экран показал Toast
                _errorMessage.value = "Письмо для восстановления отправлено на $email"
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Ошибка восстановления пароля"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Google Sign-In

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()

                // Если пользователь новый — создаём профиль в Firestore
                val isNewUser = result.additionalUserInfo?.isNewUser == true
                if (isNewUser) {
                    val user = result.user
                    userRepository.createUserProfile(
                        name  = user?.displayName ?: "Пользователь",
                        email = user?.email ?: ""
                    )
                }

                _isAuthSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка входа через Google"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Вспомогательные

    fun logout() {
        auth.signOut()
        resetAuthStatus()
    }

    fun resetAuthStatus() {
        _isAuthSuccess.value = false
    }

    fun getCurrentUserEmail(): String = auth.currentUser?.email ?: "Email не указан"
    fun getCurrentUserName(): String  = auth.currentUser?.displayName ?: "Пользователь"
}
package com.example.fincoach.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isAuthSuccess = MutableStateFlow(false)
    val isAuthSuccess = _isAuthSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Функция для очистки ошибки
    fun clearError() { _errorMessage.value = null }

    fun registerUser(email: String, pass: String, name: String) {
        // Проверка на пустые поля
        if (email.isEmpty() || pass.isEmpty() || name.isEmpty()) {
            _errorMessage.value = "Пожалуйста, заполните все поля"
            return
        }
        // Проверка длины пароля
        if (pass.length < 6) {
            _errorMessage.value = "Пароль должен быть не менее 6 символов"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val profileUpdates = userProfileChangeRequest { displayName = name }
                result.user?.updateProfile(profileUpdates)?.await()
                _isAuthSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка регистрации: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loginUser(email: String, pass: String) {
        if (email.isEmpty() || pass.isEmpty()) {
            _errorMessage.value = "Введите email и пароль"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                _isAuthSuccess.value = true
            } catch (e: Exception) {
                // Обработка конкретных ошибок входа
                _errorMessage.value = "Неверный email или пароль"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetAuthStatus() {
        _isAuthSuccess.value = false
    }

    fun getCurrentUserEmail(): String {
        return auth.currentUser?.email ?: "Email не указан"
    }

    fun getCurrentUserName(): String {
        // Если имени нет, вернем "Пользователь"
        return auth.currentUser?.displayName ?: "Пользователь"
    }

    fun logout() {
        auth.signOut()
        resetAuthStatus()
    }

    // ----------------------------------------

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                _isAuthSuccess.value = true
            } catch (e: Exception) {
                _isAuthSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}
package com.example.fincoach.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fincoach.ui.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appSettings = AppSettings(application)

    val isDarkTheme: StateFlow<Boolean> = appSettings.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val language: StateFlow<String> = appSettings.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ru")

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { appSettings.setDarkTheme(enabled) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { appSettings.setLanguage(lang) }
    }
}
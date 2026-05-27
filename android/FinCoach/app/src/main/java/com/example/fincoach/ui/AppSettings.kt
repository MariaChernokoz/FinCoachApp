package com.example.fincoach.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppSettings(private val context: Context) {

    companion object {
        val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[DARK_THEME_KEY] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DARK_THEME_KEY] = enabled }
    }
}

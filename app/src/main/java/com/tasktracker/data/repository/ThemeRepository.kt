package com.tasktracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tasktracker.data.models.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeRepository(private val context: Context) {
    private val THEME_KEY = stringPreferencesKey("app_theme")

    val currentTheme: Flow<AppTheme> = context.dataStore.data
        .map { prefs ->
            prefs[THEME_KEY]?.let { name ->
                runCatching { AppTheme.valueOf(name) }.getOrNull()
            } ?: AppTheme.PURPLE
        }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }
}

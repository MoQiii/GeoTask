package com.syj.geotask.presentation.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// 扩展属性来创建 DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

@Singleton
class ThemeManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    }

    // 获取深色模式设置的 Flow
    val darkModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false // 默认为浅色模式
        }

    // 获取当前深色模式设置（同步方法，用于初始化）
    suspend fun getDarkMode(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }.first()
    }

    // 设置深色模式
    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDarkMode
        }
    }
}

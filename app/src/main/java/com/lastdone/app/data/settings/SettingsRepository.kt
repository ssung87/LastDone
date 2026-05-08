package com.lastdone.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            impendingThresholdDays = prefs[Keys.ImpendingThreshold]
                ?: AppSettings.DEFAULT_IMPENDING_THRESHOLD,
            themeMode = prefs[Keys.ThemeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            sortMode = prefs[Keys.SortMode]?.let { runCatching { SortMode.valueOf(it) }.getOrNull() }
                ?: SortMode.STATUS,
            notifyTime = prefs[Keys.NotifyTimeSecondOfDay]
                ?.let { LocalTime.ofSecondOfDay(it.toLong().coerceIn(0L, 86_399L)) }
                ?: AppSettings.DEFAULT_NOTIFY_TIME
        )
    }.distinctUntilChanged()

    val impendingThreshold: Flow<Int> = settings.map { it.impendingThresholdDays }.distinctUntilChanged()
    val themeMode: Flow<ThemeMode> = settings.map { it.themeMode }.distinctUntilChanged()
    val sortMode: Flow<SortMode> = settings.map { it.sortMode }.distinctUntilChanged()
    val notifyTime: Flow<LocalTime> = settings.map { it.notifyTime }.distinctUntilChanged()

    suspend fun setImpendingThreshold(value: Int) {
        store.edit {
            it[Keys.ImpendingThreshold] = value
                .coerceIn(AppSettings.MIN_IMPENDING_THRESHOLD, AppSettings.MAX_IMPENDING_THRESHOLD)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.ThemeMode] = mode.name }
    }

    suspend fun setSortMode(mode: SortMode) {
        store.edit { it[Keys.SortMode] = mode.name }
    }

    suspend fun setNotifyTime(time: LocalTime) {
        store.edit { it[Keys.NotifyTimeSecondOfDay] = time.toSecondOfDay() }
    }

    private object Keys {
        val ImpendingThreshold = intPreferencesKey("impending_threshold")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val SortMode = stringPreferencesKey("sort_mode")
        val NotifyTimeSecondOfDay = intPreferencesKey("notify_time_second_of_day")
    }
}

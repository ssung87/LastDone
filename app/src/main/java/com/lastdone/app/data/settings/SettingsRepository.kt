package com.lastdone.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
                ?: AppSettings.DEFAULT_NOTIFY_TIME,
            quietHoursEnabled = prefs[Keys.QuietHoursEnabled] ?: false,
            quietHoursStart = prefs[Keys.QuietHoursStartSecondOfDay]
                ?.let { LocalTime.ofSecondOfDay(it.toLong().coerceIn(0L, 86_399L)) }
                ?: AppSettings.DEFAULT_QUIET_START,
            quietHoursEnd = prefs[Keys.QuietHoursEndSecondOfDay]
                ?.let { LocalTime.ofSecondOfDay(it.toLong().coerceIn(0L, 86_399L)) }
                ?: AppSettings.DEFAULT_QUIET_END,
            globalRepeatIntervalMinutes = prefs[Keys.GlobalRepeatIntervalMinutes]
                ?.takeIf { AppSettings.ALLOWED_GLOBAL_REPEAT_MINUTES.contains(it) }
                ?: AppSettings.DEFAULT_GLOBAL_REPEAT_MINUTES,
            doneCount = prefs[Keys.DoneCount] ?: 0,
            lastReviewedVersion = prefs[Keys.LastReviewedVersion]
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

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        store.edit { it[Keys.QuietHoursEnabled] = enabled }
    }

    suspend fun setQuietHoursStart(time: LocalTime) {
        store.edit { it[Keys.QuietHoursStartSecondOfDay] = time.toSecondOfDay() }
    }

    suspend fun setQuietHoursEnd(time: LocalTime) {
        store.edit { it[Keys.QuietHoursEndSecondOfDay] = time.toSecondOfDay() }
    }

    suspend fun setGlobalRepeatIntervalMinutes(minutes: Int) {
        val sanitized = if (AppSettings.ALLOWED_GLOBAL_REPEAT_MINUTES.contains(minutes)) {
            minutes
        } else {
            AppSettings.DEFAULT_GLOBAL_REPEAT_MINUTES
        }
        store.edit { it[Keys.GlobalRepeatIntervalMinutes] = sanitized }
    }

    suspend fun incrementDoneCount() {
        store.edit { prefs ->
            prefs[Keys.DoneCount] = (prefs[Keys.DoneCount] ?: 0) + 1
        }
    }

    suspend fun markReviewed(versionCode: Int) {
        store.edit { it[Keys.LastReviewedVersion] = versionCode }
    }

    private object Keys {
        val ImpendingThreshold = intPreferencesKey("impending_threshold")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val SortMode = stringPreferencesKey("sort_mode")
        val NotifyTimeSecondOfDay = intPreferencesKey("notify_time_second_of_day")
        val QuietHoursEnabled = booleanPreferencesKey("quiet_hours_enabled")
        val QuietHoursStartSecondOfDay = intPreferencesKey("quiet_hours_start_second_of_day")
        val QuietHoursEndSecondOfDay = intPreferencesKey("quiet_hours_end_second_of_day")
        val GlobalRepeatIntervalMinutes = intPreferencesKey("global_repeat_interval_minutes")
        val DoneCount = intPreferencesKey("done_count")
        val LastReviewedVersion = intPreferencesKey("last_reviewed_version")
    }
}

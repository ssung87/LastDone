package com.lastdone.app.data.settings

import java.time.LocalTime

data class AppSettings(
    val impendingThresholdDays: Int = DEFAULT_IMPENDING_THRESHOLD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val sortMode: SortMode = SortMode.STATUS,
    val notifyTime: LocalTime = DEFAULT_NOTIFY_TIME
) {
    companion object {
        const val DEFAULT_IMPENDING_THRESHOLD = 3
        const val MIN_IMPENDING_THRESHOLD = 1
        const val MAX_IMPENDING_THRESHOLD = 14
        val DEFAULT_NOTIFY_TIME: LocalTime = LocalTime.of(9, 0)
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class SortMode { STATUS, CATEGORY, LAST_DONE_DATE, CREATED_AT }

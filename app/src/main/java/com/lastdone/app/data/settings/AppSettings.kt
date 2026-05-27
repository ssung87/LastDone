package com.lastdone.app.data.settings

import java.time.LocalTime

data class AppSettings(
    val impendingThresholdDays: Int = DEFAULT_IMPENDING_THRESHOLD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val sortMode: SortMode = SortMode.STATUS,
    val notifyTime: LocalTime = DEFAULT_NOTIFY_TIME,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: LocalTime = DEFAULT_QUIET_START,
    val quietHoursEnd: LocalTime = DEFAULT_QUIET_END,
    val globalRepeatIntervalMinutes: Int = DEFAULT_GLOBAL_REPEAT_MINUTES,
    val doneCount: Int = 0,
    val lastReviewedVersion: Int? = null
) {
    companion object {
        const val DEFAULT_IMPENDING_THRESHOLD = 3
        const val MIN_IMPENDING_THRESHOLD = 1
        const val MAX_IMPENDING_THRESHOLD = 14
        val DEFAULT_NOTIFY_TIME: LocalTime = LocalTime.of(9, 0)
        val DEFAULT_QUIET_START: LocalTime = LocalTime.of(22, 0)
        val DEFAULT_QUIET_END: LocalTime = LocalTime.of(7, 0)
        const val DEFAULT_GLOBAL_REPEAT_MINUTES = 0
        val ALLOWED_GLOBAL_REPEAT_MINUTES = listOf(0, 10, 30, 60)
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class SortMode { STATUS, CATEGORY, LAST_DONE_DATE, CREATED_AT }

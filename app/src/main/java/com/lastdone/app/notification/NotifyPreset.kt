package com.lastdone.app.notification

import java.time.LocalTime

enum class NotifyPreset(val labelKo: String) {
    MORNING_OF("당일 아침 (09:00)"),
    NOON_OF("당일 정오 (12:00)"),
    EVENING_OF("당일 저녁 (18:00)"),
    ONE_DAY_BEFORE("1일 전"),
    THREE_DAYS_BEFORE("3일 전"),
    ONE_WEEK_BEFORE("1주일 전");

    fun leadDays(): Int = when (this) {
        MORNING_OF, NOON_OF, EVENING_OF -> 0
        ONE_DAY_BEFORE -> 1
        THREE_DAYS_BEFORE -> 3
        ONE_WEEK_BEFORE -> 7
    }

    fun timeOfDay(): LocalTime? = when (this) {
        MORNING_OF -> LocalTime.of(9, 0)
        NOON_OF -> LocalTime.of(12, 0)
        EVENING_OF -> LocalTime.of(18, 0)
        ONE_DAY_BEFORE, THREE_DAYS_BEFORE, ONE_WEEK_BEFORE -> null
    }

    companion object {
        val Default: NotifyPreset = MORNING_OF

        fun parse(value: String?): NotifyPreset = value
            ?.let { runCatching { valueOf(it) }.getOrNull() }
            ?: Default
    }
}

package com.lastdone.app.notification

enum class RepeatPreset(val labelKo: String, val minutes: Int) {
    NONE("추가 알림 없음", 0),
    EVERY_10_MIN("10분마다", 10),
    EVERY_30_MIN("30분마다", 30),
    EVERY_HOUR("1시간마다", 60);

    companion object {
        val Default: RepeatPreset = NONE

        fun parse(minutes: Int): RepeatPreset =
            entries.firstOrNull { it.minutes == minutes } ?: NONE
    }
}

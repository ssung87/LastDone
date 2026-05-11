package com.lastdone.app.notification

import java.time.LocalTime

object QuietHours {
    fun isWithin(now: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        if (start == end) return false
        return if (start.isBefore(end)) {
            !now.isBefore(start) && now.isBefore(end)
        } else {
            !now.isBefore(start) || now.isBefore(end)
        }
    }
}

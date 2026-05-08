package com.lastdone.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun calculateItemStatus(
    lastDoneDate: LocalDate,
    intervalDays: Int,
    today: LocalDate,
    impendingThreshold: Int
): ItemStatus {
    val elapsed = ChronoUnit.DAYS.between(lastDoneDate, today).toInt()
    val remaining = intervalDays - elapsed
    val kind = when {
        remaining < 0 -> ItemStatus.Kind.OVERDUE
        remaining <= impendingThreshold -> ItemStatus.Kind.IMPENDING
        else -> ItemStatus.Kind.RELAXED
    }
    return ItemStatus(daysElapsed = elapsed, daysRemaining = remaining, kind = kind)
}

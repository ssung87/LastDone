package com.lastdone.app.domain

data class ItemStatus(
    val daysElapsed: Int,
    val daysRemaining: Int,
    val kind: Kind
) {
    enum class Kind { OVERDUE, DUE_TODAY, IMPENDING, RELAXED }

    val daysOver: Int get() = if (kind == Kind.OVERDUE) -daysRemaining else 0
}

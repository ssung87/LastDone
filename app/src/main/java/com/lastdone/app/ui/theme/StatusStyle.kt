package com.lastdone.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.lastdone.app.domain.ItemStatus

fun statusColorFor(kind: ItemStatus.Kind): Color = when (kind) {
    ItemStatus.Kind.OVERDUE -> StatusOverdue
    ItemStatus.Kind.IMPENDING -> StatusImpending
    ItemStatus.Kind.RELAXED -> StatusRelaxed
}

fun statusLabelFor(kind: ItemStatus.Kind): String = when (kind) {
    ItemStatus.Kind.OVERDUE -> "초과"
    ItemStatus.Kind.IMPENDING -> "임박"
    ItemStatus.Kind.RELAXED -> "여유"
}

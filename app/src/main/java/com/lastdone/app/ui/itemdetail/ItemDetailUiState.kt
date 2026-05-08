package com.lastdone.app.ui.itemdetail

import com.lastdone.app.data.local.entity.HistoryEntity
import com.lastdone.app.domain.ItemStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val ui: ItemDetailUi? = null
)

data class ItemDetailUi(
    val name: String,
    val icon: String,
    val categoryName: String,
    val lastDoneDate: LocalDate,
    val intervalDays: Int,
    val memo: String?,
    val status: ItemStatus,
    val history: List<HistoryEntry>
)

data class HistoryEntry(
    val id: Long,
    val doneDate: LocalDate,
    val gapDays: Int?,
    val memo: String?
)

internal fun List<HistoryEntity>.toUiHistory(): List<HistoryEntry> {
    val sorted = sortedByDescending { it.doneDate }
    return sorted.mapIndexed { index, entry ->
        val previous = sorted.getOrNull(index + 1)
        HistoryEntry(
            id = entry.id,
            doneDate = entry.doneDate,
            gapDays = previous?.let {
                ChronoUnit.DAYS.between(it.doneDate, entry.doneDate).toInt()
            },
            memo = entry.memo
        )
    }
}

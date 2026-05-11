package com.lastdone.app.ui.home

import com.lastdone.app.domain.ItemStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class HomeUiState(
    val items: List<HomeItemUi>,
    val groupByStatus: Boolean = false,
    val searchQuery: String = "",
    val totalItemCount: Int = 0
)

data class HomeItemUi(
    val id: Long,
    val name: String,
    val categoryName: String,
    val icon: String?,
    val lastDoneDate: LocalDate,
    val createdAt: LocalDateTime,
    val status: ItemStatus
)

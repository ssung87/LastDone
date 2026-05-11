package com.lastdone.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lastdone.app.LastDoneApplication
import com.lastdone.app.data.local.dao.CategoryDao
import com.lastdone.app.data.local.dao.ItemDao
import com.lastdone.app.data.settings.SettingsRepository
import com.lastdone.app.data.settings.SortMode
import com.lastdone.app.domain.ItemStatus
import com.lastdone.app.domain.calculateItemStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class HomeViewModel(
    itemDao: ItemDao,
    categoryDao: CategoryDao,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        itemDao.observeAll(),
        categoryDao.observeAll(),
        settingsRepository.impendingThreshold,
        settingsRepository.sortMode,
        searchQuery
    ) { items, categories, threshold, sortMode, query ->
        val categoryNameById = categories.associate { it.id to it.name }
        val cards = items.map { item ->
            HomeItemUi(
                id = item.id,
                name = item.name,
                categoryName = categoryNameById[item.categoryId].orEmpty(),
                icon = item.icon,
                lastDoneDate = item.lastDoneDate,
                createdAt = item.createdAt,
                status = calculateItemStatus(
                    lastDoneDate = item.lastDoneDate,
                    intervalDays = item.intervalDays,
                    today = today,
                    impendingThreshold = threshold
                )
            )
        }
        val trimmed = query.trim()
        val filtered = if (trimmed.isEmpty()) cards else cards.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.categoryName.contains(trimmed, ignoreCase = true)
        }
        HomeUiState(
            items = filtered.sortedWith(comparatorFor(sortMode)),
            groupByStatus = sortMode == SortMode.STATUS && trimmed.isEmpty(),
            searchQuery = query,
            totalItemCount = cards.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(items = emptyList())
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    private fun comparatorFor(sortMode: SortMode): Comparator<HomeItemUi> = when (sortMode) {
        SortMode.STATUS -> statusOrder
        SortMode.CATEGORY -> compareBy<HomeItemUi> { it.categoryName }.then(statusOrder)
        SortMode.LAST_DONE_DATE -> compareBy { it.lastDoneDate }
        SortMode.CREATED_AT -> compareByDescending { it.createdAt }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LastDoneApplication
                HomeViewModel(
                    itemDao = app.database.itemDao(),
                    categoryDao = app.database.categoryDao(),
                    settingsRepository = app.settingsRepository
                )
            }
        }
    }
}

private val statusOrder = Comparator<HomeItemUi> { a, b ->
    val byKind = a.status.kind.ordinal.compareTo(b.status.kind.ordinal)
    if (byKind != 0) return@Comparator byKind
    when (a.status.kind) {
        ItemStatus.Kind.OVERDUE,
        ItemStatus.Kind.IMPENDING -> a.status.daysRemaining.compareTo(b.status.daysRemaining)
        ItemStatus.Kind.DUE_TODAY -> 0
        ItemStatus.Kind.RELAXED -> b.status.daysRemaining.compareTo(a.status.daysRemaining)
    }
}

package com.lastdone.app.ui.itemdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lastdone.app.LastDoneApplication
import com.lastdone.app.core.format.formatShortDate
import com.lastdone.app.data.local.dao.CategoryDao
import com.lastdone.app.data.local.dao.HistoryDao
import com.lastdone.app.data.local.dao.ItemDao
import com.lastdone.app.data.local.entity.HistoryEntity
import com.lastdone.app.data.settings.SettingsRepository
import com.lastdone.app.domain.calculateItemStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ItemDetailViewModel(
    private val itemId: Long,
    private val itemDao: ItemDao,
    private val historyDao: HistoryDao,
    categoryDao: CategoryDao,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    private val itemFlow = itemDao.observeAll().map { items ->
        items.firstOrNull { it.id == itemId }
    }

    private val categoryMapFlow = categoryDao.observeAll().map { list ->
        list.associate { it.id to it.name }
    }

    val uiState: StateFlow<ItemDetailUiState> = combine(
        itemFlow,
        historyDao.observeByItem(itemId),
        categoryMapFlow,
        settingsRepository.impendingThreshold
    ) { item, histories, categoryMap, threshold ->
        if (item == null) {
            ItemDetailUiState(isLoading = false, ui = null)
        } else {
            ItemDetailUiState(
                isLoading = false,
                ui = ItemDetailUi(
                    name = item.name,
                    icon = item.icon ?: "·",
                    categoryName = categoryMap[item.categoryId].orEmpty(),
                    lastDoneDate = item.lastDoneDate,
                    intervalDays = item.intervalDays,
                    memo = item.memo,
                    status = calculateItemStatus(
                        lastDoneDate = item.lastDoneDate,
                        intervalDays = item.intervalDays,
                        today = today,
                        impendingThreshold = threshold
                    ),
                    history = histories.toUiHistory()
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ItemDetailUiState()
    )

    private val _events = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun markDoneToday() {
        viewModelScope.launch {
            val item = itemDao.getById(itemId) ?: return@launch
            val nowDate = LocalDate.now()
            itemDao.update(item.copy(lastDoneDate = nowDate))
            historyDao.insert(
                HistoryEntity(
                    itemId = itemId,
                    doneDate = nowDate,
                    memo = null
                )
            )
            val nextDate = nowDate.plusDays(item.intervalDays.toLong())
            _events.emit("기록됨 · 다음 권장일 ${formatShortDate(nextDate)}")
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val item = itemDao.getById(itemId) ?: return@launch
            historyDao.deleteAllForItem(itemId)
            itemDao.delete(item)
            onDeleted()
        }
    }

    fun deleteHistory(historyId: Long) {
        viewModelScope.launch {
            historyDao.deleteById(historyId)
        }
    }

    companion object {
        fun factory(itemId: Long) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LastDoneApplication
                ItemDetailViewModel(
                    itemId = itemId,
                    itemDao = app.database.itemDao(),
                    historyDao = app.database.historyDao(),
                    categoryDao = app.database.categoryDao(),
                    settingsRepository = app.settingsRepository
                )
            }
        }
    }
}

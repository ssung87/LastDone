package com.lastdone.app.ui.itemdetail

import android.content.Context
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
import com.lastdone.app.data.local.dao.TemplateDao
import com.lastdone.app.data.local.entity.HistoryEntity
import com.lastdone.app.data.local.entity.TemplateEntity
import com.lastdone.app.data.settings.SettingsRepository
import com.lastdone.app.domain.calculateItemStatus
import com.lastdone.app.notification.RepeatAlarmScheduler
import com.lastdone.app.widget.LastDoneWidgetProvider
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
    private val appContext: Context,
    private val itemDao: ItemDao,
    private val historyDao: HistoryDao,
    private val templateDao: TemplateDao,
    categoryDao: CategoryDao,
    private val settingsRepository: SettingsRepository
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
            itemDao.update(item.copy(lastDoneDate = nowDate, lastNotifiedAt = null))
            historyDao.insert(
                HistoryEntity(
                    itemId = itemId,
                    doneDate = nowDate,
                    memo = null
                )
            )
            RepeatAlarmScheduler.cancel(appContext, itemId)
            LastDoneWidgetProvider.requestUpdate(appContext)
            settingsRepository.incrementDoneCount()
            val nextDate = nowDate.plusDays(item.intervalDays.toLong())
            _events.emit("기록됨 · 다음 권장일 ${formatShortDate(nextDate)}")
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val item = itemDao.getById(itemId) ?: return@launch
            historyDao.deleteAllForItem(itemId)
            itemDao.delete(item)
            RepeatAlarmScheduler.cancel(appContext, itemId)
            LastDoneWidgetProvider.requestUpdate(appContext)
            onDeleted()
        }
    }

    fun deleteHistory(historyId: Long) {
        viewModelScope.launch {
            historyDao.deleteById(historyId)
        }
    }

    fun saveAsTemplate() {
        viewModelScope.launch {
            val item = itemDao.getById(itemId) ?: return@launch
            templateDao.insert(
                TemplateEntity(
                    name = item.name,
                    intervalDays = item.intervalDays,
                    icon = item.icon,
                    categoryId = item.categoryId
                )
            )
            _events.emit("템플릿으로 저장됐어요")
        }
    }

    companion object {
        fun factory(itemId: Long) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LastDoneApplication
                ItemDetailViewModel(
                    itemId = itemId,
                    appContext = app.applicationContext,
                    itemDao = app.database.itemDao(),
                    historyDao = app.database.historyDao(),
                    templateDao = app.database.templateDao(),
                    categoryDao = app.database.categoryDao(),
                    settingsRepository = app.settingsRepository
                )
            }
        }
    }
}

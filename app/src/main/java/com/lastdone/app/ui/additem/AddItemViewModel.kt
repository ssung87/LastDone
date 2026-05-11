package com.lastdone.app.ui.additem

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lastdone.app.LastDoneApplication
import com.lastdone.app.data.local.dao.CategoryDao
import com.lastdone.app.data.local.dao.HistoryDao
import com.lastdone.app.data.local.dao.ItemDao
import com.lastdone.app.data.local.dao.TemplateDao
import com.lastdone.app.data.local.entity.HistoryEntity
import com.lastdone.app.data.local.entity.ItemEntity
import com.lastdone.app.data.local.entity.TemplateEntity
import com.lastdone.app.notification.NotifyPreset
import com.lastdone.app.notification.RepeatAlarmScheduler
import com.lastdone.app.notification.RepeatPreset
import com.lastdone.app.widget.LastDoneWidgetProvider
import com.lastdone.app.ui.templates.TemplateSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class AddItemViewModel(
    private val itemId: Long?,
    private val appContext: Context,
    private val itemDao: ItemDao,
    private val historyDao: HistoryDao,
    categoryDao: CategoryDao,
    private val templateDao: TemplateDao,
    prefill: AddItemPrefill? = null
) : ViewModel() {

    private val _state = MutableStateFlow(
        AddItemUiState(
            isEditMode = itemId != null,
            name = if (itemId == null) prefill?.name.orEmpty() else "",
            intervalDaysText = if (itemId == null && prefill != null) prefill.intervalDays.toString() else "",
            icon = if (itemId == null) prefill?.icon.orEmpty() else ""
        )
    )
    val state: StateFlow<AddItemUiState> = _state.asStateFlow()

    val userTemplates: StateFlow<List<TemplateEntity>> = templateDao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            categoryDao.observeAll().collect { categories ->
                _state.update { current ->
                    current.copy(
                        categories = categories,
                        selectedCategoryId = current.selectedCategoryId
                            ?: categories.firstOrNull()?.id
                    )
                }
            }
        }
        if (itemId != null) {
            viewModelScope.launch {
                val existing = itemDao.getById(itemId) ?: return@launch
                _state.update { current ->
                    current.copy(
                        name = existing.name,
                        selectedCategoryId = existing.categoryId,
                        lastDoneDate = existing.lastDoneDate,
                        intervalDaysText = existing.intervalDays.toString(),
                        icon = existing.icon.orEmpty(),
                        memo = existing.memo.orEmpty(),
                        notifyEnabled = existing.notifyEnabled,
                        notifyPreset = NotifyPreset.parse(existing.notifyPreset),
                        repeatPreset = RepeatPreset.parse(existing.repeatIntervalMinutes)
                    )
                }
            }
        }
    }

    fun setName(value: String) {
        _state.update { it.copy(name = value, nameError = null) }
    }

    fun setCategory(id: Long) {
        _state.update { it.copy(selectedCategoryId = id) }
    }

    fun setDate(date: java.time.LocalDate) {
        _state.update { it.copy(lastDoneDate = date) }
    }

    fun setInterval(value: String) {
        val sanitized = value.filter { it.isDigit() }.take(4)
        _state.update { it.copy(intervalDaysText = sanitized, intervalDaysError = null) }
    }

    fun setIcon(value: String) {
        _state.update { it.copy(icon = value.take(4)) }
    }

    fun setMemo(value: String) {
        _state.update { it.copy(memo = value) }
    }

    fun setNotifyEnabled(value: Boolean) {
        _state.update { it.copy(notifyEnabled = value) }
    }

    fun setNotifyPreset(value: NotifyPreset) {
        _state.update { it.copy(notifyPreset = value) }
    }

    fun setRepeatPreset(value: RepeatPreset) {
        _state.update { it.copy(repeatPreset = value) }
    }

    fun applyTemplate(selection: TemplateSelection) {
        _state.update { current ->
            current.copy(
                name = selection.name,
                nameError = null,
                intervalDaysText = selection.intervalDays.toString(),
                intervalDaysError = null,
                icon = selection.icon.orEmpty(),
                selectedCategoryId = selection.categoryId ?: current.selectedCategoryId
            )
        }
    }

    fun deleteUserTemplate(templateId: Long) {
        viewModelScope.launch {
            templateDao.deleteById(templateId)
        }
    }

    fun save() {
        val current = _state.value
        val nameError = when {
            current.name.isBlank() -> "항목명을 입력해 주세요"
            current.name.trim().length > 20 -> "항목명은 20자 이내로 입력해 주세요"
            else -> null
        }
        val intervalDays = current.intervalDaysText.toIntOrNull()
        val intervalError = when {
            intervalDays == null -> "권장 주기를 입력해 주세요"
            intervalDays < 1 -> "1일 이상이어야 합니다"
            else -> null
        }
        val categoryId = current.selectedCategoryId

        if (nameError != null || intervalError != null || intervalDays == null || categoryId == null) {
            _state.update {
                it.copy(nameError = nameError, intervalDaysError = intervalError)
            }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            if (itemId == null) {
                val newId = itemDao.insert(
                    ItemEntity(
                        name = current.name.trim(),
                        categoryId = categoryId,
                        lastDoneDate = current.lastDoneDate,
                        intervalDays = intervalDays,
                        icon = current.icon.ifBlank { null },
                        memo = current.memo.ifBlank { null },
                        notifyEnabled = current.notifyEnabled,
                        notifyTime = null,
                        notifyPreset = current.notifyPreset.name,
                        repeatIntervalMinutes = current.repeatPreset.minutes,
                        createdAt = LocalDateTime.now()
                    )
                )
                historyDao.insert(
                    HistoryEntity(
                        itemId = newId,
                        doneDate = current.lastDoneDate,
                        memo = null
                    )
                )
            } else {
                val existing = itemDao.getById(itemId) ?: run {
                    _state.update { it.copy(isSaving = false) }
                    return@launch
                }
                val presetChanged = NotifyPreset.parse(existing.notifyPreset) != current.notifyPreset
                val repeatChanged = existing.repeatIntervalMinutes != current.repeatPreset.minutes
                val notifyToggleChanged = existing.notifyEnabled != current.notifyEnabled
                val intervalOrDateChanged = existing.lastDoneDate != current.lastDoneDate ||
                    existing.intervalDays != intervalDays
                val notifyResetNeeded = presetChanged || repeatChanged ||
                    notifyToggleChanged || intervalOrDateChanged
                itemDao.update(
                    existing.copy(
                        name = current.name.trim(),
                        categoryId = categoryId,
                        lastDoneDate = current.lastDoneDate,
                        intervalDays = intervalDays,
                        icon = current.icon.ifBlank { null },
                        memo = current.memo.ifBlank { null },
                        notifyEnabled = current.notifyEnabled,
                        notifyPreset = current.notifyPreset.name,
                        repeatIntervalMinutes = current.repeatPreset.minutes,
                        lastNotifiedAt = if (notifyResetNeeded) null else existing.lastNotifiedAt
                    )
                )
                if (notifyResetNeeded) {
                    RepeatAlarmScheduler.cancel(appContext, itemId)
                }
                if (existing.lastDoneDate != current.lastDoneDate) {
                    historyDao.findMostRecentByItem(itemId)?.let { recent ->
                        historyDao.update(recent.copy(doneDate = current.lastDoneDate))
                    }
                }
            }
            _state.update { it.copy(isSaving = false, saveSuccess = true) }
            LastDoneWidgetProvider.requestUpdate(appContext)
        }
    }

    companion object {
        fun factory(
            itemId: Long? = null,
            prefill: AddItemPrefill? = null
        ) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LastDoneApplication
                AddItemViewModel(
                    itemId = itemId,
                    appContext = app.applicationContext,
                    itemDao = app.database.itemDao(),
                    historyDao = app.database.historyDao(),
                    categoryDao = app.database.categoryDao(),
                    templateDao = app.database.templateDao(),
                    prefill = prefill
                )
            }
        }
    }
}

data class AddItemPrefill(
    val name: String,
    val intervalDays: Int,
    val icon: String
)

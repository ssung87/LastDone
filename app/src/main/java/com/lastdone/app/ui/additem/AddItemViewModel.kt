package com.lastdone.app.ui.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lastdone.app.LastDoneApplication
import com.lastdone.app.data.local.dao.CategoryDao
import com.lastdone.app.data.local.dao.HistoryDao
import com.lastdone.app.data.local.dao.ItemDao
import com.lastdone.app.data.local.entity.HistoryEntity
import com.lastdone.app.data.local.entity.ItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class AddItemViewModel(
    private val itemId: Long?,
    private val itemDao: ItemDao,
    private val historyDao: HistoryDao,
    categoryDao: CategoryDao
) : ViewModel() {

    private val _state = MutableStateFlow(
        AddItemUiState(isEditMode = itemId != null)
    )
    val state: StateFlow<AddItemUiState> = _state.asStateFlow()

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
                        notifyEnabled = existing.notifyEnabled
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
                itemDao.update(
                    existing.copy(
                        name = current.name.trim(),
                        categoryId = categoryId,
                        lastDoneDate = current.lastDoneDate,
                        intervalDays = intervalDays,
                        icon = current.icon.ifBlank { null },
                        memo = current.memo.ifBlank { null },
                        notifyEnabled = current.notifyEnabled
                    )
                )
            }
            _state.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    companion object {
        fun factory(itemId: Long? = null) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LastDoneApplication
                AddItemViewModel(
                    itemId = itemId,
                    itemDao = app.database.itemDao(),
                    historyDao = app.database.historyDao(),
                    categoryDao = app.database.categoryDao()
                )
            }
        }
    }
}

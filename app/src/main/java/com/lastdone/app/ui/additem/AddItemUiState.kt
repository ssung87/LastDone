package com.lastdone.app.ui.additem

import com.lastdone.app.data.local.entity.CategoryEntity
import com.lastdone.app.notification.NotifyPreset
import com.lastdone.app.notification.RepeatPreset
import java.time.LocalDate

data class AddItemUiState(
    val name: String = "",
    val nameError: String? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    val lastDoneDate: LocalDate = LocalDate.now(),
    val intervalDaysText: String = "",
    val intervalDaysError: String? = null,
    val icon: String = "",
    val memo: String = "",
    val notifyEnabled: Boolean = false,
    val notifyPreset: NotifyPreset = NotifyPreset.Default,
    val repeatPreset: RepeatPreset = RepeatPreset.Default,
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

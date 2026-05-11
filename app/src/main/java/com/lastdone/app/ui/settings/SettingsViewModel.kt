package com.lastdone.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lastdone.app.LastDoneApplication
import com.lastdone.app.data.settings.AppSettings
import com.lastdone.app.data.settings.SettingsRepository
import com.lastdone.app.data.settings.SortMode
import com.lastdone.app.data.settings.ThemeMode
import java.time.LocalTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val state: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    fun setImpendingThreshold(value: Int) {
        viewModelScope.launch { repository.setImpendingThreshold(value) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setSortMode(mode: SortMode) {
        viewModelScope.launch { repository.setSortMode(mode) }
    }

    fun setNotifyTime(time: LocalTime) {
        viewModelScope.launch { repository.setNotifyTime(time) }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setQuietHoursEnabled(enabled) }
    }

    fun setQuietHoursStart(time: LocalTime) {
        viewModelScope.launch { repository.setQuietHoursStart(time) }
    }

    fun setQuietHoursEnd(time: LocalTime) {
        viewModelScope.launch { repository.setQuietHoursEnd(time) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LastDoneApplication
                SettingsViewModel(repository = app.settingsRepository)
            }
        }
    }
}

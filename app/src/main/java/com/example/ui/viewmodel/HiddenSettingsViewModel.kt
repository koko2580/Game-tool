package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.system.DeviceTelemetryManager
import com.example.data.system.SystemSettingsNavigator
import com.example.domain.model.ShortcutCategory
import com.example.domain.model.ShortcutLaunchResult
import com.example.domain.model.SystemSettingsShortcut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HiddenSettingsUiState(
    val shortcuts: List<SystemSettingsShortcut> = emptyList(),
    val selectedCategory: ShortcutCategory? = null,
    val isXiaomiOrHyperOs: Boolean = false,
    val osUiFlavor: String? = null,
    val lastLaunchResult: ShortcutLaunchResult? = null
)

class HiddenSettingsViewModel(
    private val settingsNavigator: SystemSettingsNavigator,
    deviceTelemetryManager: DeviceTelemetryManager
) : ViewModel() {

    private val deviceIdentity = deviceTelemetryManager.readDeviceIdentity()
    private val allShortcuts = settingsNavigator.getAvailableShortcuts(deviceIdentity.isXiaomiOrHyperOs)

    private val _uiState = MutableStateFlow(
        HiddenSettingsUiState(
            shortcuts = allShortcuts,
            isXiaomiOrHyperOs = deviceIdentity.isXiaomiOrHyperOs,
            osUiFlavor = deviceIdentity.osUiFlavor
        )
    )
    val uiState: StateFlow<HiddenSettingsUiState> = _uiState.asStateFlow()

    fun selectCategory(category: ShortcutCategory?) {
        val filtered = if (category == null) {
            allShortcuts
        } else {
            allShortcuts.filter { it.category == category }
        }
        _uiState.update {
            it.copy(
                selectedCategory = category,
                shortcuts = filtered
            )
        }
    }

    fun launchShortcut(shortcut: SystemSettingsShortcut) {
        val result = settingsNavigator.launchShortcut(shortcut)
        _uiState.update { it.copy(lastLaunchResult = result) }
    }

    fun clearLaunchResult() {
        _uiState.update { it.copy(lastLaunchResult = null) }
    }

    class Factory(
        private val navigator: SystemSettingsNavigator,
        private val telemetryManager: DeviceTelemetryManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HiddenSettingsViewModel(navigator, telemetryManager) as T
        }
    }
}

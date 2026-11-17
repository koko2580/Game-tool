package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.AppLanguageOption
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.TelemetryRepository
import com.example.data.system.DndController
import com.example.data.system.SystemSettingsNavigator
import com.example.turbo.MediaCaptureController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val canWriteSystemSettings: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasDndPolicyAccess: Boolean = false,
    val isXiaomiOrHyperOs: Boolean = false,
    val osUiFlavor: String? = null
)

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val telemetryRepository: TelemetryRepository,
    private val settingsNavigator: SystemSettingsNavigator,
    private val mediaCaptureController: MediaCaptureController,
    private val dndController: DndController
) : ViewModel() {

    private val permissionRefreshTrigger = MutableStateFlow(0L)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferencesFlow,
        telemetryRepository.telemetrySnapshot,
        permissionRefreshTrigger
    ) { prefs, snapshot, _ ->
        SettingsUiState(
            preferences = prefs,
            canWriteSystemSettings = snapshot.display.canWriteSystemSettings,
            hasOverlayPermission = mediaCaptureController.hasOverlayPermission(),
            hasDndPolicyAccess = dndController.readStatus().hasPolicyAccess,
            isXiaomiOrHyperOs = snapshot.device.isXiaomiOrHyperOs,
            osUiFlavor = snapshot.device.osUiFlavor
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguageOption) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHapticsEnabled(enabled) }
    }

    fun setReducedMotionOverride(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setReducedMotionOverride(enabled) }
    }

    fun openManageWriteSettings() {
        settingsNavigator.launchManageWriteSettings()
    }

    fun openManageOverlaySettings() {
        settingsNavigator.launchManageOverlaySettings()
    }

    fun openDndPolicySettings() {
        dndController.openPolicyAccessSettings()
    }

    fun openAppDetailsSettings() {
        settingsNavigator.launchAppDetailsSettings()
    }

    fun refreshPermissionsState() {
        permissionRefreshTrigger.value = System.currentTimeMillis()
        telemetryRepository.refreshNow()
    }

    class Factory(
        private val preferencesRepository: UserPreferencesRepository,
        private val telemetryRepository: TelemetryRepository,
        private val settingsNavigator: SystemSettingsNavigator,
        private val mediaCaptureController: MediaCaptureController,
        private val dndController: DndController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                preferencesRepository,
                telemetryRepository,
                settingsNavigator,
                mediaCaptureController,
                dndController
            ) as T
        }
    }
}

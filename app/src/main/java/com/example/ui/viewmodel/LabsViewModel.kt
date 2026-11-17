package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TelemetryRepository
import com.example.data.system.SystemSettingsNavigator
import com.example.domain.model.HttpLatencyResult
import com.example.domain.model.SystemTelemetrySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProbeEndpoint(
    val id: String,
    val name: String,
    val url: String
)

enum class LabTab {
    NETWORK,
    DISPLAY
}

data class LabsUiState(
    val telemetry: SystemTelemetrySnapshot,
    val selectedTab: LabTab = LabTab.NETWORK,
    val isProbing: Boolean = false,
    val probeResults: List<HttpLatencyResult> = emptyList(),
    val statusMessage: String? = null
)

class LabsViewModel(
    private val telemetryRepository: TelemetryRepository,
    private val settingsNavigator: SystemSettingsNavigator
) : ViewModel() {

    private val selectedTab = MutableStateFlow(LabTab.NETWORK)
    private val isProbing = MutableStateFlow(false)
    private val probeResults = MutableStateFlow<List<HttpLatencyResult>>(emptyList())
    private val statusMessage = MutableStateFlow<String?>(null)

    val endpoints = listOf(
        ProbeEndpoint("google_204", "Google Connectivity Edge", "https://connectivitycheck.gstatic.com/generate_204"),
        ProbeEndpoint("cloudflare", "Cloudflare Trace", "https://1.1.1.1/cdn-cgi/trace"),
        ProbeEndpoint("github", "GitHub API Edge", "https://api.github.com")
    )

    val uiState: StateFlow<LabsUiState> = combine(
        telemetryRepository.telemetrySnapshot,
        selectedTab,
        isProbing,
        probeResults,
        statusMessage
    ) { snapshot, tab, probing, results, message ->
        LabsUiState(
            telemetry = snapshot,
            selectedTab = tab,
            isProbing = probing,
            probeResults = results,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(2_000L),
        initialValue = LabsUiState(telemetry = telemetryRepository.telemetrySnapshot.value)
    )

    fun selectTab(tab: LabTab) {
        selectedTab.value = tab
    }

    fun runHttpLatencyProbe(endpoint: ProbeEndpoint) {
        if (isProbing.value) return
        viewModelScope.launch {
            isProbing.value = true
            val result = telemetryRepository.measureHttpLatency(endpoint.name, endpoint.url)
            probeResults.value = (listOf(result) + probeResults.value).take(6)
            isProbing.value = false
        }
    }

    fun setSystemBrightness(value0To255: Int) {
        viewModelScope.launch {
            val ok = telemetryRepository.setSystemBrightness(value0To255)
            if (!ok) {
                statusMessage.value = "Grant Modify System Settings permission to adjust brightness."
            }
        }
    }

    fun openWriteSettingsPermission() {
        val ok = settingsNavigator.launchManageWriteSettings()
        if (!ok) {
            statusMessage.value = "Unable to open system permission screen on this device."
        }
    }

    fun openDisplaySettings() {
        val shortcut = settingsNavigator
            .getAvailableShortcuts(isXiaomiOrHyperOs = uiState.value.telemetry.device.isXiaomiOrHyperOs)
            .firstOrNull { it.id == "display_settings" }
        if (shortcut != null) {
            settingsNavigator.launchShortcut(shortcut)
        }
    }

    fun openAnimationAccessibilitySettings() {
        val shortcut = settingsNavigator
            .getAvailableShortcuts(isXiaomiOrHyperOs = uiState.value.telemetry.device.isXiaomiOrHyperOs)
            .firstOrNull { it.id == "animation_settings" }
        if (shortcut != null) {
            settingsNavigator.launchShortcut(shortcut)
        }
    }

    fun refreshTelemetry() {
        telemetryRepository.refreshNow()
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    class Factory(
        private val telemetryRepository: TelemetryRepository,
        private val settingsNavigator: SystemSettingsNavigator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LabsViewModel(telemetryRepository, settingsNavigator) as T
        }
    }
}

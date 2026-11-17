package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.GamesRepository
import com.example.data.repository.TelemetryRepository
import com.example.domain.model.InstalledAppItem
import com.example.domain.model.SystemTelemetrySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val telemetry: SystemTelemetrySnapshot,
    val recentGames: List<InstalledAppItem> = emptyList(),
    val launchFeedback: String? = null
)

class DashboardViewModel(
    private val telemetryRepository: TelemetryRepository,
    private val gamesRepository: GamesRepository
) : ViewModel() {

    private val feedbackMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            gamesRepository.reloadInstalledApps()
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        telemetryRepository.telemetrySnapshot,
        gamesRepository.appsWithPrefsFlow,
        feedbackMessage
    ) { snapshot, (_, apps), feedback ->
        val recents = apps
            .filter { it.lastLaunchedMillis != null }
            .sortedByDescending { it.lastLaunchedMillis }
            .take(3)
        DashboardUiState(
            telemetry = snapshot,
            recentGames = recents,
            launchFeedback = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(2_000L),
        initialValue = DashboardUiState(telemetry = telemetryRepository.telemetrySnapshot.value)
    )

    fun refreshTelemetry() {
        telemetryRepository.refreshNow()
    }

    fun launchRecentGame(item: InstalledAppItem) {
        viewModelScope.launch {
            val ok = gamesRepository.launchApp(item.packageName)
            if (!ok) {
                feedbackMessage.value = "Unable to launch ${item.label}."
            }
        }
    }

    fun clearFeedback() {
        feedbackMessage.value = null
    }

    class Factory(
        private val telemetryRepository: TelemetryRepository,
        private val gamesRepository: GamesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(telemetryRepository, gamesRepository) as T
        }
    }
}

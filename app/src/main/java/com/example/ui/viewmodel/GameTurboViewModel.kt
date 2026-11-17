package com.example.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.GamesRepository
import com.example.data.repository.TelemetryRepository
import com.example.data.system.DndController
import com.example.data.system.DndToggleOutcome
import com.example.data.system.SystemSettingsNavigator
import com.example.domain.model.CaptureStatusState
import com.example.domain.model.DndStatusState
import com.example.domain.model.GameSessionRecord
import com.example.domain.model.InstalledAppItem
import com.example.domain.model.SystemTelemetrySnapshot
import com.example.domain.model.TurboPerformanceProfile
import com.example.turbo.MediaCaptureController
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class GameTurboUiState(
    val telemetry: SystemTelemetrySnapshot,
    val activeProfile: TurboPerformanceProfile = TurboPerformanceProfile.BALANCED,
    val activeSession: GameSessionRecord? = null,
    val elapsedSessionSeconds: Long = 0L,
    val sessionHistory: List<GameSessionRecord> = emptyList(),
    val quickLaunchGames: List<InstalledAppItem> = emptyList(),
    val isGamesLoading: Boolean = true,
    val captureState: CaptureStatusState = CaptureStatusState(),
    val isSidebarRunning: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val dndStatus: DndStatusState = DndStatusState(),
    val statusMessage: String? = null
)

class GameTurboViewModel(
    private val telemetryRepository: TelemetryRepository,
    private val gamesRepository: GamesRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val dndController: DndController,
    private val mediaCaptureController: MediaCaptureController,
    private val settingsNavigator: SystemSettingsNavigator
) : ViewModel() {

    private val localStatusMessage = MutableStateFlow<String?>(null)
    private val permissionRefreshTrigger = MutableStateFlow(0L)

    // Lifecycle-aware 1s ticker; stops automatically when UI is not subscribed
    private val timerTickFlow = flow {
        while (currentCoroutineContext().isActive) {
            emit(System.currentTimeMillis())
            delay(1_000L)
        }
    }

    init {
        viewModelScope.launch {
            gamesRepository.reloadInstalledApps()
        }
    }

    private val baseDataFlow = combine(
        telemetryRepository.telemetrySnapshot,
        preferencesRepository.preferencesFlow,
        gamesRepository.appsWithPrefsFlow,
        mediaCaptureController.captureState,
        mediaCaptureController.isSidebarActive
    ) { snapshot, prefs, (gamesLoaded, apps), capture, sidebarRunning ->
        BaseTurboBundle(snapshot, prefs, gamesLoaded, apps, capture, sidebarRunning)
    }

    val uiState: StateFlow<GameTurboUiState> = combine(
        baseDataFlow,
        timerTickFlow,
        permissionRefreshTrigger,
        localStatusMessage
    ) { bundle, nowMillis, _, msg ->
        val activeSession = bundle.prefs.activeSession
        val elapsed = activeSession?.durationSeconds(nowMillis) ?: 0L
        val quickGames = bundle.apps
            .filter { it.isAndroidGameCategory || it.isFavorite }
            .sortedWith(
                compareByDescending<InstalledAppItem> { it.isFavorite }
                    .thenByDescending { it.lastLaunchedMillis ?: 0L }
                    .thenBy { it.label.lowercase() }
            )
            .take(12)

        val resolvedMessage = msg
            ?: bundle.capture.lastError
            ?: bundle.capture.lastSavedMediaMessage

        GameTurboUiState(
            telemetry = bundle.snapshot,
            activeProfile = bundle.prefs.turboProfile,
            activeSession = activeSession,
            elapsedSessionSeconds = elapsed,
            sessionHistory = bundle.prefs.sessionHistory,
            quickLaunchGames = quickGames,
            isGamesLoading = !bundle.gamesLoaded,
            captureState = bundle.capture,
            isSidebarRunning = bundle.sidebarRunning,
            hasOverlayPermission = mediaCaptureController.hasOverlayPermission(),
            dndStatus = dndController.readStatus(),
            statusMessage = resolvedMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(2_000L),
        initialValue = GameTurboUiState(telemetry = telemetryRepository.telemetrySnapshot.value)
    )

    fun refreshEnvironmentState() {
        permissionRefreshTrigger.value = System.currentTimeMillis()
        telemetryRepository.refreshNow()
    }

    fun selectPerformanceProfile(profile: TurboPerformanceProfile) {
        viewModelScope.launch {
            preferencesRepository.setTurboProfile(profile)
            localStatusMessage.value = "${profile.title} profile active (${profile.pollIntervalMs / 1000.0}s cadence)"
        }
    }

    fun startManualSession(targetApp: InstalledAppItem? = null) {
        viewModelScope.launch {
            val activeProfile = uiState.value.activeProfile
            val session = GameSessionRecord(
                id = UUID.randomUUID().toString(),
                packageName = targetApp?.packageName ?: "com.nexorax.manual_session",
                appLabel = targetApp?.label ?: "General Gaming Session",
                startedAtMillis = System.currentTimeMillis(),
                profileUsed = activeProfile
            )
            preferencesRepository.startSession(session)
            localStatusMessage.value = "Started session for ${session.appLabel}"
        }
    }

    fun endActiveSession() {
        viewModelScope.launch {
            preferencesRepository.endActiveSession()
            localStatusMessage.value = "Session saved to history."
        }
    }

    fun clearSessionHistory() {
        viewModelScope.launch {
            preferencesRepository.clearSessionHistory()
            localStatusMessage.value = "Session history cleared."
        }
    }

    fun launchGameWithTurbo(item: InstalledAppItem) {
        viewModelScope.launch {
            val preferred = item.profile?.preferredTurboProfile
            if (preferred != null) {
                preferencesRepository.setTurboProfile(preferred)
            }
            val effectiveProfile = preferred ?: uiState.value.activeProfile
            val launched = gamesRepository.launchApp(item.packageName)
            if (launched) {
                val session = GameSessionRecord(
                    id = UUID.randomUUID().toString(),
                    packageName = item.packageName,
                    appLabel = item.label,
                    startedAtMillis = System.currentTimeMillis(),
                    profileUsed = effectiveProfile
                )
                preferencesRepository.startSession(session)
                val dndNote = if (item.profile?.enableDndReminder == true && !uiState.value.dndStatus.isDndEnabled) {
                    " • Reminder: enable DND"
                } else ""
                localStatusMessage.value = "Launching ${item.label} (${effectiveProfile.title})$dndNote"
            } else {
                localStatusMessage.value = "Unable to launch ${item.label} on this device."
            }
        }
    }

    fun toggleDnd() {
        when (val outcome = dndController.toggleDnd()) {
            is DndToggleOutcome.Updated -> {
                permissionRefreshTrigger.value = System.currentTimeMillis()
                localStatusMessage.value = if (outcome.isDndEnabled) {
                    "Do Not Disturb enabled (Priority interruptions)."
                } else {
                    "Do Not Disturb disabled."
                }
            }
            is DndToggleOutcome.PermissionRequired -> {
                dndController.openPolicyAccessSettings()
                localStatusMessage.value = "Grant Do Not Disturb access in System Settings."
            }
            is DndToggleOutcome.Error -> {
                localStatusMessage.value = outcome.message
            }
        }
    }

    fun toggleGamingSidebar() {
        if (mediaCaptureController.isSidebarActive.value) {
            mediaCaptureController.stopGamingSidebar()
            localStatusMessage.value = "Floating Gaming Sidebar stopped."
            return
        }
        if (!mediaCaptureController.hasOverlayPermission()) {
            settingsNavigator.launchManageOverlaySettings()
            localStatusMessage.value = "Enable 'Display over other apps' for the optional Gaming Sidebar."
            return
        }
        val started = mediaCaptureController.startGamingSidebar()
        localStatusMessage.value = if (started) {
            "Floating Gaming Sidebar active."
        } else {
            "Unable to start Gaming Sidebar on this device."
        }
    }

    fun openOverlayPermissionSettings() {
        settingsNavigator.launchManageOverlaySettings()
    }

    fun openWriteSettingsPermission() {
        settingsNavigator.launchManageWriteSettings()
    }

    fun setSystemBrightness(value: Int) {
        viewModelScope.launch {
            val ok = telemetryRepository.setSystemBrightness(value)
            if (!ok) {
                localStatusMessage.value = "Grant Modify System Settings permission to adjust brightness."
            }
        }
    }

    fun createMediaProjectionIntent(): Intent? {
        return mediaCaptureController.createScreenCaptureConsentIntent()
    }

    fun onScreenshotConsentResult(resultCode: Int, data: Intent?) {
        mediaCaptureController.takeScreenshotWithConsent(resultCode, data)
    }

    fun onRecordingConsentResult(resultCode: Int, data: Intent?) {
        mediaCaptureController.startRecordingWithConsent(resultCode, data)
    }

    fun stopScreenRecording() {
        mediaCaptureController.stopScreenRecording()
    }

    fun openDisplaySettings() {
        val shortcut = settingsNavigator
            .getAvailableShortcuts(uiState.value.telemetry.device.isXiaomiOrHyperOs)
            .firstOrNull { it.id == "display_settings" }
        if (shortcut != null) {
            settingsNavigator.launchShortcut(shortcut)
        }
    }

    fun clearStatusMessage() {
        localStatusMessage.value = null
        mediaCaptureController.clearCaptureMessages()
    }

    private data class BaseTurboBundle(
        val snapshot: SystemTelemetrySnapshot,
        val prefs: com.example.data.preferences.UserPreferences,
        val gamesLoaded: Boolean,
        val apps: List<InstalledAppItem>,
        val capture: CaptureStatusState,
        val sidebarRunning: Boolean
    )

    class Factory(
        private val telemetryRepository: TelemetryRepository,
        private val gamesRepository: GamesRepository,
        private val preferencesRepository: UserPreferencesRepository,
        private val dndController: DndController,
        private val mediaCaptureController: MediaCaptureController,
        private val settingsNavigator: SystemSettingsNavigator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameTurboViewModel(
                telemetryRepository,
                gamesRepository,
                preferencesRepository,
                dndController,
                mediaCaptureController,
                settingsNavigator
            ) as T
        }
    }
}

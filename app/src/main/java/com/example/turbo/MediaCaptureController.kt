package com.example.turbo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import com.example.domain.model.CaptureStatusState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MediaCaptureController(private val context: Context) {

    private val _captureState = MutableStateFlow(CaptureStatusState())
    val captureState: StateFlow<CaptureStatusState> = _captureState.asStateFlow()

    private val _isSidebarActive = MutableStateFlow(false)
    val isSidebarActive: StateFlow<Boolean> = _isSidebarActive.asStateFlow()

    fun hasOverlayPermission(): Boolean {
        return runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)
    }

    fun createScreenCaptureConsentIntent(): Intent? {
        val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            ?: return null
        return mgr.createScreenCaptureIntent()
    }

    fun startRecordingWithConsent(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            _captureState.update { it.copy(lastError = "Screen capture permission was cancelled.") }
            return
        }
        val serviceIntent = ScreenCaptureService.buildStartRecordIntent(context, resultCode, data)
        runCatching {
            context.startForegroundService(serviceIntent)
        }.onFailure { err ->
            _captureState.update { it.copy(lastError = err.localizedMessage ?: "Failed to start screen recording.") }
        }
    }

    fun takeScreenshotWithConsent(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            _captureState.update { it.copy(lastError = "Screenshot permission was cancelled.") }
            return
        }
        val serviceIntent = ScreenCaptureService.buildScreenshotIntent(context, resultCode, data)
        runCatching {
            context.startForegroundService(serviceIntent)
        }.onFailure { err ->
            _captureState.update { it.copy(lastError = err.localizedMessage ?: "Failed to capture screenshot.") }
        }
    }

    fun stopScreenRecording() {
        val stopIntent = ScreenCaptureService.buildStopIntent(context)
        runCatching { context.startService(stopIntent) }
    }

    fun launchConsentFromOverlay(mode: String) {
        val intent = CaptureConsentActivity.buildIntent(context, mode)
        runCatching { context.startActivity(intent) }
    }

    fun startGamingSidebar(): Boolean {
        if (!hasOverlayPermission()) return false
        val intent = Intent(context, GamingSidebarService::class.java)
        return runCatching {
            context.startForegroundService(intent)
            true
        }.getOrDefault(false)
    }

    fun stopGamingSidebar() {
        val intent = Intent(context, GamingSidebarService::class.java).apply {
            action = GamingSidebarService.ACTION_STOP
        }
        runCatching { context.startService(intent) }
        _isSidebarActive.value = false
    }

    fun setSidebarRunning(running: Boolean) {
        _isSidebarActive.value = running
    }

    fun updateRecording(recording: Boolean, savedMessage: String? = null, error: String? = null) {
        _captureState.update {
            it.copy(
                isRecordingScreen = recording,
                lastSavedMediaMessage = savedMessage ?: it.lastSavedMediaMessage,
                lastError = error
            )
        }
    }

    fun updateScreenshotState(capturing: Boolean, savedMessage: String? = null, error: String? = null) {
        _captureState.update {
            it.copy(
                isCapturingScreenshot = capturing,
                lastSavedMediaMessage = savedMessage ?: it.lastSavedMediaMessage,
                lastError = error
            )
        }
    }

    fun clearCaptureMessages() {
        _captureState.update { it.copy(lastSavedMediaMessage = null, lastError = null) }
    }
}

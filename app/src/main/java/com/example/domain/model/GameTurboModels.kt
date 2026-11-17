package com.example.domain.model

enum class TurboPerformanceProfile(
    val title: String,
    val subtitle: String,
    val pollIntervalMs: Long,
    val reduceVisualEffects: Boolean,
    val pauseBackgroundHistory: Boolean
) {
    BATTERY_SAVER(
        title = "Battery Saver",
        subtitle = "5s telemetry cadence • Reduced UI motion • Minimal history overhead",
        pollIntervalMs = 5_000L,
        reduceVisualEffects = true,
        pauseBackgroundHistory = true
    ),
    BALANCED(
        title = "Balanced",
        subtitle = "2.5s telemetry cadence • Standard animations • Continuous history",
        pollIntervalMs = 2_500L,
        reduceVisualEffects = false,
        pauseBackgroundHistory = false
    ),
    PERFORMANCE(
        title = "Performance",
        subtitle = "1s high-cadence monitoring • Responsive spring UI • Full telemetry",
        pollIntervalMs = 1_000L,
        reduceVisualEffects = false,
        pauseBackgroundHistory = false
    )
}

data class GameSessionRecord(
    val id: String,
    val packageName: String,
    val appLabel: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val profileUsed: TurboPerformanceProfile = TurboPerformanceProfile.BALANCED
) {
    fun durationSeconds(nowMillis: Long = System.currentTimeMillis()): Long {
        val end = endedAtMillis ?: nowMillis
        return ((end - startedAtMillis) / 1_000L).coerceAtLeast(0L)
    }
}

data class CaptureStatusState(
    val isRecordingScreen: Boolean = false,
    val isCapturingScreenshot: Boolean = false,
    val lastSavedMediaMessage: String? = null,
    val lastError: String? = null
)

data class DndStatusState(
    val hasPolicyAccess: Boolean = false,
    val isDndEnabled: Boolean = false
)

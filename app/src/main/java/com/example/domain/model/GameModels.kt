package com.example.domain.model

import android.graphics.drawable.Drawable

enum class GameFilter {
    DETECTED_GAMES,
    FAVORITES,
    ALL_APPS
}

data class GameProfile(
    val packageName: String,
    val targetRefreshNote: String = "Auto",
    val enableDndReminder: Boolean = false,
    val brightnessChecklistNote: String = "",
    val preferredTurboProfile: TurboPerformanceProfile? = null
)

data class InstalledAppItem(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val isAndroidGameCategory: Boolean,
    val isFavorite: Boolean,
    val lastLaunchedMillis: Long?,
    val profile: GameProfile?,
    val iconDrawable: Drawable? = null
)

data class GameCenterState(
    val apps: List<InstalledAppItem> = emptyList(),
    val recentApps: List<InstalledAppItem> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: GameFilter = GameFilter.DETECTED_GAMES,
    val isLoading: Boolean = true,
    val statusMessage: String? = null
)

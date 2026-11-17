package com.example.domain.model

enum class ShortcutCategory {
    PERFORMANCE_DEV,
    DISPLAY_MOTION,
    POWER_BATTERY,
    CONNECTIVITY_AUDIO,
    DEVICE_APPS
}

data class SystemSettingsShortcut(
    val id: String,
    val title: String,
    val subtitle: String,
    val action: String,
    val category: ShortcutCategory,
    val isDirectSupported: Boolean,
    val oemRestrictionNote: String? = null
)

sealed interface ShortcutLaunchResult {
    data class LaunchedDirect(val shortcutTitle: String) : ShortcutLaunchResult
    data class LaunchedFallback(val shortcutTitle: String, val reason: String) : ShortcutLaunchResult
    data class Unavailable(val shortcutTitle: String, val reason: String) : ShortcutLaunchResult
}

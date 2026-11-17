package com.example.data.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.example.domain.model.ShortcutCategory
import com.example.domain.model.ShortcutLaunchResult
import com.example.domain.model.SystemSettingsShortcut

class SystemSettingsNavigator(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun getAvailableShortcuts(isXiaomiOrHyperOs: Boolean): List<SystemSettingsShortcut> {
        val rawDefinitions = listOf(
            ShortcutDef(
                id = "dev_options",
                title = "Developer Options",
                subtitle = "GPU debug, HW overlays, animation scales & logger buffer",
                action = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
                category = ShortcutCategory.PERFORMANCE_DEV,
                oemNote = if (isXiaomiOrHyperOs) {
                    "Requires unlocking Developer Options (tap OS version 7x in Device Info) on HyperOS/MIUI."
                } else {
                    "Requires Developer Options enabled in Device Info."
                }
            ),
            ShortcutDef(
                id = "display_settings",
                title = "Display & Refresh Rate",
                subtitle = "Screen refresh rate, adaptive brightness, color scheme & dark mode",
                action = Settings.ACTION_DISPLAY_SETTINGS,
                category = ShortcutCategory.DISPLAY_MOTION
            ),
            ShortcutDef(
                id = "animation_settings",
                title = "Animation & Motion Settings",
                subtitle = "Accessibility remove-animations toggle for smoother transitions",
                action = Settings.ACTION_ACCESSIBILITY_SETTINGS,
                category = ShortcutCategory.DISPLAY_MOTION,
                oemNote = "Global 0.5x scales live inside Developer Options; Accessibility offers Reduce Motion."
            ),
            ShortcutDef(
                id = "battery_settings",
                title = "Battery & Power Usage",
                subtitle = "App battery restrictions, charging metrics & power profiles",
                action = Intent.ACTION_POWER_USAGE_SUMMARY,
                fallbackAction = Settings.ACTION_BATTERY_SAVER_SETTINGS,
                category = ShortcutCategory.POWER_BATTERY,
                oemNote = if (isXiaomiOrHyperOs) {
                    "HyperOS may route power usage through Security Center; falls back to Battery Saver."
                } else null
            ),
            ShortcutDef(
                id = "app_management",
                title = "App Management",
                subtitle = "Manage background restrictions, cache & app permissions",
                action = Settings.ACTION_APPLICATION_SETTINGS,
                category = ShortcutCategory.DEVICE_APPS
            ),
            ShortcutDef(
                id = "network_settings",
                title = "Network & Internet",
                subtitle = "Wi-Fi band preferences, Private DNS, cellular & airplane controls",
                action = Settings.ACTION_WIRELESS_SETTINGS,
                fallbackAction = Settings.ACTION_WIFI_SETTINGS,
                category = ShortcutCategory.CONNECTIVITY_AUDIO
            ),
            ShortcutDef(
                id = "sound_settings",
                title = "Sound & Haptics",
                subtitle = "Media volume, Do Not Disturb, vibration & audio effects",
                action = Settings.ACTION_SOUND_SETTINGS,
                category = ShortcutCategory.CONNECTIVITY_AUDIO
            ),
            ShortcutDef(
                id = "device_info",
                title = "Device Information",
                subtitle = "Android security patch, kernel version, build number & hardware specs",
                action = Settings.ACTION_DEVICE_INFO_SETTINGS,
                category = ShortcutCategory.DEVICE_APPS
            )
        )

        return rawDefinitions.map { def ->
            val supported = isActionResolvable(def.action) ||
                (def.fallbackAction != null && isActionResolvable(def.fallbackAction))
            SystemSettingsShortcut(
                id = def.id,
                title = def.title,
                subtitle = def.subtitle,
                action = def.action,
                category = def.category,
                isDirectSupported = supported,
                oemRestrictionNote = def.oemNote
            )
        }
    }

    fun launchShortcut(shortcut: SystemSettingsShortcut): ShortcutLaunchResult {
        val primaryIntent = Intent(shortcut.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (isIntentResolvable(primaryIntent)) {
            val ok = runCatching {
                context.startActivity(primaryIntent)
                true
            }.getOrDefault(false)
            if (ok) {
                return ShortcutLaunchResult.LaunchedDirect(shortcut.title)
            }
        }

        // Secondary fallback for battery / network variants
        val secondaryAction = when (shortcut.id) {
            "battery_settings" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "network_settings" -> Settings.ACTION_WIFI_SETTINGS
            else -> null
        }
        if (secondaryAction != null) {
            val secondaryIntent = Intent(secondaryAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (isIntentResolvable(secondaryIntent)) {
                val ok = runCatching {
                    context.startActivity(secondaryIntent)
                    true
                }.getOrDefault(false)
                if (ok) {
                    return ShortcutLaunchResult.LaunchedFallback(
                        shortcutTitle = shortcut.title,
                        reason = "Opened compatible system sub-panel."
                    )
                }
            }
        }

        // Safe fallback to general Settings
        val settingsIntent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (isIntentResolvable(settingsIntent)) {
            val ok = runCatching {
                context.startActivity(settingsIntent)
                true
            }.getOrDefault(false)
            if (ok) {
                return ShortcutLaunchResult.LaunchedFallback(
                    shortcutTitle = shortcut.title,
                    reason = "Direct panel restricted on this ROM; opened System Settings."
                )
            }
        }

        return ShortcutLaunchResult.Unavailable(
            shortcutTitle = shortcut.title,
            reason = "No compatible Settings handler found on this device."
        )
    }

    fun launchManageWriteSettings(): Boolean {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (isIntentResolvable(intent)) {
            return runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
        return launchAppDetailsSettings()
    }

    fun launchManageOverlaySettings(): Boolean {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (isIntentResolvable(intent)) {
            return runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
        return launchAppDetailsSettings()
    }

    fun launchAppDetailsSettings(): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (isIntentResolvable(intent)) {
            return runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }
        return false
    }

    private fun isActionResolvable(action: String): Boolean {
        return isIntentResolvable(Intent(action))
    }

    private fun isIntentResolvable(intent: Intent): Boolean {
        return runCatching {
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        }.getOrDefault(false)
    }

    private data class ShortcutDef(
        val id: String,
        val title: String,
        val subtitle: String,
        val action: String,
        val fallbackAction: String? = null,
        val category: ShortcutCategory,
        val oemNote: String? = null
    )
}

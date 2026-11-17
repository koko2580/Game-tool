package com.example.data.system

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.example.domain.model.DndStatusState

sealed interface DndToggleOutcome {
    data class Updated(val isDndEnabled: Boolean) : DndToggleOutcome
    data object PermissionRequired : DndToggleOutcome
    data class Error(val message: String) : DndToggleOutcome
}

class DndController(private val context: Context) {

    private val notificationManager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    fun readStatus(): DndStatusState {
        val nm = notificationManager ?: return DndStatusState(hasPolicyAccess = false, isDndEnabled = false)
        val hasAccess = runCatching { nm.isNotificationPolicyAccessGranted }.getOrDefault(false)
        val enabled = if (hasAccess) {
            runCatching {
                nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            }.getOrDefault(false)
        } else {
            false
        }
        return DndStatusState(hasPolicyAccess = hasAccess, isDndEnabled = enabled)
    }

    fun toggleDnd(): DndToggleOutcome {
        val nm = notificationManager ?: return DndToggleOutcome.Error("NotificationManager unavailable")
        val hasAccess = runCatching { nm.isNotificationPolicyAccessGranted }.getOrDefault(false)
        if (!hasAccess) {
            return DndToggleOutcome.PermissionRequired
        }
        return runCatching {
            val currentlyEnabled = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            val targetFilter = if (currentlyEnabled) {
                NotificationManager.INTERRUPTION_FILTER_ALL
            } else {
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            }
            nm.setInterruptionFilter(targetFilter)
            DndToggleOutcome.Updated(isDndEnabled = !currentlyEnabled)
        }.getOrElse { error ->
            DndToggleOutcome.Error(error.localizedMessage ?: "Unable to update Do Not Disturb mode.")
        }
    }

    fun openPolicyAccessSettings(): Boolean {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolvable = runCatching {
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        }.getOrDefault(false)

        if (resolvable) {
            return runCatching {
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }

        val fallback = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(fallback)
            true
        }.getOrDefault(false)
    }
}

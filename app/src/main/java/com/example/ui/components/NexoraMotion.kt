package com.example.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalHapticsPreference = staticCompositionLocalOf { true }

@Composable
fun ProvideMotionAndHaptics(
    userReducedMotionOverride: Boolean,
    userHapticsEnabled: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemAnimationsDisabled = remember(context) {
        runCatching {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        }.getOrDefault(false)
    }

    CompositionLocalProvider(
        LocalReducedMotion provides (userReducedMotionOverride || systemAnimationsDisabled),
        LocalHapticsPreference provides userHapticsEnabled,
        content = content
    )
}

@Composable
fun rememberNexoraHaptics(): () -> Unit {
    val hapticsEnabled = LocalHapticsPreference.current
    val haptic = LocalHapticFeedback.current
    return remember(hapticsEnabled, haptic) {
        {
            if (hapticsEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }
}

package com.example.domain

import java.util.Locale

object FormatUtils {
    fun formatMemoryBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format(Locale.US, "%.2f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            String.format(Locale.US, "%.0f MB", mb)
        }
    }

    fun formatRefreshRate(hz: Float): String {
        val rounded = kotlin.math.round(hz)
        return if (kotlin.math.abs(hz - rounded) < 0.1f) {
            "${rounded.toInt()} Hz"
        } else {
            String.format(Locale.US, "%.1f Hz", hz)
        }
    }

    fun formatBandwidthKbps(kbps: Int?): String {
        if (kbps == null || kbps <= 0) return "Unavailable"
        return if (kbps >= 1000) {
            String.format(Locale.US, "%.1f Mbps", kbps / 1000.0)
        } else {
            "$kbps Kbps"
        }
    }

    fun formatTemperature(tempC: Float?): String {
        if (tempC == null) return "Unavailable"
        return String.format(Locale.US, "%.1f°C", tempC)
    }

    fun formatDurationTimer(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val hours = safe / 3600L
        val minutes = (safe % 3600L) / 60L
        val seconds = safe % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}

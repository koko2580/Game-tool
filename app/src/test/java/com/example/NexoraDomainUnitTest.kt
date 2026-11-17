package com.example

import com.example.domain.FormatUtils
import com.example.domain.model.GameFilter
import com.example.domain.model.GameSessionRecord
import com.example.domain.model.InstalledAppItem
import com.example.domain.model.MemoryTelemetry
import com.example.domain.model.TurboPerformanceProfile
import com.example.ui.viewmodel.GameCenterViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NexoraDomainUnitTest {

    @Test
    fun `formatMemoryBytes formats MB and GB accurately`() {
        assertEquals("0 MB", FormatUtils.formatMemoryBytes(0L))
        assertEquals("512 MB", FormatUtils.formatMemoryBytes(512L * 1024L * 1024L))
        assertEquals("4.00 GB", FormatUtils.formatMemoryBytes(4L * 1024L * 1024L * 1024L))
    }

    @Test
    fun `formatRefreshRate formats integer and fractional refresh rates`() {
        assertEquals("120 Hz", FormatUtils.formatRefreshRate(120.0f))
        assertEquals("90 Hz", FormatUtils.formatRefreshRate(89.98f))
        assertEquals("144.5 Hz", FormatUtils.formatRefreshRate(144.5f))
    }

    @Test
    fun `formatDurationTimer formats mm_ss and hh_mm_ss`() {
        assertEquals("00:00", FormatUtils.formatDurationTimer(0L))
        assertEquals("01:05", FormatUtils.formatDurationTimer(65L))
        assertEquals("01:02:03", FormatUtils.formatDurationTimer(3723L))
    }

    @Test
    fun `memory telemetry usedPercent handles zero and normal totals`() {
        val zeroMem = MemoryTelemetry(0L, 0L, 0L, 0L, false)
        assertEquals(0f, zeroMem.usedPercent, 0.001f)

        val halfMem = MemoryTelemetry(4_000L, 8_000L, 4_000L, 1_000L, false)
        assertEquals(0.5f, halfMem.usedPercent, 0.001f)
    }

    @Test
    fun `turbo profiles configure distinct real polling intervals and motion flags`() {
        assertEquals(5_000L, TurboPerformanceProfile.BATTERY_SAVER.pollIntervalMs)
        assertTrue(TurboPerformanceProfile.BATTERY_SAVER.reduceVisualEffects)
        assertEquals(2_500L, TurboPerformanceProfile.BALANCED.pollIntervalMs)
        assertFalse(TurboPerformanceProfile.BALANCED.reduceVisualEffects)
        assertEquals(1_000L, TurboPerformanceProfile.PERFORMANCE.pollIntervalMs)
        assertFalse(TurboPerformanceProfile.PERFORMANCE.reduceVisualEffects)
    }

    @Test
    fun `game session record calculates duration accurately`() {
        val session = GameSessionRecord(
            id = "s1",
            packageName = "com.game.test",
            appLabel = "Test Game",
            startedAtMillis = 10_000L,
            endedAtMillis = 75_000L,
            profileUsed = TurboPerformanceProfile.PERFORMANCE
        )
        assertEquals(65L, session.durationSeconds())
    }

    @Test
    fun `game center filter and search sorts favorites and games cleanly`() {
        val sampleApps = listOf(
            InstalledAppItem("com.notes.app", "Notes", "1.0", isAndroidGameCategory = false, isFavorite = false, lastLaunchedMillis = null, profile = null),
            InstalledAppItem("com.game.alpha", "Alpha Racer", "2.1", isAndroidGameCategory = true, isFavorite = false, lastLaunchedMillis = null, profile = null),
            InstalledAppItem("com.custom.fav", "Discord Overlay", "3.0", isAndroidGameCategory = false, isFavorite = true, lastLaunchedMillis = null, profile = null)
        )

        val detectedGames = GameCenterViewModel.filterAndSearchApps(sampleApps, "", GameFilter.DETECTED_GAMES)
        assertEquals(listOf("com.custom.fav", "com.game.alpha"), detectedGames.map { it.packageName })

        val favorites = GameCenterViewModel.filterAndSearchApps(sampleApps, "", GameFilter.FAVORITES)
        assertEquals(listOf("com.custom.fav"), favorites.map { it.packageName })

        val searchedAll = GameCenterViewModel.filterAndSearchApps(sampleApps, "notes", GameFilter.ALL_APPS)
        assertEquals(listOf("com.notes.app"), searchedAll.map { it.packageName })
    }
}

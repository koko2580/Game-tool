package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.system.DeviceTelemetryManager
import com.example.data.system.SystemSettingsNavigator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NEXORA X", appName)
    }

    @Test
    fun `device telemetry manager reads non-empty identity and cpu cores`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = DeviceTelemetryManager(context)

        val identity = manager.readDeviceIdentity()
        val cpu = manager.readCpuHardwareInfo()
        val memory = manager.readMemoryTelemetry()

        assertTrue(identity.sdkInt >= 26)
        assertTrue(cpu.coreCount >= 1)
        assertTrue(memory.availableBytes >= 0L)
        assertTrue(memory.totalBytes >= memory.availableBytes)
    }

    @Test
    fun `system settings navigator builds all required shortcuts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val navigator = SystemSettingsNavigator(context)
        val shortcuts = navigator.getAvailableShortcuts(isXiaomiOrHyperOs = false)

        val ids = shortcuts.map { it.id }.toSet()
        assertTrue(ids.contains("dev_options"))
        assertTrue(ids.contains("display_settings"))
        assertTrue(ids.contains("battery_settings"))
        assertTrue(ids.contains("app_management"))
        assertTrue(ids.contains("network_settings"))
        assertTrue(ids.contains("sound_settings"))
        assertTrue(ids.contains("animation_settings"))
        assertTrue(ids.contains("device_info"))
    }
}

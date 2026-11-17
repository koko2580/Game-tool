package com.example.data.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.Display
import com.example.domain.model.BatteryTelemetry
import com.example.domain.model.CpuHardwareInfo
import com.example.domain.model.DeviceIdentity
import com.example.domain.model.DisplayModeSummary
import com.example.domain.model.DisplayTelemetry
import com.example.domain.model.MemoryTelemetry
import com.example.domain.model.ThermalLevel
import com.example.domain.model.ThermalTelemetry
import java.io.File

class DeviceTelemetryManager(private val context: Context) {

    private val activityManager: ActivityManager? =
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private val batteryManager: BatteryManager? =
        context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    private val powerManager: PowerManager? =
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val displayManager: DisplayManager? =
        context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    fun readDeviceIdentity(): DeviceIdentity {
        val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val lowerMfg = manufacturer.lowercase()
        val miuiVersion = readSystemProperty("ro.miui.ui.version.name")
        val hyperOsVersion = readSystemProperty("ro.mi.os.version.name")
        val isXiaomi = lowerMfg.contains("xiaomi") ||
            lowerMfg.contains("redmi") ||
            lowerMfg.contains("poco") ||
            !miuiVersion.isNullOrBlank() ||
            !hyperOsVersion.isNullOrBlank()

        val uiFlavor = when {
            !hyperOsVersion.isNullOrBlank() -> "HyperOS $hyperOsVersion"
            !miuiVersion.isNullOrBlank() -> "MIUI $miuiVersion"
            isXiaomi -> "HyperOS / MIUI"
            else -> null
        }

        return DeviceIdentity(
            manufacturer = manufacturer.ifBlank { "Android" },
            model = Build.MODEL.orEmpty().ifBlank { "Unknown Device" },
            deviceCode = Build.DEVICE.orEmpty(),
            hardware = Build.HARDWARE.orEmpty(),
            board = Build.BOARD.orEmpty(),
            androidVersion = Build.VERSION.RELEASE.orEmpty(),
            sdkInt = Build.VERSION.SDK_INT,
            buildDisplay = Build.DISPLAY.orEmpty(),
            isXiaomiOrHyperOs = isXiaomi,
            osUiFlavor = uiFlavor
        )
    }

    fun readMemoryTelemetry(): MemoryTelemetry {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val total = memInfo.totalMem.coerceAtLeast(0L)
        val avail = memInfo.availMem.coerceAtLeast(0L)
        val used = (total - avail).coerceAtLeast(0L)
        return MemoryTelemetry(
            availableBytes = avail,
            totalBytes = total,
            usedBytes = used,
            thresholdBytes = memInfo.threshold,
            isLowMemory = memInfo.lowMemory
        )
    }

    fun readBatteryTelemetry(): BatteryTelemetry {
        val intent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val computedPercent = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale.toFloat()).toInt().coerceIn(0, 100)
        } else {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                ?.takeIf { it in 0..100 } ?: 0
        }

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val statusLabel = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val plugSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            BatteryManager.BATTERY_PLUGGED_DOCK -> "Dock"
            else -> "Battery"
        }

        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val healthLabel = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?: Int.MIN_VALUE
        val tempC = if (tempTenths != Int.MIN_VALUE && tempTenths > -500 && tempTenths < 1500) {
            tempTenths / 10f
        } else {
            null
        }

        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }
        val currentUa = runCatching {
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                ?.takeIf { it != Int.MIN_VALUE }
        }.getOrNull()

        val technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() }

        return BatteryTelemetry(
            levelPercent = computedPercent,
            isCharging = isCharging,
            chargePlugSource = plugSource,
            statusLabel = statusLabel,
            healthLabel = healthLabel,
            temperatureCelsius = tempC,
            voltageMillivolts = voltageMv,
            currentMicroAmps = currentUa,
            technology = technology
        )
    }

    fun readThermalTelemetry(): ThermalTelemetry {
        val pm = powerManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || pm == null) {
            return ThermalTelemetry(
                level = ThermalLevel.UNAVAILABLE,
                headroomSeconds10 = null,
                supportedByPlatform = false
            )
        }

        val level = runCatching {
            when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> ThermalLevel.NONE
                PowerManager.THERMAL_STATUS_LIGHT -> ThermalLevel.LIGHT
                PowerManager.THERMAL_STATUS_MODERATE -> ThermalLevel.MODERATE
                PowerManager.THERMAL_STATUS_SEVERE -> ThermalLevel.SEVERE
                PowerManager.THERMAL_STATUS_CRITICAL -> ThermalLevel.CRITICAL
                PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalLevel.EMERGENCY
                PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalLevel.SHUTDOWN
                else -> ThermalLevel.UNAVAILABLE
            }
        }.getOrDefault(ThermalLevel.UNAVAILABLE)

        val headroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                pm.getThermalHeadroom(10).takeIf { !it.isNaN() && it >= 0f }
            }.getOrNull()
        } else {
            null
        }

        return ThermalTelemetry(
            level = level,
            headroomSeconds10 = headroom,
            supportedByPlatform = level != ThermalLevel.UNAVAILABLE
        )
    }

    fun readDisplayTelemetry(): DisplayTelemetry {
        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        val activeMode = display?.mode
        val supported = display?.supportedModes?.map { mode ->
            DisplayModeSummary(
                modeId = mode.modeId,
                widthPx = mode.physicalWidth,
                heightPx = mode.physicalHeight,
                refreshRateHz = mode.refreshRate,
                isActive = activeMode?.modeId == mode.modeId
            )
        }?.sortedByDescending { it.refreshRateHz } ?: emptyList()

        val hdrLabels = runCatching {
            @Suppress("DEPRECATION")
            val types = display?.hdrCapabilities?.supportedHdrTypes?.toList().orEmpty()
            types.mapNotNull { hdrType ->
                when (hdrType) {
                    Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                    Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                    Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                    Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
                    else -> null
                }
            }
        }.getOrDefault(emptyList())

        val brightness = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrNull()

        val autoBrightness = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) ==
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        }.getOrNull()

        val canWrite = runCatching { Settings.System.canWrite(context) }.getOrDefault(false)

        return DisplayTelemetry(
            currentRefreshRateHz = activeMode?.refreshRate ?: display?.refreshRate ?: 60f,
            widthPx = activeMode?.physicalWidth ?: 0,
            heightPx = activeMode?.physicalHeight ?: 0,
            supportedModes = supported,
            hdrCapabilities = hdrLabels,
            brightnessValue = brightness,
            isAutoBrightness = autoBrightness,
            canWriteSystemSettings = canWrite
        )
    }

    fun setSystemBrightness(brightness0To255: Int): Boolean {
        val canWrite = runCatching { Settings.System.canWrite(context) }.getOrDefault(false)
        if (!canWrite) return false
        val clamped = brightness0To255.coerceIn(10, 255)
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                clamped
            )
            true
        }.getOrDefault(false)
    }

    fun readCpuHardwareInfo(): CpuHardwareInfo {
        val coreCount = Runtime.getRuntime().availableProcessors()
        val abis = Build.SUPPORTED_ABIS?.toList().orEmpty()
        val hardwareName = runCatching {
            val file = File("/proc/cpuinfo")
            if (file.canRead()) {
                file.useLines { lines ->
                    lines.firstOrNull { line ->
                        line.startsWith("Hardware", ignoreCase = true) ||
                            line.startsWith("model name", ignoreCase = true)
                    }?.substringAfter(":")?.trim()?.takeIf { it.isNotBlank() }
                }
            } else {
                null
            }
        }.getOrNull() ?: Build.HARDWARE.takeIf { it.isNotBlank() }

        val freqs = (0 until coreCount.coerceAtMost(16)).mapNotNull { index ->
            runCatching {
                val freqFile = File("/sys/devices/system/cpu/cpu$index/cpufreq/scaling_cur_freq")
                if (freqFile.canRead()) {
                    freqFile.readText().trim().toIntOrNull()
                } else {
                    null
                }
            }.getOrNull()
        }

        return CpuHardwareInfo(
            coreCount = coreCount,
            supportedAbis = abis,
            hardwareName = hardwareName,
            readOnlyFrequenciesKhz = freqs
        )
    }

    private fun readSystemProperty(key: String): String? {
        return runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod = clazz.getMethod("get", String::class.java)
            (getMethod.invoke(null, key) as? String)?.trim()?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}

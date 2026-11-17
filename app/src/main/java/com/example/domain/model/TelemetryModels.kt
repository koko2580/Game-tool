package com.example.domain.model

enum class ThermalLevel(val label: String, val severityIndex: Int) {
    NONE("Nominal", 0),
    LIGHT("Light Load", 1),
    MODERATE("Warm", 2),
    SEVERE("Throttling", 3),
    CRITICAL("Critical", 4),
    EMERGENCY("Emergency", 5),
    SHUTDOWN("Shutdown Imminent", 6),
    UNAVAILABLE("Unavailable", -1)
}

data class DeviceIdentity(
    val manufacturer: String,
    val model: String,
    val deviceCode: String,
    val hardware: String,
    val board: String,
    val androidVersion: String,
    val sdkInt: Int,
    val buildDisplay: String,
    val isXiaomiOrHyperOs: Boolean,
    val osUiFlavor: String?
)

data class MemoryTelemetry(
    val availableBytes: Long,
    val totalBytes: Long,
    val usedBytes: Long,
    val thresholdBytes: Long,
    val isLowMemory: Boolean
) {
    val usedPercent: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class BatteryTelemetry(
    val levelPercent: Int,
    val isCharging: Boolean,
    val chargePlugSource: String,
    val statusLabel: String,
    val healthLabel: String,
    val temperatureCelsius: Float?,
    val voltageMillivolts: Int?,
    val currentMicroAmps: Int?,
    val technology: String?
)

data class ThermalTelemetry(
    val level: ThermalLevel,
    val headroomSeconds10: Float?,
    val supportedByPlatform: Boolean
)

data class DisplayModeSummary(
    val modeId: Int,
    val widthPx: Int,
    val heightPx: Int,
    val refreshRateHz: Float,
    val isActive: Boolean
)

data class DisplayTelemetry(
    val currentRefreshRateHz: Float,
    val widthPx: Int,
    val heightPx: Int,
    val supportedModes: List<DisplayModeSummary>,
    val hdrCapabilities: List<String>,
    val brightnessValue: Int?,
    val isAutoBrightness: Boolean?,
    val canWriteSystemSettings: Boolean
)

data class CpuHardwareInfo(
    val coreCount: Int,
    val supportedAbis: List<String>,
    val hardwareName: String?,
    val readOnlyFrequenciesKhz: List<Int>
)

data class NetworkLinkTelemetry(
    val isConnected: Boolean,
    val transportLabel: String,
    val isValidatedInternet: Boolean,
    val isMetered: Boolean,
    val downstreamKbps: Int?,
    val upstreamKbps: Int?
)

data class HttpLatencyResult(
    val endpointName: String,
    val url: String,
    val roundTripMs: Long?,
    val httpCode: Int?,
    val errorMessage: String?,
    val timestampMillis: Long
)

data class TelemetrySample(
    val timestampMillis: Long,
    val memoryUsedPercent: Float,
    val batteryTempCelsius: Float?,
    val batteryPercent: Int
)

data class SystemTelemetrySnapshot(
    val device: DeviceIdentity,
    val memory: MemoryTelemetry,
    val battery: BatteryTelemetry,
    val thermal: ThermalTelemetry,
    val display: DisplayTelemetry,
    val cpu: CpuHardwareInfo,
    val network: NetworkLinkTelemetry,
    val history: List<TelemetrySample>,
    val lastUpdatedMillis: Long
)

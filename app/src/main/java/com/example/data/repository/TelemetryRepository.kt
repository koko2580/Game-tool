package com.example.data.repository

import com.example.data.preferences.UserPreferencesRepository
import com.example.data.system.DeviceTelemetryManager
import com.example.data.system.NetworkTelemetryManager
import com.example.domain.model.HttpLatencyResult
import com.example.domain.model.NetworkLinkTelemetry
import com.example.domain.model.SystemTelemetrySnapshot
import com.example.domain.model.TelemetrySample
import com.example.domain.model.TurboPerformanceProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryRepository(
    private val deviceTelemetryManager: DeviceTelemetryManager,
    private val networkTelemetryManager: NetworkTelemetryManager,
    userPreferencesRepository: UserPreferencesRepository
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val manualTrigger = MutableStateFlow(0L)
    private val sampleHistory = ArrayDeque<TelemetrySample>(MAX_HISTORY_SAMPLES)
    private val historyLock = Any()

    private val cachedDeviceIdentity by lazy { deviceTelemetryManager.readDeviceIdentity() }
    private val cachedCpuInfo by lazy { deviceTelemetryManager.readCpuHardwareInfo() }

    private val profileFlow: StateFlow<TurboPerformanceProfile> = userPreferencesRepository.preferencesFlow
        .map { it.turboProfile }
        .distinctUntilChanged()
        .stateIn(
            scope = repoScope,
            started = SharingStarted.WhileSubscribed(2_000L),
            initialValue = TurboPerformanceProfile.BALANCED
        )

    private val dynamicPollingFlow = profileFlow.flatMapLatest { profile ->
        flow {
            while (currentCoroutineContext().isActive) {
                emit(profile to System.currentTimeMillis())
                delay(profile.pollIntervalMs)
            }
        }
    }

    val telemetrySnapshot: StateFlow<SystemTelemetrySnapshot> = combine(
        dynamicPollingFlow,
        manualTrigger,
        networkTelemetryManager.observeNetworkLink()
    ) { (profile, _), _, network ->
        collectSnapshot(network, recordHistory = !profile.pauseBackgroundHistory)
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = repoScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 2_000L),
            initialValue = collectSnapshot(
                network = networkTelemetryManager.readCurrentNetworkLink(),
                recordHistory = true
            )
        )

    fun refreshNow() {
        manualTrigger.value = System.currentTimeMillis()
    }

    suspend fun setSystemBrightness(brightness0To255: Int): Boolean = withContext(Dispatchers.IO) {
        val ok = deviceTelemetryManager.setSystemBrightness(brightness0To255)
        if (ok) {
            refreshNow()
        }
        ok
    }

    suspend fun measureHttpLatency(endpointName: String, url: String): HttpLatencyResult {
        return networkTelemetryManager.measureHttpLatency(endpointName, url)
    }

    private fun collectSnapshot(
        network: NetworkLinkTelemetry,
        recordHistory: Boolean
    ): SystemTelemetrySnapshot {
        val now = System.currentTimeMillis()
        val memory = deviceTelemetryManager.readMemoryTelemetry()
        val battery = deviceTelemetryManager.readBatteryTelemetry()
        val thermal = deviceTelemetryManager.readThermalTelemetry()
        val display = deviceTelemetryManager.readDisplayTelemetry()

        val historyCopy = synchronized(historyLock) {
            if (recordHistory || sampleHistory.isEmpty()) {
                val sample = TelemetrySample(
                    timestampMillis = now,
                    memoryUsedPercent = memory.usedPercent,
                    batteryTempCelsius = battery.temperatureCelsius,
                    batteryPercent = battery.levelPercent
                )
                if (sampleHistory.size >= MAX_HISTORY_SAMPLES) {
                    sampleHistory.removeFirst()
                }
                sampleHistory.addLast(sample)
            }
            sampleHistory.toList()
        }

        return SystemTelemetrySnapshot(
            device = cachedDeviceIdentity,
            memory = memory,
            battery = battery,
            thermal = thermal,
            display = display,
            cpu = cachedCpuInfo,
            network = network,
            history = historyCopy,
            lastUpdatedMillis = now
        )
    }

    companion object {
        private const val MAX_HISTORY_SAMPLES = 24
    }
}

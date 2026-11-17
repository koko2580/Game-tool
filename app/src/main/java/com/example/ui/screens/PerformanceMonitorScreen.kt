package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.FormatUtils
import com.example.domain.model.ThermalLevel
import com.example.ui.components.NexoraCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.StatusPill
import com.example.ui.components.TelemetryLineChart
import com.example.ui.components.rememberNexoraHaptics
import com.example.ui.theme.LocalNexoraColors
import com.example.ui.theme.NexoraSpacing
import com.example.ui.viewmodel.PerformanceMonitorViewModel

@Composable
fun PerformanceMonitorScreen(
    viewModel: PerformanceMonitorViewModel,
    modifier: Modifier = Modifier
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val ext = LocalNexoraColors.current
    val triggerHaptic = rememberNexoraHaptics()

    val ramSeries = snapshot.history.map { it.memoryUsedPercent * 100f }
    val tempSeries = snapshot.history.mapNotNull { it.batteryTempCelsius }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(NexoraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NexoraSpacing.md)
        ) {
            item(key = "monitor_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.monitor_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusPill(
                            text = stringResource(R.string.monitor_live_status),
                            color = ext.healthy
                        )
                    }
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            viewModel.refreshNow()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("monitor_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Sample telemetry now",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // 1. RAM Historical Chart
            item(key = "ram_chart_card") {
                StaggeredEntrance(index = 0) {
                    NexoraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("monitor_ram_chart")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.monitor_ram_chart),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${FormatUtils.formatMemoryBytes(snapshot.memory.availableBytes)} free of ${FormatUtils.formatMemoryBytes(snapshot.memory.totalBytes)} • Low Memory: ${snapshot.memory.isLowMemory}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TelemetryLineChart(
                            values = ramSeries,
                            minValue = 0f,
                            maxValue = 100f,
                            lineColor = MaterialTheme.colorScheme.primary,
                            unitSuffix = "%"
                        )
                    }
                }
            }

            // 2. Battery Temperature Historical Chart
            item(key = "battery_chart_card") {
                StaggeredEntrance(index = 1) {
                    NexoraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("monitor_battery_chart")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BatteryChargingFull,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.monitor_battery_chart),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val currentMa = snapshot.battery.currentMicroAmps?.let { "${it / 1000} mA" } ?: "Current N/A"
                        val voltMv = snapshot.battery.voltageMillivolts?.let { "$it mV" } ?: "Voltage N/A"
                        Text(
                            text = "${snapshot.battery.levelPercent}% (${snapshot.battery.statusLabel}) • $voltMv • $currentMa",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (tempSeries.isNotEmpty()) {
                            TelemetryLineChart(
                                values = tempSeries,
                                minValue = 20f,
                                maxValue = 55f,
                                lineColor = MaterialTheme.colorScheme.secondary,
                                unitSuffix = "°C"
                            )
                        } else {
                            Text(
                                text = "Battery temperature sensor is unavailable on this device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. CPU Hardware & ABI Info
            item(key = "cpu_section") {
                StaggeredEntrance(index = 2) {
                    NexoraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("monitor_cpu_card")
                    ) {
                        SectionHeader(
                            title = stringResource(R.string.monitor_cpu_section),
                            trailingBadge = "${snapshot.cpu.coreCount} Cores",
                            badgeColor = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MetricDetailRow(
                            label = "Hardware",
                            value = snapshot.cpu.hardwareName ?: stringResource(R.string.status_unavailable)
                        )
                        MetricDetailRow(
                            label = "Supported ABIs",
                            value = snapshot.cpu.supportedAbis.joinToString(", ").ifBlank { stringResource(R.string.status_unavailable) }
                        )
                        MetricDetailRow(
                            label = "Read-Only Core Clocks",
                            value = if (snapshot.cpu.readOnlyFrequenciesKhz.isNotEmpty()) {
                                snapshot.cpu.readOnlyFrequenciesKhz.joinToString(" / ") { "${it / 1000} MHz" }
                            } else {
                                "Restricted by Android / SELinux (no-root)"
                            }
                        )
                    }
                }
            }

            // 4. Thermal & Display Info
            item(key = "thermal_display_section") {
                StaggeredEntrance(index = 3) {
                    val thermalColor = when (snapshot.thermal.level) {
                        ThermalLevel.NONE, ThermalLevel.LIGHT -> ext.healthy
                        ThermalLevel.MODERATE -> ext.warning
                        ThermalLevel.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> ext.critical
                    }
                    NexoraCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Thermostat,
                                contentDescription = null,
                                tint = thermalColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.monitor_thermal_section),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        MetricDetailRow(
                            label = "PowerManager Status",
                            value = snapshot.thermal.level.label
                        )
                        MetricDetailRow(
                            label = "10s Thermal Headroom",
                            value = snapshot.thermal.headroomSeconds10?.let {
                                String.format(java.util.Locale.US, "%.2f", it)
                            } ?: stringResource(R.string.status_unavailable)
                        )
                        MetricDetailRow(
                            label = "Display Refresh Rate",
                            value = FormatUtils.formatRefreshRate(snapshot.display.currentRefreshRateHz)
                        )
                    }
                }
            }

            // 5. Network Link Summary
            item(key = "network_section") {
                StaggeredEntrance(index = 4) {
                    NexoraCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.NetworkCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.monitor_network_section),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        MetricDetailRow(
                            label = "Active Transport",
                            value = snapshot.network.transportLabel
                        )
                        MetricDetailRow(
                            label = "Validated Internet",
                            value = if (snapshot.network.isValidatedInternet) "Yes" else "No"
                        )
                        MetricDetailRow(
                            label = "Down / Up Link Cap",
                            value = "${FormatUtils.formatBandwidthKbps(snapshot.network.downstreamKbps)} / ${
                                FormatUtils.formatBandwidthKbps(snapshot.network.upstreamKbps)
                            }"
                        )
                    }
                }
            }

            // 6. Transparency Disclaimer
            item(key = "monitor_disclaimer") {
                NexoraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.monitor_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.55f)
        )
    }
}

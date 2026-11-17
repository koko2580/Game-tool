package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.FormatUtils
import com.example.domain.model.InstalledAppItem
import com.example.domain.model.ThermalLevel
import com.example.ui.components.AnimatedIntCounter
import com.example.ui.components.NexoraCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.StatusPill
import com.example.ui.components.TelemetryRingGauge
import com.example.ui.components.rememberNexoraHaptics
import com.example.ui.theme.LocalNexoraColors
import com.example.ui.theme.NexoraSpacing
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToGameTurbo: () -> Unit,
    onNavigateToGames: () -> Unit,
    onNavigateToMonitor: () -> Unit,
    onNavigateToShortcuts: () -> Unit,
    onNavigateToNetworkLab: () -> Unit,
    onNavigateToDisplayLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.launchFeedback) {
        val msg = state.launchFeedback
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        DashboardContent(
            state = state,
            onRefresh = viewModel::refreshTelemetry,
            onLaunchRecent = viewModel::launchRecentGame,
            onNavigateToGameTurbo = onNavigateToGameTurbo,
            onNavigateToGames = onNavigateToGames,
            onNavigateToMonitor = onNavigateToMonitor,
            onNavigateToShortcuts = onNavigateToShortcuts,
            onNavigateToNetworkLab = onNavigateToNetworkLab,
            onNavigateToDisplayLab = onNavigateToDisplayLab,
            contentPadding = innerPadding
        )
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onRefresh: () -> Unit,
    onLaunchRecent: (InstalledAppItem) -> Unit,
    onNavigateToGameTurbo: () -> Unit,
    onNavigateToGames: () -> Unit,
    onNavigateToMonitor: () -> Unit,
    onNavigateToShortcuts: () -> Unit,
    onNavigateToNetworkLab: () -> Unit,
    onNavigateToDisplayLab: () -> Unit,
    contentPadding: PaddingValues
) {
    val ext = LocalNexoraColors.current
    val triggerHaptic = rememberNexoraHaptics()
    val snapshot = state.telemetry
    val device = snapshot.device
    val memory = snapshot.memory
    val battery = snapshot.battery
    val thermal = snapshot.thermal
    val display = snapshot.display

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = NexoraSpacing.md,
                vertical = NexoraSpacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(NexoraSpacing.md)
        ) {
            // 1. Device Identity Hero Card
            item(key = "device_hero") {
                StaggeredEntrance(index = 0) {
                    NexoraCard(
                        accentHighlight = true,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_device_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Smartphone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(NexoraSpacing.md))
                                Column {
                                    Text(
                                        text = "${device.manufacturer} ${device.model}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Android ${device.androidVersion} (API ${device.sdkInt}) • ${device.hardware.ifBlank { device.deviceCode }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    triggerHaptic()
                                    onRefresh()
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dashboard_refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Refresh telemetry",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(NexoraSpacing.md))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatusPill(
                                text = stringResource(R.string.dashboard_no_root_badge),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (device.isXiaomiOrHyperOs) {
                                StatusPill(
                                    text = device.osUiFlavor ?: stringResource(R.string.dashboard_hyperos_badge),
                                    color = ext.warning
                                )
                            }
                            StatusPill(
                                text = snapshot.network.transportLabel,
                                color = if (snapshot.network.isConnected) ext.healthy else ext.critical
                            )
                        }
                    }
                }
            }

            // 2. Live Telemetry Section Header
            item(key = "telemetry_header") {
                StaggeredEntrance(index = 1) {
                    SectionHeader(
                        title = stringResource(R.string.dashboard_telemetry_section),
                        trailingBadge = "Live API",
                        badgeColor = ext.healthy
                    )
                }
            }

            // 3. RAM & Battery Primary Telemetry Cards
            item(key = "primary_metrics") {
                StaggeredEntrance(index = 2) {
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.md)
                        ) {
                            MemoryMetricCard(
                                memory = memory,
                                onClick = onNavigateToMonitor,
                                modifier = Modifier.weight(1f)
                            )
                            BatteryMetricCard(
                                battery = battery,
                                onClick = onNavigateToMonitor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(NexoraSpacing.md)) {
                            MemoryMetricCard(
                                memory = memory,
                                onClick = onNavigateToMonitor,
                                modifier = Modifier.fillMaxWidth()
                            )
                            BatteryMetricCard(
                                battery = battery,
                                onClick = onNavigateToMonitor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 4. Thermal & Display Refresh Cards
            item(key = "secondary_metrics") {
                StaggeredEntrance(index = 3) {
                    val thermalColor = when (thermal.level) {
                        ThermalLevel.NONE, ThermalLevel.LIGHT -> ext.healthy
                        ThermalLevel.MODERATE -> ext.warning
                        ThermalLevel.SEVERE, ThermalLevel.CRITICAL, ThermalLevel.EMERGENCY, ThermalLevel.SHUTDOWN -> ext.critical
                        ThermalLevel.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.md)
                    ) {
                        NexoraCard(
                            onClick = onNavigateToMonitor,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_thermal_card")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Thermostat,
                                    contentDescription = null,
                                    tint = thermalColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.dashboard_thermal_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(NexoraSpacing.sm))
                            Text(
                                text = thermal.level.label,
                                style = MaterialTheme.typography.titleLarge,
                                color = thermalColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (thermal.headroomSeconds10 != null) {
                                    "Headroom: ${String.format(java.util.Locale.US, "%.0f%%", (1f - thermal.headroomSeconds10).coerceAtLeast(0f) * 100f)}"
                                } else if (battery.temperatureCelsius != null) {
                                    "Battery: ${FormatUtils.formatTemperature(battery.temperatureCelsius)}"
                                } else {
                                    "PowerManager API"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        NexoraCard(
                            onClick = onNavigateToDisplayLab,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_display_card")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.dashboard_display_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(NexoraSpacing.sm))
                            AnimatedIntCounter(
                                targetValue = kotlin.math.round(display.currentRefreshRateHz).toInt(),
                                suffix = " Hz",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (display.widthPx > 0) {
                                    "${display.widthPx}×${display.heightPx} • ${display.supportedModes.size} modes"
                                } else {
                                    "DisplayManager API"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 5. Recent Game Launches (only shown when user has launched apps from Game Center)
            if (state.recentGames.isNotEmpty()) {
                item(key = "recent_games_header") {
                    StaggeredEntrance(index = 4) {
                        SectionHeader(title = stringResource(R.string.dashboard_recent_games))
                    }
                }
                item(key = "recent_games_row") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                    ) {
                        items(state.recentGames, key = { it.packageName }) { game ->
                            NexoraCard(
                                onClick = { onLaunchRecent(game) },
                                contentPadding = PaddingValues(horizontal = NexoraSpacing.md, vertical = 12.dp),
                                modifier = Modifier.testTag("recent_game_${game.packageName}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(NexoraSpacing.sm))
                                    Text(
                                        text = game.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Quick Utility Shortcuts
            item(key = "quick_actions_header") {
                StaggeredEntrance(index = 5) {
                    SectionHeader(title = stringResource(R.string.dashboard_quick_actions))
                }
            }

            item(key = "quick_actions_grid") {
                StaggeredEntrance(index = 6) {
                    Column(verticalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)) {
                        QuickShortcutTile(
                            title = "Game Turbo Control",
                            subtitle = "Session timer, profiles, DND, floating sidebar & capture",
                            icon = Icons.Rounded.Bolt,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            onClick = onNavigateToGameTurbo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_game_turbo")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                        ) {
                            QuickShortcutTile(
                                title = "Hidden Settings",
                                subtitle = "Dev & System panels",
                                icon = Icons.Rounded.DeveloperMode,
                                accentColor = MaterialTheme.colorScheme.primary,
                                onClick = onNavigateToShortcuts,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_hidden_settings")
                            )
                            QuickShortcutTile(
                                title = "Game Center",
                                subtitle = "Launch & profiles",
                                icon = Icons.Rounded.SportsEsports,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                onClick = onNavigateToGames,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_game_center")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                        ) {
                            QuickShortcutTile(
                                title = "Network Lab",
                                subtitle = "Link & HTTP latency",
                                icon = Icons.Rounded.NetworkCheck,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                onClick = onNavigateToNetworkLab,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_network_lab")
                            )
                            QuickShortcutTile(
                                title = "Display Lab",
                                subtitle = "Hz & brightness",
                                icon = Icons.Rounded.Brightness6,
                                accentColor = MaterialTheme.colorScheme.primary,
                                onClick = onNavigateToDisplayLab,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_display_lab")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryMetricCard(
    memory: com.example.domain.model.MemoryTelemetry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usedPercentInt = kotlin.math.round(memory.usedPercent * 100f).toInt()
    val ext = LocalNexoraColors.current
    val gaugeColor = when {
        memory.isLowMemory || usedPercentInt >= 88 -> ext.critical
        usedPercentInt >= 75 -> ext.warning
        else -> MaterialTheme.colorScheme.primary
    }

    NexoraCard(
        onClick = onClick,
        modifier = modifier.testTag("dashboard_ram_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Memory,
                        contentDescription = null,
                        tint = gaugeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.dashboard_ram_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(NexoraSpacing.sm))
                Text(
                    text = stringResource(
                        R.string.dashboard_available_fmt,
                        FormatUtils.formatMemoryBytes(memory.availableBytes)
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${FormatUtils.formatMemoryBytes(memory.usedBytes)} used • ${
                        stringResource(
                            R.string.dashboard_total_fmt,
                            FormatUtils.formatMemoryBytes(memory.totalBytes)
                        )
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(NexoraSpacing.md))
            TelemetryRingGauge(
                progress = memory.usedPercent,
                centerLabel = "$usedPercentInt%",
                centerSubLabel = "USED",
                accentColor = gaugeColor
            )
        }
    }
}

@Composable
private fun BatteryMetricCard(
    battery: com.example.domain.model.BatteryTelemetry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalNexoraColors.current
    val accent = when {
        battery.isCharging -> ext.healthy
        battery.levelPercent <= 20 -> ext.critical
        else -> MaterialTheme.colorScheme.secondary
    }

    NexoraCard(
        onClick = onClick,
        modifier = modifier.testTag("dashboard_battery_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                ) {
                    Icon(
                        imageVector = if (battery.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.dashboard_battery_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(NexoraSpacing.sm))
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedIntCounter(
                        targetValue = battery.levelPercent,
                        suffix = "%",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(NexoraSpacing.sm))
                    Text(
                        text = if (battery.isCharging) "${battery.statusLabel} (${battery.chargePlugSource})" else battery.statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val tempText = FormatUtils.formatTemperature(battery.temperatureCelsius)
                val voltText = battery.voltageMillivolts?.let { "${it} mV" } ?: "Voltage N/A"
                Text(
                    text = "$tempText • $voltText • Health: ${battery.healthLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(NexoraSpacing.md))
            TelemetryRingGauge(
                progress = (battery.levelPercent / 100f).coerceIn(0f, 1f),
                centerLabel = "${battery.levelPercent}%",
                centerSubLabel = if (battery.isCharging) "CHG" else "BAT",
                accentColor = accent
            )
        }
    }
}

@Composable
private fun QuickShortcutTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NexoraCard(
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.FormatUtils
import com.example.ui.components.NexoraCard
import com.example.ui.components.SpringPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.components.rememberNexoraHaptics
import com.example.ui.theme.LocalNexoraColors
import com.example.ui.theme.NexoraSpacing
import com.example.ui.viewmodel.LabTab
import com.example.ui.viewmodel.LabsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabsScreen(
    viewModel: LabsViewModel,
    initialTab: LabTab? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val triggerHaptic = rememberNexoraHaptics()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            viewModel.selectTab(initialTab)
        }
    }

    // Re-check WRITE_SETTINGS permission when user returns from System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTelemetry()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(NexoraSpacing.md),
                verticalArrangement = Arrangement.spacedBy(NexoraSpacing.md)
            ) {
                item(key = "labs_switcher") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (state.selectedTab == LabTab.NETWORK) {
                                stringResource(R.string.network_title)
                            } else {
                                stringResource(R.string.display_title)
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = state.selectedTab == LabTab.NETWORK,
                                onClick = {
                                    triggerHaptic()
                                    viewModel.selectTab(LabTab.NETWORK)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                modifier = Modifier.testTag("labs_tab_network")
                            ) {
                                Text(stringResource(R.string.labs_network_tab))
                            }
                            SegmentedButton(
                                selected = state.selectedTab == LabTab.DISPLAY,
                                onClick = {
                                    triggerHaptic()
                                    viewModel.selectTab(LabTab.DISPLAY)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                modifier = Modifier.testTag("labs_tab_display")
                            ) {
                                Text(stringResource(R.string.labs_display_tab))
                            }
                        }
                    }
                }

                if (state.selectedTab == LabTab.NETWORK) {
                    item(key = "net_connectivity_card") {
                        val ext = LocalNexoraColors.current
                        val net = state.telemetry.network
                        NexoraCard(
                            accentHighlight = true,
                            accentColor = if (net.isConnected) ext.healthy else ext.critical,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("network_status_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.NetworkCheck,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ConnectivityManager",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                StatusPill(
                                    text = if (net.isConnected) "Online" else "Offline",
                                    color = if (net.isConnected) ext.healthy else ext.critical
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            MetricDetailRow("Transport", net.transportLabel)
                            MetricDetailRow("Validated Internet", if (net.isValidatedInternet) "Validated" else "Unvalidated")
                            MetricDetailRow("Metered Network", if (net.isMetered) "Metered" else "Unmetered")
                            MetricDetailRow("Link Downstream Est.", FormatUtils.formatBandwidthKbps(net.downstreamKbps))
                            MetricDetailRow("Link Upstream Est.", FormatUtils.formatBandwidthKbps(net.upstreamKbps))
                        }
                    }

                    item(key = "net_probe_card") {
                        NexoraCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.network_probe_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.network_probe_disclaimer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)) {
                                viewModel.endpoints.forEach { endpoint ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = endpoint.name,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = endpoint.url,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        SpringPrimaryButton(
                                            text = if (state.isProbing) {
                                                stringResource(R.string.network_probe_running)
                                            } else {
                                                stringResource(R.string.network_probe_button)
                                            },
                                            enabled = !state.isProbing && state.telemetry.network.isConnected,
                                            onClick = { viewModel.runHttpLatencyProbe(endpoint) },
                                            modifier = Modifier.testTag("probe_btn_${endpoint.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (state.probeResults.isNotEmpty()) {
                        items(state.probeResults, key = { "${it.endpointName}_${it.timestampMillis}" }) { result ->
                            val ext = LocalNexoraColors.current
                            NexoraCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(result.endpointName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = result.errorMessage ?: "HTTP ${result.httpCode ?: "--"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StatusPill(
                                        text = result.roundTripMs?.let { "${it} ms HTTP" } ?: "Failed",
                                        color = if (result.roundTripMs != null) ext.healthy else ext.critical
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // DISPLAY LAB
                    item(key = "display_metrics_card") {
                        val display = state.telemetry.display
                        NexoraCard(
                            accentHighlight = true,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("display_lab_card")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Display Hardware & Modes",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            MetricDetailRow("Active Refresh Rate", FormatUtils.formatRefreshRate(display.currentRefreshRateHz))
                            MetricDetailRow(
                                "Active Resolution",
                                if (display.widthPx > 0) "${display.widthPx} × ${display.heightPx} px" else "Unavailable"
                            )
                            MetricDetailRow(
                                "HDR Capabilities",
                                if (display.hdrCapabilities.isNotEmpty()) display.hdrCapabilities.joinToString(", ") else "SDR / None reported"
                            )
                            MetricDetailRow(
                                "Supported Modes",
                                display.supportedModes.joinToString(" • ") { FormatUtils.formatRefreshRate(it.refreshRateHz) }
                                    .ifBlank { FormatUtils.formatRefreshRate(display.currentRefreshRateHz) }
                            )
                        }
                    }

                    item(key = "display_brightness_card") {
                        val display = state.telemetry.display
                        val ext = LocalNexoraColors.current
                        var sliderValue by remember(display.brightnessValue) {
                            mutableFloatStateOf((display.brightnessValue ?: 128).toFloat())
                        }

                        NexoraCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Brightness6,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.display_brightness_title),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                StatusPill(
                                    text = if (display.canWriteSystemSettings) {
                                        stringResource(R.string.status_granted)
                                    } else {
                                        stringResource(R.string.status_not_granted)
                                    },
                                    color = if (display.canWriteSystemSettings) ext.healthy else ext.warning
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            MetricDetailRow(
                                label = "Current Level (0-255)",
                                value = display.brightnessValue?.toString() ?: stringResource(R.string.status_unavailable)
                            )
                            MetricDetailRow(
                                label = "Adaptive Brightness",
                                value = when (display.isAutoBrightness) {
                                    true -> "Enabled (Auto)"
                                    false -> "Manual"
                                    null -> stringResource(R.string.status_unavailable)
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (display.canWriteSystemSettings) {
                                Text(
                                    text = "Adjust Brightness: ${sliderValue.roundToInt()} / 255",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Slider(
                                    value = sliderValue,
                                    onValueChange = { sliderValue = it },
                                    onValueChangeFinished = {
                                        viewModel.setSystemBrightness(sliderValue.roundToInt())
                                    },
                                    valueRange = 10f..255f,
                                    modifier = Modifier.testTag("display_brightness_slider")
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.display_permission_required),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                SpringPrimaryButton(
                                    text = stringResource(R.string.display_grant_permission),
                                    icon = Icons.Rounded.Security,
                                    onClick = viewModel::openWriteSettingsPermission,
                                    modifier = Modifier.testTag("display_grant_write_settings")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            SpringPrimaryButton(
                                text = "Open System Display Settings",
                                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                                onClick = viewModel::openDisplaySettings,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.testTag("display_open_system_settings")
                            )
                        }
                    }

                    item(key = "display_animation_notice") {
                        NexoraCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Animation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.display_animation_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.display_animation_explanation),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            SpringPrimaryButton(
                                text = "Open Accessibility Motion Settings",
                                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                                onClick = viewModel::openAnimationAccessibilitySettings,
                                modifier = Modifier.testTag("display_open_accessibility")
                            )
                        }
                    }
                }
            }
        }
    }
}

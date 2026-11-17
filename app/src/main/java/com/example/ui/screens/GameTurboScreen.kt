package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.FormatUtils
import com.example.domain.model.ThermalLevel
import com.example.domain.model.TurboPerformanceProfile
import com.example.ui.components.NexoraCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShimmerBox
import com.example.ui.components.SpringPrimaryButton
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.StatusPill
import com.example.ui.components.rememberNexoraHaptics
import com.example.ui.theme.LocalNexoraColors
import com.example.ui.theme.NexoraSpacing
import com.example.ui.viewmodel.GameTurboUiState
import com.example.ui.viewmodel.GameTurboViewModel
import kotlin.math.roundToInt

@Composable
fun GameTurboScreen(
    viewModel: GameTurboViewModel,
    onOpenGameCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val triggerHaptic = rememberNexoraHaptics()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Foreground services still run cleanly; notification display respects user choice */ }
    )

    val screenshotConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onScreenshotConsentResult(result.resultCode, result.data)
    }

    val recordConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onRecordingConsentResult(result.resultCode, result.data)
    }

    val ensureNotificationsThen: (() -> Unit) -> Unit = { action ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        action()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshEnvironmentState()
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
        GameTurboContent(
            state = state,
            contentPadding = innerPadding,
            onSelectProfile = viewModel::selectPerformanceProfile,
            onStartSession = { viewModel.startManualSession(null) },
            onEndSession = viewModel::endActiveSession,
            onClearHistory = viewModel::clearSessionHistory,
            onLaunchGame = viewModel::launchGameWithTurbo,
            onOpenGameCenter = onOpenGameCenter,
            onToggleDnd = viewModel::toggleDnd,
            onToggleSidebar = { ensureNotificationsThen { viewModel.toggleGamingSidebar() } },
            onOpenOverlayPermission = viewModel::openOverlayPermissionSettings,
            onOpenWriteSettings = viewModel::openWriteSettingsPermission,
            onSetBrightness = viewModel::setSystemBrightness,
            onTakeScreenshot = {
                ensureNotificationsThen {
                    viewModel.createMediaProjectionIntent()?.let { screenshotConsentLauncher.launch(it) }
                }
            },
            onToggleRecording = {
                if (state.captureState.isRecordingScreen) {
                    viewModel.stopScreenRecording()
                } else {
                    ensureNotificationsThen {
                        viewModel.createMediaProjectionIntent()?.let { recordConsentLauncher.launch(it) }
                    }
                }
            },
            onOpenDisplaySettings = viewModel::openDisplaySettings,
            triggerHaptic = triggerHaptic
        )
    }
}

@Composable
private fun GameTurboContent(
    state: GameTurboUiState,
    contentPadding: PaddingValues,
    onSelectProfile: (TurboPerformanceProfile) -> Unit,
    onStartSession: () -> Unit,
    onEndSession: () -> Unit,
    onClearHistory: () -> Unit,
    onLaunchGame: (com.example.domain.model.InstalledAppItem) -> Unit,
    onOpenGameCenter: () -> Unit,
    onToggleDnd: () -> Unit,
    onToggleSidebar: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onOpenWriteSettings: () -> Unit,
    onSetBrightness: (Int) -> Unit,
    onTakeScreenshot: () -> Unit,
    onToggleRecording: () -> Unit,
    onOpenDisplaySettings: () -> Unit,
    triggerHaptic: () -> Unit
) {
    val ext = LocalNexoraColors.current
    val snapshot = state.telemetry
    val activeSession = state.activeSession

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize()
                .testTag("turbo_lazy_list"),
            contentPadding = PaddingValues(NexoraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NexoraSpacing.md)
        ) {
            // 1. Turbo Session Hero Card
            item(key = "turbo_hero") {
                StaggeredEntrance(index = 0) {
                    NexoraCard(
                        accentHighlight = true,
                        accentColor = if (activeSession != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("turbo_hero_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (activeSession != null) activeSession.appLabel else stringResource(R.string.turbo_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Cadence: ${state.activeProfile.pollIntervalMs / 1000.0}s • ${state.activeProfile.title}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            StatusPill(
                                text = if (activeSession != null) {
                                    stringResource(R.string.turbo_session_active)
                                } else {
                                    stringResource(R.string.turbo_session_idle)
                                },
                                color = if (activeSession != null) ext.healthy else MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SESSION TIMER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatDurationTimer(state.elapsedSessionSeconds),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("turbo_session_timer")
                                )
                            }

                            if (activeSession != null) {
                                SpringPrimaryButton(
                                    text = stringResource(R.string.turbo_end_session),
                                    icon = Icons.Rounded.Stop,
                                    onClick = onEndSession,
                                    containerColor = ext.critical,
                                    modifier = Modifier.testTag("turbo_end_session_btn")
                                )
                            } else {
                                SpringPrimaryButton(
                                    text = stringResource(R.string.turbo_start_session),
                                    icon = Icons.Rounded.PlayArrow,
                                    onClick = onStartSession,
                                    modifier = Modifier.testTag("turbo_start_session_btn")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (activeSession != null && activeSession.packageName != "com.nexorax.manual_session") {
                                "Active managed session: ${activeSession.packageName}"
                            } else {
                                "Active game tracking uses managed launches from Game Center / Turbo without requiring privileged UsageStats or Accessibility access."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Live Telemetry Grid (Battery, RAM, Thermal, Refresh)
            item(key = "turbo_telemetry_strip") {
                StaggeredEntrance(index = 1) {
                    val thermalColor = when (snapshot.thermal.level) {
                        ThermalLevel.NONE, ThermalLevel.LIGHT -> ext.healthy
                        ThermalLevel.MODERATE -> ext.warning
                        ThermalLevel.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> ext.critical
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                        ) {
                            MiniTelemetryCard(
                                label = "Battery",
                                value = "${snapshot.battery.levelPercent}%",
                                sub = snapshot.battery.statusLabel,
                                icon = Icons.Rounded.BatteryChargingFull,
                                tint = if (snapshot.battery.isCharging) ext.healthy else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("turbo_metric_battery")
                            )
                            MiniTelemetryCard(
                                label = "Available RAM",
                                value = FormatUtils.formatMemoryBytes(snapshot.memory.availableBytes),
                                sub = "of ${FormatUtils.formatMemoryBytes(snapshot.memory.totalBytes)}",
                                icon = Icons.Rounded.Memory,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("turbo_metric_ram")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                        ) {
                            MiniTelemetryCard(
                                label = "Thermal State",
                                value = snapshot.thermal.level.label,
                                sub = FormatUtils.formatTemperature(snapshot.battery.temperatureCelsius),
                                icon = Icons.Rounded.Thermostat,
                                tint = thermalColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("turbo_metric_thermal")
                            )
                            MiniTelemetryCard(
                                label = "Display Rate",
                                value = FormatUtils.formatRefreshRate(snapshot.display.currentRefreshRateHz),
                                sub = snapshot.network.transportLabel,
                                icon = Icons.Rounded.Speed,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("turbo_metric_display")
                            )
                        }
                    }
                }
            }

            // 3. Performance Profiles (DataStore persisted)
            item(key = "turbo_profiles") {
                StaggeredEntrance(index = 2) {
                    NexoraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("turbo_profiles_card")
                    ) {
                        SectionHeader(
                            title = stringResource(R.string.turbo_profiles_title),
                            trailingBadge = "DataStore",
                            badgeColor = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.turbo_profiles_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TurboPerformanceProfile.entries.forEach { profile ->
                                val selected = state.activeProfile == profile
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    } else {
                                        ext.surfaceSubtle
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            triggerHaptic()
                                            onSelectProfile(profile)
                                        }
                                        .testTag("turbo_profile_${profile.name}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = profile.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = profile.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (selected) {
                                            StatusPill(
                                                text = "Active",
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Quick Launch Games Row
            item(key = "turbo_quick_games") {
                StaggeredEntrance(index = 3) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quick Game Launch",
                                style = MaterialTheme.typography.titleLarge
                            )
                            TextButton(
                                onClick = {
                                    triggerHaptic()
                                    onOpenGameCenter()
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("turbo_open_game_center")
                            ) {
                                Text("Manage All")
                            }
                        }

                        if (state.isGamesLoading) {
                            ShimmerBox(height = 76.dp)
                        } else if (state.quickLaunchGames.isEmpty()) {
                            NexoraCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.SportsEsports,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "No games starred or detected yet",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "Open Game Center to favorite any installed app or game.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)) {
                                items(state.quickLaunchGames, key = { it.packageName }) { game ->
                                    NexoraCard(
                                        onClick = { onLaunchGame(game) },
                                        contentPadding = PaddingValues(14.dp),
                                        modifier = Modifier.testTag("turbo_quick_game_${game.packageName}")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = game.label,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = game.profile?.preferredTurboProfile?.title ?: "Launch & Track",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Floating Gaming Sidebar & Screen Capture
            item(key = "turbo_sidebar_capture") {
                StaggeredEntrance(index = 4) {
                    NexoraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("turbo_sidebar_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.turbo_sidebar_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            StatusPill(
                                text = when {
                                    state.isSidebarRunning -> "Active"
                                    state.hasOverlayPermission -> "Ready"
                                    else -> "Access Needed"
                                },
                                color = when {
                                    state.isSidebarRunning -> ext.healthy
                                    state.hasOverlayPermission -> MaterialTheme.colorScheme.secondary
                                    else -> ext.warning
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.turbo_sidebar_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                        ) {
                            SpringPrimaryButton(
                                text = when {
                                    state.isSidebarRunning -> "Stop Sidebar"
                                    state.hasOverlayPermission -> "Launch Sidebar"
                                    else -> "Grant Overlay Access"
                                },
                                icon = Icons.Rounded.Layers,
                                onClick = if (state.hasOverlayPermission) onToggleSidebar else onOpenOverlayPermission,
                                containerColor = if (state.isSidebarRunning) ext.critical else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("turbo_sidebar_toggle_btn")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                        ) {
                            SpringPrimaryButton(
                                text = if (state.captureState.isCapturingScreenshot) "Capturing..." else "Screenshot",
                                icon = Icons.Rounded.PhotoCamera,
                                enabled = !state.captureState.isCapturingScreenshot && !state.captureState.isRecordingScreen,
                                onClick = onTakeScreenshot,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("turbo_screenshot_btn")
                            )
                            SpringPrimaryButton(
                                text = if (state.captureState.isRecordingScreen) "Stop Recording" else "Record Screen",
                                icon = if (state.captureState.isRecordingScreen) Icons.Rounded.Stop else Icons.Rounded.FiberManualRecord,
                                onClick = onToggleRecording,
                                containerColor = if (state.captureState.isRecordingScreen) ext.critical else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("turbo_record_btn")
                            )
                        }
                    }
                }
            }

            // 6. Gaming Tools: DND, Brightness, Network & Display Shortcut
            item(key = "turbo_tools_card") {
                StaggeredEntrance(index = 5) {
                    var brightnessVal by remember(snapshot.display.brightnessValue) {
                        mutableFloatStateOf((snapshot.display.brightnessValue ?: 128).toFloat())
                    }

                    NexoraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("turbo_tools_card")
                    ) {
                        SectionHeader(title = stringResource(R.string.turbo_tools_title))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Do Not Disturb row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (state.dndStatus.isDndEnabled) {
                                        Icons.Rounded.NotificationsOff
                                    } else {
                                        Icons.Rounded.DoNotDisturbOn
                                    },
                                    contentDescription = null,
                                    tint = if (state.dndStatus.isDndEnabled) ext.healthy else MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Do Not Disturb", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = when {
                                            !state.dndStatus.hasPolicyAccess -> "Requires Notification Policy Access"
                                            state.dndStatus.isDndEnabled -> "Active (Priority interruptions only)"
                                            else -> "Off (All notifications allowed)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            SpringPrimaryButton(
                                text = when {
                                    !state.dndStatus.hasPolicyAccess -> "Grant DND"
                                    state.dndStatus.isDndEnabled -> "Turn Off"
                                    else -> "Turn On"
                                },
                                onClick = onToggleDnd,
                                modifier = Modifier.testTag("turbo_dnd_btn")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Brightness control row
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
                                Text("Brightness", style = MaterialTheme.typography.titleSmall)
                            }
                            if (!snapshot.display.canWriteSystemSettings) {
                                TextButton(
                                    onClick = onOpenWriteSettings,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Grant Access")
                                }
                            } else {
                                Text(
                                    text = "${brightnessVal.roundToInt()} / 255",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        if (snapshot.display.canWriteSystemSettings) {
                            Slider(
                                value = brightnessVal,
                                onValueChange = { brightnessVal = it },
                                onValueChangeFinished = { onSetBrightness(brightnessVal.roundToInt()) },
                                valueRange = 10f..255f,
                                modifier = Modifier.testTag("turbo_brightness_slider")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        MetricDetailRow(
                            label = "Network (ConnectivityManager)",
                            value = "${snapshot.network.transportLabel} • ${FormatUtils.formatBandwidthKbps(snapshot.network.downstreamKbps)}"
                        )
                        MetricDetailRow(
                            label = "Display Refresh Rate",
                            value = FormatUtils.formatRefreshRate(snapshot.display.currentRefreshRateHz)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SpringPrimaryButton(
                            text = "Open System Display Settings",
                            icon = Icons.AutoMirrored.Rounded.OpenInNew,
                            onClick = onOpenDisplaySettings,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 7. Session History
            item(key = "turbo_session_history") {
                StaggeredEntrance(index = 6) {
                    NexoraCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("turbo_history_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.turbo_history_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            if (state.sessionHistory.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        triggerHaptic()
                                        onClearHistory()
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("turbo_clear_history_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Clear session history",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (state.sessionHistory.isEmpty()) {
                            Text(
                                text = stringResource(R.string.turbo_history_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.sessionHistory.take(8).forEach { session ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.appLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = session.profileUsed.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        StatusPill(
                                            text = FormatUtils.formatDurationTimer(session.durationSeconds()),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. Safety & Compatibility Notice
            item(key = "turbo_safety_notice") {
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
                            text = "No-Root Guarantee: NEXORA X never kills background processes, fabricates FPS counters, claims artificial ping reduction, or abuses Accessibility Services. Overlay and MediaProjection captures require explicit user consent.",
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
private fun MiniTelemetryCard(
    label: String,
    value: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    NexoraCard(
        contentPadding = PaddingValues(12.dp),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

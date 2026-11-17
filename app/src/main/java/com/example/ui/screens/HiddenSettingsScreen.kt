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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PermDeviceInformation
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.example.domain.model.ShortcutCategory
import com.example.domain.model.ShortcutLaunchResult
import com.example.domain.model.SystemSettingsShortcut
import com.example.ui.components.NexoraCard
import com.example.ui.components.SpringPrimaryButton
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.StatusPill
import com.example.ui.components.rememberNexoraHaptics
import com.example.ui.theme.LocalNexoraColors
import com.example.ui.theme.NexoraSpacing
import com.example.ui.viewmodel.HiddenSettingsViewModel

@Composable
fun HiddenSettingsScreen(
    viewModel: HiddenSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val triggerHaptic = rememberNexoraHaptics()

    LaunchedEffect(state.lastLaunchResult) {
        val result = state.lastLaunchResult
        if (result != null) {
            val message = when (result) {
                is ShortcutLaunchResult.LaunchedDirect -> "Opened ${result.shortcutTitle}"
                is ShortcutLaunchResult.LaunchedFallback -> "${result.shortcutTitle}: ${result.reason}"
                is ShortcutLaunchResult.Unavailable -> "${result.shortcutTitle}: ${result.reason}"
            }
            snackbarHostState.showSnackbar(message)
            viewModel.clearLaunchResult()
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
                item(key = "shortcuts_header") {
                    Column {
                        Text(
                            text = stringResource(R.string.shortcuts_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.shortcuts_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.isXiaomiOrHyperOs) {
                    item(key = "hyperos_notice") {
                        val ext = LocalNexoraColors.current
                        NexoraCard(
                            accentHighlight = true,
                            accentColor = ext.warning,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = ext.warning,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.shortcuts_hyperos_notice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                item(key = "category_chips") {
                    val categories: List<Pair<ShortcutCategory?, String>> = listOf(
                        null to "All",
                        ShortcutCategory.PERFORMANCE_DEV to "Developer",
                        ShortcutCategory.DISPLAY_MOTION to "Display",
                        ShortcutCategory.POWER_BATTERY to "Battery",
                        ShortcutCategory.CONNECTIVITY_AUDIO to "Network & Sound",
                        ShortcutCategory.DEVICE_APPS to "Apps & Device"
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                    ) {
                        items(categories.size) { index ->
                            val (cat, label) = categories[index]
                            val selected = state.selectedCategory == cat
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    triggerHaptic()
                                    viewModel.selectCategory(cat)
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.testTag("shortcut_filter_${cat?.name ?: "ALL"}")
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = state.shortcuts,
                    key = { _, item -> item.id }
                ) { index, shortcut ->
                    StaggeredEntrance(index = index) {
                        ShortcutItemCard(
                            shortcut = shortcut,
                            onLaunch = { viewModel.launchShortcut(shortcut) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutItemCard(
    shortcut: SystemSettingsShortcut,
    onLaunch: () -> Unit
) {
    val ext = LocalNexoraColors.current
    val icon = shortcutIconFor(shortcut.id)
    val accent = if (shortcut.isDirectSupported) MaterialTheme.colorScheme.secondary else ext.warning

    NexoraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shortcut_card_${shortcut.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(NexoraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                ) {
                    Text(
                        text = shortcut.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    StatusPill(
                        text = if (shortcut.isDirectSupported) {
                            stringResource(R.string.shortcuts_supported)
                        } else {
                            stringResource(R.string.shortcuts_fallback)
                        },
                        color = if (shortcut.isDirectSupported) ext.healthy else ext.warning
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = shortcut.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!shortcut.oemRestrictionNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = shortcut.oemRestrictionNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SpringPrimaryButton(
                text = stringResource(R.string.shortcuts_open),
                icon = Icons.AutoMirrored.Rounded.OpenInNew,
                onClick = onLaunch,
                containerColor = if (shortcut.isDirectSupported) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.testTag("shortcut_open_${shortcut.id}")
            )
        }
    }
}

private fun shortcutIconFor(id: String): ImageVector {
    return when (id) {
        "dev_options" -> Icons.Rounded.DeveloperMode
        "display_settings" -> Icons.Rounded.Brightness6
        "animation_settings" -> Icons.Rounded.Animation
        "battery_settings" -> Icons.Rounded.BatterySaver
        "app_management" -> Icons.Rounded.Apps
        "network_settings" -> Icons.Rounded.Wifi
        "sound_settings" -> Icons.Rounded.VolumeUp
        "device_info" -> Icons.Rounded.PermDeviceInformation
        else -> Icons.Rounded.DeveloperMode
    }
}

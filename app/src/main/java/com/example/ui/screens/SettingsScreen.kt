package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.data.preferences.AppLanguageOption
import com.example.data.preferences.AppThemeMode
import com.example.ui.components.ExpandableNexoraCard
import com.example.ui.components.NexoraCard
import com.example.ui.components.SpringPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.components.rememberNexoraHaptics
import com.example.ui.theme.LocalNexoraColors
import com.example.ui.theme.NexoraSpacing
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    val ext = LocalNexoraColors.current
    val triggerHaptic = rememberNexoraHaptics()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            item(key = "settings_header") {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Theme Preferences (Expandable)
            item(key = "theme_card") {
                ExpandableNexoraCard(
                    title = stringResource(R.string.settings_theme),
                    subtitle = prefs.themeMode.label,
                    icon = Icons.Rounded.Palette,
                    initiallyExpanded = true,
                    modifier = Modifier.testTag("settings_theme_card")
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    triggerHaptic()
                                    viewModel.setThemeMode(mode)
                                }
                                .padding(horizontal = 4.dp)
                                .testTag("theme_option_${mode.name}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = prefs.themeMode == mode,
                                onClick = {
                                    triggerHaptic()
                                    viewModel.setThemeMode(mode)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Language Preferences (Expandable)
            item(key = "language_card") {
                ExpandableNexoraCard(
                    title = stringResource(R.string.settings_language),
                    subtitle = prefs.language.displayName,
                    icon = Icons.Rounded.Language,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("settings_language_card")
                ) {
                    AppLanguageOption.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    triggerHaptic()
                                    viewModel.setLanguage(lang)
                                }
                                .padding(horizontal = 4.dp)
                                .testTag("language_option_${lang.tag}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = prefs.language == lang,
                                onClick = {
                                    triggerHaptic()
                                    viewModel.setLanguage(lang)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Haptics & Reduced Motion Toggles
            item(key = "interaction_card") {
                NexoraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Rounded.Vibration,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_haptics),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Tactile feedback on navigation and actions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = prefs.hapticsEnabled,
                            onCheckedChange = {
                                viewModel.setHapticsEnabled(it)
                            },
                            modifier = Modifier.testTag("switch_haptics")
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = ext.cardBorder
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Rounded.Animation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_reduced_motion),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Also follows Android Animator Duration Scale",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = prefs.reducedMotionOverride,
                            onCheckedChange = {
                                viewModel.setReducedMotionOverride(it)
                            },
                            modifier = Modifier.testTag("switch_reduced_motion")
                        )
                    }
                }
            }

            // Permission Manager
            item(key = "permissions_card") {
                ExpandableNexoraCard(
                    title = stringResource(R.string.settings_permissions),
                    subtitle = "Least-privilege system access transparency",
                    icon = Icons.Rounded.Security,
                    initiallyExpanded = true,
                    modifier = Modifier.testTag("settings_permissions_card")
                ) {
                    PermissionStatusRow(
                        name = "Modify System Settings (WRITE_SETTINGS)",
                        description = "Optional; used by Display Lab & Game Turbo brightness slider.",
                        statusText = if (state.canWriteSystemSettings) {
                            stringResource(R.string.status_granted)
                        } else {
                            stringResource(R.string.status_not_granted)
                        },
                        statusColor = if (state.canWriteSystemSettings) ext.healthy else ext.warning
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionStatusRow(
                        name = "Display Over Other Apps (SYSTEM_ALERT_WINDOW)",
                        description = "Optional; only used when launching the Floating Gaming Sidebar.",
                        statusText = if (state.hasOverlayPermission) {
                            stringResource(R.string.status_granted)
                        } else {
                            stringResource(R.string.status_not_granted)
                        },
                        statusColor = if (state.hasOverlayPermission) ext.healthy else ext.warning
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionStatusRow(
                        name = "Do Not Disturb Policy (ACCESS_NOTIFICATION_POLICY)",
                        description = "Optional; used by Game Turbo DND toggle.",
                        statusText = if (state.hasDndPolicyAccess) {
                            stringResource(R.string.status_granted)
                        } else {
                            stringResource(R.string.status_not_granted)
                        },
                        statusColor = if (state.hasDndPolicyAccess) ext.healthy else ext.warning
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionStatusRow(
                        name = "Network State & Internet",
                        description = "Normal install-time permissions for ConnectivityManager & optional HTTP probe.",
                        statusText = stringResource(R.string.status_install_time),
                        statusColor = ext.healthy
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PermissionStatusRow(
                        name = "Launcher Package Visibility (<queries>)",
                        description = "Targeted ACTION_MAIN/LAUNCHER manifest query without QUERY_ALL_PACKAGES.",
                        statusText = "Scoped",
                        statusColor = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                    ) {
                        SpringPrimaryButton(
                            text = "Brightness",
                            icon = Icons.AutoMirrored.Rounded.OpenInNew,
                            onClick = viewModel::openManageWriteSettings,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_btn_write_settings")
                        )
                        SpringPrimaryButton(
                            text = "Overlay",
                            icon = Icons.AutoMirrored.Rounded.OpenInNew,
                            onClick = viewModel::openManageOverlaySettings,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_btn_overlay")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                    ) {
                        SpringPrimaryButton(
                            text = "DND Access",
                            icon = Icons.AutoMirrored.Rounded.OpenInNew,
                            onClick = viewModel::openDndPolicySettings,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_btn_dnd")
                        )
                        SpringPrimaryButton(
                            text = "App Details",
                            icon = Icons.AutoMirrored.Rounded.OpenInNew,
                            onClick = viewModel::openAppDetailsSettings,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_btn_app_details")
                        )
                    }
                }
            }

            // Privacy & About
            item(key = "privacy_about_card") {
                NexoraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.PrivacyTip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_privacy),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "NEXORA X runs 100% locally on your Android device with zero analytics SDKs, zero background ad trackers, and zero root exploits. Preferences are stored locally via Jetpack DataStore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = ext.cardBorder
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_about),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    MetricDetailRow("Application", stringResource(R.string.app_name))
                    MetricDetailRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    MetricDetailRow("Architecture", "No-Root Native Android • Jetpack Compose")
                    if (state.isXiaomiOrHyperOs) {
                        MetricDetailRow("Detected OEM Skin", state.osUiFlavor ?: "HyperOS / MIUI")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    name: String,
    description: String,
    statusText: String,
    statusColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        StatusPill(text = statusText, color = statusColor)
    }
}

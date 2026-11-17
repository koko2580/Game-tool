package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.GameFilter
import com.example.domain.model.GameProfile
import com.example.domain.model.InstalledAppItem
import com.example.domain.model.TurboPerformanceProfile
import com.example.ui.components.NexoraCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShimmerBox
import com.example.ui.components.SpringPrimaryButton
import com.example.ui.components.StatusPill
import com.example.ui.components.rememberNexoraHaptics
import com.example.ui.theme.NexoraSpacing
import com.example.ui.viewmodel.GameCenterViewModel

@Composable
fun GameCenterScreen(
    viewModel: GameCenterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val triggerHaptic = rememberNexoraHaptics()
    var editingProfileApp by remember { mutableStateOf<InstalledAppItem?>(null) }

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
                item(key = "games_header") {
                    Text(
                        text = stringResource(R.string.games_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item(key = "games_search") {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = { Text(stringResource(R.string.games_search_hint)) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onSearchQueryChanged("") },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("games_search_input")
                    )
                }

                item(key = "games_filters") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                    ) {
                        val filters = listOf(
                            GameFilter.DETECTED_GAMES to stringResource(R.string.games_filter_detected),
                            GameFilter.FAVORITES to stringResource(R.string.games_filter_favorites),
                            GameFilter.ALL_APPS to stringResource(R.string.games_filter_all)
                        )
                        filters.forEach { (filter, label) ->
                            FilterChip(
                                selected = state.selectedFilter == filter,
                                onClick = {
                                    triggerHaptic()
                                    viewModel.onFilterSelected(filter)
                                },
                                label = { Text(label, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.testTag("games_filter_${filter.name}")
                            )
                        }
                    }
                }

                if (state.recentApps.isNotEmpty()) {
                    item(key = "recent_header") {
                        SectionHeader(title = stringResource(R.string.games_recent_header))
                    }
                    item(key = "recent_row") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                        ) {
                            items(state.recentApps, key = { "recent_${it.packageName}" }) { app ->
                                NexoraCard(
                                    onClick = { viewModel.launchGame(app) },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AppIconBadge(drawable = app.iconDrawable, sizeDp = 28)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.isLoading) {
                    items(4) {
                        ShimmerBox(height = 88.dp)
                    }
                } else if (state.apps.isEmpty()) {
                    item(key = "empty_games") {
                        NexoraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = NexoraSpacing.md),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SportsEsports,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.games_empty_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.games_empty_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(state.apps, key = { it.packageName }) { app ->
                        GameAppRowCard(
                            item = app,
                            onLaunch = { viewModel.launchGame(app) },
                            onToggleFavorite = {
                                triggerHaptic()
                                viewModel.toggleFavorite(app.packageName)
                            },
                            onEditProfile = {
                                triggerHaptic()
                                editingProfileApp = app
                            }
                        )
                    }
                }
            }
        }
    }

    editingProfileApp?.let { targetApp ->
        GameProfileDialog(
            app = targetApp,
            onDismiss = { editingProfileApp = null },
            onSave = { profile ->
                viewModel.saveGameProfile(profile)
                editingProfileApp = null
            }
        )
    }
}

@Composable
private fun GameAppRowCard(
    item: InstalledAppItem,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditProfile: () -> Unit
) {
    NexoraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("game_card_${item.packageName}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconBadge(drawable = item.iconDrawable, sizeDp = 46)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NexoraSpacing.sm)
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.isAndroidGameCategory) {
                        StatusPill(
                            text = "Game",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Text(
                    text = item.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.profile != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Profile: ${item.profile.targetRefreshNote}" +
                            if (item.profile.enableDndReminder) " • DND Reminder" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("favorite_btn_${item.packageName}")
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = if (item.isFavorite) "Remove ${item.label} from favorites" else "Favorite ${item.label}",
                    tint = if (item.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onEditProfile,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("profile_btn_${item.packageName}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Profile")
            }

            SpringPrimaryButton(
                text = stringResource(R.string.games_launch),
                icon = Icons.Rounded.PlayArrow,
                onClick = onLaunch,
                modifier = Modifier.testTag("launch_btn_${item.packageName}")
            )
        }
    }
}

@Composable
private fun AppIconBadge(
    drawable: Drawable?,
    sizeDp: Int
) {
    val bitmap: ImageBitmap? = remember(drawable) {
        drawable?.let { drawableToImageBitmap(it) }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SportsEsports,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size((sizeDp * 0.55f).dp)
            )
        }
    }
}

private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap? {
    return runCatching {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap.asImageBitmap()
        } else {
            val width = drawable.intrinsicWidth.coerceIn(48, 192)
            val height = drawable.intrinsicHeight.coerceIn(48, 192)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }
    }.getOrNull()
}

@Composable
private fun GameProfileDialog(
    app: InstalledAppItem,
    onDismiss: () -> Unit,
    onSave: (GameProfile) -> Unit
) {
    var refreshNote by remember(app.packageName) {
        mutableStateOf(app.profile?.targetRefreshNote ?: "120 Hz Target")
    }
    var brightnessNote by remember(app.packageName) {
        mutableStateOf(app.profile?.brightnessChecklistNote ?: "")
    }
    var dndReminder by remember(app.packageName) {
        mutableStateOf(app.profile?.enableDndReminder ?: false)
    }
    var preferredTurbo by remember(app.packageName) {
        mutableStateOf(app.profile?.preferredTurboProfile)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${app.label} — ${stringResource(R.string.games_profile_title)}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = refreshNote,
                    onValueChange = { refreshNote = it },
                    label = { Text(stringResource(R.string.games_profile_refresh_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = brightnessNote,
                    onValueChange = { brightnessNote = it },
                    label = { Text(stringResource(R.string.games_profile_brightness_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Preferred Game Turbo Profile",
                    style = MaterialTheme.typography.labelMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = preferredTurbo == null,
                            onClick = { preferredTurbo = null },
                            label = { Text("Global") }
                        )
                    }
                    items(TurboPerformanceProfile.entries) { profile ->
                        FilterChip(
                            selected = preferredTurbo == profile,
                            onClick = { preferredTurbo = profile },
                            label = { Text(profile.title) }
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = dndReminder,
                        onCheckedChange = { dndReminder = it }
                    )
                    Text(
                        text = stringResource(R.string.games_profile_dnd_reminder),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        GameProfile(
                            packageName = app.packageName,
                            targetRefreshNote = refreshNote.ifBlank { "Auto" },
                            enableDndReminder = dndReminder,
                            brightnessChecklistNote = brightnessNote.trim(),
                            preferredTurboProfile = preferredTurbo
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.games_profile_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

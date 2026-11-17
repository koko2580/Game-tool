package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalNexoraColors
import com.example.ui.theme.NexoraSpacing
import kotlinx.coroutines.delay

@Composable
fun NexoraCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    accentHighlight: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(NexoraSpacing.md),
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalNexoraColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val triggerHaptic = rememberNexoraHaptics()

    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed && !reducedMotion) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_press_scale"
    )

    val borderColor = if (accentHighlight) {
        accentColor.copy(alpha = 0.28f)
    } else {
        ext.cardBorder
    }

    val backgroundBrush = if (accentHighlight) {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.11f),
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface
            ),
            start = Offset(0f, 0f),
            end = Offset(700f, 500f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface
            )
        )
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.material3.ripple(),
                            onClick = {
                                triggerHaptic()
                                onClick()
                            }
                        )
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    var visible by remember { mutableStateOf(reducedMotion) }

    LaunchedEffect(reducedMotion) {
        if (!reducedMotion && !visible) {
            delay((index.coerceAtMost(8) * 40L))
            visible = true
        } else if (reducedMotion) {
            visible = true
        }
    }

    if (reducedMotion) {
        Box(modifier = modifier) { content() }
    } else {
        AnimatedVisibility(
            visible = visible,
            modifier = modifier,
            enter = fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 6 }
                )
        ) {
            content()
        }
    }
}

@Composable
fun AnimatedIntCounter(
    targetValue: Int,
    suffix: String = "",
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val reducedMotion = LocalReducedMotion.current
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "int_counter"
    )
    Text(
        text = "$animatedValue$suffix",
        style = style,
        color = color
    )
}

@Composable
fun AnimatedFloatCounter(
    targetValue: Float,
    decimals: Int = 1,
    suffix: String = "",
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val reducedMotion = LocalReducedMotion.current
    val animatedValue by animateFloatAsState(
        targetValue = targetValue,
        animationSpec = if (reducedMotion) tween(0) else tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "float_counter"
    )
    val formatted = remember(animatedValue, decimals) {
        String.format(java.util.Locale.US, "%.${decimals}f", animatedValue)
    }
    Text(
        text = "$formatted$suffix",
        style = style,
        color = color
    )
}

@Composable
fun SpringPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val triggerHaptic = rememberNexoraHaptics()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "btn_scale"
    )

    Button(
        onClick = {
            triggerHaptic()
            onClick()
        },
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = NexoraSpacing.md, vertical = 12.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(NexoraSpacing.sm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingBadge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.secondary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!trailingBadge.isNullOrBlank()) {
            StatusPill(
                text = trailingBadge,
                color = badgeColor
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ExpandableNexoraCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    expandedContent: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val reducedMotion = LocalReducedMotion.current
    val triggerHaptic = rememberNexoraHaptics()
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (reducedMotion) tween(0) else tween(220),
        label = "chevron_rotate"
    )

    NexoraCard(
        modifier = modifier.animateContentSize(
            animationSpec = if (reducedMotion) tween(0) else spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    triggerHaptic()
                    expanded = !expanded
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
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
            Spacer(modifier = Modifier.width(NexoraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(NexoraSpacing.sm))
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(chevronRotation)
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(NexoraSpacing.md))
            expandedContent()
        }
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    shape: Shape = RoundedCornerShape(20.dp)
) {
    val reducedMotion = LocalReducedMotion.current
    val baseColor = MaterialTheme.colorScheme.surface
    val highlightColor = LocalNexoraColors.current.surfaceElevated

    if (reducedMotion) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .background(baseColor)
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "shimmer_loading")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val brush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translate - 300f, 0f),
        end = Offset(translate, 200f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(brush)
    )
}

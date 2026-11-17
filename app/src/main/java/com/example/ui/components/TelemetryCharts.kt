package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TelemetryRingGauge(
    progress: Float,
    centerLabel: String,
    centerSubLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    strokeWidth: Dp = 7.dp
) {
    val reducedMotion = LocalReducedMotion.current
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = if (reducedMotion) tween(0) else tween(450, easing = FastOutSlowInEasing),
        label = "ring_gauge_progress"
    )
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedProgress > 0.005f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(accentColor.copy(alpha = 0.7f), accentColor)
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = centerSubLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TelemetryLineChart(
    values: List<Float>,
    minValue: Float,
    maxValue: Float,
    lineColor: Color,
    unitSuffix: String,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 132.dp
) {
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val latestText = values.lastOrNull()?.let {
        String.format(java.util.Locale.US, "%.1f%s", it, unitSuffix)
    } ?: "Collecting..."
    val minText = values.minOrNull()?.let {
        String.format(java.util.Locale.US, "Min %.1f%s", it, unitSuffix)
    } ?: "--"
    val maxText = values.maxOrNull()?.let {
        String.format(java.util.Locale.US, "Max %.1f%s", it, unitSuffix)
    } ?: "--"

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$minText  |  $maxText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = latestText,
                style = MaterialTheme.typography.labelMedium,
                color = lineColor
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            val w = size.width
            val h = size.height
            val padTop = 10.dp.toPx()
            val padBottom = 10.dp.toPx()
            val usableH = (h - padTop - padBottom).coerceAtLeast(1f)

            // Draw subtle horizontal reference grid lines
            for (i in 0..2) {
                val y = padTop + (usableH / 2f) * i
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (values.size < 2) {
                if (values.size == 1) {
                    val norm = ((values[0] - minValue) / (maxValue - minValue).coerceAtLeast(0.01f))
                        .coerceIn(0f, 1f)
                    val y = padTop + usableH * (1f - norm)
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = Offset(w / 2f, y)
                    )
                }
                return@Canvas
            }

            val range = (maxValue - minValue).coerceAtLeast(0.01f)
            val stepX = w / (values.size - 1).coerceAtLeast(1)

            val strokePath = Path()
            val fillPath = Path()

            values.forEachIndexed { index, value ->
                val x = index * stepX
                val norm = ((value - minValue) / range).coerceIn(0f, 1f)
                val y = padTop + usableH * (1f - norm)
                if (index == 0) {
                    strokePath.moveTo(x, y)
                    fillPath.moveTo(x, h - padBottom)
                    fillPath.lineTo(x, y)
                } else {
                    strokePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo((values.size - 1) * stepX, h - padBottom)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.28f),
                        lineColor.copy(alpha = 0.01f)
                    )
                )
            )

            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Highlight latest sample dot
            val lastVal = values.last()
            val lastNorm = ((lastVal - minValue) / range).coerceIn(0f, 1f)
            val lastX = (values.size - 1) * stepX
            val lastY = padTop + usableH * (1f - lastNorm)
            drawCircle(
                color = lineColor.copy(alpha = 0.25f),
                radius = 7.dp.toPx(),
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = lineColor,
                radius = 3.5.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }
    }
}

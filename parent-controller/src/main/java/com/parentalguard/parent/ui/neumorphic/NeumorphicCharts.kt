package com.parentalguard.parent.ui.neumorphic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parentalguard.parent.ui.theme.MonoFontFamily
import com.parentalguard.parent.ui.theme.trackingFor
import com.parentalguard.parent.viewmodel.ConnectionType

/** One data point for bars / donut charts. */
data class NmDatum(val label: String, val value: Float, val color: Color)

/** Recessed segmented control — a raised pill seats the active page on a groove track. */
@Composable
fun NeumorphicSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .neumorphic(shape = RoundedCornerShape(22.dp), backgroundColor = Nm.inset, elevation = 3.dp, pressed = true)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val (interactionSource, scale) = rememberNmPress(0.94f)
            Box(
                modifier = Modifier
                    .widthIn(min = 72.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .then(
                        if (selected) {
                            Modifier.neumorphic(shape = RoundedCornerShape(16.dp), backgroundColor = Nm.surface, elevation = 4.dp)
                        } else {
                            Modifier
                        }
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(interactionSource = interactionSource, indication = null) { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) Nm.primary else Nm.onSurfaceMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

/** Filter chip — raised when idle, recessed into a primary seat when active. */
@Composable
fun NeumorphicChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Nm.primary
) {
    val (interactionSource, scale) = rememberNmPress(0.94f)
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .neumorphic(
                shape = shape,
                backgroundColor = if (selected) Nm.primarySoft else Nm.surface,
                elevation = if (selected) 2.dp else 4.dp,
                pressed = selected
            )
            .clip(shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) accent else Nm.onSurfaceMuted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Concave pill with a pulsing status dot — connection / shield states. */
@Composable
fun NeumorphicStatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .neumorphic(shape = RoundedCornerShape(50), backgroundColor = Nm.inset, elevation = 2.dp, pressed = true)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeumorphicStatusDot(color, dotSize = 6.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** LOCAL / BLUETOOTH / RELAY connection pill. */
@Composable
fun NeumorphicConnectionPill(type: ConnectionType) {
    val label = when (type) {
        ConnectionType.LOCAL -> "LOCAL"
        ConnectionType.BLUETOOTH -> "BT"
        ConnectionType.CLOUD -> "RELAY"
        ConnectionType.UNKNOWN -> "…"
    }
    Box(
        modifier = Modifier
            .neumorphic(shape = RoundedCornerShape(50), backgroundColor = Nm.inset, elevation = 2.dp, pressed = true)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Nm.primary,
            fontFamily = MonoFontFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = trackingFor(1.sp)
        )
    }
}

/** Loading placeholder — a soft raised tile that breathes. */
@Composable
fun NeumorphicShimmer(
    modifier: Modifier = Modifier,
    corner: Dp = 22.dp
) {
    val alpha by rememberInfiniteTransition(label = "nm-shimmer").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "nm-shimmer-alpha"
    )
    Box(
        modifier = modifier
            .neumorphic(shape = RoundedCornerShape(corner), backgroundColor = Nm.surface, elevation = 5.dp)
            .graphicsLayer { this.alpha = alpha }
    )
}

/** Hourly bars — concave troughs with rounded colored fill. */
@Composable
fun NeumorphicBars(
    data: List<NmDatum>,
    modifier: Modifier = Modifier,
    height: Dp = 130.dp,
    labelStep: Int = 1
) {
    val max = data.maxOfOrNull { it.value } ?: 1f
    Column(modifier = modifier.height(height + 22.dp)) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Canvas(Modifier.fillMaxSize()) {
                val gap = 6.dp.toPx()
                val barWidth = (size.width - gap * (data.size - 1)) / data.size.coerceAtLeast(1)
                val corner = 6.dp.toPx()
                data.forEachIndexed { i, d ->
                    val x = i * (barWidth + gap)
                    drawRoundRect(
                        color = Nm.inset,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(corner)
                    )
                    val frac = (d.value / max).coerceIn(0.02f, 1f)
                    val barHeight = size.height * frac
                    drawRoundRect(
                        color = d.color,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(corner)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            data.forEachIndexed { i, d ->
                Box(Modifier.weight(1f)) {
                    if (i % labelStep == 0) {
                        Text(
                            text = d.label,
                            color = Nm.onSurfaceMuted,
                            fontFamily = MonoFontFamily,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/** Donut — grooved ring segmented by category colors. */
@Composable
fun NeumorphicDonut(
    data: List<NmDatum>,
    modifier: Modifier = Modifier,
    size: Dp = 118.dp,
    strokeWidth: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2 + 2.dp.toPx()
        val diameter = this.size.minDimension - inset * 2
        val radius = diameter / 2
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = Nm.inset,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        val total = data.sumOf { it.value.toDouble() }
        if (total > 0) {
            var start = -90f
            data.filter { it.value > 0 }.forEach { d ->
                val sweep = (360.0 * (d.value / total)).toFloat()
                drawArc(
                    color = d.color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
    }
}

/** Transient toast — floating raised pill. */
@Composable
fun NeumorphicToast(
    visible: Boolean,
    text: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
    ) {
        Box(
            modifier = Modifier
                .neumorphic(shape = RoundedCornerShape(50), backgroundColor = Nm.surface, elevation = 8.dp)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                text = text,
                color = Nm.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

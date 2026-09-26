package com.parentalguard.parent.ui.neumorphic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parentalguard.parent.ui.aura.formatAuraDuration
import com.parentalguard.parent.ui.theme.MonoFontFamily
import kotlin.math.abs

// ============================================================================
// NEUMORPHIC · soft pastel tile system for the parent dashboard.
// ============================================================================

object Nm {
    val bg = Color(0xFFE4E9F5)
    val surface = Color(0xFFEAEFFB)
    val inset = Color(0xFFDCE3F2)
    val lightShadow = Color(0xFFFFFFFF)
    val darkShadow = Color(0xFFA3AFCA)
    val onSurface = Color(0xFF2C3752)
    val onSurfaceMuted = Color(0xFF7A86A1)
    val primary = Color(0xFF5B6BD6)
    val primaryDeep = Color(0xFF4555B8)
    val primarySoft = Color(0xFFEDF0FC)
    val violet = Color(0xFF9A7CF2)
    val cyan = Color(0xFF3BB7C9)
    val success = Color(0xFF3BC97E)
    val warning = Color(0xFFF5B64C)
    val danger = Color(0xFFF0657A)

    val EaseOutSoft = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
}

private val EaseEmphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private fun <T> tweenFast() = tween<T>(durationMillis = 160, easing = EaseEmphasized)
private fun <T> tweenMed() = tween<T>(durationMillis = 320, easing = EaseEmphasized)

/** Press physics shared by tactile neumorphic elements. */
@Composable
fun rememberNmPress(pressedScale: Float = 0.97f): Pair<MutableInteractionSource, Float> {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "nm-press"
    )
    return interactionSource to scale
}

/**
 * Draws a neumorphic tile: soft light shadow (top-left) + soft dark shadow
 * (bottom-right) when raised, or inner shadows when pressed/inset.
 */
fun Modifier.neumorphic(
    shape: Shape = RoundedCornerShape(28.dp),
    backgroundColor: Color = Nm.surface,
    lightShadowColor: Color = Nm.lightShadow,
    darkShadowColor: Color = Nm.darkShadow,
    elevation: Dp = 7.dp,
    pressed: Boolean = false
): Modifier = drawBehind {
    val blur = elevation.toPx()
    val offset = elevation.toPx() * 0.9f
    val alpha = if (pressed) 0.42f else 0.6f

    fun shadowPaint(shadowColor: Color): Paint = Paint().apply {
        asFrameworkPaint().apply {
            isFilterBitmap = true
            color = shadowColor.toArgb()
            maskFilter = android.graphics.BlurMaskFilter(blur, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
    }

    fun outlinePath(): Path {
        val outline = shape.createOutline(size, layoutDirection, this)
        return Path().apply { addOutline(outline) }
    }

    if (pressed) {
        drawIntoCanvas { canvas ->
            val clipPath = outlinePath()
            canvas.drawPath(clipPath, Paint().apply { color = backgroundColor })
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawRect(
                rect = Rect(-size.width, -size.height, size.width * 1.25f, size.height * 0.35f),
                paint = shadowPaint(darkShadowColor.copy(alpha = alpha))
            )
            canvas.drawRect(
                rect = Rect(-size.width * 0.3f, size.height * 0.35f, size.width * 1.3f, size.height * 1.5f),
                paint = shadowPaint(lightShadowColor.copy(alpha = alpha * 0.55f))
            )
            canvas.restore()
        }
    } else {
        drawIntoCanvas { canvas ->
            val path = outlinePath()

            canvas.save()
            canvas.translate(offset, offset)
            canvas.drawPath(path, shadowPaint(darkShadowColor.copy(alpha = alpha)))
            canvas.restore()

            canvas.save()
            canvas.translate(-offset, -offset)
            canvas.drawPath(path, shadowPaint(lightShadowColor.copy(alpha = alpha * 0.8f)))
            canvas.restore()

            canvas.drawPath(path, Paint().apply { color = backgroundColor })
        }
    }
}

/** Soft pastel neumorphic backdrop with two subtle ambient color washes. */
@Composable
fun NeumorphicBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Nm.bg)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Nm.primary.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.06f),
                        radius = size.width * 0.85f
                    ),
                    radius = size.width * 0.85f,
                    center = Offset(size.width * 0.9f, size.height * 0.06f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Nm.violet.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(size.width * 0.0f, size.height * 0.98f),
                        radius = size.width * 1.15f
                    ),
                    radius = size.width * 1.15f,
                    center = Offset(size.width * 0.0f, size.height * 0.98f)
                )
            }
    ) {
        content()
    }
}

/** Raised (embossed) neumorphic tile. Supports click-to-recess. */
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 20.dp,
    corner: Dp = 26.dp,
    backgroundColor: Color = Nm.surface,
    elevation: Dp = 7.dp,
    pressed: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(corner)
    if (onClick != null) {
        val (interactionSource, scale) = rememberNmPress()
        Column(
            modifier = modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .neumorphic(shape = shape, backgroundColor = backgroundColor, elevation = elevation, pressed = scale < 0.985f)
                .clip(shape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(padding)
        ) { content() }
    } else {
        Column(
            modifier = modifier
                .neumorphic(shape = shape, backgroundColor = backgroundColor, elevation = elevation, pressed = pressed)
                .padding(padding)
        ) { content() }
    }
}

@Composable
fun NeumorphicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = Nm.onSurface,
    iconTint: Color = Nm.primary,
    enabled: Boolean = true,
    inset: Boolean = false,
    maxLines: Int = 1,
    textAlign: TextAlign = TextAlign.Start
) {
    val (interactionSource, scale) = rememberNmPress(0.96f)
    val surface = if (inset) Nm.inset else Nm.surface
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.45f }
            .neumorphic(shape = shape, backgroundColor = surface, elevation = if (inset) 3.dp else 7.dp, pressed = scale < 0.985f)
            .clip(shape)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                color = tint,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

/** Circular embossed icon button (or static tile when [onClick] is null). */
@Composable
fun NeumorphicIconTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null,
    tint: Color = Nm.primary,
    size: Dp = 44.dp,
    iconSize: Dp = 19.dp,
    onClick: (() -> Unit)? = null,
    pressed: Boolean = false
) {
    val shape = CircleShape
    if (onClick != null) {
        val (interactionSource, scale) = rememberNmPress(0.9f)
        Box(
            modifier = modifier
                .size(size)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .neumorphic(shape = shape, backgroundColor = Nm.surface, elevation = 5.dp, pressed = scale < 0.985f)
                .clip(shape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .neumorphic(shape = shape, backgroundColor = Nm.surface, elevation = 5.dp, pressed = pressed),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
        }
    }
}

/** Quick action tile — embossed card with a circular icon chip beside the label. */
@Composable
fun NeumorphicActionTile(
    icon: ImageVector,
    label: String,
    tint: Color = Nm.primary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeumorphicCard(modifier = modifier, onClick = onClick, padding = 14.dp, corner = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeumorphicIconTile(icon = icon, tint = tint, size = 38.dp, iconSize = 18.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                color = Nm.onSurface,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Stat value in the mono face. */
@Composable
fun NeumorphicStat(
    label: String,
    value: String,
    valueColor: Color = Nm.onSurface
) {
    Column {
        Text(
            text = label.uppercase(),
            color = Nm.onSurfaceMuted,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            color = valueColor,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            letterSpacing = (-0.5).sp
        )
    }
}

/** Section header with an optional embossed pill action. */
@Composable
fun NeumorphicSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            color = Nm.onSurface,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            val shape = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .neumorphic(shape = shape, backgroundColor = Nm.surface, elevation = 3.dp)
                    .clip(shape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    color = Nm.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Pulsing status dot with a soft halo. */
@Composable
fun NeumorphicStatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 12.dp
) {
    val pulse by rememberInfiniteTransition(label = "nm-dot").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = Nm.EaseOutSoft)),
        label = "nm-dot-pulse"
    )
    Box(modifier = modifier.size(dotSize * 2.6f), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(dotSize * (1.1f + pulse * 1.3f))
                .graphicsLayer { alpha = (1f - pulse) * 0.45f }
                .clip(CircleShape)
                .background(color)
        )
        Box(Modifier.size(dotSize).clip(CircleShape).background(color))
    }
}

/** Deterministic gradient orb inside an embossed ring. */
private val nmAvatarPalettes = listOf(
    listOf(Color(0xFF7C6CFF), Color(0xFF22D3EE)),
    listOf(Color(0xFFA78BFA), Color(0xFFFB7185)),
    listOf(Color(0xFF34D399), Color(0xFF22D3EE)),
    listOf(Color(0xFFFBBF24), Color(0xFFFB7185)),
    listOf(Color(0xFF38BDF8), Color(0xFFA78BFA))
)

@Composable
fun NeumorphicAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    val palette = nmAvatarPalettes[abs(name.hashCode()) % nmAvatarPalettes.size]
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = modifier
            .size(size)
            .neumorphic(shape = CircleShape, backgroundColor = Nm.surface, elevation = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.74f)
                .clip(CircleShape)
                .background(Brush.linearGradient(palette)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Screen-time ring: uniform groove track with a rounded progress arc + center content. */
@Composable
fun NeumorphicProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = 108.dp,
    strokeWidth: Dp = 11.dp,
    color: Color = Nm.primary,
    content: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2 + 1.dp.toPx()
            val diameter = size.minDimension - inset * 2
            val radius = diameter / 2
            val center = Offset(size.width / 2, size.height / 2)
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val clamped = progress.coerceIn(0f, 1f)

            drawArc(
                color = Nm.inset,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            if (clamped > 0.001f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

/** Concave groove progress bar. */
@Composable
fun NeumorphicUsageBar(
    label: String,
    valueLabel: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = Nm.primary
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = Nm.onSurfaceMuted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = valueLabel,
                color = Nm.onSurface,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .neumorphic(shape = RoundedCornerShape(6.dp), backgroundColor = Nm.inset, elevation = 2.dp, pressed = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        }
    }
}

/** Animated count-up duration rendered in the mono face. */
@Composable
fun NeumorphicDuration(
    targetMs: Long,
    modifier: Modifier = Modifier,
    fontSize: Int = 38,
    color: Color = Nm.onSurface
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(targetMs) {
        animated.animateTo(targetMs.toFloat(), tween(900, easing = Nm.EaseOutSoft))
    }
    Text(
        text = formatAuraDuration(animated.value.toLong()),
        modifier = modifier,
        color = color,
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize.sp,
        letterSpacing = (-1).sp
    )
}

/** Empty state — embossed icon tile + copy + optional action. */
@Composable
fun NeumorphicEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NeumorphicIconTile(icon = icon, tint = Nm.primary, size = 88.dp, iconSize = 36.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            color = Nm.onSurface,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = description,
            color = Nm.onSurfaceMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(22.dp))
            NeumorphicButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** Toggle — concave track with a raised knob that slides to the active side. */
@Composable
fun NeumorphicSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 16.dp else 0.dp,
        animationSpec = tween(220, easing = Nm.EaseOutSoft),
        label = "nm-switch"
    )
    val trackShape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 28.dp)
            .neumorphic(
                shape = trackShape,
                backgroundColor = if (checked) Nm.primarySoft else Nm.inset,
                elevation = 3.dp,
                pressed = !checked
            )
            .clip(trackShape)
            .clickable(interactionSource = interactionSource, indication = null) { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .offset(x = knobOffset)
                .neumorphic(
                    shape = CircleShape,
                    backgroundColor = if (checked) Nm.primary else Nm.surface,
                    elevation = 3.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
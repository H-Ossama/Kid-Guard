package com.parentalguard.child.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.parentalguard.child.ui.theme.*

/**
 * Draws a neumorphic tile: soft light shadow (top-left) + soft dark shadow
 * (bottom-right) when raised, or inner shadows when pressed/inset.
 */
fun Modifier.neumorphic(
    shape: Shape = RoundedCornerShape(28.dp),
    backgroundColor: Color = NeumorphicSurface,
    lightShadowColor: Color = NeumorphicLightShadow,
    darkShadowColor: Color = NeumorphicDarkShadow,
    elevation: Dp = 7.dp,
    pressed: Boolean = false
): Modifier = drawBehind {
    val blur = elevation.toPx()
    val offset = elevation.toPx() * 0.9f
    // Dark needs deep drop shadows but only a whisper of top highlight,
    // otherwise cards look washed out. Light keeps the classic soft look.
    val dark = ChildNmTheme.isDark
    val alpha = when {
        dark && pressed -> 0.85f
        dark -> 0.9f
        pressed -> 0.42f
        else -> 0.6f
    }
    val lightAlphaFactor = if (dark) 0.22f else if (pressed) 0.55f else 0.8f

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
                paint = shadowPaint(lightShadowColor.copy(alpha = alpha * lightAlphaFactor))
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
            canvas.drawPath(path, shadowPaint(lightShadowColor.copy(alpha = alpha * lightAlphaFactor)))
            canvas.restore()

            canvas.drawPath(path, Paint().apply { color = backgroundColor })
        }
    }
}

/**
 * Theme-aware neumorphic backdrop with two subtle ambient color washes.
 */
@Composable
fun NeumorphicBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // Slightly stronger washes in dark so depth stays visible on navy.
    val wash1 = if (ChildNmTheme.isDark) 0.16f else 0.08f
    val wash2 = if (ChildNmTheme.isDark) 0.12f else 0.06f
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeumorphicPrimary.copy(alpha = wash1), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.08f),
                        radius = size.width * 0.8f
                    ),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.9f, size.height * 0.08f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeumorphicSuccess.copy(alpha = wash2), Color.Transparent),
                        center = Offset(size.width * 0.0f, size.height * 0.95f),
                        radius = size.width * 1.1f
                    ),
                    radius = size.width * 1.1f,
                    center = Offset(size.width * 0.0f, size.height * 0.95f)
                )
            }
    ) {
        content()
    }
}

/**
 * Raised (embossed) neumorphic tile. Pass [pressed] = true to render an inset tile.
 */
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    backgroundColor: Color = Color.Unspecified,
    elevation: Dp = 7.dp,
    pressed: Boolean = false,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedBackground = if (backgroundColor == Color.Unspecified) {
        MaterialTheme.colorScheme.surface
    } else {
        backgroundColor
    }
    Column(
        modifier = modifier
            .neumorphic(
                shape = shape,
                backgroundColor = resolvedBackground,
                elevation = elevation,
                pressed = pressed
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * Neumorphic button: embossed when idle, recesses into the surface while pressed.
 */
@Composable
fun NeumorphicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    primary: Boolean = true,
    shape: Shape = RoundedCornerShape(22.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressedAnim by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "neumorphic_press"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "neumorphic_scale"
    )

    val surface = if (primary) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    val tint = if (enabled) {
        if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }
    val textColor = if (enabled) {
        if (primary) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .neumorphic(
                shape = shape,
                backgroundColor = surface,
                elevation = if (primary) 7.dp else 4.dp,
                pressed = pressedAnim > 0.5f
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Inset (concave) secondary neumorphic button.
 */
@Composable
fun NeumorphicOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    NeumorphicButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        primary = false
    )
}

/**
 * Circular embossed tile wrapping an icon (used for step icons & lock icon).
 */
@Composable
fun NeumorphicIconTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null,
    tint: Color = NeumorphicPrimary,
    size: Dp = 96.dp,
    iconSize: Dp = 44.dp,
    pressed: Boolean = false
) {
    Box(
        modifier = modifier
            .size(size)
            .neumorphic(
                shape = CircleShape,
                backgroundColor = NeumorphicSurface,
                elevation = 7.dp,
                pressed = pressed
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Small status dot with a soft colored halo.
 */
@Composable
fun NeumorphicStatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 16.dp
) {
    Box(
        modifier = modifier
            .size(dotSize)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.45f), Color.Transparent),
                        radius = dotSize.toPx() * 1.15f
                    ),
                    radius = dotSize.toPx() * 1.15f
                )
                drawCircle(color = color, radius = dotSize.toPx() / 2.4f)
            }
    )
}

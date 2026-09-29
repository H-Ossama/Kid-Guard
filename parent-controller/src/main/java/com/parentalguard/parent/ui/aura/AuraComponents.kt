package com.parentalguard.parent.ui.aura

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parentalguard.parent.ui.theme.MonoFontFamily
import com.parentalguard.parent.ui.theme.trackingFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================================
// AURA · components. Every piece consumes locked tokens from [Aura].
// ============================================================================

// --------------------------------------------------------------------------
// Entrance — staggered rise + fade. Apply as a Modifier, no wrapper needed.
// --------------------------------------------------------------------------

fun Modifier.auraEnter(index: Int = 0): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    val rise = remember { Animatable(30f) }
    LaunchedEffect(Unit) {
        delay((index * Aura.StaggerMs).toLong())
        launch { alpha.animateTo(1f, tween(520, easing = Aura.EaseOutSoft)) }
        launch { rise.animateTo(0f, Aura.springSoft()) }
    }
    graphicsLayer {
        this.alpha = alpha.value
        translationY = rise.value
    }
}

/** Press-scale physics shared by every tactile element. */
@Composable
fun rememberPressScale(pressedScale: Float = 0.965f): Pair<MutableInteractionSource, Float> {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Aura.springSnap(),
        label = "press-scale"
    )
    return interactionSource to scale
}

// --------------------------------------------------------------------------
// Glass card
// --------------------------------------------------------------------------

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 20.dp,
    corner: Dp = 26.dp,
    borderColor: Color = Aura.hairline,
    backgroundColor: Color = Aura.glass,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(corner)
    val base = modifier
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, borderColor, shape)

    if (onClick != null) {
        val (interactionSource, scale) = rememberPressScale()
        Box(
            base
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(padding)
        ) { content() }
    } else {
        Box(base.padding(padding)) { content() }
    }
}

// --------------------------------------------------------------------------
// Buttons
// --------------------------------------------------------------------------

@Composable
fun AuraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    brush: Brush = Aura.hero,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val (interactionSource, scale) = rememberPressScale(0.95f)
    val active = enabled && !loading
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.45f }
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
            .clickable(interactionSource = interactionSource, indication = null, enabled = active, onClick = onClick)
            .padding(vertical = 17.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = trackingFor(0.2.sp)
                )
            }
        }
    }
}

@Composable
fun AuraGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = Aura.textHi,
    enabled: Boolean = true
) {
    val (interactionSource, scale) = rememberPressScale(0.95f)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.45f }
            .clip(RoundedCornerShape(20.dp))
            .background(Aura.glass)
            .border(1.dp, Aura.hairlineStrong, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text = text, color = tint, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AuraIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Aura.textMid,
    contentDescription: String? = null
) {
    val (interactionSource, scale) = rememberPressScale(0.9f)
    Box(
        modifier = modifier
            .size(42.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(Aura.glass)
            .border(1.dp, Aura.hairline, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(19.dp))
    }
}

// --------------------------------------------------------------------------
// Switch — animated thumb + breathing track tint
// --------------------------------------------------------------------------

@Composable
fun AuraSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = Aura.accent
) {
    val fraction by animateFloatAsState(if (checked) 1f else 0f, Aura.springSnap(), label = "switch-x")
    val trackColor by animateColorAsState(
        if (checked) accent.copy(alpha = 0.85f) else Aura.glassStrong,
        Aura.tweenMed(), label = "switch-track"
    )
    Box(
        modifier = modifier
            .width(52.dp)
            .height(32.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
            .border(1.dp, if (checked) Color.Transparent else Aura.hairlineStrong, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Switch
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = (4.dp + (52.dp - 32.dp) * fraction))
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// --------------------------------------------------------------------------
// Segmented control — scrollable pill row, bubble springs to the selection
// --------------------------------------------------------------------------

@Composable
fun AuraSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val metrics = remember { androidx.compose.runtime.mutableStateMapOf<Int, Pair<Float, Float>>() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val bubbleX by animateFloatAsState(
        targetValue = metrics[selectedIndex]?.first ?: 0f,
        animationSpec = Aura.springSnap(),
        label = "segment-x"
    )
    val bubbleW by animateFloatAsState(
        targetValue = metrics[selectedIndex]?.second ?: 0f,
        animationSpec = Aura.springSnap(),
        label = "segment-w"
    )

    // Keep the selected segment visible
    LaunchedEffect(selectedIndex) {
        val m = metrics[selectedIndex] ?: return@LaunchedEffect
        val target = (m.first - 24f).coerceAtLeast(0f).toInt()
        scrollState.animateScrollTo(target)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Aura.glass)
            .border(1.dp, Aura.hairline, RoundedCornerShape(18.dp))
            .padding(4.dp)
    ) {
        Box(modifier = Modifier.horizontalScroll(scrollState, enabled = true)) {
            if (bubbleW > 0f) {
                Box(
                    modifier = Modifier
                        .offset { androidx.compose.ui.unit.IntOffset(bubbleX.toInt(), 0) }
                        .width(with(density) { bubbleW.toDp() })
                        .height(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Aura.heroSoft)
                        .border(1.dp, Aura.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                )
            }
            Row {
                items.forEachIndexed { index, label ->
                    val selected = index == selectedIndex
                    val color by animateColorAsState(if (selected) Aura.textHi else Aura.textMid, Aura.tweenMed(), label = "seg-$index")
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                 metrics[index] = coords.positionInRoot().x to coords.size.width.toFloat()
                            }
                            .height(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(index) }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = color,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// Filter chip
// --------------------------------------------------------------------------

@Composable
fun AuraChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Aura.accent
) {
    val bg by animateColorAsState(if (selected) accent.copy(alpha = 0.18f) else Aura.glass, Aura.tweenMed(), label = "chip-bg")
    val border by animateColorAsState(if (selected) accent.copy(alpha = 0.5f) else Aura.hairline, Aura.tweenMed(), label = "chip-border")
    val text by animateColorAsState(if (selected) Aura.textHi else Aura.textMid, Aura.tweenMed(), label = "chip-text")
    val (interactionSource, scale) = rememberPressScale(0.94f)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(label, color = text, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

// --------------------------------------------------------------------------
// Stat + status atoms
// --------------------------------------------------------------------------

@Composable
fun PulsingDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    val pulse by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = Aura.EaseOutSoft)),
        label = "dot-pulse"
    )
    Box(modifier = modifier.size(size * 2.4f), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size * (1.2f + pulse * 1.2f))
                .graphicsLayer { alpha = (1f - pulse) * 0.5f }
                .clip(CircleShape)
                .background(color)
        )
        Box(Modifier.size(size).clip(CircleShape).background(color))
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            text = text.uppercase(),
            color = color,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = trackingFor(1.sp)
        )
    }
}

/** Animated count-up for millisecond durations, rendered in the mono stat face. */
@Composable
fun AnimatedDuration(
    targetMs: Long,
    modifier: Modifier = Modifier,
    fontSize: Int = 34,
    color: Color = Aura.textHi,
    weight: FontWeight = FontWeight.Bold
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(targetMs) {
        animated.animateTo(targetMs.toFloat(), tween(900, easing = Aura.EaseOutSoft))
    }
    Text(
        text = formatAuraDuration(animated.value.toLong()),
        modifier = modifier,
        color = color,
        fontFamily = MonoFontFamily,
        fontWeight = weight,
        fontSize = fontSize.sp,
        letterSpacing = trackingFor((-1).sp)
    )
}

@Composable
fun StatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Aura.textHi,
    icon: ImageVector? = null,
    iconTint: Color = Aura.accent
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = label.uppercase(),
                color = Aura.textDim,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = trackingFor(1.2.sp),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            color = valueColor,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            letterSpacing = trackingFor((-0.5).sp)
        )
    }
}

// --------------------------------------------------------------------------
// Structure atoms
// --------------------------------------------------------------------------

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Aura.textHi, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Aura.textMid, style = MaterialTheme.typography.bodySmall)
            }
        }
        action?.invoke()
    }
}

@Composable
fun AuraDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Aura.hairline))
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, corner: Dp = 18.dp) {
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100)),
        label = "shimmer-x"
    )
    val base = if (com.parentalguard.parent.ui.theme.LocalAuraDark.current) Color.White.copy(0.05f) else Color(0xFF0B1220).copy(0.05f)
    val glow = if (com.parentalguard.parent.ui.theme.LocalAuraDark.current) Color.White.copy(0.11f) else Color(0xFF0B1220).copy(0.1f)
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(
                Brush.linearGradient(
                    colors = listOf(base, glow, base),
                    start = Offset(shimmer * 600f, 0f),
                    end = Offset(shimmer * 600f + 400f, 0f)
                )
            )
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Aura.heroSoft)
                .border(1.dp, Aura.accent.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Aura.accent, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, color = Aura.textHi, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            description,
            color = Aura.textMid,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(22.dp))
            AuraButton(text = actionLabel, onClick = onAction)
        }
    }
}

// --------------------------------------------------------------------------
// Device avatar — deterministic gradient orb keyed by the device name
// --------------------------------------------------------------------------

private val avatarPalettes = listOf(
    listOf(Color(0xFF7C6CFF), Color(0xFF22D3EE)),
    listOf(Color(0xFFA78BFA), Color(0xFFFB7185)),
    listOf(Color(0xFF34D399), Color(0xFF22D3EE)),
    listOf(Color(0xFFFBBF24), Color(0xFFFB7185)),
    listOf(Color(0xFF38BDF8), Color(0xFFA78BFA))
)

@Composable
fun DeviceAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val palette = avatarPalettes[kotlin.math.abs(name.hashCode()) % avatarPalettes.size]
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = modifier
            .size(size)
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

// --------------------------------------------------------------------------
// Connection badge
// --------------------------------------------------------------------------

@Composable
fun ConnectionPill(local: Boolean, modifier: Modifier = Modifier) {
    StatusPill(
        text = if (local) "LOCAL" else "RELAY",
        color = if (local) com.parentalguard.parent.ui.theme.AuroraSuccess else Aura.accent3,
        modifier = modifier
    )
}

// --------------------------------------------------------------------------
// Small overlay wrapper for transient center-screen badges (e.g. "Saved")
// --------------------------------------------------------------------------

@Composable
fun AuraToast(visible: Boolean, text: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(Aura.tweenMed()) + scaleIn(Aura.springSnap(), initialScale = 0.9f),
        exit = fadeOut(Aura.tweenFast()) + scaleOut(Aura.tweenFast(), targetScale = 0.94f)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Aura.glassStrong)
                .border(1.dp, Aura.hairlineStrong, RoundedCornerShape(50))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulsingDot(Aura.accent3, size = 6.dp)
            Spacer(Modifier.width(10.dp))
            Text(text, color = Aura.textHi, style = MaterialTheme.typography.labelLarge)
        }
    }
}

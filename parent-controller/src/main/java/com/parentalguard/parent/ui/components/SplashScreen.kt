package com.parentalguard.parent.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parentalguard.parent.R
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.ui.neumorphic.NeumorphicBackground
import com.parentalguard.parent.ui.neumorphic.neumorphic
import com.parentalguard.parent.ui.theme.DisplayFontFamily
import com.parentalguard.parent.ui.theme.MonoFontFamily
import com.parentalguard.parent.ui.theme.trackingFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Animated launch state for the parent command center. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val markScale = remember { Animatable(0.72f) }
    val markAlpha = remember { Animatable(0f) }
    val orbit = rememberInfiniteTransition(label = "splash-orbit")
    val orbitRotation by orbit.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "splash-orbit-rotation"
    )
    val pulse by orbit.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = Nm.EaseOutSoft),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "splash-core-pulse"
    )

    LaunchedEffect(Unit) {
        launch {
            markAlpha.animateTo(1f, tween(500))
            markScale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        progress.animateTo(1f, tween(1800, easing = Nm.EaseOutSoft))
        delay(160)
        onFinished()
    }

    NeumorphicBackground {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height * 0.43f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Nm.primary.copy(alpha = 0.16f), Color.Transparent),
                        center = center,
                        radius = size.minDimension * 0.58f
                    ),
                    center = center,
                    radius = size.minDimension * 0.58f
                )
                drawCircle(
                    color = Nm.cyan.copy(alpha = 0.08f),
                    center = Offset(size.width * 0.12f, size.height * 0.18f),
                    radius = 70.dp.toPx()
                )
                drawCircle(
                    color = Nm.violet.copy(alpha = 0.07f),
                    center = Offset(size.width * 0.9f, size.height * 0.78f),
                    radius = 110.dp.toPx()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .neumorphic(shape = CircleShape, backgroundColor = Nm.primary, elevation = 3.dp)
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = stringResource(R.string.app_name).uppercase(),
                        color = Nm.onSurface,
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = trackingFor(2.2.sp)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.splash_secure),
                        color = Nm.onSurfaceMuted,
                        fontFamily = MonoFontFamily,
                        fontSize = 9.sp,
                        letterSpacing = trackingFor(1.2.sp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(238.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            Modifier
                                .size(238.dp)
                                .graphicsLayer { rotationZ = orbitRotation }
                        ) {
                            val stroke = 1.5.dp.toPx()
                            drawCircle(
                                color = Nm.onSurfaceMuted.copy(alpha = 0.14f),
                                style = Stroke(stroke),
                                radius = size.minDimension / 2f - 16.dp.toPx()
                            )
                            drawArc(
                                brush = Brush.sweepGradient(listOf(Nm.primary, Nm.cyan, Color.Transparent, Nm.violet)),
                                startAngle = -28f,
                                sweepAngle = 92f,
                                useCenter = false,
                                style = Stroke(5.dp.toPx(), cap = StrokeCap.Round),
                                topLeft = Offset(16.dp.toPx(), 16.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width - 32.dp.toPx(),
                                    size.height - 32.dp.toPx()
                                )
                            )
                            drawCircle(
                                color = Nm.cyan,
                                radius = 4.dp.toPx(),
                                center = Offset(size.width / 2f, 17.dp.toPx())
                            )
                            rotate(180f) {
                                drawCircle(
                                    color = Nm.primary.copy(alpha = 0.8f),
                                    radius = 3.dp.toPx(),
                                    center = Offset(size.width / 2f, 17.dp.toPx())
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(122.dp)
                                .graphicsLayer {
                                    scaleX = markScale.value * pulse
                                    scaleY = markScale.value * pulse
                                    alpha = markAlpha.value
                                }
                                .neumorphic(
                                    shape = RoundedCornerShape(38.dp),
                                    backgroundColor = Nm.surface,
                                    elevation = 12.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .size(82.dp)
                                    .neumorphic(
                                        shape = CircleShape,
                                        backgroundColor = Nm.inset,
                                        elevation = 3.dp,
                                        pressed = true
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.FamilyRestroom,
                                    contentDescription = null,
                                    tint = Nm.primary,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.app_name).uppercase(),
                        color = Nm.onSurface,
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = trackingFor(5.sp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.splash_tagline),
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        letterSpacing = trackingFor(0.2.sp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphic(
                            shape = RoundedCornerShape(24.dp),
                            backgroundColor = Nm.surface,
                            elevation = 7.dp
                        )
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.splash_status),
                            color = Nm.onSurfaceMuted,
                            fontFamily = MonoFontFamily,
                            fontSize = 10.sp,
                            letterSpacing = trackingFor(1.1.sp)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${(progress.value * 100).toInt()}%",
                            color = Nm.primary,
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .neumorphic(
                                shape = RoundedCornerShape(4.dp),
                                backgroundColor = Nm.inset,
                                elevation = 2.dp,
                                pressed = true
                            )
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress.value.coerceIn(0f, 1f))
                                .height(8.dp)
                                .neumorphic(
                                    shape = RoundedCornerShape(4.dp),
                                    backgroundColor = Nm.primary,
                                    elevation = 1.dp
                                )
                        )
                    }
                }
            }
        }
    }
}

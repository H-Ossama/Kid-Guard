package com.parentalguard.child.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parentalguard.child.R
import com.parentalguard.child.data.RuleRepository
import com.parentalguard.child.ui.components.*
import com.parentalguard.child.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    onRequestUnlock: () -> Unit
) {
    val lockUntil by RuleRepository.globalLockUntil.collectAsState()
    val lockReason by RuleRepository.lockReason.collectAsState()
    val isBreak = lockReason == "BREAK"

    NeumorphicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Cooldown state is hoisted so the reactivate badge next to the
            // shield can read it too.
            val lastRequestTime by RuleRepository.lastUnlockRequestTime.collectAsState()
            val cooldownMs = 15 * 60 * 1000L
            var cooldownRemaining by remember(lastRequestTime) {
                mutableLongStateOf((lastRequestTime + cooldownMs - System.currentTimeMillis()).coerceAtLeast(0))
            }

            LaunchedEffect(lastRequestTime) {
                while (cooldownRemaining > 0) {
                    delay(1000)
                    cooldownRemaining = (lastRequestTime + cooldownMs - System.currentTimeMillis()).coerceAtLeast(0)
                }
            }

            val isCooldownActive = cooldownRemaining > 0
            val context = LocalContext.current
            val reactivatedMsg = stringResource(R.string.lock_screen_reactivated)

            // Animated pulsing icon tile
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Soft colored halo behind the tile
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val glow = if (isBreak) NeumorphicSuccess else NeumorphicPrimary
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(glow.copy(alpha = 0.26f), Color.Transparent)
                                ),
                                radius = size.minDimension / 2
                            )
                        }
                )

                NeumorphicIconTile(
                    icon = if (isBreak) Icons.Default.Warning else Icons.Default.Lock,
                    tint = if (isBreak) NeumorphicWarning else NeumorphicPrimary,
                    size = 160.dp,
                    iconSize = 64.dp
                )
            }
                // Reactivate badge: while the 15-minute request cooldown runs,
                // one tap clears it so another request can be sent immediately.
                if (isCooldownActive) {
                    NeumorphicIconTile(
                        icon = Icons.Default.NotificationsActive,
                        tint = NeumorphicWarning,
                        size = 48.dp,
                        iconSize = 22.dp,
                        contentDescription = stringResource(R.string.lock_screen_reactivate_desc),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clickable {
                                RuleRepository.clearUnlockRequestCooldown()
                                android.widget.Toast.makeText(
                                    context, reactivatedMsg,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = if (isBreak) stringResource(R.string.take_a_break_title) else stringResource(R.string.lock_screen_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).dp.toSp()
                ),
                color = NeumorphicOnSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            val breakDurationMinutes = remember(lockUntil) {
                val diff = lockUntil - System.currentTimeMillis()
                (diff / 60000).coerceAtLeast(1)
            }

            Text(
                text = if (isBreak)
                    stringResource(R.string.take_a_break_message, breakDurationMinutes)
                    else stringResource(R.string.lock_screen_message),
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.dp.toSp(),
                    letterSpacing = 0.25.dp.toSp()
                ),
                color = NeumorphicOnSurfaceMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (lockUntil > 0) {
                Spacer(modifier = Modifier.height(28.dp))

                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NeumorphicSurfaceInset,
                    elevation = 5.dp,
                    pressed = true,
                    contentPadding = 20.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var timeRemaining by remember(lockUntil) { mutableStateOf("00:00:00") }

                        LaunchedEffect(lockUntil) {
                            while(true) {
                                val diff = lockUntil - System.currentTimeMillis()
                                if (diff <= 0) {
                                    timeRemaining = "00:00:00"
                                    break
                                } else {
                                    val hours = diff / 3600000
                                    val minutes = (diff % 3600000) / 60000
                                    val seconds = (diff % 60000) / 1000
                                    timeRemaining = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                                }
                                delay(1000)
                            }
                        }

                        Text(
                            text = if (isBreak) "FREE IN" else "LOCKED FOR",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.dp.toSp()
                            ),
                            color = NeumorphicOnSurfaceMuted
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = timeRemaining,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.dp.toSp()
                            ),
                            color = NeumorphicPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1.2f))

            val allowExtensions by RuleRepository.allowExtensions.collectAsState()
            val educationOnly by RuleRepository.educationOnly.collectAsState()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isBreak && allowExtensions) {
                    NeumorphicOutlinedButton(
                        text = stringResource(R.string.btn_one_more_minute),
                        onClick = {
                            RuleRepository.updateLastUnlockRequestTime()
                            com.parentalguard.child.utils.EventHelper.sendExtensionRequest(com.parentalguard.child.ChildApp.instance)
                        },
                        // reuse cooldown if it's the same request type, but usually extensions are once per break
                        enabled = !isCooldownActive,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                }

                if (isBreak && educationOnly) {
                    NeumorphicButton(
                        text = stringResource(R.string.btn_learning_mode),
                        onClick = {
                            // The monitor service will allow EDUCATION apps.
                            // Tapping this just hides the overlay so the user can go use them.
                            onRequestUnlock() // We reuse the callback to hide the overlay
                        },
                        icon = Icons.Default.LockOpen,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                }

                NeumorphicButton(
                    text = if (isCooldownActive) {
                        val mins = cooldownRemaining / 60000
                        val secs = (cooldownRemaining % 60000) / 1000
                        stringResource(R.string.lock_screen_cooldown_btn, String.format("%02d:%02d", mins, secs))
                    } else {
                        stringResource(R.string.lock_screen_request_btn)
                    },
                    onClick = {
                        RuleRepository.updateLastUnlockRequestTime()
                        onRequestUnlock()
                    },
                    enabled = !isCooldownActive,
                    icon = if (isCooldownActive) Icons.Default.Lock else Icons.Default.LockOpen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BlackoutLockScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}

@Composable
fun QuietFocusLockScreen(
    onRequestUnlock: () -> Unit
) {
    val lockUntil by RuleRepository.globalLockUntil.collectAsState()
    val lockReason by RuleRepository.lockReason.collectAsState()
    val isBreak = lockReason == "BREAK"
    val breakDurationMinutes = remember(lockUntil) {
        ((lockUntil - System.currentTimeMillis()) / 60000).coerceAtLeast(1)
    }
    var timeRemaining by remember(lockUntil) { mutableStateOf("00:00:00") }
    val lastRequestTime by RuleRepository.lastUnlockRequestTime.collectAsState()
    var cooldownRemaining by remember(lastRequestTime) {
        mutableLongStateOf((lastRequestTime + 15 * 60 * 1000L - System.currentTimeMillis()).coerceAtLeast(0))
    }

    LaunchedEffect(lockUntil) {
        while (true) {
            val diff = lockUntil - System.currentTimeMillis()
            if (diff <= 0) {
                timeRemaining = "00:00:00"
                break
            }
            val hours = diff / 3600000
            val minutes = (diff % 3600000) / 60000
            val seconds = (diff % 60000) / 1000
            timeRemaining = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            delay(1000)
        }
    }

    LaunchedEffect(lastRequestTime) {
        while (cooldownRemaining > 0) {
            delay(1000)
            cooldownRemaining = (lastRequestTime + 15 * 60 * 1000L - System.currentTimeMillis()).coerceAtLeast(0)
        }
    }

    val context = LocalContext.current
    val reactivatedMsg = stringResource(R.string.lock_screen_reactivated)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111936))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(Color(0xFF62E6D6).copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Timer, null, tint = Color(0xFF62E6D6), modifier = Modifier.size(42.dp))
            }
                if (cooldownRemaining > 0) {
                    NeumorphicIconTile(
                        icon = Icons.Default.NotificationsActive,
                        tint = NeumorphicWarning,
                        size = 44.dp,
                        iconSize = 20.dp,
                        contentDescription = stringResource(R.string.lock_screen_reactivate_desc),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clickable {
                                RuleRepository.clearUnlockRequestCooldown()
                                android.widget.Toast.makeText(
                                    context, reactivatedMsg,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                    )
                }
            }
            Text(
                text = stringResource(R.string.quiet_focus_title),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isBreak) stringResource(R.string.take_a_break_message, breakDurationMinutes) else stringResource(R.string.quiet_focus_message),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            if (lockUntil > 0) {
                Text(
                    text = timeRemaining,
                    color = Color(0xFF62E6D6),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            NeumorphicButton(
                text = if (cooldownRemaining > 0) {
                    val mins = cooldownRemaining / 60000
                    val secs = (cooldownRemaining % 60000) / 1000
                    stringResource(R.string.lock_screen_cooldown_btn, String.format("%02d:%02d", mins, secs))
                } else stringResource(R.string.lock_screen_request_btn),
                onClick = {
                    RuleRepository.updateLastUnlockRequestTime()
                    onRequestUnlock()
                },
                enabled = cooldownRemaining <= 0,
                icon = if (cooldownRemaining > 0) Icons.Default.Lock else Icons.Default.LockOpen,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
        }
    }
}

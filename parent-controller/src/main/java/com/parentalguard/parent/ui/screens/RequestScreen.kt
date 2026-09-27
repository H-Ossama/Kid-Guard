package com.parentalguard.parent.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parentalguard.parent.R
import com.parentalguard.parent.ui.aura.auraEnter
import com.parentalguard.parent.ui.neumorphic.NeumorphicBackground
import com.parentalguard.parent.ui.neumorphic.NeumorphicButton
import com.parentalguard.parent.ui.neumorphic.NeumorphicCard
import com.parentalguard.parent.ui.neumorphic.NeumorphicIconTile
import com.parentalguard.parent.ui.neumorphic.NeumorphicSectionHeader
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.ui.neumorphic.neumorphic
import com.parentalguard.parent.ui.neumorphic.rememberNmPress
import com.parentalguard.parent.ui.theme.MonoFontFamily
import com.parentalguard.parent.viewmodel.ChildDevice
import com.parentalguard.parent.viewmodel.DeviceControlViewModel
import com.parentalguard.parent.viewmodel.DiscoveryViewModel
import java.net.InetAddress

@Composable
fun RequestScreen(
    deviceId: String,
    deviceName: String,
    requestType: String,
    appPackageName: String? = null,
    appName: String? = null,
    viewModel: DeviceControlViewModel,
    discoveryViewModel: DiscoveryViewModel? = null,
    onBack: () -> Unit
) {
    val knownDevices = discoveryViewModel?.devices?.collectAsState()?.value ?: emptyList()
    val resolvedDevice = remember(deviceId, knownDevices) {
        knownDevices.find { it.deviceId == deviceId }
    }
    // If the screen opened from a notification before devices finished loading
    // (cold start), there is nothing to resolve against yet — refresh once so
    // the real device (IP, token, MAC) replaces the placeholder below.
    androidx.compose.runtime.LaunchedEffect(deviceId, knownDevices.size) {
        if (resolvedDevice == null) discoveryViewModel?.refreshDevices()
    }
    val device = resolvedDevice
        ?: ChildDevice(
            deviceId = deviceId,
            name = deviceName,
            ip = InetAddress.getByName("127.0.0.1"),
            port = 8080,
            customName = deviceName
        )

    var showCustomDialog by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("") }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = Nm.surface,
            title = { Text(stringResource(R.string.dialog_custom_duration_title), color = Nm.onSurface, style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { if (it.all(Char::isDigit)) customMinutes = it },
                    label = { Text(stringResource(R.string.dialog_custom_duration_label)) },
                    placeholder = { Text(stringResource(R.string.placeholder_duration_example)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Nm.onSurface,
                        unfocusedTextColor = Nm.onSurface,
                        focusedBorderColor = Nm.primary,
                        unfocusedBorderColor = Nm.darkShadow.copy(alpha = 0.25f),
                        focusedLabelColor = Nm.primary,
                        unfocusedLabelColor = Nm.onSurfaceMuted,
                        focusedContainerColor = Nm.surface,
                        unfocusedContainerColor = Nm.surface,
                        cursorColor = Nm.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mins = customMinutes.toIntOrNull() ?: 0
                        if (mins > 0) {
                            viewModel.approveUnlockRequest(device, mins, appPackageName, discoveryViewModel)
                            showCustomDialog = false
                            onBack()
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_approve), color = Nm.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text(stringResource(R.string.cancel), color = Nm.onSurfaceMuted)
                }
            }
        )
    }

    NeumorphicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                NeumorphicIconTile(icon = Icons.Default.ArrowBack, onClick = onBack, contentDescription = stringResource(R.string.back))
            }

            Spacer(Modifier.height(30.dp))

            // Pulsing beacon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.auraEnter(0)
            ) {
                val pulse by rememberInfiniteTransition(label = "beacon").animateFloat(
                    initialValue = 0.92f, targetValue = 1.12f,
                    animationSpec = infiniteRepeatable(tween(1900, easing = Nm.EaseOutSoft), RepeatMode.Reverse),
                    label = "beacon-scale"
                )
                Box(
                    Modifier
                        .size(150.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = 0.35f }
                        .neumorphic(shape = CircleShape, backgroundColor = Nm.surface, elevation = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .neumorphic(shape = CircleShape, backgroundColor = Nm.inset, elevation = 2.dp, pressed = true),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (requestType == "APP") Icons.Default.Apps else Icons.Default.PhonelinkLock,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = Nm.primary
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.auraEnter(1)
            ) {
                Text(
                    text = deviceName,
                    color = Nm.onSurface,
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when (requestType) {
                        "EXTENSION" -> stringResource(R.string.request_wants_time)
                        "APP" -> stringResource(R.string.request_wants_app) + " $appName"
                        else -> stringResource(R.string.request_wants_device)
                    },
                    color = Nm.onSurfaceMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(34.dp))

            if (requestType == "EXTENSION") {
                Column(
                    modifier = Modifier.auraEnter(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeumorphicButton(
                        text = stringResource(R.string.request_approve_extension),
                        onClick = {
                            viewModel.approveExtension(device)
                            onBack()
                        },
                        icon = Icons.Default.Add,
                        tint = Nm.primary,
                        iconTint = Nm.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NeumorphicButton(
                        text = stringResource(R.string.action_deny_request),
                        onClick = {
                            viewModel.denyExtension(device)
                            onBack()
                        },
                        icon = Icons.Default.Close,
                        tint = Nm.danger,
                        iconTint = Nm.danger,
                        inset = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NeumorphicButton(
                        text = stringResource(R.string.request_allow_new),
                        onClick = { viewModel.resetRequestCooldown(device) },
                        icon = Icons.Default.Add,
                        tint = Nm.onSurfaceMuted,
                        iconTint = Nm.onSurfaceMuted,
                        inset = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column(
                    modifier = Modifier.auraEnter(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeumorphicCard(padding = 18.dp, corner = 24.dp) {
                        NeumorphicSectionHeader(title = stringResource(R.string.request_grant_access))
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DurationChip(label = "10m", modifier = Modifier.weight(1f)) {
                                viewModel.approveUnlockRequest(device, 10, appPackageName, discoveryViewModel)
                                onBack()
                            }
                            DurationChip(label = "30m", modifier = Modifier.weight(1f)) {
                                viewModel.approveUnlockRequest(device, 30, appPackageName, discoveryViewModel)
                                onBack()
                            }
                            DurationChip(label = stringResource(R.string.label_custom), modifier = Modifier.weight(1f)) {
                                showCustomDialog = true
                            }
                        }
                    }

                    NeumorphicButton(
                        text = if (requestType == "APP") stringResource(R.string.request_unlock_permanent) else stringResource(R.string.unlock_device),
                        onClick = {
                            if (requestType == "APP" && appPackageName != null) {
                                viewModel.toggleAppBlock(device, appPackageName, discoveryViewModel)
                            } else {
                                viewModel.lockDevice(device, false, discoveryViewModel)
                            }
                            onBack()
                        },
                        icon = Icons.Default.LockOpen,
                        tint = Nm.primary,
                        iconTint = Nm.primary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    NeumorphicButton(
                        text = stringResource(R.string.action_deny_request),
                        onClick = {
                            viewModel.denyUnlockRequest(device)
                            onBack()
                        },
                        icon = Icons.Default.Block,
                        tint = Nm.danger,
                        iconTint = Nm.danger,
                        inset = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NeumorphicButton(
                        text = stringResource(R.string.request_allow_new),
                        onClick = { viewModel.resetRequestCooldown(device) },
                        icon = Icons.Default.Add,
                        tint = Nm.onSurfaceMuted,
                        iconTint = Nm.onSurfaceMuted,
                        inset = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DurationChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (interactionSource, scale) = rememberNmPress(0.92f)
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .neumorphic(shape = shape, backgroundColor = Nm.surface, elevation = 5.dp)
            .clip(shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Nm.primary,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

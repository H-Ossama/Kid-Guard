package com.parentalguard.parent.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.parentalguard.parent.R
import com.parentalguard.parent.ui.aura.auraEnter
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.ui.neumorphic.NeumorphicAvatar
import com.parentalguard.parent.ui.neumorphic.NeumorphicBackground
import com.parentalguard.parent.ui.neumorphic.NeumorphicButton
import com.parentalguard.parent.ui.neumorphic.NeumorphicCard
import com.parentalguard.parent.ui.neumorphic.NeumorphicEmptyState
import com.parentalguard.parent.ui.neumorphic.NeumorphicIconTile
import com.parentalguard.parent.ui.neumorphic.NeumorphicSectionHeader
import com.parentalguard.parent.ui.neumorphic.NeumorphicStatusDot
import com.parentalguard.parent.ui.neumorphic.NeumorphicStatusPill
import com.parentalguard.parent.ui.neumorphic.neumorphic
import com.parentalguard.parent.ui.neumorphic.rememberNmPress
import com.parentalguard.parent.ui.theme.MonoFontFamily
import com.parentalguard.parent.viewmodel.BluetoothDeviceCandidate
import com.parentalguard.parent.viewmodel.ChildDevice
import com.parentalguard.parent.viewmodel.ConnectionType
import com.parentalguard.parent.viewmodel.DeviceStatusSummary

@Composable
fun CircleScreen(
    devices: List<ChildDevice>,
    isScanning: Boolean,
    bluetoothCandidates: List<BluetoothDeviceCandidate>,
    isBluetoothScanning: Boolean,
    deviceStatuses: Map<String, DeviceStatusSummary>,
    onStartScan: () -> Unit,
    onStartBluetoothScan: () -> Unit,
    onStopBluetoothScan: () -> Unit,
    onConnectBluetooth: (BluetoothDeviceCandidate) -> Unit,
    onDeviceSelected: (ChildDevice) -> Unit,
    onScanQR: () -> Unit,
    onResetAll: () -> Unit,
    onRemoveDevice: (ChildDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var devicePendingRemoval by remember { mutableStateOf<ChildDevice?>(null) }
    var showBluetoothDialog by remember { mutableStateOf(false) }
    var showBluetoothRequiredDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val hasBluetoothPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.BLUETOOTH_SCAN] == true &&
            result[Manifest.permission.BLUETOOTH_CONNECT] == true
        if (granted) {
            if (BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) {
                showBluetoothDialog = true
                onStartBluetoothScan()
            } else {
                showBluetoothRequiredDialog = true
            }
        }
    }
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) {
            showBluetoothDialog = true
            onStartBluetoothScan()
        } else {
            showBluetoothRequiredDialog = true
        }
    }

    fun openBluetoothPairing() {
        if (!hasBluetoothPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothPermissionLauncher.launch(
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                )
            }
        } else if (BluetoothAdapter.getDefaultAdapter()?.isEnabled != true) {
            showBluetoothRequiredDialog = true
        } else {
            showBluetoothDialog = true
            onStartBluetoothScan()
        }
    }

    if (showBluetoothRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showBluetoothRequiredDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = Nm.surface,
            title = { Text(stringResource(R.string.bluetooth_required_title), color = Nm.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.bluetooth_required_desc), color = Nm.onSurfaceMuted) },
            confirmButton = {
                TextButton(onClick = {
                    showBluetoothRequiredDialog = false
                    bluetoothEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }) {
                    Text(stringResource(R.string.bluetooth_enable_action), color = Nm.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBluetoothRequiredDialog = false }) {
                    Text(stringResource(R.string.cancel), color = Nm.onSurfaceMuted)
                }
            }
        )
    }

    LaunchedEffect(Unit) { onStartScan() }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = Nm.surface,
            title = { Text(stringResource(R.string.circle_reset_title), color = Nm.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.circle_reset_desc), color = Nm.onSurfaceMuted) },
            confirmButton = {
                TextButton(onClick = { onResetAll(); showResetDialog = false }) {
                    Text(stringResource(R.string.circle_reset_action), color = Nm.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancel), color = Nm.onSurfaceMuted) }
            }
        )
    }

    devicePendingRemoval?.let { pending ->
        AlertDialog(
            onDismissRequest = { devicePendingRemoval = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = Nm.surface,
            title = { Text(stringResource(R.string.circle_remove_title), color = Nm.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.circle_remove_desc, pending.customName.ifBlank { pending.name }), color = Nm.onSurfaceMuted) },
            confirmButton = {
                TextButton(onClick = { onRemoveDevice(pending); devicePendingRemoval = null }) {
                    Text(stringResource(R.string.circle_remove_action), color = Nm.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { devicePendingRemoval = null }) { Text(stringResource(R.string.cancel), color = Nm.onSurfaceMuted) }
            }
        )
    }

    if (showBluetoothDialog) {
        BluetoothPairDialog(
            candidates = bluetoothCandidates,
            isScanning = isBluetoothScanning,
            onStartScan = { openBluetoothPairing() },
            onConnect = { candidate ->
                onConnectBluetooth(candidate)
                onStopBluetoothScan()
            },
            onDismiss = {
                onStopBluetoothScan()
                showBluetoothDialog = false
            }
        )
    }

    NeumorphicBackground {
        LazyColumn(
            modifier = modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(Modifier.auraEnter(0)) {
                    Text(
                        text = stringResource(R.string.circle_title),
                        color = Nm.onSurface,
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.circle_subtitle),
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // -- Radar ----------------------------------------------------------
            item {
                NeumorphicCard(modifier = Modifier.fillMaxWidth().auraEnter(1), corner = 30.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RadarSweep(scanning = isScanning)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (isScanning) stringResource(R.string.circle_scanning) else stringResource(R.string.circle_radar_hint),
                            color = Nm.onSurfaceMuted,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NeumorphicButton(
                                text = stringResource(R.string.circle_pair_qr),
                                onClick = onScanQR,
                                icon = Icons.Default.QrCodeScanner,
                                modifier = Modifier.weight(1f)
                            )
                            NeumorphicButton(
                                text = stringResource(R.string.circle_scan_network),
                                onClick = onStartScan,
                                icon = Icons.Default.Wifi,
                                inset = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        NeumorphicButton(
                            text = stringResource(R.string.circle_pair_bluetooth),
                            onClick = { openBluetoothPairing() },
                            icon = if (isBluetoothScanning) Icons.Default.BluetoothSearching else Icons.Default.Bluetooth,
                            inset = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // -- Devices ----------------------------------------------------------
            if (devices.isNotEmpty()) {
                item {
                    Column(Modifier.auraEnter(2)) {
                        NeumorphicSectionHeader(
                            title = stringResource(R.string.section_connected_devices)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.circle_hold_hint),
                            color = Nm.onSurfaceMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                itemsIndexed(devices, key = { _, d -> d.deviceId }) { index, device ->
                        Box(Modifier.auraEnter(3 + index)) {
                            CircleDeviceRow(
                                device = device,
                                status = deviceStatuses[device.deviceId],
                                connectionType = deviceStatuses[device.deviceId]?.connectionType ?: ConnectionType.UNKNOWN,
                            onClick = { onDeviceSelected(device) },
                            onLongClick = { devicePendingRemoval = device }
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .auraEnter(devices.size + 4)
                            .padding(top = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showResetDialog = true }
                                .padding(10.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, null, tint = Nm.danger, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.circle_reset_action),
                                color = Nm.danger,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else if (!isScanning) {
                item {
                    NeumorphicCard(modifier = Modifier.fillMaxWidth().auraEnter(2), padding = 8.dp) {
                        NeumorphicEmptyState(
                            icon = Icons.Default.Radar,
                            title = stringResource(R.string.empty_no_devices_found),
                            description = stringResource(R.string.empty_no_devices_desc),
                            actionLabel = stringResource(R.string.circle_pair_qr),
                            onAction = onScanQR
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// Radar — neumorphic: recessed rings, a soft sweep, and a raised center tile.
// --------------------------------------------------------------------------

@Composable
private fun RadarSweep(scanning: Boolean) {
    val sweep by rememberInfiniteTransition(label = "radar").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (scanning) 1600 else 5200, easing = LinearEasing)),
        label = "radar-sweep"
    )
    Box(modifier = Modifier.size(168.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(168.dp)) {
            val c = center
            listOf(0.33f, 0.66f, 1f).forEach { f ->
                drawCircle(
                    color = Nm.divider,
                    radius = size.minDimension / 2f * f,
                    center = c,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Transparent, Nm.primary.copy(alpha = if (scanning) 0.55f else 0.22f)),
                    center = c
                ),
                startAngle = sweep,
                sweepAngle = 70f,
                useCenter = true
            )
        }
        Box(
            modifier = Modifier
                .size(58.dp)
                .neumorphic(shape = RoundedCornerShape(29.dp), backgroundColor = Nm.surface, elevation = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Radar, null, tint = Nm.primary, modifier = Modifier.size(24.dp))
        }
    }
}

// --------------------------------------------------------------------------
// Device row — tap opens the console, long-press offers removal.
// --------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CircleDeviceRow(
    device: ChildDevice,
    status: DeviceStatusSummary?,
    connectionType: ConnectionType,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (interactionSource, scale) = rememberNmPress()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .neumorphic(shape = RoundedCornerShape(22.dp), backgroundColor = Nm.surface, elevation = 7.dp)
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp)
    ) {
        val online = status?.isOnline == true
        val transportLabel = when (connectionType) {
            ConnectionType.LOCAL -> stringResource(R.string.connection_local)
            ConnectionType.BLUETOOTH -> stringResource(R.string.status_bt)
            ConnectionType.CLOUD -> stringResource(R.string.connection_relay)
            ConnectionType.UNKNOWN -> null
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeumorphicAvatar(name = device.customName.ifBlank { device.name })
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = device.customName.ifBlank { device.name },
                    color = Nm.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeumorphicStatusDot(
                        color = when {
                            online -> Nm.success
                            status != null -> Nm.danger
                            else -> Nm.onSurfaceMuted
                        },
                        dotSize = 5.dp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (online && transportLabel != null) {
                            "${device.ip.hostAddress ?: ""} · $transportLabel"
                        } else {
                            device.ip.hostAddress ?: ""
                        },
                        color = Nm.onSurfaceMuted,
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp
                    )
                }
            }
            // Big, glanceable presence pill: green ONLINE / red OFFLINE.
            // Fed by the 15 s presence heartbeat (direct → BT → relay).
            NeumorphicStatusPill(
                text = if (online) {
                    if (transportLabel != null) {
                        "${stringResource(R.string.status_online)} · $transportLabel"
                    } else {
                        stringResource(R.string.status_online)
                    }
                } else {
                    stringResource(R.string.status_offline)
                },
                color = when {
                    online -> Nm.success
                    status != null -> Nm.danger
                    else -> Nm.onSurfaceMuted
                }
            )
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ChevronRight, null, tint = Nm.onSurfaceMuted, modifier = Modifier.size(20.dp))
        }
    }
}

// --------------------------------------------------------------------------
// Bluetooth pairing dialog — scans for PG_Child_<id> devices and connects.
// --------------------------------------------------------------------------

@Composable
private fun BluetoothPairDialog(
    candidates: List<BluetoothDeviceCandidate>,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onConnect: (BluetoothDeviceCandidate) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Nm.surface,
        title = { Text(stringResource(R.string.bluetooth_pair_title), color = Nm.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.bluetooth_pair_desc),
                    color = Nm.onSurfaceMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))

                if (isScanning) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(color = Nm.primary, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.bluetooth_scanning), color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(14.dp))
                }

                if (candidates.isEmpty() && !isScanning) {
                    NeumorphicEmptyState(
                        icon = Icons.Default.BluetoothSearching,
                        title = stringResource(R.string.bluetooth_none_found),
                        description = stringResource(R.string.bluetooth_none_found_desc)
                    )
                }

                candidates.forEachIndexed { index, candidate ->
                    BluetoothCandidateRow(
                        candidate = candidate,
                        onConnect = { onConnect(candidate) }
                    )
                    if (index < candidates.lastIndex) Spacer(Modifier.height(10.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onStartScan) {
                Text(stringResource(R.string.bluetooth_scan_action), color = Nm.primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), color = Nm.onSurfaceMuted)
            }
        }
    )
}

@Composable
private fun BluetoothCandidateRow(
    candidate: BluetoothDeviceCandidate,
    onConnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphic(shape = RoundedCornerShape(18.dp), backgroundColor = Nm.inset, elevation = 2.dp, pressed = true)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeumorphicIconTile(
            icon = if (candidate.alreadyPaired) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
            tint = if (candidate.alreadyPaired) Nm.success else Nm.primary,
            size = 40.dp,
            iconSize = 18.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = candidate.name.ifBlank { candidate.deviceId },
                color = Nm.onSurface,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = candidate.mac,
                color = Nm.onSurfaceMuted,
                fontFamily = MonoFontFamily,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        if (candidate.alreadyPaired) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = Nm.success, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.bluetooth_in_circle),
                    color = Nm.success,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            val (interactionSource, scale) = rememberNmPress(0.94f)
            Box(
                modifier = Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .neumorphic(shape = RoundedCornerShape(50), backgroundColor = Nm.surface, elevation = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(interactionSource = interactionSource, indication = null, onClick = onConnect)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.bluetooth_connect),
                    color = Nm.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

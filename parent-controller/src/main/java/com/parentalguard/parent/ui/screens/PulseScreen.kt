package com.parentalguard.parent.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parentalguard.parent.R
import com.parentalguard.parent.ui.aura.auraEnter
import com.parentalguard.parent.ui.aura.formatAuraDuration
import com.parentalguard.parent.ui.neumorphic.NeumorphicActionTile
import com.parentalguard.parent.ui.neumorphic.NeumorphicAvatar
import com.parentalguard.parent.ui.neumorphic.NeumorphicBackground
import com.parentalguard.parent.ui.neumorphic.NeumorphicButton
import com.parentalguard.parent.ui.neumorphic.NeumorphicCard
import com.parentalguard.parent.ui.neumorphic.NeumorphicDuration
import com.parentalguard.parent.ui.neumorphic.NeumorphicEmptyState
import com.parentalguard.parent.ui.neumorphic.NeumorphicIconTile
import com.parentalguard.parent.ui.neumorphic.NeumorphicProgressRing
import com.parentalguard.parent.ui.neumorphic.NeumorphicSectionHeader
import com.parentalguard.parent.ui.neumorphic.NeumorphicStat
import com.parentalguard.parent.ui.neumorphic.NeumorphicStatusDot
import com.parentalguard.parent.ui.neumorphic.NeumorphicUsageBar
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.ui.theme.MonoFontFamily
import com.parentalguard.parent.ui.theme.trackingFor
import com.parentalguard.parent.viewmodel.ChildDevice
import com.parentalguard.parent.viewmodel.ConnectionType
import com.parentalguard.parent.viewmodel.DeviceStatusSummary
import java.util.Calendar

private const val DAILY_GOAL_MS = 8L * 60 * 60 * 1000

@Composable
fun PulseScreen(
    devices: List<ChildDevice>,
    deviceStatuses: Map<String, DeviceStatusSummary>,
    onDeviceClick: (ChildDevice) -> Unit,
    onViewAllDevices: () -> Unit,
    onScanQR: () -> Unit,
    onLockAll: () -> Unit,
    onRefresh: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenDeviceOwnerGuide: () -> Unit = {},
    onToggleDeviceLock: (ChildDevice) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalScreenTime = devices.sumOf { deviceStatuses[it.deviceId]?.todayScreenTimeMs ?: 0L }
    val onlineCount = devices.count { deviceStatuses[it.deviceId]?.isOnline == true }
    val lockedCount = devices.count { deviceStatuses[it.deviceId]?.isLocked == true }
    val offlineCount = devices.size - onlineCount
    val attentionCount = lockedCount + if (devices.isEmpty()) 0 else offlineCount
    val goalFraction = (totalScreenTime / (DAILY_GOAL_MS * devices.size).toFloat()).coerceIn(0f, 1f)

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = stringResource(
        when {
            hour < 12 -> R.string.pulse_greeting_morning
            hour < 18 -> R.string.pulse_greeting_afternoon
            else -> R.string.pulse_greeting_evening
        }
    )

    NeumorphicBackground {
        LazyColumn(
            modifier = modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -- Masthead -------------------------------------------------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().auraEnter(0),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = greeting.uppercase(),
                            color = Nm.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = trackingFor(2.5.sp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.pulse_title),
                            color = Nm.onSurface,
                            style = MaterialTheme.typography.displayMedium
                        )
                    }
                    NeumorphicIconTile(
                        icon = Icons.Default.HelpOutline,
                        onClick = onOpenDeviceOwnerGuide,
                        contentDescription = stringResource(R.string.device_owner_guide_title),
                        tint = Nm.primary,
                        size = 42.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    NeumorphicIconTile(
                        icon = Icons.Default.Refresh,
                        onClick = onRefresh,
                        contentDescription = stringResource(R.string.refresh_stats),
                        tint = Nm.onSurfaceMuted,
                        size = 46.dp
                    )
                }
            }

            if (devices.isEmpty()) {
                item {
                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth().auraEnter(1),
                        padding = 8.dp
                    ) {
                        NeumorphicEmptyState(
                            icon = Icons.Default.DevicesOther,
                            title = stringResource(R.string.pulse_empty_title),
                            description = stringResource(R.string.pulse_empty_desc),
                            actionLabel = stringResource(R.string.pulse_pair_first),
                            onAction = onScanQR
                        )
                    }
                }
            } else {
                // -- Hero: family screen-time ring -------------------------------
                item {
                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth().auraEnter(1),
                        corner = 30.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.pulse_screen_time_today).uppercase(),
                                    color = Nm.onSurfaceMuted,
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = trackingFor(1.4.sp),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(8.dp))
                                NeumorphicDuration(targetMs = totalScreenTime, fontSize = 38)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.pulse_goal),
                                    color = Nm.onSurfaceMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            NeumorphicProgressRing(
                                progress = goalFraction,
                                ringSize = 108.dp,
                                strokeWidth = 11.dp
                            ) {
                                Text(
                                    text = "${(goalFraction * 100).toInt()}%",
                                    color = Nm.onSurface,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }
                }

                // -- Stat strip ---------------------------------------------------
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().auraEnter(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeumorphicCard(modifier = Modifier.weight(1f), padding = 14.dp, corner = 20.dp) {
                            NeumorphicStat(
                                label = stringResource(R.string.status_devices),
                                value = "${devices.size}"
                            )
                        }
                        NeumorphicCard(modifier = Modifier.weight(1f), padding = 14.dp, corner = 20.dp) {
                            NeumorphicStat(
                                label = stringResource(R.string.status_online),
                                value = "$onlineCount",
                                valueColor = Nm.success
                            )
                        }
                        NeumorphicCard(modifier = Modifier.weight(1f), padding = 14.dp, corner = 20.dp) {
                            NeumorphicStat(
                                label = stringResource(R.string.pulse_shields_up),
                                value = "$lockedCount",
                                valueColor = if (lockedCount > 0) Nm.danger else Nm.onSurface
                            )
                        }
                    }
                }

                // -- Attention banner --------------------------------------------
                if (attentionCount > 0) {
                    item {
                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth().auraEnter(3),
                            onClick = onViewAllDevices,
                            padding = 16.dp,
                            corner = 20.dp,
                            backgroundColor = Nm.inset,
                            elevation = 4.dp
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NeumorphicStatusDot(Nm.danger, dotSize = 8.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.pulse_attention, attentionCount),
                                    color = Nm.onSurface,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.NorthEast, null,
                                    tint = Nm.danger,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // -- Circle preview ----------------------------------------------
                item {
                    Box(Modifier.auraEnter(4)) {
                        NeumorphicSectionHeader(
                            title = stringResource(R.string.pulse_your_circle),
                            actionLabel = stringResource(R.string.view_all),
                            onAction = onViewAllDevices
                        )
                    }
                }

                itemsIndexed(devices.take(4), key = { _, d -> d.deviceId }) { index, device ->
                    val status = deviceStatuses[device.deviceId] ?: DeviceStatusSummary()
                    Box(Modifier.auraEnter(5 + index)) {
                        PulseDeviceCard(
                            device = device,
                            status = status,
                            onClick = { onDeviceClick(device) },
                            onToggleLock = { onToggleDeviceLock(device) }
                        )
                    }
                }

                // -- Quick actions ------------------------------------------------
                item {
                    Box(Modifier.auraEnter(10)) {
                        NeumorphicSectionHeader(title = stringResource(R.string.section_quick_actions))
                    }
                }
                item {
                    Column(
                        modifier = Modifier.auraEnter(11),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NeumorphicActionTile(
                                icon = Icons.Default.Shield,
                                label = stringResource(R.string.action_lock_all),
                                tint = Nm.danger,
                                onClick = onLockAll,
                                modifier = Modifier.weight(1f)
                            )
                            NeumorphicActionTile(
                                icon = Icons.Default.QrCodeScanner,
                                label = stringResource(R.string.action_add_device),
                                tint = Nm.primary,
                                onClick = onScanQR,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NeumorphicActionTile(
                                icon = Icons.Default.AutoGraph,
                                label = stringResource(R.string.insights_title),
                                tint = Nm.violet,
                                onClick = onOpenInsights,
                                modifier = Modifier.weight(1f)
                            )
                            NeumorphicActionTile(
                                icon = Icons.Default.Refresh,
                                label = stringResource(R.string.action_refresh_report),
                                tint = Nm.cyan,
                                onClick = onRefresh,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseDeviceCard(
    device: ChildDevice,
    status: DeviceStatusSummary,
    onClick: () -> Unit,
    onToggleLock: () -> Unit
) {
    NeumorphicCard(onClick = onClick, padding = 16.dp, corner = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeumorphicAvatar(name = device.customName.ifBlank { device.name })
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = device.customName.ifBlank { device.name },
                    color = Nm.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeumorphicStatusDot(
                        if (status.isOnline) Nm.success else Nm.onSurfaceMuted,
                        dotSize = 6.dp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (status.isOnline) {
                            when (status.connectionType) {
                                 ConnectionType.LOCAL -> stringResource(R.string.status_online) + " · " + stringResource(R.string.connection_local)
                                 ConnectionType.BLUETOOTH -> stringResource(R.string.status_online) + " · " + stringResource(R.string.status_bt)
                                 else -> stringResource(R.string.status_online) + " · " + stringResource(R.string.connection_relay)
                            }
                        } else stringResource(R.string.status_offline),
                        color = if (status.isOnline) Nm.success else Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (status.isOnline) {
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            Icons.Default.BatteryStd, null,
                            tint = Nm.onSurfaceMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = " ${status.batteryLevel}%",
                            color = Nm.onSurfaceMuted,
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp
                        )
                    }
                }
                if (status.isOnline) {
                    Spacer(Modifier.height(10.dp))
                    NeumorphicUsageBar(
                        label = stringResource(R.string.console_screen_time),
                        valueLabel = formatAuraDuration(status.todayScreenTimeMs),
                        fraction = (status.todayScreenTimeMs / DAILY_GOAL_MS.toFloat()),
                        color = Nm.primary
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            NeumorphicIconTile(
                icon = Icons.Default.Shield,
                onClick = onToggleLock,
                tint = if (status.isLocked) Nm.danger else Nm.onSurfaceMuted,
                 contentDescription = stringResource(
                     if (status.isLocked) R.string.unlock_device else R.string.lock_device
                 ),
                size = 42.dp
            )
        }
    }
}

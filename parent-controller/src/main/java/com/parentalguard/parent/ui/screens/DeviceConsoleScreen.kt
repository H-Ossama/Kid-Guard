package com.parentalguard.parent.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import com.parentalguard.common.model.AppCategory
import com.parentalguard.common.model.AppUsageLog
import com.parentalguard.common.model.BlockingRule
import com.parentalguard.common.model.BlockingScreenStyle
import com.parentalguard.common.model.CapabilityState
import com.parentalguard.common.model.DailyUsageReport
import com.parentalguard.common.model.DeviceOwnerCapability
import com.parentalguard.common.model.DeviceOwnerCapabilities
import com.parentalguard.parent.R
import com.parentalguard.parent.ui.aura.auraEnter
import com.parentalguard.parent.ui.aura.formatAuraCountdown
import com.parentalguard.parent.ui.aura.formatAuraDuration
import com.parentalguard.parent.ui.navigation.ConsoleSegment
import com.parentalguard.parent.ui.navigation.consoleSegments
import com.parentalguard.parent.ui.neumorphic.NeumorphicAvatar
import com.parentalguard.parent.ui.neumorphic.NeumorphicBackground
import com.parentalguard.parent.ui.neumorphic.NeumorphicBars
import com.parentalguard.parent.ui.neumorphic.NeumorphicButton
import com.parentalguard.parent.ui.neumorphic.NeumorphicCard
import com.parentalguard.parent.ui.neumorphic.NeumorphicChip
import com.parentalguard.parent.ui.neumorphic.NeumorphicConnectionPill
import com.parentalguard.parent.ui.neumorphic.NeumorphicDonut
import com.parentalguard.parent.ui.neumorphic.NeumorphicDuration
import com.parentalguard.parent.ui.neumorphic.NeumorphicIconTile
import com.parentalguard.parent.ui.neumorphic.NeumorphicProgressRing
import com.parentalguard.parent.ui.neumorphic.NeumorphicSectionHeader
import com.parentalguard.parent.ui.neumorphic.NeumorphicSegmentedControl
import com.parentalguard.parent.ui.neumorphic.NeumorphicShimmer
import com.parentalguard.parent.ui.neumorphic.NeumorphicStat
import com.parentalguard.parent.ui.neumorphic.NeumorphicStatusDot
import com.parentalguard.parent.ui.neumorphic.NeumorphicStatusPill
import com.parentalguard.parent.ui.neumorphic.NeumorphicSwitch
import com.parentalguard.parent.ui.neumorphic.NeumorphicToast
import com.parentalguard.parent.ui.neumorphic.NeumorphicUsageBar
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.ui.neumorphic.NmDatum
import com.parentalguard.parent.ui.neumorphic.neumorphic
import com.parentalguard.parent.ui.neumorphic.rememberNmPress
import com.parentalguard.parent.ui.theme.MonoFontFamily
import com.parentalguard.parent.ui.theme.getCategoryColor
import com.parentalguard.parent.viewmodel.ChildDevice
import com.parentalguard.parent.viewmodel.ConnectionType
import com.parentalguard.parent.viewmodel.DeviceControlViewModel
import com.parentalguard.parent.viewmodel.DiscoveryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val GOAL_MS = 8L * 60 * 60 * 1000

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceConsoleScreen(
    device: ChildDevice,
    viewModel: DeviceControlViewModel,
    discoveryViewModel: DiscoveryViewModel? = null,
    onBack: () -> Unit,
    onViewHistory: () -> Unit = {},
    onDeviceRemoved: () -> Unit = {},
    onOpenDeviceOwnerGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val usageLogs by viewModel.usageLogs.collectAsState()
    val activeRules by viewModel.activeRules.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val dailyReport by viewModel.dailyReport.collectAsState()
    val appTimers by viewModel.appTimers.collectAsState()
    val categoryTimers by viewModel.categoryTimers.collectAsState()
    val connectionType by viewModel.connectionType.collectAsState()
    val isLocked by viewModel.isDeviceLocked.collectAsState()
    val blockingScreenStyles by viewModel.blockingScreenStyles.collectAsState()
    val blockingScreenStyleSaves by viewModel.blockingScreenStyleSaves.collectAsState()
    val deviceOwnerCapabilities by viewModel.deviceOwnerCapabilities.collectAsState()
    val childPinSetByDevice by viewModel.childPinSet.collectAsState()
    val isChildPinSet = childPinSetByDevice[device.deviceId] ?: false
    val usageLogsByDevice by viewModel.usageLogsByDevice.collectAsState()
    val isRefreshingApps by viewModel.isRefreshingApps.collectAsState()
    val selectedCapabilities = deviceOwnerCapabilities[device.deviceId] ?: DeviceOwnerCapabilities()
    val selectedBlockingScreenStyle =
        blockingScreenStyles[device.deviceId] ?: BlockingScreenStyle.CURRENT
    val isBlockingScreenStyleSaving = device.deviceId in blockingScreenStyleSaves
    val selectedUsageLogs = usageLogsByDevice[device.deviceId] ?: emptyList()

    val pagerState = rememberPagerState(pageCount = { consoleSegments.size })
    val scope = rememberCoroutineScope()

    var showRenameDialog by remember { mutableStateOf(false) }
    var showResetPinDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var toastText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(device) {
        viewModel.fetchStats(device)
        viewModel.fetchDailyReport(device)
    }

    // Surface VM status as a transient toast
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            toastText = statusMessage
            delay(2200)
            toastText = null
        }
    }

    if (showRenameDialog) {
        ConsoleInputDialog(
            title = stringResource(R.string.dialog_rename_title),
            label = stringResource(R.string.dialog_rename_label),
            initial = device.customName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                if (newName.isNotBlank()) viewModel.renameDevice(device, newName)
                showRenameDialog = false
            }
        )
    }

    if (showResetPinDialog) {
        ConsoleConfirmDialog(
            title = stringResource(R.string.console_reset_pin_title),
            body = if (isChildPinSet) {
                stringResource(R.string.console_reset_pin_desc, device.customName.ifBlank { device.name })
            } else {
                stringResource(R.string.console_reset_pin_desc_none, device.customName.ifBlank { device.name })
            },
            confirmLabel = stringResource(R.string.action_set),
            onConfirm = { viewModel.resetPin(device); showResetPinDialog = false },
            onDismiss = { showResetPinDialog = false }
        )
    }

    if (showRemoveDialog) {
        ConsoleConfirmDialog(
            title = stringResource(R.string.circle_remove_title),
            body = stringResource(R.string.circle_remove_desc, device.customName.ifBlank { device.name }),
            confirmLabel = stringResource(R.string.circle_remove_action),
            danger = true,
            onConfirm = { showRemoveDialog = false; onDeviceRemoved() },
            onDismiss = { showRemoveDialog = false }
        )
    }

    NeumorphicBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // -- Console masthead ------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeumorphicIconTile(icon = Icons.Default.ArrowBack, onClick = onBack, contentDescription = stringResource(R.string.back))
                Spacer(Modifier.width(12.dp))
                NeumorphicAvatar(name = device.customName.ifBlank { device.name }, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.customName.ifBlank { device.name },
                            color = Nm.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(4.dp))
                         IconButton(
                             onClick = { showRenameDialog = true },
                             modifier = Modifier.size(40.dp)
                         ) {
                             Icon(
                                 Icons.Default.Edit,
                                 contentDescription = stringResource(R.string.rename_device),
                                 tint = Nm.primary,
                                 modifier = Modifier.size(16.dp)
                             )
                         }
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeumorphicStatusDot(
                            if (connectionType != ConnectionType.UNKNOWN) Nm.success else Nm.onSurfaceMuted,
                            dotSize = 5.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        if (connectionType != ConnectionType.UNKNOWN) {
                            NeumorphicConnectionPill(connectionType)
                        } else {
                            Text(
                                stringResource(R.string.status_offline),
                                color = Nm.onSurfaceMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                NeumorphicIconTile(
                    icon = Icons.Default.Refresh,
                    onClick = {
                        viewModel.fetchStats(device, forceRefresh = true)
                        viewModel.fetchDailyReport(device)
                    },
                    contentDescription = stringResource(R.string.refresh_stats)
                )
            }

            // -- Segments --------------------------------------------------
            NeumorphicSegmentedControl(
                items = consoleSegments.map { stringResource(it.titleResId) },
                selectedIndex = pagerState.currentPage,
                onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(14.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (consoleSegments[page]) {
                    ConsoleSegment.Now -> NowSegment(
                        device = device,
                        viewModel = viewModel,
                        discoveryViewModel = discoveryViewModel,
                        usageLogs = usageLogs,
                        activeRules = activeRules,
                        isLocked = isLocked,
                        blockingScreenStyle = selectedBlockingScreenStyle,
                        isBlockingScreenStyleSaving = isBlockingScreenStyleSaving,
                        onResetPin = { showResetPinDialog = true },
                        onRemove = { showRemoveDialog = true }
                    )
                    ConsoleSegment.Apps -> AppsSegment(
                        device = device,
                        viewModel = viewModel,
                        discoveryViewModel = discoveryViewModel,
                        usageLogs = usageLogs,
                        activeRules = activeRules,
                        appTimers = appTimers,
                        isRefreshingApps = isRefreshingApps
                    )
                    ConsoleSegment.Boundaries -> BoundariesSegment(
                        device = device,
                        viewModel = viewModel,
                        usageLogs = usageLogs,
                        categoryTimers = categoryTimers
                    )
                    ConsoleSegment.Rhythm -> RhythmSegment(device = device, viewModel = viewModel)
                    ConsoleSegment.Activity -> ActivitySegment(
                        device = device,
                        viewModel = viewModel,
                        dailyReport = dailyReport,
                        onViewHistory = onViewHistory
                    )
                    ConsoleSegment.DeviceOwner -> DeviceOwnerSegment(
                        device = device,
                        viewModel = viewModel,
                        capabilities = selectedCapabilities,
                        usageLogs = selectedUsageLogs,
                        onOpenGuide = onOpenDeviceOwnerGuide
                    )
                }
            }
        }

        NeumorphicToast(
            visible = toastText != null,
            text = toastText ?: "",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp)
        )
    }
}

// ============================================================================
// NOW — shield state, vital tiles, controls, top apps, danger zone
// ============================================================================

@Composable
private fun NowSegment(
    device: ChildDevice,
    viewModel: DeviceControlViewModel,
    discoveryViewModel: DiscoveryViewModel?,
    usageLogs: List<AppUsageLog>,
    activeRules: List<BlockingRule>,
    isLocked: Boolean,
    blockingScreenStyle: BlockingScreenStyle,
    isBlockingScreenStyleSaving: Boolean,
    onResetPin: () -> Unit,
    onRemove: () -> Unit
) {
    val isAppIconHidden by viewModel.isAppIconHidden.collectAsState()
    val totalScreenTime = usageLogs.sumOf { it.totalTimeInForeground }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Shield hero
        item {
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth().auraEnter(0),
                padding = 26.dp,
                corner = 30.dp,
                backgroundColor = (if (isLocked) Nm.danger else Nm.primary).copy(alpha = 0.05f)
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    BreathingShield(locked = isLocked)
                    Spacer(Modifier.height(14.dp))
                    NeumorphicStatusPill(
                        text = if (isLocked) stringResource(R.string.console_shield_engaged) else stringResource(R.string.console_at_ease),
                        color = if (isLocked) Nm.danger else Nm.success
                    )
                    Spacer(Modifier.height(16.dp))
                    NeumorphicButton(
                        text = if (isLocked) stringResource(R.string.console_release) else stringResource(R.string.console_engage),
                        onClick = { viewModel.lockDevice(device, !isLocked, discoveryViewModel) },
                        icon = if (isLocked) Icons.Default.LockOpen else Icons.Default.Shield,
                        tint = Nm.primary,
                        iconTint = Nm.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            BlockingScreenStyleCard(
                selectedStyle = blockingScreenStyle,
                enabled = !isBlockingScreenStyleSaving,
                onStyleSelected = { viewModel.setBlockingScreenStyle(device, it) }
            )
        }

        // Vitals
        item {
            Row(
                modifier = Modifier.fillMaxWidth().auraEnter(1),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeumorphicCard(modifier = Modifier.weight(1f), padding = 14.dp, corner = 20.dp) {
                    Text(
                        stringResource(R.string.console_screen_time).uppercase(),
                        color = Nm.onSurfaceMuted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    NeumorphicDuration(targetMs = totalScreenTime, fontSize = 20)
                }
                NeumorphicCard(modifier = Modifier.weight(1f), padding = 14.dp, corner = 20.dp) {
                    NeumorphicStat(
                        label = stringResource(R.string.console_rules),
                        value = "${activeRules.size}"
                    )
                }
                NeumorphicCard(modifier = Modifier.weight(1f), padding = 14.dp, corner = 20.dp) {
                    Text(
                        stringResource(R.string.console_app_icon).uppercase(),
                        color = Nm.onSurfaceMuted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isAppIconHidden) stringResource(R.string.console_hidden) else stringResource(R.string.console_visible),
                        color = if (isAppIconHidden) Nm.violet else Nm.onSurface,
                        fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }
            }
        }

        // Controls
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth().auraEnter(2), padding = 16.dp, corner = 24.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeumorphicButton(
                        text = if (isAppIconHidden) stringResource(R.string.action_unhide_icon) else stringResource(R.string.action_hide_icon),
                        onClick = { viewModel.setAppIconVisibility(device, isAppIconHidden) },
                        icon = if (isAppIconHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        inset = true,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    NeumorphicButton(
                        text = stringResource(R.string.action_refresh_apps),
                        onClick = { viewModel.refreshIcons(device) },
                        icon = Icons.Default.Refresh,
                        inset = true,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Top apps
        item {
            Box(Modifier.auraEnter(3)) {
                NeumorphicSectionHeader(title = stringResource(R.string.console_top_apps))
            }
        }

        val topApps = usageLogs.sortedByDescending { it.totalTimeInForeground }.take(5)
        if (topApps.isEmpty()) {
            item {
                NeumorphicCard(modifier = Modifier.fillMaxWidth().auraEnter(4), padding = 24.dp) {
                    Text(
                        stringResource(R.string.label_no_usage_data),
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            item {
                NeumorphicCard(modifier = Modifier.fillMaxWidth().auraEnter(4), padding = 18.dp, corner = 24.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        topApps.forEach { app ->
                            ConsoleAppRow(
                                app = app,
                                rule = activeRules.find { it.packageName == app.packageName },
                                iconBase64 = viewModel.getAppIcon(app.packageName),
                                totalScreenTime = totalScreenTime,
                                onToggleBlock = { viewModel.toggleAppBlock(device, app.packageName, discoveryViewModel) },
                                onToggleWeb = { viewModel.toggleInternetBlock(device, app.packageName, discoveryViewModel) }
                            )
                        }
                    }
                }
            }
        }

        // Danger zone
        item {
            Column(Modifier.auraEnter(5)) {
                NmDivider(Modifier.padding(vertical = 6.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier.width(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeumorphicButton(
                            text = stringResource(R.string.console_reset_pin),
                            onClick = onResetPin,
                            icon = Icons.Default.Key,
                            tint = Nm.onSurfaceMuted,
                            iconTint = Nm.onSurfaceMuted,
                            inset = true
                        )
                        NeumorphicButton(
                            text = stringResource(R.string.console_remove_device),
                            onClick = onRemove,
                            icon = Icons.Default.DeleteOutline,
                            tint = Nm.danger,
                            iconTint = Nm.danger,
                            inset = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BreathingShield(locked: Boolean) {
    val breathe by rememberInfiniteTransition(label = "shield").animateFloat(
        initialValue = 0.96f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = Nm.EaseOutSoft),
            RepeatMode.Reverse
        ),
        label = "shield-breathe"
    )
    val accent = if (locked) Nm.danger else Nm.primary
    Box(
        modifier = Modifier
            .size(86.dp)
            .graphicsLayer { scaleX = breathe; scaleY = breathe }
            .neumorphic(shape = CircleShape, backgroundColor = accent.copy(alpha = 0.06f), elevation = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (locked) Icons.Default.Lock else Icons.Default.Shield,
            null,
            tint = accent,
            modifier = Modifier.size(34.dp)
        )
    }
}

// ============================================================================
@Composable
private fun BlockingScreenStyleCard(
    selectedStyle: BlockingScreenStyle,
    enabled: Boolean,
    onStyleSelected: (BlockingScreenStyle) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.blocking_style_title),
            color = Nm.onSurface,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(R.string.blocking_style_desc),
            color = Nm.onSurfaceMuted,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BlockingScreenStyle.values().forEach { style ->
                BlockingScreenStylePreview(
                    style = style,
                    selected = style == selectedStyle,
                    enabled = enabled,
                    onClick = { if (style != selectedStyle) onStyleSelected(style) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BlockingScreenStylePreview(
    style: BlockingScreenStyle,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeumorphicCard(
        modifier = modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick
        ),
        padding = 8.dp,
        corner = 16.dp,
        backgroundColor = if (selected) Nm.primary.copy(alpha = 0.10f) else Nm.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    when (style) {
                        BlockingScreenStyle.CURRENT -> Nm.inset
                        BlockingScreenStyle.BLACKOUT -> Color.Black
                        BlockingScreenStyle.QUIET_FOCUS -> Color(0xFF111936)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (style) {
                BlockingScreenStyle.CURRENT -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Shield, null, tint = Nm.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(
                            stringResource(R.string.blocking_style_current_preview),
                            color = Nm.onSurface,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                BlockingScreenStyle.BLACKOUT -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, null, tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(
                            stringResource(R.string.blocking_style_blackout_preview),
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                BlockingScreenStyle.QUIET_FOCUS -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Timer, null, tint = Color(0xFF62E6D6), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(3.dp))
                        Box(
                            Modifier
                                .width(44.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF62E6D6))
                        )
                    }
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Nm.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = when (style) {
                BlockingScreenStyle.CURRENT -> stringResource(R.string.blocking_style_current)
                BlockingScreenStyle.BLACKOUT -> stringResource(R.string.blocking_style_blackout)
                BlockingScreenStyle.QUIET_FOCUS -> stringResource(R.string.blocking_style_quiet_focus)
            },
            color = if (selected) Nm.primary else Nm.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp)
        )
    }
}

@Composable
private fun DeviceOwnerSegment(
    device: ChildDevice,
    viewModel: DeviceControlViewModel,
    capabilities: DeviceOwnerCapabilities,
    usageLogs: List<AppUsageLog>,
    onOpenGuide: () -> Unit
) {
    var blockedCapability by remember { mutableStateOf<DeviceOwnerCapability?>(null) }
    var showDeviceLimitDialog by remember { mutableStateOf(false) }
    var showResetScreenLockDialog by remember { mutableStateOf(false) }
    var appLimitFor by remember { mutableStateOf<String?>(null) }

    if (showDeviceLimitDialog) {
        DurationPickerDialog(
            title = stringResource(R.string.device_owner_device_limit_title),
            description = stringResource(R.string.device_owner_device_limit_desc),
            options = listOf(30, 60, 90, 120, 180, 240),
            allowRemove = true,
            onDismiss = { showDeviceLimitDialog = false },
            onConfirm = { minutes ->
                viewModel.setDeviceUsageLimit(device, minutes * 60 * 1000L)
                showDeviceLimitDialog = false
            }
        )
    }

    appLimitFor?.let { packageName ->
        DurationPickerDialog(
            title = stringResource(R.string.device_owner_app_limit_title),
            description = stringResource(R.string.device_owner_app_limit_desc),
            options = listOf(15, 30, 60, 90, 120, 180),
            allowRemove = true,
            onDismiss = { appLimitFor = null },
            onConfirm = { minutes ->
                viewModel.setAppUsageLimit(device, packageName, minutes * 60 * 1000L)
                appLimitFor = null
            }
        )
    }

    blockedCapability?.let { capability ->
        val state = capabilities.stateFor(capability)
        AlertDialog(
            onDismissRequest = { blockedCapability = null },
            containerColor = Nm.surface,
            title = {
                Text(stringResource(R.string.device_owner_unavailable_title), color = Nm.onSurface)
            },
            text = {
                Text(
                    text = if (state == CapabilityState.UNKNOWN) {
                        stringResource(R.string.device_owner_unknown_desc)
                    } else {
                        stringResource(R.string.device_owner_unavailable_desc)
                    },
                    color = Nm.onSurfaceMuted
                )
            },
            confirmButton = {
                TextButton(onClick = { blockedCapability = null }) {
                    Text(stringResource(R.string.got_it), color = Nm.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { blockedCapability = null; onOpenGuide() }) {
                    Text(stringResource(R.string.device_owner_setup_action), color = Nm.primary)
                }
            }
        )
    }

    if (showResetScreenLockDialog) {
        AlertDialog(
            onDismissRequest = { showResetScreenLockDialog = false },
            containerColor = Nm.surface,
            title = {
                Text(stringResource(R.string.device_owner_screen_lock_confirm_title), color = Nm.onSurface)
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.device_owner_screen_lock_confirm_body,
                        device.customName.ifBlank { device.name }
                    ),
                    color = Nm.onSurfaceMuted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetScreenLockDialog = false
                    viewModel.clearChildScreenLock(device)
                }) {
                    Text(stringResource(R.string.device_owner_screen_lock_action), color = Nm.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetScreenLockDialog = false }) {
                    Text(stringResource(R.string.cancel), color = Nm.primary)
                }
            }
        )
    }

    val restrictionEntries = listOf(
        "DISALLOW_INSTALL_APPS" to stringResource(R.string.device_owner_restrict_install),
        "DISALLOW_MODIFY_ACCOUNTS" to stringResource(R.string.device_owner_restrict_accounts),
        "DISALLOW_ADD_USER" to stringResource(R.string.device_owner_restrict_users)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DeviceOwnerStatusHeader(capabilities = capabilities, onOpenGuide = onOpenGuide)
        }
        item {
            OwnerActionCard(
                title = stringResource(R.string.device_owner_lock_title),
                description = stringResource(R.string.device_owner_lock_desc),
                actionLabel = stringResource(R.string.device_owner_lock_action),
                icon = Icons.Default.Lock,
                state = capabilities.stateFor(DeviceOwnerCapability.DEVICE_LOCK),
                onAction = { viewModel.setDeviceOwnerLocked(device, true) },
                onBlocked = { blockedCapability = DeviceOwnerCapability.DEVICE_LOCK }
            )
        }
        item {
            OwnerActionCard(
                title = stringResource(R.string.device_owner_device_limit_title),
                description = stringResource(R.string.device_owner_device_limit_desc),
                actionLabel = stringResource(R.string.device_owner_set_limit_action),
                icon = Icons.Default.Timer,
                state = capabilities.stateFor(DeviceOwnerCapability.DEVICE_USAGE_LIMITS),
                onAction = { showDeviceLimitDialog = true },
                onBlocked = { blockedCapability = DeviceOwnerCapability.DEVICE_USAGE_LIMITS }
            )
        }
        item {
            OwnerWifiCard(
                state = capabilities.stateFor(DeviceOwnerCapability.WIFI_TOGGLE),
                onAction = { enabled -> viewModel.setWifiEnabled(device, enabled) },
                onBlocked = { blockedCapability = DeviceOwnerCapability.WIFI_TOGGLE }
            )
        }
        item {
            OwnerActionCard(
                title = stringResource(R.string.device_owner_screen_lock_title),
                description = stringResource(R.string.device_owner_screen_lock_desc),
                actionLabel = stringResource(R.string.device_owner_screen_lock_action),
                icon = Icons.Default.Key,
                state = capabilities.stateFor(DeviceOwnerCapability.DEVICE_LOCK),
                onAction = { showResetScreenLockDialog = true },
                onBlocked = { blockedCapability = DeviceOwnerCapability.DEVICE_LOCK }
            )
        }
        item {
            Text(
                text = stringResource(R.string.device_owner_apps_title),
                color = Nm.onSurface,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        items(usageLogs.take(5), key = { it.packageName }) { app ->
            OwnerAppCard(
                app = app,
                capabilities = capabilities,
                onBlocked = { blockedCapability = it },
                onSuspend = { viewModel.setAppSuspended(device, app.packageName, true) },
                onProtectUninstall = { viewModel.setUninstallProtection(device, app.packageName, true) },
                onSetLimit = { appLimitFor = app.packageName }
            )
        }
        item {
            Text(
                text = stringResource(R.string.device_owner_restrictions_title),
                color = Nm.onSurface,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        items(restrictionEntries, key = { it.first }) { (restrictionKey, label) ->
            OwnerActionCard(
                title = label,
                description = stringResource(R.string.device_owner_restrictions_desc),
                actionLabel = stringResource(R.string.device_owner_restrict_action),
                icon = Icons.Default.Shield,
                state = capabilities.stateFor(DeviceOwnerCapability.USER_RESTRICTIONS),
                onAction = { viewModel.setUserRestriction(device, restrictionKey, true) },
                onBlocked = { blockedCapability = DeviceOwnerCapability.USER_RESTRICTIONS }
            )
        }
    }
}

@Composable
private fun DeviceOwnerStatusHeader(
    capabilities: DeviceOwnerCapabilities,
    onOpenGuide: () -> Unit
) {
    val states = DeviceOwnerCapability.values().map(capabilities::stateFor)
    val state = when {
        states.all { it == CapabilityState.AVAILABLE } -> CapabilityState.AVAILABLE
        states.any { it == CapabilityState.UNAVAILABLE } -> CapabilityState.UNAVAILABLE
        else -> CapabilityState.UNKNOWN
    }
    val color = ownerStateColor(state)
    val granted = state == CapabilityState.AVAILABLE
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 16.dp,
        corner = 22.dp,
        backgroundColor = color.copy(alpha = 0.06f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (granted) {
                NeumorphicIconTile(
                    icon = Icons.Default.VerifiedUser,
                    tint = Nm.success,
                    size = 46.dp,
                    iconSize = 22.dp
                )
            } else {
                NeumorphicStatusDot(color, dotSize = 8.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (granted) {
                        stringResource(R.string.device_owner_access_granted)
                    } else {
                        stringResource(R.string.device_owner_controls_title)
                    },
                    color = Nm.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (granted) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = if (granted) {
                        stringResource(R.string.device_owner_access_granted_desc)
                    } else when (state) {
                        CapabilityState.AVAILABLE -> stringResource(R.string.device_owner_status_available)
                        CapabilityState.UNAVAILABLE -> stringResource(R.string.device_owner_status_unavailable)
                        CapabilityState.UNKNOWN -> stringResource(R.string.device_owner_status_unknown)
                    },
                    color = color,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onOpenGuide) {
                Text(stringResource(R.string.device_owner_setup_action), color = Nm.primary)
            }
        }
    }
}

@Composable
private fun OwnerActionCard(
    title: String,
    description: String,
    actionLabel: String,
    icon: ImageVector,
    state: CapabilityState,
    onAction: () -> Unit,
    onBlocked: () -> Unit
) {
    val color = ownerStateColor(state)
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (state == CapabilityState.AVAILABLE) onAction else onBlocked,
        padding = 16.dp,
        corner = 20.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeumorphicIconTile(icon = icon, tint = color, size = 40.dp, iconSize = 19.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Nm.onSurface, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(3.dp))
                Text(description, color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            Text(actionLabel, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OwnerWifiCard(
    state: CapabilityState,
    onAction: (Boolean) -> Unit,
    onBlocked: () -> Unit
) {
    val color = ownerStateColor(state)
    val action: (Boolean) -> Unit = if (state == CapabilityState.AVAILABLE) onAction else { _ -> onBlocked() }
    NeumorphicCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp, corner = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeumorphicIconTile(icon = Icons.Default.Wifi, tint = color, size = 40.dp, iconSize = 19.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.device_owner_wifi_title), color = Nm.onSurface, style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.device_owner_wifi_desc), color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniAction(
                label = stringResource(R.string.device_owner_wifi_on),
                icon = Icons.Default.Wifi,
                tint = color,
                onClick = { action(true) },
                modifier = Modifier.weight(1f)
            )
            MiniAction(
                label = stringResource(R.string.device_owner_wifi_off),
                icon = Icons.Default.WifiOff,
                tint = color,
                onClick = { action(false) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OwnerAppCard(
    app: AppUsageLog,
    capabilities: DeviceOwnerCapabilities,
    onBlocked: (DeviceOwnerCapability) -> Unit,
    onSuspend: () -> Unit,
    onProtectUninstall: () -> Unit,
    onSetLimit: () -> Unit
) {
    val suspensionState = capabilities.stateFor(DeviceOwnerCapability.APP_SUSPENSION)
    val uninstallState = capabilities.stateFor(DeviceOwnerCapability.UNINSTALL_PROTECTION)
    val limitState = capabilities.stateFor(DeviceOwnerCapability.APP_USAGE_LIMITS)
    val color = ownerStateColor(suspensionState)
    NeumorphicCard(modifier = Modifier.fillMaxWidth(), padding = 16.dp, corner = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeumorphicIconTile(icon = Icons.Default.Apps, tint = color, size = 38.dp, iconSize = 18.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = app.appLabel?.takeIf { it.isNotBlank() }
                    ?: app.packageName.substringAfterLast('.'),
                color = Nm.onSurface,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniAction(
                label = stringResource(R.string.device_owner_suspend_action),
                icon = Icons.Default.Lock,
                tint = ownerStateColor(suspensionState),
                onClick = { if (suspensionState == CapabilityState.AVAILABLE) onSuspend() else onBlocked(DeviceOwnerCapability.APP_SUSPENSION) },
                modifier = Modifier.weight(1f)
            )
            MiniAction(
                label = stringResource(R.string.device_owner_uninstall_action),
                icon = Icons.Default.Shield,
                tint = ownerStateColor(uninstallState),
                onClick = { if (uninstallState == CapabilityState.AVAILABLE) onProtectUninstall() else onBlocked(DeviceOwnerCapability.UNINSTALL_PROTECTION) },
                modifier = Modifier.weight(1f)
            )
            MiniAction(
                label = stringResource(R.string.device_owner_limit_action),
                icon = Icons.Default.Timer,
                tint = ownerStateColor(limitState),
                onClick = { if (limitState == CapabilityState.AVAILABLE) onSetLimit() else onBlocked(DeviceOwnerCapability.APP_USAGE_LIMITS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun ownerStateColor(state: CapabilityState): Color = when (state) {
    CapabilityState.AVAILABLE -> Nm.success
    CapabilityState.UNAVAILABLE -> Nm.danger
    CapabilityState.UNKNOWN -> Nm.warning
}

// APPS — search, filter, per-app switches, allowance, daily limits
// ============================================================================

@Composable
private fun AppsSegment(
    device: ChildDevice,
    viewModel: DeviceControlViewModel,
    discoveryViewModel: DiscoveryViewModel?,
    usageLogs: List<AppUsageLog>,
    activeRules: List<BlockingRule>,
    appTimers: Map<String, Long>,
    isRefreshingApps: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<AppCategory?>(null) }

    var editCategoryFor by remember { mutableStateOf<Pair<String, AppCategory>?>(null) }
    var timerFor by remember { mutableStateOf<String?>(null) }
    var dailyLimitFor by remember { mutableStateOf<String?>(null) }
    var blockDurationFor by remember { mutableStateOf<String?>(null) }

    editCategoryFor?.let { (pkg, current) ->
        CategoryPickerDialog(
            current = current,
            onDismiss = { editCategoryFor = null },
            onConfirm = { viewModel.setAppCategory(device, pkg, it); editCategoryFor = null }
        )
    }

    timerFor?.let { pkg ->
        DurationPickerDialog(
            title = stringResource(R.string.dialog_timer_title),
            description = stringResource(R.string.dialog_timer_desc),
            options = listOf(15, 30, 45, 60, 90, 120),
            onDismiss = { timerFor = null },
            onConfirm = { viewModel.setAppTimer(device, pkg, it); timerFor = null }
        )
    }

    dailyLimitFor?.let { pkg ->
        DurationPickerDialog(
            title = stringResource(R.string.apps_daily_limit),
            description = stringResource(R.string.dialog_timer_desc),
            options = listOf(30, 60, 120, 180, 240, 300),
            allowRemove = true,
            onDismiss = { dailyLimitFor = null },
            onConfirm = { viewModel.setAppDailyLimit(device, pkg, it); dailyLimitFor = null }
        )
    }

    blockDurationFor?.let { pkg ->
        DurationPickerDialog(
            title = stringResource(R.string.apps_block_for),
            description = stringResource(R.string.apps_block_for_desc),
            options = listOf(15, 30, 60, 120, 240),
            onDismiss = { blockDurationFor = null },
            onConfirm = {
                viewModel.setAppBlockDuration(device, pkg, it)
                blockDurationFor = null
            }
        )
    }

    val filteredApps = usageLogs
        .filter { log ->
            (searchQuery.isEmpty() ||
                log.packageName.contains(searchQuery, ignoreCase = true) ||
                log.appLabel?.contains(searchQuery, ignoreCase = true) == true) &&
                (selectedCategory != null || log.category != AppCategory.SYSTEM) &&
                (selectedCategory == null || log.category == selectedCategory)
        }
        .sortedByDescending { it.totalTimeInForeground }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.placeholder_search_apps), color = Nm.onSurfaceMuted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Nm.primary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
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
            )
        )

        Spacer(Modifier.height(12.dp))

        // Category chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                NeumorphicChip(
                    label = stringResource(R.string.label_all),
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null }
                )
            }
            items(AppCategory.values().toList()) { category ->
                NeumorphicChip(
                    label = stringResource(getCategoryNameResId(category)),
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = if (selectedCategory == category) null else category },
                    accent = getCategoryColor(category)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.apps_controls_hint),
            color = Nm.onSurfaceMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        if (filteredApps.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.SearchOff, null, tint = Nm.onSurfaceMuted, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.label_no_apps_found), color = Nm.onSurfaceMuted)
                Spacer(Modifier.height(18.dp))
                NeumorphicButton(
                    text = stringResource(
                        if (isRefreshingApps) R.string.apps_refreshing else R.string.action_refresh_apps
                    ),
                    onClick = { if (!isRefreshingApps) viewModel.refreshIcons(device) },
                    icon = Icons.Default.Refresh,
                    enabled = !isRefreshingApps,
                    inset = true
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val expiration = appTimers[app.packageName] ?: 0L
                    val rule = activeRules.find { it.packageName == app.packageName }
                    AppControlCard(
                        app = app,
                        rule = rule,
                        timerExpiration = expiration,
                        iconBase64 = viewModel.getAppIcon(app.packageName),
                        onToggleBlock = { viewModel.toggleAppBlock(device, app.packageName, discoveryViewModel) },
                        onToggleWeb = { viewModel.toggleInternetBlock(device, app.packageName, discoveryViewModel) },
                        onEditCategory = { editCategoryFor = app.packageName to app.category },
                        onSetTimer = { timerFor = app.packageName },
                        onCancelTimer = { viewModel.cancelAppTimer(device, app.packageName) },
                        onSetDailyLimit = { dailyLimitFor = app.packageName },
                        onBlockNow = { viewModel.blockAppNow(device, app.packageName) },
                        onSetBlockDuration = { blockDurationFor = app.packageName }
                    )
                }
                item {
                    if (isRefreshingApps) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = Nm.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.apps_refreshing),
                                color = Nm.onSurfaceMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    NeumorphicButton(
                        text = stringResource(
                            if (isRefreshingApps) R.string.apps_refreshing else R.string.action_refresh_apps
                        ),
                        onClick = { if (!isRefreshingApps) viewModel.refreshIcons(device) },
                        icon = Icons.Default.Refresh,
                        enabled = !isRefreshingApps,
                        inset = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppControlCard(
    app: AppUsageLog,
    rule: BlockingRule?,
    timerExpiration: Long,
    iconBase64: String?,
    onToggleBlock: () -> Unit,
    onToggleWeb: () -> Unit,
    onEditCategory: () -> Unit,
    onSetTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onSetDailyLimit: () -> Unit,
    onBlockNow: () -> Unit,
    onSetBlockDuration: () -> Unit
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    if (timerExpiration > currentTime) {
        LaunchedEffect(timerExpiration) {
            while (currentTime < timerExpiration) {
                delay(1000)
                currentTime = System.currentTimeMillis()
            }
        }
    }
    val remainingMs = (timerExpiration - currentTime).coerceAtLeast(0)
    val hasActiveTimer = remainingMs > 0
    val isTimerExpired = timerExpiration > 0 && timerExpiration < currentTime
    val isBlocked = (rule != null && (rule.blockEndTime > System.currentTimeMillis() || rule.isPermanentlyBlocked)) || isTimerExpired
    val isWebBlocked = rule?.isInternetBlocked == true

    NeumorphicCard(padding = 16.dp, corner = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIconBadge(packageName = app.packageName, category = app.category, iconBase64 = iconBase64)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.appLabel?.takeIf { it.isNotBlank() }
                        ?: app.packageName.substringAfterLast("."),
                    color = Nm.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(getCategoryNameResId(app.category)),
                        color = getCategoryColor(app.category),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onEditCategory)
                            .padding(2.dp)
                    )
                    if ((rule?.maxDailyTimeMs ?: 0L) > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.apps_limit_set, (rule!!.maxDailyTimeMs / 60000).toInt()),
                            color = Nm.success,
                            fontFamily = MonoFontFamily,
                            fontSize = 10.sp
                        )
                    }
                    if (hasActiveTimer) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatAuraCountdown(remainingMs),
                            color = Nm.cyan,
                            fontFamily = MonoFontFamily,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // App and internet controls are intentionally separate: the first
            // blocks the app itself, the second blocks its network access.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.apps_app), color = Nm.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                NeumorphicSwitch(
                    checked = !isBlocked,
                    onCheckedChange = { onToggleBlock() }
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.apps_internet), color = Nm.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                NeumorphicSwitch(
                    checked = !isWebBlocked,
                    onCheckedChange = { onToggleWeb() }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isBlocked) stringResource(R.string.apps_blocked_status)
            else stringResource(R.string.apps_allowed_status),
            color = if (isBlocked) Nm.danger else Nm.success,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniAction(
                label = if (isBlocked) stringResource(R.string.apps_unblock) else stringResource(R.string.apps_block_now),
                icon = if (isBlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                tint = if (isBlocked) Nm.success else Nm.danger,
                onClick = if (isBlocked) onToggleBlock else onBlockNow,
                modifier = Modifier.weight(1f)
            )
            MiniAction(
                label = stringResource(R.string.apps_block_for),
                icon = Icons.Default.Timer,
                tint = Nm.danger,
                onClick = onSetBlockDuration,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniAction(
                label = if (hasActiveTimer) stringResource(R.string.apps_cancel_allowance) else stringResource(R.string.apps_allowance),
                icon = if (hasActiveTimer) Icons.Default.TimerOff else Icons.Default.AccessTime,
                tint = if (hasActiveTimer) Nm.danger else Nm.primary,
                onClick = if (hasActiveTimer) onCancelTimer else onSetTimer,
                modifier = Modifier.weight(1f)
            )
            MiniAction(
                label = stringResource(R.string.apps_daily_limit),
                icon = Icons.Default.History,
                tint = Nm.success,
                onClick = onSetDailyLimit,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MiniAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (interactionSource, scale) = rememberNmPress(0.95f)
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .neumorphic(shape = shape, backgroundColor = Nm.surface, elevation = 4.dp)
            .clip(shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

// ============================================================================
// BOUNDARIES — per-category daily limits + timers
// ============================================================================

@Composable
private fun BoundariesSegment(
    device: ChildDevice,
    viewModel: DeviceControlViewModel,
    usageLogs: List<AppUsageLog>,
    categoryTimers: Map<AppCategory, Long>
) {
    val categoryLimits by viewModel.categoryLimits.collectAsState()
    var timerDialogFor by remember { mutableStateOf<AppCategory?>(null) }

    timerDialogFor?.let { category ->
        DurationPickerDialog(
            title = stringResource(R.string.dialog_category_timer_title),
            description = stringResource(R.string.dialog_category_timer_desc),
            options = listOf(15, 30, 60, 120),
            onDismiss = { timerDialogFor = null },
            onConfirm = { viewModel.setCategoryTimer(device, category, it); timerDialogFor = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    stringResource(R.string.title_category_limits),
                    color = Nm.onSurface, style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.desc_category_limits),
                    color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodySmall
                )
            }
        }

        items(AppCategory.values().toList(), key = { it.name }) { category ->
            BoundaryCard(
                category = category,
                currentLimit = categoryLimits.find { it.category == category }?.maxDailyTimeMs ?: 0L,
                apps = usageLogs.filter { it.category == category },
                timerExpiration = categoryTimers[category] ?: 0L,
                onLimitChanged = { viewModel.setCategoryLimit(device, category, it) },
                onSetTimer = { timerDialogFor = category },
                onCancelTimer = { viewModel.cancelCategoryTimer(device, category) }
            )
        }
    }
}

@Composable
private fun BoundaryCard(
    category: AppCategory,
    currentLimit: Long,
    apps: List<AppUsageLog>,
    timerExpiration: Long,
    onLimitChanged: (Int) -> Unit,
    onSetTimer: () -> Unit,
    onCancelTimer: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf((currentLimit / 60000f).coerceIn(0f, 480f)) }
    var expanded by remember { mutableStateOf(false) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    if (timerExpiration > currentTime) {
        LaunchedEffect(timerExpiration) {
            while (currentTime < timerExpiration) {
                delay(1000)
                currentTime = System.currentTimeMillis()
            }
        }
    }
    val remainingMs = (timerExpiration - currentTime).coerceAtLeast(0)
    val hasActiveTimer = remainingMs > 0

    LaunchedEffect(currentLimit) {
        sliderValue = (currentLimit / 60000f).coerceIn(0f, 480f)
    }

    val color = getCategoryColor(category)

    NeumorphicCard(onClick = { expanded = !expanded }, padding = 16.dp, corner = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(getCategoryIcon(category), null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(getCategoryNameResId(category)),
                    color = Nm.onSurface, style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (sliderValue > 0) stringResource(R.string.label_limit_mins, sliderValue.toInt())
                    else stringResource(R.string.label_no_limit),
                    color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodySmall
                )
            }
            if (hasActiveTimer) {
                NeumorphicStatusPill(text = formatAuraCountdown(remainingMs), color = color)
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null, tint = Nm.onSurfaceMuted
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(Modifier.padding(top = 16.dp)) {
                if (apps.isNotEmpty()) {
                    Text(
                        stringResource(R.string.label_apps_in_category),
                        color = Nm.onSurfaceMuted, style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    apps.take(5).forEach { app ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Box(Modifier.size(4.dp).clip(CircleShape).background(Nm.onSurfaceMuted))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                app.appLabel?.takeIf { it.isNotBlank() }
                                    ?: app.packageName.substringAfterLast("."),
                                color = Nm.onSurface, style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (apps.size > 5) {
                        Text(
                            stringResource(R.string.label_more_apps, apps.size - 5),
                            color = Nm.onSurfaceMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                // Presets
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 120, 0).forEach { minutes ->
                        NeumorphicChip(
                            label = when {
                                minutes == 0 -> stringResource(R.string.label_none)
                                minutes < 60 -> "${minutes}m"
                                else -> "${minutes / 60}h"
                            },
                            selected = sliderValue.toInt() == minutes,
                            onClick = { sliderValue = minutes.toFloat(); onLimitChanged(minutes) },
                            accent = color,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    stringResource(R.string.label_daily_limit, sliderValue.toInt()),
                    color = Nm.onSurface, style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onLimitChanged(sliderValue.toInt()) },
                    valueRange = 0f..480f,
                    steps = 31,
                    colors = SliderDefaults.colors(
                        thumbColor = color,
                        activeTrackColor = color,
                        inactiveTrackColor = Nm.darkShadow.copy(alpha = 0.2f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    MiniAction(
                        label = if (hasActiveTimer) stringResource(R.string.action_cancel_timer) else stringResource(R.string.action_set_timer),
                        icon = if (hasActiveTimer) Icons.Default.TimerOff else Icons.Default.AccessTime,
                        tint = if (hasActiveTimer) Nm.danger else color,
                        onClick = if (hasActiveTimer) onCancelTimer else onSetTimer
                    )
                }
            }
        }
    }
}

// ============================================================================
// RHYTHM — automatic breaks
// ============================================================================

@Composable
private fun RhythmSegment(
    device: ChildDevice,
    viewModel: DeviceControlViewModel
) {
    val usageLimitMs by viewModel.usageLimitMs.collectAsState()
    val breakDurationMs by viewModel.breakDurationMs.collectAsState()
    val breakWarningMs by viewModel.breakWarningMs.collectAsState()
    val educationOnly by viewModel.educationOnly.collectAsState()
    val allowExtensions by viewModel.allowExtensions.collectAsState()
    val isLocked by viewModel.isDeviceLocked.collectAsState()
    val lockReason by viewModel.lockReason.collectAsState()

    val isBreakActive = isLocked && lockReason == "BREAK"

    var usageLimitMin by remember(usageLimitMs) { mutableIntStateOf((usageLimitMs / 60000).toInt().coerceIn(1, 120)) }
    var breakDurationMin by remember(breakDurationMs) { mutableIntStateOf((breakDurationMs / 60000).toInt().coerceIn(1, 60)) }
    var warningEnabled by remember(breakWarningMs) { mutableStateOf(breakWarningMs > 0) }
    var warningMin by remember(breakWarningMs) { mutableIntStateOf(if (breakWarningMs > 0) (breakWarningMs / 60000).toInt() else 2) }
    var eduOnly by remember(educationOnly) { mutableStateOf(educationOnly) }
    var extensionsAllowed by remember(allowExtensions) { mutableStateOf(allowExtensions) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth().auraEnter(0),
                padding = 24.dp,
                corner = 30.dp,
                backgroundColor = when {
                    isBreakActive -> Nm.danger.copy(alpha = 0.05f)
                    usageLimitMs > 0 -> Nm.success.copy(alpha = 0.05f)
                    else -> Nm.surface
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.rhythm_title),
                            color = Nm.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when {
                                isBreakActive -> stringResource(R.string.rhythm_on_break)
                                usageLimitMs > 0 -> stringResource(R.string.rhythm_monitoring)
                                else -> stringResource(R.string.rhythm_disabled)
                            },
                            color = when {
                                isBreakActive -> Nm.danger
                                usageLimitMs > 0 -> Nm.success
                                else -> Nm.onSurfaceMuted
                            },
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (isBreakActive) stringResource(R.string.rhythm_locked_desc) else stringResource(R.string.rhythm_info),
                            color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(
                        if (isBreakActive) Icons.Default.Snooze else Icons.Default.AccessTime,
                        null,
                        tint = if (isBreakActive) Nm.danger else if (usageLimitMs > 0) Nm.success else Nm.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                if (isBreakActive) {
                    Spacer(Modifier.height(16.dp))
                    NeumorphicButton(
                        text = stringResource(R.string.rhythm_end_break),
                        onClick = { viewModel.stopBreak(device) },
                        icon = Icons.Default.Cancel,
                        tint = Nm.primary,
                        iconTint = Nm.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth().auraEnter(1), padding = 20.dp, corner = 24.dp) {
                Text(
                    stringResource(R.string.rhythm_break_after, usageLimitMin),
                    color = Nm.onSurface, style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = usageLimitMin.toFloat(),
                    onValueChange = { usageLimitMin = it.toInt() },
                    valueRange = 5f..120f,
                    steps = 22,
                    colors = SliderDefaults.colors(
                        thumbColor = Nm.primary,
                        activeTrackColor = Nm.primary,
                        inactiveTrackColor = Nm.darkShadow.copy(alpha = 0.2f)
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.rhythm_rest, breakDurationMin),
                    color = Nm.onSurface, style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = breakDurationMin.toFloat(),
                    onValueChange = { breakDurationMin = it.toInt() },
                    valueRange = 1f..60f,
                    steps = 58,
                    colors = SliderDefaults.colors(
                        thumbColor = Nm.cyan,
                        activeTrackColor = Nm.cyan,
                        inactiveTrackColor = Nm.darkShadow.copy(alpha = 0.2f)
                    )
                )
            }
        }

        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth().auraEnter(2), padding = 20.dp, corner = 24.dp) {
                RhythmSwitchRow(
                    icon = Icons.Default.NotificationsActive,
                    label = stringResource(R.string.rhythm_notify),
                    sublabel = stringResource(R.string.rhythm_notify_desc),
                    checked = warningEnabled,
                    onCheckedChange = { warningEnabled = it }
                )
                if (warningEnabled) {
                    Column(Modifier.padding(start = 42.dp, top = 4.dp, bottom = 10.dp)) {
                        Text(
                            stringResource(R.string.rhythm_warning_before, warningMin),
                            color = Nm.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = warningMin.toFloat(),
                            onValueChange = { warningMin = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = Nm.primary,
                                activeTrackColor = Nm.primary,
                                inactiveTrackColor = Nm.darkShadow.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
                NmDivider(Modifier.padding(vertical = 12.dp))
                RhythmSwitchRow(
                    icon = Icons.Default.School,
                    label = stringResource(R.string.rhythm_learning_only),
                    sublabel = stringResource(R.string.rhythm_learning_desc),
                    checked = eduOnly,
                    onCheckedChange = { eduOnly = it }
                )
                NmDivider(Modifier.padding(vertical = 12.dp))
                RhythmSwitchRow(
                    icon = Icons.Default.AddAlarm,
                    label = stringResource(R.string.rhythm_extensions),
                    sublabel = stringResource(R.string.rhythm_extensions_desc),
                    checked = extensionsAllowed,
                    onCheckedChange = { extensionsAllowed = it }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().auraEnter(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (usageLimitMs > 0) {
                    NeumorphicButton(
                        text = stringResource(R.string.rhythm_turn_off),
                        onClick = {
                            viewModel.updateBreakRules(device, 0, 0, 0, educationOnly = false, allowExtensions = false)
                        },
                        icon = Icons.Default.Delete,
                        tint = Nm.danger,
                        iconTint = Nm.danger,
                        inset = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                NeumorphicButton(
                    text = stringResource(R.string.rhythm_save),
                    onClick = {
                        viewModel.updateBreakRules(
                            device = device,
                            usageLimitMs = usageLimitMin * 60000L,
                            breakDurationMs = breakDurationMin * 60000L,
                            breakWarningMs = if (warningEnabled) warningMin * 60000L else 0L,
                            educationOnly = eduOnly,
                            allowExtensions = extensionsAllowed
                        )
                    },
                    icon = Icons.Default.Shield,
                    tint = Nm.primary,
                    iconTint = Nm.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RhythmSwitchRow(
    icon: ImageVector,
    label: String,
    sublabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NeumorphicIconTile(
            icon = icon,
            tint = Nm.primary,
            size = 34.dp,
            iconSize = 17.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Nm.onSurface, style = MaterialTheme.typography.titleSmall)
            Text(sublabel, color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        }
        NeumorphicSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ============================================================================
// ACTIVITY — the daily report, visualised
// ============================================================================

@Composable
private fun ActivitySegment(
    device: ChildDevice,
    viewModel: DeviceControlViewModel,
    dailyReport: DailyUsageReport?,
    onViewHistory: () -> Unit
) {
    if (dailyReport == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NeumorphicShimmer(Modifier.fillMaxWidth().height(150.dp), corner = 30.dp)
            NeumorphicShimmer(Modifier.fillMaxWidth().height(220.dp), corner = 24.dp)
            NeumorphicShimmer(Modifier.fillMaxWidth().height(160.dp), corner = 24.dp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Ring summary
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth().auraEnter(0), padding = 24.dp, corner = 30.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.daily_report),
                            color = Nm.onSurface, style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            dailyReport.date,
                            color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
                        NeumorphicDuration(targetMs = dailyReport.totalScreenTimeMs, fontSize = 30)
                        Text(
                            stringResource(R.string.label_total),
                            color = Nm.onSurfaceMuted, style = MaterialTheme.typography.labelSmall
                        )
                    }
                    NeumorphicProgressRing(
                        progress = (dailyReport.totalScreenTimeMs / GOAL_MS.toFloat()).coerceIn(0f, 1f),
                        ringSize = 116.dp,
                        strokeWidth = 12.dp
                    ) {
                        Text(
                            "${((dailyReport.totalScreenTimeMs / GOAL_MS.toFloat()) * 100).toInt()}%",
                            color = Nm.onSurface, fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold, fontSize = 19.sp
                        )
                    }
                }
            }
        }

        // Hourly
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth().auraEnter(1), corner = 24.dp) {
                Text(
                    stringResource(R.string.title_hourly_timeline),
                    color = Nm.onSurface, style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.desc_hourly_timeline),
                    color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(18.dp))
                if (dailyReport.hourlyBreakdown.isNullOrEmpty()) {
                    Text(
                        stringResource(R.string.label_no_hourly_data),
                        color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    NeumorphicBars(
                        data = dailyReport.hourlyBreakdown.map { usage ->
                            NmDatum(
                                label = "${usage.hour}",
                                value = usage.usageTimeMs.toFloat(),
                                color = if (usage.usageTimeMs > 30 * 60 * 1000L) Nm.primary else Nm.cyan
                            )
                        },
                        height = 130.dp,
                        labelStep = 4
                    )
                }
            }
        }

        // Category donut
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth().auraEnter(2), corner = 24.dp) {
                Text(
                    stringResource(R.string.title_usage_by_category),
                    color = Nm.onSurface, style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeumorphicDonut(
                        data = dailyReport.categoryUsages.map { usage ->
                            NmDatum(
                                label = stringResource(getCategoryNameResId(usage.category)),
                                value = usage.totalTimeMs.toFloat(),
                                color = getCategoryColor(usage.category)
                            )
                        },
                        size = 118.dp,
                        strokeWidth = 20.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dailyReport.categoryUsages.sortedByDescending { it.totalTimeMs }.take(4).forEach { usage ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(getCategoryColor(usage.category))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(getCategoryNameResId(usage.category)),
                                    color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    formatAuraDuration(usage.totalTimeMs),
                                    color = Nm.onSurface, fontFamily = MonoFontFamily, fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Most used
        item {
            NeumorphicCard(modifier = Modifier.fillMaxWidth().auraEnter(3), corner = 24.dp) {
                Text(
                    stringResource(R.string.most_used_apps),
                    color = Nm.onSurface, style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(16.dp))
                val maxApp = dailyReport.mostUsedApps.maxOfOrNull { it.totalTimeInForeground } ?: 1L
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    dailyReport.mostUsedApps.take(6).forEach { app ->
                        NeumorphicUsageBar(
                            label = app.appLabel?.takeIf { it.isNotBlank() }
                                ?: app.packageName.substringAfterLast("."),
                            valueLabel = formatAuraDuration(app.totalTimeInForeground),
                            fraction = app.totalTimeInForeground / maxApp.toFloat(),
                            color = getCategoryColor(app.category)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().auraEnter(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeumorphicButton(
                    text = stringResource(R.string.action_refresh_report),
                    onClick = { viewModel.fetchDailyReport(device) },
                    icon = Icons.Default.Refresh,
                    inset = true,
                    modifier = Modifier.weight(1f)
                )
                NeumorphicButton(
                    text = stringResource(R.string.nav_reports),
                    onClick = onViewHistory,
                    icon = Icons.Default.History,
                    inset = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================================================
// Shared atoms
// ============================================================================

@Composable
private fun ConsoleAppRow(
    app: AppUsageLog,
    rule: BlockingRule?,
    iconBase64: String?,
    totalScreenTime: Long,
    onToggleBlock: () -> Unit,
    onToggleWeb: () -> Unit
) {
    val isBlocked = rule != null && (rule.blockEndTime > System.currentTimeMillis() || rule.isPermanentlyBlocked)
    val isWebBlocked = rule?.isInternetBlocked == true

    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIconBadge(packageName = app.packageName, category = app.category, iconBase64 = iconBase64, size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                app.appLabel?.takeIf { it.isNotBlank() }
                    ?: app.packageName.substringAfterLast("."),
                color = Nm.onSurface, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            NeumorphicUsageBar(
                label = "",
                valueLabel = formatAuraDuration(app.totalTimeInForeground),
                fraction = if (totalScreenTime > 0) app.totalTimeInForeground / totalScreenTime.toFloat() else 0f,
                color = getCategoryColor(app.category)
            )
        }
        Spacer(Modifier.width(12.dp))
        NeumorphicIconTile(
            icon = if (isBlocked) Icons.Default.Lock else Icons.Default.LockOpen,
            onClick = onToggleBlock,
            tint = if (isBlocked) Nm.danger else Nm.success,
            size = 36.dp,
            iconSize = 16.dp
        )
        Spacer(Modifier.width(6.dp))
        NeumorphicIconTile(
            icon = if (isWebBlocked) Icons.Default.WifiOff else Icons.Default.Wifi,
            onClick = onToggleWeb,
            tint = if (isWebBlocked) Nm.danger else Nm.onSurfaceMuted,
            size = 36.dp,
            iconSize = 16.dp
        )
    }
}

@Composable
private fun AppIconBadge(
    packageName: String,
    category: AppCategory,
    iconBase64: String?,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    val color = getCategoryColor(category)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = remember(iconBase64) {
            iconBase64?.let {
                try {
                    val bytes = Base64.decode(it, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
        }
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(getCategoryIcon(category), null, tint = color, modifier = Modifier.size(size / 2))
        }
    }
}

// ============================================================================
// Dialogs — consistent NEUMORPHIC voice
// ============================================================================

@Composable
private fun NmDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Nm.darkShadow.copy(alpha = 0.18f)))
}

@Composable
private fun ConsoleConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Nm.surface,
        title = { Text(title, color = Nm.onSurface, style = MaterialTheme.typography.titleLarge) },
        text = { Text(body, color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (danger) Nm.danger else Nm.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Nm.onSurfaceMuted) }
        }
    )
}

@Composable
private fun ConsoleInputDialog(
    title: String,
    label: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Nm.surface,
        title = { Text(title, color = Nm.onSurface, style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
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
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) {
                Text(stringResource(R.string.save), color = Nm.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Nm.onSurfaceMuted) }
        }
    )
}

@Composable
private fun DurationPickerDialog(
    title: String,
    description: String,
    options: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    allowRemove: Boolean = false
) {
    var customMode by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("45") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Nm.surface,
        title = { Text(title, color = Nm.onSurface, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(description, color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                if (customMode) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { if (it.all(Char::isDigit)) customText = it },
                        label = { Text(stringResource(R.string.dialog_custom_timer_label)) },
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
                } else {
                    options.chunked(3).forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowOptions.forEach { minutes ->
                                Box(Modifier.weight(1f)) {
                                    NeumorphicButton(
                                        text = if (minutes >= 60) "${minutes / 60}h${if (minutes % 60 > 0) " ${minutes % 60}m" else ""}" else "${minutes}m",
                                        onClick = { onConfirm(minutes); onDismiss() },
                                        inset = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            repeat(3 - rowOptions.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    NeumorphicButton(
                        text = stringResource(R.string.dialog_timer_custom),
                        onClick = { customMode = true },
                        icon = Icons.Default.Add,
                        inset = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (customMode) {
                TextButton(
                    onClick = { customText.toIntOrNull()?.takeIf { it > 0 }?.let { onConfirm(it); onDismiss() } },
                    enabled = (customText.toIntOrNull() ?: 0) > 0
                ) {
                    Text(stringResource(R.string.action_set), color = Nm.primary, fontWeight = FontWeight.Bold)
                }
            } else if (allowRemove) {
                TextButton(onClick = { onConfirm(0); onDismiss() }) {
                    Text(stringResource(R.string.label_none), color = Nm.danger)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Nm.onSurfaceMuted) }
        }
    )
}

@Composable
private fun CategoryPickerDialog(
    current: AppCategory,
    onDismiss: () -> Unit,
    onConfirm: (AppCategory) -> Unit
) {
    var selected by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Nm.surface,
        title = { Text(stringResource(R.string.dialog_edit_category_title), color = Nm.onSurface, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppCategory.values().forEach { category ->
                    val isSelected = selected == category
                    val color = getCategoryColor(category)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { selected = category }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(getCategoryIcon(category), null, tint = color, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(getCategoryNameResId(category)),
                            color = if (isSelected) Nm.onSurface else Nm.onSurfaceMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(R.string.save), color = Nm.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Nm.onSurfaceMuted) }
        }
    )
}

// ============================================================================
// Category identity helpers
// ============================================================================

internal fun getCategoryNameResId(category: AppCategory): Int = when (category) {
    AppCategory.SOCIAL -> R.string.category_social
    AppCategory.GAMES -> R.string.category_games
    AppCategory.EDUCATION -> R.string.category_education
    AppCategory.PRODUCTIVITY -> R.string.category_productivity
    AppCategory.ENTERTAINMENT -> R.string.category_entertainment
    AppCategory.SYSTEM -> R.string.category_system
    AppCategory.OTHER -> R.string.category_other
}

internal fun getCategoryIcon(category: AppCategory): ImageVector = when (category) {
    AppCategory.SOCIAL -> Icons.Default.People
    AppCategory.GAMES -> Icons.Default.SportsEsports
    AppCategory.EDUCATION -> Icons.Default.School
    AppCategory.PRODUCTIVITY -> Icons.Default.Work
    AppCategory.ENTERTAINMENT -> Icons.Default.Movie
    AppCategory.SYSTEM -> Icons.Default.Settings
    AppCategory.OTHER -> Icons.Default.Apps
}

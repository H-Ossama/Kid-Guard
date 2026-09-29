package com.parentalguard.parent.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.parentalguard.common.model.DailyUsageReport
import com.parentalguard.parent.UnlockRequestData
import com.parentalguard.parent.data.ReportsRepository
import com.parentalguard.parent.ui.aura.Aura
import com.parentalguard.parent.ui.aura.AuraBackground
import com.parentalguard.parent.ui.components.SplashScreen
import com.parentalguard.parent.ui.navigation.DockItem
import com.parentalguard.parent.ui.navigation.Screen
import com.parentalguard.parent.ui.navigation.dockItems
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.ui.neumorphic.neumorphic
import com.parentalguard.parent.ui.neumorphic.rememberNmPress
import com.parentalguard.parent.ui.screens.AboutScreen
import com.parentalguard.parent.ui.screens.CircleScreen
import com.parentalguard.parent.ui.screens.ControlScreen
import com.parentalguard.parent.ui.screens.DeviceConsoleScreen
import com.parentalguard.parent.ui.screens.DeviceOwnerGuideScreen
import com.parentalguard.parent.ui.screens.HelpSupportScreen
import com.parentalguard.parent.ui.screens.InsightsScreen
import com.parentalguard.parent.ui.screens.PinLockScreen
import com.parentalguard.parent.ui.screens.PulseScreen
import com.parentalguard.parent.ui.screens.ReportDetailScreen
import com.parentalguard.parent.ui.screens.RequestScreen
import com.parentalguard.parent.share.ChildApkSharer
import com.parentalguard.parent.ui.onboarding.OnboardingManager
import com.parentalguard.parent.ui.onboarding.OnboardingScreen
import com.parentalguard.parent.ui.theme.ParentalGuardTheme
import com.parentalguard.parent.ui.theme.DisplayFontFamily
import com.parentalguard.parent.ui.theme.trackingFor
import com.parentalguard.parent.viewmodel.ChildDevice
import com.parentalguard.parent.viewmodel.DeviceControlViewModel
import com.parentalguard.parent.viewmodel.DiscoveryViewModel

@Composable
fun ParentalControlApp(
    discoveryViewModel: DiscoveryViewModel = viewModel(),
    controlViewModel: DeviceControlViewModel = viewModel(),
    initialDeviceId: String? = null,
    initialUnlockRequest: UnlockRequestData? = null
) {
    ParentalGuardTheme {
        val context = LocalContext.current
        var splashDone by remember { mutableStateOf(false) }
        if (!splashDone) {
            SplashScreen(onFinished = { splashDone = true })
            return@ParentalGuardTheme
        }

        var onboardingDone by remember { mutableStateOf(OnboardingManager.isCompleted(context)) }
        if (!onboardingDone) {
            OnboardingScreen(
                onFinish = {
                    OnboardingManager.markCompleted(context)
                    onboardingDone = true
                }
            )
            return@ParentalGuardTheme
        }

        val navController = rememberNavController()
        val devices by discoveryViewModel.devices.collectAsState()
        val isScanning by discoveryViewModel.isScanning.collectAsState()

        var selectedDevice by remember { mutableStateOf<ChildDevice?>(null) }
        var selectedReport by remember { mutableStateOf<DailyUsageReport?>(null) }
        val reportsRepository = remember { ReportsRepository(discoveryViewModel.getApplication()) }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Deep links from notifications
        LaunchedEffect(initialDeviceId, initialUnlockRequest) {
            if (initialUnlockRequest != null) {
                navController.navigate(
                    Screen.Request.createRoute(
                        deviceId = initialUnlockRequest.deviceId,
                        deviceName = initialUnlockRequest.deviceName,
                        requestType = initialUnlockRequest.requestType,
                        appPackageName = initialUnlockRequest.appPackageName,
                        appName = initialUnlockRequest.appName
                    )
                ) { popUpTo(Screen.Pulse.route) }
            } else if (initialDeviceId != null) {
                navController.navigate(Screen.Console.createRoute(initialDeviceId)) {
                    popUpTo(Screen.Pulse.route)
                }
            }
        }

        androidx.activity.compose.BackHandler(enabled = navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }

        val showDock = currentRoute in dockItems.map { it.route }

        AuraBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Pulse.route,
                    modifier = Modifier
                        .fillMaxSize(),
                    enterTransition = {
                        fadeIn(Aura.tweenSlow()) + scaleIn(
                            initialScale = 0.94f,
                            animationSpec = Aura.tweenSlow()
                        )
                    },
                    exitTransition = { fadeOut(Aura.tweenFast()) },
                    popEnterTransition = { fadeIn(Aura.tweenMed()) },
                    popExitTransition = {
                        fadeOut(Aura.tweenMed()) + androidx.compose.animation.scaleOut(
                            targetScale = 1.04f,
                            animationSpec = Aura.tweenMed()
                        )
                    }
                ) {
                    composable(Screen.Pulse.route) {
                        val deviceStatuses by discoveryViewModel.deviceStatuses.collectAsState()
                        PulseScreen(
                            devices = devices,
                            deviceStatuses = deviceStatuses,
                            onDeviceClick = { device ->
                                selectedDevice = device
                                navController.navigate(Screen.Console.createRoute(device.deviceId))
                            },
                            onViewAllDevices = { navController.navigate(Screen.Circle.route) },
                            onScanQR = { navController.navigate(Screen.Pair.route) },
                            onLockAll = {
                                devices.forEach { device ->
                                    controlViewModel.lockDevice(device, true, discoveryViewModel)
                                }
                            },
                            onRefresh = { discoveryViewModel.refreshDevices() },
                            onOpenInsights = { navController.navigate(Screen.Insights.route) },
                            onOpenDeviceOwnerGuide = { navController.navigate(Screen.DeviceOwnerGuide.route) },
                            onToggleDeviceLock = { device ->
                                val locked = discoveryViewModel.deviceStatuses.value[device.deviceId]?.isLocked == true
                                controlViewModel.lockDevice(device, !locked, discoveryViewModel)
                            }
                        )
                    }

                    composable(Screen.Circle.route) {
                        val bluetoothCandidates by discoveryViewModel.bluetoothCandidates.collectAsState()
                        val isBluetoothScanning by discoveryViewModel.isBluetoothScanning.collectAsState()
                        val deviceStatuses by discoveryViewModel.deviceStatuses.collectAsState()
                        CircleScreen(
                            devices = devices,
                            isScanning = isScanning,
                            bluetoothCandidates = bluetoothCandidates,
                            isBluetoothScanning = isBluetoothScanning,
                            deviceStatuses = deviceStatuses,
                            onStartScan = { discoveryViewModel.startDiscovery() },
                            onStartBluetoothScan = { discoveryViewModel.startBluetoothScan() },
                            onStopBluetoothScan = { discoveryViewModel.stopBluetoothScan() },
                            onConnectBluetooth = { candidate -> discoveryViewModel.connectBluetoothDevice(candidate) },
                            onDeviceSelected = { device ->
                                selectedDevice = device
                                navController.navigate(Screen.Console.createRoute(device.deviceId))
                            },
                            onScanQR = { navController.navigate(Screen.Pair.route) },
                            onResetAll = { discoveryViewModel.resetAllDevices() },
                            onRemoveDevice = { device -> discoveryViewModel.removeDevice(device) }
                        )
                    }

                    composable(
                        route = Screen.Console.route,
                        arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val deviceId = backStackEntry.arguments?.getString("deviceId")
                        val device = devices.find { it.deviceId == deviceId }
                            ?: devices.find { it.ip.hostAddress == deviceId }
                            ?: selectedDevice?.takeIf { it.deviceId == deviceId }

                        if (device != null) {
                            DeviceConsoleScreen(
                                device = device,
                                viewModel = controlViewModel,
                                discoveryViewModel = discoveryViewModel,
                                onBack = { navController.popBackStack() },
                                onViewHistory = { navController.navigate(Screen.Insights.route) },
                                onOpenDeviceOwnerGuide = { navController.navigate(Screen.DeviceOwnerGuide.route) },
                                onDeviceRemoved = {
                                    discoveryViewModel.removeDevice(device)
                                    navController.popBackStack()
                                }
                            )
                        } else {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        }
                    }

                    composable(Screen.Pair.route) {
                        QRScannerScreen(
                            onQrScanned = { code ->
                                discoveryViewModel.addManualDevice(code, 8080) { device ->
                                    selectedDevice = device
                                     navController.navigate(Screen.Console.createRoute(device.deviceId)) {
                                        popUpTo(Screen.Pulse.route)
                                    }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.Request.route,
                        arguments = listOf(
                            navArgument("deviceId") { type = NavType.StringType },
                            navArgument("deviceName") { type = NavType.StringType },
                            navArgument("requestType") { type = NavType.StringType },
                            navArgument("appPackageName") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("appName") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                        val intentDeviceName = backStackEntry.arguments?.getString("deviceName") ?: ""
                        val requestType = backStackEntry.arguments?.getString("requestType") ?: "DEVICE"
                        val appPackageName = backStackEntry.arguments?.getString("appPackageName")
                        val appName = backStackEntry.arguments?.getString("appName")

                        val resolvedDeviceName = remember(deviceId, devices) {
                            devices.find { it.ip.hostAddress == deviceId }?.customName ?: intentDeviceName
                        }

                        RequestScreen(
                            deviceId = deviceId,
                            deviceName = resolvedDeviceName,
                            requestType = requestType,
                            appPackageName = appPackageName,
                            appName = appName,
                            viewModel = controlViewModel,
                            discoveryViewModel = discoveryViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Insights.route) {
                        InsightsScreen(
                            reportsRepository = reportsRepository,
                            onReportClick = { report ->
                                selectedReport = report
                                navController.navigate(Screen.ReportDetail.route)
                            }
                        )
                    }

                    composable(Screen.ReportDetail.route) {
                        ReportDetailScreen(
                            report = selectedReport,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Control.route) {
                        ControlScreen(
                            devices = devices,
                            onLanguageChanged = { languageCode ->
                                discoveryViewModel.syncLanguage(languageCode)
                            },
                            onOpenDeviceOwnerGuide = { navController.navigate(Screen.DeviceOwnerGuide.route) },
                            onShareChildApk = { ChildApkSharer.share(context) },
                            onOpenAbout = { navController.navigate(Screen.About.route) },
                            onOpenHelpSupport = { navController.navigate(Screen.HelpSupport.route) }
                        )
                    }

                    composable(Screen.DeviceOwnerGuide.route) {
                        DeviceOwnerGuideScreen(onBack = { navController.popBackStack() })
                    }

                    composable(Screen.About.route) {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }

                    composable(Screen.HelpSupport.route) {
                        HelpSupportScreen(onBack = { navController.popBackStack() })
                    }
                }

                if (showDock) {
                    AuraDock(
                        currentRoute = currentRoute,
                        onSelect = { item ->
                            if (item.route == Screen.Pulse.route) {
                                // Pulse is the graph start destination. Pop back to the
                                // existing entry instead of navigating to a second copy.
                                navController.popBackStack(Screen.Pulse.route, inclusive = false)
                            } else if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// Floating dock — neumorphic island, concave selected seat.
// ============================================================================

@Composable
private fun AuraDock(
    currentRoute: String?,
    onSelect: (DockItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .neumorphic(shape = RoundedCornerShape(32.dp), backgroundColor = Nm.surface, elevation = 10.dp)
            .border(1.dp, Nm.primary.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dockItems.forEach { item ->
            val selected = currentRoute == item.route
            val (interactionSource, scale) = rememberNmPress(0.88f)
            val iconTint by animateColorAsState(
                targetValue = if (selected) Nm.primary else Nm.onSurfaceMuted,
                animationSpec = tween(220, easing = Nm.EaseOutSoft),
                label = "dock-tint"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(22.dp))
                    .then(
                        if (selected) {
                            Modifier.neumorphic(
                                shape = RoundedCornerShape(22.dp),
                                backgroundColor = Nm.inset,
                                elevation = 4.dp,
                                pressed = true
                            )
                        } else Modifier
                    )
                    .clickable(interactionSource = interactionSource, indication = null) { onSelect(item) }
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = stringResource(item.titleResId),
                            tint = iconTint,
                            modifier = Modifier.size(if (selected) 24.dp else 21.dp)
                        )
                    }
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = stringResource(item.titleResId),
                        color = iconTint,
                        fontFamily = DisplayFontFamily,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 10.sp,
                        letterSpacing = trackingFor(0.1.sp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

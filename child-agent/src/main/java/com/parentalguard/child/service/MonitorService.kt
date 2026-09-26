package com.parentalguard.child.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.parentalguard.child.ChildApp
import com.parentalguard.child.network.CommandServer
import com.parentalguard.child.policy.DeviceOwnerManager
import com.parentalguard.child.R
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class MonitorService : Service() {

    companion object {
        /** Best-effort "is the service alive" flag for the boot watchdog. */
        @Volatile
        var isRunning: Boolean = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.Job())

    private lateinit var commandServer: CommandServer
    private lateinit var cloudRelayClient: com.parentalguard.child.network.CloudRelayClient
    private var serviceRegistrar: com.parentalguard.child.network.ServiceRegistrar? = null
    private var lockManager: com.parentalguard.child.ui.LockManager? = null
    private var bluetoothServer: com.parentalguard.child.network.BluetoothCommandServer? = null
    private val bluetoothServerLock = Any()

    private val bluetoothStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> when (
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                ) {
                    BluetoothAdapter.STATE_ON -> serviceScope.launch {
                        ensureBluetoothServer()
                    }
                    BluetoothAdapter.STATE_OFF -> bluetoothServer?.stop()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        commandServer = CommandServer(this)
        cloudRelayClient = com.parentalguard.child.network.CloudRelayClient(this)
        val bluetoothFilter = android.content.IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothStateReceiver, bluetoothFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(bluetoothStateReceiver, bluetoothFilter)
        }
        
        serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                commandServer.start() // Starts on port 8080
                ensureBluetoothServer()
                cloudRelayClient.start()
                serviceRegistrar = com.parentalguard.child.network.ServiceRegistrar(applicationContext)
                serviceRegistrar?.registerService(8080)
            } catch (e: Exception) {
                Log.e("MonitorService", "Failed to start server/service", e)
            }
        }
    }

    private val internalReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
             if (intent.action == "com.parentalguard.child.INTERNAL_EVENT") {
                 val type = intent.getStringExtra("type")
                 val payload = intent.getStringExtra("payload")
                 val deviceName = intent.getStringExtra("deviceName")
                 val requestType = intent.getStringExtra("requestType")
                 val appPackageName = intent.getStringExtra("appPackageName")
                 val appName = intent.getStringExtra("appName")
                 
                 if (type != null) {
                     try {
                         val eventType = com.parentalguard.common.network.EventType.valueOf(type)
                         val event = com.parentalguard.common.network.Packet.Event(
                             eventType = eventType,
                             payload = payload,
                             deviceId = com.parentalguard.child.utils.DeviceUtils.getDeviceId(this@MonitorService),
                             deviceName = deviceName,
                             requestType = requestType,
                             appPackageName = appPackageName,
                             appName = appName
                         )
                         commandServer.broadcast(event)
                         
                         // Also send to cloud relay if available
                         serviceScope.launch {
                             cloudRelayClient.sendEvent(event)
                         }
                     } catch (e: Exception) {
                         Log.e("MonitorService", "Failed to broadcast internal event", e)
                     }
                 }
             }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        com.parentalguard.child.data.RuleRepository.initialize(this)
        // Re-sync the VPN on every service start: a boot-time sync may have
        // been deferred while the app was in the background.
        com.parentalguard.child.data.RuleRepository.syncVpn(this)
        // Keep the persisted rescue/watchdog armed so protection survives
        // reboots and process kills even if the boot receiver was skipped.
        runCatching { BootRescueJobService.schedule(this) }
        startForeground()
        lockManager = com.parentalguard.child.ui.LockManager(applicationContext)
        startUsageMonitoring() // Coroutine
        startLockMonitoring() // New Coroutine for Lock State
        
        // Register Internal Receiver
        val filter = android.content.IntentFilter("com.parentalguard.child.INTERNAL_EVENT")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
             // For internal broadcasts, we can use RECEIVER_NOT_EXPORTED
             registerReceiver(internalReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
             // Or verify package in onReceive?
             // But for internal broadcasts, usually LocalBroadcastManager was preferred.
             // Since we use sendBroadcast(intent.setPackage(...)), it is explicit.
             registerReceiver(internalReceiver, filter)
        }
        
if (!::commandServer.isInitialized) {
             commandServer = com.parentalguard.child.network.CommandServer(applicationContext)
             serviceRegistrar = com.parentalguard.child.network.ServiceRegistrar(applicationContext)
              
             serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                 try {
                     commandServer.start() // Starts on port 8080
                     serviceRegistrar?.registerService(8080)
                 } catch (e: Exception) {
                     Log.e("MonitorService", "Failed to start server/service", e)
                 }
             }
         }

         // Permission grants do not emit a Bluetooth state change. Retry here
         // when the service receives a new start command after permissions finish.
         ensureBluetoothServer()
         
         return START_STICKY
     }

    private fun ensureBluetoothServer() {
        serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            synchronized(bluetoothServerLock) {
                if (bluetoothServer == null) {
                    bluetoothServer = com.parentalguard.child.network.BluetoothCommandServer(applicationContext)
                }
                if (bluetoothServer?.isRunning?.value != true) {
                    bluetoothServer?.start()
                }
            }
        }
    }

    private fun startLockMonitoring() {
        // Monitor for explicit lock state changes
        serviceScope.launch {
            com.parentalguard.child.data.RuleRepository.globalLock.collect { isLocked ->
                // Must run on main thread to manipulate views
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (isLocked) {
                        Log.d("MonitorService", "Global Lock Enabled")
                        lockManager?.showLockScreen()
                    } else {
                        Log.d("MonitorService", "Global Lock Disabled")
                        lockManager?.hideLockScreen()
                    }
                }
            }
        }

        // Monitor for expiration
        serviceScope.launch {
            while (isActive) {
                val until = com.parentalguard.child.data.RuleRepository.globalLockUntil.value
                val isLocked = com.parentalguard.child.data.RuleRepository.globalLock.value
                
                if (isLocked && until > 0 && until <= System.currentTimeMillis()) {
                    Log.i("MonitorService", "Lock expired, releasing.")
                    com.parentalguard.child.data.RuleRepository.setGlobalLock(false)
                }
                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
        runCatching { unregisterReceiver(bluetoothStateReceiver) }
        unregisterReceiver(internalReceiver)
        serviceRegistrar?.unregisterService()
        commandServer?.stop()
        bluetoothServer?.stop()
        lockManager?.hideLockScreen()
        serviceScope.cancel()
    }

    private fun startUsageMonitoring() {
        serviceScope.launch {
            val monitor = com.parentalguard.child.monitor.UsageMonitor(applicationContext)
            var lastMonitoredPackage: String? = null
            var lastTick = SystemClock.elapsedRealtime()

            while (isActive) {
                try {
                    val now = SystemClock.elapsedRealtime()
                    val tickDelta = now - lastTick
                    lastTick = now

                    val topPackage = getForegroundPackage()
                    
                    // If we see ourselves, it might be the lock screen or the main app.
                    // If we are showing the lock screen, we should keep monitoring the 'last' known package
                    // to see if it's still what's 'under' the lock screen.
                    val packageToEvaluate = if (topPackage == packageName) {
                        lastMonitoredPackage
                    } else {
                        topPackage
                    }
                    
                    if (topPackage != null && topPackage != packageName) {
                        lastMonitoredPackage = topPackage
                    }

                    val ruleRepo = com.parentalguard.child.data.RuleRepository
                    val isDeviceOwner = DeviceOwnerManager.isDeviceOwner(applicationContext)
                    val usagePolicyActive = isDeviceOwner && DeviceOwnerManager.hasUsageAccess(applicationContext)

                    if (!usagePolicyActive && (
                            ruleRepo.deviceOwnerDeviceUsageLimitMs.value > 0 ||
                                ruleRepo.deviceOwnerAppUsageLimits.value.isNotEmpty() ||
                                ruleRepo.deviceOwnerUsageSuspended.value.isNotEmpty()
                        )) {
                        if (isDeviceOwner) {
                            ruleRepo.deviceOwnerUsageSuspended.value.forEach { packageName ->
                                DeviceOwnerManager.setAppSuspended(applicationContext, packageName, false)
                            }
                        }
                        ruleRepo.clearDeviceOwnerPolicies()
                        if (ruleRepo.lockReason.value == "DEVICE_OWNER_USAGE") {
                            ruleRepo.setGlobalLock(false)
                        }
                    }

                    // Device Owner limits are daily UsageStats policies. They are
                    // reconciled here so they survive service restarts and day rollover.
                    val ownerDeviceLimit = ruleRepo.deviceOwnerDeviceUsageLimitMs.value
                    if (usagePolicyActive && ownerDeviceLimit > 0) {
                        val totalTodayUsage = monitor.getTodayUsage().sumOf { it.totalTimeInForeground }
                        if (totalTodayUsage >= ownerDeviceLimit && !ruleRepo.globalLock.value) {
                            Log.i("MonitorService", "Device Owner usage limit reached")
                            ruleRepo.setGlobalLock(true, "DEVICE_OWNER_USAGE")
                            if (isDeviceOwner) {
                                DeviceOwnerManager.lockNow(applicationContext)
                            }
                        }
                    } else if (ruleRepo.globalLock.value && ruleRepo.lockReason.value == "DEVICE_OWNER_USAGE") {
                        ruleRepo.setGlobalLock(false)
                    }

                    // Update "Take a Break" usage tracking
                    // Only count real foreground time while the screen is on and no lock is showing.
                    if (topPackage != null && topPackage != packageName && !lockManager!!.isShowing && tickDelta > 0) {
                        val limit = ruleRepo.usageLimitMs.value
                        if (limit > 0) {
                            val currentUsage = ruleRepo.currentBreakUsageMs.value + tickDelta
                            ruleRepo.setCurrentBreakUsage(currentUsage)
                            
                            if (currentUsage >= limit) {
                                val breakDuration = ruleRepo.breakDurationMs.value
                                Log.i("MonitorService", "Usage limit reached ($currentUsage/$limit). Triggering break for $breakDuration ms")
                                ruleRepo.setGlobalLockUntil(System.currentTimeMillis() + breakDuration, "BREAK")
                                ruleRepo.setCurrentBreakUsage(0) // Reset for after the break
                            }
                        }
                    }

                    if (packageToEvaluate != null && packageToEvaluate != packageName) {
                        // Check for App Timer (Temporary Allowance / Enforcement)
                        val timerActive = ruleRepo.isAppTimerActive(packageToEvaluate)
                        val timerSet = ruleRepo.appTimers.value.containsKey(packageToEvaluate)
                        
                        // Check for temporary global unlock
                        val isTemporarilyUnlocked = ruleRepo.isTemporarilyUnlocked()
                        val ownerAppLimit = ruleRepo.deviceOwnerAppUsageLimits.value[packageToEvaluate] ?: 0L
                        val currentUsageMs = monitor.getAppUsageToday(packageToEvaluate)
                        val ownerLimitReached = ownerAppLimit > 0 && currentUsageMs >= ownerAppLimit
                        val isOwner = usagePolicyActive
                        val category = ruleRepo.getCategory(packageToEvaluate)
                        val categoryTimerActive = ruleRepo.isCategoryTimerActive(category)
                        val categoryTimerSet = ruleRepo.categoryTimers.value.containsKey(category)
                        val allowanceActive = isTemporarilyUnlocked ||
                            (timerSet && timerActive) ||
                            (categoryTimerSet && categoryTimerActive)

                        if (isOwner && packageToEvaluate in ruleRepo.deviceOwnerUsageSuspended.value &&
                            (!ownerLimitReached || allowanceActive)
                        ) {
                            val result = DeviceOwnerManager.setAppSuspended(applicationContext, packageToEvaluate, false)
                            if (result.success) ruleRepo.markDeviceOwnerUsageSuspended(packageToEvaluate, false)
                        }

                        var shouldBlock = false
                        
                        if (isTemporarilyUnlocked) {
                            shouldBlock = false
                        } else if (timerSet) {
                            if (timerActive) {
                                shouldBlock = false // Explicitly allowed by timer
                            } else {
                                shouldBlock = true // Timer expired, force block
                            }
                        } else {
                            // Check for Category Timer
                            if (categoryTimerSet) {
                                if (categoryTimerActive) {
                                    shouldBlock = false // Explicitly allowed by category timer
                                } else {
                                    shouldBlock = true // Category timer expired, force block
                                }
                            } else {
                                // Normal Rules
                                val rules = ruleRepo.rules.value
                                val rule = rules.find { it.packageName == packageToEvaluate }
                                if (ownerLimitReached) {
                                    if (isOwner && packageToEvaluate !in ruleRepo.deviceOwnerUsageSuspended.value) {
                                        val result = DeviceOwnerManager.setAppSuspended(applicationContext, packageToEvaluate, true)
                                        if (result.success) ruleRepo.markDeviceOwnerUsageSuspended(packageToEvaluate, true)
                                    }
                                    shouldBlock = true
                                } else if (rule != null) {
                                    val isTimedBlocked = rule.blockEndTime > System.currentTimeMillis()
                                    val isPermanentlyBlocked = rule.isPermanentlyBlocked

                                    // Check daily limit
                                    val dailyLimitMs = rule.maxDailyTimeMs
                                    val isDailyLimitReached = dailyLimitMs > 0 && currentUsageMs >= dailyLimitMs

                                    shouldBlock = isTimedBlocked || isPermanentlyBlocked || isDailyLimitReached
                                }
                            }
                        }
                        
                        // Global Lock
                        val globalLock = ruleRepo.globalLock.value
                        
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (shouldBlock || globalLock) {
                                if (lockManager?.isShowing == false) {
                                    Log.i("MonitorService", "Blocking app: $packageToEvaluate (Reason: ${if(globalLock) "Global Lock" else "App Rule/Timer"})")
                                    lockManager?.showLockScreen()
                                }
                            } else {
                                if (lockManager?.isShowing == true) {
                                     Log.i("MonitorService", "Unblocking app: $packageToEvaluate")
                                     lockManager?.hideLockScreen()
                                }
                            }
                        }
                    } else if (packageToEvaluate == null) {
                        // If no package detected (e.g. permission missing), we might want to default to block if we are extra strict
                        // but for now let's just log it.
                        // Log.w("MonitorService", "Could not detect foreground package - verification needed")
                    }
                } catch (e: Exception) {
                    Log.e("MonitorService", "Error monitoring usage", e)
                }
                delay(1000)
            }
        }
    }

    private fun getForegroundPackage(): String? {
        // When the screen is off the last foreground package is stale; treating it as
        // "in use" would count sleep time toward usage limits and could trigger locks.
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
        if (!powerManager.isInteractive) return null

        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager ?: return null
        val time = System.currentTimeMillis()
        // Query events for the last 2 minutes to capture recent activity
        val events = usm.queryEvents(time - 1000 * 120, time) 
        val event = android.app.usage.UsageEvents.Event()
        var lastPackage: String? = null
        var lastEventTime = 0L
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.timeStamp >= lastEventTime) {
                    lastEventTime = event.timeStamp
                    lastPackage = event.packageName
                }
            }
        }
        
        // Fallback to UsageStats if no recent event found (e.g. app open for > 2 mins)
        if (lastPackage == null) {
             val stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60, time)
             lastPackage = stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        }
        
        return lastPackage
    }

    private fun startForeground() {
        val disguises = listOf(
            Pair(R.string.stealth_battery, R.string.stealth_battery_desc),
            Pair(R.string.stealth_update, R.string.stealth_update_desc),
            Pair(R.string.stealth_wifi, R.string.stealth_wifi_desc),
            Pair(R.string.stealth_sync, R.string.stealth_sync_desc)
        )
        val (titleId, descId) = disguises.random()

        val notification = NotificationCompat.Builder(this, ChildApp.CHANNEL_ID)
            .setContentTitle(getString(titleId))
            .setContentText(getString(descId))
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_MIN) // Low priority for stealth
            .setShowWhen(false)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }
    }
}

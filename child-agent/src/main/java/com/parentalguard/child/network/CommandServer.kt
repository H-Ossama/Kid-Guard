package com.parentalguard.child.network

import com.parentalguard.child.data.RuleRepository
import com.parentalguard.child.monitor.UsageMonitor
import com.parentalguard.child.service.MonitorService
import com.parentalguard.child.policy.DeviceOwnerManager
import com.parentalguard.child.update.ApkInstaller
import com.parentalguard.common.model.DeviceStats
import com.parentalguard.common.network.CommandType
import com.parentalguard.common.network.Packet
import android.content.Intent
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.readAvailable
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import android.content.Context
import android.util.Log
import java.time.Duration
import java.util.Collections
import kotlinx.coroutines.launch
import java.security.MessageDigest

class CommandServer(private val context: Context) {
    
    private var server: NettyApplicationEngine? = null
    private val sessions = Collections.synchronizedList(ArrayList<DefaultWebSocketServerSession>())
    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true
    }
    
    // Store parent IP for sending unlock requests
    var parentIp: String? = null

    /**
     * Validates the pairing token carried by an inbound call. Compares
     * constant-time to avoid trivial timing side-channels.
     */
    private fun isAuthorized(call: io.ktor.server.application.ApplicationCall): Boolean {
        val expected = PairingManager.getToken(context) ?: return false
        val provided = call.request.headers["X-Pair-Token"]
            ?: call.request.queryParameters["token"]
            ?: return false
        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.UTF_8),
            provided.toByteArray(Charsets.UTF_8)
        )
    }

    fun start() {
        if (server != null) return

        server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(15)
                maxFrameSize = 64 * 1024 // Bound frame size (was Long.MAX_VALUE = memory-exhaustion risk)
                masking = false
            }
            install(ContentNegotiation) {
                json(json)
            }

            // C3: require the pairing token on every route except /ping.
            // Token is sent as the X-Pair-Token header (HTTP) or ?token= query param (WebSocket).
            intercept(ApplicationCallPipeline.Call) {
                val path = call.request.path()
                if (path != "/ping" && !isAuthorized(call)) {
                    call.respond(Packet.Response(false, "Unauthorized"))
                    finish()
                }
            }

            routing {
                // ... Existing HTTP routes ...

                webSocket("/events") {
                    Log.i("CommandServer", "New WebSocket connection")
                    sessions.add(this)
                    try {
                        for (frame in incoming) {
                            // Keep alive
                        }
                    } catch (e: Exception) {
                        Log.e("CommandServer", "WebSocket error", e)
                    } finally {
                        Log.i("CommandServer", "WebSocket disconnected")
                        sessions.remove(this)
                    }
                }
                
                get("/stats") {
                    val includeIcons = call.request.queryParameters["includeIcons"]?.toBoolean() ?: false
                    // ...
                    val monitor = UsageMonitor(this@CommandServer.context)
                    val logs = monitor.getTodayUsage()
                    
                    val batteryLevel = com.parentalguard.child.utils.DeviceUtils.getBatteryLevel(this@CommandServer.context)
                    val installedApps = com.parentalguard.child.utils.DeviceUtils.getInstalledApps(this@CommandServer.context, includeIcons)
                    val hourlyBreakdown = monitor.getHourlyBreakdown()
                    val activeRules = RuleRepository.rules.value
                    val appTimers = RuleRepository.appTimers.value
                    
                    val isLocked = RuleRepository.globalLock.value
                    
                    val p = this@CommandServer.context.packageManager
                    val componentName = android.content.ComponentName(this@CommandServer.context, "com.parentalguard.child.MainActivity")
                    val isIconHidden = try {
                        p.getComponentEnabledSetting(componentName) == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    } catch (e: Exception) {
                        false
                    }
                    
                    val stats = DeviceStats(
                        batteryLevel = batteryLevel,
                        lastSeenTimestamp = System.currentTimeMillis(),
                        usageLogs = logs,
                        installedApps = installedApps,
                        hourlyBreakdown = hourlyBreakdown,
                        activeRules = activeRules,
                        isLocked = isLocked,
                        isIconHidden = isIconHidden,
                        appTimers = appTimers,
                        categoryTimers = RuleRepository.categoryTimers.value,
                        deviceName = com.parentalguard.child.utils.DeviceUtils.getDeviceName(this@CommandServer.context),
                        usageLimitMs = RuleRepository.usageLimitMs.value,
                        breakDurationMs = RuleRepository.breakDurationMs.value,
                        deviceOwnerCapabilities = DeviceOwnerManager.capabilities(this@CommandServer.context),
                        blockingScreenStyle = RuleRepository.blockingScreenStyle.value,
                        childAppVersionName = runCatching { com.parentalguard.child.BuildConfig.VERSION_NAME }.getOrNull(),
                        childAppVersionCode = runCatching { com.parentalguard.child.BuildConfig.VERSION_CODE }.getOrDefault(0),
                        childPinSet = com.parentalguard.child.security.PinManager.isPinSet(this@CommandServer.context),
                        updateResult = com.parentalguard.child.update.ApkInstallReceiver.lastResult(this@CommandServer.context),
                        pinProtectionEnabled = com.parentalguard.child.security.PinManager.isProtectionEnabled(this@CommandServer.context)
                    )
                    call.respond(Packet.Response(true, stats = stats))
                }

                post("/unhide") {
                    try {
                        val packet = call.receive<Packet.Command>()
                        if (packet.commandType == CommandType.UNHIDE_APP) {
                            val p = this@CommandServer.context.packageManager
                            val componentName = android.content.ComponentName(this@CommandServer.context, "com.parentalguard.child.MainActivity")
                            p.setComponentEnabledSetting(
                                componentName,
                                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                android.content.pm.PackageManager.DONT_KILL_APP
                            )
                            Log.i("CommandServer", "Unhide app command executed")
                            call.respond(Packet.Response(true, "App unhidden"))
                        } else {
                            call.respond(Packet.Response(false, "Invalid unhide command"))
                        }
                    } catch (e: Exception) {
                        Log.e("CommandServer", "Error unhiding app", e)
                        call.respond(Packet.Response(false, e.message))
                    }
                }

                post("/hide") {
                    try {
                        val packet = call.receive<Packet.Command>()
                        if (packet.commandType == CommandType.HIDE_APP) {
                            val p = this@CommandServer.context.packageManager
                            val componentName = android.content.ComponentName(this@CommandServer.context, "com.parentalguard.child.MainActivity")
                            p.setComponentEnabledSetting(
                                componentName,
                                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                android.content.pm.PackageManager.DONT_KILL_APP
                            )
                            Log.i("CommandServer", "Hide app command executed")
                            call.respond(Packet.Response(true, "App hidden"))
                        } else {
                            call.respond(Packet.Response(false, "Invalid hide command"))
                        }
                    } catch (e: Exception) {
                        Log.e("CommandServer", "Error hiding app", e)
                        call.respond(Packet.Response(false, e.message))
                    }
                }

                post("/rules") {
                    try {
                        val packet = call.receive<Packet.Command>()
                        if (packet.commandType == CommandType.SET_BLOCKING_SCREEN_STYLE && packet.blockingScreenStyle != null) {
                            RuleRepository.setBlockingScreenStyle(packet.blockingScreenStyle!!)
                            call.respond(Packet.Response(true, "Blocked-screen style updated", requestId = packet.requestId))
                        } else if (packet.commandType == CommandType.UPDATE_RULES && packet.ruleSet != null) {
                            RuleRepository.updateRules(packet.ruleSet!!.rules)
                            RuleRepository.updateCategoryLimits(packet.ruleSet!!.categoryLimits)
                            // Only explicit future timestamps touch lock state. A plain
                            // rules/break sync carries null (or 0 from older parents)
                            // and must never release an active lock or wipe an
                            // approved temporary unlock.
                            packet.ruleSet!!.temporaryUnlockUntil?.takeIf { it > 0 }?.let {
                                RuleRepository.setTemporaryUnlock(it)
                            }
                            packet.ruleSet!!.globalLockUntil?.takeIf { it > 0 }?.let {
                                RuleRepository.setGlobalLockUntil(it)
                            }
                            RuleRepository.setBreakRules(packet.ruleSet!!.usageLimitMs, packet.ruleSet!!.breakDurationMs)
                            Log.i("CommandServer", "Rules updated: ${packet.ruleSet!!.rules.size} rules, Break: ${packet.ruleSet!!.usageLimitMs}/${packet.ruleSet!!.breakDurationMs}")
                            call.respond(Packet.Response(true, "Rules updated", requestId = packet.requestId))
                        } else {
                            call.respond(Packet.Response(false, "Invalid command", requestId = packet.requestId))
                        }
                    } catch (e: Exception) {
                        Log.e("CommandServer", "Error updating rules", e)
                        call.respond(Packet.Response(false, e.message))
                    }
                }

                post("/device-owner") {
                    try {
                        val packet = call.receive<Packet.Command>()
                        call.respond(CommandDispatcher.dispatch(this@CommandServer.context, packet))
                    } catch (e: Exception) {
                        Log.e("CommandServer", "Error handling Device Owner command", e)
                        call.respond(Packet.Response(false, e.message))
                    }
                }

                post("/device-name") {
                    try {
                        val packet = call.receive<Packet.Command>()
                        if (packet.commandType == CommandType.UPDATE_DEVICE_NAME && packet.deviceName != null) {
                            Log.i("CommandServer", "Renaming device to: ${packet.deviceName}")
                            com.parentalguard.child.utils.DeviceUtils.setCustomDeviceName(this@CommandServer.context, packet.deviceName!!)
                            call.respond(Packet.Response(true, "Device renamed"))
                        } else if (packet.commandType == CommandType.SET_APP_CATEGORY && packet.packageName != null && packet.category != null) {
                            val pkgName = packet.packageName!!
                            val cat = packet.category!!
                            RuleRepository.setCustomCategory(pkgName, cat)
                            Log.i("CommandServer", "Set category for $pkgName to $cat")
                            call.respond(Packet.Response(true, "Category updated"))
                        } else if (packet.commandType == CommandType.SET_APP_TIMER && packet.packageName != null && packet.timerDurationMs != null) {
                            val pkgName = packet.packageName!!
                            val duration = packet.timerDurationMs!!
                            RuleRepository.setAppTimer(pkgName, duration)
                            Log.i("CommandServer", "Set timer for $pkgName to ${duration}ms")
                            call.respond(Packet.Response(true, "Timer set"))
                        } else if (packet.commandType == CommandType.SET_CATEGORY_TIMER && packet.category != null && packet.timerDurationMs != null) {
                            val cat = packet.category!!
                            val duration = packet.timerDurationMs!!
                            RuleRepository.setCategoryTimer(cat, duration)
                            Log.i("CommandServer", "Set category timer for $cat to ${duration}ms")
                            call.respond(Packet.Response(true, "Category timer set"))
                        } else if (packet.commandType == CommandType.SET_LANGUAGE && packet.languageCode != null) {
                            Log.i("CommandServer", "Ignoring language command; child follows device locale")
                            call.respond(Packet.Response(true, "Child follows device language"))
                        } else if (packet.commandType == CommandType.SET_RELAY_PARENT_ID && packet.relayParentId != null) {
                            Log.i("CommandServer", "Relay Parent ID received: ${packet.relayParentId}")
                            val prefs = this@CommandServer.context.getSharedPreferences("relay_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("parent_id", packet.relayParentId).apply()
                            call.respond(Packet.Response(true, "Relay Parent ID updated"))
                        } else if (packet.commandType == CommandType.SET_PIN_PROTECTION && packet.enabled != null) {
                            val enabled = packet.enabled!!
                            Log.i("CommandServer", "PIN protection set to: $enabled")
                            com.parentalguard.child.security.PinManager.setProtectionEnabled(this@CommandServer.context, enabled)
                            call.respond(
                                Packet.Response(
                                    true,
                                    if (enabled) "PIN protection enabled" else "PIN protection disabled"
                                )
                            )
                        } else {
                             call.respond(Packet.Response(false, "Invalid command"))
                        }
                    } catch (e: Exception) {
                        Log.e("CommandServer", "Error renaming device", e)
                        call.respond(Packet.Response(false, e.message))
                    }
                }
                
                post("/lock") {
                     try {
                         val packet = call.receive<Packet.Command>()
                         if (packet.commandType == CommandType.LOCK_DEVICE) {
                             RuleRepository.setGlobalLock(true)
                             call.respond(Packet.Response(true, "Device Locked"))
                         } else if (packet.commandType == CommandType.UNLOCK_DEVICE) {
                             RuleRepository.setGlobalLock(false)
                             call.respond(Packet.Response(true, "Device Unlocked"))
                         } else if (packet.commandType == CommandType.SET_LANGUAGE && packet.languageCode != null) {
                              Log.i("CommandServer", "Ignoring language command; child follows device locale")
                              call.respond(Packet.Response(true, "Child follows device language"))
                         } else {
                             call.respond(Packet.Response(false, "Invalid lock/command"))
                         }
                     } catch (e: Exception) {
                         call.respond(Packet.Response(false, e.message))
                     }
                }
                
                // Handle unlock approval/denial from parent
                post("/unlock-response") {
                    try {
                        val packet = call.receive<Packet.Command>()
                        when (packet.commandType) {
                            CommandType.APPROVE_UNLOCK -> {
                                val duration = packet.unlockDurationMs ?: (10 * 60 * 1000L) // Default 10 minutes
                                if (packet.packageName != null) {
                                    RuleRepository.setAppTimer(packet.packageName!!, duration)
                                    Log.i("CommandServer", "App ${packet.packageName} unlock approved for ${duration / 60000} minutes")
                                    call.respond(Packet.Response(true, "App unlock approved"))
                                } else {
                                    val unlockUntil = System.currentTimeMillis() + duration
                                    RuleRepository.setTemporaryUnlock(unlockUntil)
                                    Log.i("CommandServer", "Device unlock approved for ${duration / 60000} minutes")
                                    call.respond(Packet.Response(true, "Device unlock approved"))
                                }
                            }
                            CommandType.DENY_UNLOCK -> {
                                Log.i("CommandServer", "Unlock request denied")
                                call.respond(Packet.Response(true, "Unlock denied"))
                            }
                            CommandType.APPROVE_EXTENSION -> {
                                // "One more minute": extend the active break (global lock) by 60s
                                Log.i("CommandServer", "Extension approved (+1 min)")
                                if (RuleRepository.globalLock.value) {
                                    val newUntil = RuleRepository.globalLockUntil.value + 60_000
                                    RuleRepository.setGlobalLockUntil(newUntil, RuleRepository.lockReason.value)
                                }
                                call.respond(Packet.Response(true, "Extension approved (+1 min)"))
                            }
                            CommandType.DENY_EXTENSION -> {
                                Log.i("CommandServer", "Extension denied")
                                call.respond(Packet.Response(true, "Extension denied"))
                            }
                            CommandType.STOP_BREAK -> {
                                Log.i("CommandServer", "Break stopped by parent")
                                RuleRepository.setGlobalLock(false)
                                call.respond(Packet.Response(true, "Break stopped"))
                            }
                            CommandType.RESET_REQUEST_COOLDOWN -> {
                                Log.i("CommandServer", "Request cooldown cleared by parent")
                                RuleRepository.clearUnlockRequestCooldown()
                                call.respond(Packet.Response(true, "Request cooldown cleared"))
                            }
                            else -> {
                                call.respond(Packet.Response(false, "Invalid unlock response"))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CommandServer", "Error handling unlock response", e)
                        call.respond(Packet.Response(false, e.message))
                    }
                }
                
                // Get daily report
                get("/daily-report") {
                    try {
                        val monitor = UsageMonitor(this@CommandServer.context)
                        val report = monitor.generateDailyReport()
                        call.respond(Packet.Response(true, dailyReport = report))
                    } catch (e: Exception) {
                        Log.e("CommandServer", "Error generating report", e)
                        call.respond(Packet.Response(false, e.message))
                    }
                }
                
                
                // Reset PIN from parent
                post("/reset-pin") {
                    try {
                        com.parentalguard.child.security.PinManager.resetPin(this@CommandServer.context)
                        Log.i("CommandServer", "PIN reset requested by parent")
                        call.respond(Packet.Response(true, "PIN reset successfully"))
                    } catch (e: Exception) {
                        Log.e("CommandServer", "Error resetting PIN", e)
                        call.respond(Packet.Response(false, e.message))
                    }
                }
                
                // Parent-pushed update: raw APK bytes in the request body.
                // Device Owner installs silently; otherwise the system shows
                // the install-confirmation screen on this device.
                post("/update") {
                    try {
                        val dir = java.io.File(this@CommandServer.context.cacheDir, "updates").apply { mkdirs() }
                        val tmp = java.io.File(dir, "parent-pushed.apk.tmp")
                        val channel = call.receiveChannel()
                        tmp.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024)
                            while (!channel.isClosedForRead) {
                                val read = channel.readAvailable(buf, 0, buf.size)
                                if (read <= 0) break
                                out.write(buf, 0, read)
                            }
                        }
                        if (tmp.length() < 1_000_000) {
                            tmp.delete()
                            call.respond(Packet.Response(false, "APK upload was truncated"))
                        } else {
                            val dest = java.io.File(dir, "parent-pushed.apk")
                            if (dest.exists()) dest.delete()
                            tmp.renameTo(dest)
                            when (val result = ApkInstaller.installApk(this@CommandServer.context, dest)) {
                                is ApkInstaller.Result.Accepted -> {
                                    com.parentalguard.child.update.ApkInstallReceiver.saveResult(
                                        this@CommandServer.context, "INSTALLING"
                                    )
                                    call.respond(Packet.Response(true, "Update accepted for install"))
                                }
                                is ApkInstaller.Result.Rejected ->
                                    call.respond(Packet.Response(false, result.reason))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CommandServer", "Error installing update", e)
                        call.respond(Packet.Response(false, e.message))
                    }
                }

                get("/ping") {
                    call.respond(Packet.Response(true, "Pong"))
                }
            }
        }.start(wait = false)
        Log.i("CommandServer", "Server started on port 8080")
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }
    
    /**
     * Broadcast an event to all connected parents (WebSocket clients).
     */
    fun broadcast(event: Packet.Event) {
        val message = json.encodeToString<Packet>(event)
        server?.application?.launch {
            synchronized(sessions) {
                sessions.forEach { session ->
                    launch {
                        try {
                            session.send(Frame.Text(message))
                        } catch (e: Exception) {
                            Log.e("CommandServer", "Broadcast failed", e)
                        }
                    }
                }
            }
        }
    }
}


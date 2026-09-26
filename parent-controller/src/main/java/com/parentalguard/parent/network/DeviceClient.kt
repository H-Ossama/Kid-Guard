package com.parentalguard.parent.network

import android.content.Context
import android.util.Log

import com.parentalguard.common.model.*
import com.parentalguard.common.network.CommandType
import com.parentalguard.common.network.Packet
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString

data class DeviceResponse<T>(
    val response: T?,
    val connectionType: com.parentalguard.parent.viewmodel.ConnectionType
)

class DeviceClient(context: Context? = null) {
    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets)
    }

    private val cloudRelay = context?.let { CloudRelayClient(it).apply { start() } }

    private val bluetoothClient = BluetoothClient(context)
    private val bluetoothMacs = java.util.concurrent.ConcurrentHashMap<String, String>() // deviceId -> BT MAC

    fun registerBluetoothMac(deviceId: String, mac: String) {
        bluetoothMacs[deviceId] = mac
    }

    private val pairTokens = java.util.concurrent.ConcurrentHashMap<String, String>() // deviceId -> pairing token

    fun registerPairToken(deviceId: String, token: String) {
        pairTokens[deviceId] = token
    }

    fun observeEvents(ip: String, port: Int, deviceId: String? = null): Flow<Packet.Event> {
        val wsPath = deviceId?.let { pairTokens[it] }?.let { "/events?token=$it" } ?: "/events"
        val localFlow = flow {
            while (currentCoroutineContext().isActive) {
                try {
                    client.webSocket(method = HttpMethod.Get, host = ip, port = port, path = wsPath) {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                try {
                                    val packet = json.decodeFromString<Packet>(text)
                                    if (packet is Packet.Event) {
                                        emit(packet.copy(deviceId = deviceId))
                                    }
                                } catch (e: Exception) {
                                    Log.e("DeviceClient", "Local event parse error", e)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("DeviceClient", "Local WebSocket failed for $ip, relying on relay if active")
                    delay(10000) // Backoff
                }
            }
        }

        val relayFlow = if (deviceId != null && cloudRelay != null) {
            cloudRelay.events.filter { it.deviceId == deviceId }
        } else {
            emptyFlow()
        }
        
        return merge(localFlow, relayFlow)
    }

    private suspend fun executeCommand(
        ip: String,
        port: Int,
        deviceId: String?,
        endpoint: String,
        command: Packet.Command,
        isGet: Boolean = false,
        skipDirect: Boolean = false
    ): DeviceResponse<Packet.Response> = withContext(Dispatchers.IO) {
        // Devices paired purely over Bluetooth use a placeholder IP (0.0.0.0);
        // the direct HTTP tier can never reach them, so go straight to RFCOMM.
        val btMac = deviceId?.let { bluetoothMacs[it] }
        val bluetoothOnly = btMac != null && (ip.isBlank() || ip == "0.0.0.0")

        // 1. Try Direct Connection
        if (!skipDirect && !bluetoothOnly) {
            try {
                val token = deviceId?.let { pairTokens[it] }
                val response: Packet.Response? = withTimeoutOrNull(5000) { // Increased to 5s
                    client.request {
                        method = if (isGet) HttpMethod.Get else HttpMethod.Post
                        url {
                            protocol = URLProtocol.HTTP
                            host = ip
                            this.port = port
                            path(endpoint.trimStart('/'))
                        }
                        if (token != null) header("X-Pair-Token", token)
                        if (isGet) {
                            if (command.includeIcons == true) parameter("includeIcons", "true")
                        } else {
                            setBody(command)
                            header("Content-Type", "application/json")
                        }
                    }.body()
                }
                if (response != null) {
                    Log.i("DeviceClient", "Direct connection success to $ip:$port")
                    return@withContext DeviceResponse(response, com.parentalguard.parent.viewmodel.ConnectionType.LOCAL)
                } else {
                    Log.w("DeviceClient", "Direct connection timed out (5s) for $ip:$port")
                }
            } catch (e: Exception) {
                Log.w("DeviceClient", "Direct connection error to $ip:$port: ${e.message}")
            }
        }

        // 2. Try Bluetooth Fallback (same WiFi not required)
        if (btMac != null) {
            try {
                val btResponse = withTimeoutOrNull(6000) {
                    bluetoothClient.executeCommand(btMac, command, deviceId?.let { pairTokens[it] })
                }
                if (btResponse != null) {
                    Log.i("DeviceClient", "Bluetooth connection success for $deviceId")
                    return@withContext DeviceResponse(btResponse, com.parentalguard.parent.viewmodel.ConnectionType.BLUETOOTH)
                } else {
                    Log.w("DeviceClient", "Bluetooth connection failed for $deviceId")
                }
            } catch (e: Exception) {
                Log.w("DeviceClient", "Bluetooth connection error for $deviceId: ${e.message}")
            }
        }

        // 3. Try Cloud Relay Fallback
        if (deviceId != null && cloudRelay != null) {
            val relayResponse = cloudRelay.sendCommand(deviceId, command)
            if (relayResponse != null) {
                return@withContext DeviceResponse(relayResponse, com.parentalguard.parent.viewmodel.ConnectionType.CLOUD)
            }
        }

        DeviceResponse(null, com.parentalguard.parent.viewmodel.ConnectionType.UNKNOWN)
    }

    suspend fun getStatsWithConnectionType(
        ip: String, 
        port: Int, 
        deviceId: String?, 
        includeIcons: Boolean = false,
        skipDirect: Boolean = false
    ): DeviceResponse<Packet.Response> {
        val command = Packet.Command(CommandType.GET_STATS, includeIcons = includeIcons)
        val first = executeCommand(ip, port, deviceId, "/stats", command, isGet = true, skipDirect = skipDirect)
        // App-icon payloads can exceed the Bluetooth framing limit (64 KB), which
        // fails the whole frame. Retry once without icons so usage and settings
        // still load over an RFCOMM-only link.
        if (includeIcons && first.response == null) {
            val retry = executeCommand(
                ip, port, deviceId, "/stats",
                Packet.Command(CommandType.GET_STATS, includeIcons = false),
                isGet = true, skipDirect = skipDirect
            )
            if (retry.response != null) return retry
        }
        return first
    }

    suspend fun getStats(ip: String, port: Int, deviceId: String?, includeIcons: Boolean = false): Packet.Response? {
        return getStatsWithConnectionType(ip, port, deviceId, includeIcons).response
    }

    suspend fun updateRules(ip: String, port: Int, deviceId: String? = null, rules: List<BlockingRule>): Packet.Response? {
        val command = Packet.Command(CommandType.UPDATE_RULES, ruleSet = RuleSet(rules))
        return executeCommand(ip, port, deviceId, "/rules", command).response
    }

    suspend fun setBlockingScreenStyle(
        ip: String,
        port: Int,
        deviceId: String? = null,
        style: BlockingScreenStyle
    ): Packet.Response? {
        val command = Packet.Command(
            commandType = CommandType.SET_BLOCKING_SCREEN_STYLE,
            blockingScreenStyle = style
        )
        return executeCommand(ip, port, deviceId, "/rules", command).response
    }
    
    suspend fun setBreakRules(
        ip: String, 
        port: Int, 
        deviceId: String? = null, 
        rules: List<BlockingRule>,
        usageLimitMs: Long,
        breakDurationMs: Long,
        breakWarningMs: Long = 0,
        educationOnly: Boolean = false,
        allowExtensions: Boolean = false
    ): Packet.Response? {
        val command = Packet.Command(
            CommandType.UPDATE_RULES, 
            ruleSet = RuleSet(
                rules = rules,
                usageLimitMs = usageLimitMs,
                breakDurationMs = breakDurationMs,
                breakWarningMs = breakWarningMs,
                educationOnly = educationOnly,
                allowExtensions = allowExtensions
            )
        )
        return executeCommand(ip, port, deviceId, "/rules", command).response
    }
    
    suspend fun updateCategoryLimits(ip: String, port: Int, deviceId: String? = null, categoryLimits: List<CategoryLimit>): Packet.Response? {
        val command = Packet.Command(CommandType.UPDATE_RULES, ruleSet = RuleSet(emptyList(), categoryLimits))
        return executeCommand(ip, port, deviceId, "/rules", command).response
    }
    
    suspend fun syncRelayParentId(ip: String, port: Int, deviceId: String? = null): Packet.Response? {
        val parentId = cloudRelay?.parentId ?: return null
        val command = Packet.Command(CommandType.SET_RELAY_PARENT_ID, relayParentId = parentId)
        // deviceId enables the cloud-relay tier when the LAN address is stale.
        return executeCommand(ip, port, deviceId, "/device-name", command).response
    }
    
    suspend fun getDailyReport(ip: String, port: Int, deviceId: String? = null): Packet.Response? {
        val command = Packet.Command(CommandType.SEND_DAILY_REPORT)
        // For now, let's keep it simple. If we need purely cloud, we might need a GET_DAILY_REPORT command type.
        return executeCommand(ip, port, deviceId, "/daily-report", command, isGet = true).response
    }
    
    suspend fun approveUnlock(ip: String, port: Int, deviceId: String? = null, durationMs: Long, packageName: String? = null): Packet.Response? {
        val command = Packet.Command(CommandType.APPROVE_UNLOCK, unlockDurationMs = durationMs, packageName = packageName)
        return executeCommand(ip, port, deviceId, "/unlock-response", command).response
    }
    
    suspend fun denyUnlock(ip: String, port: Int, deviceId: String? = null): Packet.Response? {
        val command = Packet.Command(CommandType.DENY_UNLOCK)
        return executeCommand(ip, port, deviceId, "/unlock-response", command).response
    }

    suspend fun approveExtension(ip: String, port: Int, deviceId: String? = null): Packet.Response? {
        val command = Packet.Command(CommandType.APPROVE_EXTENSION)
        return executeCommand(ip, port, deviceId, "/unlock-response", command).response
    }

    suspend fun denyExtension(ip: String, port: Int, deviceId: String? = null): Packet.Response? {
        val command = Packet.Command(CommandType.DENY_EXTENSION)
        return executeCommand(ip, port, deviceId, "/unlock-response", command).response
    }

    suspend fun stopBreak(ip: String, port: Int, deviceId: String? = null): Packet.Response? {
        val command = Packet.Command(CommandType.STOP_BREAK)
        return executeCommand(ip, port, deviceId, "/unlock-response", command).response
    }

    suspend fun resetPin(ip: String, port: Int, deviceId: String? = null): Packet.Response? {
        val command = Packet.Command(CommandType.RESET_PIN)
        return executeCommand(ip, port, deviceId, "/reset-pin", command).response
    }

    suspend fun updateDeviceName(ip: String, port: Int, deviceId: String? = null, newName: String): Packet.Response? {
        val command = Packet.Command(CommandType.UPDATE_DEVICE_NAME, deviceName = newName)
        return executeCommand(ip, port, deviceId, "/device-name", command).response
    }

    suspend fun setLock(ip: String, port: Int, deviceId: String? = null, locked: Boolean): Packet.Response? {
        val type = if (locked) CommandType.LOCK_DEVICE else CommandType.UNLOCK_DEVICE
        val command = Packet.Command(commandType = type)
        return executeCommand(ip, port, deviceId, "/lock", command).response
    }

    suspend fun setAppIconVisibility(ip: String, port: Int, deviceId: String? = null, visible: Boolean): Packet.Response? {
        val type = if (visible) CommandType.UNHIDE_APP else CommandType.HIDE_APP
        val command = Packet.Command(commandType = type)
        val path = if (visible) "/unhide" else "/hide"
        return executeCommand(ip, port, deviceId, path, command).response
    }
    
    suspend fun setAppCategory(ip: String, port: Int, deviceId: String? = null, packageName: String, category: AppCategory): Packet.Response? {
        val command = Packet.Command(CommandType.SET_APP_CATEGORY, packageName = packageName, category = category)
        return executeCommand(ip, port, deviceId, "/device-name", command).response // Server handles polymorphic commands here
    }

    suspend fun setAppTimer(ip: String, port: Int, deviceId: String? = null, packageName: String, durationMs: Long): Packet.Response? {
        val command = Packet.Command(CommandType.SET_APP_TIMER, packageName = packageName, timerDurationMs = durationMs)
        return executeCommand(ip, port, deviceId, "/device-name", command).response
    }

    suspend fun setCategoryTimer(ip: String, port: Int, deviceId: String? = null, category: AppCategory, durationMs: Long): Packet.Response? {
        val command = Packet.Command(CommandType.SET_CATEGORY_TIMER, category = category, timerDurationMs = durationMs)
        return executeCommand(ip, port, deviceId, "/device-name", command).response
    }

    suspend fun setLanguage(ip: String, port: Int, deviceId: String? = null, languageCode: String): Packet.Response? {
        val command = Packet.Command(CommandType.SET_LANGUAGE, languageCode = languageCode)
        return executeCommand(ip, port, deviceId, "/device-name", command).response
    }

    suspend fun sendDeviceOwnerCommand(
        ip: String,
        port: Int,
        deviceId: String? = null,
        command: Packet.Command
    ): Packet.Response? {
        return executeCommand(ip, port, deviceId, "/device-owner", command).response
    }

    /**
     * Pushes a downloaded child APK to a paired child over LAN (POST /update).
     * The child installs it silently when it is Device Owner, otherwise Android
     * shows the install-confirmation screen on the child device.
     * Returns true when the child accepted the APK for install.
     */
    suspend fun uploadChildApk(
        ip: String,
        port: Int,
        deviceId: String?,
        apkFile: java.io.File
    ): Boolean = withContext(Dispatchers.IO) {
        if (!apkFile.exists() || apkFile.length() < 1_000_000) return@withContext false
        // APK push only works over direct LAN (binary body is too big for BT/relay tiers).
        if (ip.isBlank() || ip == "0.0.0.0") return@withContext false
        try {
            val token = deviceId?.let { pairTokens[it] }
            val response: Packet.Response? = withTimeoutOrNull(180_000) {
                client.request {
                    method = HttpMethod.Post
                    url {
                        protocol = URLProtocol.HTTP
                        host = ip
                        this.port = port
                        path("update")
                    }
                    if (token != null) header("X-Pair-Token", token)
                    header("Content-Type", "application/vnd.android.package-archive")
                    setBody(apkFile.readBytes())
                }.body()
            }
            response?.success == true
        } catch (e: Exception) {
            Log.w("DeviceClient", "APK upload failed for $ip:$port: ${e.message}")
            false
        }
    }
}


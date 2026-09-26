package com.parentalguard.child.network

import android.content.Context
import android.util.Log
import com.parentalguard.common.network.CloudConfig
import com.parentalguard.common.network.Packet
import com.parentalguard.child.utils.DeviceUtils
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class RegisterRequest(
    val deviceId: String,
    val deviceType: String,
    val parentId: String? = null
)

@Serializable
data class RegisterResponse(
    val token: String
)

@Serializable
data class RelayMessage(
    val targetDeviceId: String? = null,
    val fromDeviceId: String? = null,
    val type: String,
    val payload: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class CloudRelayClient(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets)
    }

    private var session: DefaultClientWebSocketSession? = null
    private var job: Job? = null
    private val deviceId = DeviceUtils.getDeviceId(context)
    private val parentId: String?
        get() = context.getSharedPreferences("relay_prefs", Context.MODE_PRIVATE).getString("parent_id", null)

    fun start() {
        if (job?.isActive == true) return
        job = CoroutineScope(Dispatchers.IO).launch {
            var currentDelay = 5000L
            while (isActive) {
                try {
                    connect()
                    // If we return here, it means connection closed cleanly or after a successful session
                    // Reset backoff only if we had a successful long-lived connection, 
                    // but for now, resetting to base delay is fine as we are reconnecting.
                    currentDelay = 5000L
                } catch (e: java.nio.channels.UnresolvedAddressException) {
                    Log.e("CloudRelayClient", "DNS Resolution failed. Retrying in ${currentDelay / 1000}s")
                    delay(currentDelay)
                    currentDelay = (currentDelay * 2).coerceAtMost(60000L)
                } catch (e: Exception) {
                    Log.e("CloudRelayClient", "Connection error: ${e.message}. Retrying in ${currentDelay / 1000}s", e)
                    delay(currentDelay)
                    currentDelay = (currentDelay * 2).coerceAtMost(60000L)
                }
            }
        }
    }

    private suspend fun connect() {
        // 1. Get Token (include the known parent ID so the relay can map us)
        // This will throw if it fails, which is what we want for the retry logic in start()
        val knownParentId = parentId
        val response = client.post(CloudConfig.BASE_URL + CloudConfig.ENDPOINT_REGISTER) {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(deviceId, "child", knownParentId))
        }.body<RegisterResponse>()

        val token = response.token
        Log.i("CloudRelayClient", "Cloud Token obtained. Connecting to WebSocket...")

        // 2. Connect WebSocket
        client.webSocket(CloudConfig.WS_URL + "?token=$token") {
            session = this
            Log.i("CloudRelayClient", "Connected to Cloud Relay")
            
            // Listen for messages
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    handleIncomingMessage(frame.readText())
                }
            }
        }
        // WebSocket session closed
        Log.i("CloudRelayClient", "Cloud Relay Session disconnected")
    }
    private suspend fun handleIncomingMessage(text: String) {
        try {
            val relayWrapper = json.decodeFromString<RelayMessage>(text)
            if (relayWrapper.type == "COMMAND" && relayWrapper.payload != null) {
                val packet = json.decodeFromString<Packet>(relayWrapper.payload)
                if (packet is Packet.Command) {
                    Log.i("CloudRelayClient", "Received Command: ${packet.commandType}")
                    val dispatched = dispatchCommand(packet)
                    // Echo the requestId back so the parent can correlate this response
                    val response = if (packet.requestId != null) dispatched.copy(requestId = packet.requestId) else dispatched
                    
                    // Send response back to the sender
                    sendToRelay(RelayMessage(
                        targetDeviceId = relayWrapper.fromDeviceId,
                        type = "RESPONSE",
                        payload = json.encodeToString<Packet>(response)
                    ))
                }
            } else {
                Log.d("CloudRelayClient", "Received relay message: type=${relayWrapper.type}, hasPayload=${relayWrapper.payload != null}")
            }
        } catch (e: Exception) {
            Log.e("CloudRelayClient", "Error handling message", e)
        }
    }

    private suspend fun dispatchCommand(command: Packet.Command): Packet.Response {
        return CommandDispatcher.dispatch(context, command)
    }

    private suspend fun sendToRelay(message: RelayMessage) {
        try {
            session?.send(Frame.Text(json.encodeToString(message)))
        } catch (e: Exception) {
            Log.e("CloudRelayClient", "Failed to send to relay", e)
        }
    }

    suspend fun sendEvent(event: Packet.Event) {
        val target = parentId
        if (target == null) {
            // The parent never synced its relay ID (pairing-time sync missed).
            // Without a target the relay cannot route this — it is dropped.
            // The parent re-syncs its ID on every successful refresh, which heals this.
            Log.w("CloudRelayClient", "Dropping ${event.eventType}: unknown relay parent ID")
            return
        }
        val payload = json.encodeToString<Packet>(event)
        val message = RelayMessage(
            targetDeviceId = target,
            type = "EVENT",
            payload = payload
        )
        sendToRelay(message)
    }

    fun stop() {
        job?.cancel()
        job = null
        session = null
    }
}

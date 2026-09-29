package com.parentalguard.common.network

import kotlinx.serialization.json.Json
import com.parentalguard.common.model.CapabilityState
import com.parentalguard.common.model.BlockingScreenStyle
import com.parentalguard.common.model.DeviceOwnerCapability
import com.parentalguard.common.model.DeviceOwnerCapabilities
import com.parentalguard.common.model.DeviceStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PacketSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun commandRoundTrip_preservesRequestId() {
        val command = Packet.Command(
            commandType = CommandType.LOCK_DEVICE,
            requestId = "req-1234"
        )

        val encoded = json.encodeToString(Packet.serializer(), command)
        val decoded = json.decodeFromString(Packet.serializer(), encoded) as Packet.Command

        assertEquals(CommandType.LOCK_DEVICE, decoded.commandType)
        assertEquals("req-1234", decoded.requestId)
    }

    @Test
    fun responseRoundTrip_echoesRequestId() {
        val response = Packet.Response(success = true, message = "Locked", requestId = "req-1234")

        val encoded = json.encodeToString(Packet.serializer(), response)
        val decoded = json.decodeFromString(Packet.serializer(), encoded) as Packet.Response

        assertTrue(decoded.success)
        assertEquals("req-1234", decoded.requestId)
        assertEquals("Locked", decoded.message)
    }

    @Test
    fun eventRoundTrip_preservesFields() {
        val event = Packet.Event(
            eventType = EventType.UNLOCK_REQUESTED,
            deviceId = "child-42",
            requestType = "DEVICE",
            payload = "Please unlock"
        )

        val encoded = json.encodeToString(Packet.serializer(), event)
        val decoded = json.decodeFromString(Packet.serializer(), encoded) as Packet.Event

        assertEquals(EventType.UNLOCK_REQUESTED, decoded.eventType)
        assertEquals("child-42", decoded.deviceId)
        assertEquals("DEVICE", decoded.requestType)
        assertEquals("Please unlock", decoded.payload)
    }

    @Test
    fun commandType_serializesToEnumName() {
        val command = Packet.Command(commandType = CommandType.SET_RELAY_PARENT_ID, relayParentId = "relay-7")

        val encoded = json.encodeToString(Packet.serializer(), command)

        assertTrue(encoded.contains("\"commandType\":\"SET_RELAY_PARENT_ID\""))
        assertTrue(encoded.contains("\"relayParentId\":\"relay-7\""))
    }

    @Test
    fun decode_toleratesUnknownKeys() {
        val jsonWithUnknown = """
            {"type":"com.parentalguard.common.network.Packet.Command","commandType":"PING","requestId":"req-1","unknownField":42}
        """.trimIndent()

        val decoded = json.decodeFromString(Packet.serializer(), jsonWithUnknown) as Packet.Command

        assertEquals(CommandType.PING, decoded.commandType)
        assertEquals("req-1", decoded.requestId)
    }

    @Test
    fun optionalFields_defaultToNull() {
        val command = Packet.Command(commandType = CommandType.PING)

        val encoded = json.encodeToString(Packet.serializer(), command)
        val decoded = json.decodeFromString(Packet.serializer(), encoded) as Packet.Command

        assertNull(decoded.requestId)
        assertNull(decoded.languageCode)
    }

    @Test
    fun deviceOwnerCommand_preservesGatePayload() {
        val command = Packet.Command(
            commandType = CommandType.DEVICE_OWNER_SET_WIFI_ENABLED,
            enabled = false
        )

        val encoded = json.encodeToString(Packet.serializer(), command)
        val decoded = json.decodeFromString(Packet.serializer(), encoded) as Packet.Command

        assertEquals(CommandType.DEVICE_OWNER_SET_WIFI_ENABLED, decoded.commandType)
        assertEquals(false, decoded.enabled)
    }

    @Test
    fun missingDeviceOwnerCapability_defaultsToUnknown() {
        val capabilities = DeviceOwnerCapabilities()

        assertEquals(
            CapabilityState.UNKNOWN,
            capabilities.stateFor(DeviceOwnerCapability.WIFI_TOGGLE)
        )
        assertEquals(
            CapabilityState.UNKNOWN,
            capabilities.stateFor(DeviceOwnerCapability.APP_SUSPENSION)
        )
    }

    @Test
    fun deviceOwnerCapabilities_roundTripPreservesStates() {
        val capabilities = DeviceOwnerCapabilities(
            wifiToggle = CapabilityState.AVAILABLE,
            appSuspension = CapabilityState.UNAVAILABLE
        )

        val encoded = json.encodeToString(DeviceOwnerCapabilities.serializer(), capabilities)
        val decoded = json.decodeFromString(DeviceOwnerCapabilities.serializer(), encoded)

        assertEquals(CapabilityState.AVAILABLE, decoded.wifiToggle)
        assertEquals(CapabilityState.UNAVAILABLE, decoded.appSuspension)
    }

    @Test
    fun blockingScreenStyleCommand_roundTripPreservesStyle() {
        val command = Packet.Command(
            commandType = CommandType.SET_BLOCKING_SCREEN_STYLE,
            blockingScreenStyle = BlockingScreenStyle.QUIET_FOCUS
        )

        val encoded = json.encodeToString(Packet.serializer(), command)
        val decoded = json.decodeFromString(Packet.serializer(), encoded) as Packet.Command

        assertEquals(BlockingScreenStyle.QUIET_FOCUS, decoded.blockingScreenStyle)
    }

    @Test
    fun responseRoundTrip_preservesBlockingScreenStyle() {
        val response = Packet.Response(
            success = true,
            stats = DeviceStats(
                batteryLevel = 80,
                lastSeenTimestamp = 123L,
                usageLogs = emptyList(),
                blockingScreenStyle = BlockingScreenStyle.BLACKOUT
            )
        )

        val encoded = json.encodeToString(Packet.serializer(), response)
        val decoded = json.decodeFromString(Packet.serializer(), encoded) as Packet.Response

        assertEquals(BlockingScreenStyle.BLACKOUT, decoded.stats?.blockingScreenStyle)
    }

    @Test
    fun deviceStatsWithoutBlockingStyle_defaultsToLegacyNull() {
        val jsonWithoutStyle = """
            {"batteryLevel":80,"lastSeenTimestamp":123,"usageLogs":[]}
        """.trimIndent()

        val decoded = json.decodeFromString(DeviceStats.serializer(), jsonWithoutStyle)

        assertNull(decoded.blockingScreenStyle)
    }

    @Test
    fun deviceStatsWithoutOwnerState_defaultsToEmpty() {
        // Payloads from older child builds lack the suspend/protect sets.
        val legacyJson = """
            {"batteryLevel":80,"lastSeenTimestamp":123,"usageLogs":[]}
        """.trimIndent()

        val decoded = json.decodeFromString(DeviceStats.serializer(), legacyJson)

        assertTrue(decoded.deviceOwnerSuspended.isEmpty())
        assertTrue(decoded.deviceOwnerUninstallProtected.isEmpty())
    }

    @Test
    fun deviceStatsOwnerState_roundTripPreservesSets() {
        val stats = DeviceStats(
            batteryLevel = 80,
            lastSeenTimestamp = 123L,
            usageLogs = emptyList(),
            deviceOwnerSuspended = setOf("com.example.game"),
            deviceOwnerUninstallProtected = setOf("com.example.video")
        )

        val encoded = json.encodeToString(DeviceStats.serializer(), stats)
        val decoded = json.decodeFromString(DeviceStats.serializer(), encoded)

        assertEquals(setOf("com.example.game"), decoded.deviceOwnerSuspended)
        assertEquals(setOf("com.example.video"), decoded.deviceOwnerUninstallProtected)
    }
}

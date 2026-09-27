package com.parentalguard.parent.network

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.parentalguard.common.network.BluetoothConfig
import com.parentalguard.common.network.Packet
import com.parentalguard.parent.viewmodel.ChildDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Classic Bluetooth RFCOMM (SPP) client. Discovers child devices that advertise
 * themselves as "PG_Child_<deviceId>" and tunnels the [Packet] JSON protocol
 * over the socket (same framing as the child's BluetoothCommandServer).
 */
class BluetoothClient(private val context: Context?) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun isBluetoothAvailable(): Boolean {
        return try {
            BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
        } catch (e: Exception) {
            false
        }
    }

    private fun hasScanPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun hasConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context!!, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    /**
     * Emits (deviceId, BluetoothDevice) pairs for child devices.
     * Starts with already-bonded devices, then runs discovery for unpaired
     * devices that expose the PG_Child_<id> name.
     */
    fun discoverChildren(): Flow<Pair<String, BluetoothDevice>> = callbackFlow {
        val ctx = context
        if (ctx == null || !isBluetoothAvailable() || !hasScanPermission() || !hasConnectPermission()) {
            Log.w(TAG, "Bluetooth unavailable or missing scan permission")
            close()
            return@callbackFlow
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()

        // 1. Bonded devices (no pairing prompt needed to read the address)
        runCatching { adapter?.bondedDevices }.getOrNull()?.forEach { device ->
            extractDeviceId(device)?.let { trySend(it to device) }
        }

        // 2. Unpaired devices currently in range
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                        if (device != null) {
                            extractDeviceId(device)?.let { trySend(it to device) }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> close()
                }
            }
        }
        val filter = android.content.IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            ctx.registerReceiver(receiver, filter)
        }

        try {
            runCatching { adapter?.cancelDiscovery() }
            val started = runCatching { adapter?.startDiscovery() }.getOrNull() ?: false
            Log.i(TAG, "Bluetooth discovery started: $started")
            if (!started) close()
        } catch (e: Exception) {
            Log.w(TAG, "Discovery error: ${e.message}")
            close(e)
        }

        awaitClose {
            runCatching { adapter?.cancelDiscovery() }
            runCatching { ctx.unregisterReceiver(receiver) }
        }
    }

    private fun extractDeviceId(device: BluetoothDevice): String? {
        val name = runCatching { device.name }.getOrNull() ?: return null
        val deviceId = BluetoothConfig.deviceIdFromBluetoothName(name)
        Log.d(TAG, "Bluetooth device found: name=$name, matchedChild=${deviceId != null}")
        return deviceId
    }

    /**
     * Bonds with a child device (one-time OS-level pairing). After this, the
     * child stays reachable by MAC even when it is not discoverable, so the
     * tablet never needs to ask "make visible" again. Shows the standard
     * system pairing dialog once — accept it on both devices during setup.
     * Returns true when already bonded or the bond process started.
     */
    fun bondDevice(bluetoothMac: String): Boolean {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            if (!adapter.isEnabled || !hasConnectPermission()) return false
            val remote = adapter.getRemoteDevice(bluetoothMac)
            val state = runCatching { remote.bondState }.getOrNull()
            if (state == BluetoothDevice.BOND_BONDED) return true
            runCatching { remote.createBond() }.getOrDefault(false)
        } catch (e: Exception) {
            Log.w(TAG, "Bond failed for $bluetoothMac: ${e.message}")
            false
        }
    }

    /**
     * Connects to [device] over RFCOMM and exchanges a single command/response pair.
     */
    suspend fun executeCommand(device: ChildDevice, command: Packet.Command): Packet.Response? {
        val mac = device.bluetoothMac ?: return null
        return executeCommand(mac, command, device.pairToken)
    }

    suspend fun executeCommand(
        bluetoothMac: String,
        command: Packet.Command,
        pairToken: String? = null
    ): Packet.Response? {
        return withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                try {
                    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext null
                    if (!adapter.isEnabled || !hasConnectPermission()) return@withContext null

                    val remote = adapter.getRemoteDevice(bluetoothMac)
                    val socket: BluetoothSocket = remote.createRfcommSocketToServiceRecord(BluetoothConfig.SPP_UUID)

                    try {
                        // Discovery monopolizes the radio and commonly causes RFCOMM
                        // connects to time out. Stop it before opening the socket.
                        runCatching { adapter.cancelDiscovery() }
                        socket.connect()
                        exchange(socket.inputStream, socket.outputStream, command, pairToken)
                    } finally {
                        runCatching { socket.close() }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Bluetooth command failed for $bluetoothMac: ${e.message}")
                    null
                }
            }
        }
    }

    private fun exchange(
        input: InputStream,
        output: OutputStream,
        command: Packet.Command,
        pairToken: String?
    ): Packet.Response? {
        val authenticatedCommand = command.copy(authToken = pairToken)
        val payload = json.encodeToString<Packet>(authenticatedCommand).toByteArray(Charsets.UTF_8)
        output.write(ByteBuffer.allocate(4).putInt(payload.size).array())
        output.write(payload)
        output.flush()

        val lenBytes = readFully(input, 4) ?: return null
        val len = ByteBuffer.wrap(lenBytes).int
        if (len <= 0 || len > BluetoothConfig.MAX_PACKET_SIZE) return null
        val body = readFully(input, len) ?: return null
        return runCatching {
            json.decodeFromString<Packet>(String(body, Charsets.UTF_8)) as? Packet.Response
        }.getOrNull()
    }

    private fun readFully(input: InputStream, count: Int): ByteArray? {
        val buffer = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = input.read(buffer, offset, count - offset)
            if (read < 0) return null
            offset += read
        }
        return buffer
    }

    companion object {
        private const val TAG = "BluetoothClient"
        private const val CONNECT_TIMEOUT_MS = 6000L
    }
}

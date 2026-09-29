package com.parentalguard.parent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.parentalguard.common.network.EventType
import com.parentalguard.common.network.Packet
import com.parentalguard.parent.R
import com.parentalguard.parent.network.DeviceClient
import com.parentalguard.parent.ui.components.NotificationHelper
import com.parentalguard.parent.data.DeviceRepository
import com.parentalguard.parent.viewmodel.ChildDevice
import kotlinx.coroutines.*
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

class NotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var deviceClient: DeviceClient
    private val observationJobs = ConcurrentHashMap<String, Job>()
    private val activeJobIps = ConcurrentHashMap<String, String>() // deviceId -> last used IP
    
    // In-memory cache of known devices (deviceId -> Device)
    private val knownDevices = ConcurrentHashMap<String, ChildDevice>()
    private lateinit var deviceRepository: DeviceRepository

    companion object {
        private const val CHANNEL_ID = "parent_monitor_service"
        private const val ACTION_START_MONITORING = "com.parentalguard.parent.action.START_MONITORING"
        private const val ACTION_STOP_MONITORING = "com.parentalguard.parent.action.STOP_MONITORING"
        private const val EXTRA_IP = "extra_ip"
        private const val EXTRA_PORT = "extra_port"
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_DEVICE_ID = "extra_device_id"
        private const val EXTRA_PAIR_TOKEN = "extra_pair_token"
        private const val EXTRA_BT_MAC = "extra_bt_mac"

        fun startMonitoring(context: Context, device: ChildDevice) {
            val intent = Intent(context, NotificationService::class.java).apply {
                action = ACTION_START_MONITORING
                putExtra(EXTRA_IP, device.ip.hostAddress)
                putExtra(EXTRA_PORT, device.port)
                putExtra(EXTRA_NAME, device.customName)
                putExtra(EXTRA_DEVICE_ID, device.deviceId)
                putExtra(EXTRA_PAIR_TOKEN, device.pairToken)
                putExtra(EXTRA_BT_MAC, device.bluetoothMac)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopMonitoring(context: Context, deviceId: String) {
            val intent = Intent(context, NotificationService::class.java).apply {
                action = ACTION_STOP_MONITORING
                putExtra(EXTRA_DEVICE_ID, deviceId)
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Foreground promotion FIRST: Android kills the process with
        // ForegroundServiceDidNotStartInTimeException if startForeground()
        // isn't reached within seconds of startForegroundService(). Cold
        // start (especially right after an update, while dexopt hogs the
        // main thread) can stall everything below past that deadline.
        createNotificationChannel()
        startForeground(1, createNotification())
        deviceClient = DeviceClient(this)
        deviceRepository = DeviceRepository(this)
        loadSavedDevices()
    }

    private fun loadSavedDevices() {
        val saved = deviceRepository.loadDevices()
        saved.forEach { device ->
            knownDevices[device.deviceId] = device
            // The service owns a separate DeviceClient: register credentials
            // here too or every request (events socket included) is rejected
            // as Unauthorized on paired children.
            device.pairToken?.let { deviceClient.registerPairToken(device.deviceId, it) }
            device.bluetoothMac?.let { deviceClient.registerBluetoothMac(device.deviceId, it) }
            observeDeviceEvents(device) // Start monitoring on service startup
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITORING) {
            val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)
            if (deviceId != null) stopObserving(deviceId)
        } else if (intent?.action == ACTION_START_MONITORING) {
            val ip = intent.getStringExtra(EXTRA_IP)
            val port = intent.getIntExtra(EXTRA_PORT, 0)
            val name = intent.getStringExtra(EXTRA_NAME) ?: "Unknown"
            val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)
            val pairToken = intent.getStringExtra(EXTRA_PAIR_TOKEN)
            val btMac = intent.getStringExtra(EXTRA_BT_MAC)

            if (ip != null && port != 0 && deviceId != null) {
                 try {
                     val device = ChildDevice(
                         deviceId = deviceId, name = name, ip = InetAddress.getByName(ip), port = port,
                         customName = name, bluetoothMac = btMac, pairToken = pairToken
                     )
                     knownDevices[deviceId] = device // Update or add
                     pairToken?.let { deviceClient.registerPairToken(deviceId, it) }
                     btMac?.let { deviceClient.registerBluetoothMac(deviceId, it) }
                     observeDeviceEvents(device)
                 } catch (e: Exception) {
                     Log.e("NotificationService", "Error adding device", e)
                 }
            }
        }
        return START_STICKY
    }

    private fun stopObserving(deviceId: String) {
        observationJobs.remove(deviceId)?.cancel()
        activeJobIps.remove(deviceId)
        knownDevices.remove(deviceId)
        Log.i("NotificationService", "Stopped observation for $deviceId")
    }

    private fun observeDeviceEvents(device: ChildDevice) {
        val deviceId = device.deviceId
        val deviceIp = device.ip.hostAddress ?: ""
        
        val existingJob = observationJobs[deviceId]
        val activeIp = activeJobIps[deviceId]
        
        // If IP is same as what we are already tracking, no need to restart
        if (existingJob?.isActive == true && activeIp == deviceIp) {
            return
        }
        
        // Cancel old job if it exists (e.g. IP changed)
        existingJob?.cancel()
        knownDevices[deviceId] = device
        activeJobIps[deviceId] = deviceIp

        Log.i("NotificationService", "Starting observation for ${device.customName} ($deviceId) at $deviceIp")
        
        observationJobs[deviceId] = serviceScope.launch {
            var retryDelay = 5000L
            while (isActive) {
                try {
                    deviceClient.observeEvents(deviceIp, device.port, deviceId).collect { event ->
                        handleEvent(device, event)
                        retryDelay = 5000L
                    }
                } catch (e: Exception) {
                    if (!isActive) break
                    Log.d("NotificationService", "Observation connection failed for $deviceId ($deviceIp). Retrying in ${retryDelay/1000}s...")
                    delay(retryDelay)
                    retryDelay = (retryDelay * 1.5).toLong().coerceAtMost(60000L)
                }
            }
        }
    }

    private fun handleEvent(device: ChildDevice, event: Packet.Event) {
        // Resolve the latest name from the repository if possible
        val deviceId = device.deviceId
        val customName = deviceRepository.getDeviceName(device.ip.hostAddress ?: "", deviceId) ?: device.customName
        
        Log.i("NotificationService", "Received event: ${event.eventType} from $customName")
        if (event.eventType == EventType.UNLOCK_REQUESTED) {
            NotificationHelper.showUnlockRequestNotification(
                context = this,
                deviceId = deviceId,
                deviceName = customName, 
                requestType = event.requestType ?: "DEVICE",
                appPackageName = event.appPackageName,
                appName = event.appName
            )
        } else if (event.eventType == EventType.EXTENSION_REQUESTED) {
            NotificationHelper.showExtensionRequestNotification(
                context = this,
                deviceId = deviceId,
                deviceName = customName
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Parental Monitor Service",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kid Guard Active")
            .setContentText("Listening for child device events...")
            .setSmallIcon(R.drawable.ic_parent_logo) // Ensure this exists, or use default
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

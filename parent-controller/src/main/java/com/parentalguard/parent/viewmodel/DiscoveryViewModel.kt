package com.parentalguard.parent.viewmodel

import android.app.Application
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.parentalguard.common.utils.DiscoveryUtils
import com.parentalguard.parent.data.DeviceRepository
import com.parentalguard.parent.data.ReportsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import com.parentalguard.parent.network.DeviceClient
import com.parentalguard.parent.ui.components.NotificationHelper
import com.parentalguard.common.network.*

data class ChildDevice(
    val deviceId: String,      // Unique ID for cloud relay
    val name: String,          // Original device name
    var ip: InetAddress,       // Mutable to allow updating IP for same deviceId
    val port: Int,
    var customName: String = name,  // User-defined custom name
    var reportedName: String? = null, // Latest name reported by the child
    var bluetoothName: String? = null, // BT adapter name advertised by the child (PG_Child_<id>)
    var bluetoothMac: String? = null,  // Remote BT device address, resolved via discovery
    var pairToken: String? = null      // Pairing token from the child's QR (authenticates LAN commands)
)

/** A child device found via Bluetooth discovery, ready to be added to the circle. */
data class BluetoothDeviceCandidate(
    val deviceId: String,
    val name: String,
    val mac: String,
    val alreadyPaired: Boolean = false
)

enum class ConnectionType {
    LOCAL,      // Direct WiFi connection
    BLUETOOTH,  // Classic RFCOMM (SPP) connection
    CLOUD,      // Via cloud relay server  
    UNKNOWN     // Status not yet determined
}

data class DeviceStatusSummary(
    val isOnline: Boolean = false,
    val isLocked: Boolean = false,
    val batteryLevel: Int = 0,
    val activeRulesCount: Int = 0,
    val todayScreenTimeMs: Long = 0,
    val lastUpdate: Long = 0,
    val connectionType: ConnectionType = ConnectionType.UNKNOWN,
    val reportedDeviceName: String? = null
)

class DiscoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val _devices = MutableStateFlow<List<ChildDevice>>(emptyList())
    val devices: StateFlow<List<ChildDevice>> = _devices.asStateFlow()

    private val _deviceStatuses = MutableStateFlow<Map<String, DeviceStatusSummary>>(emptyMap()) // Keyed by deviceId
    val deviceStatuses: StateFlow<Map<String, DeviceStatusSummary>> = _deviceStatuses.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isBluetoothScanning = MutableStateFlow(false)
    val isBluetoothScanning: StateFlow<Boolean> = _isBluetoothScanning.asStateFlow()

    private val _bluetoothCandidates = MutableStateFlow<List<BluetoothDeviceCandidate>>(emptyList())
    val bluetoothCandidates: StateFlow<List<BluetoothDeviceCandidate>> = _bluetoothCandidates.asStateFlow()

    private var bluetoothScanJob: Job? = null
    private var autoBluetoothDiscoveryJob: Job? = null

    private val deviceClient = DeviceClient(application)
    private val bluetoothClient = com.parentalguard.parent.network.BluetoothClient(application)
    private val observationJobs = ConcurrentHashMap<String, Job>()
    
    // Repositories for persistence
    private val deviceRepository = DeviceRepository(application)
    private val reportsRepository = ReportsRepository(application)

    init {
        // Load saved devices on startup
        loadSavedDevices()
        // Start network discovery automatically
        startDiscovery()
        // Discover Bluetooth children to fill in their MACs for the BT fallback tier
        autoBluetoothDiscoveryJob = startBluetoothDiscoveryInternal()
    }

    private fun loadSavedDevices() {
        viewModelScope.launch {
            try {
                val savedDevices = withContext(Dispatchers.IO) {
                    deviceRepository.loadDevices()
                }
                
                if (savedDevices.isNotEmpty()) {
                    _devices.value = savedDevices
                    Log.i("DiscoveryViewModel", "Loaded ${savedDevices.size} saved devices")
                    
                    // Restore pairing tokens so LAN commands remain authenticated after restart
                    savedDevices.forEach { device ->
                        device.pairToken?.let { deviceClient.registerPairToken(device.deviceId, it) }
                        device.bluetoothMac?.let { deviceClient.registerBluetoothMac(device.deviceId, it) }
                    }
                    
                    // Start monitoring all saved devices
                    savedDevices.forEach { device ->
                        observeDeviceEvents(device)
                    }
                    
                    // Initial sync of statuses
                    refreshDevices()
                } else {
                    Log.i("DiscoveryViewModel", "No saved devices found")
                }
            } catch (e: Exception) {
                Log.e("DiscoveryViewModel", "Error loading saved devices", e)
            }
        }
    }

    private fun saveDevices() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    deviceRepository.saveDevices(_devices.value)
                }
                Log.i("DiscoveryViewModel", "Saved ${_devices.value.size} devices")
            } catch (e: Exception) {
                Log.e("DiscoveryViewModel", "Error saving devices", e)
            }
        }
    }

    fun addManualDevice(qrCode: String, defaultPort: Int, onDeviceAdded: ((ChildDevice) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val parts = qrCode.split("|")
                val deviceId: String
                val hostPort: String
                val deviceName: String
                val bluetoothName: String?
                val pairToken: String?
                when {
                    parts.size >= 5 -> {
                        deviceId = parts[0]; hostPort = parts[1]; deviceName = parts[2]
                        bluetoothName = parts[3]; pairToken = parts[4]
                    }
                    parts.size == 4 -> {
                        deviceId = parts[0]; hostPort = parts[1]; deviceName = parts[2]
                        bluetoothName = parts[3]; pairToken = null
                    }
                    parts.size == 3 -> {
                        deviceId = parts[0]; hostPort = parts[1]; deviceName = parts[2]
                        bluetoothName = null; pairToken = null
                    }
                    else -> {
                        // Fallback for old IP-only QR codes
                        deviceId = "legacy_${qrCode.replace(".", "_")}"
                        hostPort = qrCode
                        deviceName = "Legacy Device"
                        bluetoothName = null
                        pairToken = null
                    }
                }

                val hostParts = hostPort.split(":")
                val ip = hostParts[0]
                val port = hostParts.getOrNull(1)?.toIntOrNull() ?: defaultPort
                
                val host = withContext(Dispatchers.IO) { InetAddress.getByName(ip) }
                
                val existingCustomName = withContext(Dispatchers.IO) {
                    deviceRepository.getDeviceName(hostAddress = ip, deviceId = deviceId)
                }
                
                val newDevice = ChildDevice(
                    deviceId = deviceId,
                    name = deviceName,
                    ip = host,
                    port = port,
                    customName = existingCustomName ?: deviceName,
                    reportedName = deviceName,
                    bluetoothName = bluetoothName,
                    pairToken = pairToken
                )
                
                if (pairToken != null) deviceClient.registerPairToken(deviceId, pairToken)
                
                val currentList = _devices.value.toMutableList()
                val existingIndex = currentList.indexOfFirst { it.deviceId == newDevice.deviceId }
                
                if (existingIndex == -1) {
                    currentList.add(newDevice)
                    _devices.value = currentList
                    saveDevices()
                    observeDeviceEvents(newDevice)
                } else {
                    // Update IP/Port if they changed (same device, new network)
                    Log.i("DiscoveryViewModel", "Updating existing device ${newDevice.deviceId} with new IP ${newDevice.ip}")
                    currentList[existingIndex].ip = newDevice.ip
                    _devices.value = currentList
                    saveDevices()
                    connectionHealthCache.remove(newDevice.deviceId) // Clear failure cache
                    observeDeviceEvents(currentList[existingIndex]) // Refresh monitoring
                }
                refreshDevices()                // Sync Relay Parent ID immediately while we have local connection
                viewModelScope.launch {
                    try {
                        deviceClient.syncRelayParentId(ip, port, newDevice.deviceId)
                        Log.i("DiscoveryViewModel", "Synced Relay Parent ID to $ip")
                    } catch (e: Exception) {
                        Log.e("DiscoveryViewModel", "Failed to sync relay ID", e)
                    }
                }
                                onDeviceAdded?.invoke(newDevice)
            } catch (e: Exception) {
                Log.e("DiscoveryViewModel", "Failed to add device from QR: $qrCode", e)
            }
        }
    }

    private fun observeDeviceEvents(device: ChildDevice) {
        // Delegate observation to Foreground Service to ensure it runs in background
        com.parentalguard.parent.service.NotificationService.startMonitoring(getApplication(), device)
    }

    /**
     * Scans for child devices advertising the PG_Child_<deviceId> Bluetooth name
     * and records their MAC address so DeviceClient can use BT as a fallback tier.
     */
    private fun startBluetoothDiscoveryInternal(): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            bluetoothClient.discoverChildren().collect { (deviceId, btDevice) ->
                val currentList = _devices.value.toMutableList()
                val index = currentList.indexOfFirst { it.deviceId == deviceId }
                val mac = runCatching { btDevice.address }.getOrNull() ?: return@collect

                deviceClient.registerBluetoothMac(deviceId, mac)

                if (index != -1) {
                    val current = currentList[index]
                    val updated = current.copy(bluetoothMac = mac)
                    if (updated.bluetoothMac != current.bluetoothMac || updated.bluetoothName == null) {
                        currentList[index] = updated.copy(
                            bluetoothName = current.bluetoothName ?: runCatching { btDevice.name }.getOrNull()
                                ?: BluetoothConfig.bluetoothNameFor(deviceId)
                        )
                        _devices.value = currentList
                        saveDevices()
                        Log.i("DiscoveryViewModel", "Linked BT ${mac} to ${current.customName}")
                    }
                }
            }
        }
    }

    /**
     * Manual Bluetooth scan for the "Pair via Bluetooth" flow. Populates
     * [bluetoothCandidates] with nearby children advertising PG_Child_<id>.
     */
    fun startBluetoothScan() {
        if (bluetoothScanJob?.isActive == true) return
        // Pause the auto-discovery while a manual scan owns the adapter, so the
        // ACTION_FOUND results reach this collector (Android allows one discovery).
        autoBluetoothDiscoveryJob?.cancel()
        autoBluetoothDiscoveryJob = null
        _isBluetoothScanning.value = true
        _bluetoothCandidates.value = emptyList()
        bluetoothScanJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                bluetoothClient.discoverChildren().collect { (deviceId, btDevice) ->
                    val mac = runCatching { btDevice.address }.getOrNull() ?: return@collect
                    val name = runCatching { btDevice.name }.getOrNull()
                        ?: BluetoothConfig.bluetoothNameFor(deviceId)
                    val alreadyPaired = _devices.value.any { it.deviceId == deviceId }
                    val candidates = _bluetoothCandidates.value.toMutableList()
                    candidates.removeAll { it.deviceId == deviceId }
                    candidates.add(BluetoothDeviceCandidate(deviceId, name, mac, alreadyPaired))
                    _bluetoothCandidates.value = candidates
                    Log.i("DiscoveryViewModel", "BT scan found ${name} (${mac})")
                }
            } catch (e: Exception) {
                Log.e("DiscoveryViewModel", "Bluetooth scan error", e)
            } finally {
                _isBluetoothScanning.value = false
            }
        }
    }

    fun stopBluetoothScan() {
        bluetoothScanJob?.cancel()
        bluetoothScanJob = null
        _isBluetoothScanning.value = false
        // Resume the background auto-discovery that links MACs for known devices.
        if (autoBluetoothDiscoveryJob?.isActive != true) {
            autoBluetoothDiscoveryJob = startBluetoothDiscoveryInternal()
        }
    }

    /**
     * Adds a device found over Bluetooth to the circle. Fetches the child's
     * friendly name over the RFCOMM link (falling back to the BT name), links
     * the MAC so commands can tunnel over Bluetooth, and persists the device.
     */
    fun connectBluetoothDevice(candidate: BluetoothDeviceCandidate) {
        viewModelScope.launch(Dispatchers.IO) {
            val mac = candidate.mac
            deviceClient.registerBluetoothMac(candidate.deviceId, mac)

            // Verify the link and grab the child's friendly device name.
            // Direct RFCOMM works immediately (token-authenticated); then bond
            // once so the tablet stays reachable forever without having to be
            // discoverable (no more "make visible" prompts on the child).
            var friendlyName = candidate.name
            var handshakeOk = false
            try {
                val statsResponse = bluetoothClient.executeCommand(mac, Packet.Command(CommandType.GET_STATS))
                statsResponse?.stats?.deviceName?.takeIf { it.isNotBlank() }?.let { friendlyName = it }
                handshakeOk = statsResponse != null
            } catch (e: Exception) {
                Log.w("DiscoveryViewModel", "BT handshake failed for ${candidate.deviceId}", e)
            }
            if (handshakeOk) {
                val bonded = bluetoothClient.bondDevice(mac)
                Log.i("DiscoveryViewModel", "Bond with ${candidate.deviceId}: $bonded (accept pairing on both devices once)")
            }
            if (friendlyName.startsWith(BluetoothConfig.SERVICE_NAME_PREFIX)) {
                friendlyName = friendlyName.removePrefix(BluetoothConfig.SERVICE_NAME_PREFIX)
            }

            val currentList = _devices.value.toMutableList()
            val index = currentList.indexOfFirst { it.deviceId == candidate.deviceId }
            if (index != -1) {
                val current = currentList[index]
                val updated = current.copy(
                    bluetoothMac = mac,
                    bluetoothName = current.bluetoothName ?: candidate.name
                )
                currentList[index] = updated
                _devices.value = currentList
                saveDevices()
                observeDeviceEvents(updated)
                Log.i("DiscoveryViewModel", "Linked Bluetooth ${mac} to existing ${updated.customName}")
            } else {
                val placeholderIp = runCatching { InetAddress.getByName("0.0.0.0") }.getOrNull() ?: return@launch
                val newDevice = ChildDevice(
                    deviceId = candidate.deviceId,
                    name = candidate.name,
                    ip = placeholderIp,
                    port = 8080,
                    customName = friendlyName,
                    reportedName = friendlyName,
                    bluetoothName = candidate.name,
                    bluetoothMac = mac
                )
                currentList.add(newDevice)
                _devices.value = currentList
                saveDevices()
                observeDeviceEvents(newDevice)
                Log.i("DiscoveryViewModel", "Added Bluetooth device ${friendlyName} (${candidate.deviceId})")
            }

            _bluetoothCandidates.value = _bluetoothCandidates.value.map {
                if (it.deviceId == candidate.deviceId) it.copy(alreadyPaired = true) else it
            }
        }
    }

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true
        
        viewModelScope.launch {
            DiscoveryUtils.discoverServices(getApplication()).collect { serviceInfo ->
                val sName = serviceInfo.serviceName
                val discoveredDeviceId = if (sName.startsWith(DiscoveryUtils.SERVICE_NAME_PREFIX)) {
                    sName.removePrefix(DiscoveryUtils.SERVICE_NAME_PREFIX)
                } else {
                    @Suppress("DEPRECATION")
                    "nsd_${serviceInfo.host?.hostAddress ?: ""}"
                }

                @Suppress("DEPRECATION")
                val hostAddress = serviceInfo.host?.hostAddress ?: ""
                val existingCustomName = withContext(Dispatchers.IO) {
                    deviceRepository.getDeviceName(hostAddress, discoveredDeviceId)
                }
                
                @Suppress("DEPRECATION")
                val host = serviceInfo.host
                
                val newDevice = ChildDevice(
                    deviceId = discoveredDeviceId,
                    name = serviceInfo.serviceName,
                    ip = host,
                    port = serviceInfo.port,
                    customName = existingCustomName ?: serviceInfo.serviceName
                )
                
                val currentList = _devices.value.toMutableList()
                val existingIndex = currentList.indexOfFirst { it.deviceId == newDevice.deviceId }
                
                if (existingIndex == -1) {
                    currentList.add(newDevice)
                    _devices.value = currentList
                    saveDevices() // Persist discovered device
                    observeDeviceEvents(newDevice)
                } else {
                    // Update IP in case it changed on the local network
                    if (currentList[existingIndex].ip != newDevice.ip) {
                        Log.i("DiscoveryViewModel", "Updating IP for ${newDevice.deviceId} to ${newDevice.ip}")
                        currentList[existingIndex].ip = newDevice.ip
                        _devices.value = currentList
                        saveDevices()
                        connectionHealthCache.remove(newDevice.deviceId) // Clear failure cache on IP update
                        observeDeviceEvents(currentList[existingIndex]) // Update background monitoring IP
                    }
                }
                refreshDevices()
            }
        }
    }
    
    fun updateDeviceName(device: ChildDevice, newName: String) {
        val currentList = _devices.value.toMutableList()
        val index = currentList.indexOfFirst { it.deviceId == device.deviceId }
        
        if (index != -1) {
            val updatedDevice = currentList[index].copy(customName = newName)
            currentList[index] = updatedDevice
            _devices.value = currentList
            saveDevices() // Persist the name change
            observeDeviceEvents(updatedDevice) // Refresh service cache with new name
            Log.i("DiscoveryViewModel", "Updated device name to: $newName")
        }
    }
    
    fun removeDevice(device: ChildDevice) {
        val currentList = _devices.value.toMutableList()
        currentList.removeAll { it.deviceId == device.deviceId }
        _devices.value = currentList
        _deviceStatuses.value = _deviceStatuses.value - device.deviceId
        saveDevices() // Persist the removal
        com.parentalguard.parent.service.NotificationService.stopMonitoring(getApplication(), device.deviceId)
        Log.i("DiscoveryViewModel", "Removed device: ${device.customName}")
    }
    
    fun resetAllDevices() {
        val removed = _devices.value
        _devices.value = emptyList()
        _deviceStatuses.value = emptyMap()
        saveDevices()
        removed.forEach { device ->
            com.parentalguard.parent.service.NotificationService.stopMonitoring(getApplication(), device.deviceId)
        }
        Log.i("DiscoveryViewModel", "Reset all devices")
    }
    
    fun updateDeviceStatus(deviceId: String, isLocked: Boolean) {
        val currentStatuses = _deviceStatuses.value.toMutableMap()
        val existing = currentStatuses[deviceId] ?: DeviceStatusSummary()
        currentStatuses[deviceId] = existing.copy(
            isLocked = isLocked,
            lastUpdate = System.currentTimeMillis()
        )
        _deviceStatuses.value = currentStatuses
    }

    private val connectionHealthCache = ConcurrentHashMap<String, Long>() // deviceId -> lastFailTime

    fun refreshDevices() {
        viewModelScope.launch {
            val devicesList = _devices.value
            val results = devicesList.map { device ->
                viewModelScope.async(Dispatchers.IO) {
                    val status = fetchDeviceStatus(device)
                    device.deviceId to status
                }
            }.awaitAll()

                _deviceStatuses.value = results.toMap()
                applyReportedNames(results.toMap())
        }
    }

    private suspend fun fetchDeviceStatus(device: ChildDevice): DeviceStatusSummary {
        return withContext(Dispatchers.IO) {
            val ip = device.ip.hostAddress ?: ""
            
            // Check cache for recent failures to decide if we should try direct or go straight to relay
            val lastFail = connectionHealthCache[device.deviceId] ?: 0L
            val skipDirect = System.currentTimeMillis() - lastFail < 15_000 // Reduced to 15s

            Log.d("DiscoveryViewModel", "Checking status for ${device.customName} at $ip (skipDirect=$skipDirect)")

            val responseResult = deviceClient.getStatsWithConnectionType(
                ip, 
                device.port, 
                device.deviceId, 
                includeIcons = false,
                skipDirect = skipDirect
            )

            if (responseResult.response != null && responseResult.response.success) {
                val stats = responseResult.response.stats
                val screenTime = stats?.usageLogs?.sumOf { it.totalTimeInForeground } ?: 0L

                // The child drops its cloud events when it doesn't know our relay
                // ID (pairing-time sync may have been missed). Re-sync on every
                // successful contact — idempotent and cheap — so unlock/extension
                // requests keep flowing even after reinstalls or missed pairings.
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        deviceClient.syncRelayParentId(ip, device.port, device.deviceId)
                    }.onFailure {
                        Log.w("DiscoveryViewModel", "Relay ID re-sync failed for ${device.deviceId}")
                    }
                }

                DeviceStatusSummary(
                    isOnline = true,
                    isLocked = stats?.isLocked ?: false,
                    batteryLevel = stats?.batteryLevel ?: 0,
                    activeRulesCount = stats?.activeRules?.size ?: 0,
                    todayScreenTimeMs = screenTime,
                    lastUpdate = System.currentTimeMillis(),
                    connectionType = responseResult.connectionType,
                    reportedDeviceName = stats?.deviceName
                )
            } else {
                if (!skipDirect && responseResult.connectionType == ConnectionType.UNKNOWN) {
                    Log.w("DiscoveryViewModel", "Local & Cloud both failed for ${device.deviceId}")
                    connectionHealthCache[device.deviceId] = System.currentTimeMillis()
                }
                DeviceStatusSummary(isOnline = false, connectionType = responseResult.connectionType)
            }
        }
    }

    private fun applyReportedNames(statuses: Map<String, DeviceStatusSummary>) {
        var changed = false
        val updated = _devices.value.map { device ->
            val reported = statuses[device.deviceId]?.reportedDeviceName
                ?.trim()
                ?.takeIf { it.isNotEmpty() && !it.startsWith(BluetoothConfig.SERVICE_NAME_PREFIX) }
                ?: return@map device

            val current = device.customName.trim()
            val generatedName = current.isBlank() ||
                current == device.name ||
                current == device.deviceId ||
                current.startsWith(BluetoothConfig.SERVICE_NAME_PREFIX) ||
                current.startsWith("Legacy Device") ||
                current == device.reportedName

            val next = device.copy(
                customName = if (generatedName) reported else device.customName,
                reportedName = reported
            )
            if (next != device) changed = true
            next
        }
        if (changed) {
            _devices.value = updated
            saveDevices()
        }
    }
    fun syncLanguage(languageCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _devices.value.forEach { device ->
                device.ip.hostAddress?.let { ip ->
                    launch {
                        val response = deviceClient.setLanguage(ip, device.port, device.deviceId, languageCode)
                        if (response != null && response.success) {
                            Log.i("DiscoveryViewModel", "Synced language $languageCode to ${device.customName}")
                        } else {
                            Log.e("DiscoveryViewModel", "Failed to sync language to ${device.customName}")
                        }
                    }
                }
            }
        }
    }
}

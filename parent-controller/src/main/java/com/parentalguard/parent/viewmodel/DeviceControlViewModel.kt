package com.parentalguard.parent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parentalguard.common.model.*
import com.parentalguard.common.network.CommandType
import com.parentalguard.common.network.Packet
import com.parentalguard.parent.network.DeviceClient
import com.parentalguard.parent.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.parentalguard.parent.data.ReportsRepository

class DeviceControlViewModel(application: Application) : AndroidViewModel(application) {
    private val client = DeviceClient(application)
    private val reportsRepository = ReportsRepository(application)

    private val _usageLogs = MutableStateFlow<List<AppUsageLog>>(emptyList())
    val usageLogs: StateFlow<List<AppUsageLog>> = _usageLogs.asStateFlow()

    private val _usageLogsByDevice = MutableStateFlow<Map<String, List<AppUsageLog>>>(emptyMap())
    val usageLogsByDevice: StateFlow<Map<String, List<AppUsageLog>>> = _usageLogsByDevice.asStateFlow()

    private val _activeRules = MutableStateFlow<List<BlockingRule>>(emptyList())
    val activeRules: StateFlow<List<BlockingRule>> = _activeRules.asStateFlow()

    private val _blockingScreenStyles =
        MutableStateFlow<Map<String, BlockingScreenStyle>>(emptyMap())
    val blockingScreenStyles: StateFlow<Map<String, BlockingScreenStyle>> =
        _blockingScreenStyles.asStateFlow()

    private val _blockingScreenStyleSaves = MutableStateFlow<Set<String>>(emptySet())
    val blockingScreenStyleSaves: StateFlow<Set<String>> = _blockingScreenStyleSaves.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()
    
    private val _categoryLimits = MutableStateFlow<List<CategoryLimit>>(emptyList())
    val categoryLimits: StateFlow<List<CategoryLimit>> = _categoryLimits.asStateFlow()
    
    private val _dailyReport = MutableStateFlow<DailyUsageReport?>(null)
    val dailyReport: StateFlow<DailyUsageReport?> = _dailyReport.asStateFlow()
    
    private val _isDeviceLocked = MutableStateFlow(false)
    val isDeviceLocked: StateFlow<Boolean> = _isDeviceLocked.asStateFlow()

    private val _isAppIconHidden = MutableStateFlow(false)
    val isAppIconHidden: StateFlow<Boolean> = _isAppIconHidden.asStateFlow()

    private val _connectionType = MutableStateFlow(ConnectionType.UNKNOWN)
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    private val _appTimers = MutableStateFlow<Map<String, Long>>(emptyMap())
    val appTimers: StateFlow<Map<String, Long>> = _appTimers.asStateFlow()

    private val _usageLimitMs = MutableStateFlow<Long>(0)
    val usageLimitMs: StateFlow<Long> = _usageLimitMs.asStateFlow()

    private val _breakDurationMs = MutableStateFlow<Long>(0)
    val breakDurationMs: StateFlow<Long> = _breakDurationMs.asStateFlow()

    private val _lockReason = MutableStateFlow<String?>(null)
    val lockReason: StateFlow<String?> = _lockReason.asStateFlow()

    private val _breakWarningMs = MutableStateFlow<Long>(0)
    val breakWarningMs: StateFlow<Long> = _breakWarningMs.asStateFlow()

    private val _educationOnly = MutableStateFlow<Boolean>(false)
    val educationOnly: StateFlow<Boolean> = _educationOnly.asStateFlow()

    private val _allowExtensions = MutableStateFlow<Boolean>(false)
    val allowExtensions: StateFlow<Boolean> = _allowExtensions.asStateFlow()

    private val _appIcons = MutableStateFlow<Map<String, String>>(emptyMap())

    private val _isRefreshingApps = MutableStateFlow(false)
    val isRefreshingApps: StateFlow<Boolean> = _isRefreshingApps.asStateFlow()

    private val _deviceOwnerCapabilities =
        MutableStateFlow<Map<String, DeviceOwnerCapabilities>>(emptyMap())
    val deviceOwnerCapabilities: StateFlow<Map<String, DeviceOwnerCapabilities>> =
        _deviceOwnerCapabilities.asStateFlow()

    private val _childPinSet = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val childPinSet: StateFlow<Map<String, Boolean>> = _childPinSet.asStateFlow()

    private val _pinProtection = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val pinProtection: StateFlow<Map<String, Boolean>> = _pinProtection.asStateFlow()

    private val statsGenerations = mutableMapOf<String, Long>()
    
    fun refresh(device: ChildDevice) {
        fetchStats(device, forceRefresh = true)
    }

    fun fetchStats(device: ChildDevice, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            registerDeviceCredentials(device)
            val generation = synchronized(statsGenerations) {
                val next = (statsGenerations[device.deviceId] ?: 0L) + 1L
                statsGenerations[device.deviceId] = next
                next
            }
            _deviceOwnerCapabilities.value = _deviceOwnerCapabilities.value +
                (device.deviceId to DeviceOwnerCapabilities())
            // Check if we need to fetch icons (if map is empty)
            val includeIcons = _appIcons.value.isEmpty()
            val deviceResponse = client.getStatsWithConnectionType(
                device.ip.hostAddress ?: "", 
                device.port, 
                device.deviceId, 
                includeIcons,
                skipDirect = if (forceRefresh) false else false // Default logic in client is local first
            )
            val response = deviceResponse.response
            
            if (response != null && response.success) {
                val isLatest = synchronized(statsGenerations) {
                    statsGenerations[device.deviceId] == generation
                }
                if (!isLatest) return@launch
                _connectionType.value = deviceResponse.connectionType
                val usageLogs = mergeInstalledApps(
                    response.stats?.usageLogs.orEmpty(),
                    response.stats?.installedApps.orEmpty()
                )
                _usageLogs.value = usageLogs
                _usageLogsByDevice.value = _usageLogsByDevice.value +
                    (device.deviceId to usageLogs)
                _activeRules.value = response.stats?.activeRules ?: emptyList()
                _isDeviceLocked.value = response.stats?.isLocked ?: false
                _isAppIconHidden.value = response.stats?.isIconHidden ?: false
                _appTimers.value = response.stats?.appTimers ?: emptyMap()
                _usageLimitMs.value = response.stats?.usageLimitMs ?: 0L
                _breakDurationMs.value = response.stats?.breakDurationMs ?: 0L
                _breakWarningMs.value = response.stats?.breakWarningMs ?: 0L
                _educationOnly.value = response.stats?.educationOnly ?: false
                _allowExtensions.value = response.stats?.allowExtensions ?: false
                _lockReason.value = response.stats?.lockReason
                _deviceOwnerCapabilities.value = _deviceOwnerCapabilities.value +
                    (device.deviceId to (response.stats?.deviceOwnerCapabilities ?: DeviceOwnerCapabilities()))
                _blockingScreenStyles.value = _blockingScreenStyles.value +
                    (device.deviceId to (response.stats?.blockingScreenStyle ?: BlockingScreenStyle.CURRENT))
                _childPinSet.value = _childPinSet.value +
                    (device.deviceId to (response.stats?.childPinSet ?: false))
                _pinProtection.value = _pinProtection.value +
                    (device.deviceId to (response.stats?.pinProtectionEnabled ?: true))
                
                // Process icons if included
                if (includeIcons) {
                    val newIcons = response.stats?.installedApps?.mapNotNull { app ->
                        if (app.iconBase64 != null) app.packageName to app.iconBase64!! else null
                    }?.toMap() ?: emptyMap()
                    
                    if (newIcons.isNotEmpty()) {
                        _appIcons.value = _appIcons.value + newIcons
                    }
                }
                
                _statusMessage.value = "Stats updated"
            } else {
                val isLatest = synchronized(statsGenerations) {
                    statsGenerations[device.deviceId] == generation
                }
                if (!isLatest) return@launch
                _deviceOwnerCapabilities.value = _deviceOwnerCapabilities.value +
                    (device.deviceId to DeviceOwnerCapabilities())
                _statusMessage.value = "Failed to fetch stats"
            }
        }
    }
    
    // Helper to request icons explicitly (e.g. on pull to refresh if needed)
    fun refreshIcons(device: ChildDevice) {
        if (_isRefreshingApps.value) return
        viewModelScope.launch {
            _isRefreshingApps.value = true
            registerDeviceCredentials(device)
            try {
                val response = client.getStatsWithConnectionType(
                    device.ip.hostAddress ?: "",
                    device.port,
                    device.deviceId,
                    includeIcons = true
                ).response
                if (response != null && response.success) {
                    val stats = response.stats
                    val usageLogs = mergeInstalledApps(
                        stats?.usageLogs.orEmpty(),
                        stats?.installedApps.orEmpty()
                    )
                    _usageLogs.value = usageLogs
                    _usageLogsByDevice.value = _usageLogsByDevice.value +
                        (device.deviceId to usageLogs)

                    val newIcons = stats?.installedApps?.mapNotNull { app ->
                        if (app.iconBase64 != null) app.packageName to app.iconBase64!! else null
                    }?.toMap().orEmpty()
                    if (newIcons.isNotEmpty()) {
                        _appIcons.value = _appIcons.value + newIcons
                    }
                    _statusMessage.value = text(R.string.apps_refresh_success)
                } else {
                    _statusMessage.value = text(R.string.apps_refresh_failed)
                }
            } finally {
                _isRefreshingApps.value = false
            }
        }
    }

    private fun mergeInstalledApps(
        usageLogs: List<AppUsageLog>,
        installedApps: List<AppInfo>
    ): List<AppUsageLog> {
        val usageByPackage = usageLogs.mergedByPackage().associateBy { it.packageName }
        val installedByPackage = installedApps.associateBy { it.packageName }
        val today = android.text.format.DateFormat
            .format("yyyy-MM-dd", System.currentTimeMillis())
            .toString()

        return (usageByPackage.keys + installedByPackage.keys).map { packageName ->
            val usage = usageByPackage[packageName]
            val installed = installedByPackage[packageName]
            usage?.copy(
                category = if (installed?.category == AppCategory.SYSTEM) {
                    AppCategory.SYSTEM
                } else if (usage.category == AppCategory.OTHER && installed != null) {
                    installed.category
                } else {
                    usage.category
                },
                appLabel = usage.appLabel ?: installed?.label
            ) ?: AppUsageLog(
                packageName = packageName,
                totalTimeInForeground = 0L,
                lastTimeUsed = 0L,
                date = today,
                category = installed?.category ?: AppCategory.OTHER,
                appLabel = installed?.label
            )
        }.sortedByDescending { it.totalTimeInForeground }
    }

    fun getAppIcon(packageName: String): String? {
        return _appIcons.value[packageName]
    }

    fun fetchDailyReport(device: ChildDevice) {
        viewModelScope.launch {
            registerDeviceCredentials(device)
            val response = client.getDailyReport(device.ip.hostAddress ?: "", device.port, device.deviceId)
            if (response != null && response.success && response.dailyReport != null) {
                _dailyReport.value = response.dailyReport
                // Save to historical reports
                reportsRepository.saveReport(response.dailyReport!!)
                _statusMessage.value = "Report loaded and saved"
            } else {
                _statusMessage.value = "Failed to fetch report"
            }
        }
    }

    fun toggleAppBlock(device: ChildDevice, packageName: String, discoveryViewModel: DiscoveryViewModel? = null) {
        viewModelScope.launch {
            registerDeviceCredentials(device)
            val currentRule = _activeRules.value.find { it.packageName == packageName }
            // Check if currently blocked (rule exists and not expired)
            val isBlocked = currentRule != null && (currentRule.blockEndTime > System.currentTimeMillis() || currentRule.isPermanentlyBlocked)
            
            val newRules = if (isBlocked) {
                // Unblock the app without removing an independent internet block,
                // category, or daily-limit rule.
                if (currentRule != null && (currentRule.isInternetBlocked ||
                            currentRule.maxDailyTimeMs > 0 ||
                            currentRule.category != AppCategory.OTHER ||
                            currentRule.schedule.isNotEmpty())) {
                    _activeRules.value.map {
                        if (it.packageName == packageName) it.copy(
                            blockEndTime = 0L,
                            isPermanentlyBlocked = false
                        ) else it
                    }
                } else {
                    _activeRules.value.filter { it.packageName != packageName }
                }
            } else {
                // The App switch is a quick one-hour block.
                val rule = currentRule?.copy(
                    blockEndTime = System.currentTimeMillis() + 3600000,
                    isPermanentlyBlocked = false
                ) ?: BlockingRule(packageName, 0, System.currentTimeMillis() + 3600000)
                if (currentRule == null) _activeRules.value + rule
                else _activeRules.value.map { if (it.packageName == packageName) rule else it }
            }
            
            val response = client.updateRules(device.ip.hostAddress ?: "", device.port, device.deviceId, newRules)
             if (response != null && response.success) {
                _activeRules.value = newRules
                _statusMessage.value = if (isBlocked) "App unblocked" else "App blocked"
                
                // CRITICAL: If unblocking, also clear any app timer that might be enforcing the block
                if (isBlocked) {
                    client.setAppTimer(device.ip.hostAddress ?: "", device.port, device.deviceId, packageName, 0L)
                }
                
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to update app status"
            }
        }
    }

    fun toggleInternetBlock(device: ChildDevice, packageName: String, discoveryViewModel: DiscoveryViewModel? = null) {
        viewModelScope.launch {
            val rule = _activeRules.value.find { it.packageName == packageName }
            val newRules = if (rule == null) {
                _activeRules.value + BlockingRule(packageName = packageName, maxDailyTimeMs = 0, isInternetBlocked = true)
            } else {
                _activeRules.value.map { 
                    if (it.packageName == packageName) it.copy(isInternetBlocked = !it.isInternetBlocked)
                    else it
                }
            }
            
            val response = client.updateRules(device.ip.hostAddress ?: "", device.port, device.deviceId, newRules)
            if (response != null && response.success) {
                _statusMessage.value = "Internet status updated"
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to update internet status"
            }
        }
    }

    fun setAppIconVisibility(device: ChildDevice, visible: Boolean) {
        viewModelScope.launch {
            val response = client.setAppIconVisibility(device.ip.hostAddress ?: "", device.port, device.deviceId, visible)
            if (response != null && response.success) {
                _statusMessage.value = if (visible) "App unhidden on child device" else "App hidden on child device"
            } else {
                _statusMessage.value = if (visible) "Failed to unhide app" else "Failed to hide app"
            }
        }
    }

    fun setAppDailyLimit(device: ChildDevice, packageName: String, minutes: Int) {
        viewModelScope.launch {
            val rule = _activeRules.value.find { it.packageName == packageName }
            val newRules = if (rule == null) {
                _activeRules.value + BlockingRule(packageName = packageName, maxDailyTimeMs = minutes * 60 * 1000L)
            } else {
                _activeRules.value.map { 
                    if (it.packageName == packageName) it.copy(maxDailyTimeMs = minutes * 60 * 1000L)
                    else it
                }
            }
            
            val response = client.updateRules(device.ip.hostAddress ?: "", device.port, device.deviceId, newRules)
            if (response != null && response.success) {
                _statusMessage.value = "Daily limit updated"
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to update daily limit"
            }
        }
    }

    fun renameDevice(device: ChildDevice, newName: String) {
        viewModelScope.launch {
            device.customName = newName
            val response = client.updateDeviceName(device.ip.hostAddress ?: "", device.port, device.deviceId, newName)
            if (response != null && response.success) {
                _statusMessage.value = "Device renamed"
            } else {
                _statusMessage.value = "Failed to rename device (network error)"
            }
        }
    }
    
    fun setCategoryLimit(device: ChildDevice, category: AppCategory, durationMinutes: Int) {
        viewModelScope.launch {
            val limits = _categoryLimits.value.toMutableList()
            limits.removeAll { it.category == category }
            limits.add(CategoryLimit(category, durationMinutes * 60 * 1000L))
            _categoryLimits.value = limits
            
            val response = client.updateCategoryLimits(device.ip.hostAddress ?: "", device.port, device.deviceId, limits)
            if (response != null && response.success) {
                _statusMessage.value = "Category limit set"
            } else {
                _statusMessage.value = "Failed to set category limit"
            }
        }
    }
    
    fun approveUnlockRequest(device: ChildDevice, durationMinutes: Int = 10, packageName: String? = null, discoveryViewModel: DiscoveryViewModel? = null) {
        viewModelScope.launch {
            registerDeviceCredentials(device)
            val response = client.approveUnlock(device.ip.hostAddress ?: "", device.port, device.deviceId, durationMinutes * 60 * 1000L, packageName)
            if (response != null && response.success) {
                _statusMessage.value = "Unlock approved for $durationMinutes minutes"
                // If this was a device unlock, update shared status
                if (packageName == null) {
                    discoveryViewModel?.updateDeviceStatus(device.deviceId, false)
                }
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to approve unlock"
            }
        }
    }
    
    fun denyUnlockRequest(device: ChildDevice) {
        viewModelScope.launch {
            registerDeviceCredentials(device)
            val response = client.denyUnlock(device.ip.hostAddress ?: "", device.port, device.deviceId)
            if (response != null && response.success) {
                _statusMessage.value = "Unlock request denied"
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to deny unlock"
            }
        }
    }
    
    fun approveExtension(device: ChildDevice) {
        viewModelScope.launch {
            registerDeviceCredentials(device)
            val response = client.approveExtension(device.ip.hostAddress ?: "", device.port, device.deviceId)
            if (response != null && response.success) {
                _statusMessage.value = "Extension approved (+1 min)"
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to approve extension"
            }
        }
    }
    
    fun denyExtension(device: ChildDevice) {
        viewModelScope.launch {
            registerDeviceCredentials(device)
            val response = client.denyExtension(device.ip.hostAddress ?: "", device.port, device.deviceId)
            if (response != null && response.success) {
                _statusMessage.value = "Extension denied"
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to deny extension"
            }
        }
    }

    fun resetRequestCooldown(device: ChildDevice) {
        viewModelScope.launch {
            registerDeviceCredentials(device)
            val response = client.resetRequestCooldown(device.ip.hostAddress ?: "", device.port, device.deviceId)
            if (response != null && response.success) {
                _statusMessage.value = "Child can send another request now"
            } else {
                _statusMessage.value = "Failed to reset request cooldown"
            }
        }
    }

    fun setPinProtection(device: ChildDevice, enabled: Boolean) {
        viewModelScope.launch {
            registerDeviceCredentials(device)
            val response = client.setPinProtection(device.ip.hostAddress ?: "", device.port, device.deviceId, enabled)
            if (response != null && response.success) {
                _statusMessage.value = if (enabled) "Child PIN protection enabled" else "Child PIN protection disabled"
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to update PIN protection"
            }
        }
    }

    fun resetPin(device: ChildDevice) {
        viewModelScope.launch {
            registerDeviceCredentials(device)
            val response = client.resetPin(device.ip.hostAddress ?: "", device.port, device.deviceId)
            if (response != null && response.success) {
                _statusMessage.value = "PIN reset successfully"
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to reset PIN"
            }
        }
    }
    
    // ... existing code ...

    fun lockDevice(device: ChildDevice, locked: Boolean, discoveryViewModel: DiscoveryViewModel? = null) {
        viewModelScope.launch {
            registerDeviceCredentials(device)
            val response = client.setLock(device.ip.hostAddress ?: "", device.port, device.deviceId, locked)
            if (response != null && response.success) {
                _isDeviceLocked.value = locked
                _statusMessage.value = if (locked) "Device LOCKED" else "Device UNLOCKED"
                
                // NEW: Update shared status immediately
                discoveryViewModel?.updateDeviceStatus(device.deviceId, locked)
                
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to toggle lock"
            }
        }
    }
    fun setAppCategory(device: ChildDevice, packageName: String, category: AppCategory) {
        viewModelScope.launch {
            val response = client.setAppCategory(device.ip.hostAddress ?: "", device.port, device.deviceId, packageName, category)
            if (response != null && response.success) {
                _statusMessage.value = "Category updated"
                // Ideally refresh stats to reflect change if immediate response needed, but device might take a moment
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to update category"
            }
        }
    }

    fun setAppTimer(device: ChildDevice, packageName: String, durationMinutes: Int) {
         viewModelScope.launch {
            val response = client.setAppTimer(device.ip.hostAddress ?: "", device.port, device.deviceId, packageName, durationMinutes * 60 * 1000L)
            if (response != null && response.success) {
                _statusMessage.value = "Timer set for $durationMinutes mins"
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to set timer"
            }
        }
    }

    fun cancelAppTimer(device: ChildDevice, packageName: String) {
        viewModelScope.launch {
            val response = client.setAppTimer(device.ip.hostAddress ?: "", device.port, device.deviceId, packageName, 0L)
            if (response != null && response.success) {
                _statusMessage.value = "Timer canceled"
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to cancel timer"
            }
        }
    }

    private val _categoryTimers = MutableStateFlow<Map<AppCategory, Long>>(emptyMap())
    val categoryTimers: StateFlow<Map<AppCategory, Long>> = _categoryTimers.asStateFlow()

    fun setCategoryTimer(device: ChildDevice, category: AppCategory, durationMinutes: Int) {
        viewModelScope.launch {
            val response = client.setCategoryTimer(device.ip.hostAddress ?: "", device.port, device.deviceId, category, durationMinutes * 60 * 1000L)
            if (response != null && response.success) {
                _statusMessage.value = "Category timer set for $durationMinutes mins"
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to set category timer"
            }
        }
    }

    fun cancelCategoryTimer(device: ChildDevice, category: AppCategory) {
        viewModelScope.launch {
            val response = client.setCategoryTimer(device.ip.hostAddress ?: "", device.port, device.deviceId, category, 0L)
            if (response != null && response.success) {
                _statusMessage.value = "Category timer canceled"
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to cancel category timer"
            }
        }
    }

    fun updateBreakRules(
        device: ChildDevice, 
        usageLimitMs: Long, 
        breakDurationMs: Long,
        breakWarningMs: Long = 0,
        educationOnly: Boolean = false,
        allowExtensions: Boolean = false
    ) {
        viewModelScope.launch {
            val response = client.setBreakRules(
                device.ip.hostAddress ?: "", 
                device.port, 
                device.deviceId, 
                _activeRules.value,
                usageLimitMs,
                breakDurationMs,
                breakWarningMs,
                educationOnly,
                allowExtensions
            )
            if (response != null && response.success) {
                _usageLimitMs.value = usageLimitMs
                _breakDurationMs.value = breakDurationMs
                _breakWarningMs.value = breakWarningMs
                _educationOnly.value = educationOnly
                _allowExtensions.value = allowExtensions
                _statusMessage.value = "Break rules updated"
            } else {
                _statusMessage.value = "Failed to update break rules"
            }
        }
    }

    fun stopBreak(device: ChildDevice) {
        viewModelScope.launch {
            registerDeviceCredentials(device)
            val response = client.stopBreak(device.ip.hostAddress ?: "", device.port, device.deviceId)
            if (response != null && response.success) {
                _statusMessage.value = "Break stopped"
                // Refresh stats to update lock status
                fetchStats(device)
            } else {
                _statusMessage.value = "Failed to stop break"
            }
        }
    }

    fun blockAppNow(device: ChildDevice, packageName: String) {
        viewModelScope.launch {
            val currentRule = _activeRules.value.find { it.packageName == packageName }
            val rule = currentRule?.copy(
                blockEndTime = 0L,
                isPermanentlyBlocked = true
            ) ?: BlockingRule(
                packageName = packageName,
                maxDailyTimeMs = 0L,
                isPermanentlyBlocked = true
            )
            val newRules = if (currentRule == null) {
                _activeRules.value + rule
            } else {
                _activeRules.value.map { if (it.packageName == packageName) rule else it }
            }
            val response = client.updateRules(device.ip.hostAddress ?: "", device.port, device.deviceId, newRules)
            if (response != null && response.success) {
                _activeRules.value = newRules
                _statusMessage.value = text(R.string.apps_blocked_now)
                fetchStats(device)
            } else {
                _statusMessage.value = text(R.string.apps_block_failed)
            }
        }
    }

    fun setAppBlockDuration(device: ChildDevice, packageName: String, durationMinutes: Int) {
        if (durationMinutes <= 0) return
        viewModelScope.launch {
            val currentRule = _activeRules.value.find { it.packageName == packageName }
            val rule = currentRule?.copy(
                blockEndTime = System.currentTimeMillis() + durationMinutes * 60_000L,
                isPermanentlyBlocked = false
            ) ?: BlockingRule(
                packageName = packageName,
                maxDailyTimeMs = 0L,
                blockEndTime = System.currentTimeMillis() + durationMinutes * 60_000L
            )
            val newRules = if (currentRule == null) {
                _activeRules.value + rule
            } else {
                _activeRules.value.map { if (it.packageName == packageName) rule else it }
            }
            val response = client.updateRules(device.ip.hostAddress ?: "", device.port, device.deviceId, newRules)
            if (response != null && response.success) {
                _activeRules.value = newRules
                _statusMessage.value = text(R.string.apps_block_duration_set, durationMinutes)
                fetchStats(device)
            } else {
                _statusMessage.value = text(R.string.apps_block_failed)
            }
        }
    }

    fun setBlockingScreenStyle(device: ChildDevice, style: BlockingScreenStyle) {
        if (device.deviceId in _blockingScreenStyleSaves.value) return

        val generation = synchronized(statsGenerations) {
            val next = (statsGenerations["style:${device.deviceId}"] ?: 0L) + 1L
            statsGenerations["style:${device.deviceId}"] = next
            next
        }
        _blockingScreenStyleSaves.value = _blockingScreenStyleSaves.value + device.deviceId

        viewModelScope.launch {
            try {
                registerDeviceCredentials(device)
                val response = client.setBlockingScreenStyle(
                    ip = device.ip.hostAddress ?: "",
                    port = device.port,
                    deviceId = device.deviceId,
                    style = style
                )
                val isLatest = synchronized(statsGenerations) {
                    statsGenerations["style:${device.deviceId}"] == generation
                }
                if (isLatest && response != null && response.success) {
                    _blockingScreenStyles.value = _blockingScreenStyles.value +
                        (device.deviceId to style)
                    _statusMessage.value = text(R.string.blocking_style_saved)
                    fetchStats(device, forceRefresh = true)
                } else if (isLatest) {
                    _statusMessage.value = text(R.string.blocking_style_save_failed)
                }
            } finally {
                val isLatest = synchronized(statsGenerations) {
                    statsGenerations["style:${device.deviceId}"] == generation
                }
                if (isLatest) {
                    _blockingScreenStyleSaves.value = _blockingScreenStyleSaves.value - device.deviceId
                }
            }
        }
    }

    fun setAppSuspended(device: ChildDevice, packageName: String, suspended: Boolean) {
        sendDeviceOwnerCommand(
            device = device,
            capability = DeviceOwnerCapability.APP_SUSPENSION,
            command = Packet.Command(
                commandType = CommandType.DEVICE_OWNER_SET_APP_SUSPENDED,
                packageName = packageName,
                enabled = suspended
            ),
            successMessage = text(if (suspended) R.string.device_owner_app_suspended else R.string.device_owner_app_resumed)
        )
    }

    fun setUninstallProtection(device: ChildDevice, packageName: String, enabled: Boolean) {
        sendDeviceOwnerCommand(
            device = device,
            capability = DeviceOwnerCapability.UNINSTALL_PROTECTION,
            command = Packet.Command(
                commandType = CommandType.DEVICE_OWNER_SET_UNINSTALL_PROTECTION,
                packageName = packageName,
                enabled = enabled
            ),
            successMessage = text(if (enabled) R.string.device_owner_uninstall_enabled else R.string.device_owner_uninstall_disabled)
        )
    }

    fun setUserRestriction(device: ChildDevice, restrictionKey: String, enabled: Boolean) {
        sendDeviceOwnerCommand(
            device = device,
            capability = DeviceOwnerCapability.USER_RESTRICTIONS,
            command = Packet.Command(
                commandType = CommandType.DEVICE_OWNER_SET_USER_RESTRICTION,
                restrictionKey = restrictionKey,
                enabled = enabled
            ),
            successMessage = text(R.string.device_owner_restriction_updated)
        )
    }

    fun setDeviceOwnerLocked(device: ChildDevice, locked: Boolean) {
        sendDeviceOwnerCommand(
            device = device,
            capability = DeviceOwnerCapability.DEVICE_LOCK,
            command = Packet.Command(
                commandType = CommandType.DEVICE_OWNER_SET_DEVICE_LOCKED,
                enabled = locked
            ),
            successMessage = text(if (locked) R.string.device_owner_device_locked else R.string.device_owner_device_unlocked)
        )
    }

    fun setDeviceUsageLimit(device: ChildDevice, durationMs: Long) {
        sendDeviceOwnerCommand(
            device = device,
            capability = DeviceOwnerCapability.DEVICE_USAGE_LIMITS,
            command = Packet.Command(
                commandType = CommandType.DEVICE_OWNER_SET_DEVICE_USAGE_LIMIT,
                timerDurationMs = durationMs
            ),
            successMessage = text(R.string.device_owner_device_limit_updated)
        )
    }

    fun setAppUsageLimit(device: ChildDevice, packageName: String, durationMs: Long) {
        sendDeviceOwnerCommand(
            device = device,
            capability = DeviceOwnerCapability.APP_USAGE_LIMITS,
            command = Packet.Command(
                commandType = CommandType.DEVICE_OWNER_SET_APP_USAGE_LIMIT,
                packageName = packageName,
                timerDurationMs = durationMs
            ),
            successMessage = text(R.string.device_owner_app_limit_updated)
        )
    }

    fun setWifiEnabled(device: ChildDevice, enabled: Boolean) {
        sendDeviceOwnerCommand(
            device = device,
            capability = DeviceOwnerCapability.WIFI_TOGGLE,
            command = Packet.Command(
                commandType = CommandType.DEVICE_OWNER_SET_WIFI_ENABLED,
                enabled = enabled
            ),
            successMessage = text(if (enabled) R.string.device_owner_wifi_enabled else R.string.device_owner_wifi_disabled)
        )
    }

    fun clearChildScreenLock(device: ChildDevice) {
        sendDeviceOwnerCommand(
            device = device,
            capability = DeviceOwnerCapability.DEVICE_LOCK,
            command = Packet.Command(
                commandType = CommandType.DEVICE_OWNER_RESET_SCREEN_LOCK
            ),
            successMessage = text(R.string.device_owner_screen_lock_cleared)
        )
    }

    private fun sendDeviceOwnerCommand(
        device: ChildDevice,
        capability: DeviceOwnerCapability,
        command: Packet.Command,
        successMessage: String
    ) {
        val state = _deviceOwnerCapabilities.value[device.deviceId]
            ?.stateFor(capability)
            ?: CapabilityState.UNKNOWN

        if (state != CapabilityState.AVAILABLE) {
            _statusMessage.value = if (state == CapabilityState.UNAVAILABLE) {
                text(R.string.device_owner_unavailable_desc)
            } else {
                text(R.string.device_owner_unknown_desc)
            }
            return
        }

        viewModelScope.launch {
            registerDeviceCredentials(device)
            val response = client.sendDeviceOwnerCommand(
                device.ip.hostAddress ?: "",
                device.port,
                device.deviceId,
                command
            )
            if (response != null && response.success) {
                _statusMessage.value = successMessage
                fetchStats(device, forceRefresh = true)
            } else {
                _statusMessage.value = text(R.string.device_owner_command_failed)
            }
        }
    }

    private fun text(
        @androidx.annotation.StringRes resourceId: Int,
        vararg formatArgs: Any
    ): String = getApplication<Application>().getString(resourceId, *formatArgs)

    private fun registerDeviceCredentials(device: ChildDevice) {
        device.pairToken?.let { client.registerPairToken(device.deviceId, it) }
        device.bluetoothMac?.let { client.registerBluetoothMac(device.deviceId, it) }
    }
}


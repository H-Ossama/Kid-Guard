package com.parentalguard.child.data

import android.content.Context
import com.parentalguard.common.model.BlockingRule
import com.parentalguard.common.model.CategoryLimit
import com.parentalguard.common.model.AppCategory
import com.parentalguard.common.model.BlockingScreenStyle
import com.parentalguard.common.utils.CategoryMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RuleRepository {
    private var persistentStateManager: PersistentStateManager? = null

    fun initialize(context: Context) {
        if (persistentStateManager != null) return
        persistentStateManager = PersistentStateManager(context)
        loadPersistedState()
        // Always ensure VPN is in sync on startup (no-ops safely in background —
        // InternetBlockerService defers the start instead of crashing).
        syncVpn(context)
    }

    /**
     * Re-applies the VPN internet-blocking state from the current rules.
     * Safe to call from background contexts (boot, job, service restarts):
     * the service start is deferred — never throws — when the system refuses
     * background starts, and runs on the next foreground start instead.
     */
    fun syncVpn(context: Context) {
        notifyVpn(context)
    }

    private fun loadPersistedState() {
        persistentStateManager?.let { manager ->
            _rules.value = manager.loadRules()
            _categoryLimits.value = manager.loadCategoryLimits()
            
            val (isLocked, lockUntil) = manager.loadGlobalLock()
            // Check if timed lock has expired during downtime
            if (isLocked && lockUntil > 0 && lockUntil <= System.currentTimeMillis()) {
                _globalLock.value = false
                _globalLockUntil.value = 0
                _lockReason.value = null
                manager.saveGlobalLock(false, 0, null)
            } else {
                _globalLock.value = isLocked
                _globalLockUntil.value = lockUntil
                _lockReason.value = manager.loadLockReason()
            }
            
            _temporaryUnlockUntil.value = manager.loadTemporaryUnlockUntil()
            _appTimers.value = manager.loadAppTimers()
            _categoryTimers.value = manager.loadCategoryTimers()
            _lastUnlockRequestTime.value = manager.loadLastUnlockRequestTime()

            val breakData = manager.loadBreakRules()
            _usageLimitMs.value = breakData.usageLimit
            _breakDurationMs.value = breakData.breakDuration
            _breakWarningMs.value = breakData.warningMs
            _educationOnly.value = breakData.educationOnly
            _allowExtensions.value = breakData.allowExtensions
            _currentBreakUsageMs.value = manager.loadCurrentBreakUsage()
            _customCategories.value = manager.loadCustomCategories()
            _warningsShown.value = manager.loadWarningsShown()
            _blockingScreenStyle.value = manager.loadBlockingScreenStyle()
            _deviceOwnerDeviceUsageLimitMs.value = manager.loadOwnerDeviceUsageLimit()
            _deviceOwnerAppUsageLimits.value = manager.loadOwnerAppUsageLimits()
            _deviceOwnerUsageSuspended.value = manager.loadOwnerUsageSuspended()
        }
    }


    private val _rules = MutableStateFlow<List<BlockingRule>>(emptyList())
    val rules: StateFlow<List<BlockingRule>> = _rules.asStateFlow()
    
    private val _categoryLimits = MutableStateFlow<List<CategoryLimit>>(emptyList())
    val categoryLimits: StateFlow<List<CategoryLimit>> = _categoryLimits.asStateFlow()

    private val _blockingScreenStyle = MutableStateFlow(BlockingScreenStyle.CURRENT)
    val blockingScreenStyle: StateFlow<BlockingScreenStyle> = _blockingScreenStyle.asStateFlow()

    fun setBlockingScreenStyle(style: BlockingScreenStyle) {
        _blockingScreenStyle.value = style
        persistentStateManager?.saveBlockingScreenStyle(style)
    }

    fun updateRules(newRules: List<BlockingRule>) {
        _rules.value = newRules
        persistentStateManager?.saveRules(newRules)
        
        // Trigger VPN update if any internet rules changed
        notifyVpn(com.parentalguard.child.ChildApp.instance)
    }

    private fun notifyVpn(context: Context) {
        val rules = _rules.value
        val hasInternetBlock = rules.any { it.isInternetBlocked }
        
        if (hasInternetBlock) {
            com.parentalguard.child.service.InternetBlockerService.start(context)
            com.parentalguard.child.service.InternetBlockerService.update(context)
        } else {
            // Note: In a real app, you might want to stop the service IF it was running
            // or just update it with empty list (which will effectively unblock all)
            com.parentalguard.child.service.InternetBlockerService.update(context)
            // Optional: stop if empty to save battery
            // com.parentalguard.child.service.InternetBlockerService.stop(context)
        }
    }
    
    fun updateCategoryLimits(limits: List<CategoryLimit>) {
        _categoryLimits.value = limits
        persistentStateManager?.saveCategoryLimits(limits)
    }

    private val _usageLimitMs = MutableStateFlow<Long>(0)
    val usageLimitMs: StateFlow<Long> = _usageLimitMs.asStateFlow()

    private val _breakDurationMs = MutableStateFlow<Long>(0)
    val breakDurationMs: StateFlow<Long> = _breakDurationMs.asStateFlow()

    private val _currentBreakUsageMs = MutableStateFlow<Long>(0)
    val currentBreakUsageMs: StateFlow<Long> = _currentBreakUsageMs.asStateFlow()

    private val _breakWarningMs = MutableStateFlow<Long>(0)
    val breakWarningMs: StateFlow<Long> = _breakWarningMs.asStateFlow()

    private val _educationOnly = MutableStateFlow<Boolean>(false)
    val educationOnly: StateFlow<Boolean> = _educationOnly.asStateFlow()

    private val _allowExtensions = MutableStateFlow<Boolean>(false)
    val allowExtensions: StateFlow<Boolean> = _allowExtensions.asStateFlow()

    private val _deviceOwnerDeviceUsageLimitMs = MutableStateFlow(0L)
    val deviceOwnerDeviceUsageLimitMs: StateFlow<Long> = _deviceOwnerDeviceUsageLimitMs.asStateFlow()

    private val _deviceOwnerAppUsageLimits = MutableStateFlow<Map<String, Long>>(emptyMap())
    val deviceOwnerAppUsageLimits: StateFlow<Map<String, Long>> = _deviceOwnerAppUsageLimits.asStateFlow()

    private val _deviceOwnerUsageSuspended = MutableStateFlow<Set<String>>(emptySet())
    val deviceOwnerUsageSuspended: StateFlow<Set<String>> = _deviceOwnerUsageSuspended.asStateFlow()

    fun setDeviceOwnerDeviceUsageLimit(limitMs: Long) {
        _deviceOwnerDeviceUsageLimitMs.value = limitMs.coerceAtLeast(0L)
        persistentStateManager?.saveOwnerDeviceUsageLimit(_deviceOwnerDeviceUsageLimitMs.value)
    }

    fun setDeviceOwnerAppUsageLimit(packageName: String, limitMs: Long) {
        val limits = _deviceOwnerAppUsageLimits.value.toMutableMap()
        if (limitMs > 0) limits[packageName] = limitMs else limits.remove(packageName)
        _deviceOwnerAppUsageLimits.value = limits
        persistentStateManager?.saveOwnerAppUsageLimits(limits)
    }

    fun markDeviceOwnerUsageSuspended(packageName: String, suspended: Boolean) {
        val packages = _deviceOwnerUsageSuspended.value.toMutableSet()
        if (suspended) packages.add(packageName) else packages.remove(packageName)
        _deviceOwnerUsageSuspended.value = packages
        persistentStateManager?.saveOwnerUsageSuspended(packages)
    }

    fun clearDeviceOwnerPolicies() {
        _deviceOwnerDeviceUsageLimitMs.value = 0L
        _deviceOwnerAppUsageLimits.value = emptyMap()
        _deviceOwnerUsageSuspended.value = emptySet()
        persistentStateManager?.saveOwnerDeviceUsageLimit(0L)
        persistentStateManager?.saveOwnerAppUsageLimits(emptyMap())
        persistentStateManager?.saveOwnerUsageSuspended(emptySet())
    }

    fun setBreakRules(limit: Long, duration: Long, warningMs: Long = 0, educationOnly: Boolean = false, allowExtensions: Boolean = false) {
        _usageLimitMs.value = limit
        _breakDurationMs.value = duration
        _breakWarningMs.value = warningMs
        _educationOnly.value = educationOnly
        _allowExtensions.value = allowExtensions
        persistentStateManager?.saveBreakRules(limit, duration, warningMs, educationOnly, allowExtensions)
    }

    fun setCurrentBreakUsage(usage: Long) {
        _currentBreakUsageMs.value = usage
        persistentStateManager?.saveCurrentBreakUsage(usage)
    }

    private val _globalLock = MutableStateFlow<Boolean>(false)
    val globalLock: StateFlow<Boolean> = _globalLock.asStateFlow()

    private val _globalLockUntil = MutableStateFlow<Long>(0)
    val globalLockUntil: StateFlow<Long> = _globalLockUntil.asStateFlow()

    private val _lockReason = MutableStateFlow<String?>(null)
    val lockReason: StateFlow<String?> = _lockReason.asStateFlow()

    fun setGlobalLock(locked: Boolean, reason: String? = null) {
        _globalLock.value = locked
        _lockReason.value = reason
        // A permanent lock must never retain a stale timed lockUntil, otherwise the
        // expiry loops in MonitorService/loadPersistedState would auto-unlock it.
        _globalLockUntil.value = 0
        if (!locked) {
            _lockReason.value = null
        }
        persistentStateManager?.saveGlobalLock(locked, _globalLockUntil.value, _lockReason.value)
    }

    fun setGlobalLockUntil(timestamp: Long, reason: String? = null) {
        _globalLockUntil.value = timestamp
        _lockReason.value = reason
        if (timestamp > System.currentTimeMillis()) {
            _globalLock.value = true
        } else {
             _globalLock.value = false
             _globalLockUntil.value = 0
             _lockReason.value = null
        }
        persistentStateManager?.saveGlobalLock(_globalLock.value, _globalLockUntil.value, _lockReason.value)
    }
    
    // Temporary unlock management
    private val _temporaryUnlockUntil = MutableStateFlow<Long>(0)
    val temporaryUnlockUntil: StateFlow<Long> = _temporaryUnlockUntil.asStateFlow()
    
    fun setTemporaryUnlock(untilTimestamp: Long) {
        _temporaryUnlockUntil.value = untilTimestamp
        persistentStateManager?.saveTemporaryUnlockUntil(untilTimestamp)
    }
    
    fun isTemporarilyUnlocked(): Boolean {
        return System.currentTimeMillis() < _temporaryUnlockUntil.value
    }

    fun getRuleForPackage(packageName: String): BlockingRule? {
        return _rules.value.find { it.packageName == packageName }
    }
    
    fun getCategoryLimit(category: AppCategory): CategoryLimit? {
        return _categoryLimits.value.find { it.category == category }
    }
    
    /**
     * Check if an app is whitelisted (system-level or user-defined)
     */
    fun isWhitelisted(packageName: String): Boolean {
        // Check system whitelist
        if (CategoryMapper.isWhitelisted(packageName)) return true
        
        // Check user-defined whitelist from rules
        val rule = getRuleForPackage(packageName)
        return rule?.isWhitelisted == true
    }
    
    // Track which apps/categories have shown warnings for the current day
    // Map of Identifier -> DateString (yyyy-MM-dd)
    private val _warningsShown = MutableStateFlow<Map<String, String>>(emptyMap())
    
    fun markWarningShown(identifier: String) {
        val current = _warningsShown.value.toMutableMap()
        val today = android.text.format.DateFormat.format("yyyy-MM-dd", System.currentTimeMillis()).toString()
        current[identifier] = today
        _warningsShown.value = current
        persistentStateManager?.saveWarningsShown(current)
    }
    
    fun hasWarningBeenShown(identifier: String): Boolean {
        val lastShownDate = _warningsShown.value[identifier] ?: return false
        val today = android.text.format.DateFormat.format("yyyy-MM-dd", System.currentTimeMillis()).toString()
        return lastShownDate == today
    }
    
    fun clearWarnings() {
        _warningsShown.value = emptyMap()
        persistentStateManager?.saveWarningsShown(emptyMap())
    }
    
    // Custom Categories Override
    private val _customCategories = MutableStateFlow<Map<String, AppCategory>>(emptyMap())
    
    fun setCustomCategory(packageName: String, category: AppCategory) {
        val current = _customCategories.value.toMutableMap()
        current[packageName] = category
        _customCategories.value = current
        persistentStateManager?.saveCustomCategories(current)
    }

    fun getCategory(packageName: String, appLabel: String? = null): AppCategory {
        return _customCategories.value[packageName] 
            ?: CategoryMapper.getCategoryForPackage(packageName, appLabel)
    }

    // App Timers (One-time allowance)
    // Map of PackageName -> ExpirationTimestamp
    private val _appTimers = MutableStateFlow<Map<String, Long>>(emptyMap())
    val appTimers: StateFlow<Map<String, Long>> = _appTimers.asStateFlow()

    fun setAppTimer(packageName: String, durationMs: Long) {
        val current = _appTimers.value.toMutableMap()
        if (durationMs > 0) {
            current[packageName] = System.currentTimeMillis() + durationMs
        } else {
            current.remove(packageName)
        }
        _appTimers.value = current
        persistentStateManager?.saveAppTimers(current)
    }

    // Category Timers
    private val _categoryTimers = MutableStateFlow<Map<AppCategory, Long>>(emptyMap())
    val categoryTimers: StateFlow<Map<AppCategory, Long>> = _categoryTimers.asStateFlow()

    fun setCategoryTimer(category: AppCategory, durationMs: Long) {
        val current = _categoryTimers.value.toMutableMap()
        if (durationMs > 0) {
            current[category] = System.currentTimeMillis() + durationMs
        } else {
            current.remove(category)
        }
        _categoryTimers.value = current
        persistentStateManager?.saveCategoryTimers(current)
    }

    fun isCategoryTimerActive(category: AppCategory): Boolean {
        val expiration = _categoryTimers.value[category] ?: return false
        return System.currentTimeMillis() < expiration
    }

    fun isAppTimerActive(packageName: String): Boolean {
        val expiration = _appTimers.value[packageName] ?: return false
        return System.currentTimeMillis() < expiration
    }

    // Track last unlock request time to prevent spam
    private val _lastUnlockRequestTime = MutableStateFlow<Long>(0)
    val lastUnlockRequestTime: StateFlow<Long> = _lastUnlockRequestTime.asStateFlow()

    fun updateLastUnlockRequestTime() {
        val now = System.currentTimeMillis()
        _lastUnlockRequestTime.value = now
        persistentStateManager?.saveLastUnlockRequestTime(now)
    }

    /**
     * Clears the 15-minute request cooldown so the child can send another
     * unlock/extension request immediately (reactivate button on the lock
     * screen). The parent still approves or denies every single request.
     */
    fun clearUnlockRequestCooldown() {
        _lastUnlockRequestTime.value = 0
        persistentStateManager?.saveLastUnlockRequestTime(0)
    }

    // For testing/bootstrap
    init {
        // Example rule: Block settings (just for test, danger!) or a game
        // _rules.value = listOf(BlockingRule("com.example.game", 0, isPermanentlyBlocked = true))
    }
}


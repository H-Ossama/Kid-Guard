package com.parentalguard.child.data

import android.content.Context
import android.content.SharedPreferences
import com.parentalguard.common.model.BlockingRule
import com.parentalguard.common.model.CategoryLimit
import com.parentalguard.common.model.AppCategory
import com.parentalguard.common.model.BlockingScreenStyle
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class PersistentStateManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("lock_state_prefs", Context.MODE_PRIVATE)
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = false
    }
    
    companion object {
        private const val KEY_GLOBAL_LOCK = "global_lock"
        private const val KEY_GLOBAL_LOCK_UNTIL = "global_lock_until"
        private const val KEY_GLOBAL_LOCK_REASON = "global_lock_reason"
        private const val KEY_RULES = "blocking_rules"
        private const val KEY_CATEGORY_LIMITS = "category_limits"
        private const val KEY_TEMP_UNLOCK_UNTIL = "temp_unlock_until"
        private const val KEY_APP_TIMERS = "app_timers"
        private const val KEY_CATEGORY_TIMERS = "category_timers"
        private const val KEY_LAST_UNLOCK_REQUEST = "last_unlock_request"
        private const val KEY_USAGE_LIMIT = "usage_limit"
        private const val KEY_BREAK_DURATION = "break_duration"
        private const val KEY_BREAK_WARNING = "break_warning"
        private const val KEY_EDUCATION_ONLY = "education_only"
        private const val KEY_ALLOW_EXTENSIONS = "allow_extensions"
        private const val KEY_CURRENT_BREAK_USAGE = "current_break_usage"
        private const val KEY_CUSTOM_CATEGORIES = "custom_categories"
        private const val KEY_WARNINGS_SHOWN = "warnings_shown"
        private const val KEY_BLOCKING_SCREEN_STYLE = "blocking_screen_style"
        private const val KEY_OWNER_DEVICE_USAGE_LIMIT = "owner_device_usage_limit"
        private const val KEY_OWNER_APP_USAGE_LIMITS = "owner_app_usage_limits"
        private const val KEY_OWNER_USAGE_SUSPENDED = "owner_usage_suspended"
        private const val KEY_OWNER_MANUAL_SUSPENDED = "owner_manual_suspended"
        private const val KEY_OWNER_UNINSTALL_PROTECTED = "owner_uninstall_protected"
    }
    
    fun saveBreakRules(usageLimit: Long, breakDuration: Long, warningMs: Long, educationOnly: Boolean, allowExtensions: Boolean) {
        prefs.edit()
            .putLong(KEY_USAGE_LIMIT, usageLimit)
            .putLong(KEY_BREAK_DURATION, breakDuration)
            .putLong(KEY_BREAK_WARNING, warningMs)
            .putBoolean(KEY_EDUCATION_ONLY, educationOnly)
            .putBoolean(KEY_ALLOW_EXTENSIONS, allowExtensions)
            .apply()
    }

    fun loadBreakRules(): BreakRuleData {
        return BreakRuleData(
            prefs.getLong(KEY_USAGE_LIMIT, 0L),
            prefs.getLong(KEY_BREAK_DURATION, 0L),
            prefs.getLong(KEY_BREAK_WARNING, 0L),
            prefs.getBoolean(KEY_EDUCATION_ONLY, false),
            prefs.getBoolean(KEY_ALLOW_EXTENSIONS, false)
        )
    }

    data class BreakRuleData(
        val usageLimit: Long,
        val breakDuration: Long,
        val warningMs: Long,
        val educationOnly: Boolean,
        val allowExtensions: Boolean
    )

    fun saveCurrentBreakUsage(usage: Long) {
        prefs.edit().putLong(KEY_CURRENT_BREAK_USAGE, usage).apply()
    }

    fun loadCurrentBreakUsage(): Long {
        return prefs.getLong(KEY_CURRENT_BREAK_USAGE, 0L)
    }
    
    fun saveGlobalLock(isLocked: Boolean, lockUntil: Long, reason: String? = null) {
        prefs.edit()
            .putBoolean(KEY_GLOBAL_LOCK, isLocked)
            .putLong(KEY_GLOBAL_LOCK_UNTIL, lockUntil)
            .putString(KEY_GLOBAL_LOCK_REASON, reason)
            .apply()
    }
    
    fun loadGlobalLock(): Pair<Boolean, Long> {
        val isLocked = prefs.getBoolean(KEY_GLOBAL_LOCK, false)
        val lockUntil = prefs.getLong(KEY_GLOBAL_LOCK_UNTIL, 0L)
        return Pair(isLocked, lockUntil)
    }
    
    fun saveRules(rules: List<BlockingRule>) {
        try {
            val jsonString = json.encodeToString(rules)
            prefs.edit().putString(KEY_RULES, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun loadRules(): List<BlockingRule> {
        val jsonString = prefs.getString(KEY_RULES, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<BlockingRule>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun saveCategoryLimits(limits: List<CategoryLimit>) {
        try {
            val jsonString = json.encodeToString(limits)
            prefs.edit().putString(KEY_CATEGORY_LIMITS, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun loadCategoryLimits(): List<CategoryLimit> {
        val jsonString = prefs.getString(KEY_CATEGORY_LIMITS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<CategoryLimit>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun saveTemporaryUnlockUntil(timestamp: Long) {
        prefs.edit().putLong(KEY_TEMP_UNLOCK_UNTIL, timestamp).apply()
    }
    
    fun loadTemporaryUnlockUntil(): Long {
        return prefs.getLong(KEY_TEMP_UNLOCK_UNTIL, 0L)
    }
    
    fun saveAppTimers(timers: Map<String, Long>) {
        try {
            val jsonString = json.encodeToString(timers)
            prefs.edit().putString(KEY_APP_TIMERS, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun loadAppTimers(): Map<String, Long> {
        val jsonString = prefs.getString(KEY_APP_TIMERS, null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, Long>>(jsonString)
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    fun saveCategoryTimers(timers: Map<AppCategory, Long>) {
        try {
            val jsonString = json.encodeToString(timers)
            prefs.edit().putString(KEY_CATEGORY_TIMERS, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun loadCategoryTimers(): Map<AppCategory, Long> {
        val jsonString = prefs.getString(KEY_CATEGORY_TIMERS, null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<AppCategory, Long>>(jsonString)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveLastUnlockRequestTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_UNLOCK_REQUEST, time).apply()
    }

    fun loadLastUnlockRequestTime(): Long {
        return prefs.getLong(KEY_LAST_UNLOCK_REQUEST, 0L)
    }

    fun saveCustomCategories(categories: Map<String, AppCategory>) {
        try {
            val jsonString = json.encodeToString(categories)
            prefs.edit().putString(KEY_CUSTOM_CATEGORIES, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCustomCategories(): Map<String, AppCategory> {
        val jsonString = prefs.getString(KEY_CUSTOM_CATEGORIES, null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, AppCategory>>(jsonString)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveWarningsShown(warnings: Map<String, String>) {
        try {
            val jsonString = json.encodeToString(warnings)
            prefs.edit().putString(KEY_WARNINGS_SHOWN, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadWarningsShown(): Map<String, String> {
        val jsonString = prefs.getString(KEY_WARNINGS_SHOWN, null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, String>>(jsonString)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun loadLockReason(): String? = prefs.getString(KEY_GLOBAL_LOCK_REASON, null)

    fun saveBlockingScreenStyle(style: BlockingScreenStyle) {
        prefs.edit().putString(KEY_BLOCKING_SCREEN_STYLE, style.name).apply()
    }

    fun loadBlockingScreenStyle(): BlockingScreenStyle {
        return prefs.getString(KEY_BLOCKING_SCREEN_STYLE, null)
            ?.let { runCatching { BlockingScreenStyle.valueOf(it) }.getOrNull() }
            ?: BlockingScreenStyle.CURRENT
    }

    fun saveOwnerDeviceUsageLimit(limitMs: Long) {
        prefs.edit().putLong(KEY_OWNER_DEVICE_USAGE_LIMIT, limitMs).apply()
    }

    fun loadOwnerDeviceUsageLimit(): Long =
        prefs.getLong(KEY_OWNER_DEVICE_USAGE_LIMIT, 0L)

    fun saveOwnerAppUsageLimits(limits: Map<String, Long>) {
        try {
            prefs.edit().putString(KEY_OWNER_APP_USAGE_LIMITS, json.encodeToString(limits)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadOwnerAppUsageLimits(): Map<String, Long> {
        val value = prefs.getString(KEY_OWNER_APP_USAGE_LIMITS, null) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, Long>>(value) }.getOrDefault(emptyMap())
    }

    fun saveOwnerUsageSuspended(packages: Set<String>) {
        try {
            prefs.edit().putString(KEY_OWNER_USAGE_SUSPENDED, json.encodeToString(packages.toList())).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadOwnerUsageSuspended(): Set<String> {
        val value = prefs.getString(KEY_OWNER_USAGE_SUSPENDED, null) ?: return emptySet()
        return runCatching { json.decodeFromString<List<String>>(value).toSet() }.getOrDefault(emptySet())
    }

    fun saveOwnerManualSuspended(packages: Set<String>) {
        try {
            prefs.edit().putString(KEY_OWNER_MANUAL_SUSPENDED, json.encodeToString(packages.toList())).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadOwnerManualSuspended(): Set<String> {
        val value = prefs.getString(KEY_OWNER_MANUAL_SUSPENDED, null) ?: return emptySet()
        return runCatching { json.decodeFromString<List<String>>(value).toSet() }.getOrDefault(emptySet())
    }

    fun saveOwnerUninstallProtected(packages: Set<String>) {
        try {
            prefs.edit().putString(KEY_OWNER_UNINSTALL_PROTECTED, json.encodeToString(packages.toList())).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadOwnerUninstallProtected(): Set<String> {
        val value = prefs.getString(KEY_OWNER_UNINSTALL_PROTECTED, null) ?: return emptySet()
        return runCatching { json.decodeFromString<List<String>>(value).toSet() }.getOrDefault(emptySet())
    }
}

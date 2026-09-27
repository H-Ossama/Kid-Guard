package com.parentalguard.common.network

import kotlinx.serialization.Serializable
import com.parentalguard.common.model.RuleSet
import com.parentalguard.common.model.DeviceStats
import com.parentalguard.common.model.DailyUsageReport

@Serializable
sealed class Packet {
    
    @Serializable
    data class Command(
        val commandType: CommandType, // Renamed from 'type' to avoid JSON discriminator conflict
        val ruleSet: RuleSet? = null,
        val unlockDurationMs: Long? = null, // For temporary unlocks
        val requestMessage: String? = null, // For unlock requests from child
        val approved: Boolean? = null, // For unlock approval/denial
        val deviceName: String? = null, // For device naming
        val includeIcons: Boolean? = null, // Request to include app icons (Base64)
        val packageName: String? = null,
        val category: com.parentalguard.common.model.AppCategory? = null,
        val timerDurationMs: Long? = null,
        val languageCode: String? = null,
        val relayParentId: String? = null,
        val enabled: Boolean? = null,
        val restrictionKey: String? = null,
        val blockingScreenStyle: com.parentalguard.common.model.BlockingScreenStyle? = null,
        val authToken: String? = null,
        val requestId: String? = null // Correlates relay responses to their originating command
    ) : Packet()

    @Serializable
    data class Response(
        val success: Boolean,
        val message: String? = null,
        val stats: DeviceStats? = null,
        val dailyReport: DailyUsageReport? = null,
        val requestId: String? = null // Echoed back from the originating command
    ) : Packet()

    @Serializable
    data class Event(
        val eventType: EventType, // Renamed from 'type' to avoid JSON discriminator conflict
        val payload: String? = null,
        val timestamp: Long = System.currentTimeMillis(),
        val deviceId: String? = null,          // ID of child device 
        // Unlock request details
        val deviceName: String? = null,        // Name of child device requesting unlock
        val requestType: String? = null,       // "DEVICE" or "APP"
        val appPackageName: String? = null,    // Package name for app-specific unlocks
        val appName: String? = null            // Display name for app-specific unlocks
    ) : Packet()
}

@Serializable
enum class EventType {
    BATTERY_CHANGED,
    APP_INSTALLED,
    APP_REMOVED,
    LOCK_STATUS_CHANGED,
    UNLOCK_REQUESTED,
    EXTENSION_REQUESTED
}

@Serializable
enum class CommandType {
    GET_STATS,
    UPDATE_RULES,
    LOCK_DEVICE,
    UNLOCK_DEVICE,
    PING,
    REQUEST_UNLOCK,      // Child requests temporary unlock
    APPROVE_UNLOCK,      // Parent approves unlock request
    DENY_UNLOCK,         // Parent denies unlock request
    SEND_DAILY_REPORT,   // Child sends daily usage report
    UPDATE_DEVICE_NAME,  // Parent updates device custom name
    UNHIDE_APP,          // Parent requests to unhide child app icon
    HIDE_APP,            // Parent requests to hide child app icon
    SET_APP_CATEGORY,    // Parent sets a category for an app
    SET_APP_TIMER,       // Parent sets a temporary timer for an app
    SET_CATEGORY_TIMER,  // Parent sets a temporary timer for a category
    SET_LANGUAGE,        // Parent sets the language
    RESET_PIN,           // Parent resets the child PIN
    SET_RELAY_PARENT_ID, // Parent informs child of its Relay ID
    APPROVE_EXTENSION,   // Parent approves "One More Minute"
    DENY_EXTENSION,      // Parent denies extension request
    STOP_BREAK,           // Parent manually stops a break session
    DEVICE_OWNER_SET_APP_SUSPENDED,
    DEVICE_OWNER_SET_UNINSTALL_PROTECTION,
    DEVICE_OWNER_SET_USER_RESTRICTION,
    DEVICE_OWNER_SET_DEVICE_LOCKED,
    DEVICE_OWNER_SET_DEVICE_USAGE_LIMIT,
    DEVICE_OWNER_SET_APP_USAGE_LIMIT,
    DEVICE_OWNER_SET_WIFI_ENABLED,
    DEVICE_OWNER_RESET_SCREEN_LOCK,
    RESET_REQUEST_COOLDOWN,
    SET_BLOCKING_SCREEN_STYLE
}

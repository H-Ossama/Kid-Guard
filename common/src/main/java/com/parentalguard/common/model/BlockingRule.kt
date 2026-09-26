package com.parentalguard.common.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppCategory {
    SOCIAL,      // Social media apps (Facebook, Instagram, WhatsApp, etc.)
    GAMES,       // Gaming apps
    EDUCATION,   // Educational apps
    PRODUCTIVITY,// Productivity apps (Office, Notes, etc.)
    ENTERTAINMENT, // Entertainment (YouTube, Netflix, etc.)
    SYSTEM,      // Built-in Android/system apps, hidden from the normal app list
    OTHER        // Uncategorized apps
}

@Serializable
data class TimeRange(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7) // 1 = Monday, 7 = Sunday
)

@Serializable
data class BlockingRule(
    val packageName: String,
    val maxDailyTimeMs: Long,
    val blockEndTime: Long = 0, // Timestamp when block expires. 0 if not blocked.
    val isPermanentlyBlocked: Boolean = false,
    val isInternetBlocked: Boolean = false,
    val category: AppCategory = AppCategory.OTHER,
    val isWhitelisted: Boolean = false, // If true, never block this app
    val schedule: List<TimeRange> = emptyList() // Periods when the app is blocked
)

@Serializable
data class CategoryLimit(
    val category: AppCategory,
    val maxDailyTimeMs: Long
)

@Serializable
data class RuleSet(
    val rules: List<BlockingRule>,
    val categoryLimits: List<CategoryLimit> = emptyList(),
    // Null = "leave the current value alone". A plain rules/category/break sync
    // must never wipe an active lock or an approved temporary unlock, so the
    // child only applies these when they carry an explicit future timestamp.
    val globalLockUntil: Long? = null, // Device-wide lock timestamp
    val temporaryUnlockUntil: Long? = null, // Temporary unlock timestamp
    val usageLimitMs: Long = 0, // Threshold for total device usage before break
    val breakDurationMs: Long = 0, // Duration of the forced break
    val breakWarningMs: Long = 0, // Minutes before break to show warning (0 = disabled)
    val educationOnly: Boolean = false, // If true, only block non-educational apps during break
    val allowExtensions: Boolean = false, // If true, child can request "One More Minute"
    val rollingUsageWindowMs: Long = 0 // Optional: window for usage calculation (e.g. 24h)
)

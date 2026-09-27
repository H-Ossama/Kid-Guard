package com.parentalguard.common.model

import kotlinx.serialization.Serializable

@Serializable
data class AppUsageLog(
    val packageName: String,
    val totalTimeInForeground: Long, // milliseconds
    val lastTimeUsed: Long, // timestamp
    val date: String, // YYYY-MM-DD for easy aggregation
    val category: AppCategory = AppCategory.OTHER,
    val appLabel: String? = null
)

/**
 * UsageStatsManager can return more than one record for a package. The UI and
 * command layer treat a package as one app, so merge those records first.
 */
fun List<AppUsageLog>.mergedByPackage(): List<AppUsageLog> =
    groupBy { it.packageName }
        .values
        .map { entries ->
            val latest = entries.maxByOrNull { it.lastTimeUsed } ?: return@map null
            latest.copy(
                totalTimeInForeground = entries.sumOf { it.totalTimeInForeground }
            )
        }
        .filterNotNull()
        .sortedByDescending { it.totalTimeInForeground }

@Serializable
data class CategoryUsage(
    val category: AppCategory,
    val totalTimeMs: Long,
    val appCount: Int
)

@Serializable
data class HourlyUsage(
    val hour: Int, // 0-23
    val usageTimeMs: Long
)

@Serializable
data class DailyUsageReport(
    val date: String, // YYYY-MM-DD
    val deviceName: String,
    val totalScreenTimeMs: Long,
    val appUsages: List<AppUsageLog>,
    val categoryUsages: List<CategoryUsage>,
    val mostUsedApps: List<AppUsageLog>, // Top 5 apps
    val hourlyBreakdown: List<HourlyUsage> = emptyList(),
    val blockedAttempts: Int = 0
)

@Serializable
data class DeviceStats(
    val batteryLevel: Int,
    val lastSeenTimestamp: Long,
    val usageLogs: List<AppUsageLog>,
    val installedApps: List<AppInfo> = emptyList(),
    val hourlyBreakdown: List<HourlyUsage> = emptyList(),
    val activeRules: List<BlockingRule> = emptyList(),
    val isLocked: Boolean = false,
    val isIconHidden: Boolean = false,
    val appTimers: Map<String, Long> = emptyMap(), // Map of PackageName -> ExpirationTimestamp
    val categoryTimers: Map<AppCategory, Long> = emptyMap(), // Map of Category -> ExpirationTimestamp
    val deviceName: String? = null, // Custom device name from child
    val usageLimitMs: Long = 0,
    val breakDurationMs: Long = 0,
    val breakWarningMs: Long = 0,
    val educationOnly: Boolean = false,
    val allowExtensions: Boolean = false,
    val lockReason: String? = null,
    val deviceOwnerCapabilities: DeviceOwnerCapabilities = DeviceOwnerCapabilities(),
    val blockingScreenStyle: BlockingScreenStyle? = null,
    val childAppVersionName: String? = null, // Child APK versionName (for parent-pushed updates)
    val childAppVersionCode: Int = 0, // Child APK versionCode (for parent-pushed updates)
    val childPinSet: Boolean = false, // Whether the child protection PIN is set
    val updateResult: String? = null, // Last parent-pushed install outcome ("OK x.y.z" / "FAIL ...")
    val pinProtectionEnabled: Boolean = true // Master PIN-gate switch, parent-controlled
)

@Serializable
data class AppInfo(
    val packageName: String,
    val label: String,
    val category: AppCategory = AppCategory.OTHER,
    val isSystem: Boolean = false,
    val iconBase64: String? = null // Base64 encoded icon
)

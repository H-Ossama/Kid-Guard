package com.parentalguard.child.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import android.content.Intent
import com.parentalguard.child.data.RuleRepository
import com.parentalguard.child.monitor.UsageMonitor
import com.parentalguard.child.notification.WarningNotificationManager
import com.parentalguard.common.utils.CategoryMapper
import android.net.Uri
import android.provider.Settings

class BlockerAccessibilityService : AccessibilityService() {
    
    private lateinit var usageMonitor: UsageMonitor

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("BlockerService", "Service Connected")
        usageMonitor = UsageMonitor(applicationContext)
        WarningNotificationManager.createNotificationChannel(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val packageName = event.packageName?.toString() ?: ""

                // While the device is locked, SystemUI (notification shade /
                // quick settings) renders ABOVE the overlay lock screen and
                // would otherwise offer Wi-Fi toggles and a Settings shortcut.
                // Collapse it immediately with BACK (best-effort on
                // non-Device-Owner devices; Device Owner locks it at OS level).
                if (packageName == "com.android.systemui" && isLockdownActive()) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    return
                }

                // If we are in Settings (App Info), handle the Force Stop clicks
                if (packageName == "com.android.settings") {
                    handleSettingsAutomation()
                    return
                }

                // Periodic check of all windows to catch PiP
                checkAllWindows()
            }
        }
    }

    private fun checkAllWindows() {
        val allWindows = windows
        if (allWindows.isEmpty()) return
        
        for (window in allWindows) {
            val root = window.root ?: continue
            val pkgName = root.packageName?.toString() ?: continue
            
            // Skip system UI, settings, and our own app
            if (pkgName == "com.android.systemui" || pkgName == "com.android.settings" || pkgName == packageName) {
                continue
            }
            
            // Re-use existing check logic
            checkAndBlock(pkgName)
        }
    }

    private fun handleSettingsAutomation() {
        val rootNode = rootInActiveWindow ?: return
        
        // Supported languages: English, French, Arabic
        val forceStopLabels = listOf(
            "Force stop", "FORCE STOP", 
            "Forcer l'arrêt", "FORCER L'ARRÊT",
            "فرض الإيقاف", "إيقاف إجباري"
        )
        
        val confirmLabels = listOf(
            "OK", "Ok", "موافق",
            "Force stop", "FORCE STOP", "Forcer l'arrêt", "إيقاف إجباري"
        )

        // 1. Look for Force Stop button
        for (label in forceStopLabels) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(label)
            for (node in nodes) {
                if (node.isClickable && node.isEnabled) {
                    Log.i("BlockerService", "Clicking Force Stop button: $label")
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
            }
        }
        
        // 2. Handle confirmation dialog
        for (label in confirmLabels) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(label)
            for (node in nodes) {
                if (node.isClickable && node.className?.contains("Button") == true) {
                    Log.i("BlockerService", "Confirming Force Stop: $label")
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
            }
        }
    }

    /**
     * True while any lockdown is in force, using only in-memory rule state
     * (no UsageStats queries — this runs on every window event).
     */
    private fun isLockdownActive(): Boolean {
        if (RuleRepository.globalLock.value) return true
        val now = System.currentTimeMillis()
        if (RuleRepository.rules.value.any { it.isPermanentlyBlocked || it.blockEndTime > now }) {
            return true
        }
        // An expired allowance timer means the app is currently re-blocked.
        if (RuleRepository.appTimers.value.any { (pkg, _) -> !RuleRepository.isAppTimerActive(pkg) }) {
            return true
        }
        if (RuleRepository.categoryTimers.value.any { (cat, _) -> !RuleRepository.isCategoryTimerActive(cat) }) {
            return true
        }
        return false
    }

    private fun checkAndBlock(packageName: String) {
        // 1. Check if app is whitelisted (Phone, SMS, Calculator, Settings, etc.)
        if (RuleRepository.isWhitelisted(packageName)) {
            Log.d("BlockerService", "$packageName is whitelisted, allowing access")
            return
        }
        
        // 2. Don't block the child agent app itself
        if (packageName == this.packageName) return
        
        // 3. Check for temporary unlock
        if (RuleRepository.isTemporarilyUnlocked()) {
            Log.d("BlockerService", "Temporary unlock active, allowing access")
            return
        }
        
        // 4. Check global lock
        val isGloballyLocked = RuleRepository.globalLock.value
        if (isGloballyLocked) {
             Log.i("BlockerService", "Global Lock Active. Blocking $packageName")
             forceStopApp(packageName)
             showBlockingOverlay(packageName)
             return
        }

        // 5. Check app-specific rules
        val rule = RuleRepository.getRuleForPackage(packageName)
        if (rule != null) {
            val shouldBlock = rule.isPermanentlyBlocked || 
                              (rule.blockEndTime > System.currentTimeMillis()) ||
                              isWithinSchedule(rule.schedule)
            
            if (shouldBlock) {
                Log.i("BlockerService", "Blocking $packageName (Schedule or Manual)")
                forceStopApp(packageName)
                showBlockingOverlay(packageName)
                return
            }
            
            // Check time limit and show warning if needed
            if (rule.maxDailyTimeMs > 0) {
                val currentUsage = usageMonitor.getAppUsageToday(packageName)
                if (currentUsage >= rule.maxDailyTimeMs) {
                    Log.i("BlockerService", "Time limit exceeded for $packageName")
                    forceStopApp(packageName)
                    showBlockingOverlay(packageName)
                    return
                }
                
                // Show warning if approaching limit
                if (usageMonitor.shouldShowWarning(packageName, rule.maxDailyTimeMs)) {
                    val identifier = "app_$packageName"
                    if (!RuleRepository.hasWarningBeenShown(identifier)) {
                        val remainingMs = rule.maxDailyTimeMs - currentUsage
                        val remainingMinutes = (remainingMs / 60000).toInt()
                        WarningNotificationManager.showWarning(
                            applicationContext,
                            packageName.substringAfterLast("."),
                            remainingMinutes
                        )
                        RuleRepository.markWarningShown(identifier)
                    }
                }
            }
        }
        
        // 6. Check category-based limits
        val category = CategoryMapper.getCategoryForPackage(packageName)
        val categoryLimit = RuleRepository.getCategoryLimit(category)
        if (categoryLimit != null && categoryLimit.maxDailyTimeMs > 0) {
            val currentUsage = usageMonitor.getCategoryUsageToday(category)
            if (currentUsage >= categoryLimit.maxDailyTimeMs) {
                Log.i("BlockerService", "Category limit exceeded for $category")
                forceStopApp(packageName)
                showBlockingOverlay(packageName, isCategory = true, categoryName = category.name)
                return
            }
            
            // Show warning if approaching category limit
            if (usageMonitor.shouldShowCategoryWarning(category, categoryLimit.maxDailyTimeMs)) {
                val identifier = "category_${category.name}"
                if (!RuleRepository.hasWarningBeenShown(identifier)) {
                    val remainingMs = categoryLimit.maxDailyTimeMs - currentUsage
                    val remainingMinutes = (remainingMs / 60000).toInt()
                    WarningNotificationManager.showWarning(
                        applicationContext,
                        category.name,
                        remainingMinutes
                    )
                    RuleRepository.markWarningShown(identifier)
                }
            }
        }
    }

    private fun forceStopApp(packageName: String) {
        try {
            // First send to home to minimize PiP if possible
            performGlobalAction(GLOBAL_ACTION_HOME)
            
            // Then launch App Info to trigger automation
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("BlockerService", "Failed to launch app info for $packageName", e)
        }
    }

    private fun showBlockingOverlay(
        packageName: String, 
        isCategory: Boolean = false, 
        categoryName: String? = null
    ) {
        // We will delegate to a separate UI helper or broadcast to the service to show a view
        // For simplicity, let's just toast and loop Home for now, or launch a BlockingActivity
        // Ideally, we use WindowManager to add a view directly.
        
        try {
             val intent = android.content.Intent(this, com.parentalguard.child.ui.BlockingActivity::class.java).apply {
                 addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                 putExtra("PACKAGE_NAME", packageName)
                 putExtra("IS_CATEGORY", isCategory)
                 putExtra("CATEGORY_NAME", categoryName)
             }
             startActivity(intent)
        } catch (e: Exception) {
            Log.e("BlockerService", "Failed to start blocking activity", e)
        }
    }

    private fun isWithinSchedule(schedule: List<com.parentalguard.common.model.TimeRange>): Boolean {
        if (schedule.isEmpty()) return false
        
        val now = java.util.Calendar.getInstance()
        val dayOfWeek = when (now.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            java.util.Calendar.SUNDAY -> 7
            else -> 1
        }
        val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = now.get(java.util.Calendar.MINUTE)
        val currentMinutes = hour * 60 + minute

        return schedule.any { range ->
            if (!range.daysOfWeek.contains(dayOfWeek)) return@any false
            
            val startMinutes = range.startHour * 60 + range.startMinute
            val endMinutes = range.endHour * 60 + range.endMinute
            
            if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else {
                // Overnight schedule (e.g., 22:00 to 06:00)
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }
        }
    }

    override fun onInterrupt() {
        Log.w("BlockerService", "Service Interrupted")
    }
}

package com.parentalguard.child.network

import android.content.Context
import com.parentalguard.child.data.RuleRepository
import com.parentalguard.child.monitor.UsageMonitor
import com.parentalguard.child.policy.DeviceOwnerManager
import com.parentalguard.child.utils.DeviceUtils
import com.parentalguard.common.model.CapabilityState
import com.parentalguard.common.model.DeviceStats
import com.parentalguard.common.network.CommandType
import com.parentalguard.common.network.Packet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single dispatch point for parent-issued [Packet.Command]s, shared by every
 * transport (Ktor HTTP routes, cloud relay WebSocket, Bluetooth RFCOMM) so the
 * behaviour is identical regardless of how the command arrived.
 */
object CommandDispatcher {

    suspend fun dispatch(context: Context, command: Packet.Command): Packet.Response {
        return withContext(Dispatchers.IO) {
            try {
                val response = when (command.commandType) {
                    CommandType.GET_STATS -> {
                        val monitor = UsageMonitor(context)
                        val p = context.packageManager
                        val componentName = android.content.ComponentName(context, "com.parentalguard.child.MainActivity")
                        val isIconHidden = try {
                            p.getComponentEnabledSetting(componentName) == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        } catch (e: Exception) {
                            false
                        }

                        Packet.Response(true, stats = DeviceStats(
                            batteryLevel = DeviceUtils.getBatteryLevel(context),
                            lastSeenTimestamp = System.currentTimeMillis(),
                            usageLogs = monitor.getTodayUsage(),
                            installedApps = DeviceUtils.getInstalledApps(context, command.includeIcons ?: false),
                            hourlyBreakdown = monitor.getHourlyBreakdown(),
                            activeRules = RuleRepository.rules.value,
                            isLocked = RuleRepository.globalLock.value,
                            isIconHidden = isIconHidden,
                            appTimers = RuleRepository.appTimers.value,
                            categoryTimers = RuleRepository.categoryTimers.value,
                            deviceName = DeviceUtils.getDeviceName(context),
                            usageLimitMs = RuleRepository.usageLimitMs.value,
                             breakDurationMs = RuleRepository.breakDurationMs.value,
                             deviceOwnerCapabilities = DeviceOwnerManager.capabilities(context),
                             blockingScreenStyle = RuleRepository.blockingScreenStyle.value,
                             childAppVersionName = runCatching { com.parentalguard.child.BuildConfig.VERSION_NAME }.getOrNull(),
                             childAppVersionCode = runCatching { com.parentalguard.child.BuildConfig.VERSION_CODE }.getOrDefault(0)
                          ))
                     }

                    CommandType.SEND_DAILY_REPORT -> {
                        Packet.Response(
                            true,
                            dailyReport = UsageMonitor(context).generateDailyReport()
                        )
                    }

                    CommandType.LOCK_DEVICE -> {
                        RuleRepository.setGlobalLock(true)
                        Packet.Response(true, "Locked")
                    }

                    CommandType.UNLOCK_DEVICE -> {
                        RuleRepository.setGlobalLock(false)
                        Packet.Response(true, "Unlocked")
                    }

                    CommandType.UPDATE_RULES -> {
                        if (command.ruleSet != null) {
                            RuleRepository.updateRules(command.ruleSet!!.rules)
                            RuleRepository.updateCategoryLimits(command.ruleSet!!.categoryLimits)
                            // Same guard as the HTTP route: null/0 lock timers in a
                            // plain sync must not release an active lock or wipe an
                            // approved temporary unlock.
                            command.ruleSet!!.temporaryUnlockUntil?.takeIf { it > 0 }?.let {
                                RuleRepository.setTemporaryUnlock(it)
                            }
                            command.ruleSet!!.globalLockUntil?.takeIf { it > 0 }?.let {
                                RuleRepository.setGlobalLockUntil(it)
                            }
                            RuleRepository.setBreakRules(
                                command.ruleSet!!.usageLimitMs,
                                command.ruleSet!!.breakDurationMs,
                                command.ruleSet!!.breakWarningMs,
                                command.ruleSet!!.educationOnly,
                                command.ruleSet!!.allowExtensions
                            )
                            Packet.Response(true, "Rules updated")
                        } else {
                            Packet.Response(false, "Invalid rule set")
                        }
                    }

                    CommandType.SET_BLOCKING_SCREEN_STYLE -> {
                        val style = command.blockingScreenStyle
                        if (style != null) {
                            RuleRepository.setBlockingScreenStyle(style)
                            Packet.Response(true, "Blocked-screen style updated")
                        } else {
                            Packet.Response(false, "Invalid blocking-screen style")
                        }
                    }

                    CommandType.UPDATE_DEVICE_NAME -> {
                        if (command.deviceName != null) {
                            DeviceUtils.setCustomDeviceName(context, command.deviceName!!)
                            Packet.Response(true, "Device renamed")
                        } else {
                            Packet.Response(false, "Invalid device name")
                        }
                    }

                    CommandType.SET_APP_CATEGORY -> {
                        if (command.packageName != null && command.category != null) {
                            RuleRepository.setCustomCategory(command.packageName!!, command.category!!)
                            Packet.Response(true, "Category updated")
                        } else {
                            Packet.Response(false, "Invalid category data")
                        }
                    }

                    CommandType.SET_APP_TIMER -> {
                        if (command.packageName != null && command.timerDurationMs != null) {
                            RuleRepository.setAppTimer(command.packageName!!, command.timerDurationMs!!)
                            Packet.Response(true, "App timer set")
                        } else {
                            Packet.Response(false, "Invalid timer data")
                        }
                    }

                    CommandType.SET_CATEGORY_TIMER -> {
                        if (command.category != null && command.timerDurationMs != null) {
                            RuleRepository.setCategoryTimer(command.category!!, command.timerDurationMs!!)
                            Packet.Response(true, "Category timer set")
                        } else {
                            Packet.Response(false, "Invalid timer data")
                        }
                    }

                    CommandType.HIDE_APP -> {
                        setAppVisibility(context, false)
                        Packet.Response(true, "App hidden")
                    }

                    CommandType.UNHIDE_APP -> {
                        setAppVisibility(context, true)
                        Packet.Response(true, "App unhidden")
                    }

                    CommandType.APPROVE_UNLOCK -> {
                        val duration = command.unlockDurationMs ?: (10 * 60 * 1000L)
                        if (command.packageName != null) {
                            RuleRepository.setAppTimer(command.packageName!!, duration)
                            Packet.Response(true, "App unlock approved")
                        } else {
                            val unlockUntil = System.currentTimeMillis() + duration
                            RuleRepository.setTemporaryUnlock(unlockUntil)
                            Packet.Response(true, "Device unlock approved")
                        }
                    }

                    CommandType.DENY_UNLOCK -> {
                        Packet.Response(true, "Unlock denied")
                    }

                    CommandType.APPROVE_EXTENSION -> {
                        if (RuleRepository.globalLock.value) {
                            val newUntil = RuleRepository.globalLockUntil.value + 60_000
                            RuleRepository.setGlobalLockUntil(newUntil, RuleRepository.lockReason.value)
                        }
                        Packet.Response(true, "Extension approved (+1 min)")
                    }

                    CommandType.DENY_EXTENSION -> {
                        Packet.Response(true, "Extension denied")
                    }

                    CommandType.STOP_BREAK -> {
                        RuleRepository.setGlobalLock(false)
                        Packet.Response(true, "Break stopped")
                    }

                    CommandType.RESET_PIN -> {
                        com.parentalguard.child.security.PinManager.resetPin(context)
                        Packet.Response(true, "PIN reset successfully")
                    }

                    CommandType.SET_LANGUAGE -> {
                        if (command.languageCode != null) {
                            updateLanguage()
                            Packet.Response(true, "Language updated")
                        } else {
                            Packet.Response(false, "Invalid language code")
                        }
                    }

                    CommandType.SET_RELAY_PARENT_ID -> {
                        if (command.relayParentId != null) {
                            val prefs = context.getSharedPreferences("relay_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("parent_id", command.relayParentId).apply()
                            Packet.Response(true, "Relay Parent ID updated")
                        } else {
                            Packet.Response(false, "Invalid relay parent ID")
                        }
                    }

                    CommandType.DEVICE_OWNER_SET_APP_SUSPENDED -> {
                        val packageName = command.packageName
                        val suspended = command.enabled
                        if (packageName == null || suspended == null) {
                            Packet.Response(false, "Invalid app suspension command")
                        } else {
                            val result = DeviceOwnerManager.setAppSuspended(context, packageName, suspended)
                            Packet.Response(result.success, result.message)
                        }
                    }

                    CommandType.DEVICE_OWNER_SET_UNINSTALL_PROTECTION -> {
                        val packageName = command.packageName
                        val enabled = command.enabled
                        if (packageName == null || enabled == null) {
                            Packet.Response(false, "Invalid uninstall protection command")
                        } else {
                            val result = DeviceOwnerManager.setUninstallProtection(context, packageName, enabled)
                            Packet.Response(result.success, result.message)
                        }
                    }

                    CommandType.DEVICE_OWNER_SET_USER_RESTRICTION -> {
                        val restrictionKey = command.restrictionKey
                        val enabled = command.enabled
                        if (restrictionKey == null || enabled == null) {
                            Packet.Response(false, "Invalid user restriction command")
                        } else {
                            val result = DeviceOwnerManager.setUserRestriction(context, restrictionKey, enabled)
                            Packet.Response(result.success, result.message)
                        }
                    }

                    CommandType.DEVICE_OWNER_SET_DEVICE_LOCKED -> {
                        val locked = command.enabled
                        if (locked == null) {
                            Packet.Response(false, "Invalid device lock command")
                        } else {
                            val result = DeviceOwnerManager.setDeviceLocked(context, locked)
                            Packet.Response(result.success, result.message)
                        }
                    }

                    CommandType.DEVICE_OWNER_SET_DEVICE_USAGE_LIMIT -> {
                        val limitMs = command.timerDurationMs
                        if (limitMs == null || limitMs < 0) {
                            Packet.Response(false, "Invalid device usage limit")
                        } else if (DeviceOwnerManager.capabilities(context).deviceUsageLimits != CapabilityState.AVAILABLE) {
                            Packet.Response(false, "UNAVAILABLE: Device Owner usage access is not ready")
                        } else {
                            RuleRepository.setDeviceOwnerDeviceUsageLimit(limitMs)
                            Packet.Response(true, "Device usage limit updated")
                        }
                    }

                    CommandType.DEVICE_OWNER_SET_APP_USAGE_LIMIT -> {
                        val packageName = command.packageName
                        val limitMs = command.timerDurationMs
                        if (packageName == null || limitMs == null || limitMs < 0) {
                            Packet.Response(false, "Invalid app usage limit")
                        } else if (!DeviceOwnerManager.isValidManagedPackage(context, packageName)) {
                            Packet.Response(false, "Invalid managed app package")
                        } else if (DeviceOwnerManager.capabilities(context).appUsageLimits != CapabilityState.AVAILABLE) {
                            Packet.Response(false, "UNAVAILABLE: Device Owner usage access is not ready")
                        } else {
                            RuleRepository.setDeviceOwnerAppUsageLimit(packageName, limitMs)
                            Packet.Response(true, "App usage limit updated")
                        }
                    }

                    CommandType.DEVICE_OWNER_SET_WIFI_ENABLED -> {
                        val enabled = command.enabled
                        if (enabled == null) {
                            Packet.Response(false, "Invalid Wi-Fi command")
                        } else {
                            val result = DeviceOwnerManager.setWifiEnabled(context, enabled)
                            Packet.Response(result.success, result.message)
                        }
                    }

                    else -> Packet.Response(false, "Command not implemented: ${command.commandType}")
                }
                response.copy(requestId = command.requestId)
            } catch (e: Exception) {
                Packet.Response(false, "Error: ${e.message}", requestId = command.requestId)
            }
        }
    }

    private fun setAppVisibility(context: Context, visible: Boolean) {
        val p = context.packageManager
        val componentName = android.content.ComponentName(context, "com.parentalguard.child.MainActivity")
        p.setComponentEnabledSetting(
            componentName,
            if (visible) android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            android.content.pm.PackageManager.DONT_KILL_APP
        )
    }

    private fun updateLanguage() {
        // The child app follows the device locale through Android resource qualifiers.
        // Language commands from the controller are intentionally ignored.
    }
}

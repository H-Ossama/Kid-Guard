package com.parentalguard.child.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.parentalguard.child.data.RuleRepository
import com.parentalguard.child.policy.DeviceOwnerManager
import com.parentalguard.child.service.BootRescueJobService
import com.parentalguard.child.service.MonitorService

/**
 * Restarts protection after reboot / shutdown / app update.
 *
 * Why this exists: on Android 12+ `startForegroundService()` from a boot
 * receiver is a *background* start and the system can refuse it
 * (`ForegroundServiceStartNotAllowedException`), which used to leave the
 * child with NO service running — a parent-set lock silently disappeared
 * after every reboot. So this receiver now does three independent things:
 *
 * 1. Tries to start [MonitorService] (works on older Android + whenever the
 *    system allows it), never crashing if it is refused.
 * 2. Re-issues the Device Owner hard lock immediately when the persisted
 *    state says the device must stay locked. This is a plain
 *    DevicePolicyManager call — it needs no service and cannot be refused.
 * 3. Schedules the persisted rescue/watchdog job, which retries the service
 *    start and re-applies the lock even if this receiver's start was refused.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != Intent.ACTION_USER_UNLOCKED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        RuleRepository.initialize(context)

        // Escrow the screen-lock reset token as soon as storage is available.
        // USER_UNLOCKED is the key trigger: at BOOT_COMPLETED the credential
        // storage may still be locked, which makes the escrow silently fail.
        if (action == Intent.ACTION_USER_UNLOCKED) {
            runCatching { DeviceOwnerManager.escrowResetPasswordToken(context) }
                .onFailure { Log.w("BootReceiver", "Token escrow deferred", it) }
        }

        // Persisted rescue: retries the service start + re-applies the lock.
        runCatching { BootRescueJobService.schedule(context) }
            .onFailure { Log.w("BootReceiver", "Cannot schedule rescue job", it) }

        // Hard re-lock: works with zero services running (Device Owner only).
        if (RuleRepository.globalLock.value && DeviceOwnerManager.isDeviceOwner(context)) {
            val result = runCatching { DeviceOwnerManager.lockNow(context) }.getOrNull()
            Log.i("BootReceiver", "Re-applied boot lock: ${result?.message}")
        }

        // Normal path: start the monitoring service (may be refused on API 31+).
        try {
            val serviceIntent = Intent(context, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException on Android 12+: the
            // rescue job above will retry shortly. Never crash the receiver.
            Log.w("BootReceiver", "Service start refused at boot, rescue job scheduled", e)
        }
    }
}

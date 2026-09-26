package com.parentalguard.child.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.parentalguard.child.data.RuleRepository
import com.parentalguard.child.policy.DeviceOwnerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Persisted rescue + watchdog for protection across reboots.
 *
 * - One-shot rescue job (fires within ~1 min of boot): retries starting
 *   [MonitorService] and re-applies the Device Owner hard lock when the
 *   persisted state says the device must stay locked.
 * - Periodic watchdog (every 15 min, survives reboots): if the service died
 *   (killed, crashed, Samsung optimized it away) it restarts it, and if the
 *   device must be locked it re-issues the hard lock immediately.
 *
 * JobScheduler jobs are the reliable boot primitive here: they are persisted
 * across reboots and their execution is initiated by the system, unlike a
 * `startForegroundService()` call from a background receiver which Android
 * 12+ can refuse.
 */
class BootRescueJobService : JobService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        scope.launch {
            try {
                RuleRepository.initialize(applicationContext)

                // The persisted lock survives reboot — re-enforce it first,
                // before anything else, so there is no unlocked window.
                if (RuleRepository.globalLock.value &&
                    DeviceOwnerManager.isDeviceOwner(applicationContext)
                ) {
                    runCatching { DeviceOwnerManager.lockNow(applicationContext) }
                        .onSuccess { Log.i(TAG, "Watchdog re-applied lock: ${it.message}") }
                        .onFailure { Log.w(TAG, "Watchdog cannot re-apply lock", it) }
                }

                if (!MonitorService.isRunning) {
                    Log.i(TAG, "MonitorService not running, (re)starting it")
                    try {
                        val intent = Intent(applicationContext, MonitorService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            applicationContext.startForegroundService(intent)
                        } else {
                            applicationContext.startService(intent)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Watchdog service start refused, will retry next run", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Rescue job failed", e)
            } finally {
                jobFinished(params, false)
            }
        }
        return true // work continues on the coroutine
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true // reschedule if interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "BootRescue"
        private const val JOB_ID_RESCUE = 1001
        private const val JOB_ID_WATCHDOG = 1002
        private const val WATCHDOG_INTERVAL_MS = 15 * 60 * 1000L

        /** Schedules both the one-shot rescue and the periodic watchdog. Idempotent. */
        fun schedule(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            val component = ComponentName(context, BootRescueJobService::class.java)

            scheduler.schedule(
                JobInfo.Builder(JOB_ID_RESCUE, component)
                    .setOverrideDeadline(60_000L)
                    .setPersisted(true)
                    .build()
            )
            scheduler.schedule(
                JobInfo.Builder(JOB_ID_WATCHDOG, component)
                    .setPeriodic(WATCHDOG_INTERVAL_MS)
                    .setPersisted(true)
                    .build()
            )
        }
    }
}

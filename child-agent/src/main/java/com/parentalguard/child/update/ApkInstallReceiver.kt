package com.parentalguard.child.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

/**
 * Receives the [PackageInstaller] session-commit status for parent-pushed
 * updates. On success the system replaces the app (process is killed);
 * on failure we only log — the parent sees the failure via the /update
 * response or a retry.
 */
class ApkInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ApkInstaller.ACTION_INSTALL_COMMIT) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Not Device Owner: bring the confirmation UI to the front.
                val confirm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                }
                try {
                    confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirm)
                } catch (e: Exception) {
                    Log.e("ApkInstallReceiver", "Cannot show install confirmation", e)
                }
            }
            PackageInstaller.STATUS_SUCCESS ->
                Log.i("ApkInstallReceiver", "Child update installed")
            else ->
                Log.e("ApkInstallReceiver", "Child update failed ($status): $message")
        }
    }
}

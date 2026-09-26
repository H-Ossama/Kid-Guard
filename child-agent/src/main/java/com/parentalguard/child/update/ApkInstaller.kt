package com.parentalguard.child.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import com.parentalguard.child.policy.DeviceOwnerManager
import java.io.File

/**
 * Installs a parent-pushed child APK via [PackageInstaller].
 *
 * - Device Owner: the session commits silently, no taps needed on the child.
 * - Not owner: Android shows the normal install-confirmation screen on the
 *   child device (requires REQUEST_INSTALL_PACKAGES + "Install unknown apps"
 *   allowed for this app). The parent already warned about this in its UI.
 */
object ApkInstaller {
    const val ACTION_INSTALL_COMMIT = "com.parentalguard.child.UPDATE_INSTALL_COMMIT"
    private const val TAG = "ApkInstaller"
    private const val MIN_APK_BYTES = 1_000_000L

    sealed interface Result {
        data object Accepted : Result
        data class Rejected(val reason: String) : Result
    }

    fun installApk(context: Context, apkFile: File): Result {
        if (!apkFile.exists() || apkFile.length() < MIN_APK_BYTES) {
            return Result.Rejected("APK file is missing or truncated")
        }
        // Basic APK sanity: ZIP magic "PK".
        try {
            apkFile.inputStream().use { input ->
                val magic = ByteArray(2)
                if (input.read(magic) != 2 || magic[0] != 0x50.toByte() || magic[1] != 0x4B.toByte()) {
                    return Result.Rejected("File is not a valid APK")
                }
            }
        } catch (e: Exception) {
            return Result.Rejected("Cannot read APK: ${e.message}")
        }

        val silent = DeviceOwnerManager.isDeviceOwner(context)
        Log.i(TAG, "Installing ${apkFile.length()} bytes (silent=$silent)")

        return try {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)
            session.use { s ->
                s.openWrite("update", 0, apkFile.length()).use { out ->
                    apkFile.inputStream().use { input -> input.copyTo(out) }
                    s.fsync(out)
                }
                val commitIntent = Intent(context, ApkInstallReceiver::class.java).apply {
                    action = ACTION_INSTALL_COMMIT
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                val pending = PendingIntent.getBroadcast(context, sessionId, commitIntent, flags)
                s.commit(pending.intentSender)
            }
            Result.Accepted
        } catch (e: SecurityException) {
            Log.e(TAG, "Install rejected", e)
            Result.Rejected("System rejected the install: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Install failed", e)
            Result.Rejected(e.message ?: "install failed")
        }
    }
}

package com.parentalguard.child.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.app.PendingIntent
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.parentalguard.child.data.RuleRepository
import com.parentalguard.child.R
import android.content.Context

class InternetBlockerService : VpnService() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    
    private val blockedPackages = mutableSetOf<String>()

    companion object {
        const val ACTION_START = "com.parentalguard.child.action.START_VPN"
        const val ACTION_STOP = "com.parentalguard.child.action.STOP_VPN"
        const val ACTION_UPDATE = "com.parentalguard.child.action.UPDATE_VPN"
        const val NOTIFICATION_ID = 2
        const val CHANNEL_ID = "internet_blocker_channel"

        fun start(context: Context) {
            try {
                val intent = Intent(context, InternetBlockerService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Background start refused (e.g. right after boot): the VPN
                // re-syncs on the next foreground start via syncVpn().
                android.util.Log.w("InternetBlocker", "VPN start deferred (background)", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, InternetBlockerService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                android.util.Log.w("InternetBlocker", "VPN stop deferred (background)", e)
            }
        }

        fun update(context: Context) {
            try {
                val intent = Intent(context, InternetBlockerService::class.java).apply {
                    action = ACTION_UPDATE
                }
                context.startService(intent)
            } catch (e: Exception) {
                android.util.Log.w("InternetBlocker", "VPN update deferred (background)", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("InternetBlocker", "onStartCommand action: ${intent?.action}")
         when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
            ACTION_UPDATE -> updateBlockedApps()
        }
        return START_STICKY
    }

    private fun startVpn() {
        Log.d("InternetBlocker", "startVpn() called. isRunning: $isRunning")
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        isRunning = true
        
        scope.launch {
            try {
                // Load blocked apps from RuleRepository
                val rules = RuleRepository.rules.value
                blockedPackages.clear()
                blockedPackages.addAll(rules.filter { it.isInternetBlocked }.map { it.packageName })
                
                Log.d("InternetBlocker", "Blocked Target List: ${blockedPackages.joinToString(", ")}")

                // If no apps are blocked, we don't necessarily stop the VPN, 
                // but we keep it running as a sinkhole if the parent wants it.
                // However, for battery efficiency, if empty, we could stop.
                // For now, let's establish even if empty to ensure the VPN status is "Connected".

                // Close existing interface if any before establishing new one
                vpnInterface?.close()
                vpnInterface = null

                val builder = Builder()
                    .setSession("ParentalGuard")
                    .addAddress("10.0.0.1", 24)
                    .addRoute("0.0.0.0", 0)
                    .addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)
                    .addRoute("::", 0)
                    .setMtu(65535)
                    .setBlocking(false)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    builder.setMetered(false)
                }
                
                blockedPackages.forEach { packageName ->
                    try {
                        Log.d("InternetBlocker", "Blocking: $packageName")
                        builder.addAllowedApplication(packageName)
                    } catch (e: Exception) {
                        Log.e("InternetBlocker", "Failed to add allowed application: $packageName", e)
                    }
                }
                
                Log.d("InternetBlocker", "Establishing VPN interface...")
                vpnInterface = builder.establish()
                
                if (vpnInterface != null) {
                    Log.i("InternetBlocker", "VPN Interface established successfully.")
                } else {
                    Log.e("InternetBlocker", "Failed to establish VPN interface.")
                    stopVpn()
                }
                
            } catch (e: Exception) {
                Log.e("InternetBlocker", "Error starting VPN", e)
                stopVpn()
            }
        }
    }
    
    private fun stopVpn(stopService: Boolean = true) {
        Log.i("InternetBlocker", "Stopping VPN interface... (stopService=$stopService)")
        if (stopService) isRunning = false
        
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (stopService) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    
    private fun updateBlockedApps() {
        if (isRunning) {
            stopVpn(stopService = false)
            startVpn()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Internet Blocker Status",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.internet_blocker_title))
            .setContentText(getString(R.string.internet_control_active))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
        job.cancel()
    }
}

package com.parentalguard.child

import android.app.AppOpsManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.parentalguard.child.ui.OnboardingActivity
import com.parentalguard.child.ui.screens.MainScreen
import com.parentalguard.child.ui.screens.PinGateMode
import com.parentalguard.child.ui.screens.PinGateScreen
import com.parentalguard.child.ui.theme.ParentalGuardTheme
import com.parentalguard.child.service.MonitorService

import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val connectionString = mutableStateOf("")
    private val status = mutableStateOf("")
    private val qrBitmap = mutableStateOf<Bitmap?>(null)
    private val deviceName = mutableStateOf("")
    // Protection PIN gate: settings are hidden until the PIN is set + verified.
    // The parent can clear a forgotten PIN remotely ("Reset child PIN").
    private val pinSet = mutableStateOf(false)
    private val pinUnlocked = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check permissions and redirect to Onboarding if needed
        if (!hasUsageStatsPermission() || !hasOverlayPermission()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        
        requestBluetoothPermissions()
        
        status.value = getString(R.string.status_initializing)
        refreshPinState()

        setContent {
            ParentalGuardTheme {
                when {
                    !pinSet.value -> PinGateScreen(
                        mode = PinGateMode.SETUP,
                        onSetupComplete = { pin ->
                            if (com.parentalguard.child.security.PinManager.setPin(this, pin)) {
                                Toast.makeText(this, getString(R.string.pin_set_success), Toast.LENGTH_SHORT).show()
                                refreshPinState()
                                pinUnlocked.value = true
                            }
                        },
                        onVerify = { false }
                    )
                    !pinUnlocked.value -> PinGateScreen(
                        mode = PinGateMode.VERIFY,
                        onSetupComplete = {},
                        onVerify = { pin ->
                            val ok = com.parentalguard.child.security.PinManager.verifyPin(this, pin)
                            if (ok) pinUnlocked.value = true
                            ok
                        },
                        onRemovePin = {
                            com.parentalguard.child.security.PinManager.resetPin(this)
                            refreshPinState()
                            pinUnlocked.value = true
                            Toast.makeText(this, getString(R.string.pin_removed), Toast.LENGTH_SHORT).show()
                        }
                    )
                    else -> MainScreen(
                        connectionString = connectionString.value,
                        status = status.value,
                        qrBitmap = qrBitmap.value,
                        deviceName = deviceName.value,
                        onRequestUnlock = { requestTemporaryUnlock() },
                        onHideIcon = { hideLauncherIcon() },
                        onRenameDevice = { showRenameDialog() }
                    )
                }
            }
        }
        
        // Start service explicitly on open
        val serviceIntent = Intent(this, MonitorService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        updateConnectionInfo()
    }

    override fun onResume() {
        super.onResume()
        // Re-read PIN state: a remote parent reset while we were away must
        // drop back to setup instead of leaving a stale gate behind.
        refreshPinState()
        requestBluetoothDiscoverable()
    }

    private fun refreshPinState() {
        pinSet.value = com.parentalguard.child.security.PinManager.isPinSet(this)
        if (!pinSet.value) pinUnlocked.value = false
    }

    private fun updateConnectionInfo() {
        val ip = getLocalIpAddress()
        val port = 8080
        val deviceId = com.parentalguard.child.utils.DeviceUtils.getDeviceId(this)
        // Use the DeviceUtils helper that accounts for custom name
        val name = com.parentalguard.child.utils.DeviceUtils.getDeviceName(this)
        deviceName.value = name
        
        if (ip != null) {
            // New pairing format: deviceId|ip:port|deviceName|bluetoothName|pairToken
            val btName = com.parentalguard.common.network.BluetoothConfig.bluetoothNameFor(deviceId)
            val pairToken = com.parentalguard.child.network.PairingManager.getOrCreateToken(this)
            val connStr = "$deviceId|$ip:$port|$name|$btName|$pairToken"
            connectionString.value = "$ip:$port" // Display only IP:Port for simplicity
            status.value = getString(R.string.status_running)
            
            val bitmap = com.parentalguard.child.ui.QRCodeGenerator.generateQRCode(connStr, 512, 512)
            qrBitmap.value = bitmap
        } else {
            connectionString.value = getString(R.string.address_unavailable)
            status.value = getString(R.string.status_no_network)
        }
    }

    private fun showRenameDialog() {
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT
        input.setText(deviceName.value)
        
        // Add some padding
        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 50
        params.rightMargin = 50
        input.layoutParams = params
        container.addView(input)

        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.rename_device_title))
            .setView(container)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    com.parentalguard.child.utils.DeviceUtils.setCustomDeviceName(this, newName)
                    Toast.makeText(this, getString(R.string.device_renamed, newName), Toast.LENGTH_SHORT).show()
                    updateConnectionInfo() // Refresh UI
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces().toList()
            
            // Prioritize standard WiFi interface names (wlan0, eth0)
            val sortedIfaces = interfaces.sortedByDescending { iface ->
                val name = iface.name.lowercase()
                when {
                    name.contains("wlan") -> 3
                    name.contains("eth") -> 2
                    else -> 1
                }
            }

            for (iface in sortedIfaces) {
                if (iface.isLoopback || !iface.isUp) continue
                
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    private fun hasOverlayPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this)
        }
        return true
    }

    private fun requestBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            requestBluetoothDiscoverable()
            return
        }
        val needed = listOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_ADVERTISE
        ).filter {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            requestPermissions(needed.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
        } else {
            requestBluetoothDiscoverable()
        }
    }

    private fun requestBluetoothDiscoverable() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val hasConnect = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasAdvertise = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_ADVERTISE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasConnect || !hasAdvertise) return
        }
        val adapter = runCatching { BluetoothAdapter.getDefaultAdapter() }.getOrNull() ?: return
        if (!adapter.isEnabled) return
        // A bonded parent connects directly by MAC address — the tablet does
        // not need to be discoverable. Once paired, never ask again.
        val bondedCount = runCatching { adapter.bondedDevices?.size ?: 0 }.getOrDefault(0)
        if (bondedCount > 0) return
        val discoverable = runCatching {
            adapter.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
        }.getOrDefault(false)
        if (!discoverable) {
            startActivity(
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600)
                }
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS &&
            grantResults.isNotEmpty() &&
            grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
        ) {
            requestBluetoothDiscoverable()
            val serviceIntent = Intent(this, MonitorService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun hideLauncherIcon() {
        val p = packageManager
        val componentName = android.content.ComponentName(this, MainActivity::class.java)
        p.setComponentEnabledSetting(
            componentName,
            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            0 // Don't use DONT_KILL_APP to ensure the launcher refreshes
        )
        Toast.makeText(this, getString(R.string.toast_icon_hidden), Toast.LENGTH_LONG).show()
        
        // Kill the app completely so the launcher updates
        finishAffinity()
        System.exit(0)
    }
    
    private fun requestTemporaryUnlock() {
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.request_unlock))
            .setMessage(getString(R.string.request_unlock_message))
            .setPositiveButton(getString(R.string.approve)) { _, _ ->
                com.parentalguard.child.utils.EventHelper.sendUnlockRequest(
                    context = this,
                    requestType = "DEVICE"
                )
                Toast.makeText(this, getString(R.string.request_sent), Toast.LENGTH_LONG).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1001
    }
}

package com.parentalguard.child.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

object PinManager {
    private const val PREFS_NAME = "pin_prefs"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_SET = "pin_set"
    private const val KEY_PROTECTION_ENABLED = "pin_protection_enabled"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Hash a PIN using SHA-256
     */
    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Set a new PIN
     */
    fun setPin(context: Context, pin: String): Boolean {
        if (pin.length < 4) return false
        
        val hashedPin = hashPin(pin)
        getPrefs(context).edit()
            .putString(KEY_PIN_HASH, hashedPin)
            .putBoolean(KEY_PIN_SET, true)
            .apply()
        return true
    }
    
    /**
     * Verify a PIN
     */
    fun verifyPin(context: Context, pin: String): Boolean {
        val storedHash = getPrefs(context).getString(KEY_PIN_HASH, null) ?: return false
        val inputHash = hashPin(pin)
        return storedHash == inputHash
    }
    
    /**
     * Check if PIN is set
     */
    fun isPinSet(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PIN_SET, false)
    }
    
    /**
     * Reset PIN (only via parent command)
     */
    fun resetPin(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_PIN_SET, false)
            .apply()
    }

    /**
     * Master switch, controlled ONLY by the parent app. When disabled, the
     * child opens settings freely: no setup page, no verification page, and
     * the child itself has no way to turn it back on.
     */
    fun isProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PROTECTION_ENABLED, true)
    }

    fun setProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PROTECTION_ENABLED, enabled).apply()
    }
}

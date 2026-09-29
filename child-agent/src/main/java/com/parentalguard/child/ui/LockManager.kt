package com.parentalguard.child.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.parentalguard.child.ui.screens.LockScreen
import com.parentalguard.child.ui.screens.BlackoutLockScreen
import com.parentalguard.child.ui.screens.QuietFocusLockScreen
import com.parentalguard.child.ui.theme.ParentalGuardTheme
import com.parentalguard.common.model.BlockingScreenStyle
import android.widget.Toast

class LockManager(private val context: Context) : LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        /**
         * Every overlay window ever attached in this process. A stale manager
         * instance (service re-init) must never leave a window behind that no
         * live manager will dismiss — hide() sweeps the whole registry.
         */
        private val attachedOverlays = mutableListOf<Pair<WindowManager, ComposeView>>()
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    val isShowing: Boolean get() = overlayView != null || attachedOverlays.isNotEmpty()
    
    // Lifecycle components for Compose
    private val _lifecycleRegistry = LifecycleRegistry(this)
    private val _savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = _lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = _savedStateRegistryController.savedStateRegistry

    init {
        // Initialize SavedStateRegistry
        _savedStateRegistryController.performAttach()
        _savedStateRegistryController.performRestore(null)
    }

    fun showLockScreen() {
        if (isShowing) return // Already shown (by this or any manager instance)

        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
            )

            // Create ComposeView
            overlayView = ComposeView(context).apply {
                // Determine lifecycle owners
                setViewTreeLifecycleOwner(this@LockManager)
                setViewTreeSavedStateRegistryOwner(this@LockManager)
                
                setContent {
                    ParentalGuardTheme {
                        val style by com.parentalguard.child.data.RuleRepository.blockingScreenStyle.collectAsState()
                        val requestUnlock = {
                            com.parentalguard.child.utils.EventHelper.sendUnlockRequest(
                                context = context,
                                requestType = "DEVICE"
                            )
                            Toast.makeText(context, "Unlock Requested", Toast.LENGTH_SHORT).show()
                        }
                        when (style) {
                            BlockingScreenStyle.CURRENT -> LockScreen(onRequestUnlock = requestUnlock)
                            BlockingScreenStyle.BLACKOUT -> BlackoutLockScreen()
                            BlockingScreenStyle.QUIET_FOCUS -> QuietFocusLockScreen(onRequestUnlock = requestUnlock)
                        }
                    }
                }
            }
            
            // Add to Window
            windowManager.addView(overlayView, params)
            synchronized(attachedOverlays) {
                attachedOverlays.add(windowManager to overlayView!!)
            }

            // Activate or resume the reusable Compose lifecycle.
            if (_lifecycleRegistry.currentState == Lifecycle.State.INITIALIZED) {
                _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            }
            if (_lifecycleRegistry.currentState < Lifecycle.State.STARTED) {
                _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            }
            if (_lifecycleRegistry.currentState < Lifecycle.State.RESUMED) {
                _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup on failure
            val failedView = overlayView
            overlayView = null
            if (failedView != null) {
                synchronized(attachedOverlays) {
                    attachedOverlays.removeAll { it.second === failedView }
                }
            }
        }
    }

    fun hideLockScreen() {
        try {
            // Deactivate Lifecycle
            _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            _lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Sweep EVERY attached overlay — including windows added by a stale
        // manager instance that no longer exists. A failed removeView must
        // never leave a zombie window behind, so the registry is cleared
        // regardless and the local reference is always nulled.
        synchronized(attachedOverlays) {
            val pending = attachedOverlays.toList()
            attachedOverlays.clear()
            for ((wm, view) in pending) {
                try {
                    wm.removeView(view)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }
}

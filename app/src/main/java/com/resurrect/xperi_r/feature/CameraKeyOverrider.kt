package com.resurrect.xperi_r.feature

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.launch
import logcat.logcat
import com.resurrect.xperi_r.XperiRApplication

class CameraKeyOverrider(
    private val lifecycleOwner: LifecycleOwner,
    private val service: AccessibilityService,
) : DefaultLifecycleObserver {
    private var focusAction: Action? = null
    private var shutterAction: Action? = null
    private var longPressAction: Action? = null
    private var enabled = false

    private val handler = Handler(Looper.getMainLooper())
    private var cameraKeyDownAt = 0L
    private var longPressFired = false
    private var suppressNextSelfInjected = false

    private val longPressRunnable = Runnable {
        longPressFired = true
        longPressAction?.let {
            logcat { "Camera key long-press -> $it" }
            it.runAction(service)
        }
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!enabled) return false

        return when (event.keyCode) {
            KeyEvent.KEYCODE_FOCUS -> handleFocusKey(event)
            KeyEvent.KEYCODE_CAMERA -> handleCameraKey(event)
            else -> false
        }
    }

    private fun handleFocusKey(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_UP) return true
        focusAction?.let {
            logcat { "Camera key half-press -> $it" }
            it.runAction(service)
        }
        return true
    }

    private fun handleCameraKey(event: KeyEvent): Boolean {
        if (suppressNextSelfInjected) {
            suppressNextSelfInjected = false
            return false
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                cameraKeyDownAt = System.currentTimeMillis()
                longPressFired = false
                handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT_MS)
                return true
            }
            KeyEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRunnable)
                if (longPressFired) {
                    return true
                }
                runShutterAction()
                return true
            }
        }
        return true
    }

    private fun runShutterAction() {
        val action = shutterAction ?: return
        logcat { "Camera key full-press -> $action" }
        when (action) {
            is IntentAction, is DigitalAssistantAction -> {
                action.runAction(service)
            }
            else -> action.runAction(service)
        }
    }

    fun injectShutterKeyToForegroundApp() {
        suppressNextSelfInjected = true
        Shell.cmd("input keyevent ${KeyEvent.KEYCODE_CAMERA}").submit()
    }

    init {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                XperiRApplication.prefs.cameraButtonFlow.collect {
                    enabled = it.enabled
                    focusAction = it.focusAction
                    shutterAction = it.shutterAction
                    longPressAction = it.longPressAction
                    logcat { "CameraKeyOverrider enabled=$enabled" }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        handler.removeCallbacks(longPressRunnable)
    }

    companion object {
        private const val LONG_PRESS_TIMEOUT_MS = 500L

        val isSupported = true
    }
}

data class CameraButtonPrefs(
    val enabled: Boolean = false,
    val focusAction: Action? = null,
    val shutterAction: Action? = null,
    val longPressAction: Action? = null,
)

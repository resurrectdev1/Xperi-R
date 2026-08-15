package com.resurrect.xperi_r.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import logcat.logcat
import com.resurrect.xperi_r.feature.CameraKeyOverrider
import com.resurrect.xperi_r.feature.FlipToShush
import com.resurrect.xperi_r.feature.GAKeyOverrider
import com.resurrect.xperi_r.feature.LockscreenShortcutHelper
import com.resurrect.xperi_r.feature.PerAppRefreshRateController
import com.resurrect.xperi_r.feature.PocketNoTouchy

class TadanoAccessibilityService :
    AccessibilityService(),
    LifecycleOwner {
    private val dispatcher = ServiceLifecycleDispatcher(this)
    private var gaKeyOverrider: GAKeyOverrider? = null
    private var pocketNoTouchy: PocketNoTouchy? = null
    private var flipToShush: FlipToShush? = null
    private var lockscreenShortcutHelper: LockscreenShortcutHelper? = null
    private var cameraKeyOverrider: CameraKeyOverrider? = null
    private var perAppRefreshRateController: PerAppRefreshRateController? = null

    override fun onServiceConnected() {
        dispatcher.onServicePreSuperOnBind()
        super.onServiceConnected()
        isActive = true

        if (CameraKeyOverrider.isSupported) {

            serviceInfo =
                serviceInfo.apply {
                    flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                }
        }
        logcat { "onServiceConnected" }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        gaKeyOverrider?.onAccessibilityEvent(event)
        perAppRefreshRateController?.onAccessibilityEvent(event)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)
        return cameraKeyOverrider?.onKeyEvent(event) ?: super.onKeyEvent(event)
    }

    override fun onInterrupt() {
        logcat { "onInterrupt" }
    }

    override fun onCreate() {
        if (GAKeyOverrider.isSupported) {
            gaKeyOverrider = GAKeyOverrider(this, this)
        }
        pocketNoTouchy = PocketNoTouchy(this, this)
        flipToShush = FlipToShush(this, this)
        lockscreenShortcutHelper = LockscreenShortcutHelper(this, this)
        if (CameraKeyOverrider.isSupported) {
            cameraKeyOverrider = CameraKeyOverrider(this, this)
        }
        perAppRefreshRateController = PerAppRefreshRateController(this, this)
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
        logcat { "onCreate" }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        dispatcher.onServicePreSuperOnStart()
        logcat { "onStartCommand" }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        isActive = false
        super.onDestroy()
        logcat { "onDestroy" }
    }

    override val lifecycle = dispatcher.lifecycle

    companion object {
        var isActive: Boolean by mutableStateOf(false)
            private set
    }
}

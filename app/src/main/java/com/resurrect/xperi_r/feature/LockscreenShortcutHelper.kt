package com.resurrect.xperi_r.feature

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import logcat.logcat
import com.resurrect.xperi_r.XperiRApplication
import com.resurrect.xperi_r.util.canWriteSecureSettings

class LockscreenShortcutHelper(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
) : DefaultLifecycleObserver {
    private var receiverRegistered = false
    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                if (!context.canWriteSecureSettings) {
                    return
                }
                val keyguardLocked = context.getSystemService<KeyguardManager>()!!.isKeyguardLocked
                val screenOn = intent.action != Intent.ACTION_SCREEN_OFF
                if (screenOn && keyguardLocked) {
                    lifecycleOwner.lifecycleScope.launch {
                        delay(75)
                        logcat { "Set camera lockscreen shortcuts to custom ${intent.action}" }
                        val prefs = XperiRApplication.prefs
                        Settings.Secure.putString(
                            context.contentResolver,
                            LOCKSCREEN_LEFT_BUTTON,
                            prefs.lockscreenLeftAction.first(),
                        )
                        Settings.Secure.putString(
                            context.contentResolver,
                            LOCKSCREEN_RIGHT_BUTTON,
                            prefs.lockscreenRightAction.first(),
                        )
                    }
                } else {
                    logcat { "Set lockscreen shortcuts to system default" }
                    Settings.Secure.putString(context.contentResolver, LOCKSCREEN_LEFT_BUTTON, null)
                    Settings.Secure.putString(context.contentResolver, LOCKSCREEN_RIGHT_BUTTON, null)
                }
            }
        }

    override fun onDestroy(owner: LifecycleOwner) {
        updateReceiverState(false)
    }

    private fun updateReceiverState(state: Boolean) {
        if (state) {
            if (!receiverRegistered) {
                logcat { "Registering receiver" }
                val filter =
                    IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_OFF)
                        addAction(Intent.ACTION_SCREEN_ON)
                        priority = 999
                    }
                context.registerReceiver(receiver, filter)
                receiverRegistered = true
            }
        } else if (receiverRegistered) {
            logcat { "Unregistering receiver" }
            context.unregisterReceiver(receiver)
            receiverRegistered = false
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val prefs = XperiRApplication.prefs
                prefs.lockscreenLeftAction
                    .combine(prefs.lockscreenRightAction) { a, b -> a != null || b != null }
                    .collect { updateReceiverState(it) }
            }
        }
    }

    companion object {
        const val LOCKSCREEN_LEFT_BUTTON = "sysui_keyguard_left"
        const val LOCKSCREEN_RIGHT_BUTTON = "sysui_keyguard_right"
    }
}

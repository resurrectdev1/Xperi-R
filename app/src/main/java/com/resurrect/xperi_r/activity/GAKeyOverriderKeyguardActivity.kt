package com.resurrect.xperi_r.activity

import android.app.KeyguardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.resurrect.xperi_r.XperiRApplication
import com.resurrect.xperi_r.ui.destination.KeyguardUnlock
import com.resurrect.xperi_r.ui.theme.XperiRM3Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class GAKeyOverriderKeyguardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            XperiRM3Theme {
                KeyguardUnlock(onClick = { dismissKeyguard() })
            }
        }
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    private fun dismissKeyguard() {
        getSystemService<KeyguardManager>()?.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    lifecycleScope.launch {
                        val action =
                            runBlocking {
                                XperiRApplication.prefs.assistButtonFlow
                                    .first()
                                    .action
                            }
                        action?.runAction(this@GAKeyOverriderKeyguardActivity)
                        finish()
                    }
                }
            },
        )
    }

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                delay(700)
                dismissKeyguard()
            }
        }
    }
}

package com.resurrect.xperi_r.feature

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import com.resurrect.xperi_r.XperiRApplication

class PerAppRefreshRateController(
    private val lifecycleOwner: LifecycleOwner,
    private val service: AccessibilityService,
) : DefaultLifecycleObserver {
    private var enabled = false
    private var packageRateMap: Map<String, Int> = emptyMap()
    private var currentlyCappedPackage: String? = null

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!enabled) return
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == currentlyCappedPackage) return

        val targetRate = packageRateMap[pkg]
        if (targetRate != null) {
            applyRate(targetRate)
            currentlyCappedPackage = pkg
        } else if (currentlyCappedPackage != null) {
            restoreDefaultRate()
            currentlyCappedPackage = null
        }
    }

    private fun applyRate(hz: Int) {
        logcat { "Capping refresh rate to ${hz}Hz" }
        Shell.cmd(
            "settings put system peak_refresh_rate $hz",
            "settings put system min_refresh_rate $hz",
        ).submit { result ->
            if (!result.isSuccess) {
                logcat(LogPriority.ERROR) { "Failed to set refresh rate, is root available?\n${result.err}" }
            }
        }
    }

    private fun restoreDefaultRate() {
        logcat { "Restoring default refresh rate ($defaultPeakRefreshRateHz Hz)" }
        Shell.cmd(
            "settings put system peak_refresh_rate $defaultPeakRefreshRateHz",
            "settings put system min_refresh_rate 60",
        ).submit()
    }

    init {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                XperiRApplication.prefs.perAppRefreshRateFlow.collect {
                    enabled = it.enabled
                    packageRateMap = it.packageRateMap
                    logcat { "PerAppRefreshRateController enabled=$enabled entries=${packageRateMap.size}" }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        if (currentlyCappedPackage != null) {
            restoreDefaultRate()
        }
    }

    companion object {

        const val defaultPeakRefreshRateHz = 120

        fun serialize(map: Map<String, Int>): String = map.entries.joinToString(",") { "${it.key}:${it.value}" }

        fun deserialize(raw: String): Map<String, Int> =
            raw.split(",")
                .mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size != 2) return@mapNotNull null
                    val hz = parts[1].toIntOrNull() ?: return@mapNotNull null
                    parts[0] to hz
                }.toMap()
    }
}

data class PerAppRefreshRatePrefs(
    val enabled: Boolean = false,
    val packageRateMap: Map<String, Int> = emptyMap(),
)

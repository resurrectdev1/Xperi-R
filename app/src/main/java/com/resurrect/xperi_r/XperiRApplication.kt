package com.resurrect.xperi_r

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import com.topjohnwu.superuser.Shell
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import com.resurrect.xperi_r.activity.LinkTargetChooserActivity
import com.resurrect.xperi_r.service.TeaTileService
import com.resurrect.xperi_r.util.PreferencesRepository
import com.resurrect.xperi_r.util.PreferencesSerializer
import com.resurrect.xperi_r.util.isRootAvailable

private val Context.preferencesStore: DataStore<Preferences> by dataStore(
    fileName = "preferences.pb",
    serializer = PreferencesSerializer(),
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                produceSharedPreferences = {
                    context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
                },
            ) { sharedPrefs: SharedPreferencesView, currentData: Preferences ->
                currentData.copy(
                    assistButtonEnabled = if (sharedPrefs.contains("assist_button_enabled")) {
                        sharedPrefs.getBoolean("assist_button_enabled", true)
                    } else {
                        currentData.assistButtonEnabled
                    },
                    assistButtonAction = sharedPrefs.getString("assist_button_action", currentData.assistButtonAction) ?: currentData.assistButtonAction,
                    hideAssistantCue = if (sharedPrefs.contains("hide_assistant_cue")) {
                        sharedPrefs.getBoolean("hide_assistant_cue", false)
                    } else {
                        currentData.hideAssistantCue
                    },
                    preventPocketTouchEnabled = if (sharedPrefs.contains("prevent_pocket_touch")) {
                        sharedPrefs.getBoolean("prevent_pocket_touch", false)
                    } else {
                        currentData.preventPocketTouchEnabled
                    },
                    flipToShushEnabled = if (sharedPrefs.contains("flip_to_shush")) {
                        sharedPrefs.getBoolean("flip_to_shush", false)
                    } else {
                        currentData.flipToShushEnabled
                    },
                    coffeeBoardingDone = if (sharedPrefs.contains("coffee_boarding_done")) {
                        sharedPrefs.getBoolean("coffee_boarding_done", false)
                    } else {
                        currentData.coffeeBoardingDone
                    },
                    teaBoardingDone = if (sharedPrefs.contains("tea_boarding_done")) {
                        sharedPrefs.getBoolean("tea_boarding_done", false)
                    } else {
                        currentData.teaBoardingDone
                    },
                )
            },
            SharedPreferencesMigration(
                produceSharedPreferences = { context.getSharedPreferences("secure_settings", Context.MODE_PRIVATE) },
            ) { sharedPrefs: SharedPreferencesView, currentData: Preferences ->
                currentData.copy(
                    lockscreenLeftAction = sharedPrefs.getString("sysui_keyguard_left", currentData.lockscreenLeftAction) ?: currentData.lockscreenLeftAction,
                    lockscreenRightAction = sharedPrefs.getString("sysui_keyguard_right", currentData.lockscreenRightAction) ?: currentData.lockscreenRightAction,
                )
            },
        )
    },
)

class XperiRApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)

        Shell.setDefaultBuilder(
            Shell.Builder
                .create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR or if (!isRootAvailable) Shell.FLAG_NON_ROOT_SHELL else 0)
                .setTimeout(10),
        )

        if (getSystemService<SensorManager>()?.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, TeaTileService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }

        val state =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
        packageManager.setComponentEnabledSetting(
            ComponentName(this, LinkTargetChooserActivity::class.java),
            state,
            PackageManager.DONT_KILL_APP,
        )

        prefs = PreferencesRepository(preferencesStore)
    }

    companion object {
        lateinit var prefs: PreferencesRepository
            private set
    }
}

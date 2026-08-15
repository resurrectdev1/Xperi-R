package com.resurrect.xperi_r.util

import androidx.datastore.core.DataStore
import com.resurrect.xperi_r.Preferences
import com.resurrect.xperi_r.feature.Action
import com.resurrect.xperi_r.feature.CameraButtonPrefs
import com.resurrect.xperi_r.feature.LockscreenShortcutHelper
import com.resurrect.xperi_r.feature.PerAppRefreshRateController
import com.resurrect.xperi_r.feature.PerAppRefreshRatePrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import logcat.LogPriority
import logcat.logcat
import java.io.IOException

class PreferencesRepository(
    private val preferencesStore: DataStore<Preferences>,
) {
    private val preferencesFlow: Flow<Preferences> =
        preferencesStore.data
            .catch { exception ->
                if (exception is IOException) {
                    logcat(priority = LogPriority.ERROR) { "Error reading preferences." }
                    emit(Preferences())
                } else {
                    throw exception
                }
            }

    val assistButtonFlow: Flow<AssistButtonPrefs> =
        preferencesFlow.map {
            AssistButtonPrefs(
                it.assistButtonEnabled,
                Action.fromPlainString(it.assistButtonAction),
                it.hideAssistantCue,
            )
        }

    val preventPocketTouchEnabledFlow: Flow<Boolean> = preferencesFlow.map { it.preventPocketTouchEnabled }
    val flipToShushEnabledFlow: Flow<Boolean> = preferencesFlow.map { it.flipToShushEnabled }

    val cameraButtonFlow: Flow<CameraButtonPrefs> =
        preferencesFlow.map {
            CameraButtonPrefs(
                it.cameraButtonEnabled,
                Action.fromPlainString(it.cameraFocusAction),
                Action.fromPlainString(it.cameraShutterAction),
                Action.fromPlainString(it.cameraLongPressAction),
            )
        }

    val perAppRefreshRateFlow: Flow<PerAppRefreshRatePrefs> =
        preferencesFlow.map {
            PerAppRefreshRatePrefs(
                it.perAppRefreshRateEnabled,
                PerAppRefreshRateController.deserialize(it.perAppRefreshRateMap),
            )
        }

    val coffeeBoardingDone: Flow<Boolean> = preferencesFlow.map { it.coffeeBoardingDone }
    val teaBoardingDone: Flow<Boolean> = preferencesFlow.map { it.teaBoardingDone }

    val lockscreenLeftAction: Flow<String?> =
        preferencesFlow.map {
            it.lockscreenLeftAction.takeIf { action -> action.isNotEmpty() }
        }
    val lockscreenRightAction: Flow<String?> =
        preferencesFlow.map {
            it.lockscreenRightAction.takeIf { action -> action.isNotEmpty() }
        }

    suspend fun setLockscreenAction(
        key: String,
        value: String?,
    ) {
        val newValue = value ?: ""
        preferencesStore.updateDataSilently {
            when (key) {
                LockscreenShortcutHelper.LOCKSCREEN_LEFT_BUTTON -> it.copy(lockscreenLeftAction = newValue)
                LockscreenShortcutHelper.LOCKSCREEN_RIGHT_BUTTON -> it.copy(lockscreenRightAction = newValue)
                else -> it
            }
        }
    }

    suspend fun setAssistButtonEnabled(enabled: Boolean) {
        preferencesStore.updateDataSilently {
            it.copy(assistButtonEnabled = enabled)
        }
    }

    suspend fun setAssistButtonAction(action: Action?) {
        preferencesStore.updateDataSilently {
            it.copy(assistButtonAction = action?.toPlainString() ?: "")
        }
    }

    suspend fun setHideAssistantCue(enabled: Boolean) {
        preferencesStore.updateDataSilently {
            it.copy(hideAssistantCue = enabled)
        }
    }

    suspend fun setPreventPocketTouchEnabled(enabled: Boolean) {
        preferencesStore.updateDataSilently {
            it.copy(preventPocketTouchEnabled = enabled)
        }
    }

    suspend fun setFlipToShushEnabled(enabled: Boolean) {
        preferencesStore.updateDataSilently {
            it.copy(flipToShushEnabled = enabled)
        }
    }

    suspend fun setCameraButtonEnabled(enabled: Boolean) {
        preferencesStore.updateDataSilently { it.copy(cameraButtonEnabled = enabled) }
    }

    suspend fun setCameraFocusAction(action: Action?) {
        preferencesStore.updateDataSilently { it.copy(cameraFocusAction = action?.toPlainString() ?: "") }
    }

    suspend fun setCameraShutterAction(action: Action?) {
        preferencesStore.updateDataSilently { it.copy(cameraShutterAction = action?.toPlainString() ?: "") }
    }

    suspend fun setCameraLongPressAction(action: Action?) {
        preferencesStore.updateDataSilently { it.copy(cameraLongPressAction = action?.toPlainString() ?: "") }
    }

    suspend fun setPerAppRefreshRateEnabled(enabled: Boolean) {
        preferencesStore.updateDataSilently { it.copy(perAppRefreshRateEnabled = enabled) }
    }

    suspend fun setPerAppRefreshRateMap(map: Map<String, Int>) {
        preferencesStore.updateDataSilently { it.copy(perAppRefreshRateMap = PerAppRefreshRateController.serialize(map)) }
    }

    suspend fun setCoffeeBoardingDone() {
        preferencesStore.updateDataSilently {
            it.copy(coffeeBoardingDone = true)
        }
    }

    suspend fun setTeaBoardingDone() {
        preferencesStore.updateDataSilently {
            it.copy(teaBoardingDone = true)
        }
    }

    private suspend fun DataStore<Preferences>.updateDataSilently(t: (Preferences) -> Preferences) {
        try {
            preferencesStore.updateData(t)
        } catch (_: IOException) {
            logcat(priority = LogPriority.ERROR) { "Error writing preferences." }
        }
    }
}

data class AssistButtonPrefs(
    val enabled: Boolean = false,
    val action: Action? = null,
    val hideAssistantCue: Boolean = false,
)

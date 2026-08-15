package com.resurrect.xperi_r.ui.destination

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.resurrect.xperi_r.R
import com.resurrect.xperi_r.XperiRApplication
import com.resurrect.xperi_r.activity.MainActivityViewModel
import com.resurrect.xperi_r.feature.CameraButtonPrefs
import com.resurrect.xperi_r.service.TadanoAccessibilityService
import com.resurrect.xperi_r.ui.Screen
import com.resurrect.xperi_r.ui.component.Preference
import com.resurrect.xperi_r.ui.component.SwitchPreference
import kotlinx.coroutines.launch

object CameraKeyActionTarget {
    const val FOCUS = "focus"
    const val SHUTTER = "shutter"
    const val LONG_PRESS = "long_press"
}

@Composable
fun CameraKeyOverriderSettings(
    navController: NavController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = XperiRApplication.prefs
    val cameraPrefs by prefs.cameraButtonFlow.collectAsState(initial = CameraButtonPrefs())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            SwitchPreference(
                title = stringResource(id = R.string.camera_key_overrider_title),
                subtitle = stringResource(id = R.string.camera_key_overrider_desc),
                checked = cameraPrefs.enabled,
                enabled = TadanoAccessibilityService.isActive,
                onCheckedChange = { scope.launch { prefs.setCameraButtonEnabled(it) } },
            )
        }
        item {
            Preference(
                title = stringResource(id = R.string.camera_key_half_press_title),
                subtitle = cameraPrefs.focusAction?.getLabel(context)
                    ?: stringResource(id = R.string.off),
                enabled = cameraPrefs.enabled,
                onPreferenceClick = {
                    navController.navigate(Screen.CameraKeyActionSelection.createRoute(CameraKeyActionTarget.FOCUS))
                },
            )
        }
        item {
            Preference(
                title = stringResource(id = R.string.camera_key_full_press_title),
                subtitle = cameraPrefs.shutterAction?.getLabel(context)
                    ?: stringResource(id = R.string.off),
                enabled = cameraPrefs.enabled,
                onPreferenceClick = {
                    navController.navigate(Screen.CameraKeyActionSelection.createRoute(CameraKeyActionTarget.SHUTTER))
                },
            )
        }
        item {
            Preference(
                title = stringResource(id = R.string.camera_key_long_press_title),
                subtitle = cameraPrefs.longPressAction?.getLabel(context)
                    ?: stringResource(id = R.string.off),
                enabled = cameraPrefs.enabled,
                onPreferenceClick = {
                    navController.navigate(Screen.CameraKeyActionSelection.createRoute(CameraKeyActionTarget.LONG_PRESS))
                },
            )
        }
    }
}

@Composable
fun CameraKeyActionSelection(
    navController: NavController,
    contentPadding: PaddingValues,
    target: String,
    modifier: Modifier = Modifier,
    @Suppress("ktlint:compose:vm-forwarding-check")
    mainViewModel: MainActivityViewModel = viewModel(),
) {
    val prefs = XperiRApplication.prefs
    ActionSelectionPager(
        navController = navController,
        contentPadding = contentPadding,
        modifier = modifier,
        mainViewModel = mainViewModel,
        onActionSelected = { action ->
            when (target) {
                CameraKeyActionTarget.FOCUS -> prefs.setCameraFocusAction(action)
                CameraKeyActionTarget.SHUTTER -> prefs.setCameraShutterAction(action)
                CameraKeyActionTarget.LONG_PRESS -> prefs.setCameraLongPressAction(action)
            }
        },
    )
}

package com.resurrect.xperi_r.ui.destination

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.resurrect.xperi_r.R
import com.resurrect.xperi_r.XperiRApplication
import com.resurrect.xperi_r.feature.FlipToShush
import com.resurrect.xperi_r.feature.GAKeyOverrider
import com.resurrect.xperi_r.service.TadanoAccessibilityService
import com.resurrect.xperi_r.ui.Screen
import com.resurrect.xperi_r.ui.component.AccessibilityServiceCard
import com.resurrect.xperi_r.ui.component.Preference
import com.resurrect.xperi_r.ui.component.SoftDivider
import com.resurrect.xperi_r.ui.component.SwitchPreference
import com.resurrect.xperi_r.util.AssistButtonPrefs
import com.resurrect.xperi_r.util.highlightSettingsTo

@Composable
fun Home(
    navController: NavController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = XperiRApplication.prefs
    val buttonPrefs by prefs.assistButtonFlow.collectAsState(initial = AssistButtonPrefs())
    Box(modifier = modifier) {
        LazyColumn(contentPadding = contentPadding) {
            if (!TadanoAccessibilityService.isActive) {
                item {
                    AccessibilityServiceCard(
                        onButtonClick = {
                            val serviceCn = ComponentName(context, TadanoAccessibilityService::class.java).flattenToString()
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).highlightSettingsTo(serviceCn)
                            context.startActivity(intent)
                        },
                    )
                }
            }
            if (GAKeyOverrider.isSupported) {
                item {
                    Preference(
                        title = stringResource(id = R.string.assistant_button_title),
                        subtitle = buttonPrefs.let { (enabled, action, _) ->
                            if (!enabled) {
                                stringResource(R.string.off)
                            } else {
                                action?.getLabel(context)
                                    ?: stringResource(id = R.string.assistant_action_select_default_value)
                            }
                        },
                        enabled = TadanoAccessibilityService.isActive,
                        onPreferenceClick = { navController.navigate(Screen.AssistantButtonSettings.route) },
                    )
                }
            }
            item {
                val flipToShushEnabled by prefs.flipToShushEnabledFlow.collectAsState(initial = false)
                val fullTimeFlipToShush = remember { FlipToShush.supportFullTimeListening(context) }
                val subtitle = remember {
                    context.getString(R.string.flip_to_shush_desc) +
                        if (!fullTimeFlipToShush) {
                            "\n\n${context.getString(R.string.flip_to_shush_desc_extra_screen_on)}"
                        } else {
                            ""
                        }
                }
                val dndAccessCheck = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    val isGrantedDndAccess = context.getSystemService<NotificationManager>()!!
                        .isNotificationPolicyAccessGranted
                    if (isGrantedDndAccess) {
                        // Always true becos
                        scope.launch { prefs.setFlipToShushEnabled(true) }
                    }
                }
                SwitchPreference(
                    title = stringResource(R.string.flip_to_shush_title),
                    subtitle = subtitle,
                    checked = flipToShushEnabled,
                    enabled = TadanoAccessibilityService.isActive,
                    onCheckedChange = {
                        if (it) {
                            val isGrantedDndAccess = context.getSystemService<NotificationManager>()!!
                                .isNotificationPolicyAccessGranted
                            if (!isGrantedDndAccess) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.allow_dnd_access_toast),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                    .highlightSettingsTo(context.packageName)
                                dndAccessCheck.launch(intent)
                                return@SwitchPreference
                            }
                        }
                        scope.launch { prefs.setFlipToShushEnabled(it) }
                    },
                )
            }
            item {
                val preventPocketTouch by prefs.preventPocketTouchEnabledFlow.collectAsState(initial = false)
                SwitchPreference(
                    title = stringResource(R.string.pocket_no_touchy_title),
                    subtitle = stringResource(R.string.pocket_no_touchy_desc),
                    checked = preventPocketTouch,
                    enabled = TadanoAccessibilityService.isActive,
                    onCheckedChange = {
                        scope.launch { prefs.setPreventPocketTouchEnabled(it) }
                    },
                )
            }
            item {
                Preference(
                    title = stringResource(R.string.lockscreen_shortcut_title),
                    subtitle = stringResource(R.string.lockscreen_shortcut_desc),
                    enabled = TadanoAccessibilityService.isActive,
                    onPreferenceClick = {
                        navController.navigate(Screen.LockscreenShortcutSettings.route)
                    },
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    SoftDivider()
                    Preference(
                        title = stringResource(R.string.android_app_link_title),
                        subtitle = stringResource(id = R.string.android_app_link_subtitle),
                        onPreferenceClick = {
                            navController.navigate(Screen.AndroidAppLinkSettings.route)
                        },
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = painterResource(id = R.drawable.xperi_r),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(64.dp),
            )
        }
    }
}

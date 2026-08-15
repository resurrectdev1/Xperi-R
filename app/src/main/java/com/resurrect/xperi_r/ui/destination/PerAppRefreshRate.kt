package com.resurrect.xperi_r.ui.destination

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.resurrect.xperi_r.R
import com.resurrect.xperi_r.XperiRApplication
import com.resurrect.xperi_r.activity.MainActivityViewModel
import com.resurrect.xperi_r.feature.PerAppRefreshRatePrefs
import com.resurrect.xperi_r.service.TadanoAccessibilityService
import com.resurrect.xperi_r.ui.Screen
import com.resurrect.xperi_r.ui.component.ApplicationRow
import com.resurrect.xperi_r.ui.component.CommonActionRow
import com.resurrect.xperi_r.ui.component.Preference
import com.resurrect.xperi_r.ui.component.SwitchPreference
import com.resurrect.xperi_r.util.getPackageLabel
import com.resurrect.xperi_r.util.isRootAvailable
import kotlinx.coroutines.launch

private val COMMON_REFRESH_RATES = listOf(30, 40, 48, 60, 90, 120)

@Composable
fun PerAppRefreshRateSettings(
    navController: NavController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = XperiRApplication.prefs
    val refreshRatePrefs by prefs.perAppRefreshRateFlow.collectAsState(initial = PerAppRefreshRatePrefs())
    val entries = remember(refreshRatePrefs.packageRateMap) {
        refreshRatePrefs.packageRateMap.entries.sortedBy { context.getPackageLabel(it.key).lowercase() }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            SwitchPreference(
                title = stringResource(id = R.string.per_app_refresh_rate_title),
                subtitle = if (!isRootAvailable) {
                    stringResource(id = R.string.per_app_refresh_rate_root_required)
                } else {
                    stringResource(id = R.string.per_app_refresh_rate_desc)
                },
                checked = refreshRatePrefs.enabled,
                enabled = TadanoAccessibilityService.isActive && isRootAvailable,
                onCheckedChange = { scope.launch { prefs.setPerAppRefreshRateEnabled(it) } },
            )
        }
        item {
            Preference(
                title = stringResource(id = R.string.per_app_refresh_rate_add_app),
                enabled = refreshRatePrefs.enabled,
                onPreferenceClick = {
                    navController.navigate(Screen.PerAppRefreshRateAppSelection.route)
                },
            )
        }
        if (entries.isEmpty()) {
            item {
                Text(
                    text = stringResource(id = R.string.per_app_refresh_rate_empty),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(entries, key = { it.key }) { entry ->
                PackageRateRow(
                    packageName = entry.key,
                    hz = entry.value,
                    onClick = {
                        navController.navigate(Screen.PerAppRefreshRateHzSelection.createRoute(entry.key))
                    },
                    onRemove = {
                        scope.launch {
                            prefs.setPerAppRefreshRateMap(refreshRatePrefs.packageRateMap - entry.key)
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun PerAppRefreshRateAppSelection(
    navController: NavController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    mainViewModel: MainActivityViewModel = viewModel(),
) {
    val items by mainViewModel.appsList.collectAsState()
    val isRefreshing by mainViewModel.isRefreshingAppsList.collectAsState()
    val state = rememberPullToRefreshState()
    val uniqueApps = remember(items) { items.distinctBy { it.componentName.packageName } }

    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = isRefreshing,
        onRefresh = { mainViewModel.refreshAppsList() },
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.padding(contentPadding).align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = state,
            )
        },
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
        ) {
            items(uniqueApps) { item ->
                ApplicationRow(
                    item = item,
                    onClick = {
                        navController.navigate(
                            Screen.PerAppRefreshRateHzSelection.createRoute(it.packageName),
                        )
                    },
                )
            }
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
fun PerAppRefreshRateHzSelection(
    navController: NavController,
    contentPadding: PaddingValues,
    packageName: String,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val prefs = XperiRApplication.prefs
    val refreshRatePrefs by prefs.perAppRefreshRateFlow.collectAsState(initial = PerAppRefreshRatePrefs())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(COMMON_REFRESH_RATES) { hz ->
            CommonActionRow(
                iconVector = Icons.Rounded.Speed,
                label = stringResource(id = R.string.hz_value_format, hz),
                onClick = {
                    scope.launch {
                        prefs.setPerAppRefreshRateMap(refreshRatePrefs.packageRateMap + (packageName to hz))
                        navController.popBackStack(Screen.PerAppRefreshRateSettings.route, inclusive = false)
                    }
                },
            )
        }
    }
}

@Composable
private fun PackageRateRow(
    packageName: String,
    hz: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var label by remember(packageName) { mutableStateOf(packageName) }
    var icon by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(packageName) {
        label = context.getPackageLabel(packageName)
        icon = try {
            context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    ListItem(
        modifier = modifier.clickable(onClick = onClick).fillMaxWidth(),
        headlineContent = { Text(text = label) },
        supportingContent = { Text(text = stringResource(id = R.string.hz_value_format, hz)) },
        leadingContent = {
            val bitmap = icon
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(36.dp))
            }
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(imageVector = Icons.Rounded.Clear, contentDescription = stringResource(id = R.string.button_remove))
            }
        },
    )
}

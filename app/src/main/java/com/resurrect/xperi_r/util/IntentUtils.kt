package com.resurrect.xperi_r.util

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import com.resurrect.xperi_r.feature.IntentAction
import logcat.LogPriority
import logcat.logcat

val RELEASES_PAGE_INTENT = Intent(ACTION_VIEW, "https://github.com/resurrectdev1/Xperi-R/releases/latest".toUri())
const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

@Suppress("DEPRECATION")
suspend fun Intent.setAsAssistantAction(prefs: PreferencesRepository) {
    if (!isValidExtraType(Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)) {
        logcat(LogPriority.ERROR) { "Returned intent doesn't have shortcut intent extra!" }
        return
    }
    val name = getStringExtra(Intent.EXTRA_SHORTCUT_NAME)
    logcat { "Preparing to save intent action with label $name" }
    val extra = IntentCompat.getParcelableExtra(this, Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)
    val intent = Intent(extra).apply {
        // For UI
        putExtra(Intent.EXTRA_SHORTCUT_NAME, name)
    }
    prefs.setAssistButtonAction(IntentAction(intent))
}

fun Intent.loadLabel(context: Context): String {
    val pm = context.packageManager

    @Suppress("DEPRECATION")
    return getStringExtra(Intent.EXTRA_SHORTCUT_NAME)
        ?: (
            pm.resolveActivityCompat(this, PackageManager.MATCH_ALL)?.loadLabel(pm)?.toString()
                ?: pm.getApplicationLabel(pm.getApplicationInfoCompat(component!!.packageName, 0)).toString()
            )
}

private inline fun <reified T> Intent.isValidExtraType(
    key: String,
    type: Class<T>,
): Boolean = type.isInstance(IntentCompat.getParcelableExtra(this, key, T::class.java))
fun Intent.highlightSettingsTo(string: String): Intent {
    putExtra(EXTRA_FRAGMENT_ARG_KEY, string)
    val bundle = bundleOf(EXTRA_FRAGMENT_ARG_KEY to string)
    putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)
    return this
}

fun createShareTextIntent(text: String): Intent {
    val sendIntent: Intent =
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
    return Intent.createChooser(sendIntent, null)
}

package com.resurrect.xperi_r.util

import android.app.Activity
import android.content.Intent

fun Activity.shareLink(newLink: String) {
    val newIntent =
        Intent
            .createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = intent.type
                    putExtra(Intent.EXTRA_TEXT, newLink)
                },
                null,
            ).apply {
                putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(componentName))
            }
    startActivity(newIntent)
}

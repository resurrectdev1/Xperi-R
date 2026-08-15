package com.resurrect.xperi_r.item

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class LinkHandlerAppItem(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
    val linkHandlingAllowed: Boolean,
    val verifiedDomains: Set<String>,
    val userSelectedDomains: Set<String>,
    val unapprovedDomains: Set<String>,
) {
    val isApproved: Boolean = verifiedDomains.isNotEmpty() || userSelectedDomains.isNotEmpty()
    val isUnapproved: Boolean = !isApproved && unapprovedDomains.isNotEmpty()
}

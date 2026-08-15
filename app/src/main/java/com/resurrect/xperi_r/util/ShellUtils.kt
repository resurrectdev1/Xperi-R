package com.resurrect.xperi_r.util

import java.io.File

val isRootAvailable: Boolean
    get() {
        val path = System.getenv("PATH")
        if (!path.isNullOrEmpty()) {
            path.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().forEach { dir ->
                if (File(dir, "su").canExecute()) {
                    return true
                }
            }
        }
        return false
    }

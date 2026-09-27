package com.parentalguard.parent.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

fun currentAppVersion(context: Context): String {
    return runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "3.0.2"
}

fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

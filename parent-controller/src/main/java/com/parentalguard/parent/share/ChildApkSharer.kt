package com.parentalguard.parent.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.parentalguard.parent.R
import java.io.File

object ChildApkSharer {
    fun share(context: Context) {
        try {
            val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val apk = File(sharedDir, "kidguard-child.apk")
            context.assets.open("kidguard-child.apk").use { input ->
                apk.outputStream().use { output -> input.copyTo(output) }
            }
            shareFile(context, apk)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.share_child_apk_unavailable),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /** Shares any APK file (e.g. a release downloaded from GitHub). */
    fun shareFile(context: Context, apk: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apk
            )
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(context.getString(R.string.share_child_apk_title), uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(
                    sendIntent,
                    context.getString(R.string.share_child_apk_chooser)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.share_child_apk_unavailable),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

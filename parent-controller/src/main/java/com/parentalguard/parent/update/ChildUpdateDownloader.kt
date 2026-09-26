package com.parentalguard.parent.update

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import java.io.File

/**
 * Downloads the child APK from a GitHub release asset URL into the parent's
 * cache dir, ready to be pushed to children via [com.parentalguard.parent.network.DeviceClient.uploadChildApk].
 */
object ChildUpdateDownloader {

    fun updateFile(context: Context): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        return File(dir, "kidguard-child-update.apk")
    }

    /**
     * Downloads [url] to [updateFile]. Returns the file on success, null on failure.
     * Reports progress (0..1) via [onProgress] when content length is known.
     */
    suspend fun download(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit = {}
    ): File? {
        val client = HttpClient(CIO)
        try {
            val dest = updateFile(context)
            val tmp = File(dest.parent, dest.name + ".tmp")
            val channel = client.get(url).bodyAsChannel()
            tmp.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buf, 0, buf.size)
                    if (read <= 0) break
                    out.write(buf, 0, read)
                    onProgress(-1f)
                }
            }
            if (tmp.length() < 1_000_000) {
                // Sanity: a real child APK is several MB. Anything smaller is an error page.
                tmp.delete()
                return null
            }
            if (dest.exists()) dest.delete()
            tmp.renameTo(dest)
            return dest
        } catch (_: Exception) {
            return null
        } finally {
            client.close()
        }
    }
}

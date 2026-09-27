package com.parentalguard.parent.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.parentalguard.parent.R
import com.parentalguard.parent.network.DeviceClient
import com.parentalguard.parent.share.ChildApkSharer
import com.parentalguard.parent.ui.currentAppVersion
import com.parentalguard.parent.ui.neumorphic.NeumorphicButton
import com.parentalguard.parent.ui.neumorphic.NeumorphicCard
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.viewmodel.ChildDevice
import kotlinx.coroutines.launch
import java.io.File

private sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val latestTag: String) : UpdateUiState
    data class Available(val info: GitHubReleaseChecker.ChildReleaseInfo) : UpdateUiState
    data object Downloading : UpdateUiState
    data class Downloaded(
        val info: GitHubReleaseChecker.ChildReleaseInfo,
        val file: File
    ) : UpdateUiState
    data class Pushing(val done: Int, val total: Int) : UpdateUiState
    data class Done(val ok: Int, val failed: Int) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

/**
 * "Child app updates" card for the Control (settings) screen: checks GitHub
 * releases, downloads the child APK once, then either pushes it to every
 * paired child over LAN or shares the file (Quick Share/Bluetooth) for a
 * manual install — e.g. a brand-new tablet.
 *
 * Storage: the downloaded file lives in cache and is deleted automatically
 * after a fully successful push. (The copy bundled inside this app cannot be
 * deleted at runtime — it is baked into the APK — so the GitHub copy is the
 * way to get it back if space is ever reclaimed by other means.)
 */
@Composable
fun ChildUpdateSection(
    devices: List<ChildDevice>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { DeviceClient() }
    var state by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }
    val installed = remember { currentAppVersion(context) }

    suspend fun checkForUpdate(): UpdateUiState {
        val info = GitHubReleaseChecker.fetchLatestChildRelease()
        return if (info == null) {
            UpdateUiState.Error("GitHub unreachable")
        } else if (GitHubReleaseChecker.isNewerVersion(info.versionTag, installed)) {
            UpdateUiState.Available(info)
        } else {
            UpdateUiState.UpToDate(info.versionTag)
        }
    }

    NeumorphicCard(padding = 16.dp, corner = 24.dp, modifier = modifier.fillMaxWidth()) {
        Column {
            Text(
                text = stringResource(R.string.child_update_title),
                color = Nm.onSurface,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.child_update_desc),
                color = Nm.onSurfaceMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(10.dp))

            val statusText = when (val s = state) {
                UpdateUiState.Idle -> null
                UpdateUiState.Checking -> context.getString(R.string.child_update_checking)
                is UpdateUiState.UpToDate ->
                    context.getString(R.string.child_update_up_to_date) + " (${s.latestTag})"
                is UpdateUiState.Available ->
                    context.getString(R.string.child_update_current, installed, s.info.versionTag)
                UpdateUiState.Downloading -> context.getString(R.string.child_update_downloading)
                is UpdateUiState.Downloaded ->
                    context.getString(R.string.child_update_downloaded, s.info.versionTag)
                is UpdateUiState.Pushing ->
                    context.getString(R.string.child_update_pushing) + " (${s.done}/${s.total})"
                is UpdateUiState.Done -> context.getString(R.string.child_update_success) +
                    " (${s.ok} ok, ${s.failed} failed)"
                is UpdateUiState.Error -> context.getString(R.string.child_update_failed, s.message)
            }
            if (statusText != null) {
                Text(text = statusText, color = Nm.onSurface, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
            }

            when (val s = state) {
                UpdateUiState.Idle,
                is UpdateUiState.Error,
                is UpdateUiState.UpToDate,
                is UpdateUiState.Done -> {
                    NeumorphicButton(
                        text = stringResource(R.string.child_update_check),
                        onClick = {
                            scope.launch {
                                state = UpdateUiState.Checking
                                state = checkForUpdate()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is UpdateUiState.Available -> {
                    Text(
                        text = context.getString(R.string.child_update_available, s.info.versionTag),
                        color = Nm.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    NeumorphicButton(
                        text = stringResource(R.string.child_update_download),
                        onClick = {
                            scope.launch {
                                state = UpdateUiState.Downloading
                                val apk = ChildUpdateDownloader.download(context, s.info.downloadUrl)
                                state = if (apk == null) {
                                    UpdateUiState.Error("download failed")
                                } else {
                                    UpdateUiState.Downloaded(s.info, apk)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    NeumorphicButton(
                        text = stringResource(R.string.child_update_check),
                        onClick = {
                            scope.launch {
                                state = UpdateUiState.Checking
                                state = checkForUpdate()
                            }
                        },
                        inset = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is UpdateUiState.Downloaded -> {
                    NeumorphicButton(
                        text = context.getString(R.string.child_update_push, devices.size),
                        onClick = {
                            scope.launch {
                                if (devices.isEmpty()) {
                                    state = UpdateUiState.Error(
                                        context.getString(R.string.child_update_no_devices)
                                    )
                                    return@launch
                                }
                                var ok = 0
                                var failed = 0
                                devices.forEachIndexed { index, device ->
                                    state = UpdateUiState.Pushing(index, devices.size)
                                    device.pairToken?.let { client.registerPairToken(device.deviceId, it) }
                                    val pushed = client.uploadChildApk(
                                        ip = device.ip.hostAddress ?: "",
                                        port = device.port,
                                        deviceId = device.deviceId,
                                        apkFile = s.file
                                    )
                                    if (pushed) ok++ else failed++
                                }
                                if (failed == 0) {
                                    // Fully delivered — delete the cached copy to save
                                    // space. Re-download from GitHub anytime.
                                    s.file.delete()
                                }
                                state = UpdateUiState.Done(ok, failed)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    NeumorphicButton(
                        text = stringResource(R.string.child_update_share),
                        onClick = { ChildApkSharer.shareFile(context, s.file) },
                        inset = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                UpdateUiState.Checking,
                UpdateUiState.Downloading,
                is UpdateUiState.Pushing -> {
                    // Busy — no buttons until the step finishes.
                }
            }
        }
    }
}

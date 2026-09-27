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
import com.parentalguard.parent.ui.neumorphic.NeumorphicButton
import com.parentalguard.parent.ui.neumorphic.NeumorphicCard
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.viewmodel.ChildDevice
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

/** Installed version + last install outcome reported by one child. Nulls = unreachable. */
private data class DeviceVersion(
    val version: String?,
    val updateResult: String?
)

private sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val info: GitHubReleaseChecker.ChildReleaseInfo) : UpdateUiState
    data class Available(
        val info: GitHubReleaseChecker.ChildReleaseInfo,
        val stats: Map<String, DeviceVersion>
    ) : UpdateUiState
    data object Downloading : UpdateUiState
    data class Downloaded(
        val info: GitHubReleaseChecker.ChildReleaseInfo,
        val file: File,
        val stats: Map<String, DeviceVersion>
    ) : UpdateUiState
    data class Pushing(val done: Int, val total: Int) : UpdateUiState
    data object Verifying : UpdateUiState
    data class Done(
        val ok: Int,
        val failed: Int,
        /** Kept when some device still needs it — retry without re-downloading. */
        val file: File?,
        val stats: Map<String, DeviceVersion>,
        val latestTag: String
    ) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

/**
 * "Child app updates" card for the Control (settings) screen.
 *
 * Correctness rules:
 * - What matters is each CHILD's installed version, never the parent's.
 * - A downloaded file is reused across checks (no forced re-download).
 * - After pushing, versions are re-read from the devices: only when every
 *   target reports the new version is the cache deleted. Otherwise the file
 *   is kept and the real per-device outcome (including the child's own
 *   install result, e.g. a PackageInstaller failure) is shown.
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

    suspend fun fetchStats(): Map<String, DeviceVersion> = coroutineScope {
        devices.map { device ->
            async {
                device.pairToken?.let { client.registerPairToken(device.deviceId, it) }
                device.bluetoothMac?.let { client.registerBluetoothMac(device.deviceId, it) }
                val stats = runCatching {
                    client.getStatsWithConnectionType(
                        ip = device.ip.hostAddress ?: "",
                        port = device.port,
                        deviceId = device.deviceId,
                        includeIcons = false
                    ).response?.stats
                }.getOrNull()
                device.deviceId to DeviceVersion(
                    version = stats?.childAppVersionName,
                    updateResult = stats?.updateResult
                )
            }
        }.awaitAll().toMap()
    }

    fun Map<String, DeviceVersion>.updatable(tag: String): List<ChildDevice> =
        devices.filter { device ->
            val installed = get(device.deviceId)?.version
            installed == null || GitHubReleaseChecker.isNewerVersion(tag, installed)
        }

    suspend fun checkForUpdate(): UpdateUiState {
        val info = GitHubReleaseChecker.fetchLatestChildRelease()
            ?: return UpdateUiState.Error("GitHub unreachable")
        val stats = fetchStats()
        val needsUpdate = devices.any { device ->
            val installed = stats[device.deviceId]?.version
            installed == null || GitHubReleaseChecker.isNewerVersion(info.versionTag, installed)
        }
        if (!needsUpdate) return UpdateUiState.UpToDate(info)
        // Reuse a previous download of this exact version instead of fetching again.
        if (ChildUpdateDownloader.isCacheValid(context, info.versionTag)) {
            return UpdateUiState.Downloaded(info, ChildUpdateDownloader.updateFile(context), stats)
        }
        return UpdateUiState.Available(info, stats)
    }

    suspend fun pushFile(file: File, targets: List<ChildDevice>, tag: String) {
        var ok = 0
        var failed = 0
        targets.forEachIndexed { index, device ->
            state = UpdateUiState.Pushing(index, targets.size)
            device.pairToken?.let { client.registerPairToken(device.deviceId, it) }
            val pushed = client.uploadChildApk(
                ip = device.ip.hostAddress ?: "",
                port = device.port,
                deviceId = device.deviceId,
                apkFile = file
            )
            if (pushed) ok++ else failed++
        }
        // "Pushed" only means the child accepted the file — the install itself
        // happens on-device. Re-read versions to learn the truth.
        state = UpdateUiState.Verifying
        val fresh = fetchStats()
        val cleanTag = GitHubReleaseChecker.cleanTag(tag)
        val stillOutdated = targets.any { device ->
            val v = fresh[device.deviceId]?.version
            v == null || GitHubReleaseChecker.isNewerVersion(cleanTag, v)
        }
        val keptFile = if (!stillOutdated) {
            ChildUpdateDownloader.clearCache(context)
            null
        } else {
            if (file.exists()) file else null
        }
        state = UpdateUiState.Done(ok, failed, keptFile, fresh, cleanTag)
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

            // Per-device version + install-result lines when known.
            val statsAndTag: Pair<Map<String, DeviceVersion>, String?>? = when (val s = state) {
                is UpdateUiState.Available -> s.stats to s.info.versionTag
                is UpdateUiState.Downloaded -> s.stats to s.info.versionTag
                is UpdateUiState.Done -> s.stats to s.latestTag
                else -> null
            }
            val versionLines = statsAndTag?.let { (statsMap, tag) ->
                statsMap.toDeviceLines(devices, context, tag)
            }
            versionLines?.forEach { line ->
                Text(text = line, color = Nm.onSurface, style = MaterialTheme.typography.bodyMedium)
            }
            if (versionLines != null) Spacer(Modifier.height(10.dp))

            val statusText = when (val s = state) {
                UpdateUiState.Idle -> null
                UpdateUiState.Checking -> context.getString(R.string.child_update_checking)
                is UpdateUiState.UpToDate ->
                    context.getString(R.string.child_update_up_to_date) + " (${s.info.versionTag})"
                is UpdateUiState.Available ->
                    context.getString(R.string.child_update_available, s.info.versionTag)
                UpdateUiState.Downloading -> context.getString(R.string.child_update_downloading)
                is UpdateUiState.Downloaded ->
                    if (ChildUpdateDownloader.isCacheValid(context, s.info.versionTag) &&
                        s.file.length() == ChildUpdateDownloader.updateFile(context).length()
                    ) {
                        context.getString(R.string.child_update_cached, s.info.versionTag)
                    } else {
                        context.getString(R.string.child_update_downloaded, s.info.versionTag)
                    }
                is UpdateUiState.Pushing ->
                    context.getString(R.string.child_update_pushing) + " (${s.done}/${s.total})"
                UpdateUiState.Verifying -> context.getString(R.string.child_update_verifying)
                is UpdateUiState.Done -> context.getString(R.string.child_update_success) +
                    " (${s.ok} ok, ${s.failed} failed)"
                is UpdateUiState.Error -> context.getString(R.string.child_update_failed, s.message)
            }
            if (statusText != null) {
                Text(
                    text = statusText,
                    color = if (state is UpdateUiState.Available) Nm.primary else Nm.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
            }

            when (val s = state) {
                UpdateUiState.Idle,
                is UpdateUiState.Error -> {
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
                is UpdateUiState.UpToDate -> {
                    // Even when versions match, allow (re)installing the same
                    // build — e.g. to deliver a fix without a version bump, or
                    // repair a misbehaving install.
                    NeumorphicButton(
                        text = stringResource(R.string.child_update_download),
                        onClick = {
                            scope.launch {
                                state = UpdateUiState.Downloading
                                val apk = ChildUpdateDownloader.download(
                                    context, s.info.downloadUrl, s.info.versionTag
                                )
                                state = if (apk == null) {
                                    UpdateUiState.Error("download failed")
                                } else {
                                    UpdateUiState.Downloaded(
                                        s.info, apk, fetchStats()
                                    )
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
                is UpdateUiState.Available -> {
                    if (s.stats.updatable(s.info.versionTag).isEmpty()) {
                        Text(
                            text = stringResource(R.string.child_update_all_current),
                            color = Nm.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    NeumorphicButton(
                        text = stringResource(R.string.child_update_download),
                        onClick = {
                            scope.launch {
                                state = UpdateUiState.Downloading
                                val apk = ChildUpdateDownloader.download(
                                    context, s.info.downloadUrl, s.info.versionTag
                                )
                                state = if (apk == null) {
                                    UpdateUiState.Error("download failed")
                                } else {
                                    UpdateUiState.Downloaded(s.info, apk, s.stats)
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
                    val targets = s.stats.updatable(s.info.versionTag)
                    NeumorphicButton(
                        text = context.getString(R.string.child_update_push, targets.size),
                        onClick = {
                            scope.launch {
                                pushFile(
                                    s.file, targets,
                                    GitHubReleaseChecker.cleanTag(s.info.versionTag)
                                )
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
                is UpdateUiState.Done -> {
                    if (s.file != null && s.file.exists()) {
                        NeumorphicButton(
                            text = stringResource(R.string.child_update_push_again),
                            onClick = {
                                scope.launch {
                                    val current = fetchStats()
                                    // Push again (same file, no download) to devices
                                    // still not on the confirmed version.
                                    val retryTargets = devices.filter { device ->
                                        val v = current[device.deviceId]?.version
                                        v == null || GitHubReleaseChecker.isNewerVersion(
                                            s.latestTag, v
                                        )
                                    }
                                    pushFile(
                                        s.file,
                                        retryTargets.ifEmpty { devices },
                                        s.latestTag
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }
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
                UpdateUiState.Checking,
                UpdateUiState.Downloading,
                UpdateUiState.Verifying,
                is UpdateUiState.Pushing -> {
                    // Busy — no buttons until the step finishes.
                }
            }
        }
    }
}

private fun Map<String, DeviceVersion>.toDeviceLines(
    devices: List<ChildDevice>,
    context: android.content.Context,
    latestTag: String? = null
): List<String> {
    val lines = mutableListOf<String>()
    devices.forEach { device ->
        val name = device.customName.ifBlank { device.name }.ifBlank { device.deviceId }
        val dv = get(device.deviceId)
        val versionLine = when (val installed = dv?.version) {
            null -> context.getString(R.string.child_update_device_offline, name)
            else -> if (latestTag != null &&
                GitHubReleaseChecker.isNewerVersion(latestTag, installed)
            ) {
                context.getString(R.string.child_update_device_needs, name, installed)
            } else {
                context.getString(R.string.child_update_device_current, name, installed)
            }
        }
        lines.add(versionLine)
        dv?.updateResult?.takeIf { it.isNotBlank() }?.let { result ->
            lines.add(context.getString(R.string.child_update_install_result, name, result))
        }
    }
    return lines
}

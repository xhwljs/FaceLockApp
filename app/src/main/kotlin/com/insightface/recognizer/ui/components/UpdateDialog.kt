package com.insightface.recognizer.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.insightface.recognizer.App
import com.insightface.recognizer.update.AppUpdateManager
import com.insightface.recognizer.update.GitHubRelease

/** Renders the appropriate dialog for the current [AppUpdateManager.State]. */
@Composable
fun UpdateDialog(state: AppUpdateManager.State) {
    when (state) {
        is AppUpdateManager.State.UpdateAvailable -> UpdateAvailableDialog(state)
        is AppUpdateManager.State.Downloading -> DownloadingDialog(
            progress = state.progress,
            downloadedBytes = state.downloadedBytes,
            totalBytes = state.totalBytes,
        )
        is AppUpdateManager.State.ReadyToInstall -> ReadyToInstallDialog(state.apkUri, state.forceUpdate)
        is AppUpdateManager.State.Error -> ErrorDialog(state.message)
        else -> Unit
    }
}

@Composable
private fun UpdateAvailableDialog(state: AppUpdateManager.State.UpdateAvailable) {
    val release = state.result.release
    val force = state.forceUpdate
    val hasApk = release?.apkAsset != null

    // 强制更新时拦截系统返回键，避免用户绕过更新。
    BackHandler(enabled = force) { /* swallow back press */ }

    AlertDialog(
        onDismissRequest = {
            // 强制更新不允许关闭对话框；非强制更新可稍后再说。
        },
        title = {
            val tag = if (force) "（强制更新）" else ""
            Text("发现新版本 v${state.result.latestVersion}$tag")
        },
        text = {
            // 内容可滚动，确保长更新说明也能完整查看。
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("当前版本：v${state.result.currentVersion}")
                if (force) {
                    Text(
                        "此版本为重要更新，必须升级后才能继续使用。",
                        fontWeight = FontWeight.Medium,
                    )
                }
                release?.name?.takeIf { it.isNotBlank() }?.let {
                    Text("版本名称：$it")
                }
                release?.body?.takeIf { it.isNotBlank() }?.let { notes ->
                    Text(
                        text = notes
                            .replace(GitHubRelease.FORCE_UPDATE_MARKER, "")
                            .trim(),
                    )
                }
                if (!hasApk) {
                    Text("该 Release 未附带 .apk 安装包，请前往 GitHub 下载。")
                }
            }
        },
        confirmButton = {
            if (hasApk && release != null) {
                TextButton(onClick = {
                    App.get().updateManager.download(release, force)
                }) { Text("立即更新") }
            } else {
                TextButton(onClick = {}) { Text("前往 GitHub") }
            }
        },
        dismissButton = {
            // 强制更新时不显示「稍后」按钮，用户只能更新。
            if (!force) {
                TextButton(onClick = {}) { Text("稍后") }
            }
        },
    )
}

@Composable
private fun DownloadingDialog(
    progress: Int,
    downloadedBytes: Long,
    totalBytes: Long,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("正在下载更新") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { if (progress >= 0) progress / 100f else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
                val sizeText = if (totalBytes > 0) {
                    "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
                } else {
                    formatBytes(downloadedBytes)
                }
                val percentText = if (progress >= 0) "$progress%" else "下载中…"
                Text(
                    "$percentText  |  $sizeText",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

/** 将字节数格式化为人类可读的大小，如 "12.3 MB"。 */
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.lastIndex) {
        size /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) "${bytes} ${units[0]}"
    else String.format("%.1f %s", size, units[unitIndex])
}

@Composable
private fun ReadyToInstallDialog(apkUri: android.net.Uri, forceUpdate: Boolean) {
    BackHandler(enabled = forceUpdate) { /* swallow back press */ }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("更新已下载") },
        text = { Text("是否立即安装新版本？安装完成后应用将重启。") },
        confirmButton = {
            TextButton(onClick = { App.get().updateManager.install(apkUri) }) {
                Text("安装")
            }
        },
        dismissButton = {
            // 强制更新时不允许跳过安装。
            if (!forceUpdate) {
                TextButton(onClick = {}) { Text("稍后") }
            }
        },
    )
}

@Composable
private fun ErrorDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("更新检查失败") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {}) { Text("确定") }
        },
    )
}

@Suppress("unused")
private fun GitHubRelease.assetName(): String = apkAsset?.name.orEmpty()

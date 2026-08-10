package com.insightface.recognizer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.insightface.recognizer.App
import com.insightface.recognizer.update.AppUpdateManager
import com.insightface.recognizer.update.GitHubRelease

/** Renders the appropriate dialog for the current [AppUpdateManager.State]. */
@Composable
fun UpdateDialog(state: AppUpdateManager.State) {
    when (state) {
        is AppUpdateManager.State.UpdateAvailable -> UpdateAvailableDialog(state)
        is AppUpdateManager.State.Downloading -> DownloadingDialog(state.progress)
        is AppUpdateManager.State.ReadyToInstall -> ReadyToInstallDialog(state.apkUri)
        is AppUpdateManager.State.Error -> ErrorDialog(state.message)
        else -> Unit
    }
}

@Composable
private fun UpdateAvailableDialog(state: AppUpdateManager.State.UpdateAvailable) {
    val release = state.result.release
    AlertDialog(
        onDismissRequest = {
            // Allow dismiss; the check restarts on next cold launch.
        },
        title = { Text("发现新版本 v${state.result.latestVersion}") },
        text = {
            Column {
                Text("当前版本：v${state.result.currentVersion}")
                release?.name?.takeIf { it.isNotBlank() }?.let {
                    Text("版本名称：$it", modifier = Modifier.padding(top = 8.dp))
                }
                release?.body?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it.take(600),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (release?.apkAsset == null) {
                    Text(
                        "该 Release 未附带 .apk 安装包，请前往 GitHub 下载。",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (release?.apkAsset != null) {
                TextButton(onClick = { App.get().updateManager.download(release) }) {
                    Text("立即更新")
                }
            } else {
                TextButton(onClick = {}) { Text("前往 GitHub") }
            }
        },
        dismissButton = {
            TextButton(onClick = {}) { Text("稍后") }
        },
    )
}

@Composable
private fun DownloadingDialog(progress: Int) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("正在下载更新") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("$progress%", modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

@Composable
private fun ReadyToInstallDialog(apkUri: android.net.Uri) {
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
            TextButton(onClick = {}) { Text("稍后") }
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

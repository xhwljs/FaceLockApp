package com.insightface.recognizer.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads a release APK and launches the system package installer. The APK is written to
 * app-private external storage (`getExternalFilesDir/updates/`) and exposed to the installer
 * through the app's [FileProvider] (authority `<package>.fileprovider`).
 *
 * "Auto update" on Android cannot silently replace the running app — the OS always shows the
 * install confirmation. We therefore download automatically on startup when a new version is
 * found, then prompt the install dialog. This matches the user's "启动时检测版本更新，实现自动
 * 更新" requirement within Android's security model.
 */
class AppUpdateManager(private val app: Context) {

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data class UpdateAvailable(val result: UpdateChecker.CheckResult) : State {
            val forceUpdate: Boolean get() = result.forceUpdate
        }
        data object NoUpdate : State
        data class Downloading(val progress: Int) : State
        data class ReadyToInstall(val apkUri: Uri, val forceUpdate: Boolean) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val checker = UpdateChecker()
    private val scope = CoroutineScope(Dispatchers.IO)

    /** Called on app startup: checks GitHub Releases for a newer version. */
    fun checkOnStartup() {
        if (_state.value is State.Checking) return
        _state.value = State.Checking
        scope.launch {
            val result = checker.checkLatest()
            _state.value = if (result.hasUpdate) State.UpdateAvailable(result) else {
                if (result.error != null) State.Error(result.error) else State.NoUpdate
            }
        }
    }

    /** Downloads the APK asset of an available update. */
    fun download(release: GitHubRelease, forceUpdate: Boolean = false) {
        val asset = release.apkAsset ?: run {
            _state.value = State.Error("该 Release 未包含 .apk 安装包")
            return
        }
        scope.launch {
            try {
                val uri = downloadApk(asset.browserDownloadUrl, asset.name)
                _state.value = State.ReadyToInstall(uri, forceUpdate)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "下载失败")
            }
        }
    }

    /** Launches the system installer for a downloaded APK. Must be called from the UI thread. */
    fun install(apkUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(intent)
    }

    private suspend fun downloadApk(url: String, fileName: String): Uri = withContext(Dispatchers.IO) {
        val dir = File(app.getExternalFilesDir(null), "updates").apply { if (!exists()) mkdirs() }
        val target = File(dir, fileName)
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "InsightFaceRecognizer-AppUpdate")
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        conn.inputStream.use { input ->
            FileOutputStream(target).use { output ->
                val total = conn.contentLengthLong
                var copied = 0L
                val buf = ByteArray(8 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    output.write(buf, 0, n)
                    copied += n
                    if (total > 0) {
                        val percent = (copied * 100 / total).toInt().coerceIn(0, 100)
                        _state.value = State.Downloading(percent)
                    }
                }
                output.flush()
                output.fd.sync()
            }
        }
        FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", target)
    }
}

package com.insightface.recognizer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.insightface.recognizer.App
import com.insightface.recognizer.BuildConfig
import com.insightface.recognizer.data.FaceEngine
import com.insightface.recognizer.data.FaceManager
import com.insightface.recognizer.ui.theme.AppTheme
import com.insightface.recognizer.ui.theme.LocalThemeManager

@Composable
fun SettingsScreen() {
    val app = App.get()
    val themeManager = LocalThemeManager.current
    val faceState by app.faceManager.state.collectAsState()
    val updateState by app.updateManager.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        // --- Theme switcher (light themes only, no dark mode) ---
        SectionCard("主题") {
            Text(
                "选择应用主题（仅亮色，无暗色模式）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(AppTheme.entries.toList()) { theme ->
                    ThemeChip(
                        theme = theme,
                        selected = themeManager.current == theme,
                        onClick = { themeManager.setTheme(theme) },
                    )
                }
            }
        }

        // --- Model switcher ---
        SectionCard("识别模型") {
            val currentModel = when (faceState) {
                FaceManager.State.READY, FaceManager.State.LAUNCHING -> FaceEngine.DEFAULT_MODEL
                else -> FaceEngine.DEFAULT_MODEL
            }
            Text(
                "InspireFace 模型包（随 JitPack AAR 内置）。切换会重新加载引擎与人脸库。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModelButton("Megatron", "精度更高（默认）", currentModel == "Megatron") {
                    app.faceManager.switchModel(FaceEngine.DEFAULT_MODEL)
                }
                ModelButton("Pikachu", "轻量更快", currentModel == "Pikachu") {
                    app.faceManager.switchModel(FaceEngine.LIGHT_MODEL)
                }
            }
            Text(
                "引擎状态：${faceState.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // --- Version & update ---
        SectionCard("版本与更新") {
            InfoRow("当前版本", "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            InfoRow("GitHub 仓库", "${BuildConfig.GITHUB_REPO_OWNER}/${BuildConfig.GITHUB_REPO_NAME}")
            InfoRow(
                "更新检查",
                when (val s = updateState) {
                    is com.insightface.recognizer.update.AppUpdateManager.State.Checking -> "检查中…"
                    is com.insightface.recognizer.update.AppUpdateManager.State.NoUpdate -> "已是最新版本"
                    is com.insightface.recognizer.update.AppUpdateManager.State.UpdateAvailable -> "发现新版本 v${s.result.latestVersion}"
                    is com.insightface.recognizer.update.AppUpdateManager.State.Downloading -> "下载中 ${s.progress}%"
                    is com.insightface.recognizer.update.AppUpdateManager.State.ReadyToInstall -> "已下载，可安装"
                    is com.insightface.recognizer.update.AppUpdateManager.State.Error -> "失败：${s.message}"
                    com.insightface.recognizer.update.AppUpdateManager.State.Idle -> "未检查"
                },
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { app.updateManager.checkOnStartup() }) {
                Text("立即检查更新")
            }
        }

        SectionCard("关于") {
            Text(
                "本应用对接 InsightFace 官方 InspireFace SDK（deepinsight/insightface）。识别通过本地 FeatureHub 向量库进行 1:N 比对；需先在「人脸库」注册已知人脸，识别页面才能判断“图片中的人是谁”。",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun ThemeChip(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val swatch = theme.colors().primary
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(swatch)
                .then(
                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ),
        )
        Text(
            theme.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModelButton(name: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.size(width = 130.dp, height = 64.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

package com.insightface.recognizer.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.insightface.recognizer.App
import com.insightface.recognizer.data.FaceManager

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val faceState by App.get().faceManager.state.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("InsightFace 人脸识别", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "基于官方 InspireFace SDK（deepinsight/insightface）。选择照片即可识别图片中的人是谁，并返回 SDK 的所有检测条目。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EngineStatus(faceState)
        ActionCard("识别照片中的人脸", "选一张照片 → 检测 → 1:N 识别", Icons.Outlined.ImageSearch) { onNavigate("recognize") }
        ActionCard("管理人脸库", "注册 / 重命名 / 删除 已知人脸", Icons.Outlined.VerifiedUser) { onNavigate("manage") }
    }
}

@Composable
private fun EngineStatus(state: FaceManager.State) {
    val (text, color) = when (state) {
        FaceManager.State.NOT_LAUNCHED -> "引擎未启动" to MaterialTheme.colorScheme.onSurfaceVariant
        FaceManager.State.LAUNCHING -> "正在加载 InspireFace 模型…" to MaterialTheme.colorScheme.onSurfaceVariant
        FaceManager.State.READY -> "引擎就绪" to MaterialTheme.colorScheme.tertiary
        FaceManager.State.FAILED -> "引擎加载失败" to MaterialTheme.colorScheme.error
    }
    Text(text, color = color, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun ActionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

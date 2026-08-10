package com.insightface.recognizer.ui.recognize

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.insightface.recognizer.data.FaceAnalyzer

@Composable
fun RecognizeScreen(vm: RecognizeViewModel = viewModel()) {
    val uiState by vm.state.collectAsState()
    val registerState by vm.registerState.collectAsState()
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) vm.analyze(uri) }

    var registeringFace by remember { mutableStateOf<FaceAnalyzer.Face?>(null) }
    var registerName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { photoPicker.launch("image/*") }) {
                Icon(Icons.Outlined.ImageSearch, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("选择照片")
            }
        }

        when (val s = uiState) {
            RecognizeViewModel.UiState.Idle -> EmptyState()
            RecognizeViewModel.UiState.Loading -> LoadingState()
            is RecognizeViewModel.UiState.Error -> ErrorState(s.message)
            is RecognizeViewModel.UiState.Ready -> ReadyContent(
                state = s,
                onRegister = { face ->
                    registeringFace = face
                    registerName = ""
                },
            )
        }
    }

    registeringFace?.let { face ->
        RegisterDialog(
            name = registerName,
            onNameChange = { registerName = it },
            onConfirm = {
                val f = face
                registeringFace = null
                vm.registerFace(f, registerName)
            },
            onDismiss = { registeringFace = null },
        )
    }

    // Registration result feedback
    when (val rs = registerState) {
        is RecognizeViewModel.RegisterState.Success -> {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { vm.clearRegisterState() },
                title = { Text("注册成功") },
                text = { Text("人脸「${rs.name}」已注册到人脸库。") },
                confirmButton = {
                    TextButton(onClick = { vm.clearRegisterState() }) { Text("确定") }
                },
            )
        }
        is RecognizeViewModel.RegisterState.Failed -> {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { vm.clearRegisterState() },
                title = { Text("注册失败") },
                text = { Text(rs.message) },
                confirmButton = {
                    TextButton(onClick = { vm.clearRegisterState() }) { Text("确定") }
                },
            )
        }
        is RecognizeViewModel.RegisterState.Registering -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("正在注册到人脸库…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        RecognizeViewModel.RegisterState.Idle -> Unit
    }
}

@Composable
private fun ReadyContent(
    state: RecognizeViewModel.UiState.Ready,
    onRegister: (FaceAnalyzer.Face) -> Unit,
) {
    val result = state.result
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors()) {
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    FaceBoxOverlay(
                        bitmap = state.bitmap,
                        faces = result.faces,
                    )
                }
            }
        }
        item {
            SummaryRow(result)
        }
        if (result.status == FaceAnalyzer.Status.READY) {
            items(result.faces) { face ->
                FaceDetailCard(face = face, onRegister = onRegister)
            }
        }
    }
}

@Composable
private fun SummaryRow(result: FaceAnalyzer.Result) {
    when (result.status) {
        FaceAnalyzer.Status.NO_FACE -> StatusPill("未检测到人脸", MaterialTheme.colorScheme.error)
        FaceAnalyzer.Status.PROCESS_FAILED -> StatusPill("检测处理失败", MaterialTheme.colorScheme.error)
        FaceAnalyzer.Status.READY -> StatusPill("检测到 ${result.faces.size} 张人脸", MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun FaceDetailCard(face: FaceAnalyzer.Face, onRegister: (FaceAnalyzer.Face) -> Unit) {
    val a = face.attributes
    val r = face.recognition
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("人脸 #${face.index + 1}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (r != null && r.matched) {
                    Text("✓ ${r.name ?: "已匹配"}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("未识别", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SectionTitle("识别结果")
            InfoLine("是否匹配", if (r?.matched == true) "是" else "否")
            r?.let {
                InfoLine("相似度 (cosine)", FaceLabels.confidence(it.confidence))
                InfoLine("匹配阈值", FaceLabels.confidence(it.threshold))
                InfoLine("匹配 ID", it.identityId.toString())
            }

            SectionTitle("检测框")
            InfoLine("位置 (x,y,w,h)", "%.0f, %.0f, %.0f, %.0f".format(face.rect.left, face.rect.top, face.rect.width(), face.rect.height()))
            InfoLine("关键点数量", "${face.denseLandmarks?.size ?: 0} 点")
            // FaceFeature is the SDK's 512-d recognition embedding (Pikachu/Megatron). The Java
            // wrapper exposes it as an opaque token, so we report presence rather than a field.
            InfoLine("识别特征", if (face.feature != null) "已提取" else "未提取")

            SectionTitle("属性分析")
            InfoLine("性别", FaceLabels.gender(a.gender))
            InfoLine("年龄段", FaceLabels.ageBracket(a.ageBracket))
            InfoLine("人种", FaceLabels.race(a.race))
            InfoLine("口罩", FaceLabels.mask(a.maskConfidence))
            InfoLine("图像质量", FaceLabels.quality(a.qualityScore))
            InfoLine("嘴巴", FaceLabels.jawOpen(a.jawOpen))
            InfoLine("左眼", FaceLabels.eyeOpen(a.leftEyeConfidence))
            InfoLine("右眼", FaceLabels.eyeOpen(a.rightEyeConfidence))

            if (r == null || !r.matched) {
                Spacer(Modifier.height(4.dp))
                Button(onClick = { onRegister(face) }) {
                    Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("注册到人脸库")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Spacer(Modifier.height(4.dp))
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
        Text(text, color = color, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.ImageSearch, contentDescription = null, modifier = Modifier.height(64.dp))
            Spacer(Modifier.height(12.dp))
            Text("选择一张照片开始识别", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("正在通过 InsightFace 分析…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun RegisterDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("注册人脸") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("姓名") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("注册") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

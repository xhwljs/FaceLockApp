package com.insightface.recognizer.ui.manage

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.insightface.recognizer.data.FaceRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManageScreen(vm: ManageViewModel = viewModel()) {
    val records by vm.records.collectAsState()
    val keyword by vm.keyword.collectAsState()
    var editing by remember { mutableStateOf<FaceRepository.FaceRecord?>(null) }
    var editName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<FaceRepository.FaceRecord?>(null) }

    // 每次屏幕进入组合时刷新人脸库。
    // 使用 restoreState 导航时 ViewModel 会复用，init 不会再触发，
    // 所以必须在这里用 LaunchedEffect 确保从其他页面返回后列表是最新的。
    LaunchedEffect(Unit) { vm.refresh() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("人脸库管理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "已注册的人脸会用于 1:N 识别。点击编辑可重命名，删除会同时移除特征与头像。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = keyword,
            onValueChange = vm::setKeyword,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索姓名 / ID") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
        )

        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("人脸库为空，请到「识别」页面注册人脸", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            // 用 weight(1f) 让 LazyColumn 填充剩余空间，避免 fillMaxSize 在 Column 中
            // 与上方元素竞争高度导致列表区域为零。
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(records, key = { it.id }) { record ->
                    FaceRow(
                        record = record,
                        onRename = {
                            editing = record
                            editName = record.name
                        },
                        onDelete = { pendingDelete = record },
                    )
                }
            }
        }
    }

    editing?.let { record ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("姓名") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val r = record
                    editing = null
                    vm.rename(r.id, editName)
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("取消") } },
        )
    }

    pendingDelete?.let { record ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除人脸") },
            text = { Text("确定要删除「${record.name}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    val r = record
                    pendingDelete = null
                    vm.delete(r.id)
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun FaceRow(
    record: FaceRepository.FaceRecord,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = File(record.cropPath),
                contentDescription = record.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(record.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("ID: ${record.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("更新: ${dateFormat.format(Date(record.updatedAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRename) { Icon(Icons.Outlined.Edit, contentDescription = "重命名") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

package com.insightface.recognizer.ui.recognize

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import com.insightface.recognizer.data.FaceAnalyzer

/**
 * Renders the source [bitmap] and overlays every detected face box on top of it. The box
 * color encodes the recognition verdict: green = matched identity, blue = unknown face.
 */
@Composable
fun FaceBoxOverlay(
    bitmap: Bitmap,
    faces: List<FaceAnalyzer.Face>,
    modifier: Modifier = Modifier,
) {
    val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().aspectRatio(aspect),
    ) {
        // Background image first (drawn at the bottom).
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "所选照片",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().aspectRatio(aspect),
        )
        // Face boxes on top.
        val displayWidth = constraints.maxWidth.toFloat()
        val displayHeight = constraints.maxHeight.toFloat()
        val scaleX = displayWidth / bitmap.width
        val scaleY = displayHeight / bitmap.height
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(aspect)) {
            faces.forEachIndexed { _, face ->
                val left = face.rect.left * scaleX
                val top = face.rect.top * scaleY
                val w = face.rect.width() * scaleX
                val h = face.rect.height() * scaleY
                val color = if (face.recognition?.matched == true) Color(0xFF059669) else Color(0xFF2563EB)
                drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    style = Stroke(width = 4f),
                )
                drawRect(
                    color = color,
                    topLeft = Offset(left, (top - 26f).coerceAtLeast(0f)),
                    size = Size(54f, 26f),
                )
            }
        }
    }
}

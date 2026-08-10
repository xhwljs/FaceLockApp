package com.insightface.recognizer.ui.recognize

import com.insightface.sdk.inspireface.base.FaceAttributeResult

/**
 * Human-readable mappings for the integer codes the InspireFace attribute pipeline returns.
 * Code semantics follow the InsightFace attribute models (gender / race / age bracket).
 */
object FaceLabels {

    /** 0 = Male, 1 = Female (InsightFace gender convention). */
    fun gender(code: Int): String = when (code) {
        0 -> "男"
        1 -> "女"
        else -> "未知($code)"
    }

    /** 0 = Yellow/Asian, 1 = White/Caucasian, 2 = Black, 3 = Brown. */
    fun race(code: Int): String = when (code) {
        0 -> "黄种人"
        1 -> "白种人"
        2 -> "黑种人"
        3 -> "棕色人种"
        else -> "未知($code)"
    }

    /** ageBracket is an index into the SDK age bins; shown as a bracket range. */
    fun ageBracket(code: Int): String {
        if (code < 0) return "未知"
        // InsightFace age attribute emits a bracket index; approximate ranges.
        val ranges = listOf(
            "0-2", "3-9", "10-19", "20-29", "30-39", "40-49", "50-59", "60-69", "70+"
        )
        return ranges.getOrNull(code) ?: "未知($code)"
    }

    fun mask(confidence: Float): String = when {
        confidence.isNaN() -> "未检测"
        confidence > 0.5f -> "戴口罩 (${(confidence * 100).toInt()}%)"
        else -> "未戴口罩 (${((1 - confidence) * 100).toInt()}%)"
    }

    fun quality(score: Float): String =
        if (score.isNaN()) "未检测" else "${(score * 100).toInt()}%"

    fun eyeOpen(confidence: Float): String = when {
        confidence.isNaN() -> "未检测"
        confidence > 0.5f -> "睁开 (${(confidence * 100).toInt()}%)"
        else -> "闭合 (${((1 - confidence) * 100).toInt()}%)"
    }

    fun jawOpen(state: Int): String = when (state) {
        0 -> "闭合"
        1 -> "张开"
        else -> "未知($state)"
    }

    fun confidence(value: Float): String =
        if (value.isNaN()) "—" else "%.4f".format(value)
}

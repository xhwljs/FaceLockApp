package com.insightface.recognizer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Inter is the design-system heading/body font. We fall back to the platform default sans
// family so the app stays readable offline without bundling font files.
private val InterFamily = FontFamily.Default

internal val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFamily, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    ),
)

package com.insightface.recognizer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Five light-only themes. There is intentionally NO dark theme variant — per the product
 * requirement the app only ships light surfaces. Each [AppTheme] maps to a hand-tuned
 * Material 3 light [androidx.compose.material3.ColorScheme].
 *
 * The default "Navy" palette is the one produced by the UI UX Pro Max design system
 * (primary #1E3A5F, accent #059669).
 */
enum class AppTheme(val displayName: String) {
    Navy("深海蓝"),
    Ocean("海洋青"),
    Forest("森林绿"),
    Coral("日落橙"),
    Violet("皇室紫")
}

/** Per-theme color tokens. */
internal data class ThemeColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val error: Color,
    val onError: Color,
)

internal val NavyColors = ThemeColors(
    primary = Color(0xFF1E3A5F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E2F5),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF2563EB),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF059669),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB7F4D6),
    onTertiaryContainer = Color(0xFF002112),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFFE4E7EB),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
)

internal val OceanColors = NavyColors.copy(
    primary = Color(0xFF006A6A),
    primaryContainer = Color(0xFF9CF1F0),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF4A6363),
    tertiary = Color(0xFF4B6074),
)

internal val ForestColors = NavyColors.copy(
    primary = Color(0xFF2E7D32),
    primaryContainer = Color(0xFFC8F0CB),
    onPrimaryContainer = Color(0xFF002106),
    secondary = Color(0xFF55624C),
    tertiary = Color(0xFF38656A),
)

internal val CoralColors = NavyColors.copy(
    primary = Color(0xFFE8590C),
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF390C00),
    secondary = Color(0xFF77574B),
    tertiary = Color(0xFF6C5D2F),
)

internal val VioletColors = NavyColors.copy(
    primary = Color(0xFF6B46C1),
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
)

internal fun AppTheme.colors(): ThemeColors = when (this) {
    AppTheme.Navy -> NavyColors
    AppTheme.Ocean -> OceanColors
    AppTheme.Forest -> ForestColors
    AppTheme.Coral -> CoralColors
    AppTheme.Violet -> VioletColors
}

package com.insightface.recognizer.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The active app theme. Provided once at the root so any screen can read/switch it.
 */
val LocalThemeManager = staticCompositionLocalOf<ThemeManager> {
    error("ThemeManager not provided")
}

/** Persists the selected [AppTheme] and exposes it as a reactive state. */
class ThemeManager(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("ui_settings", Context.MODE_PRIVATE)

    private var _current: AppTheme = loadTheme()

    val current: AppTheme get() = _current

    private fun loadTheme(): AppTheme {
        val name = prefs.getString(KEY_THEME, AppTheme.Navy.name) ?: AppTheme.Navy.name
        return runCatching { AppTheme.valueOf(name) }.getOrDefault(AppTheme.Navy)
    }

    fun setTheme(theme: AppTheme) {
        if (theme == _current) return
        _current = theme
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    private companion object {
        const val KEY_THEME = "app_theme"
    }
}

@Composable
fun InsightFaceTheme(
    theme: AppTheme,
    content: @Composable () -> Unit,
) {
    // The product ships light surfaces only — isSystemInDarkTheme() is intentionally ignored.
    @Suppress("UnusedSymbol") val ignored = isSystemInDarkTheme()
    val c = theme.colors()
    val colorScheme = lightColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.primaryContainer,
        onPrimaryContainer = c.onPrimaryContainer,
        secondary = c.secondary,
        onSecondary = c.onSecondary,
        tertiary = c.tertiary,
        onTertiary = c.onTertiary,
        tertiaryContainer = c.tertiaryContainer,
        onTertiaryContainer = c.onTertiaryContainer,
        background = c.background,
        onBackground = c.onBackground,
        surface = c.surface,
        onSurface = c.onSurface,
        surfaceVariant = c.surfaceVariant,
        onSurfaceVariant = c.onSurfaceVariant,
        outline = c.outline,
        error = c.error,
        onError = c.onError,
        scrim = Color(0x66000000),
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

/** Convenience wrapper that pulls the active theme from [LocalThemeManager]. */
@Composable
fun InsightFaceApp(content: @Composable () -> Unit) {
    val manager = LocalThemeManager.current
    InsightFaceTheme(theme = manager.current, content = content)
}

/** Shorthand for CompositionLocalProvider boilerplate at the activity root. */
@Composable
fun ProvideThemeManager(
    manager: ThemeManager,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalThemeManager provides manager, content = content)
}

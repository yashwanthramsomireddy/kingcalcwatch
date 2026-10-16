package `in`.teamexykings.kingcalcwatch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import `in`.teamexykings.kingcalcwatch.settings.KeyStyle
import `in`.teamexykings.kingcalcwatch.settings.ThemeMode

val LocalKeyStyle = staticCompositionLocalOf { KeyStyle.ROUND }

val LocalKcColors = staticCompositionLocalOf { kcColorsFor(isLight = false, accentOverride = null) }

@Composable
fun KingCalcWatchTheme(
    themeMode: ThemeMode = ThemeMode.PITCH_BLACK,
    accentColorArgb: Long? = null,
    keyStyle: KeyStyle = KeyStyle.ROUND,
    content: @Composable () -> Unit
) {
    val systemIsLight = !isSystemInDarkTheme()
    val isLight = when (themeMode) {
        ThemeMode.PITCH_BLACK -> false
        ThemeMode.WHITE -> true
        ThemeMode.SYSTEM -> systemIsLight
    }
    val accent = accentColorArgb?.let { Color(it) }
    val kcColors = kcColorsFor(isLight = isLight, accentOverride = accent)

    val wearColors = Colors(
        primary = kcColors.onSurface,
        primaryVariant = kcColors.surfaceOperator,
        secondary = kcColors.accent,
        secondaryVariant = kcColors.surfaceOperator,
        background = kcColors.background,
        surface = kcColors.surfaceDigit,
        error = kcColors.error,
        onPrimary = kcColors.background,
        onSecondary = kcColors.onAccent,
        onBackground = kcColors.onSurface,
        onSurface = kcColors.onSurface,
        onSurfaceVariant = kcColors.secondaryText,
        onError = kcColors.onSurface
    )

    CompositionLocalProvider(LocalKcColors provides kcColors, LocalKeyStyle provides keyStyle) {
        MaterialTheme(colors = wearColors, content = content)
    }
}

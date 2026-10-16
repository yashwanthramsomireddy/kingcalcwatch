package `in`.teamexykings.kingcalcwatch.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens for the app, resolved once per theme+accent choice and
 * handed down via [LocalKcColors]. Kept separate from Wear's MaterialTheme.Colors
 * (which only has ~8 generic slots) so every screen reads clearly-named tokens
 * instead of guessing which slot means what.
 */
data class KcColors(
    val background: Color,
    val surfaceDigit: Color,      // digit + decimal keys
    val surfaceOperator: Color,   // operator / function / memory keys
    val onSurface: Color,         // text/icons on background & surfaces
    val secondaryText: Color,     // the small running-expression line, "last synced" captions, etc.
    val error: Color,
    val accent: Color,            // "=" button, selected states, highlights
    val onAccent: Color,
    val isLight: Boolean
)

private val PitchBlackBase = KcColors(
    background = Color(0xFF000000),
    surfaceDigit = Color(0xFF1A1A1A),
    surfaceOperator = Color(0xFF2B2B2B),
    onSurface = Color(0xFFFFFFFF),
    secondaryText = Color(0xFFB3B3B3),
    error = Color(0xFFFF6E6E),
    accent = Color(0xFFFFFFFF),
    onAccent = Color(0xFF000000),
    isLight = false
)

private val WhiteBase = KcColors(
    background = Color(0xFFFFFFFF),
    surfaceDigit = Color(0xFFF0F0F0),
    surfaceOperator = Color(0xFFE2E2E2),
    onSurface = Color(0xFF111111),
    secondaryText = Color(0xFF6B6B6B),
    error = Color(0xFFD32F2F),
    accent = Color(0xFF000000),
    onAccent = Color(0xFFFFFFFF),
    isLight = true
)

/** One selectable accent swatch. [color] null means "Classic" — the theme's
 * own neutral accent (white on black / black on white), which is the default. */
data class AccentOption(val name: String, val color: Color?)

val AccentPresets: List<AccentOption> = listOf(
    AccentOption("Classic", null),
    AccentOption("Red", Color(0xFFE53935)),
    AccentOption("Orange", Color(0xFFFB8C00)),
    AccentOption("Amber", Color(0xFFFFB300)),
    AccentOption("Green", Color(0xFF43A047)),
    AccentOption("Teal", Color(0xFF00897B)),
    AccentOption("Blue", Color(0xFF1E88E5)),
    AccentOption("Purple", Color(0xFF8E24AA)),
    AccentOption("Pink", Color(0xFFD81B60))
)

/** Picks black or white text for readable contrast on an arbitrary accent color. */
private fun contrastingOnColor(color: Color): Color {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return if (luminance > 0.55) Color(0xFF000000) else Color(0xFFFFFFFF)
}

/** Resolves the full color set for a theme base + optional custom accent override. */
fun kcColorsFor(isLight: Boolean, accentOverride: Color?): KcColors {
    val base = if (isLight) WhiteBase else PitchBlackBase
    if (accentOverride == null) return base
    return base.copy(accent = accentOverride, onAccent = contrastingOnColor(accentOverride))
}

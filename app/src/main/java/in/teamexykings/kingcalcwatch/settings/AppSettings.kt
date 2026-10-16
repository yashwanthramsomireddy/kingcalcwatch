package `in`.teamexykings.kingcalcwatch.settings

/**
 * @param accentColorArgb null means "Classic" (the theme's own neutral accent).
 *   Stored as a raw ARGB Long rather than a Compose Color so this model has
 *   no UI-toolkit dependency.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.PITCH_BLACK,
    val accentColorArgb: Long? = null,
    val calculatorMode: CalculatorMode = CalculatorMode.COMPACT,
    /** Off by default: a lit screen is the biggest battery cost on a watch. */
    val keepScreenOn: Boolean = false,
    val keyStyle: KeyStyle = KeyStyle.ROUND
) {
    companion object {
        val DEFAULT = AppSettings()
    }
}

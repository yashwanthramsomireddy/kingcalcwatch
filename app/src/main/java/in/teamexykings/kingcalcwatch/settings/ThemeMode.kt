package `in`.teamexykings.kingcalcwatch.settings

/** Pitch-black and white match King Calc's Android app; System follows the
 * watch's own dark/light setting. */
enum class ThemeMode(val label: String) {
    PITCH_BLACK("Pitch Black"),
    WHITE("White"),
    SYSTEM("System")
}

enum class CalculatorMode(val label: String) {
    COMPACT("Compact"),
    EXPANDED("Expanded")
}

/** Round = pill-shaped keys (default); Classic = rounded-square keys like a regular calculator. */
enum class KeyStyle(val label: String) {
    ROUND("Round"),
    CLASSIC("Classic")
}

package `in`.teamexykings.kingcalcwatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import `in`.teamexykings.kingcalcwatch.settings.CalculatorMode
import `in`.teamexykings.kingcalcwatch.ui.theme.LocalKcColors

/** What one key on the grid does, kept separate from [CalculatorAction] so the
 * UI layer owns labels/placement and the engine stays UI-agnostic. */
sealed class CalculatorKey(val label: String) {
    data class Digit(val digit: Char) : CalculatorKey(digit.toString())
    data object Decimal : CalculatorKey(".")
    data class Op(val operator: Operator) : CalculatorKey(operator.symbol)
    data object Equals : CalculatorKey("=")
    data object Percent : CalculatorKey("%")
    data object ToggleSign : CalculatorKey("±")
    data object Backspace : CalculatorKey("⌫")
    data object Clear : CalculatorKey("C")

    // Expanded mode only
    data object SquareRoot : CalculatorKey("√")
    data object Square : CalculatorKey("x²")
    data object Reciprocal : CalculatorKey("1/x")
    data object MemoryAdd : CalculatorKey("M+")
    data object MemorySubtract : CalculatorKey("M−")
    data object MemoryRecall : CalculatorKey("MR")
    data object MemoryClear : CalculatorKey("MC")

    /** Invisible placeholder — keeps a short row aligned to the 4-column grid
     * without being a tappable key. */
    data object Blank : CalculatorKey("")
}

fun CalculatorKey.toAction(): CalculatorAction? = when (this) {
    is CalculatorKey.Digit -> CalculatorAction.Digit(digit)
    is CalculatorKey.Decimal -> CalculatorAction.Decimal
    is CalculatorKey.Op -> CalculatorAction.Op(operator)
    is CalculatorKey.Equals -> CalculatorAction.Equals
    is CalculatorKey.Percent -> CalculatorAction.Percent
    is CalculatorKey.ToggleSign -> CalculatorAction.ToggleSign
    is CalculatorKey.Backspace -> CalculatorAction.Backspace
    is CalculatorKey.Clear -> CalculatorAction.Clear
    is CalculatorKey.SquareRoot -> CalculatorAction.SquareRoot
    is CalculatorKey.Square -> CalculatorAction.Square
    is CalculatorKey.Reciprocal -> CalculatorAction.Reciprocal
    is CalculatorKey.MemoryAdd -> CalculatorAction.MemoryAdd
    is CalculatorKey.MemorySubtract -> CalculatorAction.MemorySubtract
    is CalculatorKey.MemoryRecall -> CalculatorAction.MemoryRecall
    is CalculatorKey.MemoryClear -> CalculatorAction.MemoryClear
    is CalculatorKey.Blank -> null
}

/** Compact: the default 4-function grid (5 rows x 4 cols = 20 keys). Sized to
 * always fit the round display without scrolling — see CalculatorScreen. */
val CompactKeyRows: List<List<CalculatorKey>> = listOf(
    listOf(CalculatorKey.Clear, CalculatorKey.ToggleSign, CalculatorKey.Percent, CalculatorKey.Backspace),
    listOf(CalculatorKey.Digit('7'), CalculatorKey.Digit('8'), CalculatorKey.Digit('9'), CalculatorKey.Op(Operator.DIVIDE)),
    listOf(CalculatorKey.Digit('4'), CalculatorKey.Digit('5'), CalculatorKey.Digit('6'), CalculatorKey.Op(Operator.MULTIPLY)),
    listOf(CalculatorKey.Digit('1'), CalculatorKey.Digit('2'), CalculatorKey.Digit('3'), CalculatorKey.Op(Operator.SUBTRACT)),
    listOf(CalculatorKey.Digit('0'), CalculatorKey.Decimal, CalculatorKey.Equals, CalculatorKey.Op(Operator.ADD))
)

/** Expanded: Compact's 20 keys plus a memory row (MC, MR, M−, M+) and a
 * root/square/reciprocal row (7 rows x 4 cols = 27 keys). Taller than the
 * screen by design — CalculatorScreen enables rotary/crown scroll for this
 * mode only. */
val ExpandedKeyRows: List<List<CalculatorKey>> = listOf(
    listOf(CalculatorKey.MemoryClear, CalculatorKey.MemoryRecall, CalculatorKey.MemorySubtract, CalculatorKey.MemoryAdd),
    listOf(CalculatorKey.SquareRoot, CalculatorKey.Square, CalculatorKey.Reciprocal, CalculatorKey.Blank)
) + CompactKeyRows

fun keyRowsFor(mode: CalculatorMode): List<List<CalculatorKey>> = when (mode) {
    CalculatorMode.COMPACT -> CompactKeyRows
    CalculatorMode.EXPANDED -> ExpandedKeyRows
}

/**
 * One row of keys. Width and height are separate so Compact mode can stretch
 * keys into pills that fill the row; when they're equal the keys are circles.
 */
@Composable
fun CalculatorKeyRow(
    keys: List<CalculatorKey>,
    buttonWidth: Dp,
    buttonHeight: Dp,
    onKeyPress: (CalculatorKey) -> Unit,
    modifier: Modifier = Modifier,
    columnSpacing: Dp = 6.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(columnSpacing)
    ) {
        for (key in keys) {
            if (key is CalculatorKey.Blank) {
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.size(width = buttonWidth, height = buttonHeight)
                )
            } else {
                CalculatorKeyButton(
                    key = key,
                    width = buttonWidth,
                    height = buttonHeight,
                    onClick = { onKeyPress(key) }
                )
            }
        }
    }
}

@Composable
private fun CalculatorKeyButton(
    key: CalculatorKey,
    width: Dp,
    height: Dp,
    onClick: () -> Unit
) {
    val kc = LocalKcColors.current
    val (background, content) = when (key) {
        is CalculatorKey.Equals -> kc.accent to kc.onAccent
        is CalculatorKey.Digit, is CalculatorKey.Decimal -> kc.surfaceDigit to kc.onSurface
        else -> kc.surfaceOperator to kc.onSurface
    }
    // Scale the label with the key so bigger keys get bigger, easier-to-read text.
    val labelSize = (minOf(width, height).value * 0.52f).coerceIn(11f, 18f).sp

    Button(
        onClick = onClick,
        modifier = Modifier.size(width = width, height = height),
        shape = keyShape(height), // pill (Round) or rounded square (Classic)
        colors = ButtonDefaults.buttonColors(backgroundColor = background, contentColor = content)
    ) {
        Text(
            text = key.label,
            style = MaterialTheme.typography.button.copy(fontSize = labelSize),
            maxLines = 1
        )
    }
}

package `in`.teamexykings.kingcalcwatch

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Pure Kotlin calculator state machine — no Android dependencies, so it is
 * fully unit-testable on the JVM (see app/src/test) without an emulator.
 *
 * Deliberately simple for v1: one pending operator at a time, no expression
 * history, no parentheses. That matches a glanceable watch calculator.
 */

enum class Operator(val symbol: String) {
    ADD("+"),
    SUBTRACT("−"),
    MULTIPLY("×"),
    DIVIDE("÷")
}

sealed class CalculatorAction {
    data class Digit(val digit: Char) : CalculatorAction()
    data object Decimal : CalculatorAction()
    data class Op(val operator: Operator) : CalculatorAction()
    data object Equals : CalculatorAction()
    data object Percent : CalculatorAction()
    data object ToggleSign : CalculatorAction()
    data object Backspace : CalculatorAction()
    data object Clear : CalculatorAction()

    // Expanded-mode only (see CalculatorButtons.kt) — unary ops apply directly
    // to the displayed value.
    data object SquareRoot : CalculatorAction()
    data object Square : CalculatorAction()
    data object Reciprocal : CalculatorAction()

    // Expanded-mode only — single-slot memory register, same semantics as a
    // physical calculator's M+/M-/MR/MC.
    data object MemoryAdd : CalculatorAction()
    data object MemorySubtract : CalculatorAction()
    data object MemoryRecall : CalculatorAction()
    data object MemoryClear : CalculatorAction()
}

/**
 * @param display what's currently shown — either the operand being typed, or
 *   (after Equals) the result.
 * @param expression the small line above the main display showing e.g. "12 +",
 *   so a mid-calculation glance still makes sense on a tiny round screen.
 * @param firstOperand the operand captured when an operator was pressed.
 * @param pendingOperator operator waiting for its second operand.
 * @param startFreshOnNextDigit true right after an operator/equals/clear, so
 *   the next digit replaces the display instead of appending to it.
 * @param isError true after a fault (e.g. divide by zero); any key but Clear
 *   is ignored until cleared, matching standard calculator behavior.
 * @param memory the single-slot memory register (Expanded mode only). Persists
 *   across Clear, same as a physical calculator's memory — only MC resets it.
 */
data class CalculatorState(
    val display: String = "0",
    val expression: String = "",
    val firstOperand: BigDecimal? = null,
    val pendingOperator: Operator? = null,
    val startFreshOnNextDigit: Boolean = true,
    val isError: Boolean = false,
    val memory: BigDecimal = BigDecimal.ZERO
) {
    companion object {
        val INITIAL = CalculatorState()
    }
}

object CalculatorEngine {

    private const val MAX_DISPLAY_DIGITS = 10
    private val MATH_CONTEXT = MathContext(12, RoundingMode.HALF_UP)

    fun reduce(state: CalculatorState, action: CalculatorAction): CalculatorState {
        // Only Clear escapes an error state — everything else is a no-op,
        // same as every physical calculator.
        if (state.isError && action !is CalculatorAction.Clear) return state

        return when (action) {
            is CalculatorAction.Digit -> onDigit(state, action.digit)
            is CalculatorAction.Decimal -> onDecimal(state)
            is CalculatorAction.Op -> onOperator(state, action.operator)
            is CalculatorAction.Equals -> onEquals(state)
            is CalculatorAction.Percent -> onPercent(state)
            is CalculatorAction.ToggleSign -> onToggleSign(state)
            is CalculatorAction.Backspace -> onBackspace(state)
            // Clear resets the calculation but keeps the memory register —
            // matches every physical calculator (only MC touches memory).
            is CalculatorAction.Clear -> CalculatorState.INITIAL.copy(memory = state.memory)
            is CalculatorAction.SquareRoot -> onSquareRoot(state)
            is CalculatorAction.Square -> onUnary(state) { it.multiply(it) }
            is CalculatorAction.Reciprocal -> onReciprocal(state)
            is CalculatorAction.MemoryAdd -> state.copy(
                memory = state.memory.add(parseDisplay(state.display)),
                startFreshOnNextDigit = true
            )
            is CalculatorAction.MemorySubtract -> state.copy(
                memory = state.memory.subtract(parseDisplay(state.display)),
                startFreshOnNextDigit = true
            )
            is CalculatorAction.MemoryRecall -> state.copy(
                display = formatResult(state.memory),
                startFreshOnNextDigit = true
            )
            is CalculatorAction.MemoryClear -> state.copy(memory = BigDecimal.ZERO)
        }
    }

    private fun onDigit(state: CalculatorState, digit: Char): CalculatorState {
        val current = if (state.startFreshOnNextDigit) "" else state.display
        if (current.replace("-", "").length >= MAX_DISPLAY_DIGITS) return state

        val next = when {
            current.isEmpty() && digit == '0' -> "0"
            current == "0" -> digit.toString()
            current == "-0" -> "-$digit"
            else -> current + digit
        }
        return state.copy(display = next, startFreshOnNextDigit = false)
    }

    private fun onDecimal(state: CalculatorState): CalculatorState {
        if (state.startFreshOnNextDigit) {
            return state.copy(display = "0.", startFreshOnNextDigit = false)
        }
        if (state.display.contains('.')) return state
        return state.copy(display = state.display + ".")
    }

    private fun onOperator(state: CalculatorState, operator: Operator): CalculatorState {
        // Chain without pressing "=" — e.g. "12 + 5 + " computes the running
        // total first, same as a physical calculator.
        val resolved = if (state.pendingOperator != null && !state.startFreshOnNextDigit) {
            applyPending(state) ?: return state.copy(isError = true, display = "Error")
        } else {
            state
        }
        val operand = resolved.firstOperand ?: parseDisplay(resolved.display)
        return resolved.copy(
            firstOperand = operand,
            pendingOperator = operator,
            expression = "${formatForExpression(operand)} ${operator.symbol}",
            startFreshOnNextDigit = true
        )
    }

    private fun onEquals(state: CalculatorState): CalculatorState {
        if (state.pendingOperator == null) return state
        val result = applyPending(state) ?: return state.copy(isError = true, display = "Error")
        return result.copy(
            firstOperand = null,
            pendingOperator = null,
            expression = "",
            startFreshOnNextDigit = true
        )
    }

    private fun onPercent(state: CalculatorState): CalculatorState {
        val current = parseDisplay(state.display)
        val percentValue = when (state.pendingOperator) {
            Operator.ADD, Operator.SUBTRACT -> {
                val base = state.firstOperand ?: BigDecimal.ZERO
                base.multiply(current).divide(BigDecimal(100), MATH_CONTEXT)
            }
            else -> current.divide(BigDecimal(100), MATH_CONTEXT)
        }
        return state.copy(display = formatResult(percentValue), startFreshOnNextDigit = true)
    }

    private fun onToggleSign(state: CalculatorState): CalculatorState {
        if (state.display == "0") return state
        val toggled = if (state.display.startsWith("-")) {
            state.display.removePrefix("-")
        } else {
            "-${state.display}"
        }
        return state.copy(display = toggled)
    }

    private fun onBackspace(state: CalculatorState): CalculatorState {
        if (state.startFreshOnNextDigit) return state
        val trimmed = state.display.dropLast(1)
        val next = when {
            trimmed.isEmpty() || trimmed == "-" -> "0"
            else -> trimmed
        }
        return state.copy(display = next, startFreshOnNextDigit = next == "0")
    }

    /** Applies a unary function (x², etc.) directly to the displayed value. */
    private fun onUnary(state: CalculatorState, fn: (BigDecimal) -> BigDecimal): CalculatorState {
        val result = fn(parseDisplay(state.display))
        return state.copy(display = formatResult(result), startFreshOnNextDigit = true)
    }

    private fun onSquareRoot(state: CalculatorState): CalculatorState {
        val value = parseDisplay(state.display)
        if (value.signum() < 0) return state.copy(isError = true, display = "Error")
        val result = sqrt(value)
        return state.copy(display = formatResult(result), startFreshOnNextDigit = true)
    }

    /** Newton-Raphson square root. BigDecimal.sqrt(MathContext) needs API 31+;
     * this keeps Expanded mode working on Wear OS 3 (API 30) devices too. */
    private fun sqrt(value: BigDecimal): BigDecimal {
        if (value.signum() == 0) return BigDecimal.ZERO
        var guess = BigDecimal(Math.sqrt(value.toDouble())).let {
            if (it.signum() == 0) BigDecimal.ONE else it
        }
        repeat(20) {
            guess = guess.add(value.divide(guess, MATH_CONTEXT)).divide(BigDecimal(2), MATH_CONTEXT)
        }
        return guess
    }

    private fun onReciprocal(state: CalculatorState): CalculatorState {
        val value = parseDisplay(state.display)
        if (value.compareTo(BigDecimal.ZERO) == 0) return state.copy(isError = true, display = "Error")
        val result = BigDecimal.ONE.divide(value, MATH_CONTEXT)
        return state.copy(display = formatResult(result), startFreshOnNextDigit = true)
    }

    /** Applies the pending operator to (firstOperand, display); null on fault (e.g. ÷0). */
    private fun applyPending(state: CalculatorState): CalculatorState? {
        val operator = state.pendingOperator ?: return state
        val first = state.firstOperand ?: return state
        val second = parseDisplay(state.display)

        val result = when (operator) {
            Operator.ADD -> first.add(second)
            Operator.SUBTRACT -> first.subtract(second)
            Operator.MULTIPLY -> first.multiply(second)
            Operator.DIVIDE -> {
                if (second.compareTo(BigDecimal.ZERO) == 0) return null
                first.divide(second, MATH_CONTEXT)
            }
        }
        return state.copy(display = formatResult(result))
    }

    private fun parseDisplay(display: String): BigDecimal =
        display.toBigDecimalOrNull() ?: BigDecimal.ZERO

    /** Strips trailing zeros/decimal point, and switches to compact scientific
     * notation only once a result would otherwise overflow the display. */
    private fun formatResult(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        val plain = stripped.toPlainString()
        if (plain.replace("-", "").replace(".", "").length <= MAX_DISPLAY_DIGITS) {
            return plain
        }
        return String.format("%.4e", stripped)
    }

    private fun formatForExpression(value: BigDecimal): String =
        value.stripTrailingZeros().toPlainString()
}

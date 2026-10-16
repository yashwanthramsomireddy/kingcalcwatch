package `in`.teamexykings.kingcalcwatch

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorEngineTest {

    private fun type(text: String, state: CalculatorState = CalculatorState.INITIAL): CalculatorState {
        var s = state
        for (c in text) {
            s = when (c) {
                in '0'..'9' -> CalculatorEngine.reduce(s, CalculatorAction.Digit(c))
                '.' -> CalculatorEngine.reduce(s, CalculatorAction.Decimal)
                '+' -> CalculatorEngine.reduce(s, CalculatorAction.Op(Operator.ADD))
                '-' -> CalculatorEngine.reduce(s, CalculatorAction.Op(Operator.SUBTRACT))
                '*' -> CalculatorEngine.reduce(s, CalculatorAction.Op(Operator.MULTIPLY))
                '/' -> CalculatorEngine.reduce(s, CalculatorAction.Op(Operator.DIVIDE))
                '=' -> CalculatorEngine.reduce(s, CalculatorAction.Equals)
                else -> s
            }
        }
        return s
    }

    @Test
    fun `simple addition`() {
        assertEquals("15", type("12+3=").display)
    }

    @Test
    fun `simple subtraction`() {
        assertEquals("9", type("12-3=").display)
    }

    @Test
    fun `simple multiplication`() {
        assertEquals("36", type("12*3=").display)
    }

    @Test
    fun `simple division`() {
        assertEquals("4", type("12/3=").display)
    }

    @Test
    fun `division by zero shows Error`() {
        val result = type("5/0=")
        assertEquals("Error", result.display)
        assertEquals(true, result.isError)
    }

    @Test
    fun `clear recovers from error`() {
        val afterError = type("5/0=")
        val cleared = CalculatorEngine.reduce(afterError, CalculatorAction.Clear)
        assertEquals("0", cleared.display)
        assertEquals(false, cleared.isError)
    }

    @Test
    fun `decimal input`() {
        assertEquals("3.5", type("1.5+2=").display)
    }

    @Test
    fun `chained operators apply running total`() {
        // 2 + 3 + 4 = 9, same as a physical calculator (no "=" between + presses)
        assertEquals("9", type("2+3+4=").display)
    }

    @Test
    fun `repeated leading zero is ignored`() {
        assertEquals("5", type("005").display)
    }

    @Test
    fun `backspace removes last digit`() {
        val s = type("123")
        val s2 = CalculatorEngine.reduce(s, CalculatorAction.Backspace)
        assertEquals("12", s2.display)
    }

    @Test
    fun `backspace to empty resets to zero`() {
        val s = type("5")
        val s2 = CalculatorEngine.reduce(s, CalculatorAction.Backspace)
        assertEquals("0", s2.display)
    }

    @Test
    fun `toggle sign flips and unflips`() {
        val s = type("5")
        val neg = CalculatorEngine.reduce(s, CalculatorAction.ToggleSign)
        assertEquals("-5", neg.display)
        val pos = CalculatorEngine.reduce(neg, CalculatorAction.ToggleSign)
        assertEquals("5", pos.display)
    }

    @Test
    fun `percent on its own divides by 100`() {
        val s = type("50")
        val pct = CalculatorEngine.reduce(s, CalculatorAction.Percent)
        assertEquals("0.5", pct.display)
    }

    @Test
    fun `percent after addition is relative to first operand`() {
        // 200 + 10% => 200 + (200 * 10 / 100) = 220
        val s = type("200+10")
        val pct = CalculatorEngine.reduce(s, CalculatorAction.Percent)
        val result = CalculatorEngine.reduce(pct, CalculatorAction.Equals)
        assertEquals("220", result.display)
    }

    @Test
    fun `clear resets to initial state`() {
        val s = type("123+45")
        val cleared = CalculatorEngine.reduce(s, CalculatorAction.Clear)
        assertEquals(CalculatorState.INITIAL, cleared)
    }

    @Test
    fun `trailing decimal point is dropped from result formatting`() {
        assertEquals("5", type("2.5+2.5=").display)
    }

    // --- Expanded-mode operations ---

    @Test
    fun `square root of a perfect square`() {
        val s = type("9")
        val result = CalculatorEngine.reduce(s, CalculatorAction.SquareRoot)
        assertEquals("3", result.display)
    }

    @Test
    fun `square root of a negative number is an error`() {
        val s = type("5").let { CalculatorEngine.reduce(it, CalculatorAction.ToggleSign) }
        val result = CalculatorEngine.reduce(s, CalculatorAction.SquareRoot)
        assertEquals("Error", result.display)
        assertEquals(true, result.isError)
    }

    @Test
    fun `square doubles via multiplication`() {
        val s = type("5")
        val result = CalculatorEngine.reduce(s, CalculatorAction.Square)
        assertEquals("25", result.display)
    }

    @Test
    fun `reciprocal of a number`() {
        val s = type("4")
        val result = CalculatorEngine.reduce(s, CalculatorAction.Reciprocal)
        assertEquals("0.25", result.display)
    }

    @Test
    fun `reciprocal of zero is an error`() {
        val s = type("0")
        val result = CalculatorEngine.reduce(s, CalculatorAction.Reciprocal)
        assertEquals("Error", result.display)
    }

    @Test
    fun `memory add then recall`() {
        val s = type("10")
        val afterAdd = CalculatorEngine.reduce(s, CalculatorAction.MemoryAdd)
        val afterMore = type("5", afterAdd)
        val afterAdd2 = CalculatorEngine.reduce(afterMore, CalculatorAction.MemoryAdd)
        val recalled = CalculatorEngine.reduce(afterAdd2, CalculatorAction.MemoryRecall)
        assertEquals("15", recalled.display)
    }

    @Test
    fun `memory subtract reduces the register`() {
        val s = type("10")
        val afterAdd = CalculatorEngine.reduce(s, CalculatorAction.MemoryAdd)
        val afterMore = type("3", afterAdd)
        val afterSub = CalculatorEngine.reduce(afterMore, CalculatorAction.MemorySubtract)
        val recalled = CalculatorEngine.reduce(afterSub, CalculatorAction.MemoryRecall)
        assertEquals("7", recalled.display)
    }

    @Test
    fun `memory clear zeroes the register`() {
        val s = type("10")
        val afterAdd = CalculatorEngine.reduce(s, CalculatorAction.MemoryAdd)
        val afterClear = CalculatorEngine.reduce(afterAdd, CalculatorAction.MemoryClear)
        val recalled = CalculatorEngine.reduce(afterClear, CalculatorAction.MemoryRecall)
        assertEquals("0", recalled.display)
    }

    @Test
    fun `clear preserves memory, unlike the calculation state`() {
        val s = type("10")
        val afterAdd = CalculatorEngine.reduce(s, CalculatorAction.MemoryAdd)
        val afterTyping = type("999", afterAdd)
        val cleared = CalculatorEngine.reduce(afterTyping, CalculatorAction.Clear)
        assertEquals("0", cleared.display) // calculation state reset
        val recalled = CalculatorEngine.reduce(cleared, CalculatorAction.MemoryRecall)
        assertEquals("10", recalled.display) // but memory survived
    }
}

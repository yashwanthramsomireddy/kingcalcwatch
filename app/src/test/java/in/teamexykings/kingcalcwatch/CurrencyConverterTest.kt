package `in`.teamexykings.kingcalcwatch

import `in`.teamexykings.kingcalcwatch.currency.CurrencyConverter
import `in`.teamexykings.kingcalcwatch.currency.RatesSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class CurrencyConverterTest {

    private val snapshot = RatesSnapshot(
        base = "USD",
        rates = mapOf("EUR" to 0.5, "INR" to 80.0, "USD" to 1.0),
        fetchedAtMillis = 0L
    )

    @Test
    fun `converts from the base currency`() {
        val result = CurrencyConverter.convert(BigDecimal("10"), "USD", "INR", snapshot)
        assertEquals(0, BigDecimal("800").compareTo(result))
    }

    @Test
    fun `converts into the base currency`() {
        val result = CurrencyConverter.convert(BigDecimal("80"), "INR", "USD", snapshot)
        assertEquals(0, BigDecimal("1").compareTo(result))
    }

    @Test
    fun `converts between two non-base currencies via the base`() {
        // 1 EUR = 2 USD = 160 INR
        val result = CurrencyConverter.convert(BigDecimal("1"), "EUR", "INR", snapshot)
        assertEquals(0, BigDecimal("160").compareTo(result))
    }

    @Test
    fun `unknown currency returns null`() {
        assertNull(CurrencyConverter.convert(BigDecimal("1"), "USD", "XXX", snapshot))
    }

    @Test
    fun `keypad builds an amount`() {
        var text = "0"
        for (key in listOf("1", "2", ".", "5")) text = CurrencyConverter.applyKey(text, key)
        assertEquals("12.5", text)
    }

    @Test
    fun `keypad ignores a second decimal point`() {
        var text = "1"
        for (key in listOf(".", "5", ".", "2")) text = CurrencyConverter.applyKey(text, key)
        assertEquals("1.52", text)
    }

    @Test
    fun `backspace on the last digit resets to zero`() {
        assertEquals("0", CurrencyConverter.applyKey("7", "⌫"))
    }

    @Test
    fun `leading zero is replaced by the first digit`() {
        assertEquals("5", CurrencyConverter.applyKey("0", "5"))
    }

    @Test
    fun `format uses two decimals for normal amounts`() {
        assertEquals("92.13", CurrencyConverter.formatAmount(BigDecimal("92.1286")))
    }

    @Test
    fun `format keeps precision for tiny amounts`() {
        assertEquals("0.000123", CurrencyConverter.formatAmount(BigDecimal("0.0001234")))
    }
}

package `in`.teamexykings.kingcalcwatch.currency

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class CurrencyInfo(val code: String, val name: String)

/** A fetched set of exchange rates, all quoted against [base]. */
data class RatesSnapshot(
    val base: String,
    val rates: Map<String, Double>,
    val fetchedAtMillis: Long
)

/** Everything the converter persists between launches. */
data class CurrencyState(
    val snapshot: RatesSnapshot? = null,
    val currencies: List<CurrencyInfo> = emptyList(),
    val fromCode: String = "USD",
    val toCode: String = "EUR",
    val favourites: Set<String> = DEFAULT_FAVOURITES
)

/** Starred on first run so the picker's Favourites section isn't empty; un-starrable. */
val DEFAULT_FAVOURITES: Set<String> = setOf("USD", "EUR", "GBP", "INR")

/** Pure conversion maths — no Android or network, so it's unit-testable on the JVM. */
object CurrencyConverter {

    private val MATH_CONTEXT = MathContext(12, RoundingMode.HALF_UP)

    /**
     * Converts [amount] from one currency to another using a snapshot quoted
     * against any base: amount * (rate[to] / rate[from]). Null if either
     * currency is missing from the snapshot.
     */
    fun convert(amount: BigDecimal, fromCode: String, toCode: String, snapshot: RatesSnapshot): BigDecimal? {
        val from = rateOf(fromCode, snapshot) ?: return null
        val to = rateOf(toCode, snapshot) ?: return null
        if (from == 0.0) return null
        return amount
            .multiply(BigDecimal(to.toString()), MATH_CONTEXT)
            .divide(BigDecimal(from.toString()), MATH_CONTEXT)
    }

    private const val MAX_AMOUNT_CHARS = 12

    /** Applies one keypad press ("0"-"9", ".", or "⌫") to the amount being typed. */
    fun applyKey(current: String, key: String): String = when (key) {
        "⌫" -> current.dropLast(1).ifEmpty { "0" }
        "." -> if (current.contains('.')) current else "$current."
        else -> when {
            current.length >= MAX_AMOUNT_CHARS -> current
            current == "0" -> key
            else -> current + key
        }
    }

    /** Parses typed text like "12." or "0" into a number; blank/invalid reads as zero. */
    fun parseAmount(text: String): BigDecimal = text.toBigDecimalOrNull() ?: BigDecimal.ZERO

    private fun rateOf(code: String, snapshot: RatesSnapshot): Double? =
        if (code == snapshot.base) 1.0 else snapshot.rates[code]

    /** Rounds for display: 2 decimals normally, more for tiny values so they don't show as 0.00. */
    fun formatAmount(value: BigDecimal): String {
        val abs = value.abs()
        val scale = when {
            abs.compareTo(BigDecimal.ZERO) == 0 -> 2
            abs < BigDecimal("0.01") -> 6
            abs < BigDecimal("1") -> 4
            else -> 2
        }
        return value.setScale(scale, RoundingMode.HALF_UP).toPlainString()
    }
}

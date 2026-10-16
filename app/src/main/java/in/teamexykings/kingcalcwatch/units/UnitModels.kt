package `in`.teamexykings.kingcalcwatch.units

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** [factor] converts one of this unit into the category's base unit (unused for temperature). */
data class UnitDef(val id: String, val name: String, val factor: BigDecimal)

private fun u(id: String, name: String, factor: String) = UnitDef(id, name, BigDecimal(factor))

/** Same eight categories as King Calc on Android, including the Indian area units. */
enum class UnitCategory(
    val label: String,
    val units: List<UnitDef>,
    val defaultFromId: String,
    val defaultToId: String
) {
    LENGTH(
        "Length",
        listOf(
            u("mm", "Millimetre", "0.001"), u("cm", "Centimetre", "0.01"), u("m", "Metre", "1"),
            u("km", "Kilometre", "1000"), u("in", "Inch", "0.0254"), u("ft", "Foot", "0.3048"),
            u("yd", "Yard", "0.9144"), u("mi", "Mile", "1609.344"), u("nmi", "Nautical mile", "1852")
        ),
        "m", "ft"
    ),
    WEIGHT(
        "Weight",
        listOf(
            u("mg", "Milligram", "0.000001"), u("g", "Gram", "0.001"), u("kg", "Kilogram", "1"),
            u("qtl", "Quintal", "100"), u("t", "Tonne", "1000"), u("oz", "Ounce", "0.028349523125"),
            u("lb", "Pound", "0.45359237"), u("st", "Stone", "6.35029318")
        ),
        "kg", "lb"
    ),
    TEMPERATURE(
        "Temperature",
        listOf(u("°C", "Celsius", "1"), u("°F", "Fahrenheit", "1"), u("K", "Kelvin", "1")),
        "°C", "°F"
    ),
    AREA(
        "Area",
        listOf(
            u("m²", "Square metre", "1"), u("km²", "Square kilometre", "1000000"),
            u("ha", "Hectare", "10000"), u("acre", "Acre", "4046.8564224"),
            u("ft²", "Square foot", "0.09290304"), u("yd²", "Square yard", "0.83612736"),
            // Indian units: 1 cent = 435.6 ft², 1 guntha = 1,089 ft², 1 ground = 2,400 ft²
            u("cent", "Cent", "40.468564224"), u("guntha", "Guntha", "101.17141056"),
            u("ground", "Ground", "222.967296")
        ),
        "m²", "ft²"
    ),
    VOLUME(
        "Volume",
        listOf(
            u("mL", "Millilitre", "0.001"), u("L", "Litre", "1"), u("m³", "Cubic metre", "1000"),
            u("tsp", "Teaspoon (US)", "0.00492892159375"), u("tbsp", "Tablespoon (US)", "0.01478676478125"),
            u("fl oz", "Fluid ounce (US)", "0.0295735295625"), u("cup", "Cup (US)", "0.2365882365"),
            u("pt", "Pint (US)", "0.473176473"), u("qt", "Quart (US)", "0.946352946"),
            u("gal", "Gallon (US)", "3.785411784")
        ),
        "L", "gal"
    ),
    SPEED(
        "Speed",
        listOf(
            u("m/s", "Metre/second", "1"), u("km/h", "Kilometre/hour", "0.277777777778"),
            u("mph", "Mile/hour", "0.44704"), u("kn", "Knot", "0.514444444444"),
            u("ft/s", "Foot/second", "0.3048")
        ),
        "km/h", "mph"
    ),
    TIME(
        "Time",
        listOf(
            u("ms", "Millisecond", "0.001"), u("s", "Second", "1"), u("min", "Minute", "60"),
            u("h", "Hour", "3600"), u("day", "Day", "86400"), u("week", "Week", "604800"),
            u("month", "Month (average)", "2629746"), u("year", "Year (average)", "31556952")
        ),
        "h", "min"
    ),
    DATA(
        "Data",
        listOf(
            u("bit", "Bit", "0.125"), u("B", "Byte", "1"), u("KB", "Kilobyte", "1024"),
            u("MB", "Megabyte", "1048576"), u("GB", "Gigabyte", "1073741824"),
            u("TB", "Terabyte", "1099511627776")
        ),
        "MB", "KB"
    );

    fun unit(id: String): UnitDef? = units.firstOrNull { it.id == id }
    val defaultFrom: UnitDef get() = unit(defaultFromId)!!
    val defaultTo: UnitDef get() = unit(defaultToId)!!
}

/** Pure conversion maths — no Android, so it's unit-testable on the JVM. */
object UnitConverter {
    private val MC = MathContext(14, RoundingMode.HALF_UP)

    fun convert(value: BigDecimal, category: UnitCategory, from: UnitDef, to: UnitDef): BigDecimal {
        if (category == UnitCategory.TEMPERATURE) {
            val celsius = when (from.id) {
                "°F" -> value.subtract(BigDecimal(32)).multiply(BigDecimal(5)).divide(BigDecimal(9), MC)
                "K" -> value.subtract(BigDecimal("273.15"))
                else -> value
            }
            return when (to.id) {
                "°F" -> celsius.multiply(BigDecimal(9)).divide(BigDecimal(5), MC).add(BigDecimal(32))
                "K" -> celsius.add(BigDecimal("273.15"))
                else -> celsius
            }
        }
        return value.multiply(from.factor).divide(to.factor, MC)
    }

    /** Up to 8 significant digits, no trailing zeros; scientific only for extreme sizes. */
    fun format(value: BigDecimal): String {
        if (value.signum() == 0) return "0"
        val abs = value.abs()
        if (abs >= BigDecimal("1E12") || abs < BigDecimal("1E-8")) {
            return String.format("%.4e", value)
        }
        return value.round(MathContext(8, RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString()
    }
}

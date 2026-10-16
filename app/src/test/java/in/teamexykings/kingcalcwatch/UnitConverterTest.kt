package `in`.teamexykings.kingcalcwatch

import `in`.teamexykings.kingcalcwatch.units.UnitCategory
import `in`.teamexykings.kingcalcwatch.units.UnitConverter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class UnitConverterTest {

    private fun conv(v: String, c: UnitCategory, from: String, to: String) =
        UnitConverter.format(UnitConverter.convert(BigDecimal(v), c, c.unit(from)!!, c.unit(to)!!))

    @Test fun kilometresToMetres() = assertEquals("1000", conv("1", UnitCategory.LENGTH, "km", "m"))
    @Test fun celsiusToFahrenheit() = assertEquals("212", conv("100", UnitCategory.TEMPERATURE, "°C", "°F"))
    @Test fun negativeFahrenheitToCelsius() = assertEquals("-40", conv("-40", UnitCategory.TEMPERATURE, "°F", "°C"))
    @Test fun centToSquareFeet() = assertEquals("435.6", conv("1", UnitCategory.AREA, "cent", "ft²"))
    @Test fun gunthaToSquareFeet() = assertEquals("1089", conv("1", UnitCategory.AREA, "guntha", "ft²"))
    @Test fun megabytesToKilobytes() = assertEquals("1024", conv("1", UnitCategory.DATA, "MB", "KB"))
}

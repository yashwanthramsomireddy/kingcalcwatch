package `in`.teamexykings.kingcalcwatch.units

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.unitsDataStore by preferencesDataStore(name = "king_calc_watch_units")

/** The category plus the from/to unit chosen in each category (remembered per category). */
data class UnitsState(
    val category: UnitCategory = UnitCategory.LENGTH,
    val from: UnitDef = UnitCategory.LENGTH.defaultFrom,
    val to: UnitDef = UnitCategory.LENGTH.defaultTo
)

class UnitsRepository(private val context: Context) {

    private val categoryKey = stringPreferencesKey("category")
    private fun fromKey(c: UnitCategory) = stringPreferencesKey("from_${c.name}")
    private fun toKey(c: UnitCategory) = stringPreferencesKey("to_${c.name}")

    val state: Flow<UnitsState> = context.unitsDataStore.data.map { prefs ->
        val category = prefs[categoryKey]
            ?.let { raw -> runCatching { UnitCategory.valueOf(raw) }.getOrNull() }
            ?: UnitCategory.LENGTH
        val from = prefs[fromKey(category)]?.let { category.unit(it) } ?: category.defaultFrom
        var to = prefs[toKey(category)]?.let { category.unit(it) } ?: category.defaultTo
        // Never the same unit on both sides.
        if (to.id == from.id) to = category.units.first { it.id != from.id }
        UnitsState(category, from, to)
    }

    suspend fun setCategory(category: UnitCategory) {
        context.unitsDataStore.edit { it[categoryKey] = category.name }
    }

    /** Picking the unit already on the other side swaps them. */
    suspend fun setFrom(category: UnitCategory, id: String) {
        context.unitsDataStore.edit { prefs ->
            val oldFrom = prefs[fromKey(category)] ?: category.defaultFromId
            val to = prefs[toKey(category)] ?: category.defaultToId
            if (id == to) prefs[toKey(category)] = oldFrom
            prefs[fromKey(category)] = id
        }
    }

    suspend fun setTo(category: UnitCategory, id: String) {
        context.unitsDataStore.edit { prefs ->
            val from = prefs[fromKey(category)] ?: category.defaultFromId
            val oldTo = prefs[toKey(category)] ?: category.defaultToId
            if (id == from) prefs[fromKey(category)] = oldTo
            prefs[toKey(category)] = id
        }
    }

    suspend fun swap(category: UnitCategory) {
        context.unitsDataStore.edit { prefs ->
            val from = prefs[fromKey(category)] ?: category.defaultFromId
            val to = prefs[toKey(category)] ?: category.defaultToId
            prefs[fromKey(category)] = to
            prefs[toKey(category)] = from
        }
    }
}

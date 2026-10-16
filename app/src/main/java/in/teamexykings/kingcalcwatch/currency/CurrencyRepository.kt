package `in`.teamexykings.kingcalcwatch.currency

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.currencyDataStore by preferencesDataStore(name = "king_calc_watch_currency")

sealed class RefreshResult {
    data object Success : RefreshResult()
    data class Failed(val reason: String) : RefreshResult()
}

/**
 * Owns the converter's persisted state: the last-fetched rates (so conversion
 * keeps working offline), the currency list, and the chosen from/to pair.
 * Network is only touched by [refresh]; everything else reads the cache.
 */
class CurrencyRepository(private val context: Context) {

    private object Keys {
        val SNAPSHOT = stringPreferencesKey("rates_snapshot_json")
        val CURRENCIES = stringPreferencesKey("currencies_json")
        val FROM = stringPreferencesKey("from_code")
        val TO = stringPreferencesKey("to_code")
        val FAVOURITES = stringSetPreferencesKey("favourite_codes")
    }

    companion object {
        /** Indian users start with INR on the left and USD on the right; everyone else USD / EUR. */
        private fun isIndia() = java.util.Locale.getDefault().country == "IN" ||
            java.util.TimeZone.getDefault().id.let { it == "Asia/Kolkata" || it == "Asia/Calcutta" }
        fun defaultFrom() = if (isIndia()) "INR" else "USD"
        fun defaultTo() = if (isIndia()) "USD" else "EUR"

        /** Rates older than this get refreshed automatically when the converter opens. */
        const val STALE_AFTER_MILLIS = 6L * 60 * 60 * 1000
    }

    val state: Flow<CurrencyState> = context.currencyDataStore.data.map { prefs ->
        val from = prefs[Keys.FROM] ?: defaultFrom()
        var to = prefs[Keys.TO] ?: defaultTo()
        // Both sides can never be the same currency (older saved data could have it).
        if (to == from) to = if (from == "USD") "EUR" else "USD"
        CurrencyState(
            snapshot = prefs[Keys.SNAPSHOT]?.let { runCatching { decodeSnapshot(it) }.getOrNull() },
            currencies = prefs[Keys.CURRENCIES]?.let { runCatching { decodeCurrencies(it) }.getOrNull() }
                ?: emptyList(),
            fromCode = from,
            toCode = to,
            favourites = prefs[Keys.FAVOURITES] ?: DEFAULT_FAVOURITES
        )
    }

    /** Picking the currency already on the other side swaps them, so both sides are never equal. */
    suspend fun setFrom(code: String) {
        context.currencyDataStore.edit { prefs ->
            val oldFrom = prefs[Keys.FROM] ?: defaultFrom()
            val to = prefs[Keys.TO] ?: defaultTo()
            if (code == to) prefs[Keys.TO] = oldFrom
            prefs[Keys.FROM] = code
        }
    }

    suspend fun setTo(code: String) {
        context.currencyDataStore.edit { prefs ->
            val from = prefs[Keys.FROM] ?: defaultFrom()
            val oldTo = prefs[Keys.TO] ?: defaultTo()
            if (code == from) prefs[Keys.FROM] = oldTo
            prefs[Keys.TO] = code
        }
    }

    suspend fun toggleFavourite(code: String) {
        context.currencyDataStore.edit { prefs ->
            val current = prefs[Keys.FAVOURITES] ?: DEFAULT_FAVOURITES
            prefs[Keys.FAVOURITES] = if (code in current) current - code else current + code
        }
    }

    suspend fun swap() {
        context.currencyDataStore.edit { prefs ->
            val from = prefs[Keys.FROM] ?: defaultFrom()
            val to = prefs[Keys.TO] ?: defaultTo()
            prefs[Keys.FROM] = to
            prefs[Keys.TO] = from
        }
    }

    fun isStale(snapshot: RatesSnapshot?, nowMillis: Long = System.currentTimeMillis()): Boolean =
        snapshot == null || nowMillis - snapshot.fetchedAtMillis > STALE_AFTER_MILLIS

    /** Fetches fresh rates (and the currency list, if we don't have one yet) and caches them. */
    suspend fun refresh(): RefreshResult {
        return try {
            val snapshot = ExchangeRateApi.fetchLatest()
            // The open endpoint has no name list, so use Android's built-in
            // currency names (falls back to the bare code for unknown ones).
            val currencies = snapshot.rates.keys.sorted().map { code ->
                val name = runCatching { java.util.Currency.getInstance(code).displayName }.getOrNull()
                CurrencyInfo(code, name?.takeIf { it.isNotBlank() && it != code } ?: code)
            }
            context.currencyDataStore.edit { prefs ->
                prefs[Keys.SNAPSHOT] = encodeSnapshot(snapshot)
                prefs[Keys.CURRENCIES] = encodeCurrencies(currencies)
            }
            RefreshResult.Success
        } catch (e: Exception) {
            RefreshResult.Failed(e.message ?: "Couldn't reach the rate service")
        }
    }

    private fun encodeSnapshot(snapshot: RatesSnapshot): String = JSONObject().apply {
        put("base", snapshot.base)
        put("fetchedAt", snapshot.fetchedAtMillis)
        put("rates", JSONObject().also { obj -> snapshot.rates.forEach { (k, v) -> obj.put(k, v) } })
    }.toString()

    private fun decodeSnapshot(raw: String): RatesSnapshot {
        val json = JSONObject(raw)
        val ratesJson = json.getJSONObject("rates")
        return RatesSnapshot(
            base = json.getString("base"),
            rates = buildMap { for (code in ratesJson.keys()) put(code, ratesJson.getDouble(code)) },
            fetchedAtMillis = json.getLong("fetchedAt")
        )
    }

    private fun encodeCurrencies(list: List<CurrencyInfo>): String = JSONArray().also { arr ->
        list.forEach { arr.put(JSONArray().put(it.code).put(it.name)) }
    }.toString()

    private fun decodeCurrencies(raw: String): List<CurrencyInfo> {
        val arr = JSONArray(raw)
        return List(arr.length()) { i ->
            val pair = arr.getJSONArray(i)
            CurrencyInfo(code = pair.getString(0), name = pair.getString(1))
        }
    }
}

package `in`.teamexykings.kingcalcwatch.currency

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal client for ExchangeRate-API's open-access endpoint (open.er-api.com):
 * no API key, just attribution (shown in About). Plain HttpURLConnection +
 * org.json (both built into Android) rather than Retrofit/OkHttp — one small GET
 * doesn't justify the extra APK size and battery cost on a watch.
 */
object ExchangeRateApi {

    private const val BASE_URL = "https://open.er-api.com/v6/latest"
    private const val TIMEOUT_MS = 8_000

    class ApiException(message: String) : Exception(message)

    suspend fun fetchLatest(base: String = "USD"): RatesSnapshot = withContext(Dispatchers.IO) {
        val json = get("$BASE_URL/$base")
        val ratesJson = json.getJSONObject("rates")
        val rates = buildMap {
            for (code in ratesJson.keys()) put(code, ratesJson.getDouble(code))
        }
        RatesSnapshot(
            base = json.optString("base_code", base),
            rates = rates,
            fetchedAtMillis = System.currentTimeMillis()
        )
    }

    private fun get(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
        }
        try {
            val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }
                ?: throw ApiException("Empty response (HTTP ${connection.responseCode})")
            val json = JSONObject(body)
            if (json.optString("result") != "success") {
                throw ApiException(json.optString("error-type", "Request failed"))
            }
            return json
        } finally {
            connection.disconnect()
        }
    }
}

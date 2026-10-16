package `in`.teamexykings.kingcalcwatch.history

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.historyDataStore by preferencesDataStore(name = "king_calc_watch_history")

data class HistoryEntry(val expression: String, val result: String)

/** Last [MAX_ENTRIES] calculations, newest first, kept only on the watch. */
class HistoryRepository(private val context: Context) {

    private val key = stringPreferencesKey("entries_json")

    val entries: Flow<List<HistoryEntry>> = context.historyDataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { decode(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun add(entry: HistoryEntry) {
        val current = entries.first()
        val updated = (listOf(entry) + current).take(MAX_ENTRIES)
        context.historyDataStore.edit { it[key] = encode(updated) }
    }

    suspend fun clear() {
        context.historyDataStore.edit { it.remove(key) }
    }

    private fun encode(list: List<HistoryEntry>): String = JSONArray().also { arr ->
        list.forEach { arr.put(JSONArray().put(it.expression).put(it.result)) }
    }.toString()

    private fun decode(raw: String): List<HistoryEntry> {
        val arr = JSONArray(raw)
        return List(arr.length()) { i ->
            val pair = arr.getJSONArray(i)
            HistoryEntry(pair.getString(0), pair.getString(1))
        }
    }

    companion object {
        const val MAX_ENTRIES = 50
    }
}

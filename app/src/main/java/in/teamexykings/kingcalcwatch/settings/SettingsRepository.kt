package `in`.teamexykings.kingcalcwatch.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "king_calc_watch_settings")

/** Persists theme, accent color, and compact/expanded mode across launches —
 * DataStore, not a database, since this is just a handful of small values. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = longPreferencesKey("accent_color_argb")
        val HAS_ACCENT_OVERRIDE = booleanPreferencesKey("has_accent_override")
        val CALCULATOR_MODE = stringPreferencesKey("calculator_mode")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_STYLE = stringPreferencesKey("key_style")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { raw ->
                runCatching { ThemeMode.valueOf(raw) }.getOrNull()
            } ?: ThemeMode.PITCH_BLACK,
            accentColorArgb = if (prefs[Keys.HAS_ACCENT_OVERRIDE] == true) prefs[Keys.ACCENT_COLOR] else null,
            calculatorMode = prefs[Keys.CALCULATOR_MODE]?.let { raw ->
                runCatching { CalculatorMode.valueOf(raw) }.getOrNull()
            } ?: CalculatorMode.COMPACT,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: false,
            keyStyle = prefs[Keys.KEY_STYLE]?.let { raw ->
                runCatching { KeyStyle.valueOf(raw) }.getOrNull()
            } ?: KeyStyle.ROUND
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentColor(argb: Long?) {
        context.settingsDataStore.edit { prefs ->
            if (argb == null) {
                prefs[Keys.HAS_ACCENT_OVERRIDE] = false
            } else {
                prefs[Keys.HAS_ACCENT_OVERRIDE] = true
                prefs[Keys.ACCENT_COLOR] = argb
            }
        }
    }

    suspend fun setCalculatorMode(mode: CalculatorMode) {
        context.settingsDataStore.edit { it[Keys.CALCULATOR_MODE] = mode.name }
    }

    suspend fun setKeyStyle(style: KeyStyle) {
        context.settingsDataStore.edit { it[Keys.KEY_STYLE] = style.name }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }
}

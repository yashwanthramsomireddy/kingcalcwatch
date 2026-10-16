package `in`.teamexykings.kingcalcwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import `in`.teamexykings.kingcalcwatch.currency.CurrencyRepository
import `in`.teamexykings.kingcalcwatch.history.HistoryRepository
import `in`.teamexykings.kingcalcwatch.settings.SettingsRepository
import `in`.teamexykings.kingcalcwatch.units.UnitsRepository

/**
 * Single-activity entry point. The calculator is fully offline; the only
 * network use is the currency converter fetching public exchange rates.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepo = SettingsRepository(applicationContext)
        val currencyRepo = CurrencyRepository(applicationContext)
        val unitsRepo = UnitsRepository(applicationContext)
        val historyRepo = HistoryRepository(applicationContext)

        setContent {
            AppRoot(
                settingsRepo = settingsRepo,
                currencyRepo = currencyRepo,
                unitsRepo = unitsRepo,
                historyRepo = historyRepo
            )
        }
    }
}

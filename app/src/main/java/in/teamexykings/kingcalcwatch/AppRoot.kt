package `in`.teamexykings.kingcalcwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import `in`.teamexykings.kingcalcwatch.currency.CurrencyRepository
import `in`.teamexykings.kingcalcwatch.currency.CurrencyState
import `in`.teamexykings.kingcalcwatch.currency.RefreshResult
import `in`.teamexykings.kingcalcwatch.history.HistoryRepository
import `in`.teamexykings.kingcalcwatch.units.UnitCategory
import `in`.teamexykings.kingcalcwatch.units.UnitsRepository
import `in`.teamexykings.kingcalcwatch.units.UnitsState
import `in`.teamexykings.kingcalcwatch.settings.AppSettings
import `in`.teamexykings.kingcalcwatch.settings.SettingsRepository
import `in`.teamexykings.kingcalcwatch.ui.theme.KingCalcWatchTheme
import `in`.teamexykings.kingcalcwatch.ui.theme.LocalKcColors
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_ABOUT = "about"
private const val ROUTE_PICK_FROM = "pick_from"
private const val ROUTE_PICK_TO = "pick_to"
private const val ROUTE_HISTORY = "history"
private const val ROUTE_PICK_CATEGORY = "pick_category"
private const val ROUTE_PICK_UNIT_FROM = "pick_unit_from"
private const val ROUTE_PICK_UNIT_TO = "pick_unit_to"

private const val PAGE_CALCULATOR = 0
private const val PAGE_CURRENCY = 1
private const val PAGE_UNITS = 2

@Composable
fun AppRoot(
    settingsRepo: SettingsRepository,
    currencyRepo: CurrencyRepository,
    unitsRepo: UnitsRepository,
    historyRepo: HistoryRepository
) {
    val scope = rememberCoroutineScope()
    val settings by settingsRepo.settings.collectAsState(initial = AppSettings.DEFAULT)
    val currencyState by currencyRepo.state.collectAsState(initial = CurrencyState())
    val unitsState by unitsRepo.state.collectAsState(initial = UnitsState())
    val historyEntries by historyRepo.entries.collectAsState(initial = emptyList())

    var isRefreshing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun refreshRates() {
        if (isRefreshing) return
        scope.launch {
            isRefreshing = true
            statusMessage = when (val result = currencyRepo.refresh()) {
                is RefreshResult.Success -> null
                is RefreshResult.Failed ->
                    if (currencyState.snapshot != null) "Offline — using saved rates" else "Couldn't fetch rates"
            }
            isRefreshing = false
        }
    }

    // Optional (off by default): hold the screen on while the app is in the foreground.
    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    KingCalcWatchTheme(
        themeMode = settings.themeMode,
        accentColorArgb = settings.accentColorArgb,
        keyStyle = settings.keyStyle
    ) {
        val navController = rememberSwipeDismissableNavController()

        Box(modifier = Modifier.fillMaxSize().background(LocalKcColors.current.background)) {
            SwipeDismissableNavHost(navController = navController, startDestination = ROUTE_HOME) {

                composable(ROUTE_HOME) {
                    val pagerState = rememberPagerState(initialPage = PAGE_CALCULATOR, pageCount = { 3 })

                    // Sync rates the first time the converter page is shown (and whenever
                    // it's revisited with stale rates). Calculator stays fully offline.
                    LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.currentPage == PAGE_CURRENCY && currencyRepo.isStale(currencyState.snapshot)) {
                            refreshRates()
                        }
                    }

                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        when (page) {
                            PAGE_CALCULATOR -> CalculatorScreen(
                                calculatorMode = settings.calculatorMode,
                                onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
                                onHistoryClick = { navController.navigate(ROUTE_HISTORY) },
                                onRecordHistory = { entry -> scope.launch { historyRepo.add(entry) } }
                            )
                            PAGE_UNITS -> UnitsScreen(
                                state = unitsState,
                                onPickCategory = { navController.navigate(ROUTE_PICK_CATEGORY) },
                                onPickFrom = { navController.navigate(ROUTE_PICK_UNIT_FROM) },
                                onPickTo = { navController.navigate(ROUTE_PICK_UNIT_TO) },
                                onSelectTo = { id -> scope.launch { unitsRepo.setTo(unitsState.category, id) } },
                                onSwap = { scope.launch { unitsRepo.swap(unitsState.category) } },
                                onSettingsClick = { navController.navigate(ROUTE_SETTINGS) }
                            )
                            else -> CurrencyScreen(
                                state = currencyState,
                                isRefreshing = isRefreshing,
                                statusMessage = statusMessage,
                                onPickFrom = { navController.navigate(ROUTE_PICK_FROM) },
                                onPickTo = { navController.navigate(ROUTE_PICK_TO) },
                                onSelectTo = { code -> scope.launch { currencyRepo.setTo(code) } },
                                onSwap = { scope.launch { currencyRepo.swap() } },
                                onRefresh = { refreshRates() },
                                onSettingsClick = { navController.navigate(ROUTE_SETTINGS) }
                            )
                        }
                    }
                }

                composable(ROUTE_SETTINGS) {
                    SettingsScreen(
                        settings = settings,
                        onThemeModeChange = { scope.launch { settingsRepo.setThemeMode(it) } },
                        onAccentChange = { scope.launch { settingsRepo.setAccentColor(it) } },
                        onCalculatorModeChange = { scope.launch { settingsRepo.setCalculatorMode(it) } },
                        onKeepScreenOnChange = { scope.launch { settingsRepo.setKeepScreenOn(it) } },
                        onKeyStyleChange = { scope.launch { settingsRepo.setKeyStyle(it) } },
                        onAboutClick = { navController.navigate(ROUTE_ABOUT) }
                    )
                }

                composable(ROUTE_ABOUT) { AboutScreen() }

                composable(ROUTE_HISTORY) {
                    HistoryScreen(
                        entries = historyEntries,
                        onClear = { scope.launch { historyRepo.clear() } }
                    )
                }

                composable(ROUTE_PICK_CATEGORY) {
                    ChoiceListScreen(
                        title = "Category",
                        options = UnitCategory.entries.map { ChoiceOption(it.name, it.label) },
                        selectedId = unitsState.category.name,
                        onPick = { name ->
                            scope.launch { unitsRepo.setCategory(UnitCategory.valueOf(name)) }
                            navController.popBackStack()
                        }
                    )
                }

                composable(ROUTE_PICK_UNIT_FROM) {
                    ChoiceListScreen(
                        title = "Convert from",
                        options = unitsState.category.units.map { ChoiceOption(it.id, it.id, it.name) },
                        selectedId = unitsState.from.id,
                        onPick = { id ->
                            scope.launch { unitsRepo.setFrom(unitsState.category, id) }
                            navController.popBackStack()
                        }
                    )
                }

                composable(ROUTE_PICK_UNIT_TO) {
                    ChoiceListScreen(
                        title = "Convert to",
                        options = unitsState.category.units.map { ChoiceOption(it.id, it.id, it.name) },
                        selectedId = unitsState.to.id,
                        onPick = { id ->
                            scope.launch { unitsRepo.setTo(unitsState.category, id) }
                            navController.popBackStack()
                        }
                    )
                }

                composable(ROUTE_PICK_FROM) {
                    CurrencyPickerScreen(
                        title = "Convert from",
                        currencies = currencyState.currencies,
                        selectedCode = currencyState.fromCode,
                        favourites = currencyState.favourites,
                        onToggleFavourite = { code -> scope.launch { currencyRepo.toggleFavourite(code) } },
                        onPick = { code ->
                            scope.launch { currencyRepo.setFrom(code) }
                            navController.popBackStack()
                        }
                    )
                }

                composable(ROUTE_PICK_TO) {
                    CurrencyPickerScreen(
                        title = "Convert to",
                        currencies = currencyState.currencies,
                        selectedCode = currencyState.toCode,
                        favourites = currencyState.favourites,
                        onToggleFavourite = { code -> scope.launch { currencyRepo.toggleFavourite(code) } },
                        onPick = { code ->
                            scope.launch { currencyRepo.setTo(code) }
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

package `in`.teamexykings.kingcalcwatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import `in`.teamexykings.kingcalcwatch.history.HistoryEntry
import `in`.teamexykings.kingcalcwatch.settings.CalculatorMode
import `in`.teamexykings.kingcalcwatch.settings.KeyStyle
import `in`.teamexykings.kingcalcwatch.ui.theme.LocalKeyStyle
import `in`.teamexykings.kingcalcwatch.ui.theme.LocalKcColors
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

private val CalculatorStateSaver = Saver<CalculatorState, List<Any>>(
    save = {
        listOf(
            it.display,
            it.expression,
            it.firstOperand?.toPlainString() ?: "",
            it.pendingOperator?.name ?: "",
            it.startFreshOnNextDigit,
            it.isError,
            it.memory.toPlainString()
        )
    },
    restore = { saved ->
        CalculatorState(
            display = saved[0] as String,
            expression = saved[1] as String,
            firstOperand = (saved[2] as String).takeIf { it.isNotEmpty() }?.toBigDecimal(),
            pendingOperator = (saved[3] as String).takeIf { it.isNotEmpty() }?.let { Operator.valueOf(it) },
            startFreshOnNextDigit = saved[4] as Boolean,
            isError = saved[5] as Boolean,
            memory = (saved[6] as String).toBigDecimalOrNull() ?: BigDecimal.ZERO
        )
    }
)

// Compact-mode layout budget, in dp (plain floats so the round-screen maths below is easy).
private const val TopInset = 8f
private const val HeaderHeight = 62f          // expression line + main number (gear sits inside this row)
private const val RowGap = 3f
private const val CompactColumnGap = 5f
private const val SideMargin = 8f
private const val EdgeMargin = 6f             // breathing room between a row's ends and the bezel
// The bottom row may only be as low as the point where the circle is still this
// fraction of the screen width — below that the keys would be too narrow to tap.
private const val MinBottomRowFraction = 0.56f

private val ColumnSpacing = 6.dp

@Composable
fun CalculatorScreen(
    calculatorMode: CalculatorMode,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onRecordHistory: (HistoryEntry) -> Unit
) {
    var state by rememberSaveable(stateSaver = CalculatorStateSaver) {
        mutableStateOf(CalculatorState.INITIAL)
    }

    val onKeyPress: (CalculatorKey) -> Unit = { key ->
        key.toAction()?.let { action ->
            val before = state
            val after = CalculatorEngine.reduce(before, action)
            // A finished calculation (12 + 8 =) goes into the history.
            val first = before.firstOperand
            val operator = before.pendingOperator
            if (action is CalculatorAction.Equals && first != null && operator != null && !after.isError) {
                onRecordHistory(
                    HistoryEntry(
                        expression = "${first.stripTrailingZeros().toPlainString()} ${operator.symbol} ${before.display}",
                        result = after.display
                    )
                )
            }
            state = after
        }
    }

    when (calculatorMode) {
        CalculatorMode.COMPACT -> CompactCalculator(state, onKeyPress, onSettingsClick, onHistoryClick)
        CalculatorMode.EXPANDED -> ExpandedCalculator(state, onKeyPress, onSettingsClick, onHistoryClick)
    }
}

@Composable
private fun CompactCalculator(
    state: CalculatorState,
    onKeyPress: (CalculatorKey) -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    // No Vignette / TimeText here: every dp of the round screen goes to the keys.
    Scaffold(timeText = {}) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val w = maxWidth.value
            val h = maxHeight.value
            val radius = minOf(w, h) / 2f
            val cy = h / 2f
            val rows = CompactKeyRows
            // Classic = old-school grid: tighter gaps so the keys read as a boxy block.
            val classic = LocalKeyStyle.current == KeyStyle.CLASSIC
            val rowGap = if (classic) 2f else RowGap
            val columnGap = if (classic) 3f else CompactColumnGap

            // Width of the circle (the usable chord) at a given distance from the centre line.
            fun chordAt(d: Float): Float =
                2f * sqrt(max(radius * radius - d * d, 0f))

            val keysTop = TopInset + HeaderHeight + rowGap
            val bottomLimit = cy + sqrt(
                max(radius * radius - (MinBottomRowFraction * w / 2f).let { it * it }, 0f)
            )
            val rowHeight = max(
                ((bottomLimit - keysTop - rowGap * (rows.size - 1)) / rows.size),
                16f
            )

            // Gear sits in the top-right corner of the header, tucked inside the circle,
            // so it never overlaps the big number underneath.
            val gearSize = 24f
            val gearCenterY = TopInset + gearSize / 2f
            val gearEnd = max((w - chordAt(abs(gearCenterY - cy))) / 2f + 12f, 0f)

            Box(modifier = Modifier.fillMaxSize()) {
                // Header: display centred, gear tucked at the left of the same row.
                Box(
                    modifier = Modifier
                        .padding(top = TopInset.dp)
                        .fillMaxWidth()
                        .height(HeaderHeight.dp)
                ) {
                    DisplayArea(
                        state = state,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    HistoryButton(
                        onClick = onHistoryClick,
                        size = gearSize.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = gearEnd.dp)
                    )
                    SettingsGearButton(
                        onClick = onSettingsClick,
                        size = gearSize.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = gearEnd.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = keysTop.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(rowGap.dp)
                ) {
                    rows.forEachIndexed { i, row ->
                        val top = keysTop + i * (rowHeight + rowGap)
                        val bottom = top + rowHeight
                        val farthest = max(abs(top - cy), abs(bottom - cy))
                        val rowWidth = minOf(w - 2 * SideMargin, chordAt(farthest) - 2 * EdgeMargin)
                        val keyWidth = max((rowWidth - columnGap * 3) / 4f, 16f)
                        CalculatorKeyRow(
                            keys = row,
                            buttonWidth = keyWidth.dp,
                            buttonHeight = rowHeight.dp,
                            columnSpacing = columnGap.dp,
                            onKeyPress = onKeyPress
                        )
                    }
                }
            }
        }
    }
}

// Rotary/focus APIs below are still experimental in Wear Compose Foundation 1.4.
@OptIn(ExperimentalWearFoundationApi::class)
@Composable
private fun ExpandedCalculator(
    state: CalculatorState,
    onKeyPress: (CalculatorKey) -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = rememberActiveFocusRequester()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val horizontalInset = maxWidth * 0.09f
            val gridWidth = maxWidth - horizontalInset * 2
            val buttonSize = (gridWidth - ColumnSpacing * 3) / 4

            ScalingLazyColumn(
                state = listState,
                anchorType = ScalingLazyListAnchorType.ItemStart,
                autoCentering = null,
                // Flat items: a fixed calculator grid, not a list that should
                // fade/shrink toward the screen edges.
                scalingParams = ScalingLazyColumnDefaults.scalingParams(edgeScale = 1f, edgeAlpha = 1f),
                contentPadding = PaddingValues(
                    top = 28.dp,
                    bottom = 44.dp, // keeps the last row (0 . = +) clear of the round bezel
                    start = horizontalInset,
                    end = horizontalInset
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .rotaryScrollable(
                        behavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
                        focusRequester = focusRequester
                    )
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HistoryButton(onClick = onHistoryClick)
                        SettingsGearButton(onClick = onSettingsClick)
                    }
                }
                item { DisplayArea(state = state, modifier = Modifier.fillMaxWidth()) }

                items(ExpandedKeyRows) { row ->
                    CalculatorKeyRow(
                        keys = row,
                        buttonWidth = buttonSize,
                        buttonHeight = buttonSize,
                        onKeyPress = onKeyPress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplayArea(state: CalculatorState, modifier: Modifier = Modifier) {
    val kc = LocalKcColors.current
    Column(
        modifier = modifier.padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Small running-expression line, e.g. "12 +" — keeps a mid-chain glance
        // meaningful. Shows a tiny "M" marker when memory holds a value.
        val memoryMarker = if (state.memory.signum() != 0) "M  " else ""
        BasicText(
            text = (memoryMarker + state.expression).ifEmpty { " " },
            style = TextStyle(
                color = kc.secondaryText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            ),
            maxLines = 1
        )
        BasicText(
            text = state.display,
            style = TextStyle(
                color = if (state.isError) kc.error else kc.onSurface,
                fontSize = displayFontSize(state.display),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            maxLines = 1
        )
    }
}

/** Shrinks the main number as digits grow, so a long result never clips on
 * the narrow round display. */
private fun displayFontSize(display: String) = when {
    display.length <= 6 -> 30.sp
    display.length <= 8 -> 24.sp
    display.length <= 10 -> 20.sp
    else -> 17.sp
}

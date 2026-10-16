package `in`.teamexykings.kingcalcwatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import `in`.teamexykings.kingcalcwatch.currency.CurrencyConverter
import `in`.teamexykings.kingcalcwatch.currency.CurrencyState
import `in`.teamexykings.kingcalcwatch.ui.theme.LocalKcColors
import kotlinx.coroutines.launch

internal val KeypadRows = listOf(
    listOf("7", "8", "9"),
    listOf("4", "5", "6"),
    listOf("1", "2", "3"),
    listOf(".", "0", "⌫")
)

/**
 * Currency converter page. Unlike the Compact calculator this one scrolls
 * with the rotary bezel — it has more to show (pickers, result, sync status),
 * and converting is a deliberate action rather than a glance.
 */
@Composable
fun CurrencyScreen(
    state: CurrencyState,
    isRefreshing: Boolean,
    statusMessage: String?,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onSelectTo: (String) -> Unit,
    onSwap: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val kc = LocalKcColors.current
    var amountText by rememberSaveable { mutableStateOf("1") }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val gridWidth = screenWidth * 0.66f
    val keyWidth = (gridWidth - 8.dp) / 3
    val keyHeight = 30.dp

    val snapshot = state.snapshot
    val converted = snapshot?.let {
        CurrencyConverter.convert(CurrencyConverter.parseAmount(amountText), state.fromCode, state.toCode, it)
    }

    val listState = rememberScalingLazyListState()
    val scope = rememberCoroutineScope()

    // Every currency: the left (base) one first, then favourites, then the rest A-Z.
    val sortedAll = state.currencies.sortedBy { it.code }
    val rowList = sortedAll.filter { it.code == state.fromCode } +
        sortedAll.filter { it.code != state.fromCode && it.code in state.favourites } +
        sortedAll.filter { it.code != state.fromCode && it.code !in state.favourites }
    val amount = CurrencyConverter.parseAmount(amountText)

    RotaryScreenList(listState = listState) {
        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                SettingsGearButton(onClick = onSettingsClick, size = 34.dp)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrencyPill(code = state.fromCode, onClick = onPickFrom)
                Button(
                    onClick = onSwap,
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = kc.surfaceOperator,
                        contentColor = kc.onSurface
                    )
                ) { Text("⇄", style = MaterialTheme.typography.caption1) }
                CurrencyPill(code = state.toCode, onClick = onPickTo)
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$amountText ${state.fromCode}",
                        style = MaterialTheme.typography.caption1,
                        color = kc.secondaryText,
                        maxLines = 1
                    )
                    ClearButton(onClick = { amountText = "0" })
                }
                Text(
                    text = if (converted != null) {
                        "${CurrencyConverter.formatAmount(converted)} ${state.toCode}"
                    } else {
                        "—"
                    },
                    style = MaterialTheme.typography.title2,
                    color = kc.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        items(KeypadRows.size) { rowIndex ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                for (key in KeypadRows[rowIndex]) {
                    KeypadButton(label = key, width = keyWidth, height = keyHeight) {
                        amountText = CurrencyConverter.applyKey(amountText, key)
                    }
                }
            }
        }

        item {
            Text(
                text = statusText(isRefreshing, statusMessage, snapshot?.fetchedAtMillis),
                style = MaterialTheme.typography.caption2,
                color = kc.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRefresh,
                enabled = !isRefreshing,
                label = { Text(if (isRefreshing) "Updating…" else "Refresh rates") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }

        // King Calc-style: the typed amount converted into every currency at once.
        if (snapshot != null && rowList.isNotEmpty()) {
            item { ListHeader { Text("All currencies") } }
            items(rowList.size) { i ->
                val c = rowList[i]
                val value = CurrencyConverter.convert(amount, state.fromCode, c.code, snapshot)
                val isTo = c.code == state.toCode
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // The base (left) currency can't also be the right-hand one.
                        if (c.code != state.fromCode) {
                            onSelectTo(c.code)
                            scope.launch { listState.animateScrollToItem(0) }
                        }
                    },
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${flagFor(c.code)}${c.code}", maxLines = 1)
                            Text(value?.let { CurrencyConverter.formatAmount(it) } ?: "—", maxLines = 1)
                        }
                    },
                    secondaryLabel = { Text(c.name, maxLines = 1) },
                    colors = if (isTo) {
                        ChipDefaults.chipColors(
                            backgroundColor = kc.accent,
                            contentColor = kc.onAccent,
                            secondaryContentColor = kc.onAccent
                        )
                    } else {
                        ChipDefaults.chipColors(
                            backgroundColor = kc.surfaceOperator,
                            contentColor = kc.onSurface
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CurrencyPill(code: String, onClick: () -> Unit) {
    val kc = LocalKcColors.current
    CompactChip(
        onClick = onClick,
        label = {
            Text(
                text = code,
                style = MaterialTheme.typography.caption1,
                maxLines = 1
            )
        },
        colors = ChipDefaults.chipColors(
            backgroundColor = kc.surfaceDigit,
            contentColor = kc.onSurface
        )
    )
}

@Composable
internal fun KeypadButton(label: String, width: Dp, height: Dp, onClick: () -> Unit) {
    val kc = LocalKcColors.current
    val isDigit = label.firstOrNull()?.isDigit() == true
    Button(
        onClick = onClick,
        modifier = Modifier.size(width = width, height = height),
        shape = keyShape(height),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isDigit) kc.surfaceDigit else kc.surfaceOperator,
            contentColor = kc.onSurface
        )
    ) { Text(label, style = MaterialTheme.typography.button) }
}

private fun statusText(isRefreshing: Boolean, message: String?, fetchedAtMillis: Long?): String = when {
    isRefreshing -> "Updating rates…"
    message != null -> message
    fetchedAtMillis != null -> "Rates updated ${relativeTime(fetchedAtMillis)}"
    else -> "No rates yet"
}

private fun relativeTime(thenMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val minutes = ((nowMillis - thenMillis) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}

package `in`.teamexykings.kingcalcwatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import `in`.teamexykings.kingcalcwatch.ui.theme.LocalKcColors
import `in`.teamexykings.kingcalcwatch.units.UnitCategory
import `in`.teamexykings.kingcalcwatch.units.UnitConverter
import `in`.teamexykings.kingcalcwatch.units.UnitsState
import kotlinx.coroutines.launch

/**
 * Unit converter, laid out like the currency page: category, from ⇄ to, amount
 * with a red C, keypad, then every unit in the category converted at once.
 */
@Composable
fun UnitsScreen(
    state: UnitsState,
    onPickCategory: () -> Unit,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onSelectTo: (String) -> Unit,
    onSwap: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val kc = LocalKcColors.current
    var amountText by rememberSaveable { mutableStateOf("1") }
    val listState = rememberScalingLazyListState()
    val scope = rememberCoroutineScope()

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val gridWidth = screenWidth * 0.66f
    val keyWidth = (gridWidth - 8.dp) / 3

    val category = state.category
    val amount = CurrencyConverter.parseAmount(amountText)
    val converted = UnitConverter.convert(amount, category, state.from, state.to)
    // The "from" unit first, then the rest in their natural order.
    val rowList = listOf(state.from) + category.units.filter { it.id != state.from.id }

    fun onKey(key: String) {
        amountText = if (key == "⌫" && amountText.length <= 2 && amountText.startsWith("-")) {
            "0"
        } else {
            CurrencyConverter.applyKey(amountText, key)
        }
    }

    RotaryScreenList(listState = listState) {
        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                SettingsGearButton(onClick = onSettingsClick, size = 34.dp)
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                UnitPill(text = category.label, onClick = onPickCategory)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UnitPill(text = state.from.id, onClick = onPickFrom)
                Button(
                    onClick = onSwap,
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = kc.surfaceOperator,
                        contentColor = kc.onSurface
                    )
                ) { Text("⇄", style = MaterialTheme.typography.caption1) }
                UnitPill(text = state.to.id, onClick = onPickTo)
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
                        text = "$amountText ${state.from.id}",
                        style = MaterialTheme.typography.caption1,
                        color = kc.secondaryText,
                        maxLines = 1
                    )
                    ClearButton(onClick = { amountText = "0" })
                    if (category == UnitCategory.TEMPERATURE) {
                        // Temperatures can be negative — a sign toggle only where it matters.
                        Button(
                            onClick = {
                                amountText = when {
                                    amountText == "0" -> "0"
                                    amountText.startsWith("-") -> amountText.removePrefix("-")
                                    else -> "-$amountText"
                                }
                            },
                            modifier = Modifier.size(26.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = kc.surfaceOperator,
                                contentColor = kc.onSurface
                            )
                        ) { Text("±", style = MaterialTheme.typography.caption1) }
                    }
                }
                Text(
                    text = "${UnitConverter.format(converted)} ${state.to.id}",
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
                    KeypadButton(label = key, width = keyWidth, height = 30.dp) { onKey(key) }
                }
            }
        }

        item { ListHeader { Text("All ${category.label.lowercase()} units") } }
        items(rowList.size) { i ->
            val unit = rowList[i]
            val value = UnitConverter.convert(amount, category, state.from, unit)
            val isTo = unit.id == state.to.id
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    // The "from" unit can't also be the "to" unit.
                    if (unit.id != state.from.id) {
                        onSelectTo(unit.id)
                        scope.launch { listState.animateScrollToItem(0) }
                    }
                },
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(unit.id, maxLines = 1)
                        Text(UnitConverter.format(value), maxLines = 1)
                    }
                },
                secondaryLabel = { Text(unit.name, maxLines = 1) },
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

@Composable
private fun UnitPill(text: String, onClick: () -> Unit) {
    val kc = LocalKcColors.current
    CompactChip(
        onClick = onClick,
        label = { Text(text = text, style = MaterialTheme.typography.caption1, maxLines = 1) },
        colors = ChipDefaults.chipColors(
            backgroundColor = kc.surfaceDigit,
            contentColor = kc.onSurface
        )
    )
}

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import `in`.teamexykings.kingcalcwatch.currency.CurrencyInfo
import `in`.teamexykings.kingcalcwatch.ui.theme.LocalKcColors
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Every supported currency. Starred ones are pinned in a Favourites section at
 * the top; the rest follow A-Z, with a "Jump to letter" grid so ~160 rows
 * don't need endless scrolling. Tap a row to select; tap its star to favourite.
 */
@Composable
fun CurrencyPickerScreen(
    title: String,
    currencies: List<CurrencyInfo>,
    selectedCode: String,
    favourites: Set<String>,
    onToggleFavourite: (String) -> Unit,
    onPick: (String) -> Unit
) {
    val kc = LocalKcColors.current
    val listState = rememberScalingLazyListState()
    val scope = rememberCoroutineScope()
    var showLetters by remember { mutableStateOf(false) }

    val sorted = currencies.sortedBy { it.code }
    val favouriteList = sorted.filter { it.code in favourites }
    val others = sorted.filter { it.code !in favourites }

    // Item layout: 0 header, 1 jump chip, then Favourites (header + rows),
    // then "All currencies" (header + rows). Used to compute scroll targets.
    val favouriteBlock = if (favouriteList.isEmpty()) 0 else 1 + favouriteList.size
    val allHeaderIndex = 2 + favouriteBlock
    val firstIndexForLetter: Map<Char, Int> = buildMap {
        others.forEachIndexed { i, c ->
            val letter = c.code.first()
            if (letter !in this) put(letter, allHeaderIndex + 1 + i)
        }
    }

    RotaryScreenList(listState = listState) {
        item { ListHeader { Text(title) } }

        if (currencies.isEmpty()) {
            item {
                Text(
                    text = "No currency list yet.\nConnect to the internet and tap Refresh rates on the converter.",
                    style = MaterialTheme.typography.caption2,
                    color = kc.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
            return@RotaryScreenList
        }

        // One fixed item whose content toggles, so row indices never shift.
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!showLetters) {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showLetters = true },
                        label = { Text("Jump to letter  A–Z", maxLines = 1) },
                        colors = ChipDefaults.secondaryChipColors()
                    )
                } else {
                    firstIndexForLetter.keys.sorted().chunked(5).forEach { rowLetters ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            rowLetters.forEach { letter ->
                                Button(
                                    modifier = Modifier.size(34.dp),
                                    shape = CircleShape,
                                    onClick = {
                                        showLetters = false
                                        scope.launch { listState.scrollToItem(firstIndexForLetter.getValue(letter)) }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = kc.surfaceOperator,
                                        contentColor = kc.onSurface
                                    )
                                ) { Text(letter.toString()) }
                            }
                        }
                    }
                }
            }
        }

        if (favouriteList.isNotEmpty()) {
            item { ListHeader { Text("★ Favourites") } }
            items(favouriteList.size) { i ->
                val c = favouriteList[i]
                CurrencyRow(c, c.code == selectedCode, true, onPick, onToggleFavourite)
            }
        }

        item { ListHeader { Text("All currencies") } }
        items(others.size) { i ->
            val c = others[i]
            CurrencyRow(c, c.code == selectedCode, false, onPick, onToggleFavourite)
        }
    }
}

@Composable
private fun CurrencyRow(
    currency: CurrencyInfo,
    selected: Boolean,
    isFavourite: Boolean,
    onPick: (String) -> Unit,
    onToggleFavourite: (String) -> Unit
) {
    val kc = LocalKcColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Chip(
            modifier = Modifier.weight(1f),
            onClick = { onPick(currency.code) },
            label = { Text("${flagFor(currency.code)}${currency.code}", maxLines = 1) },
            secondaryLabel = { Text(currency.name, maxLines = 1) },
            colors = if (selected) {
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
        Button(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            onClick = { onToggleFavourite(currency.code) },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = kc.background,
                contentColor = if (isFavourite) kc.accent else kc.secondaryText
            )
        ) { Text(if (isFavourite) "★" else "☆") }
    }
}

private val IsoCountries: Set<String> = Locale.getISOCountries().toSet()

/** "🇮🇳 " for INR, "🇪🇺 " for EUR; empty for codes with no country (metals, XDR, ...). */
internal fun flagFor(code: String): String {
    val region = if (code == "EUR") "EU" else code.take(2)
    if (region != "EU" && region !in IsoCountries) return ""
    if (code.startsWith("X")) return ""
    val flag = region.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
    return "$flag "
}

package `in`.teamexykings.kingcalcwatch

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import `in`.teamexykings.kingcalcwatch.ui.theme.LocalKcColors

/** One selectable row: [id] is what gets reported back, [secondary] is optional small text. */
data class ChoiceOption(val id: String, val label: String, val secondary: String? = null)

/** Generic single-choice list (units categories and units). */
@Composable
fun ChoiceListScreen(
    title: String,
    options: List<ChoiceOption>,
    selectedId: String,
    onPick: (String) -> Unit
) {
    val kc = LocalKcColors.current
    RotaryScreenList {
        item { ListHeader { Text(title) } }
        items(options.size) { i ->
            val option = options[i]
            val selected = option.id == selectedId
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onPick(option.id) },
                label = { Text(option.label, maxLines = 1) },
                secondaryLabel = option.secondary?.let { text -> ({ Text(text, maxLines = 1) }) },
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
        }
    }
}

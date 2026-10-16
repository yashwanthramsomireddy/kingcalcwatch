package `in`.teamexykings.kingcalcwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import `in`.teamexykings.kingcalcwatch.history.HistoryEntry
import `in`.teamexykings.kingcalcwatch.ui.theme.LocalKcColors

/** Recent calculations, newest first, with a one-tap Clear. */
@Composable
fun HistoryScreen(entries: List<HistoryEntry>, onClear: () -> Unit) {
    val kc = LocalKcColors.current

    RotaryScreenList {
        item { ListHeader { Text("History") } }

        if (entries.isEmpty()) {
            item {
                Text(
                    text = "No calculations yet.\nResults appear here when you press =.",
                    style = MaterialTheme.typography.caption2,
                    color = kc.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        } else {
            items(entries.size) { i ->
                val e = entries[i]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(kc.surfaceOperator, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = e.expression,
                        style = MaterialTheme.typography.caption2,
                        color = kc.secondaryText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "= ${e.result}",
                        style = MaterialTheme.typography.title3,
                        color = kc.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClear,
                    label = { Text("Clear history", textAlign = TextAlign.Center) },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = ClearRed,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                )
            }
        }
    }
}

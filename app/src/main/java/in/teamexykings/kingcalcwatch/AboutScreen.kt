package `in`.teamexykings.kingcalcwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import `in`.teamexykings.kingcalcwatch.ui.theme.LocalKcColors

@Composable
fun AboutScreen() {
    RotaryScreenList {
        item { ListHeader { Text("About") } }

        item { InfoCard("King Calc", big = true) }
        item { InfoCard("Watch edition  ·  v${BuildConfig.VERSION_NAME}") }
        item { InfoCard("A calculator and currency converter for your wrist.") }
        item { InfoCard("Built by Yashwanth Ram Somireddy") }
        item { InfoCard("TeamExyKings") }
        item { InfoCard("Free and open source (MIT).\nNo ads. No tracking.") }
        item { InfoCard("Exchange rates by ExchangeRate-API\n(exchangerate-api.com)") }

        item { InfoCard("teamexykings.in") }
    }
}

/** A non-clickable rounded card, matching the look of the website chip. */
@Composable
private fun InfoCard(text: String, big: Boolean = false) {
    val kc = LocalKcColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(kc.surfaceOperator, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if (big) MaterialTheme.typography.title3 else MaterialTheme.typography.caption1,
            color = kc.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

package `in`.teamexykings.kingcalcwatch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import `in`.teamexykings.kingcalcwatch.settings.KeyStyle
import `in`.teamexykings.kingcalcwatch.ui.theme.LocalKcColors
import `in`.teamexykings.kingcalcwatch.ui.theme.LocalKeyStyle

/**
 * Standard scrollable screen for lists (Settings, About, currency picker):
 * a ScalingLazyColumn driven by the rotating bezel/crown, with the time and
 * a scroll position indicator.
 */
@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun RotaryScreenList(
    listState: ScalingLazyListState = rememberScalingLazyListState(),
    content: ScalingLazyListScope.() -> Unit
) {
    val focusRequester = rememberActiveFocusRequester()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        modifier = Modifier.fillMaxSize()
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .rotaryScrollable(
                    behavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
                    focusRequester = focusRequester
                ),
            content = content
        )
    }
}

/**
 * Small gear-icon button shown at the top of every top-level screen
 * (Calculator, Currency Converter). Kept centered rather than pinned to a
 * corner — the round bezel clips true corners, so a centered position just
 * above the main content is the one spot that's always fully visible.
 */
@Composable
fun SettingsGearButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp
) {
    val kc = LocalKcColors.current
    Button(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = kc.background,
            contentColor = kc.secondaryText
        )
    ) {
        Text(
            text = "⚙",
            style = androidx.wear.compose.material.MaterialTheme.typography.caption2
                .copy(fontSize = (size.value * 0.62f).sp)
        )
    }
}

/** Red used for every "clear" control. */
val ClearRed = Color(0xFFD32F2F)

/** Round red C that clears the amount in one tap (Currency and Units tabs). */
@Composable
fun ClearButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.size(26.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(backgroundColor = ClearRed, contentColor = Color.White)
    ) {
        Text("C", style = androidx.wear.compose.material.MaterialTheme.typography.caption1)
    }
}

/** Small clock-face button that opens the calculation history. */
@Composable
fun HistoryButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    val kc = LocalKcColors.current
    Button(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = kc.background,
            contentColor = kc.secondaryText
        )
    ) {
        val tint = kc.secondaryText
        Canvas(modifier = Modifier.size(size * 0.62f)) {
            val stroke = this.size.minDimension * 0.11f
            val c = center
            val r = this.size.minDimension / 2f - stroke / 2f
            drawCircle(color = tint, radius = r, center = c, style = Stroke(width = stroke))
            drawLine(tint, c, Offset(c.x, c.y - r * 0.6f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(tint, c, Offset(c.x + r * 0.45f, c.y), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

/** Key outline for the chosen key style: pill (Round) or rounded square (Classic). */
@Composable
fun keyShape(height: Dp): Shape = when (LocalKeyStyle.current) {
    KeyStyle.ROUND -> RoundedCornerShape(percent = 50)
    // Old-school boxy keys: small corners, scaled to the key so short keys stay rectangular.
    KeyStyle.CLASSIC -> RoundedCornerShape(minOf(10.dp, height / 4))
}

package `in`.teamexykings.kingcalcwatch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import `in`.teamexykings.kingcalcwatch.settings.AppSettings
import `in`.teamexykings.kingcalcwatch.settings.CalculatorMode
import `in`.teamexykings.kingcalcwatch.settings.KeyStyle
import `in`.teamexykings.kingcalcwatch.settings.ThemeMode
import `in`.teamexykings.kingcalcwatch.ui.theme.AccentOption
import `in`.teamexykings.kingcalcwatch.ui.theme.AccentPresets
import `in`.teamexykings.kingcalcwatch.ui.theme.LocalKcColors

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentChange: (Long?) -> Unit,
    onCalculatorModeChange: (CalculatorMode) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onKeyStyleChange: (KeyStyle) -> Unit,
    onAboutClick: () -> Unit
) {
    RotaryScreenList {
        item { ListHeader { Text("Settings") } }

        item { SectionLabel("Theme") }
        items(ThemeMode.entries.toList()) { mode ->
            ChoiceChip(
                label = mode.label,
                selected = settings.themeMode == mode,
                onClick = { onThemeModeChange(mode) }
            )
        }

        item { SectionLabel("Calculator") }
        items(CalculatorMode.entries.toList()) { mode ->
            ChoiceChip(
                label = mode.label,
                selected = settings.calculatorMode == mode,
                onClick = { onCalculatorModeChange(mode) }
            )
        }

        item { SectionLabel("Key style") }
        items(KeyStyle.entries.toList()) { style ->
            ChoiceChip(
                label = style.label,
                selected = settings.keyStyle == style,
                onClick = { onKeyStyleChange(style) }
            )
        }

        item { SectionLabel("Accent color") }
        // Two rows of swatches — nine in one row would overflow the round screen.
        items(AccentPresets.chunked(5)) { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                for (option in rowOptions) {
                    AccentSwatch(
                        option = option,
                        selected = option.color?.toStoredArgb() == settings.accentColorArgb,
                        onClick = { onAccentChange(option.color?.toStoredArgb()) }
                    )
                }
            }
        }

        item { SectionLabel("Screen") }
        item {
            ChoiceChip(
                label = "Keep screen on",
                selected = settings.keepScreenOn,
                selectedText = "On",
                unselectedText = "Off",
                onClick = { onKeepScreenOnChange(!settings.keepScreenOn) }
            )
        }
        item {
            Text(
                text = "⚠ Caution: the screen stays lit while King Calc is open, which drains the battery much faster. Turn off when you don't need it.",
                style = MaterialTheme.typography.caption2,
                color = LocalKcColors.current.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        item { SectionLabel("") }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAboutClick,
                label = { Text("About") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

/** ARGB as an unsigned Long — what we persist, and what Theme turns back into a Color. */
private fun Color.toStoredArgb(): Long = toArgb().toLong() and 0xFFFFFFFFL

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption2,
        color = LocalKcColors.current.secondaryText,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedText: String = "Selected",
    unselectedText: String? = null
) {
    val kc = LocalKcColors.current
    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        label = { Text(label) },
        secondaryLabel = when {
            selected -> ({ Text(selectedText) })
            unselectedText != null -> ({ Text(unselectedText) })
            else -> null
        },
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

@Composable
private fun AccentSwatch(option: AccentOption, selected: Boolean, onClick: () -> Unit) {
    val kc = LocalKcColors.current
    // "Classic" has no color of its own — show the theme's neutral accent
    // with a ring so it reads as "default" rather than a missing swatch.
    val fill = option.color ?: kc.onSurface
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(fill)
            .border(
                BorderStroke(if (selected) 3.dp else 1.dp, if (selected) kc.onSurface else kc.secondaryText),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // (No letter on the default swatch — a "C" read like the Clear key.)
    }
}

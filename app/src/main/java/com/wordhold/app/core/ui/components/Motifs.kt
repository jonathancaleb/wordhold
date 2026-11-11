package com.wordhold.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.em
import com.wordhold.app.core.ui.theme.Dimens
import com.wordhold.app.core.ui.theme.Spacing
import com.wordhold.app.core.ui.theme.Stroke
import com.wordhold.app.core.ui.theme.WordholdTheme

/*
 * The small pictures on the game cards. Each shows how its game plays. They are
 * decoration: the card that holds one hides it from screen readers.
 */

/** Decorative tilts, in degrees, applied to a run of tiles in turn. */
val TILTS = listOf(-5f, 3f, -3f, 4f, -4f)

/** A jumble of letters, and under it the word they make. */
@Composable
fun UnscrambleMotif() {
    val colors = WordholdTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        WordPill("TLAE", colors.butter, tilt = -3f)
        WordPill("LATE", colors.periwinkle, tilt = 2f, modifier = Modifier.padding(start = Spacing.lg))
    }
}

/** Loose letters over a word that has been marked as found. */
@Composable
fun WordSearchMotif() {
    val colors = WordholdTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        BasicText(
            text = "KQWXB",
            style = WordholdTheme.type.chip.copy(color = colors.ink, letterSpacing = 0.3.em),
            modifier = Modifier.padding(start = Spacing.sm),
        )
        WordPill("FOUND", colors.tileCorrect, letterSpacing = 0.3f, pill = true)
    }
}

/** A three by three corner of a crossword grid. */
@Composable
fun CrosswordMotif() {
    val colors = WordholdTheme.colors
    val open = listOf(true, true, false, true, false, true, false, true, true)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs - Stroke.key / 2)) {
        open.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs - Stroke.key / 2)) {
                row.forEach { isOpen ->
                    Box(
                        Modifier.size(Dimens.motifCell).then(
                            if (isOpen) Modifier.outlined(colors.white, colors.ink, WordholdTheme.shapes.cell, Stroke.key)
                            else Modifier.background(colors.ink, WordholdTheme.shapes.cell)
                        )
                    )
                }
            }
        }
    }
}

/** Two digits placed and one square still to fill. */
@Composable
fun SudokuMotif() {
    val colors = WordholdTheme.colors
    val style = WordholdTheme.type.chip
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tile)) {
        LetterTile('5', Dimens.motifTile, fill = colors.white, tilt = -3f, textStyle = style)
        LetterTile('3', Dimens.motifTile, fill = colors.butter, tilt = 3f, textStyle = style)
        LetterTile(null, Dimens.motifTile)
    }
}

@Composable
private fun WordPill(
    word: String,
    fill: Color,
    tilt: Float = 0f,
    letterSpacing: Float = 0.12f,
    pill: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = WordholdTheme.colors
    val shape = if (pill) WordholdTheme.shapes.pill else WordholdTheme.shapes.tile
    Box(
        modifier
            .rotate(tilt)
            .outlined(fill, colors.ink, shape, Stroke.small)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs - Stroke.key / 2)
    ) {
        BasicText(
            text = word,
            style = WordholdTheme.type.chip.copy(color = colors.on(fill), letterSpacing = letterSpacing.em),
            maxLines = 1,
            softWrap = false,
        )
    }
}

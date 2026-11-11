package com.wordhold.app.games.sudoku

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.wordhold.app.core.ui.components.CellStatus
import com.wordhold.app.core.ui.components.KeyCap
import com.wordhold.app.core.ui.components.StatusChips
import com.wordhold.app.core.ui.components.cellFill
import com.wordhold.app.core.ui.components.pressable
import com.wordhold.app.core.ui.theme.Spacing
import com.wordhold.app.core.ui.theme.Stroke
import com.wordhold.app.core.ui.theme.WordholdTheme
import com.wordhold.app.core.ui.theme.fittedTextStyle

@Composable
fun SudokuScreen(game: SudokuGame) {
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type
    val selected = game.selected
    val selectedDigit = selected?.let { game.cells[it] }?.takeIf { it != 0 }
    val boardShape = WordholdTheme.shapes.m

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = Spacing.section, bottom = Spacing.lg)
    ) {
        StatusChips(left = "Mistakes ${game.mistakes}", right = "Score ${game.score}")
        Spacer(Modifier.height(Spacing.section))

        // Board. It runs closer to the screen edges than other content, to make its
        // 81 cells as large as a phone allows. Thin lines between cells, ink ones
        // between the 3x3 boxes, and the card outline around it all.
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm)
                .aspectRatio(1f)
                .border(Stroke.large, colors.ink, boardShape)
                .clip(boardShape)
                .background(colors.white)
                .drawWithContent {
                    drawContent()
                    val cell = size.width / 9
                    for (i in 1..8) {
                        val heavy = i % 3 == 0
                        val color = if (heavy) colors.ink else colors.emptyOutline
                        val width = (if (heavy) Stroke.key else Stroke.hairline).toPx()
                        drawLine(color, Offset(i * cell, 0f), Offset(i * cell, size.height), width)
                        drawLine(color, Offset(0f, i * cell), Offset(size.width, i * cell), width)
                    }
                }
        ) {
            // Digits the puzzle came with are set in the display face, the player's own in the body face.
            val playerStyle = fittedTextStyle(maxWidth / 9, fraction = 0.52f)
            val givenStyle = playerStyle.copy(fontFamily = type.display.fontFamily, fontWeight = type.display.fontWeight)
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 9) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 9) {
                            val index = row * 9 + col
                            val digit = game.cells[index]
                            val isSelected = index == selected
                            val isGiven = game.given[index]
                            // Only the player's own digits are judged, and only a wrong one is marked.
                            val wrong = digit != 0 && !isGiven && game.isWrong(index)
                            val related = selected != null &&
                                (sharesUnit(index, selected) || (selectedDigit != null && digit == selectedDigit))
                            val fill = colors.cellFill(
                                selected = isSelected,
                                related = related,
                                status = if (wrong) CellStatus.Wrong else CellStatus.Neutral,
                            )
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(fill)
                                    .pressable(pressedAlpha = 0.6f) { game.select(index) }
                                    .semantics {
                                        this.selected = isSelected
                                        contentDescription = "Row ${row + 1}, column ${col + 1}, " + when {
                                            digit == 0 -> "empty"
                                            wrong -> "$digit, wrong"
                                            isGiven -> "$digit, given"
                                            else -> "$digit"
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (digit != 0) {
                                    BasicText(
                                        text = digit.toString(),
                                        style = (if (isGiven) givenStyle else playerStyle).copy(color = colors.on(fill)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        // Number pad in two rows, so every key is comfortably large. A digit is
        // disabled once all nine of it are placed.
        Column(
            Modifier.padding(horizontal = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.keyRow),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tile)) {
                for (digit in 1..5) DigitKey(game, digit, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tile)) {
                for (digit in 6..9) DigitKey(game, digit, Modifier.weight(1f))
                KeyCap("Erase", onClick = { game.erase() }, modifier = Modifier.weight(1f), word = true)
            }
        }
    }
}

@Composable
private fun DigitKey(game: SudokuGame, digit: Int, modifier: Modifier) {
    KeyCap(
        label = digit.toString(),
        onClick = { game.checkAnswer(digit.toString()) },
        modifier = modifier,
        enabled = game.placedCount(digit) < 9,
    )
}

/** True if the two cells are in the same row, column or 3x3 box. */
private fun sharesUnit(a: Int, b: Int): Boolean =
    a / 9 == b / 9 || a % 9 == b % 9 || (a / 27 == b / 27 && a % 9 / 3 == b % 9 / 3)

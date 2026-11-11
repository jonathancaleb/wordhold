package com.wordhold.app.games.crossword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.wordhold.app.core.ui.components.CapsLabel
import com.wordhold.app.core.ui.components.CellStatus
import com.wordhold.app.core.ui.components.LetterKeyboard
import com.wordhold.app.core.ui.components.OutlinedCard
import com.wordhold.app.core.ui.components.SecondaryButton
import com.wordhold.app.core.ui.components.StatusChips
import com.wordhold.app.core.ui.components.cellFill
import com.wordhold.app.core.ui.components.outlined
import com.wordhold.app.core.ui.components.pressable
import com.wordhold.app.core.ui.theme.Spacing
import com.wordhold.app.core.ui.theme.Stroke
import com.wordhold.app.core.ui.theme.WordholdTheme
import com.wordhold.app.core.ui.theme.fittedTextStyle

@Composable
fun CrosswordScreen(game: CrosswordGame) {
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type
    val puzzle = game.puzzle
    val current = game.currentWord
    val wrongCells = puzzle.words.indices.filter { game.isWrong(it) }.flatMap { puzzle.words[it].cells }.toSet()
    val solvedCells = game.solved.flatMap { puzzle.words[it].cells }.toSet()
    val numbers = puzzle.words.associate { it.cells.first() to it.number }

    Column(Modifier.fillMaxSize().padding(top = Spacing.section, bottom = Spacing.lg)) {
        StatusChips(
            left = "Solved ${game.solved.size} of ${puzzle.words.size}",
            right = "Score ${game.score}",
        )
        Spacer(Modifier.height(Spacing.section))

        // Grid. Only letter cells are drawn; the gaps between words are left as background.
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            val cellSize = minOf(Spacing.minTouch, maxWidth / puzzle.cols.coerceAtLeast(1))
            val letterStyle = fittedTextStyle(cellSize)
            val numberStyle = fittedTextStyle(cellSize, fraction = 0.22f)
            Column {
                for (row in 0 until puzzle.rows) {
                    Row {
                        for (col in 0 until puzzle.cols) {
                            val cell = row * puzzle.cols + col
                            if (puzzle.solution[cell] == null) {
                                Spacer(Modifier.size(cellSize))
                                continue
                            }
                            val isSelected = cell == game.selectedCell
                            val entry = game.entries[cell]
                            val status = when (cell) {
                                in solvedCells -> CellStatus.Solved
                                in wrongCells -> CellStatus.Wrong
                                else -> CellStatus.Neutral
                            }
                            val fill = colors.cellFill(
                                selected = isSelected,
                                related = current != null && cell in current.cells,
                                status = status,
                            )
                            Box(
                                Modifier
                                    .size(cellSize)
                                    .padding(Stroke.hairline)
                                    .outlined(
                                        fill = fill,
                                        outline = colors.ink,
                                        shape = WordholdTheme.shapes.cell,
                                        stroke = if (isSelected) Stroke.large else Stroke.key,
                                    )
                                    .pressable { game.tapCell(cell) }
                                    .semantics {
                                        selected = isSelected
                                        contentDescription = "Row ${row + 1}, column ${col + 1}, " + when {
                                            entry == null -> "empty"
                                            status == CellStatus.Solved -> "${entry.uppercase()}, correct"
                                            status == CellStatus.Wrong -> "${entry.uppercase()}, wrong"
                                            else -> entry.uppercase()
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                numbers[cell]?.let { number ->
                                    BasicText(
                                        text = number.toString(),
                                        style = numberStyle.copy(color = colors.on(fill)),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = Spacing.xs, top = Stroke.large),
                                    )
                                }
                                if (entry != null) {
                                    BasicText(entry.uppercase(), style = letterStyle.copy(color = colors.on(fill)))
                                }
                            }
                        }
                    }
                }
            }
        }

        // The clue for the selected word, and the way on to the next one.
        Row(
            Modifier.fillMaxWidth().padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedCard(
                modifier = Modifier.weight(1f),
                shape = WordholdTheme.shapes.m,
                padding = PaddingValues(horizontal = Spacing.section, vertical = Spacing.sm),
            ) {
                if (current != null) {
                    CapsLabel("${current.label}  ·  ${current.answer.length} letters")
                    BasicText(
                        text = current.clue,
                        style = type.body.copy(color = colors.ink),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            SecondaryButton("Next", onClick = { game.nextWord() })
        }
        Spacer(Modifier.height(Spacing.grid))

        LetterKeyboard(onLetter = game::type, onDelete = game::backspace)
    }
}

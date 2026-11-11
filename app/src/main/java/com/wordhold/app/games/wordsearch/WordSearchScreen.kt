package com.wordhold.app.games.wordsearch

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.wordhold.app.core.ui.components.BoardCard
import com.wordhold.app.core.ui.components.PillChip
import com.wordhold.app.core.ui.components.StatusChips
import com.wordhold.app.core.ui.theme.Spacing
import com.wordhold.app.core.ui.theme.WordholdTheme
import com.wordhold.app.core.ui.theme.fittedTextStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSearchScreen(game: WordSearchGame) {
    val colors = WordholdTheme.colors
    val n = game.gridSize

    // The drag in progress: where it began and the snapped cell it currently reaches.
    var selection by remember { mutableStateOf<Pair<Cell, Cell>?>(null) }

    val foundPlacements = game.placements.filter { it.word in game.found }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.section, bottom = Spacing.lg)
    ) {
        StatusChips(
            left = "Found ${game.found.size} of ${game.placements.size}",
            right = "Score ${game.score}",
        )
        Spacer(Modifier.height(Spacing.section))

        BoardCard(Modifier.fillMaxWidth(), padding = PaddingValues(Spacing.grid)) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(game, n) {
                        awaitEachGesture {
                            fun cellAt(position: Offset) = Cell(
                                row = (position.y / size.height * n).toInt().coerceIn(0, n - 1),
                                col = (position.x / size.width * n).toInt().coerceIn(0, n - 1),
                            )

                            val down = awaitFirstDown()
                            val start = cellAt(down.position)
                            selection = start to start
                            val lifted = drag(down.id) { change ->
                                selection = start to game.snap(start, cellAt(change.position))
                                change.consume()
                            }
                            val ended = selection
                            selection = null
                            if (lifted && ended != null) game.select(ended.first, ended.second)
                        }
                    }
                    // Found words (green) and the live drag (yellow) are drawn as marker
                    // strokes underneath the letters, which stay ink on both.
                    .drawBehind {
                        foundPlacements.forEach { markerStroke(it.cells.first(), it.cells.last(), n, colors.tileCorrect) }
                        selection?.let { (start, end) -> markerStroke(start, end, n, colors.tilePresent) }
                    }
                    .clearAndSetSemantics {
                        contentDescription = "Letter grid, $n by $n. Drag across letters to select a word."
                    }
            ) {
                val letterStyle = fittedTextStyle(maxWidth / n.coerceAtLeast(1)).copy(color = colors.ink)
                Column(Modifier.fillMaxSize()) {
                    game.grid.forEach { letters ->
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            letters.forEach { letter ->
                                Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                                    BasicText(letter.uppercase(), style = letterStyle)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.section))

        // The words to find. A found one turns green.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            game.placements.forEach { placement ->
                val isFound = placement.word in game.found
                PillChip(
                    text = placement.word.uppercase(),
                    fill = if (isFound) colors.tileCorrect else colors.white,
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = placement.word + if (isFound) ", found" else ", not found"
                    },
                )
            }
        }
    }
}

/** One round-capped stroke from the centre of [from] to the centre of [to]. */
private fun DrawScope.markerStroke(from: Cell, to: Cell, gridSize: Int, color: Color) {
    val cell = size.width / gridSize
    fun centre(c: Cell) = Offset((c.col + 0.5f) * cell, (c.row + 0.5f) * cell)
    val width = cell * 0.74f
    if (from == to) {
        drawCircle(color, radius = width / 2, center = centre(from))
    } else {
        drawLine(color, centre(from), centre(to), strokeWidth = width, cap = StrokeCap.Round)
    }
}

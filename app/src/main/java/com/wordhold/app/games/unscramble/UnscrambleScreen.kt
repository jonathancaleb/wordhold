package com.wordhold.app.games.unscramble

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.wordhold.app.core.ui.components.BoardCard
import com.wordhold.app.core.ui.components.LetterTile
import com.wordhold.app.core.ui.components.PrimaryButton
import com.wordhold.app.core.ui.components.SecondaryButton
import com.wordhold.app.core.ui.components.StatusChips
import com.wordhold.app.core.ui.components.pressable
import com.wordhold.app.core.ui.theme.Dimens
import com.wordhold.app.core.ui.theme.Spacing
import com.wordhold.app.core.ui.theme.WordholdTheme
import kotlinx.coroutines.launch

@Composable
fun UnscrambleScreen(game: UnscrambleGame) {
    val colors = WordholdTheme.colors
    val letters = game.letters

    // Indices into `letters`, in the order the player tapped them. Resets for each
    // new word and whenever the letters are reshuffled.
    var picked by remember(letters, game.wordNumber) { mutableStateOf(listOf<Int>()) }
    var rejected by remember(letters, game.wordNumber) { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(horizontal = Spacing.screen)) {
        StatusChips(
            left = "Word ${game.wordNumber} of ${game.wordCount}",
            right = "Score ${game.score}",
            modifier = Modifier.padding(top = Spacing.section),
        )

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(Spacing.section))

            // Answer slots, on the board: tap a placed letter to take it back. A word
            // that was checked and rejected turns dark, like any wrong answer.
            BoardCard(Modifier.fillMaxWidth().graphicsLayer { translationX = shake.value * density }) {
                TileRows(count = letters.length) { slot, size ->
                    val source = picked.getOrNull(slot)
                    if (source == null) {
                        LetterTile(letter = null, size = size)
                    } else {
                        val letter = letters[source]
                        LetterTile(
                            letter = letter,
                            size = size,
                            fill = if (rejected) colors.tileAbsent else colors.white,
                            modifier = Modifier
                                .pressable(pressedScale = 0.94f) {
                                    picked = picked - source
                                    rejected = false
                                }
                                .semantics {
                                    contentDescription = letter.uppercase() + if (rejected) ", wrong" else ", placed"
                                },
                        )
                    }
                }
            }

            BasicText(
                text = when {
                    rejected -> "Not a word. Try again."
                    game.previousWord != null -> "Last word: ${game.previousWord!!.uppercase()}"
                    else -> ""
                },
                style = WordholdTheme.type.body.copy(color = colors.ink, textAlign = TextAlign.Center),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.lg)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )

            // Scrambled letters: tap to place in the next free slot.
            TileRows(count = letters.length) { index, size ->
                if (index in picked) {
                    Spacer(Modifier.size(size)) // keeps the gap where the tile was
                } else {
                    LetterTile(
                        letter = letters[index],
                        size = size,
                        fill = colors.butter,
                        modifier = Modifier.pressable(pressedScale = 0.94f) {
                            picked = picked + index
                            rejected = false
                        },
                    )
                }
            }
            Spacer(Modifier.height(Spacing.section))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SecondaryButton("Shuffle", onClick = { game.shuffle() })
            if (game.skipsLeft > 0) {
                SecondaryButton("Skip (${game.skipsLeft} left)", onClick = { game.skip() })
            }
        }
        Spacer(Modifier.height(Spacing.grid))
        PrimaryButton(
            label = "Check",
            enabled = picked.size == letters.length,
            onClick = {
                val guess = picked.map { letters[it] }.joinToString("")
                if (!game.checkAnswer(guess)) {
                    rejected = true
                    scope.launch {
                        for (x in listOf(-12f, 12f, -8f, 8f, 0f)) shake.animateTo(x, tween(50))
                    }
                }
            },
        )
        Spacer(Modifier.height(Spacing.lg))
    }
}

/**
 * [count] square tiles, centred. Tiles are never smaller than [Spacing.minTouch]:
 * a word too long for one row at that size is split evenly over two or more.
 */
@Composable
private fun TileRows(
    count: Int,
    modifier: Modifier = Modifier,
    tile: @Composable (index: Int, size: Dp) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val gap = Spacing.tile
        val fitPerRow = ((maxWidth + gap) / (Spacing.minTouch + gap)).toInt().coerceAtLeast(1)
        val rows = (count + fitPerRow - 1) / fitPerRow
        val perRow = (count + rows - 1) / rows
        val size = minOf(Dimens.gridTile, (maxWidth - gap * (perRow - 1)) / perRow)
        Column(verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
            (0 until count).chunked(perRow).forEach { indices ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    indices.forEach { tile(it, size) }
                }
            }
        }
    }
}

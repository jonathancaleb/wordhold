package com.wordhold.app.core.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wordhold.app.core.ui.components.BoardCard
import com.wordhold.app.core.ui.components.CapsLabel
import com.wordhold.app.core.ui.components.Heading
import com.wordhold.app.core.ui.components.OutlinedCard
import com.wordhold.app.core.ui.components.PillChip
import com.wordhold.app.core.ui.components.PrimaryButton
import com.wordhold.app.core.ui.components.SecondaryButton
import com.wordhold.app.core.ui.theme.Spacing
import com.wordhold.app.core.ui.theme.WordholdTheme
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * The small rewards shown after each level: a cheer, a rank that climbs with the
 * levels, and a fact about words. All chosen by level, so they rotate predictably
 * instead of repeating at random.
 */
object Whimsy {
    private val cheers = listOf(
        "Nicely done",
        "Letter perfect",
        "Sharp as a pencil",
        "That clicked",
        "Spellbinding",
        "Top of the page",
        "Smooth solving",
        "Word perfect",
    )

    /** Rank names and the level at which each is reached. */
    private val ranks = listOf(
        1 to "Page Turner",
        3 to "Word Nibbler",
        5 to "Bookworm",
        8 to "Puzzle Pilot",
        12 to "Wordsmith",
        16 to "Lexicon Keeper",
        20 to "Grand Vocabularian",
    )

    private val facts = listOf(
        "A pangram uses every letter of the alphabet. \"The quick brown fox jumps over the lazy dog\" is the famous one.",
        "\"Bookkeeper\" has three pairs of double letters in a row.",
        "A palindrome reads the same in both directions, like \"level\", \"kayak\" and \"racecar\".",
        "\"Queue\" sounds exactly the same with its last four letters removed.",
        "The dot over a lowercase i or j is called a tittle.",
        "\"Rhythm\" has no a, e, i, o or u.",
        "\"Stressed\" spelled backwards is \"desserts\".",
        "\"Uncopyrightable\" has fifteen letters and does not repeat any of them.",
        "The word \"alphabet\" comes from alpha and beta, the first two Greek letters.",
        "E is the most frequently used letter in English.",
        "\"Typewriter\" can be typed using only the top row of letters on a keyboard.",
        "\"Listen\" and \"silent\" are anagrams: the same letters in a different order.",
        "The first crossword was published in a New York newspaper in 1913. It was called a \"word-cross\".",
        "\"Sudoku\" is short for a Japanese phrase meaning roughly \"the digits must remain single\".",
        "A piece of writing that deliberately avoids one letter is called a lipogram. A whole novel has been written without the letter e.",
        "The # symbol has a formal name: the octothorpe.",
        "Words that sound like what they describe, such as \"buzz\" and \"splash\", are called onomatopoeia.",
        "\"Strengths\" has nine letters and only one vowel.",
        "\"Facetious\" contains all five vowels in alphabetical order.",
        "\"Queueing\" has five vowels in a row.",
        "Q is the only letter that does not appear in the name of any US state.",
        "\"Almost\" is one of the longest common words with its letters in alphabetical order.",
        "The & sign is called an ampersand, and was once recited at the end of the alphabet.",
        "\"NOON\" reads the same forwards, backwards and upside down.",
    )

    fun cheer(level: Int): String = cheers[(level - 1).mod(cheers.size)]

    /** The rank held by a player who is on [level]. */
    fun rank(level: Int): String = ranks.last { level >= it.first }.second

    /** [gameIndex] offsets the rotation so each game shows different facts at the same level. */
    fun fact(level: Int, gameIndex: Int): String = facts[(level - 1 + gameIndex * 7).mod(facts.size)]
}

/** End-of-level screen: confetti, a cheer, the score, the player's rank, a word fact, and the way onward. */
@Composable
fun LevelComplete(
    level: Int,
    score: Int,
    gameIndex: Int,
    onNextLevel: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type
    // Finishing this level puts the player on the next one, which may carry a new rank.
    val rank = Whimsy.rank(level + 1)
    val promoted = rank != Whimsy.rank(level)

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.section),
            ) {
                Spacer(Modifier.height(Spacing.xs))
                BoardCard(Modifier.fillMaxWidth(), padding = PaddingValues(Spacing.lg)) {
                    Heading(Whimsy.cheer(level), style = type.title.copy(textAlign = TextAlign.Center))
                    BasicText(
                        text = "Level $level complete",
                        style = type.body.copy(color = colors.ink),
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                    Spacer(Modifier.height(Spacing.screen))
                    BasicText(score.toString(), style = type.display.copy(color = colors.ink))
                    BasicText("points", style = type.chip.copy(color = colors.ink))
                    Spacer(Modifier.height(Spacing.screen))
                    PillChip(
                        text = (if (promoted) "New rank: " else "Rank: ") + rank,
                        fill = if (promoted) colors.tileCorrect else colors.butter,
                    )
                }
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = WordholdTheme.shapes.xl,
                    padding = PaddingValues(Spacing.screen + Spacing.xs / 2),
                ) {
                    CapsLabel("Did you know?")
                    BasicText(
                        text = Whimsy.fact(level, gameIndex),
                        style = type.body.copy(color = colors.ink),
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.section))
            PrimaryButton("Next level", onClick = onNextLevel)
            Spacer(Modifier.height(Spacing.sm))
            SecondaryButton("Back to games", onClick = onBack, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.screen))
        }
        Confetti(
            seed = level,
            colors = listOf(colors.ink, colors.periwinkle, colors.butter, colors.tileCorrect, colors.white),
        )
    }
}

private class Flake(
    val x: Float,
    val delay: Float,
    val drift: Float,
    val wobble: Float,
    val spin: Float,
    val size: Float,
    val color: Color,
)

/**
 * One shower of paper flakes falling the height of the screen, then gone. Purely
 * decorative: it ignores touches and is hidden from screen readers. With system
 * animations switched off it finishes instantly and draws nothing.
 */
@Composable
private fun Confetti(seed: Int, colors: List<Color>, modifier: Modifier = Modifier) {
    val flakes = remember(seed) {
        val random = Random(seed)
        List(70) {
            Flake(
                x = random.nextFloat(),
                delay = random.nextFloat() * 0.35f,
                drift = (random.nextFloat() - 0.5f) * 0.25f,
                wobble = 1f + random.nextFloat() * 3f,
                spin = (random.nextFloat() - 0.5f) * 1440f,
                size = 6f + random.nextFloat() * 6f,
                color = colors[random.nextInt(colors.size)],
            )
        }
    }
    val progress = remember(seed) { Animatable(0f) }
    LaunchedEffect(seed) { progress.animateTo(1f, tween(durationMillis = 2600, easing = LinearEasing)) }

    Canvas(modifier.fillMaxSize().clearAndSetSemantics { }) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        for (flake in flakes) {
            // Each flake starts after its own delay and takes the rest of the time to fall.
            val fall = ((t - flake.delay) / (1f - flake.delay)).coerceIn(0f, 1f)
            if (fall <= 0f) continue
            val side = flake.size.dp.toPx()
            val x = (flake.x + flake.drift * fall + 0.02f * sin(fall * flake.wobble * 2 * PI).toFloat()) * size.width
            val y = -side + fall * fall * (size.height + 2 * side)
            rotate(degrees = flake.spin * fall, pivot = Offset(x, y)) {
                drawRect(
                    color = flake.color,
                    topLeft = Offset(x - side / 2, y - side / 4),
                    size = Size(side, side / 2),
                    alpha = (1f - fall).coerceIn(0f, 1f) * 0.9f + 0.1f,
                )
            }
        }
    }
}

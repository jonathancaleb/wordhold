package com.wordhold.app.core.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wordhold.app.core.ui.components.Heading
import com.wordhold.app.core.ui.components.IconButton
import com.wordhold.app.core.ui.components.LetterTile
import com.wordhold.app.core.ui.components.ModeCard
import com.wordhold.app.core.ui.components.NumberedStep
import com.wordhold.app.core.ui.components.OutlinedCard
import com.wordhold.app.core.ui.components.PrimaryButton
import com.wordhold.app.core.ui.components.TILTS
import com.wordhold.app.core.ui.components.WordholdIcons
import com.wordhold.app.core.ui.theme.Dimens
import com.wordhold.app.core.ui.theme.Spacing
import com.wordhold.app.core.ui.theme.WordholdColors
import com.wordhold.app.core.ui.theme.WordholdTheme
import com.wordhold.app.wordholdApp

private fun WordholdColors.stepFill(look: StepTile): Color = when (look) {
    StepTile.Plain -> white
    StepTile.Correct -> tileCorrect
    StepTile.Present -> tilePresent
    StepTile.Absent -> tileAbsent
}

/** The looks the intro's five example tiles take, in order. */
private val EXAMPLE_LOOKS =
    listOf(StepTile.Correct, StepTile.Present, StepTile.Absent, StepTile.Correct, StepTile.Absent)

/**
 * What a game is and how it works, before playing it: a title card, three steps,
 * where the player stands, and the Play button pinned to the bottom.
 */
@Composable
fun GameIntroScreen(
    entry: GameEntry<*>,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.wordholdApp
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type
    val statsByGame by remember { app.sessions.statsByGame }.collectAsStateWithLifecycle(emptyMap())
    val savedIds by app.savedGames.savedIds.collectAsStateWithLifecycle()
    val stats = statsByGame[entry.id]
    val canResume = entry.id in savedIds

    Column(modifier.fillMaxSize().background(colors.sage)) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = Spacing.grid)
                .defaultMinSize(minHeight = Dimens.topBar),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(WordholdIcons.Back, "Back to games", onBack)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                fill = colors.periwinkle,
                shape = WordholdTheme.shapes.xl,
                padding = PaddingValues(Spacing.lg),
            ) {
                Row(
                    Modifier.clearAndSetSemantics { },
                    horizontalArrangement = Arrangement.spacedBy(Spacing.tile),
                ) {
                    entry.example.forEachIndexed { index, char ->
                        LetterTile(
                            letter = char,
                            size = Dimens.introTile,
                            fill = colors.stepFill(EXAMPLE_LOOKS[index % EXAMPLE_LOOKS.size]),
                            tilt = TILTS[index % TILTS.size],
                        )
                    }
                }
                Heading(entry.title, style = type.heroTitle, modifier = Modifier.padding(top = Spacing.screen + Spacing.xs / 2))
                BasicText(
                    text = entry.description,
                    style = type.lead.copy(color = colors.ink),
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = WordholdTheme.shapes.xl,
                padding = PaddingValues(
                    start = Spacing.screen + Spacing.xs / 2,
                    end = Spacing.screen + Spacing.xs / 2,
                    top = Spacing.screen + Spacing.xs / 2,
                    bottom = Spacing.section,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.grid),
            ) {
                Heading("How it works")
                entry.steps.forEachIndexed { index, step ->
                    NumberedStep(index + 1, step.tile, colors.stepFill(step.look), step.text)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.grid)) {
                ModeCard(
                    title = "Level ${stats?.level ?: 1}",
                    caption = if (canResume) "In progress" else "Up next",
                    fill = colors.butter,
                    modifier = Modifier.weight(1f),
                )
                ModeCard(
                    title = "Best score",
                    caption = stats?.bestScore?.toString() ?: "Not played yet",
                    fill = colors.white,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.section, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PrimaryButton(if (canResume) "Resume" else "Play", onClick = onPlay)
            BasicText(
                text = "Progress stays on this phone.",
                style = type.caption.copy(color = colors.ink, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

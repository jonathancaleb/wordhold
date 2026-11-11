package com.wordhold.app.catalogue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wordhold.app.core.data.SavedGameStore
import com.wordhold.app.core.data.SessionRepository
import com.wordhold.app.core.game.GameEntry
import com.wordhold.app.core.game.GameRegistry
import com.wordhold.app.core.ui.components.CapsLabel
import com.wordhold.app.core.ui.components.Heading
import com.wordhold.app.core.ui.components.OutlinedCard
import com.wordhold.app.core.ui.components.PillButton
import com.wordhold.app.core.ui.components.StreakPill
import com.wordhold.app.core.ui.components.outlined
import com.wordhold.app.core.ui.components.pressable
import com.wordhold.app.core.ui.components.scribble
import com.wordhold.app.core.ui.theme.Dimens
import com.wordhold.app.core.ui.theme.GameTone
import com.wordhold.app.core.ui.theme.Spacing
import com.wordhold.app.core.ui.theme.WordholdTheme
import com.wordhold.app.core.ui.theme.tone
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.wordhold.app.wordholdApp

data class CatalogueItem(
    val id: String,
    val title: String,
    val tagline: String,
    val tone: GameTone,
    val motif: @Composable () -> Unit,
    /** True when an unfinished game is saved and the button should say "Resume". */
    val canResume: Boolean,
)

data class CatalogueUiState(
    val items: List<CatalogueItem>,
    /** The game featured in the hero card today, or null if there are no games. */
    val daily: CatalogueItem?,
    val streakDays: Int,
)

class CatalogueViewModel(
    private val games: List<GameEntry<*>>,
    private val dailyId: String,
    sessions: SessionRepository,
    savedGames: SavedGameStore,
) : ViewModel() {
    private fun state(streakDays: Int, saved: Set<String>): CatalogueUiState {
        val items = games.map { CatalogueItem(it.id, it.title, it.tagline, it.tone, it.motif, canResume = it.id in saved) }
        return CatalogueUiState(items, items.firstOrNull { it.id == dailyId }, streakDays)
    }

    val state: StateFlow<CatalogueUiState> =
        combine(sessions.overallStats, savedGames.savedIds) { overall, saved -> state(overall.streakDays, saved) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                state(streakDays = 0, savedGames.savedIds.value),
            )
}

/**
 * The home of the app: wordmark and streak, today's puzzle, and a card for every game.
 * It only knows [CatalogueItem]s, never a concrete game.
 *
 * @param onOpen a game card was tapped: show that game's intro.
 * @param onPlay the daily puzzle was tapped: go straight into the game.
 */
@Composable
fun CatalogueScreen(
    onOpen: (gameId: String) -> Unit,
    onPlay: (gameId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.wordholdApp
    val viewModel: CatalogueViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CatalogueViewModel(GameRegistry.games, GameRegistry.daily().id, app.sessions, app.savedGames)
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    CatalogueContent(state, onOpen, onPlay, modifier)
}

@Composable
fun CatalogueContent(
    state: CatalogueUiState,
    onOpen: (gameId: String) -> Unit,
    onPlay: (gameId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.section),
    ) {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = Dimens.streakPill + Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Heading("Wordhold", style = WordholdTheme.type.title, modifier = Modifier.weight(1f))
            StreakPill(state.streakDays)
        }

        state.daily?.let { daily -> DailyHero(daily, onPlay = { onPlay(daily.id) }) }

        Heading("All games")

        state.items.chunked(2).forEach { pair ->
            // The two cards of a row are as tall as the taller of them.
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(Spacing.grid),
            ) {
                pair.forEach { item ->
                    GameCard(item, onClick = { onOpen(item.id) }, modifier = Modifier.weight(1f).fillMaxHeight())
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(Spacing.none))
    }
}

/** Today's puzzle. The whole card is the button; the Play pill is its label. */
@Composable
private fun DailyHero(item: CatalogueItem, onPlay: () -> Unit) {
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.98f, onClick = onPlay)
            .outlined(colors.periwinkle, colors.ink, WordholdTheme.shapes.xl)
            .padding(
                start = Spacing.screen + Spacing.tile,
                end = Spacing.screen + Spacing.xs / 2,
                top = Spacing.screen + Spacing.xs / 2,
                bottom = Spacing.screen + Spacing.xs / 2,
            )
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.grid),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.tile)) {
            CapsLabel("Daily puzzle")
            BasicText(
                text = item.title,
                style = type.title.copy(color = colors.ink),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = type.cardTitle.fontSize, maxFontSize = type.title.fontSize),
                modifier = Modifier.padding(vertical = Spacing.xs).scribble(colors.butter),
            )
            BasicText(item.tagline, style = type.caption.copy(color = colors.ink))
        }
        PillButton(if (item.canResume) "Resume" else "Play")
    }
}

@Composable
private fun GameCard(item: CatalogueItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type
    OutlinedCard(
        modifier = modifier
            .pressable(pressedScale = 0.97f, onClick = onClick)
            .defaultMinSize(minHeight = Dimens.gameCard)
            .semantics(mergeDescendants = true) { },
        fill = colors.tone(item.tone),
        padding = PaddingValues(Spacing.section),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.clearAndSetSemantics { }) { item.motif() }
        Column(Modifier.padding(top = Spacing.grid)) {
            // A long name shrinks to stay on one line of a narrow card.
            BasicText(
                text = item.title,
                style = type.cardTitle.copy(color = colors.ink),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = type.label.fontSize, maxFontSize = type.cardTitle.fontSize),
            )
            BasicText(item.tagline, style = type.caption.copy(color = colors.ink))
        }
    }
}

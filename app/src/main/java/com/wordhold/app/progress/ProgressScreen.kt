package com.wordhold.app.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wordhold.app.core.data.SessionRepository
import com.wordhold.app.core.data.SessionStats
import com.wordhold.app.core.data.bestStreakDays
import com.wordhold.app.core.data.currentEpochDay
import com.wordhold.app.core.data.recentWeeks
import com.wordhold.app.core.data.streakDays
import com.wordhold.app.core.game.GameEntry
import com.wordhold.app.core.game.GameRegistry
import com.wordhold.app.core.ui.components.Heading
import com.wordhold.app.core.ui.components.IconButton
import com.wordhold.app.core.ui.components.ListRow
import com.wordhold.app.core.ui.components.OutlinedCard
import com.wordhold.app.core.ui.components.StatCard
import com.wordhold.app.core.ui.components.WordholdIcons
import com.wordhold.app.core.ui.components.outlined
import com.wordhold.app.core.ui.theme.Dimens
import com.wordhold.app.core.ui.theme.GameTone
import com.wordhold.app.core.ui.theme.Spacing
import com.wordhold.app.core.ui.theme.Stroke
import com.wordhold.app.core.ui.theme.WordholdTheme
import com.wordhold.app.core.ui.theme.badge
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.wordhold.app.wordholdApp

/** One row of "By game". */
data class GameProgress(val title: String, val tone: GameTone, val level: Int)

data class ProgressUiState(
    val streakDays: Int,
    val bestStreakDays: Int,
    /** The last five weeks, Monday first: whether a level was completed on each day. */
    val played: List<Boolean>,
    /** In catalogue order. */
    val games: List<GameProgress>,
)

class ProgressViewModel(
    private val games: List<GameEntry<*>>,
    sessions: SessionRepository,
) : ViewModel() {
    private fun state(completedDays: Set<Long>, byGame: Map<String, SessionStats>): ProgressUiState {
        val today = currentEpochDay()
        return ProgressUiState(
            streakDays = streakDays(completedDays, today),
            bestStreakDays = bestStreakDays(completedDays),
            played = recentWeeks(today).map { it in completedDays },
            games = games.map { GameProgress(it.title, it.tone, byGame[it.id]?.level ?: 1) },
        )
    }

    val state: StateFlow<ProgressUiState> =
        combine(sessions.completedDays, sessions.statsByGame) { days, byGame -> state(days, byGame) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), state(emptySet(), emptyMap()))
}

private val WEEKDAYS = listOf("M", "T", "W", "T", "F", "S", "S")

/** Streaks, the days played, and the level reached in each game, read from the shared `game_sessions` table. */
@Composable
fun ProgressScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val app = LocalContext.current.wordholdApp
    val viewModel: ProgressViewModel = viewModel(
        factory = viewModelFactory { initializer { ProgressViewModel(GameRegistry.games, app.sessions) } }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type

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
            Heading("Progress", style = type.title, modifier = Modifier.weight(1f))
            IconButton(WordholdIcons.Settings, "Settings", onOpenSettings)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.grid)) {
            StatCard(state.streakDays.toString(), "Day streak", colors.periwinkle, Modifier.weight(1f))
            StatCard(state.bestStreakDays.toString(), "Best streak", colors.butter, Modifier.weight(1f))
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs / 2),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Heading("Last 5 weeks", style = type.sectionTitle, modifier = Modifier.weight(1f))
                BasicText("Days played", style = type.caption.copy(color = colors.ink))
            }
            val playedCount = state.played.count { it }
            Column(
                Modifier.clearAndSetSemantics {
                    contentDescription = "$playedCount of the last ${state.played.size} days played"
                },
                verticalArrangement = Arrangement.spacedBy(Spacing.tile),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tile)) {
                    WEEKDAYS.forEach { day ->
                        BasicText(
                            text = day,
                            style = type.navLabelActive.copy(color = colors.ink, textAlign = TextAlign.Center),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                state.played.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tile)) {
                        week.forEach { played ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(Dimens.dayCell)
                                    .outlined(
                                        fill = if (played) colors.tileCorrect else colors.white,
                                        outline = if (played) colors.ink else colors.mutedOutline,
                                        shape = WordholdTheme.shapes.day,
                                        stroke = Stroke.key,
                                    )
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Heading("By game", style = type.sectionTitle)
            state.games.forEach { game ->
                ListRow(
                    label = game.title,
                    badge = game.title.first(),
                    badgeFill = colors.badge(game.tone),
                    value = "Level ${game.level}",
                )
            }
        }
        Spacer(Modifier.height(Spacing.none))
    }
}

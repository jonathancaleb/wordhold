package com.wordhold.app.catalogue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wordhold.app.core.data.SessionRepository
import com.wordhold.app.core.data.SessionStats
import com.wordhold.app.core.game.GameEntry
import com.wordhold.app.core.game.GameRegistry
import com.wordhold.app.core.ui.components.PillButton
import com.wordhold.app.core.ui.components.WordholdCard
import com.wordhold.app.core.ui.theme.WordholdTheme
import com.wordhold.app.wordholdApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CatalogueItem(
    val id: String,
    val title: String,
    val description: String,
    /** Null until the game has been played at least once. */
    val stats: SessionStats?,
)

class CatalogueViewModel(
    private val games: List<GameEntry<*>>,
    sessions: SessionRepository,
) : ViewModel() {
    private fun items(stats: Map<String, SessionStats>) =
        games.map { CatalogueItem(it.id, it.title, it.description, stats[it.id]) }

    val items: StateFlow<List<CatalogueItem>> = sessions.statsByGame
        .map(::items)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), items(emptyMap()))
}

/** The list of games. It only knows [CatalogueItem]s, never a concrete game. */
@Composable
fun CatalogueScreen(
    onPlay: (gameId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.wordholdApp
    val viewModel: CatalogueViewModel = viewModel(
        factory = viewModelFactory { initializer { CatalogueViewModel(GameRegistry.games, app.sessions) } }
    )
    val items by viewModel.items.collectAsStateWithLifecycle()
    CatalogueList(items, onPlay, modifier)
}

@Composable
fun CatalogueList(
    items: List<CatalogueItem>,
    onPlay: (gameId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            BasicText(
                text = "No games yet",
                style = WordholdTheme.type.body.copy(
                    color = WordholdTheme.colors.secondaryLabel,
                    textAlign = TextAlign.Center,
                ),
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.id }) { item ->
            GameCard(item, onPlay = { onPlay(item.id) })
        }
    }
}

@Composable
private fun GameCard(item: CatalogueItem, onPlay: () -> Unit) {
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type
    WordholdCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Letter tile: the game's initial, marked in highlighter.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.highlight, RoundedCornerShape(12.dp))
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = item.title.take(1).uppercase(),
                    style = type.title.copy(color = colors.onHighlight, fontWeight = FontWeight.Black),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                BasicText(item.title, style = type.headline.copy(color = colors.label))
                BasicText(item.description, style = type.subheadline.copy(color = colors.secondaryLabel))
                item.stats?.let { stats ->
                    BasicText(
                        text = statsLine(stats),
                        style = type.footnote.copy(color = colors.tint, fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            PillButton(label = "Play", onClick = onPlay)
        }
    }
}

private fun statsLine(stats: SessionStats): String = buildString {
    append("Last score ${stats.lastScore}")
    if (stats.streakDays > 0) append(" · ${stats.streakDays}-day streak")
}

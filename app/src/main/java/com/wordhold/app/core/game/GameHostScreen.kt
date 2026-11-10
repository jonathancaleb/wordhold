package com.wordhold.app.core.game

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wordhold.app.core.data.SessionRepository
import com.wordhold.app.core.ui.components.NavigationBar
import com.wordhold.app.wordholdApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Owns one play of a game: creates and starts it, times it, and writes exactly one
 * `game_sessions` row, either when the game completes or when the player leaves
 * part-way with a score. Games never touch the database themselves.
 */
class GameHostViewModel(
    entry: GameEntry<*>,
    private val sessions: SessionRepository,
) : ViewModel() {
    val game: WordGame = entry.newGame().also { it.start() }

    private val startedAt = SystemClock.elapsedRealtime()
    private var recorded = false

    init {
        viewModelScope.launch {
            snapshotFlow { game.isComplete }.first { it }
            record()
        }
    }

    override fun onCleared() {
        if (game.isComplete || game.score > 0) record()
    }

    private fun record() {
        if (recorded) return
        recorded = true
        sessions.record(
            gameId = game.id,
            score = game.score,
            durationSeconds = ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt(),
            completed = game.isComplete,
        )
    }
}

/** Shared chrome around every game: navigation bar on top, the game's own UI below. */
@Composable
fun GameHostScreen(
    entry: GameEntry<*>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.wordholdApp
    val viewModel: GameHostViewModel = viewModel(
        key = entry.id,
        factory = viewModelFactory { initializer { GameHostViewModel(entry, app.sessions) } },
    )
    Column(modifier.fillMaxSize()) {
        NavigationBar(title = entry.title, backLabel = "Games", onBack = onBack)
        Box(Modifier.weight(1f).windowInsetsPadding(WindowInsets.navigationBars)) {
            entry.Content(viewModel.game)
        }
    }
}

package com.wordhold.app.core.game

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wordhold.app.core.data.SavedGameStore
import com.wordhold.app.core.data.SessionRepository
import com.wordhold.app.core.ui.components.IconButton
import com.wordhold.app.core.ui.components.TopBar
import com.wordhold.app.core.ui.components.WordholdIcons
import com.wordhold.app.core.ui.theme.LocalScreenBackground
import com.wordhold.app.core.ui.theme.WordholdTheme
import com.wordhold.app.core.ui.theme.tone
import com.wordhold.app.core.words.GameAssets
import com.wordhold.app.wordholdApp
import kotlinx.coroutines.launch

/**
 * Optional extra for games that get harder as the player advances. The host sets
 * [level] (1 is the easiest) before every [WordGame.start]; the game decides what a
 * level means. Difficulty should level off rather than grow without limit.
 */
interface LeveledGame {
    var level: Int
}

/** Optional extra for games that can give a hint. The host puts a hint button in the top bar. */
interface HintGame {
    /** Gives a hint. Returns false if there was nothing to reveal. */
    fun hint(): Boolean
}

/**
 * Optional extra for games long enough to be worth resuming. The host saves the
 * state when the player leaves an unfinished game and offers it back next time.
 */
interface ResumableGame {
    /** The game in progress as a string, or null if there is nothing worth keeping. */
    fun saveState(): String?

    /** Restores a string from [saveState]. Returns false, changing nothing, if it is unusable. */
    fun restoreState(state: String): Boolean
}

/**
 * Owns the plays of one game while its screen is open: creates it, resumes it or
 * starts it at the player's level, times each level, and writes one `game_sessions`
 * row per level played. Games never touch storage themselves.
 *
 * The player's level in a game is the number of levels they have completed in it,
 * plus one, so it comes straight from the sessions table with nothing extra stored.
 *
 * Leaving part-way saves a [ResumableGame] for later; any other game is recorded as
 * an unfinished session if it has a score.
 */
class GameHostViewModel(
    entry: GameEntry<*>,
    assets: GameAssets,
    private val sessions: SessionRepository,
    private val savedGames: SavedGameStore,
) : ViewModel() {
    val game: WordGame = entry.newGame(assets)

    /** False for the moment it takes to look up the player's level. */
    var ready by mutableStateOf(false)
        private set

    /** The level being played, or just finished. */
    var level by mutableIntStateOf(1)
        private set

    /** Seconds played before this screen opened, when resuming. */
    private var priorSeconds = 0
    private var startedAt = SystemClock.elapsedRealtime()
    private var recorded = false

    init {
        viewModelScope.launch {
            val saved = savedGames.load(game.id)
            if (saved != null && game is ResumableGame && game.restoreState(saved.state)) {
                priorSeconds = saved.elapsedSeconds
                startedAt = SystemClock.elapsedRealtime()
                level = (game as? LeveledGame)?.level ?: 1
            } else {
                savedGames.clear(game.id)
                level = sessions.completedCount(game.id) + 1
                startLevel()
            }
            ready = true
            snapshotFlow { game.isComplete }.collect { complete -> if (complete) record() }
        }
    }

    /** Moves on after a completed level. */
    fun nextLevel() {
        if (!game.isComplete) return
        level++
        startLevel()
    }

    /** Saves an unfinished resumable game. Called when the app goes to the background. */
    fun saveProgress() {
        if (!ready || game !is ResumableGame || game.isComplete) return
        val state = game.saveState()
        if (state != null) savedGames.save(game.id, state, elapsedSeconds()) else savedGames.clear(game.id)
    }

    override fun onCleared() {
        when {
            !ready -> Unit
            game.isComplete -> record()
            game is ResumableGame -> saveProgress()
            game.score > 0 -> record()
        }
    }

    private fun startLevel() {
        (game as? LeveledGame)?.level = level
        game.start()
        priorSeconds = 0
        startedAt = SystemClock.elapsedRealtime()
        recorded = false
    }

    private fun elapsedSeconds(): Int =
        priorSeconds + ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()

    private fun record() {
        if (recorded) return
        recorded = true
        savedGames.clear(game.id)
        sessions.record(
            gameId = game.id,
            score = game.score,
            durationSeconds = elapsedSeconds(),
            completed = game.isComplete,
        )
    }
}

/**
 * Shared chrome around every game: the top bar with the level, the game's own UI
 * below on the game's colour, and the same end-of-level screen once the game reports
 * it is complete.
 */
@Composable
fun GameHostScreen(
    entry: GameEntry<*>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Where "Back to games" leads after a level. */
    onHome: () -> Unit = onBack,
) {
    val app = LocalContext.current.wordholdApp
    val viewModel: GameHostViewModel = viewModel(
        key = entry.id,
        factory = viewModelFactory {
            initializer { GameHostViewModel(entry, app.gameAssets, app.sessions, app.savedGames) }
        },
    )
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.saveProgress() }

    val game = viewModel.game
    val background = WordholdTheme.colors.tone(entry.tone)
    val playing = viewModel.ready && !game.isComplete
    CompositionLocalProvider(LocalScreenBackground provides background) {
        Column(modifier.fillMaxSize().background(background)) {
            TopBar(
                title = entry.title,
                onBack = onBack,
                caption = if (playing) "Level ${viewModel.level}" else null,
                trailing = if (playing && game is HintGame) {
                    { IconButton(WordholdIcons.Hint, "Hint", onClick = { game.hint() }) }
                } else {
                    null
                },
            )
            Box(Modifier.weight(1f).windowInsetsPadding(WindowInsets.navigationBars)) {
                when {
                    !viewModel.ready -> Unit
                    game.isComplete -> LevelComplete(
                        level = viewModel.level,
                        score = game.score,
                        gameIndex = GameRegistry.games.indexOf(entry),
                        onNextLevel = viewModel::nextLevel,
                        onBack = onHome,
                    )
                    else -> entry.Content(game)
                }
            }
        }
    }
}

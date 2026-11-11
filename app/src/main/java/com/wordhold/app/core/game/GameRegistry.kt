package com.wordhold.app.core.game

import androidx.compose.runtime.Composable
import com.wordhold.app.core.data.currentEpochDay
import com.wordhold.app.core.ui.components.CrosswordMotif
import com.wordhold.app.core.ui.components.SudokuMotif
import com.wordhold.app.core.ui.components.UnscrambleMotif
import com.wordhold.app.core.ui.components.WordSearchMotif
import com.wordhold.app.core.ui.theme.GameTone
import com.wordhold.app.core.words.GameAssets
import com.wordhold.app.games.crossword.CrosswordGame
import com.wordhold.app.games.crossword.CrosswordScreen
import com.wordhold.app.games.sudoku.SudokuGame
import com.wordhold.app.games.sudoku.SudokuScreen
import com.wordhold.app.games.unscramble.UnscrambleGame
import com.wordhold.app.games.unscramble.UnscrambleScreen
import com.wordhold.app.games.wordsearch.WordSearchGame
import com.wordhold.app.games.wordsearch.WordSearchScreen

/** The look of the example tile in a "How it works" step. */
enum class StepTile { Plain, Correct, Present, Absent }

/** One line of a game's "How it works": an example tile and what it means. */
class HowStep(val tile: Char, val look: StepTile, val text: String)

/**
 * One catalogue entry: the metadata the catalogue and intro show, a factory for the
 * game, and the composable that plays it.
 *
 * [id] and [title] must match the [WordGame] that [create] returns. They are repeated
 * here so the catalogue can list games without instantiating them.
 */
class GameEntry<G : WordGame>(
    val id: String,
    val title: String,
    /** A few words under the title on the game's card. */
    val tagline: String,
    /** One sentence on the game's intro. */
    val description: String,
    /** The colour of the game's card and of its screens. */
    val tone: GameTone,
    /** Five characters shown as tilted tiles on the intro. */
    val example: String,
    /** The three steps of "How it works". */
    val steps: List<HowStep>,
    /** The small picture on the game's card. */
    val motif: @Composable () -> Unit,
    private val create: (GameAssets) -> G,
    private val content: @Composable (game: G) -> Unit,
) {
    fun newGame(assets: GameAssets): G = create(assets).also {
        check(it.id == id) { "GameEntry id '$id' does not match WordGame id '${it.id}'" }
    }

    /** Renders [game], which must have come from this entry's [newGame]. */
    @Composable
    fun Content(game: WordGame) {
        @Suppress("UNCHECKED_CAST")
        content(game as G)
    }
}

/**
 * The static list of games. Adding a game means adding its class and one entry here;
 * the catalogue, progress and host screens pick it up from this list.
 */
object GameRegistry {
    val games: List<GameEntry<*>> = listOf(
        GameEntry(
            id = UnscrambleGame.ID,
            title = "Unscramble",
            tagline = "Unjumble the letters",
            description = "Rearrange the jumbled letters into a word.",
            tone = GameTone.White,
            example = "MIXED",
            steps = listOf(
                HowStep('T', StepTile.Present, "Tap letters to build the word"),
                HowStep('E', StepTile.Plain, "Tap a placed letter to take it back"),
                HowStep('L', StepTile.Correct, "Check it, or shuffle and skip"),
            ),
            motif = { UnscrambleMotif() },
            create = { UnscrambleGame(it.words) },
            content = { game -> UnscrambleScreen(game) },
        ),
        GameEntry(
            id = WordSearchGame.ID,
            title = "Word Search",
            tagline = "Find the hidden words",
            description = "Find every hidden word in the letter grid.",
            tone = GameTone.Periwinkle,
            example = "FOUND",
            steps = listOf(
                HowStep('W', StepTile.Plain, "Words run across, down and diagonally"),
                HowStep('O', StepTile.Present, "Drag from the first letter to the last"),
                HowStep('D', StepTile.Correct, "Found words turn green"),
            ),
            motif = { WordSearchMotif() },
            create = { WordSearchGame(it.words) },
            content = { game -> WordSearchScreen(game) },
        ),
        GameEntry(
            id = CrosswordGame.ID,
            title = "Crossword",
            tagline = "Small grid, quick clues",
            description = "Solve the clues to fill the grid.",
            tone = GameTone.Butter,
            example = "CLUES",
            steps = listOf(
                HowStep('C', StepTile.Present, "Tap a square to read its clue"),
                HowStep('A', StepTile.Correct, "A right answer turns green"),
                HowStep('T', StepTile.Absent, "A wrong one turns dark"),
            ),
            motif = { CrosswordMotif() },
            create = { CrosswordGame(it.crosswordClues) },
            content = { game -> CrosswordScreen(game) },
        ),
        GameEntry(
            id = SudokuGame.ID,
            title = "Sudoku",
            tagline = "Place the digits 1 to 9",
            description = "Fill every row, column and box with the digits 1 to 9.",
            tone = GameTone.White,
            example = "49327",
            steps = listOf(
                HowStep('5', StepTile.Plain, "No digit repeats in a row, column or box"),
                HowStep('3', StepTile.Present, "Tap a square, then tap a digit"),
                HowStep('7', StepTile.Absent, "A digit that does not fit turns dark"),
            ),
            motif = { SudokuMotif() },
            create = { SudokuGame() },
            content = { game -> SudokuScreen(game) },
        ),
    )

    fun find(id: String): GameEntry<*>? = games.firstOrNull { it.id == id }

    /** The game featured as the daily puzzle. It moves on to the next game each day. */
    fun daily(today: Long = currentEpochDay()): GameEntry<*> = games[Math.floorMod(today, games.size.toLong()).toInt()]
}

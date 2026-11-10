package com.wordhold.app.core.game

import androidx.compose.runtime.Composable

/**
 * One catalogue entry: the metadata the catalogue shows, a factory for the game,
 * and the composable that plays it.
 *
 * [id] and [title] must match the [WordGame] that [create] returns. They are repeated
 * here so the catalogue can list games without instantiating them.
 */
class GameEntry<G : WordGame>(
    val id: String,
    val title: String,
    val description: String,
    private val create: () -> G,
    private val content: @Composable (game: G) -> Unit,
) {
    fun newGame(): G = create().also {
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
        // GameEntry(
        //     id = "unscramble",
        //     title = "Unscramble",
        //     description = "Rearrange the letters into a word.",
        //     create = { UnscrambleGame(...) },
        //     content = { game -> UnscrambleScreen(game) },
        // ),
    )

    fun find(id: String): GameEntry<*>? = games.firstOrNull { it.id == id }
}

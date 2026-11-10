package com.wordhold.app.core.game

/**
 * The contract every game in the catalogue implements.
 *
 * Implementations should back [score] and [isComplete] with Compose snapshot state
 * (`mutableIntStateOf` / `mutableStateOf`) so the game screen and the host, which
 * records the session once [isComplete] turns true, both observe changes.
 */
interface WordGame {
    /** Stable identifier. Stored in `game_sessions.gameId`, so never change it after release. */
    val id: String
    val title: String

    /** Begins a fresh round, resetting [score] and [isComplete]. */
    fun start()

    /** Returns true if [input] is accepted as a correct answer in the current state. */
    fun checkAnswer(input: String): Boolean

    val score: Int
    val isComplete: Boolean
}

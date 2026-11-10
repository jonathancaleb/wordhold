package com.wordhold.app.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SessionRepository(
    private val dao: GameSessionDao,
    /** Outlives screens, so a session still saves when the player leaves mid-write. */
    private val appScope: CoroutineScope,
) {
    /** Stats across every game. */
    val overallStats: Flow<SessionStats> =
        dao.observeAll().map { SessionStats.from(it, currentEpochDay()) }

    /** Stats keyed by game id. Games never played have no entry. */
    val statsByGame: Flow<Map<String, SessionStats>> =
        dao.observeAll().map { sessions ->
            val today = currentEpochDay()
            sessions.groupBy { it.gameId }.mapValues { SessionStats.from(it.value, today) }
        }

    fun record(gameId: String, score: Int, durationSeconds: Int, completed: Boolean) {
        appScope.launch {
            dao.insert(
                GameSession(
                    gameId = gameId,
                    date = currentEpochDay(),
                    score = score,
                    durationSeconds = durationSeconds,
                    completed = completed,
                )
            )
        }
    }
}

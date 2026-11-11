package com.wordhold.app.core.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SavedGame(val state: String, val elapsedSeconds: Int)

/**
 * Unfinished games that can be resumed, at most one per game id. These are
 * in-progress boards, not history, so they live outside the `game_sessions` table.
 */
class SavedGameStore(context: Context) {
    private val prefs = context.getSharedPreferences("saved_games", Context.MODE_PRIVATE)

    private val _savedIds = MutableStateFlow(readIds())

    /** Ids of games that currently have something to resume. */
    val savedIds: StateFlow<Set<String>> = _savedIds

    fun load(gameId: String): SavedGame? {
        val state = prefs.getString(STATE + gameId, null) ?: return null
        return SavedGame(state, prefs.getInt(ELAPSED + gameId, 0))
    }

    fun save(gameId: String, state: String, elapsedSeconds: Int) {
        prefs.edit {
            putString(STATE + gameId, state)
            putInt(ELAPSED + gameId, elapsedSeconds)
        }
        _savedIds.value = readIds()
    }

    fun clear(gameId: String) {
        prefs.edit {
            remove(STATE + gameId)
            remove(ELAPSED + gameId)
        }
        _savedIds.value = readIds()
    }

    fun clearAll() {
        prefs.edit { clear() }
        _savedIds.value = emptySet()
    }

    private fun readIds(): Set<String> =
        prefs.all.keys.filter { it.startsWith(STATE) }.map { it.removePrefix(STATE) }.toSet()

    private companion object {
        const val STATE = "state:"
        const val ELAPSED = "elapsed:"
    }
}

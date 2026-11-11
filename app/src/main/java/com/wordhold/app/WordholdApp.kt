package com.wordhold.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.wordhold.app.core.data.SavedGameStore
import com.wordhold.app.core.data.SessionRepository
import com.wordhold.app.core.data.WordholdDatabase
import com.wordhold.app.core.words.GameAssets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class WordholdApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database by lazy {
        Room.databaseBuilder(this, WordholdDatabase::class.java, "wordhold.db").build()
    }

    val sessions by lazy { SessionRepository(database.gameSessionDao(), appScope) }

    val savedGames by lazy { SavedGameStore(this) }

    /** Word list and crossword clues, each parsed from its bundled asset on first use. */
    val gameAssets by lazy { GameAssets(assets) }
}

val Context.wordholdApp: WordholdApp get() = applicationContext as WordholdApp

package com.wordhold.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.wordhold.app.core.data.SessionRepository
import com.wordhold.app.core.data.WordholdDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class WordholdApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database by lazy {
        Room.databaseBuilder(this, WordholdDatabase::class.java, "wordhold.db").build()
    }

    val sessions by lazy { SessionRepository(database.gameSessionDao(), appScope) }
}

val Context.wordholdApp: WordholdApp get() = applicationContext as WordholdApp

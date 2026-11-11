package com.wordhold.app.core.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * One play of one game. This single table backs per-game stats and the combined
 * streak/progress view; games do not get their own tables.
 */
@Entity(
    tableName = "game_sessions",
    indices = [Index("gameId"), Index("date")],
)
data class GameSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [com.wordhold.app.core.game.WordGame.id] of the game played. */
    val gameId: String,
    /** Local calendar day the session ended, as days since 1970-01-01. See [currentEpochDay]. */
    val date: Long,
    val score: Int,
    val durationSeconds: Int,
    val completed: Boolean,
)

@Dao
interface GameSessionDao {
    @Insert
    suspend fun insert(session: GameSession)

    @Query("DELETE FROM game_sessions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM game_sessions WHERE gameId = :gameId AND completed = 1")
    suspend fun countCompleted(gameId: String): Int

    /** Newest first. */
    @Query("SELECT * FROM game_sessions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<GameSession>>
}

@Database(entities = [GameSession::class], version = 1, exportSchema = true)
abstract class WordholdDatabase : RoomDatabase() {
    abstract fun gameSessionDao(): GameSessionDao
}

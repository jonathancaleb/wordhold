package com.wordhold.app.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class SessionStatsTest {
    private val today = 20_000L

    private fun session(day: Long, score: Int = 10, completed: Boolean = true, seconds: Int = 60) =
        GameSession(gameId = "g", date = day, score = score, durationSeconds = seconds, completed = completed)

    @Test
    fun `streak is zero with no sessions`() {
        assertEquals(0, streakDays(emptyList(), today))
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        assertEquals(3, streakDays(listOf(today, today - 1, today - 2, today - 4), today))
    }

    @Test
    fun `streak survives until the end of today when last play was yesterday`() {
        assertEquals(2, streakDays(listOf(today - 1, today - 2), today))
    }

    @Test
    fun `streak is broken after a missed day`() {
        assertEquals(0, streakDays(listOf(today - 2, today - 3), today))
    }

    @Test
    fun `several sessions on one day count once`() {
        assertEquals(1, streakDays(listOf(today, today, today), today))
    }

    @Test
    fun `stats use newest session for last score and ignore unfinished games for streak`() {
        val stats = SessionStats.from(
            listOf(
                session(today, score = 5, completed = false, seconds = 30),
                session(today - 1, score = 40),
                session(today - 2, score = 25),
            ),
            today,
        )
        assertEquals(5, stats.lastScore)
        assertEquals(40, stats.bestScore)
        assertEquals(2, stats.streakDays)
        assertEquals(2, stats.completedCount)
        assertEquals(150L, stats.totalSeconds)
    }

    @Test
    fun `empty stats report not played`() {
        val stats = SessionStats.from(emptyList(), today)
        assertNull(stats.lastScore)
        assertFalse(stats.hasPlayed)
    }

    @Test
    fun `epoch day uses the local calendar day, including west of UTC before 1970`() {
        val nairobi = TimeZone.getTimeZone("Africa/Nairobi") // UTC+3, no DST
        // 1970-01-01 22:00 UTC is already 1970-01-02 in Nairobi.
        assertEquals(1L, currentEpochDay(22 * 3_600_000L, nairobi))
        val newYork = TimeZone.getTimeZone("America/New_York")
        // 1970-01-01 02:00 UTC is still 1969-12-31 in New York.
        assertEquals(-1L, currentEpochDay(2 * 3_600_000L, newYork))
    }
}

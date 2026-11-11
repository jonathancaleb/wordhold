package com.wordhold.app.core.data

import java.util.TimeZone

private const val MILLIS_PER_DAY = 86_400_000L

/** Today's local calendar day as days since 1970-01-01 (java.time is unavailable at minSdk 24). */
fun currentEpochDay(
    nowMillis: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): Long = Math.floorDiv(nowMillis + zone.getOffset(nowMillis), MILLIS_PER_DAY)

/**
 * Number of consecutive days with a completed session, counting back from [today].
 * A streak stays alive through today: if nothing is completed yet today, it counts
 * back from yesterday instead.
 */
fun streakDays(completedDays: Collection<Long>, today: Long): Int {
    val days = completedDays.toSet()
    var day = if (today in days) today else today - 1
    var streak = 0
    while (day in days) {
        streak++
        day--
    }
    return streak
}

/** The longest run of consecutive days in [completedDays]. */
fun bestStreakDays(completedDays: Collection<Long>): Int {
    var best = 0
    var run = 0
    var previous: Long? = null
    for (day in completedDays.toSortedSet()) {
        run = if (previous != null && day == previous + 1) run + 1 else 1
        best = maxOf(best, run)
        previous = day
    }
    return best
}

/**
 * The days of the last [weeks] calendar weeks, Monday first, ending with the week that
 * contains [today]. Later days of that week are included, so the list always fills a
 * seven-column grid.
 */
fun recentWeeks(today: Long, weeks: Int = 5): List<Long> {
    // Day 0, 1970-01-01, was a Thursday.
    val monday = today - Math.floorMod(today + 3, 7L)
    val first = monday - 7L * (weeks - 1)
    return List(weeks * 7) { first + it }
}

/** Stats derived from a set of sessions: either one game's, or all of them. */
data class SessionStats(
    val lastScore: Int?,
    val bestScore: Int?,
    val streakDays: Int,
    val completedCount: Int,
    val totalSeconds: Long,
) {
    val hasPlayed: Boolean get() = lastScore != null

    /** For one game's stats: the level the player is on. Each completed session is one level cleared. */
    val level: Int get() = completedCount + 1

    companion object {
        /** [sessions] must be ordered newest first, as [GameSessionDao.observeAll] returns them. */
        fun from(sessions: List<GameSession>, today: Long): SessionStats {
            val completed = sessions.filter { it.completed }
            return SessionStats(
                lastScore = sessions.firstOrNull()?.score,
                bestScore = sessions.maxOfOrNull { it.score },
                streakDays = streakDays(completed.map { it.date }, today),
                completedCount = completed.size,
                totalSeconds = sessions.sumOf { it.durationSeconds.toLong() },
            )
        }
    }
}

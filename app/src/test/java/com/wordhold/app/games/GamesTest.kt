package com.wordhold.app.games

import com.wordhold.app.core.words.WordList
import com.wordhold.app.games.unscramble.UnscrambleGame
import com.wordhold.app.games.wordsearch.Cell
import com.wordhold.app.games.wordsearch.WordSearchGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class UnscrambleGameTest {
    private val words = WordList(
        words = listOf("book", "stale", "river", "planet", "garden", "library", "kitchen", "alphabet", "notebook"),
        alsoAccepted = listOf("least", "slate", "steal", "platen", "danger", "gander", "ranged", "thicken"),
    )

    /** Level 3 draws 5-6 letter words: stale, river, planet, garden. */
    private fun game(seed: Int = 1, level: Int = 3) =
        UnscrambleGame(words, Random(seed), roundSize = 3).apply { this.level = level; start() }

    /** The round's answer for the letters on screen (the test list has one curated word per letter set). */
    private fun UnscrambleGame.answer() =
        words.words.first { it.toList().sorted() == letters.toList().sorted() }

    @Test
    fun `words get longer and skips fewer as the level rises`() {
        val expected = mapOf(1 to 4..5, 3 to 5..6, 6 to 6..7, 10 to 7..8, 40 to 7..8)
        for ((level, lengths) in expected) {
            repeat(10) { seed ->
                val game = game(seed, level)
                repeat(game.wordCount) {
                    assertTrue("level $level gave ${game.letters}", game.letters.length in lengths)
                    game.checkAnswer(game.answer())
                }
            }
        }
        assertEquals(3, game(level = 1).skipsLeft)
        assertEquals(2, game(level = 5).skipsLeft)
        assertEquals(1, game(level = 30).skipsLeft)
    }

    @Test
    fun `scrambles are never themselves a word`() {
        repeat(20) { seed ->
            val game = game(seed)
            assertFalse("scramble must not be a word: ${game.letters}", words.isWord(game.letters))
        }
    }

    @Test
    fun `correct answer scores ten per letter and advances`() {
        val game = game()
        val answer = game.answer()
        assertTrue(game.checkAnswer(answer.uppercase()))
        assertEquals(answer.length * 10, game.score)
        assertEquals(2, game.wordNumber)
        assertEquals(answer, game.previousWord)
    }

    @Test
    fun `any real anagram is accepted, non-words and wrong letters are not`() {
        // Level 1 with this list is a round of exactly "book" and "stale".
        val game = game(level = 1)
        if (game.answer() != "stale") game.checkAnswer(game.answer())
        val before = game.score
        assertFalse(game.checkAnswer("tsale")) // right letters, not a word
        assertFalse(game.checkAnswer("river")) // a word, wrong letters
        assertFalse(game.checkAnswer("stal"))
        assertEquals(before, game.score)
        assertTrue(game.checkAnswer("slate"))
        assertEquals(before + 50, game.score)
    }

    @Test
    fun `skips are limited, score nothing, and finishing the last word completes the round`() {
        val game = game(level = 5) // two skips
        game.skip()
        game.skip()
        assertEquals(3, game.wordNumber)
        assertEquals(0, game.skipsLeft)
        game.skip() // no skips left: nothing happens
        assertEquals(3, game.wordNumber)
        assertFalse(game.isComplete)
        assertEquals(0, game.score)

        assertTrue(game.checkAnswer(game.answer()))
        assertTrue(game.isComplete)
        assertFalse(game.checkAnswer("stale"))
    }

    @Test
    fun `shuffle keeps the same letters in a different order`() {
        val game = game()
        val before = game.letters
        game.shuffle()
        assertNotEquals(before, game.letters)
        assertEquals(before.toList().sorted(), game.letters.toList().sorted())
    }

    @Test
    fun `start resets a finished round`() {
        val game = game()
        repeat(3) { game.checkAnswer(game.answer()) }
        assertTrue(game.isComplete)
        game.start()
        assertFalse(game.isComplete)
        assertEquals(0, game.score)
        assertEquals(1, game.wordNumber)
        assertEquals(2, game.skipsLeft)
    }

    @Test
    fun `a round in progress can be saved and resumed exactly where it stopped`() {
        val game = game()
        assertNull("nothing to save before the first word is done", game.saveState())
        assertTrue(game.checkAnswer(game.answer()))
        game.skip()
        val saved = game.saveState()
        assertNotNull(saved)

        val restored = UnscrambleGame(words, Random(99), roundSize = 3)
        assertTrue(restored.restoreState(saved!!))
        assertEquals(3, restored.level)
        assertEquals(game.letters, restored.letters)
        assertEquals(game.wordNumber, restored.wordNumber)
        assertEquals(game.wordCount, restored.wordCount)
        assertEquals(game.skipsLeft, restored.skipsLeft)
        assertEquals(game.score, restored.score)
        assertEquals(game.previousWord, restored.previousWord)
        // The restored round still knows its answers and can be finished.
        assertTrue(restored.checkAnswer(restored.answer()))
        assertTrue(restored.isComplete)
        assertNull(restored.saveState())
    }

    @Test
    fun `corrupt saved rounds are rejected`() {
        val game = UnscrambleGame(words, Random(1))
        assertFalse(game.restoreState(""))
        assertFalse(game.restoreState("1|3|stale,river|5|1|0|elats|"))   // word number out of range
        assertFalse(game.restoreState("1|3|stale,river|1|1|0|xxxxx|"))   // letters are not the word's
        assertFalse(game.restoreState("1|3|stale,RIVER|1|1|0|elats|"))   // not lowercase a-z
        assertFalse(game.restoreState("2|3|stale,river|1|1|0|elats|"))   // unknown version
        assertTrue(game.restoreState("1|3|stale,river|1|1|0|elats|"))
    }
}

class WordSearchGameTest {
    private val words = WordList(
        listOf(
            "book", "read", "reader", "story", "novel", "planet", "garden", "library", "puzzle", "word",
            "alphabet", "river", "ocean", "tiger", "camera", "window", "kitchen", "morning", "notebook", "silver",
        )
    )

    private fun game(seed: Int = 1, level: Int = 3) = WordSearchGame(words, Random(seed)).apply { this.level = level; start() }

    @Test
    fun `every board is full and hides each word where its placement says, at every level`() {
        val expected = mapOf(1 to (7 to 5), 4 to (8 to 6), 7 to (9 to 8), 12 to (10 to 10))
        for ((level, shape) in expected) {
            val (size, count) = shape
            repeat(25) { seed ->
                val game = game(seed, level)
                assertEquals(size, game.gridSize)
                assertEquals(size, game.grid.size)
                assertTrue(game.grid.all { row -> row.length == size && row.all { it in 'a'..'z' } })
                assertEquals(count, game.placements.size)
                for (placement in game.placements) {
                    val read = placement.cells.map { game.grid[it.row][it.col] }.joinToString("")
                    assertEquals(placement.word, read)
                    assertEquals(placement.cells, game.cellsBetween(placement.cells.first(), placement.cells.last()))
                }
            }
        }
    }

    @Test
    fun `early levels only run across and down, late levels also run backwards`() {
        fun steps(level: Int) = (0 until 40).flatMap { seed ->
            game(seed, level).placements.map { p -> (p.cells[1].row - p.cells[0].row) to (p.cells[1].col - p.cells[0].col) }
        }.toSet()

        assertEquals(setOf(0 to 1, 1 to 0), steps(1))
        assertTrue((0 to -1) in steps(12))
        assertTrue((1 to 1) in steps(4))
        assertFalse((0 to -1) in steps(4))
    }

    @Test
    fun `no chosen word contains another`() {
        repeat(50) { seed ->
            val chosen = game(seed).placements.map { it.word }
            assertFalse(chosen.toString(), "read" in chosen && "reader" in chosen)
        }
    }

    @Test
    fun `selecting a word in either direction finds it once, and finding all completes the game`() {
        val game = game()
        val (first, rest) = game.placements.first() to game.placements.drop(1)

        assertTrue(game.select(first.cells.last(), first.cells.first())) // backwards
        assertEquals(first.word.length * 10, game.score)
        assertFalse("already found", game.select(first.cells.first(), first.cells.last()))
        assertFalse(game.isComplete)

        rest.forEach { assertTrue(game.select(it.cells.first(), it.cells.last())) }
        assertTrue(game.isComplete)
        assertEquals(game.placements.sumOf { it.word.length } * 10, game.score)
    }

    @Test
    fun `a selection that is not a hidden word or not a straight line is rejected`() {
        val game = game()
        assertFalse(game.select(Cell(0, 0), Cell(1, 2)))
        assertFalse(game.checkAnswer("zzzz"))
        assertEquals(0, game.score)
    }

    @Test
    fun `snap keeps aligned cells, straightens wobbly drags and stays on the board`() {
        val game = game() // 8 x 8
        assertEquals(Cell(3, 3), game.snap(Cell(3, 3), Cell(3, 3)))
        assertEquals(Cell(3, 7), game.snap(Cell(3, 3), Cell(3, 7)))
        assertEquals(Cell(0, 0), game.snap(Cell(3, 3), Cell(0, 0)))
        assertEquals(Cell(3, 7), game.snap(Cell(3, 3), Cell(4, 7))) // nearly horizontal
        assertEquals(Cell(7, 3), game.snap(Cell(3, 3), Cell(7, 2))) // nearly vertical
        // Diagonal from (6,1) toward (2,6) would leave the grid at length 5; clamps to the edge.
        val snapped = game.snap(Cell(6, 1), Cell(2, 6))
        assertTrue(snapped.row in 0..7 && snapped.col in 0..7)
        assertTrue(game.cellsBetween(Cell(6, 1), snapped).isNotEmpty())
    }

    @Test
    fun `a board in progress can be saved and resumed, at every level`() {
        for (level in listOf(1, 4, 7, 12)) {
            val game = game(level = level)
            assertNull("nothing to save before the first find", game.saveState())
            val first = game.placements.first()
            assertTrue(game.select(first.cells.first(), first.cells.last()))
            val saved = game.saveState()
            assertNotNull(saved)

            val restored = WordSearchGame(words, Random(99))
            assertTrue(restored.restoreState(saved!!))
            assertEquals(level, restored.level)
            assertEquals(game.gridSize, restored.gridSize)
            assertEquals(game.grid, restored.grid)
            assertEquals(game.found, restored.found)
            assertEquals(game.score, restored.score)
            assertEquals(game.placements.map { it.word to it.cells }, restored.placements.map { it.word to it.cells })

            assertFalse("already found", restored.select(first.cells.first(), first.cells.last()))
            restored.placements.drop(1).forEach { assertTrue(restored.select(it.cells.last(), it.cells.first())) }
            assertTrue(restored.isComplete)
            assertNull(restored.saveState())
        }
    }

    @Test
    fun `corrupt saved boards are rejected`() {
        val game = WordSearchGame(words, Random(1))
        val good = "1|1|abc,def,ghi|abc:0:0:0:2;adg:0:0:2:0|abc|30"
        assertTrue(game.restoreState(good))
        assertFalse(game.restoreState(""))
        assertFalse(game.restoreState("1|1|abc,def,gh|abc:0:0:0:2;adg:0:0:2:0|abc|30"))   // grid not square
        assertFalse(game.restoreState("1|1|abc,def,ghi|abd:0:0:0:2;adg:0:0:2:0|abd|30"))   // word is not in the grid there
        assertFalse(game.restoreState("1|1|abc,def,ghi|abc:0:0:0:9;adg:0:0:2:0|abc|30"))   // off the board
        assertFalse(game.restoreState("1|1|abc,def,ghi|abc:0:0:0:2;adg:0:0:2:0|xyz|30"))   // found a word that is not hidden
        assertFalse(game.restoreState("1|1|abc,def,ghi|abc:0:0:0:2|abc|30"))               // everything already found
        assertFalse(game.restoreState("7|1|abc,def,ghi|abc:0:0:0:2;adg:0:0:2:0|abc|30"))   // unknown version
    }
}

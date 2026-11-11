package com.wordhold.app.games

import com.wordhold.app.core.game.Whimsy
import com.wordhold.app.games.crossword.CrosswordGame
import com.wordhold.app.games.crossword.CrosswordGenerator
import com.wordhold.app.games.crossword.CrosswordPuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CrosswordGameTest {
    /** The real bundled clue bank, read straight from the asset file. */
    private val clues: Map<String, String> = run {
        val file = listOf("src/main/assets/crossword.json", "app/src/main/assets/crossword.json")
            .map(::File).first { it.exists() }
        Regex("\"([a-z]+)\":\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(file.readText().substringAfter("\"clues\":"))
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    private fun game(level: Int = 1) = CrosswordGame(clues).apply { this.level = level; start() }

    /** Every maximal run of two or more letters in the grid, across then down, as lists of cell indices. */
    private fun runs(puzzle: CrosswordPuzzle): Set<List<Int>> {
        val found = HashSet<List<Int>>()
        fun scan(lines: Int, length: Int, index: (Int, Int) -> Int) {
            for (line in 0 until lines) {
                var run = ArrayList<Int>()
                for (i in 0..length) {
                    val cell = if (i < length) index(line, i) else -1
                    if (cell >= 0 && puzzle.solution[cell] != null) {
                        run.add(cell)
                    } else {
                        if (run.size >= 2) found += run
                        run = ArrayList()
                    }
                }
            }
        }
        scan(puzzle.rows, puzzle.cols) { r, c -> r * puzzle.cols + c }
        scan(puzzle.cols, puzzle.rows) { c, r -> r * puzzle.cols + c }
        return found
    }

    @Test
    fun `clue bank loads`() {
        assertTrue("only ${clues.size} clues parsed", clues.size > 200)
        assertEquals("Water flowing to the sea", clues["river"])
    }

    @Test
    fun `every level up to 40 is a proper interlocking crossword that fits its size`() {
        for (level in 1..40) {
            val spec = CrosswordGenerator.specFor(level)
            val puzzle = CrosswordGenerator.generate(clues, level)
            val label = "level $level"

            assertTrue("$label: ${puzzle.rows}x${puzzle.cols}", puzzle.rows <= spec.maxSize && puzzle.cols <= spec.maxSize)
            assertEquals("$label word count", spec.wordCount, puzzle.words.size)
            assertEquals("$label has a repeated answer", puzzle.words.size, puzzle.words.map { it.answer }.toSet().size)

            for (word in puzzle.words) {
                assertTrue("$label: ${word.answer} length", word.answer.length in spec.lengths)
                assertEquals(label, word.answer, word.cells.map { puzzle.solution[it] }.joinToString(""))
                assertEquals(label, clues[word.answer], word.clue)
                val step = if (word.across) 1 else puzzle.cols
                assertTrue(label, word.cells.zipWithNext().all { (a, b) -> b - a == step })
                assertTrue("$label: ${word.answer} crosses nothing",
                    puzzle.words.any { other -> other !== word && other.cells.any { it in word.cells } })
            }
            // The only runs of letters in the grid are the placed words: nothing is spelled by accident.
            assertEquals(label, puzzle.words.map { it.cells }.toSet(), runs(puzzle))
            // Every letter cell belongs to a word.
            val covered = puzzle.words.flatMap { it.cells }.toSet()
            assertTrue(label, puzzle.solution.indices.all { (puzzle.solution[it] != null) == (it in covered) })
            // Numbers rise in reading order of the starting cells.
            val starts = puzzle.words.map { it.number to it.cells.first() }.distinct().sortedBy { it.first }
            assertEquals(label, starts.map { it.second }.sorted(), starts.map { it.second })
        }
    }

    @Test
    fun `a level is always the same puzzle, and different levels differ`() {
        val a = CrosswordGenerator.generate(clues, 6)
        val b = CrosswordGenerator.generate(clues, 6)
        assertEquals(a.solution, b.solution)
        assertEquals(a.words.map { it.answer }, b.words.map { it.answer })
        assertNotEquals(a.words.map { it.answer }, CrosswordGenerator.generate(clues, 7).words.map { it.answer })
    }

    @Test
    fun `puzzles grow with the level`() {
        assertEquals(4, game(1).puzzle.words.size)
        assertEquals(6, game(4).puzzle.words.size)
        assertEquals(8, game(8).puzzle.words.size)
        assertEquals(10, game(15).puzzle.words.size)
    }

    @Test
    fun `typing a word letter by letter solves it, scores it and moves on`() {
        val game = game()
        val first = game.currentWord!!
        assertEquals(first.cells.first(), game.selectedCell)
        first.answer.forEach { game.type(it.uppercaseChar()) }

        assertTrue(0 in game.solved)
        // Crossing words may complete too, so the score is at least this word's.
        assertTrue(game.score >= first.answer.length * 10)
        assertTrue(first.cells.all { game.isLocked(it) })
        assertNotEquals(0, game.selectedWord)
    }

    @Test
    fun `a wrong word is flagged, can be corrected with backspace, and locked letters cannot be changed`() {
        val game = game()
        val word = game.currentWord!!
        val wrongLast = if (word.answer.last() == 'z') 'y' else 'z'
        (word.answer.dropLast(1) + wrongLast).forEach { game.type(it) }
        assertTrue(game.isWrong(0))
        assertEquals(0, game.score)
        assertEquals(word.cells.last(), game.selectedCell)

        game.backspace()
        assertNull(game.entries[word.cells.last()])
        assertFalse(game.isWrong(0))
        game.type(word.answer.last())
        assertTrue(0 in game.solved)

        game.tapCell(word.cells.first())
        game.type('q')
        assertEquals(word.answer.first(), game.entries[word.cells.first()])
    }

    @Test
    fun `tapping a crossing cell twice switches between across and down`() {
        val game = game(4)
        val words = game.puzzle.words
        val crossing = words[0].cells.first { cell -> words.count { cell in it.cells } == 2 }
        game.tapCell(crossing)
        val firstChoice = game.selectedWord
        game.tapCell(crossing)
        assertNotEquals(firstChoice, game.selectedWord)
        assertTrue(crossing in game.currentWord!!.cells)
        game.tapCell(crossing)
        assertEquals(firstChoice, game.selectedWord)
    }

    @Test
    fun `hint reveals the selected letter for five points`() {
        val game = game()
        val words = game.puzzle.words
        // Solve one word for some score, then take a hint on another.
        assertTrue(game.checkAnswer(words[0].answer))
        val scoreBefore = game.score
        val target = words.indices.first { it !in game.solved }
        val cell = words[target].cells.first { !game.isLocked(it) }
        game.tapCell(cell)
        assertTrue(game.hint())
        assertEquals(game.puzzle.solution[cell], game.entries[cell])
        // Five points off, unless that letter happened to finish the word.
        if (target !in game.solved) assertEquals(scoreBefore - 5, game.score)
        assertFalse("nothing left to reveal there", game.selectedCell == cell && game.hint())
    }

    @Test
    fun `solving every word completes the level`() {
        for (level in listOf(1, 5, 12)) {
            val game = game(level)
            var guard = 0
            while (!game.isComplete && guard++ < 50) {
                val word = game.currentWord!!
                assertTrue(game.checkAnswer(word.answer))
                if (!game.isComplete) game.nextWord()
            }
            assertTrue(game.isComplete)
            assertEquals(game.puzzle.words.sumOf { it.answer.length } * 10, game.score)
            assertFalse(game.checkAnswer("abc"))
            assertNull(game.saveState())
        }
    }

    @Test
    fun `an untouched puzzle is not saved, a started one round-trips with its level`() {
        val game = game(7)
        assertNull(game.saveState())
        assertTrue(game.checkAnswer(game.currentWord!!.answer))
        game.nextWord()
        game.type('x')
        val saved = game.saveState()
        assertNotNull(saved)

        val restored = CrosswordGame(clues)
        assertTrue(restored.restoreState(saved!!))
        assertEquals(7, restored.level)
        assertEquals(game.entries, restored.entries)
        assertEquals(game.solved, restored.solved)
        assertEquals(game.score, restored.score)
        assertFalse(restored.currentWord == null)
        assertTrue(restored.selectedWord !in restored.solved)
    }

    @Test
    fun `corrupt saved state is rejected`() {
        val game = CrosswordGame(clues)
        assertFalse(game.restoreState(""))
        assertFalse(game.restoreState("1|0|....|0"))
        assertFalse(game.restoreState("1|3|abc|0"))
        assertFalse(game.restoreState("9|3|abc|0"))
        // Right length, but every cell holds a letter, including the gaps between words.
        val size = CrosswordGenerator.generate(clues, 3).solution.size
        assertFalse(game.restoreState("1|3|${"a".repeat(size)}|0"))
    }
}

class WhimsyTest {
    @Test
    fun `rank climbs with the level and never falls`() {
        assertEquals("Page Turner", Whimsy.rank(1))
        assertEquals("Page Turner", Whimsy.rank(2))
        assertEquals("Word Nibbler", Whimsy.rank(3))
        assertEquals("Bookworm", Whimsy.rank(5))
        assertEquals("Grand Vocabularian", Whimsy.rank(500))
    }

    @Test
    fun `cheers and facts are defined for every level and vary`() {
        for (level in 1..200) for (game in 0..5) {
            assertTrue(Whimsy.cheer(level).isNotBlank())
            assertTrue(Whimsy.fact(level, game).isNotBlank())
        }
        assertNotEquals(Whimsy.fact(1, 0), Whimsy.fact(2, 0))
        assertNotEquals(Whimsy.fact(1, 0), Whimsy.fact(1, 1))
        assertNotEquals(Whimsy.cheer(1), Whimsy.cheer(2))
    }
}

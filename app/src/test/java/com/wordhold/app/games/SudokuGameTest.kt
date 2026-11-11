package com.wordhold.app.games

import com.wordhold.app.games.sudoku.SudokuGame
import com.wordhold.app.games.sudoku.SudokuGenerator
import com.wordhold.app.settings.LegalBlock
import com.wordhold.app.settings.parseLegal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SudokuGameTest {
    /** Level 5 starts with 36 digits given. */
    private fun game(seed: Int = 1, level: Int = 5) = SudokuGame(Random(seed)).apply { this.level = level; start() }

    private fun units(): List<List<Int>> {
        val rows = (0 until 9).map { r -> (0 until 9).map { r * 9 + it } }
        val cols = (0 until 9).map { c -> (0 until 9).map { it * 9 + c } }
        val boxes = (0 until 9).map { b -> (0 until 9).map { (b / 3 * 3 + it / 3) * 9 + b % 3 * 3 + it % 3 } }
        return rows + cols + boxes
    }

    /** The unique solution, found by brute force: the one digit per empty cell that keeps it solvable. */
    private fun solve(cells: List<Int>): IntArray {
        val grid = cells.toIntArray()
        for (i in grid.indices) {
            if (grid[i] != 0) continue
            grid[i] = (1..9).first { d ->
                grid[i] = d
                SudokuGenerator.countSolutions(grid, limit = 1) == 1
            }
        }
        return grid
    }

    @Test
    fun `solved grids are valid`() {
        repeat(20) { seed ->
            val grid = SudokuGenerator.solvedGrid(Random(seed))
            units().forEach { unit -> assertEquals((1..9).toSet(), unit.map { grid[it] }.toSet()) }
        }
    }

    @Test
    fun `puzzles have exactly one solution and the requested number of givens`() {
        repeat(10) { seed ->
            val game = game(seed)
            assertEquals(1, SudokuGenerator.countSolutions(game.cells.toIntArray(), limit = 2))
            assertEquals(36, game.given.count { it })
            assertTrue(game.cells.indices.all { game.given[it] == (game.cells[it] != 0) })
        }
    }

    @Test
    fun `higher levels give fewer digits, levelling off at 26`() {
        assertEquals(44, SudokuGame.givens(1))
        assertEquals(36, SudokuGame.givens(5))
        assertEquals(26, SudokuGame.givens(10))
        assertEquals(26, SudokuGame.givens(99))
        assertEquals(44, game(level = 1).given.count { it })
        // Hard puzzles cannot always be carved down to the target, but must get close and stay unique.
        repeat(5) { seed ->
            val hard = game(seed, level = 10)
            assertTrue(hard.given.count { it } in 26..30)
            assertEquals(1, SudokuGenerator.countSolutions(hard.cells.toIntArray(), limit = 2))
        }
    }

    @Test
    fun `solver reports multiple and zero solutions`() {
        assertEquals(2, SudokuGenerator.countSolutions(IntArray(81), limit = 2))
        val contradictory = IntArray(81).also { it[0] = 5; it[1] = 5 }
        assertEquals(0, SudokuGenerator.countSolutions(contradictory, limit = 2))
    }

    @Test
    fun `correct digit scores and locks, wrong digit costs points and can be erased`() {
        val game = game()
        val solution = solve(game.cells)
        val (a, b) = game.cells.indices.filter { !game.given[it] }

        assertFalse("nothing selected", game.checkAnswer(solution[a].toString()))

        game.select(a)
        assertTrue(game.checkAnswer(solution[a].toString()))
        assertEquals(10, game.score)
        assertTrue(game.isLocked(a))
        assertFalse("locked cell cannot be changed", game.checkAnswer((solution[a] % 9 + 1).toString()))
        assertEquals(solution[a], game.cells[a])

        game.select(b)
        val wrong = solution[b] % 9 + 1
        assertFalse(game.checkAnswer(wrong.toString()))
        assertEquals(1, game.mistakes)
        assertEquals(5, game.score)
        assertTrue(game.isWrong(b))
        game.erase()
        assertEquals(0, game.cells[b])

        assertFalse(game.checkAnswer("0"))
        assertFalse(game.checkAnswer("x"))
        assertEquals(1, game.mistakes)
    }

    @Test
    fun `given cells cannot be changed and score never goes negative`() {
        val game = game()
        val solution = solve(game.cells)
        game.select(game.given.indexOf(true))
        assertFalse(game.checkAnswer("1"))
        assertEquals(0, game.mistakes)

        val empty = game.given.indexOf(false)
        game.select(empty)
        game.checkAnswer((solution[empty] % 9 + 1).toString())
        assertEquals(0, game.score)
    }

    @Test
    fun `filling every cell completes the game`() {
        val game = game()
        val solution = solve(game.cells)
        val empties = game.cells.indices.filter { !game.given[it] }
        empties.forEach {
            assertFalse(game.isComplete)
            game.select(it)
            assertTrue(game.checkAnswer(solution[it].toString()))
        }
        assertTrue(game.isComplete)
        assertEquals(empties.size * 10, game.score)
        assertNull("a finished game is not saved", game.saveState())
    }

    @Test
    fun `an untouched puzzle is not saved, a started one round-trips`() {
        val game = game()
        assertNull(game.saveState())

        val solution = solve(game.cells)
        val (a, b) = game.cells.indices.filter { !game.given[it] }
        game.select(a); game.checkAnswer(solution[a].toString())
        game.select(b); game.checkAnswer((solution[b] % 9 + 1).toString())
        val saved = game.saveState()
        assertNotNull(saved)

        val restored = SudokuGame(Random(99))
        assertTrue(restored.restoreState(saved!!))
        assertEquals(game.cells, restored.cells)
        assertEquals(game.given, restored.given)
        assertEquals(game.score, restored.score)
        assertEquals(game.mistakes, restored.mistakes)
        assertEquals(5, restored.level)
        assertTrue(restored.isWrong(b))
        // The restored game still knows the solution.
        restored.select(b)
        assertTrue(restored.checkAnswer(solution[b].toString()))
    }

    @Test
    fun `corrupt saved state is rejected`() {
        val game = SudokuGame(Random(1))
        assertFalse(game.restoreState(""))
        assertFalse(game.restoreState("2|5|abc|def|ghi|0|0"))
        // A save from the older format, before levels.
        assertFalse(game.restoreState("1|" + "1".repeat(81) + "|" + "0".repeat(81) + "|" + "0".repeat(81) + "|0|0"))
        // Solution containing a zero.
        assertFalse(game.restoreState("2|5|" + "0".repeat(81) + "|" + "0".repeat(81) + "|" + "0".repeat(81) + "|0|0"))
    }
}

class LegalParserTest {
    @Test
    fun `parses headings, bullets and paragraphs, drops the title and fills in the contact address`() {
        val blocks = parseLegal(
            "# Title\n\nEffective today\n\n## Contact\n\n- one\n- two\n\nWrite to {{CONTACT_EMAIL}}.\n",
            contactEmail = "help@example.com",
        )
        assertEquals(
            listOf(
                LegalBlock.Paragraph("Effective today"),
                LegalBlock.Heading("Contact"),
                LegalBlock.Bullet("one"),
                LegalBlock.Bullet("two"),
                LegalBlock.Paragraph("Write to help@example.com."),
            ),
            blocks,
        )
    }
}

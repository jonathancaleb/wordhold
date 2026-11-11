package com.wordhold.app.games.sudoku

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wordhold.app.core.game.LeveledGame
import com.wordhold.app.core.game.ResumableGame
import com.wordhold.app.core.game.WordGame
import kotlin.random.Random

/**
 * Classic 9x9 Sudoku. Cells are indexed 0..80, row by row; 0 means empty.
 *
 * Each entry is checked as it is made: a correct digit scores 10 and locks in, a
 * wrong one costs 5 (never below zero), counts as a mistake and stays on the board
 * marked wrong until it is replaced or erased.
 *
 * Higher levels start with fewer digits filled in.
 */
class SudokuGame(
    private val random: Random = Random.Default,
) : WordGame, LeveledGame, ResumableGame {
    override val id = ID
    override val title = "Sudoku"
    override var level = 1

    override var score by mutableIntStateOf(0)
        private set
    override var isComplete by mutableStateOf(false)
        private set

    /** Current digits, 81 of them. */
    var cells by mutableStateOf(List(CELLS) { 0 })
        private set

    /** True where the digit was part of the puzzle and cannot be changed. */
    var given by mutableStateOf(List(CELLS) { false })
        private set
    var selected by mutableStateOf<Int?>(null)
        private set
    var mistakes by mutableIntStateOf(0)
        private set

    private var solution = IntArray(CELLS)

    override fun start() {
        solution = SudokuGenerator.solvedGrid(random)
        val puzzle = SudokuGenerator.puzzle(solution, givens(level), random)
        cells = puzzle.toList()
        given = puzzle.map { it != 0 }
        selected = null
        mistakes = 0
        score = 0
        isComplete = false
    }

    fun select(index: Int) {
        if (index in 0 until CELLS) selected = index
    }

    fun isWrong(index: Int): Boolean = cells[index] != 0 && cells[index] != solution[index]

    /** A cell can be edited unless it was given or already holds the right digit. */
    fun isLocked(index: Int): Boolean = given[index] || cells[index] == solution[index]

    /** Places the digit in [input] ("1".."9") into the selected cell and says whether it is right. */
    override fun checkAnswer(input: String): Boolean {
        val digit = input.trim().toIntOrNull() ?: return false
        val index = selected ?: return false
        if (isComplete || digit !in 1..9 || isLocked(index) || cells[index] == digit) return false

        cells = cells.toMutableList().also { it[index] = digit }
        val correct = digit == solution[index]
        if (correct) {
            score += POINTS_PER_CELL
            isComplete = cells.indices.all { cells[it] == solution[it] }
        } else {
            mistakes++
            score = maxOf(0, score - MISTAKE_PENALTY)
        }
        return correct
    }

    /** Clears a wrong digit from the selected cell. */
    fun erase() {
        val index = selected ?: return
        if (!isComplete && isWrong(index)) cells = cells.toMutableList().also { it[index] = 0 }
    }

    /** How many of [digit] are correctly on the board; the pad dims a digit at nine. */
    fun placedCount(digit: Int): Int = cells.indices.count { cells[it] == digit && solution[it] == digit }

    // --- ResumableGame -------------------------------------------------------

    override fun saveState(): String? {
        val untouched = cells.indices.all { given[it] || cells[it] == 0 }
        if (isComplete || untouched) return null
        return listOf(
            VERSION,
            level.toString(),
            solution.joinToString(""),
            given.joinToString("") { if (it) "1" else "0" },
            cells.joinToString(""),
            mistakes.toString(),
            score.toString(),
        ).joinToString("|")
    }

    override fun restoreState(state: String): Boolean {
        val parts = state.split("|")
        if (parts.size != 7 || parts[0] != VERSION) return false
        val savedLevel = parts[1].toIntOrNull()?.takeIf { it >= 1 } ?: return false
        val grids = parts.subList(2, 5)
        if (grids.any { grid -> grid.length != CELLS || grid.any { it !in '0'..'9' } }) return false
        val savedSolution = parts[2].map { it - '0' }.toIntArray()
        val savedGiven = parts[3].map { it == '1' }
        val savedCells = parts[4].map { it - '0' }
        val savedMistakes = parts[5].toIntOrNull() ?: return false
        val savedScore = parts[6].toIntOrNull() ?: return false
        val consistent = savedSolution.all { it in 1..9 } &&
            savedCells.indices.all { !savedGiven[it] || savedCells[it] == savedSolution[it] }
        if (!consistent) return false

        level = savedLevel
        solution = savedSolution
        given = savedGiven
        cells = savedCells
        mistakes = savedMistakes
        score = savedScore
        selected = null
        isComplete = false
        return true
    }

    companion object {
        const val ID = "sudoku"
        const val CELLS = 81
        const val POINTS_PER_CELL = 10
        const val MISTAKE_PENALTY = 5
        private const val VERSION = "2"

        /** 44 digits given at level 1, two fewer each level, down to 26 from level 10. */
        fun givens(level: Int): Int = (46 - 2 * level).coerceIn(26, 44)
    }
}

/** Builds solved grids and carves single-solution puzzles out of them. */
internal object SudokuGenerator {
    /** A random valid solved grid: a base pattern with its bands, rows, columns and digits shuffled. */
    fun solvedGrid(random: Random): IntArray {
        fun shuffledLines() = (0..2).shuffled(random).flatMap { band -> (0..2).shuffled(random).map { band * 3 + it } }
        val rows = shuffledLines()
        val cols = shuffledLines()
        val digits = (1..9).shuffled(random)
        return IntArray(81) { i ->
            val r = rows[i / 9]
            val c = cols[i % 9]
            digits[(3 * (r % 3) + r / 3 + c) % 9]
        }
    }

    /**
     * Removes digits from [solution] in random order, keeping each removal only if the
     * puzzle still has exactly one solution, until [givens] remain (or no more can go).
     */
    fun puzzle(solution: IntArray, givens: Int, random: Random): IntArray {
        val grid = solution.copyOf()
        var remaining = 81
        for (index in (0 until 81).shuffled(random)) {
            if (remaining <= givens) break
            val digit = grid[index]
            grid[index] = 0
            if (countSolutions(grid, limit = 2) == 1) remaining-- else grid[index] = digit
        }
        return grid
    }

    /** Counts solutions of [grid], stopping at [limit]. Does not modify [grid]. */
    fun countSolutions(grid: IntArray, limit: Int): Int {
        val work = grid.copyOf()
        val rows = IntArray(9)
        val cols = IntArray(9)
        val boxes = IntArray(9)
        for (i in 0 until 81) {
            val d = work[i]
            if (d != 0) {
                val bit = 1 shl d
                val (r, c, b) = Triple(i / 9, i % 9, i / 27 * 3 + i % 9 / 3)
                if ((rows[r] or cols[c] or boxes[b]) and bit != 0) return 0 // contradictory givens
                rows[r] = rows[r] or bit
                cols[c] = cols[c] or bit
                boxes[b] = boxes[b] or bit
            }
        }

        var count = 0
        fun search() {
            // Branch on the empty cell with the fewest candidates.
            var best = -1
            var bestMask = 0
            var bestOptions = 10
            for (i in 0 until 81) {
                if (work[i] != 0) continue
                val free = ALL_DIGITS and (rows[i / 9] or cols[i % 9] or boxes[i / 27 * 3 + i % 9 / 3]).inv()
                val options = Integer.bitCount(free)
                if (options < bestOptions) {
                    best = i
                    bestMask = free
                    bestOptions = options
                    if (options <= 1) break
                }
            }
            if (best == -1) {
                count++
                return
            }
            val r = best / 9
            val c = best % 9
            val b = best / 27 * 3 + c / 3
            for (d in 1..9) {
                val bit = 1 shl d
                if (bestMask and bit == 0) continue
                work[best] = d
                rows[r] = rows[r] or bit; cols[c] = cols[c] or bit; boxes[b] = boxes[b] or bit
                search()
                rows[r] = rows[r] xor bit; cols[c] = cols[c] xor bit; boxes[b] = boxes[b] xor bit
                work[best] = 0
                if (count >= limit) return
            }
        }
        search()
        return count
    }

    private const val ALL_DIGITS = 0b1111111110
}

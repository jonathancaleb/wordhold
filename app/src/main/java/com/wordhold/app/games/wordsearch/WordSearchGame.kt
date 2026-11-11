package com.wordhold.app.games.wordsearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wordhold.app.core.game.LeveledGame
import com.wordhold.app.core.game.ResumableGame
import com.wordhold.app.core.game.WordGame
import com.wordhold.app.core.words.WordList
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class Cell(val row: Int, val col: Int)

/** A hidden word and the cells it occupies, first letter first. */
class Placement(val word: String, val cells: List<Cell>)

/**
 * A square letter grid with words hidden in straight lines. A word may be selected
 * from either end. Each word found scores 10 points per letter.
 *
 * Higher levels use a bigger grid with more words, and add diagonals, then words
 * written backwards.
 */
class WordSearchGame(
    private val words: WordList,
    private val random: Random = Random.Default,
) : WordGame, LeveledGame, ResumableGame {
    override val id = ID
    override val title = "Word Search"
    override var level = 1

    override var score by mutableIntStateOf(0)
        private set
    override var isComplete by mutableStateOf(false)
        private set

    var gridSize by mutableIntStateOf(0)
        private set

    /** Grid rows, lowercase, each [gridSize] long. */
    var grid by mutableStateOf<List<String>>(emptyList())
        private set
    var placements by mutableStateOf<List<Placement>>(emptyList())
        private set
    var found by mutableStateOf<Set<String>>(emptySet())
        private set

    override fun start() {
        val spec = specFor(level)
        val pool = words.wordsOfLength(4..minOf(spec.maxWordLength, spec.gridSize))
        check(pool.isNotEmpty()) { "Word list has no words that fit the grid" }
        repeat(MAX_BOARDS) {
            if (tryBuildBoard(pool, spec)) {
                found = emptySet()
                score = 0
                isComplete = false
                return
            }
        }
        error("Could not build a word search board")
    }

    /** Accepts the letters of any hidden word not yet found, read in either direction. */
    override fun checkAnswer(input: String): Boolean {
        if (isComplete) return false
        val guess = input.trim().lowercase()
        val match = placements.firstOrNull {
            it.word !in found && (it.word == guess || it.word == guess.reversed())
        } ?: return false
        found = found + match.word
        score += match.word.length * POINTS_PER_LETTER
        isComplete = found.size == placements.size
        return true
    }

    /** Checks the straight run of cells from [start] to [end]. */
    fun select(start: Cell, end: Cell): Boolean {
        val cells = cellsBetween(start, end)
        if (cells.isEmpty()) return false
        return checkAnswer(cells.map { grid[it.row][it.col] }.joinToString(""))
    }

    /**
     * The cell nearest [toward] that lies on one of the eight straight lines out of
     * [start], so a slightly wobbly drag still selects a clean line.
     */
    fun snap(start: Cell, toward: Cell): Cell {
        val dRow = toward.row - start.row
        val dCol = toward.col - start.col
        if (dRow == 0 && dCol == 0) return start
        val octant = (atan2(dRow.toDouble(), dCol.toDouble()) / (PI / 4)).roundToInt()
        val stepRow = sin(octant * PI / 4).roundToInt()
        val stepCol = cos(octant * PI / 4).roundToInt()
        var length = max(abs(dRow), abs(dCol))
        while (length > 0 && !inBounds(start.row + stepRow * length, start.col + stepCol * length)) length--
        return Cell(start.row + stepRow * length, start.col + stepCol * length)
    }

    /** Cells from [start] to [end] inclusive, or empty if they are not on a straight line. */
    fun cellsBetween(start: Cell, end: Cell): List<Cell> {
        val dRow = end.row - start.row
        val dCol = end.col - start.col
        if (dRow != 0 && dCol != 0 && abs(dRow) != abs(dCol)) return emptyList()
        val length = max(abs(dRow), abs(dCol))
        val stepRow = Integer.signum(dRow)
        val stepCol = Integer.signum(dCol)
        return List(length + 1) { Cell(start.row + stepRow * it, start.col + stepCol * it) }
    }

    private fun inBounds(row: Int, col: Int, size: Int = gridSize) = row in 0 until size && col in 0 until size

    private fun tryBuildBoard(pool: List<String>, spec: Spec): Boolean {
        // No chosen word may sit inside another ("read" in "reader"), or selecting
        // part of the longer one would count as finding the shorter.
        val chosen = mutableListOf<String>()
        for (word in pool.shuffled(random)) {
            if (chosen.size == spec.wordCount) break
            val clashes = chosen.any {
                word in it || it in word || word.reversed() in it || it.reversed() in word
            }
            if (!clashes) chosen += word
        }

        val board = Array(spec.gridSize) { CharArray(spec.gridSize) { EMPTY } }
        val placed = chosen.sortedByDescending { it.length }.map { place(board, it, spec) ?: return false }

        for (row in board) for (col in row.indices) {
            if (row[col] == EMPTY) row[col] = 'a' + random.nextInt(26)
        }
        gridSize = spec.gridSize
        grid = board.map { String(it) }
        placements = placed.sortedBy { it.word }
        return true
    }

    private fun place(board: Array<CharArray>, word: String, spec: Spec): Placement? {
        val size = spec.gridSize
        repeat(MAX_PLACEMENT_TRIES) {
            val (stepRow, stepCol) = spec.directions[random.nextInt(spec.directions.size)]
            val start = Cell(random.nextInt(size), random.nextInt(size))
            val cells = word.indices.map { Cell(start.row + stepRow * it, start.col + stepCol * it) }
            val fits = cells.withIndex().all { (i, cell) ->
                inBounds(cell.row, cell.col, size) && board[cell.row][cell.col].let { it == EMPTY || it == word[i] }
            }
            if (fits) {
                cells.forEachIndexed { i, cell -> board[cell.row][cell.col] = word[i] }
                return Placement(word, cells)
            }
        }
        return null
    }

    // --- ResumableGame -------------------------------------------------------

    override fun saveState(): String? {
        if (isComplete || found.isEmpty()) return null
        val placed = placements.joinToString(";") { p ->
            listOf(p.word, p.cells.first().row, p.cells.first().col, p.cells.last().row, p.cells.last().col).joinToString(":")
        }
        return listOf(VERSION, level, grid.joinToString(","), placed, found.joinToString(","), score).joinToString("|")
    }

    override fun restoreState(state: String): Boolean {
        val parts = state.split("|")
        if (parts.size != 6 || parts[0] != VERSION) return false
        val savedLevel = parts[1].toIntOrNull()?.takeIf { it >= 1 } ?: return false
        val savedGrid = parts[2].split(",")
        val size = savedGrid.size
        if (size < 2 || savedGrid.any { row -> row.length != size || row.any { it !in 'a'..'z' } }) return false

        val savedPlacements = parts[3].split(";").map { text ->
            val fields = text.split(":")
            if (fields.size != 5) return false
            val numbers = fields.drop(1).map { it.toIntOrNull() ?: return false }
            if (numbers.any { it !in 0 until size }) return false
            val cells = cellsBetween(Cell(numbers[0], numbers[1]), Cell(numbers[2], numbers[3]))
            // The placement must really spell its word in the saved grid.
            if (cells.map { savedGrid[it.row][it.col] }.joinToString("") != fields[0]) return false
            Placement(fields[0], cells)
        }
        val savedFound = parts[4].split(",").filter { it.isNotEmpty() }.toSet()
        val savedScore = parts[5].toIntOrNull()?.takeIf { it >= 0 } ?: return false
        val names = savedPlacements.map { it.word }.toSet()
        if (savedPlacements.isEmpty() || !names.containsAll(savedFound) || savedFound.size >= names.size) return false

        level = savedLevel
        gridSize = size
        grid = savedGrid
        placements = savedPlacements
        found = savedFound
        score = savedScore
        isComplete = false
        return true
    }

    class Spec(val gridSize: Int, val wordCount: Int, val maxWordLength: Int, val directions: List<Pair<Int, Int>>)

    companion object {
        const val ID = "wordsearch"
        const val POINTS_PER_LETTER = 10
        private const val VERSION = "1"
        private const val EMPTY = ' '
        private const val MAX_BOARDS = 200
        private const val MAX_PLACEMENT_TRIES = 200

        // Directions as (row step, column step).
        private val STRAIGHT = listOf(0 to 1, 1 to 0)
        private val DIAGONAL = listOf(1 to 1, -1 to 1)
        private val BACKWARDS = listOf(0 to -1, -1 to 0, -1 to -1, 1 to -1)

        fun specFor(level: Int): Spec = when {
            level <= 2 -> Spec(gridSize = 7, wordCount = 5, maxWordLength = 6, directions = STRAIGHT)
            level <= 5 -> Spec(gridSize = 8, wordCount = 6, maxWordLength = 7, directions = STRAIGHT + DIAGONAL)
            level <= 9 -> Spec(gridSize = 9, wordCount = 8, maxWordLength = 8, directions = STRAIGHT + DIAGONAL + BACKWARDS.take(2))
            else -> Spec(gridSize = 10, wordCount = 10, maxWordLength = 8, directions = STRAIGHT + DIAGONAL + BACKWARDS)
        }
    }
}

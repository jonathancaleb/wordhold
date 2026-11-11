package com.wordhold.app.games.crossword

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wordhold.app.core.game.HintGame
import com.wordhold.app.core.game.LeveledGame
import com.wordhold.app.core.game.ResumableGame
import com.wordhold.app.core.game.WordGame
import kotlin.random.Random

/** One answer in the grid. [cells] are grid indices (row * cols + col), first letter first. */
class CrosswordWord(
    val number: Int,
    val across: Boolean,
    val answer: String,
    val clue: String,
    val cells: List<Int>,
) {
    val label: String get() = "$number ${if (across) "Across" else "Down"}"
}

/** A finished layout. [solution] has a letter for every grid cell that is part of a word, null elsewhere. */
class CrosswordPuzzle(
    val rows: Int,
    val cols: Int,
    val words: List<CrosswordWord>,
    val solution: List<Char?>,
)

/**
 * A crossword whose size grows with the level. Every level is a fixed puzzle: the
 * same level always produces the same grid, so a saved game only needs the letters
 * typed so far.
 *
 * A word scores 10 points per letter when all its letters are right; a hint reveals
 * one letter and costs 5. A fully typed word that is wrong shows as wrong until edited.
 */
class CrosswordGame(private val clues: Map<String, String>) : WordGame, LeveledGame, ResumableGame, HintGame {
    override val id = ID
    override val title = "Crossword"
    override var level = 1

    override var score by mutableIntStateOf(0)
        private set
    override var isComplete by mutableStateOf(false)
        private set

    var puzzle by mutableStateOf(CrosswordPuzzle(0, 0, emptyList(), emptyList()))
        private set

    /** The letter typed into each grid cell, null where empty or not a letter cell. */
    var entries by mutableStateOf<List<Char?>>(emptyList())
        private set

    /** Indices into `puzzle.words` of the words solved so far. */
    var solved by mutableStateOf<Set<Int>>(emptySet())
        private set
    var selectedCell by mutableStateOf<Int?>(null)
        private set
    var selectedWord by mutableStateOf<Int?>(null)
        private set

    val currentWord: CrosswordWord? get() = selectedWord?.let { puzzle.words[it] }

    override fun start() {
        puzzle = CrosswordGenerator.generate(clues, level)
        entries = List(puzzle.rows * puzzle.cols) { null }
        solved = emptySet()
        score = 0
        isComplete = false
        selectWord(0)
    }

    /** Selects the cell. Tapping the selected cell again switches between its across and down words. */
    fun tapCell(cell: Int) {
        val wordsHere = puzzle.words.indices.filter { cell in puzzle.words[it].cells }
        if (wordsHere.isEmpty()) return
        selectedWord = when {
            cell == selectedCell && wordsHere.size > 1 -> wordsHere.first { it != selectedWord }
            selectedWord in wordsHere -> selectedWord
            else -> wordsHere.firstOrNull { it !in solved } ?: wordsHere.first()
        }
        selectedCell = cell
    }

    /** Moves to the next unsolved word after the current one, wrapping round. */
    fun nextWord() {
        val count = puzzle.words.size
        val from = selectedWord ?: -1
        val next = (1..count).map { (from + it).mod(count) }.firstOrNull { it !in solved } ?: return
        selectWord(next)
    }

    /** Types [letter] into the selected cell and moves on to the word's next open cell. */
    fun type(letter: Char) {
        val cell = selectedCell ?: return
        val wordIndex = selectedWord ?: return
        if (isComplete || !letter.isLetter()) return
        if (!isLocked(cell)) setEntry(cell, letter.lowercaseChar())
        evaluate()
        if (wordIndex in solved) {
            if (!isComplete) nextWord()
        } else {
            val cells = puzzle.words[wordIndex].cells
            val after = cells.drop(cells.indexOf(cell) + 1)
            selectedCell = after.firstOrNull { entries[it] == null } ?: after.firstOrNull { !isLocked(it) } ?: cell
        }
    }

    /** Clears the selected cell, or steps back and clears the one before it if already empty. */
    fun backspace() {
        val cell = selectedCell ?: return
        val word = currentWord ?: return
        if (isComplete) return
        if (entries[cell] != null && !isLocked(cell)) {
            setEntry(cell, null)
            return
        }
        val previous = word.cells.take(word.cells.indexOf(cell)).lastOrNull { !isLocked(it) } ?: return
        setEntry(previous, null)
        selectedCell = previous
    }

    /** Reveals the selected cell's letter for a small cost. Returns false if there was nothing to reveal. */
    override fun hint(): Boolean {
        val cell = selectedCell ?: return false
        if (isComplete || isLocked(cell) || entries[cell] == puzzle.solution[cell]) return false
        setEntry(cell, puzzle.solution[cell])
        score = maxOf(0, score - HINT_COST)
        evaluate()
        if (selectedWord in solved && !isComplete) nextWord()
        return true
    }

    /** Enters [input] as the whole selected word and says whether that solved it. */
    override fun checkAnswer(input: String): Boolean {
        val wordIndex = selectedWord ?: return false
        val word = puzzle.words[wordIndex]
        val guess = input.trim().lowercase()
        if (isComplete || wordIndex in solved || guess.length != word.cells.size) return false
        word.cells.forEachIndexed { i, cell -> if (!isLocked(cell)) setEntry(cell, guess[i]) }
        evaluate()
        return wordIndex in solved
    }

    /** A cell is locked once any word through it is solved. */
    fun isLocked(cell: Int): Boolean = solved.any { cell in puzzle.words[it].cells }

    /** True when every cell of the word is filled but it is not right. */
    fun isWrong(wordIndex: Int): Boolean =
        wordIndex !in solved && puzzle.words[wordIndex].cells.all { entries[it] != null }

    private fun selectWord(index: Int) {
        val word = puzzle.words.getOrNull(index) ?: return
        selectedWord = index
        selectedCell = word.cells.firstOrNull { entries[it] == null } ?: word.cells.first()
    }

    private fun setEntry(cell: Int, letter: Char?) {
        entries = entries.toMutableList().also { it[cell] = letter }
    }

    /** Marks as solved every unsolved word whose cells are now all correct. */
    private fun evaluate() {
        val newlySolved = puzzle.words.indices.filter { index ->
            index !in solved && puzzle.words[index].cells.all { entries[it] == puzzle.solution[it] }
        }
        if (newlySolved.isEmpty()) return
        solved = solved + newlySolved
        score += newlySolved.sumOf { puzzle.words[it].answer.length } * POINTS_PER_LETTER
        isComplete = solved.size == puzzle.words.size
    }

    // --- ResumableGame -------------------------------------------------------

    override fun saveState(): String? {
        if (isComplete || entries.all { it == null }) return null
        val typed = entries.joinToString("") { (it ?: EMPTY).toString() }
        return listOf(VERSION, level, typed, score).joinToString("|")
    }

    override fun restoreState(state: String): Boolean {
        val parts = state.split("|")
        if (parts.size != 4 || parts[0] != VERSION) return false
        val savedLevel = parts[1].toIntOrNull()?.takeIf { it >= 1 } ?: return false
        val savedScore = parts[3].toIntOrNull() ?: return false
        val candidate = CrosswordGenerator.generate(clues, savedLevel)
        val typed = parts[2]
        // Letters may only sit on letter cells; anything else means the puzzle has changed.
        val fits = typed.length == candidate.solution.size && typed.indices.all { i ->
            typed[i] == EMPTY || (typed[i] in 'a'..'z' && candidate.solution[i] != null)
        }
        if (!fits) return false

        level = savedLevel
        puzzle = candidate
        entries = typed.map { if (it == EMPTY) null else it }
        solved = puzzle.words.indices.filter { index ->
            puzzle.words[index].cells.all { entries[it] == puzzle.solution[it] }
        }.toSet()
        score = savedScore
        isComplete = false
        selectWord(puzzle.words.indices.firstOrNull { it !in solved } ?: 0)
        return true
    }

    companion object {
        const val ID = "crossword"
        const val POINTS_PER_LETTER = 10
        const val HINT_COST = 5
        private const val VERSION = "1"
        private const val EMPTY = '.'
    }
}

/** Lays out interlocking words on a grid, bigger and fuller as the level rises. */
internal object CrosswordGenerator {
    class Spec(val maxSize: Int, val wordCount: Int, val lengths: IntRange)

    fun specFor(level: Int): Spec = when {
        level <= 2 -> Spec(maxSize = 7, wordCount = 4, lengths = 3..5)
        level <= 5 -> Spec(maxSize = 8, wordCount = 6, lengths = 3..6)
        level <= 9 -> Spec(maxSize = 9, wordCount = 8, lengths = 4..7)
        else -> Spec(maxSize = 10, wordCount = 10, lengths = 4..8)
    }

    private class Placed(val answer: String, val row: Int, val col: Int, val across: Boolean) {
        fun cell(i: Int) = if (across) row to col + i else row + i to col
    }

    /** Working board on unbounded coordinates; it is shifted to start at 0,0 once finished. */
    private class Board(val maxSize: Int) {
        val letters = HashMap<Pair<Int, Int>, Char>()
        val acrossCells = HashSet<Pair<Int, Int>>()
        val downCells = HashSet<Pair<Int, Int>>()
        val placed = ArrayList<Placed>()

        /** Number of crossings if [word] may legally go here, or -1 if it may not. */
        fun crossings(word: Placed): Int {
            val n = word.answer.length
            val (dr, dc) = if (word.across) 0 to 1 else 1 to 0
            // The cells just before and after the word must be empty.
            if ((word.row - dr to word.col - dc) in letters) return -1
            if ((word.row + dr * n to word.col + dc * n) in letters) return -1
            val sameDirection = if (word.across) acrossCells else downCells
            var crossings = 0
            for (i in 0 until n) {
                val cell = word.cell(i)
                val existing = letters[cell]
                if (existing != null) {
                    if (existing != word.answer[i] || cell in sameDirection) return -1
                    crossings++
                } else {
                    // A new letter must not touch a neighbour sideways, or it would spell something unintended.
                    val (r, c) = cell
                    if ((r - dc to c - dr) in letters || (r + dc to c + dr) in letters) return -1
                }
            }
            if (placed.isNotEmpty() && (crossings == 0 || crossings == n)) return -1

            val cells = letters.keys + (0 until n).map { word.cell(it) }
            val height = cells.maxOf { it.first } - cells.minOf { it.first } + 1
            val width = cells.maxOf { it.second } - cells.minOf { it.second } + 1
            return if (height <= maxSize && width <= maxSize) crossings else -1
        }

        fun place(word: Placed) {
            for (i in word.answer.indices) {
                val cell = word.cell(i)
                letters[cell] = word.answer[i]
                (if (word.across) acrossCells else downCells) += cell
            }
            placed += word
        }
    }

    fun generate(clues: Map<String, String>, level: Int): CrosswordPuzzle {
        val spec = specFor(level)
        val pool = clues.keys.filter { it.length in spec.lengths }.sorted()
        require(pool.isNotEmpty()) { "No crossword answers of length ${spec.lengths}" }

        var best: Board? = null
        for (attempt in 0 until ATTEMPTS) {
            val board = build(pool, spec, Random(level * 1_000 + attempt))
            if (best == null || board.placed.size > best.placed.size) best = board
            if (board.placed.size >= spec.wordCount) break
        }
        return finish(best!!, clues)
    }

    private fun build(pool: List<String>, spec: Spec, random: Random): Board {
        val board = Board(spec.maxSize)
        val candidates = pool.shuffled(random).take(CANDIDATES).toMutableList()
        board.place(Placed(candidates.removeAt(0), 0, 0, across = true))

        // Two passes: a word with nowhere to go at first may fit once more letters are down.
        repeat(2) {
            val iterator = candidates.iterator()
            while (iterator.hasNext() && board.placed.size < spec.wordCount) {
                val answer = iterator.next()
                val options = ArrayList<Pair<Placed, Int>>()
                for ((cell, letter) in board.letters.entries.sortedWith(compareBy({ it.key.first }, { it.key.second }))) {
                    for (i in answer.indices) {
                        if (answer[i] != letter) continue
                        for (across in listOf(true, false)) {
                            val word = if (across) Placed(answer, cell.first, cell.second - i, true)
                            else Placed(answer, cell.first - i, cell.second, false)
                            val crossings = board.crossings(word)
                            if (crossings > 0) options += word to crossings
                        }
                    }
                }
                if (options.isEmpty()) continue
                // Prefer the placement that interlocks most; break ties at random.
                val most = options.maxOf { it.second }
                board.place(options.filter { it.second == most }.random(random).first)
                iterator.remove()
            }
        }
        return board
    }

    private fun finish(board: Board, clues: Map<String, String>): CrosswordPuzzle {
        val top = board.letters.keys.minOf { it.first }
        val left = board.letters.keys.minOf { it.second }
        val rows = board.letters.keys.maxOf { it.first } - top + 1
        val cols = board.letters.keys.maxOf { it.second } - left + 1
        fun index(cell: Pair<Int, Int>) = (cell.first - top) * cols + (cell.second - left)

        val solution = MutableList<Char?>(rows * cols) { null }
        board.letters.forEach { (cell, letter) -> solution[index(cell)] = letter }

        // Standard numbering: cells that start a word are numbered in reading order,
        // and an across and a down word starting on the same cell share the number.
        val numbers = HashMap<Int, Int>()
        board.placed.map { index(it.cell(0)) }.distinct().sorted().forEachIndexed { i, cell -> numbers[cell] = i + 1 }

        val words = board.placed
            .map { word ->
                val cells = word.answer.indices.map { index(word.cell(it)) }
                CrosswordWord(numbers.getValue(cells.first()), word.across, word.answer, clues.getValue(word.answer), cells)
            }
            .sortedWith(compareBy({ !it.across }, { it.number }))
        return CrosswordPuzzle(rows, cols, words, solution)
    }

    private const val ATTEMPTS = 40
    private const val CANDIDATES = 40
}

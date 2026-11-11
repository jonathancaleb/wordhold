package com.wordhold.app.games.unscramble

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wordhold.app.core.game.LeveledGame
import com.wordhold.app.core.game.ResumableGame
import com.wordhold.app.core.game.WordGame
import com.wordhold.app.core.words.WordList
import kotlin.random.Random

/**
 * A round of scrambled words. Each word's letters are shown shuffled; any real word
 * that uses all of them is accepted. Solving scores 10 points per letter. A limited
 * number of words may be skipped for no score, and the round is complete after the
 * last word.
 *
 * Higher levels use longer words and allow fewer skips.
 */
class UnscrambleGame(
    private val words: WordList,
    private val random: Random = Random.Default,
    private val roundSize: Int = 8,
) : WordGame, LeveledGame, ResumableGame {
    override val id = ID
    override val title = "Unscramble"
    override var level = 1

    override var score by mutableIntStateOf(0)
        private set
    override var isComplete by mutableStateOf(false)
        private set

    /** The current word's letters in scrambled order, lowercase. */
    var letters by mutableStateOf("")
        private set

    /** 1-based position in the round. */
    var wordNumber by mutableIntStateOf(1)
        private set
    var wordCount by mutableIntStateOf(0)
        private set
    var skipsLeft by mutableIntStateOf(0)
        private set

    /** The word just solved or skipped, shown so a skipped answer is not left a mystery. */
    var previousWord by mutableStateOf<String?>(null)
        private set

    private var round: List<String> = emptyList()
    private val target: String get() = round[wordNumber - 1]

    override fun start() {
        val lengths = wordLengths(level)
        round = words.wordsOfLength(lengths).shuffled(random).take(roundSize)
        check(round.isNotEmpty()) { "Word list has no words of length $lengths" }
        wordCount = round.size
        wordNumber = 1
        skipsLeft = skips(level)
        score = 0
        previousWord = null
        isComplete = false
        letters = scramble(target, avoid = "")
    }

    override fun checkAnswer(input: String): Boolean {
        if (isComplete) return false
        val guess = input.trim().lowercase()
        val usesAllLetters = guess.length == target.length && guess.sorted() == target.sorted()
        if (!usesAllLetters || !words.isWord(guess)) return false
        score += guess.length * POINTS_PER_LETTER
        advance(shown = guess)
        return true
    }

    /** Gives up on the current word and moves on without scoring, if any skips remain. */
    fun skip() {
        if (isComplete || skipsLeft == 0) return
        skipsLeft--
        advance(shown = target)
    }

    /** Rearranges the current word's letters. */
    fun shuffle() {
        if (!isComplete) letters = scramble(target, avoid = letters)
    }

    private fun advance(shown: String) {
        previousWord = shown
        if (wordNumber == round.size) {
            isComplete = true
        } else {
            wordNumber++
            letters = scramble(target, avoid = "")
        }
    }

    /** Shuffles until the result is not itself a word (so never the answer) and differs from [avoid]. */
    private fun scramble(word: String, avoid: String): String {
        var candidate = word
        repeat(30) {
            candidate = word.toList().shuffled(random).joinToString("")
            if (candidate != avoid && !words.isWord(candidate)) return candidate
        }
        return candidate
    }

    private fun String.sorted(): String = toCharArray().sorted().joinToString("")

    // --- ResumableGame -------------------------------------------------------

    override fun saveState(): String? {
        // Nothing to keep until the first word is behind the player.
        if (isComplete || wordNumber == 1) return null
        return listOf(
            VERSION, level, round.joinToString(","), wordNumber, skipsLeft, score, letters, previousWord.orEmpty(),
        ).joinToString("|")
    }

    override fun restoreState(state: String): Boolean {
        val parts = state.split("|")
        if (parts.size != 8 || parts[0] != VERSION) return false
        val savedLevel = parts[1].toIntOrNull()?.takeIf { it >= 1 } ?: return false
        val savedRound = parts[2].split(",")
        if (savedRound.any { word -> word.isEmpty() || word.any { it !in 'a'..'z' } }) return false
        val savedNumber = parts[3].toIntOrNull()?.takeIf { it in 1..savedRound.size } ?: return false
        val savedSkips = parts[4].toIntOrNull()?.takeIf { it >= 0 } ?: return false
        val savedScore = parts[5].toIntOrNull()?.takeIf { it >= 0 } ?: return false
        val savedLetters = parts[6]
        if (savedLetters.sorted() != savedRound[savedNumber - 1].sorted()) return false

        level = savedLevel
        round = savedRound
        wordCount = savedRound.size
        wordNumber = savedNumber
        skipsLeft = savedSkips
        score = savedScore
        letters = savedLetters
        previousWord = parts[7].ifEmpty { null }
        isComplete = false
        return true
    }

    companion object {
        const val ID = "unscramble"
        const val POINTS_PER_LETTER = 10
        private const val VERSION = "1"

        fun wordLengths(level: Int): IntRange = when {
            level <= 2 -> 4..5
            level <= 5 -> 5..6
            level <= 9 -> 6..7
            else -> 7..8
        }

        /** Always at least one, so a single impossible word cannot end a run. */
        fun skips(level: Int): Int = when {
            level <= 2 -> 3
            level <= 6 -> 2
            else -> 1
        }
    }
}

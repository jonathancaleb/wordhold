package com.wordhold.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.wordhold.app.R

/**
 * The colour tokens of the design spec. The values live in `res/values/colors.xml`,
 * the single source also used by the window theme. The app has this one light
 * appearance; it ignores system dark mode.
 *
 * Text is [ink] on every fill except [tileAbsent] and ink buttons, where it is [white].
 *
 * How the games use them, so the same thing looks the same everywhere:
 *  - selected cell: [butter] with an ink outline
 *  - related to the selection (its word, its row): [periwinkle]
 *  - solved, found or correct: [tileCorrect]
 *  - in progress (a drag, a letter in the wrong place): [tilePresent]
 *  - wrong: [tileAbsent], with white text
 */
@Immutable
data class WordholdColors(
    val ink: Color,
    val sage: Color,
    val periwinkle: Color,
    val butter: Color,
    val white: Color,
    val tileCorrect: Color,
    val tilePresent: Color,
    val tileAbsent: Color,
    val emptyOutline: Color,
    val mutedOutline: Color,
) {
    /** The text colour that is legible on [fill]. */
    fun on(fill: Color): Color = if (fill == tileAbsent || fill == ink) white else ink
}

/** The colour a game is shown in: its catalogue card and the background of its screen. */
enum class GameTone { White, Butter, Periwinkle }

/** The card and screen-background colour of [tone]. */
fun WordholdColors.tone(tone: GameTone): Color = when (tone) {
    GameTone.White -> white
    GameTone.Butter -> butter
    GameTone.Periwinkle -> periwinkle
}

/** A colour for [tone] that stands out on a white row: white-toned games borrow the green. */
fun WordholdColors.badge(tone: GameTone): Color = if (tone == GameTone.White) tileCorrect else tone(tone)

/** Outline widths. Every card, tile, pill and key has one; only the page background does not. */
object Stroke {
    /** Cards, pills, the bottom bar's top edge. */
    val large = 3.dp

    /** Filled and typed tiles, small chips. */
    val small = 2.5.dp

    /** Keys, and the cells of a dense grid. */
    val key = 2.dp

    /** Empty tiles, drawn in `emptyOutline`. */
    val empty = 2.dp

    /** Lines inside a dense grid. */
    val hairline = 1.dp

    /** The hand-drawn loop behind the daily hero title. */
    val scribble = 5.dp
}

object Radius {
    /** Hero cards, boards. */
    val xl = 28.dp

    /** Game cards, stat cards. */
    val l = 24.dp

    /** Mode cards, dense boards. */
    val m = 20.dp

    /** List rows. */
    val s = 18.dp
    val key = 10.dp
    val tile = 8.dp

    /** Cells of a dense grid, and the tiny tiles of a card motif. */
    val cell = 4.dp
}

/** Corner shapes, one per [Radius], plus the pill. */
@Immutable
class WordholdShapes {
    val xl = RoundedCornerShape(Radius.xl)
    val l = RoundedCornerShape(Radius.l)
    val m = RoundedCornerShape(Radius.m)
    val s = RoundedCornerShape(Radius.s)
    val key = RoundedCornerShape(Radius.key)
    val tile = RoundedCornerShape(Radius.tile)
    val cell = RoundedCornerShape(Radius.cell)
    val day = RoundedCornerShape(Dimens.dayCellRadius)
    val pill = RoundedCornerShape(percent = 50)
}

/** The spacing scale. Layout gaps and paddings are taken from here rather than typed in. */
object Spacing {
    val none = 0.dp
    val xs = 4.dp
    val sm = 8.dp

    /** Gap between the cards of a grid. */
    val grid = 12.dp

    /** Gap between the sections of a screen. */
    val section = 14.dp

    /** Side padding of every screen. */
    val screen = 16.dp
    val lg = 24.dp

    /** Gap between tiles, and between keys in a row. */
    val tile = 6.dp

    /** Gap between rows of keys. */
    val keyRow = 8.dp

    /** Smallest size of anything tappable, keyboard keys excepted. */
    val minTouch = 48.dp
}

/** Fixed component sizes from the spec. */
object Dimens {
    val topBar = 48.dp
    val bottomNav = 80.dp
    val navPillWidth = 64.dp
    val navPillHeight = 32.dp
    val streakPill = 40.dp
    val chip = 36.dp
    val playPillWidth = 76.dp
    val playPillHeight = 52.dp
    val primaryButton = 58.dp
    val key = 52.dp
    val keyWidth = 31.dp
    val keyWideWidth = 48.dp
    val gridTile = 54.dp
    val introTile = 46.dp
    val stepCircle = 36.dp
    val stepTile = 34.dp
    val motifTile = 28.dp
    val motifCell = 17.dp
    val rowBadge = 38.dp
    val listRow = 56.dp
    val dayCell = 26.dp
    val dayCellRadius = 7.dp
    val gameCard = 156.dp
    val modeCard = 68.dp
    val iconLarge = 26.dp
    val iconNav = 22.dp
    val iconSmall = 16.dp
}

private val Fraunces = FontFamily(Font(R.font.fraunces_black, FontWeight.Black))
private val Figtree = FontFamily(
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
)

/**
 * The text styles of the spec: Fraunces 900 for display, Figtree 600 and 700 for the
 * rest. Both are bundled TTFs. Screens pick a style and set its colour; they do not
 * override sizes. For text that must fit a box, use [fittedTextStyle].
 */
@Immutable
class WordholdTypography {
    private val displayBase = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Black)
    private val bodyBase = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold)
    private val bold = bodyBase.copy(fontWeight = FontWeight.Bold)

    /** Stat numbers. */
    val display = displayBase.copy(fontSize = 52.sp, lineHeight = 55.sp, letterSpacing = (-0.02).em)

    /** The game's name on its intro. */
    val heroTitle = displayBase.copy(fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-0.02).em)

    /** Wordmark, screen titles, the daily hero title. */
    val title = displayBase.copy(fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em)
    val topBarTitle = displayBase.copy(fontSize = 24.sp, lineHeight = 27.sp, letterSpacing = (-0.01).em)

    /** Game card titles, section headings. */
    val cardTitle = displayBase.copy(fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em)

    /** Headings inside progress cards. */
    val sectionTitle = displayBase.copy(fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em)

    /** The number in a numbered step and the letter in a row badge. */
    val badge = displayBase.copy(fontSize = 17.sp, lineHeight = 20.sp)

    val buttonLarge = bold.copy(fontSize = 18.sp, lineHeight = 22.sp)
    val button = bold.copy(fontSize = 17.sp, lineHeight = 22.sp)

    /** One-line description under a hero title. */
    val lead = bodyBase.copy(fontSize = 17.sp, lineHeight = 23.sp)

    /** Names in rows and mode cards. */
    val label = bold.copy(fontSize = 16.sp, lineHeight = 20.sp)
    val body = bodyBase.copy(fontSize = 15.sp, lineHeight = 20.sp)
    val chip = bold.copy(fontSize = 14.sp, lineHeight = 18.sp)
    val caption = bodyBase.copy(fontSize = 13.sp, lineHeight = 17.sp)

    /** Small uppercase label. Pass the text already uppercased. */
    val overline = bold.copy(fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.06.em)
    val navLabel = bodyBase.copy(fontSize = 12.sp, lineHeight = 15.sp)
    val navLabelActive = bold.copy(fontSize = 12.sp, lineHeight = 15.sp)
    val tileLetter = bold.copy(fontSize = 26.sp, lineHeight = 30.sp)
    val keyLetter = bold.copy(fontSize = 16.sp, lineHeight = 20.sp)
    val keyWord = bold.copy(fontSize = 13.sp, lineHeight = 16.sp)
}

/**
 * A style for a glyph that has to fit inside a box of side [box]: tile letters, grid
 * digits. Its size is a [fraction] of the box and deliberately does not grow with the
 * system font size, because the box cannot grow with it.
 */
@Composable
fun fittedTextStyle(box: Dp, fraction: Float = 0.48f): TextStyle {
    val size = with(LocalDensity.current) { (box * fraction).toSp() }
    return WordholdTheme.type.tileLetter.copy(fontSize = size, lineHeight = size * 1.2f)
}

private val LocalColors = staticCompositionLocalOf<WordholdColors> { error("WordholdTheme is missing") }
private val LocalTypography = staticCompositionLocalOf { WordholdTypography() }
private val LocalShapes = staticCompositionLocalOf { WordholdShapes() }

/** The background colour of the screen being drawn, for components that must contrast with it. */
val LocalScreenBackground = staticCompositionLocalOf { Color.Unspecified }

object WordholdTheme {
    val colors: WordholdColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val type: WordholdTypography
        @Composable @ReadOnlyComposable get() = LocalTypography.current
    val shapes: WordholdShapes
        @Composable @ReadOnlyComposable get() = LocalShapes.current
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordholdTheme(content: @Composable () -> Unit) {
    val colors = WordholdColors(
        ink = colorResource(R.color.wh_ink),
        sage = colorResource(R.color.wh_sage),
        periwinkle = colorResource(R.color.wh_periwinkle),
        butter = colorResource(R.color.wh_butter),
        white = colorResource(R.color.wh_white),
        tileCorrect = colorResource(R.color.wh_tile_correct),
        tilePresent = colorResource(R.color.wh_tile_present),
        tileAbsent = colorResource(R.color.wh_tile_absent),
        emptyOutline = colorResource(R.color.wh_empty_outline),
        mutedOutline = colorResource(R.color.wh_muted_outline),
    )
    val type = WordholdTypography()

    // Material3 stays underneath as a base, mapped onto the same palette so any
    // Material component that does get used still matches.
    val scheme = lightColorScheme(
        primary = colors.ink, onPrimary = colors.white,
        secondary = colors.butter, onSecondary = colors.ink,
        tertiary = colors.periwinkle, onTertiary = colors.ink,
        background = colors.sage, onBackground = colors.ink,
        surface = colors.white, onSurface = colors.ink,
        onSurfaceVariant = colors.ink,
        outline = colors.ink, outlineVariant = colors.mutedOutline,
        error = colors.tileAbsent, onError = colors.white,
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(bodyLarge = type.lead, bodyMedium = type.body, bodySmall = type.caption),
    ) {
        CompositionLocalProvider(
            LocalColors provides colors,
            LocalTypography provides type,
            LocalScreenBackground provides colors.sage,
            // No Material ripple anywhere; taps use Modifier.pressable feedback instead.
            LocalRippleConfiguration provides null,
            content = content,
        )
    }
}

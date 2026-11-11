package com.wordhold.app.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.wordhold.app.core.ui.theme.Dimens
import com.wordhold.app.core.ui.theme.LocalScreenBackground
import com.wordhold.app.core.ui.theme.Spacing
import com.wordhold.app.core.ui.theme.Stroke
import com.wordhold.app.core.ui.theme.WordholdColors
import com.wordhold.app.core.ui.theme.WordholdTheme
import com.wordhold.app.core.ui.theme.fittedTextStyle

/**
 * Tap feedback: the element dims (and optionally shrinks) while pressed, then eases
 * back. Replaces the Material ripple everywhere in the app.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedAlpha: Float = 0.7f,
    pressedScale: Float = 1f,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Snap in on press, ease out on release.
    val release = tween<Float>(durationMillis = if (pressed) 0 else 220)
    val alpha by animateFloatAsState(if (pressed) pressedAlpha else 1f, release, label = "pressAlpha")
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, release, label = "pressScale")
    this
        .graphicsLayer {
            this.alpha = if (enabled) alpha else 0.35f
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        )
}

/** The look of every card, tile, pill and key: a flat [fill] inside an outline. */
fun Modifier.outlined(fill: Color, outline: Color, shape: Shape, stroke: Dp = Stroke.large): Modifier =
    background(fill, shape).border(stroke, outline, shape)

/** An outlined card. The default is a white game card; pass [fill] and [shape] for the others. */
@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    fill: Color = WordholdTheme.colors.white,
    shape: Shape = WordholdTheme.shapes.l,
    padding: PaddingValues = PaddingValues(Spacing.section),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.outlined(fill, WordholdTheme.colors.ink, shape).padding(padding),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

/** The white card a game's board sits in. */
@Composable
fun BoardCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(Spacing.screen),
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        modifier = modifier,
        shape = WordholdTheme.shapes.xl,
        padding = padding,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/**
 * The ink "Play" pill of a hero card. With a null [onClick] it is only drawn, for a
 * card that is itself the button.
 */
@Composable
fun PillButton(label: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = WordholdTheme.colors
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressable(pressedScale = 0.96f, onClick = onClick) else Modifier)
            .defaultMinSize(minWidth = Dimens.playPillWidth, minHeight = Dimens.playPillHeight)
            .background(colors.ink, WordholdTheme.shapes.pill)
            .padding(horizontal = Spacing.screen),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = WordholdTheme.type.button.copy(color = colors.white), maxLines = 1)
    }
}

/** Full-width ink pill for a screen's main action. Grows taller if the text needs it. */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = WordholdTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressable(enabled = enabled, pressedScale = 0.98f, onClick = onClick)
            .defaultMinSize(minHeight = Dimens.primaryButton)
            .background(colors.ink, WordholdTheme.shapes.pill)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = WordholdTheme.type.buttonLarge.copy(color = colors.white, textAlign = TextAlign.Center))
    }
}

/** Outlined pill for secondary actions: Shuffle, Skip, Cancel. An ink [fill] makes it the confirming one. */
@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = WordholdTheme.colors.white,
    enabled: Boolean = true,
) {
    val colors = WordholdTheme.colors
    Box(
        modifier = modifier
            .pressable(enabled = enabled, pressedScale = 0.97f, onClick = onClick)
            .defaultMinSize(minWidth = Spacing.minTouch, minHeight = Spacing.minTouch)
            .outlined(fill, colors.ink, WordholdTheme.shapes.pill)
            .padding(horizontal = Spacing.screen, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = WordholdTheme.type.chip.copy(color = colors.on(fill), textAlign = TextAlign.Center))
    }
}

/** A 48 x 48 icon-only button. [description] is what a screen reader says. */
@Composable
fun IconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Spacing.minTouch)
            .pressable(pressedAlpha = 0.5f, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null)
    }
}

/**
 * The bar on top of every pushed screen: back button, a centred title over an optional
 * caption line, and room for one trailing icon button. It draws no background of its
 * own, so the screen's colour runs to the top edge.
 */
@Composable
fun TopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = WordholdTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Spacing.grid)
            .defaultMinSize(minHeight = Dimens.topBar),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(WordholdIcons.Back, "Back", onBack)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                text = title,
                style = WordholdTheme.type.topBarTitle.copy(color = colors.ink, textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (caption != null) {
                BasicText(
                    text = caption,
                    style = WordholdTheme.type.caption.copy(color = colors.ink, textAlign = TextAlign.Center),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(Modifier.size(Spacing.minTouch), contentAlignment = Alignment.Center) { trailing?.invoke() }
    }
}

/** The title of a root screen, or of a section when given a smaller [style]. */
@Composable
fun Heading(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = WordholdTheme.type.cardTitle,
) {
    BasicText(text, style = style.copy(color = WordholdTheme.colors.ink), modifier = modifier.semantics { heading() })
}

/**
 * A loose hand-drawn loop around the content, a little larger than it on every side.
 * Used behind the daily hero title.
 */
fun Modifier.scribble(color: Color): Modifier = drawBehind {
    val overhangX = Spacing.grid.toPx()
    val overhangY = Spacing.sm.toPx()
    // The loop is drawn on a 200 x 60 grid and stretched over the content.
    val sx = (size.width + overhangX * 2) / 200f
    val sy = (size.height + overhangY * 2) / 60f
    fun x(v: Float) = v * sx - overhangX
    fun y(v: Float) = v * sy - overhangY
    val loop = Path().apply {
        moveTo(x(8f), y(34f))
        cubicTo(x(4f), y(12f), x(60f), y(4f), x(110f), y(6f))
        cubicTo(x(170f), y(8f), x(198f), y(20f), x(190f), y(36f))
        cubicTo(x(182f), y(52f), x(110f), y(56f), x(60f), y(52f))
        cubicTo(x(26f), y(50f), x(12f), y(44f), x(14f), y(38f))
    }
    drawPath(loop, color, style = DrawStroke(width = Stroke.scribble.toPx(), cap = StrokeCap.Round))
}

/** The white pill showing the current streak. */
@Composable
fun StreakPill(days: Int, modifier: Modifier = Modifier) {
    val colors = WordholdTheme.colors
    val text = if (days == 1) "1 day" else "$days days"
    Row(
        modifier = modifier
            .height(Dimens.streakPill)
            .outlined(colors.white, colors.ink, WordholdTheme.shapes.pill)
            .padding(start = Spacing.grid, end = Spacing.section)
            .clearAndSetSemantics { contentDescription = "Streak: $text" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        FlameIcon()
        BasicText(text, style = WordholdTheme.type.chip.copy(color = colors.ink), maxLines = 1)
    }
}

/** A small outlined pill holding one fact: "Score 120", "Found 3 of 8". */
@Composable
fun PillChip(text: String, modifier: Modifier = Modifier, fill: Color = WordholdTheme.colors.white) {
    val colors = WordholdTheme.colors
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = Dimens.chip)
            .outlined(fill, colors.ink, WordholdTheme.shapes.pill)
            .padding(horizontal = Spacing.screen),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = WordholdTheme.type.chip.copy(color = colors.on(fill)), maxLines = 1)
    }
}

/** Two facts about the game in progress, as a centred pair of chips under the top bar. */
@Composable
fun StatusChips(left: String, right: String, modifier: Modifier = Modifier) {
    val colors = WordholdTheme.colors
    // The second chip is coloured, in whichever tone the screen behind it is not.
    val accent = if (LocalScreenBackground.current == colors.butter) colors.periwinkle else colors.butter
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp, Alignment.CenterHorizontally),
    ) {
        PillChip(left)
        PillChip(right, fill = accent)
    }
}

/** The small uppercase label above a hero title or a clue. */
@Composable
fun CapsLabel(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start) {
    BasicText(
        text = text.uppercase(),
        style = WordholdTheme.type.overline.copy(color = WordholdTheme.colors.ink, textAlign = textAlign),
        modifier = modifier,
    )
}

/**
 * A square letter (or digit) tile. A null [letter] draws an empty slot, outlined in
 * the lighter empty colour unless [fill] says the slot is taken. [tilt] is for
 * decoration only: tiles in a playable grid are never rotated.
 */
@Composable
fun LetterTile(
    letter: Char?,
    size: Dp,
    modifier: Modifier = Modifier,
    fill: Color = WordholdTheme.colors.white,
    tilt: Float = 0f,
    textStyle: TextStyle? = null,
) {
    val colors = WordholdTheme.colors
    val empty = letter == null && fill == colors.white
    Box(
        modifier = modifier
            .size(size)
            .rotate(tilt)
            .outlined(
                fill = fill,
                outline = if (empty) colors.emptyOutline else colors.ink,
                shape = WordholdTheme.shapes.tile,
                stroke = if (empty) Stroke.empty else Stroke.small,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (letter != null) {
            BasicText(letter.uppercase(), style = (textStyle ?: fittedTextStyle(size)).copy(color = colors.on(fill)))
        }
    }
}

/**
 * One key of an in-app keyboard or number pad: [label] as a letter, as a smaller word
 * ("Erase") when [word] is set, or an [icon] with a [description] in place of text.
 * Keys are 52 high and as wide as the caller lays them out.
 */
@Composable
fun KeyCap(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    word: Boolean = false,
    fill: Color = WordholdTheme.colors.white,
    icon: ImageVector? = null,
    description: String = label,
) {
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type
    Box(
        modifier = modifier
            .height(Dimens.key)
            .pressable(enabled = enabled, pressedAlpha = 0.5f, pressedScale = 0.94f, onClick = onClick)
            .outlined(fill, colors.ink, WordholdTheme.shapes.key, Stroke.key)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, size = Dimens.iconNav, tint = colors.on(fill))
        } else {
            BasicText(
                text = label,
                style = (if (word) type.keyWord else type.keyLetter).copy(color = colors.on(fill)),
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

private val KEY_ROWS = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")

/**
 * The in-app letter keyboard, so a board is never covered by the system one. Three
 * centred rows, the last ending with Delete. Keys keep the designed proportions and
 * shrink together on a narrow phone.
 */
@Composable
fun LetterKeyboard(onLetter: (Char) -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal = Spacing.sm)) {
        val gap = Spacing.tile
        val keyWidth = minOf(Dimens.keyWidth * 1.3f, (maxWidth - gap * 9) / 10)
        val wideWidth = keyWidth * (Dimens.keyWideWidth / Dimens.keyWidth)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.keyRow)) {
            KEY_ROWS.forEachIndexed { rowIndex, letters ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                ) {
                    letters.forEach { letter ->
                        KeyCap(letter.toString(), onClick = { onLetter(letter) }, modifier = Modifier.width(keyWidth))
                    }
                    if (rowIndex == KEY_ROWS.lastIndex) {
                        KeyCap(
                            label = "Delete",
                            onClick = onDelete,
                            modifier = Modifier.width(wideWidth),
                            icon = WordholdIcons.Delete,
                        )
                    }
                }
            }
        }
    }
}

/** What a grid cell's contents are, as far as the game has judged them. */
enum class CellStatus { Neutral, Solved, Wrong }

/**
 * The fill of a grid cell, the same in every game: the selected cell is butter, a
 * wrong one is dark, a solved one is green, and whatever is related to the selection
 * is periwinkle. Pair it with [WordholdColors.on] for the text.
 */
fun WordholdColors.cellFill(selected: Boolean, related: Boolean, status: CellStatus): Color = when {
    selected -> butter
    status == CellStatus.Wrong -> tileAbsent
    status == CellStatus.Solved -> tileCorrect
    related -> periwinkle
    else -> white
}

/** One step of a "How it works" list: its number, an example tile, and what the tile means. */
@Composable
fun NumberedStep(number: Int, tile: Char, tileFill: Color, text: String, modifier: Modifier = Modifier) {
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.grid),
    ) {
        Box(
            Modifier.size(Dimens.stepCircle).outlined(colors.white, colors.ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(number.toString(), style = type.badge.copy(color = colors.ink))
        }
        LetterTile(
            letter = tile,
            size = Dimens.stepTile,
            fill = tileFill,
            textStyle = type.label,
            modifier = Modifier.clearAndSetSemantics { },
        )
        BasicText(text, style = type.body.copy(color = colors.ink), modifier = Modifier.weight(1f))
    }
}

/** One big number over its label. */
@Composable
fun StatCard(value: String, label: String, fill: Color, modifier: Modifier = Modifier) {
    val colors = WordholdTheme.colors
    OutlinedCard(
        modifier = modifier.semantics(mergeDescendants = true) { },
        fill = fill,
        padding = PaddingValues(horizontal = Spacing.screen + 2.dp, vertical = Spacing.section),
    ) {
        BasicText(value, style = WordholdTheme.type.display.copy(color = colors.ink), maxLines = 1)
        BasicText(label, style = WordholdTheme.type.chip.copy(color = colors.ink))
    }
}

/** A small titled card: a bold name over one caption line. */
@Composable
fun ModeCard(title: String, caption: String, fill: Color, modifier: Modifier = Modifier) {
    val colors = WordholdTheme.colors
    OutlinedCard(
        modifier = modifier.defaultMinSize(minHeight = Dimens.modeCard).semantics(mergeDescendants = true) { },
        fill = fill,
        shape = WordholdTheme.shapes.m,
        padding = PaddingValues(horizontal = Spacing.section, vertical = Spacing.sm + 2.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(title, style = WordholdTheme.type.label.copy(color = colors.ink))
        BasicText(caption, style = WordholdTheme.type.caption.copy(color = colors.ink))
    }
}

/**
 * One outlined row of a list. [badge] puts a letter in a coloured circle at the start,
 * [value] sits at the end, and with [onClick] the row is tappable and shows a chevron.
 */
@Composable
fun ListRow(
    label: String,
    modifier: Modifier = Modifier,
    badge: Char? = null,
    badgeFill: Color = WordholdTheme.colors.white,
    value: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier.semantics(mergeDescendants = true) { })
            .defaultMinSize(minHeight = Dimens.listRow)
            .outlined(colors.white, colors.ink, WordholdTheme.shapes.s)
            .padding(start = if (badge != null) Spacing.sm else Spacing.screen, end = Spacing.screen, top = Spacing.tile, bottom = Spacing.tile),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.grid),
    ) {
        if (badge != null) {
            Box(
                Modifier
                    .size(Dimens.rowBadge)
                    .outlined(badgeFill, colors.ink, CircleShape, Stroke.small)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(badge.uppercase(), style = type.badge.copy(color = colors.on(badgeFill)))
            }
        }
        BasicText(label, style = type.label.copy(color = colors.ink), modifier = Modifier.weight(1f))
        if (value != null) BasicText(value, style = type.caption.copy(color = colors.ink))
        if (onClick != null) Icon(WordholdIcons.Forward, contentDescription = null, size = Dimens.iconNav)
    }
}

/** One destination of the [BottomNav]. */
class NavItem(val label: String, val icon: ImageVector)

/**
 * The bottom bar: white, with an ink top edge. The active item's icon sits in a butter
 * pill and its label is bold. The bar's white runs on under the system navigation bar.
 */
@Composable
fun BottomNav(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WordholdTheme.colors
    val type = WordholdTheme.type
    Column(modifier.fillMaxWidth().background(colors.white)) {
        Box(Modifier.fillMaxWidth().height(Stroke.large).background(colors.ink))
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .defaultMinSize(minHeight = Dimens.bottomNav - Stroke.large)
                .padding(start = Spacing.grid, end = Spacing.grid, top = Spacing.sm, bottom = Spacing.grid)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val active = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = Spacing.minTouch)
                        .selectable(
                            selected = active,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterVertically),
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.navPillWidth, Dimens.navPillHeight)
                            .then(
                                if (active) Modifier.outlined(colors.butter, colors.ink, WordholdTheme.shapes.pill, Stroke.small)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(item.icon, contentDescription = null, size = Dimens.iconNav)
                    }
                    BasicText(
                        text = item.label,
                        style = (if (active) type.navLabelActive else type.navLabel).copy(color = colors.ink),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** A confirmation dialog: title, message, and Cancel beside the confirming action. */
@Composable
fun Alert(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = WordholdTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        OutlinedCard(
            modifier = Modifier.width(300.dp),
            shape = WordholdTheme.shapes.xl,
            padding = PaddingValues(Spacing.screen + Spacing.xs),
        ) {
            Heading(title)
            BasicText(
                text = message,
                style = WordholdTheme.type.body.copy(color = colors.ink),
                modifier = Modifier.padding(top = Spacing.sm),
            )
            Spacer(Modifier.height(Spacing.screen))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.grid)) {
                SecondaryButton("Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                SecondaryButton(confirmLabel, onClick = onConfirm, modifier = Modifier.weight(1f), fill = colors.ink)
            }
        }
    }
}

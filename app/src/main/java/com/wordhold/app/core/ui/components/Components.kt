package com.wordhold.app.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wordhold.app.core.ui.theme.WordholdTheme

/**
 * iOS-style tap feedback: the element dims (and optionally shrinks) while pressed,
 * then eases back. Replaces the Material ripple everywhere in the app.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedAlpha: Float = 0.45f,
    pressedScale: Float = 1f,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Snap in on press, ease out on release, as UIKit buttons do.
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

/** White (or raised-dark) rounded card with a soft shadow in light mode and none in dark. */
@Composable
fun WordholdCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WordholdTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .shadow(
                elevation = if (colors.isDark) 0.dp else 10.dp,
                shape = shape,
                clip = false,
                ambientColor = colors.shadow,
                spotColor = colors.shadow,
            )
            .background(colors.surface, shape)
            .padding(16.dp),
        content = content,
    )
}

/** Compact capsule button, used for a row's single action ("Play"). */
@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WordholdTheme.colors
    Box(
        modifier = modifier
            .pressable(pressedScale = 0.96f, onClick = onClick)
            .defaultMinSize(minWidth = 76.dp, minHeight = 36.dp)
            .background(colors.fill, RoundedCornerShape(50))
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = WordholdTheme.type.subheadline.copy(color = colors.tint, fontWeight = FontWeight.Bold),
        )
    }
}

/**
 * iOS navigation bar for pushed screens: centred title, text-only back button on the
 * left, hairline underneath. Root screens use a large title instead.
 */
@Composable
fun NavigationBar(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WordholdTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box(Modifier.fillMaxWidth().height(44.dp)) {
            BasicText(
                text = title,
                style = WordholdTheme.type.headline.copy(color = colors.label, textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 96.dp)
                    .semantics { heading() },
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .pressable(onClick = onBack)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = backLabel,
                    style = WordholdTheme.type.body.copy(color = colors.tint),
                    maxLines = 1,
                )
            }
        }
        Hairline()
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(0.5.dp).background(WordholdTheme.colors.separator))
}

/** iOS segmented control: a recessed track with a raised thumb that slides to the selection. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WordholdTheme.colors
    val thumbShape = RoundedCornerShape(8.dp)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(colors.fill, RoundedCornerShape(10.dp))
            .padding(2.dp)
    ) {
        val segmentWidth = maxWidth / options.size
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "segmentThumb",
        )
        Box(
            Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(3.dp, thumbShape, clip = false, ambientColor = colors.shadow, spotColor = colors.shadow)
                .background(colors.segmentThumb, thumbShape)
        )
        Row(Modifier.fillMaxSize().selectableGroup()) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = option,
                        style = WordholdTheme.type.footnote.copy(
                            color = colors.label,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

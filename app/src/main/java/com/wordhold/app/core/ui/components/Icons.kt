package com.wordhold.app.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wordhold.app.core.ui.theme.Dimens
import com.wordhold.app.core.ui.theme.WordholdTheme

/**
 * The app's icons: round-capped line drawings on a 24 x 24 grid, as in the designs.
 * They are drawn in black and tinted where they are shown, so colours stay in the theme.
 */
object WordholdIcons {
    val Back = stroked("Back", 2.6f, "M15 5l-7 7 7 7")
    val Forward = stroked("Forward", 2.6f, "M9 5l7 7-7 7")
    val Help = stroked(
        "Help", 2.4f,
        "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z",
        "M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.7.4-1 .9-1 1.7M12 17h.01",
    )
    val Hint = stroked(
        "Hint", 2.4f,
        "M9 18h6M10 21h4M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z",
    )
    val Settings = stroked(
        "Settings", 2.4f,
        "M4 7h10M18 7h2M4 17h2M10 17h10",
        "M16 5a2 2 0 1 0 0 4a2 2 0 1 0 0-4z",
        "M8 15a2 2 0 1 0 0 4a2 2 0 1 0 0-4z",
    )
    val Delete = stroked("Delete", 2.2f, "M9 6h10a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H9l-6-6z", "M12 10l4 4M16 10l-4 4")

    val Games = stroked(
        "Games", 2.2f,
        roundedSquare(4f, 4f), roundedSquare(13f, 4f), roundedSquare(4f, 13f), roundedSquare(13f, 13f),
    )
    val Daily = stroked(
        "Daily", 2.2f,
        "M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z",
        "M4 10h16M9 3v4M15 3v4",
    )
    val Progress = stroked("Progress", 2.2f, "M5 20V11M12 20V5M19 20v-6")

    private const val FLAME = "M12 3c1 4 5 5.5 5 10a5 5 0 0 1-10 0c0-2 1-3 2-4 .3 1.2 1 2 2 2 0-3-1-5 1-8z"
    val FlameOutline = stroked("Flame", 2.5f, FLAME)
    val FlameFill = filled("FlameFill", FLAME)

    /** A 7 x 7 square with 1.5 corners, as an SVG path. */
    private fun roundedSquare(x: Float, y: Float) =
        "M${x + 1.5f} ${y}h4a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-4a1.5 1.5 0 0 1-1.5-1.5v-4a1.5 1.5 0 0 1 1.5-1.5z"

    private fun stroked(name: String, strokeWidth: Float, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun filled(name: String, path: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
        }.build()
}

/** Draws [icon] in [tint]. A null [contentDescription] marks it as decoration. */
@Composable
fun Icon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.iconLarge,
    tint: Color = WordholdTheme.colors.ink,
) {
    Image(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

/** The streak flame: butter inside an ink outline. Decorative. */
@Composable
fun FlameIcon(modifier: Modifier = Modifier, size: Dp = Dimens.iconSmall) {
    Box(modifier.size(size)) {
        Icon(WordholdIcons.FlameFill, null, size = size, tint = WordholdTheme.colors.butter)
        Icon(WordholdIcons.FlameOutline, null, size = size)
    }
}

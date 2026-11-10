package com.wordhold.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Palette: fountain-pen ink on cool paper, with a highlighter-marker accent.
 * Ink indigo is the tint (links, buttons); marigold marks letters and found words.
 * Names follow iOS semantic colours rather than Material roles.
 */
@Immutable
data class WordholdColors(
    val isDark: Boolean,
    /** Screen background (iOS "grouped background"). */
    val background: Color,
    /** Cards and bars. */
    val surface: Color,
    /** Recessed fills: segmented-control track, pill buttons. */
    val fill: Color,
    val segmentThumb: Color,
    val label: Color,
    val secondaryLabel: Color,
    val separator: Color,
    val tint: Color,
    val onTint: Color,
    val highlight: Color,
    val onHighlight: Color,
    val success: Color,
    val danger: Color,
    val shadow: Color,
)

private val LightColors = WordholdColors(
    isDark = false,
    background = Color(0xFFF1F2F6),
    surface = Color(0xFFFFFFFF),
    fill = Color(0xFFE3E5EC),
    segmentThumb = Color(0xFFFFFFFF),
    label = Color(0xFF171A2B),
    secondaryLabel = Color(0xFF5D6278),
    separator = Color(0xFFD5D8E1),
    tint = Color(0xFF2F3FA3),
    onTint = Color(0xFFFFFFFF),
    highlight = Color(0xFFF6C445),
    onHighlight = Color(0xFF2A2100),
    success = Color(0xFF1F7A5A),
    danger = Color(0xFFB93A32),
    shadow = Color(0x40172040),
)

private val DarkColors = WordholdColors(
    isDark = true,
    background = Color(0xFF131626),
    surface = Color(0xFF1D2136),
    fill = Color(0xFF2A2F49),
    segmentThumb = Color(0xFF454C72),
    label = Color(0xFFECEEF7),
    secondaryLabel = Color(0xFFA1A6BD),
    separator = Color(0xFF303552),
    tint = Color(0xFFA9B4FF),
    onTint = Color(0xFF10143A),
    highlight = Color(0xFFE8B93E),
    onHighlight = Color(0xFF2A2100),
    success = Color(0xFF5CC8A0),
    danger = Color(0xFFF0837A),
    shadow = Color.Transparent,
)

/**
 * The iOS text-style scale. The font is the system default: SF Pro cannot be bundled
 * on Android, so the sizes, weights and tracking carry the iOS feel instead.
 */
@Immutable
class WordholdTypography {
    private val base = TextStyle(fontFamily = FontFamily.Default)

    val largeTitle = base.copy(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.37.sp)
    val title = base.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.35.sp)
    val headline = base.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.41).sp)
    val body = base.copy(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.41).sp)
    val subheadline = base.copy(fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.24).sp)
    val footnote = base.copy(fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = (-0.08).sp)
}

private val LocalColors = staticCompositionLocalOf { LightColors }
private val LocalTypography = staticCompositionLocalOf { WordholdTypography() }

object WordholdTheme {
    val colors: WordholdColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val type: WordholdTypography
        @Composable @ReadOnlyComposable get() = LocalTypography.current
}

/** Follows the system light/dark setting, with no dynamic (wallpaper) colour. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordholdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val type = WordholdTypography()

    // Material3 stays underneath as a base, mapped onto the same palette so any
    // Material component that does get used still matches.
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.tint, onPrimary = colors.onTint,
            secondary = colors.highlight, onSecondary = colors.onHighlight,
            background = colors.background, onBackground = colors.label,
            surface = colors.surface, onSurface = colors.label,
            onSurfaceVariant = colors.secondaryLabel,
            outline = colors.separator, error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.tint, onPrimary = colors.onTint,
            secondary = colors.highlight, onSecondary = colors.onHighlight,
            background = colors.background, onBackground = colors.label,
            surface = colors.surface, onSurface = colors.label,
            onSurfaceVariant = colors.secondaryLabel,
            outline = colors.separator, error = colors.danger,
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(bodyLarge = type.body, bodyMedium = type.subheadline, bodySmall = type.footnote),
    ) {
        CompositionLocalProvider(
            LocalColors provides colors,
            LocalTypography provides type,
            // No Material ripple anywhere; taps use Modifier.pressable feedback instead.
            LocalRippleConfiguration provides null,
            content = content,
        )
    }
}

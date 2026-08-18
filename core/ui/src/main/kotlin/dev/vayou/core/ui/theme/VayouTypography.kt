package dev.vayou.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The type scale. A component takes a style from here and never states a size of its own.
 *
 * Material's sizes and line heights, which is what every Android app of this kind is measured
 * against, with two departures that the last few years have made standard.
 *
 * Tracking is zero from 14sp up. Material's positive tracking on body text dates from Roboto being
 * drawn tight; at reading sizes it now just loosens a paragraph. It stays positive only at 11 and
 * 12sp, where letters do need the air, and negative at display sizes, where a headline is read as
 * one shape.
 *
 * Titles, labels and buttons are SemiBold rather than Medium. Weight is how a hierarchy is stated
 * now that everything else is flat -- no rules, no boxes, no second colour.
 */
@Immutable
data class VayouTypography(
    val displayLarge: TextStyle = style(FontWeight.Bold, 57, 64, -1.0),
    val displayMedium: TextStyle = style(FontWeight.Bold, 45, 52, -0.5),
    val headlineLarge: TextStyle = style(FontWeight.Bold, 32, 40, -0.5),
    val headlineMedium: TextStyle = style(FontWeight.Bold, 28, 36, -0.25),
    val headlineSmall: TextStyle = style(FontWeight.SemiBold, 24, 32, -0.2),
    val titleLarge: TextStyle = style(FontWeight.SemiBold, 22, 28, 0.0),
    val titleMedium: TextStyle = style(FontWeight.SemiBold, 16, 24, 0.0),
    val titleSmall: TextStyle = style(FontWeight.SemiBold, 14, 20, 0.0),
    val bodyLarge: TextStyle = style(FontWeight.Normal, 16, 24, 0.0),
    val bodyMedium: TextStyle = style(FontWeight.Normal, 14, 20, 0.0),
    val bodySmall: TextStyle = style(FontWeight.Normal, 12, 16, 0.2),
    val labelLarge: TextStyle = style(FontWeight.SemiBold, 14, 20, 0.0),
    val labelMedium: TextStyle = style(FontWeight.Medium, 12, 16, 0.3),
    val labelSmall: TextStyle = style(FontWeight.Medium, 11, 16, 0.4),
)

/** The four numbers that differ between the styles, so the fourteen entries read as a scale. */
private fun style(weight: FontWeight, size: Int, lineHeight: Int, letterSpacing: Double) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val VayouDefaultTypography = VayouTypography()

val LocalVayouTypography = staticCompositionLocalOf { VayouDefaultTypography }

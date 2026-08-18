package dev.vayou.core.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vayou.core.model.PlayerPreferences

/**
 * Three letters in the look asked for.
 *
 * The one that is meant to read on a bright scene is shown on one: dark letters on a pale plate. The
 * rest are shown as they are drawn, over the card's own dark ground.
 */
@Composable
fun SubtitleSample(preset: SubtitlePreset, modifier: Modifier = Modifier) {
    val style = remember(preset) { preset.applyTo(PlayerPreferences()) }
    val isLight = preset == SubtitlePreset.Light
    val edge = subtitleEdge(style.subtitleOutlineEnabled, style.subtitleShadow)
    val weight = if (style.subtitleTextBold) FontWeight.Bold else FontWeight.Normal
    val shadow = if (edge == SubtitleEdge.Shadow || edge == SubtitleEdge.Raised) {
        Shadow(color = Color.Black, offset = Offset(SampleShadow, SampleShadow), blurRadius = SampleShadowBlur)
    } else {
        Shadow.None
    }
    val plate = when {
        isLight -> Color(PaleScrim)
        style.subtitleBackground -> Color.Black
        else -> null
    }

    Box(
        modifier = if (plate == null) {
            modifier
        } else {
            modifier
                .background(plate, RoundedCornerShape(PlateCorner))
                .padding(horizontal = SamplePlateX, vertical = SamplePlateY)
        },
    ) {
        if (edge == SubtitleEdge.Outline || edge == SubtitleEdge.Raised) {
            BasicText(
                text = SampleText,
                style = TextStyle(
                    color = Color(style.subtitleOutlineColor),
                    fontSize = SampleSize,
                    fontWeight = weight,
                    drawStyle = Stroke(width = SampleStroke, join = StrokeJoin.Round),
                ),
            )
        }
        BasicText(
            text = SampleText,
            style = TextStyle(
                color = if (isLight) Color.Black else Color(style.subtitleTextColor),
                fontSize = SampleSize,
                fontWeight = weight,
                shadow = shadow,
            ),
        )
    }
}

private val SamplePlateX = 6.dp

private val SamplePlateY = 2.dp

/** In pixels, as [Stroke] takes them: wide enough to read as an edge at [SampleSize]. */
private const val SampleStroke = 6f

private const val SampleShadow = 2f

private const val SampleShadowBlur = 4f

/** Behind the one look meant for a bright scene, so its dark letters have something to be dark on. */
private const val PaleScrim = 0x80FFFFFF

private const val SampleText = "Abc"

private val SampleSize = 22.sp

private val PlateCorner = 4.dp

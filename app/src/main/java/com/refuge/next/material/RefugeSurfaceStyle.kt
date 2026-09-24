package com.refuge.next.material

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.refuge.next.design.RefugePalette
import androidx.compose.ui.graphics.shadow.Shadow as SurfaceShadow

internal val LocalGlassControlSurface = staticCompositionLocalOf { Color.White.copy(alpha = .48f) }

/** Content never samples a backdrop. Shadows are drawn outside its clip. */
internal fun Modifier.refugeContentMaterial(
    palette: RefugePalette,
    shape: Shape,
    fill: Color = palette.contentSurface,
    elevated: Boolean = false,
): Modifier {
    val isDark = palette.background.luminance() < .5f
    return this
        .then(
            if (elevated) Modifier.dropShadow(
                shape,
                SurfaceShadow(
                    radius = 10.dp,
                    offset = DpOffset(0.dp, 3.dp),
                    // A strong black drop shadow reads as a hard outline on
                    // the black app canvas. Keep only a quiet elevation cue.
                    color = Color.Black.copy(alpha = if (isDark) .07f else .08f),
                ),
        ) else Modifier,
        )
        .clip(shape)
        .background(fill)
}

internal object RefugeGlassStyle {
    val controlHighlight = Highlight.Default.copy(alpha = .26f)
    val barHighlight = Highlight.Default.copy(alpha = .22f)
    val controlShadow = Shadow(
        radius = 10.dp,
        offset = DpOffset(0.dp, 2.dp),
        color = Color.Transparent,
    )
    val barShadow = Shadow(
        radius = 16.dp,
        offset = DpOffset(0.dp, 4.dp),
        color = Color.Transparent,
    )
}

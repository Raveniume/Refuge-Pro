package com.refuge.next.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import com.refuge.next.design.RefugePalette

/** Sibling shell capture keeps foreground text out of its own refraction source. */
@Composable
fun RefugeFloatingGlass(
    backdrop: Backdrop,
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val doubleLayer = LocalDoubleLayerGlass.current
    val shell = rememberLayerBackdrop()
    val shape = Capsule(RoundedCornerStyle.Continuous)
    Box(modifier) {
        if (doubleLayer) {
            Box(Modifier.matchParentSize().layerBackdrop(shell).drawBackdrop(
                backdrop = backdrop, shape = { shape },
                effects = { vibrancy(); blur(8.dp.toPx()); lens(10.dp.toPx(), 12.dp.toPx()) },
                onDrawSurface = { drawRect(palette.contentSurface.copy(alpha = .36f)) },
            ))
        }
        val source = if (doubleLayer) rememberCombinedBackdrop(backdrop, shell) else backdrop
        Box(Modifier.drawBackdrop(
            backdrop = source, shape = { shape },
            effects = {
                vibrancy()
                blur(if (doubleLayer) 1.dp.toPx() else 6.dp.toPx())
                lens(8.dp.toPx(), 12.dp.toPx())
            },
            highlight = { RefugeGlassStyle.controlHighlight },
            shadow = { RefugeGlassStyle.controlShadow },
            innerShadow = null,
            onDrawSurface = { drawRect(palette.contentSurface.copy(alpha = if (doubleLayer) .30f else .64f)) },
        ), content = content)
    }
}

package com.refuge.next.material

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.refuge.next.design.RefugePalette

@Composable
fun RefugeScene(
    palette: RefugePalette,
    content: @Composable BoxScope.(backdrop: LayerBackdrop) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        val backdrop = rememberLayerBackdrop()
        Canvas(
            Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            drawRect(
                Brush.linearGradient(
                    colors = listOf(palette.backgroundEdge, palette.background, palette.background),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * .84f, size.height),
                ),
            )
            drawCircle(
                color = palette.accent.copy(alpha = .035f),
                radius = size.maxDimension * .72f,
                center = Offset(size.width * .12f, size.height * .08f),
            )
            drawCircle(
                color = palette.backgroundLight.copy(alpha = .024f),
                radius = size.maxDimension * .58f,
                center = Offset(size.width * .92f, size.height * .72f),
            )
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, palette.text.copy(alpha = .018f), Color.Transparent),
                    start = Offset(size.width * .04f, size.height * .12f),
                    end = Offset(size.width * .96f, size.height * .88f),
                ),
            )
        }
        content(backdrop)
    }
}

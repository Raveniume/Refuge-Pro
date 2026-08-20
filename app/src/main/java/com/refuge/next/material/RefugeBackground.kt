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
import androidx.compose.ui.graphics.StrokeCap
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
                    end = Offset(size.width * .78f, size.height),
                ),
            )
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, palette.backgroundLight.copy(alpha = .035f), Color.Transparent),
                    start = Offset(size.width * .08f, 0f),
                    end = Offset(size.width * .92f, size.height),
                ),
            )
            val curve = Path().apply {
                moveTo(-size.width * .08f, size.height * .28f)
                cubicTo(
                    size.width * .24f, size.height * .18f,
                    size.width * .42f, size.height * .42f,
                    size.width * .62f, size.height * .30f,
                )
                cubicTo(
                    size.width * .82f, size.height * .18f,
                    size.width * .94f, size.height * .34f,
                    size.width * 1.08f, size.height * .24f,
                )
            }
            drawPath(curve, palette.outline.copy(alpha = .075f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
            val lowerCurve = Path().apply {
                moveTo(-size.width * .04f, size.height * .78f)
                cubicTo(
                    size.width * .25f, size.height * .68f,
                    size.width * .46f, size.height * .86f,
                    size.width * .70f, size.height * .72f,
                )
                cubicTo(
                    size.width * .88f, size.height * .62f,
                    size.width * .98f, size.height * .80f,
                    size.width * 1.06f, size.height * .72f,
                )
            }
            drawPath(lowerCurve, palette.outline.copy(alpha = .045f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
        }
        content(backdrop)
    }
}

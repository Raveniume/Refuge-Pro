package com.refuge.next.material

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius

@Composable
fun RefugeOpticalTest(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    modifier: Modifier = Modifier,
) {
    val opticalBackdrop = rememberLayerBackdrop()
    Box(
        modifier
            .fillMaxWidth()
            .height(164.dp)
            .background(palette.contentSurface.copy(alpha = .34f), androidx.compose.foundation.shape.RoundedCornerShape(RefugeRadius.panel))
            .padding(1.dp),
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(162.dp)
                .layerBackdrop(opticalBackdrop),
        ) {
            drawRect(palette.background.copy(alpha = .28f))
            val trace = Path().apply {
                moveTo(-size.width * .04f, size.height * .62f)
                cubicTo(
                    size.width * .18f, size.height * .28f,
                    size.width * .32f, size.height * .86f,
                    size.width * .48f, size.height * .50f,
                )
                cubicTo(
                    size.width * .66f, size.height * .10f,
                    size.width * .78f, size.height * .88f,
                    size.width * 1.04f, size.height * .36f,
                )
            }
            drawPath(
                trace,
                color = palette.accent.copy(alpha = .78f),
                style = Stroke(width = 1.6.dp.toPx()),
            )
            drawLine(
                palette.outline.copy(alpha = .34f),
                Offset(0f, size.height * .20f),
                Offset(size.width, size.height * .20f),
                strokeWidth = 1.dp.toPx(),
            )
        }
        RefugeLiquidGlass(
            backdrop = rememberCombinedBackdrop(backdrop, opticalBackdrop),
            palette = palette,
            modifier = Modifier
                .fillMaxWidth(.56f)
                .height(74.dp)
                .align(androidx.compose.ui.Alignment.Center),
            radius = RefugeRadius.control,
            refractionHeight = 30.dp,
            refractionAmount = 42.dp,
            blurRadius = 2.dp,
            surfaceAlpha = .018f,
        ) {}
    }
}

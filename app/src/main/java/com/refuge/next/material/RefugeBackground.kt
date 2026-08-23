package com.refuge.next.material

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.refuge.next.R
import com.refuge.next.design.RefugePalette

/** Temporary approximation of the iOS/iPadOS 27 reference wallpaper. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.ReferenceWallpaper(palette: RefugePalette) {
    val dark = palette.background.luminance() < .5f
    val base = if (dark) Color(0xFF101522) else Color(0xFFF4F1EE)
    drawRect(base)
    drawRect(
        Brush.linearGradient(
            colors = if (dark) {
                listOf(Color(0xFF27314B), Color(0xFF171A2A), Color(0xFF101522))
            } else {
                listOf(Color(0xFFE8D7C5), Color(0xFFE9E7EE), Color(0xFFDDE8EF))
            },
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
    )
    val wave = Path().apply {
        moveTo(-size.width * .12f, size.height * .34f)
        cubicTo(size.width * .18f, size.height * .08f, size.width * .40f, size.height * .12f, size.width * .58f, size.height * .34f)
        cubicTo(size.width * .76f, size.height * .56f, size.width * .94f, size.height * .52f, size.width * 1.12f, size.height * .28f)
        lineTo(size.width * 1.12f, -size.height * .08f)
        lineTo(-size.width * .12f, -size.height * .08f)
        close()
    }
    drawPath(
        wave,
        Brush.linearGradient(
            if (dark) listOf(Color(0xFF3D4868).copy(alpha = .55f), Color(0xFF8A8097).copy(alpha = .18f))
            else listOf(Color(0xFFF3E0C8).copy(alpha = .88f), Color(0xFFB9C6D9).copy(alpha = .55f)),
        ),
    )
    val lowerWave = Path().apply {
        moveTo(-size.width * .14f, size.height * .78f)
        cubicTo(size.width * .18f, size.height * .58f, size.width * .46f, size.height * .62f, size.width * .68f, size.height * .80f)
        cubicTo(size.width * .88f, size.height * .96f, size.width * 1.06f, size.height * .92f, size.width * 1.14f, size.height * .74f)
        lineTo(size.width * 1.14f, size.height * 1.10f)
        lineTo(-size.width * .14f, size.height * 1.10f)
        close()
    }
    drawPath(
        lowerWave,
        Brush.linearGradient(
            if (dark) listOf(Color(0xFF23304B).copy(alpha = .44f), Color(0xFF4B415A).copy(alpha = .22f))
            else listOf(Color(0xFFD4E7F0).copy(alpha = .78f), Color(0xFFE9D8E8).copy(alpha = .60f)),
        ),
    )
    drawLine(
        color = if (dark) Color.White.copy(alpha = .11f) else Color.White.copy(alpha = .64f),
        start = Offset(size.width * .02f, size.height * .32f),
        end = Offset(size.width * .60f, size.height * .05f),
        strokeWidth = 1.2.dp.toPx(),
    )
}

@Composable
fun RefugeScene(
    palette: RefugePalette,
    content: @Composable BoxScope.(backdrop: LayerBackdrop) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        val backdrop = rememberLayerBackdrop()
        val isDark = palette.background.luminance() < .5f
        val darkFilter = if (isDark) ColorFilter.colorMatrix(
            ColorMatrix(floatArrayOf(
                .34f, 0f, 0f, 0f, 3f,
                0f, .39f, 0f, 0f, 5f,
                0f, 0f, .52f, 0f, 13f,
                0f, 0f, 0f, 1f, 0f,
            )),
        ) else null
        Image(
            painter = painterResource(R.drawable.reference_wallpaper_light),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = darkFilter,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        )
        content(backdrop)
    }
}

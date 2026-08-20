package com.refuge.next.material

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.design.RefugeTypography

@Composable
fun RefugeM80Thumbnail(
    palette: RefugePalette,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(RefugeRadius.image))
            .background(palette.backgroundEdge),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.linearGradient(
                    colors = listOf(palette.backgroundLight, palette.backgroundEdge, palette.background),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
            )
            drawLine(
                palette.accent.copy(alpha = .18f),
                Offset(0f, size.height * .72f),
                Offset(size.width, size.height * .58f),
                1.dp.toPx(),
            )
            val ship = Path().apply {
                moveTo(size.width * .12f, size.height * .64f)
                lineTo(size.width * .40f, size.height * .42f)
                lineTo(size.width * .58f, size.height * .48f)
                lineTo(size.width * .88f, size.height * .34f)
                lineTo(size.width * .78f, size.height * .66f)
                lineTo(size.width * .54f, size.height * .60f)
                lineTo(size.width * .30f, size.height * .78f)
                close()
            }
            drawPath(ship, palette.accent.copy(alpha = .88f))
            drawPath(
                ship,
                palette.text.copy(alpha = .52f),
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
            )
            drawRoundRect(
                color = palette.text.copy(alpha = .72f),
                topLeft = Offset(size.width * .42f, size.height * .50f),
                size = Size(size.width * .19f, size.height * .06f),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
            drawCircle(
                palette.positive.copy(alpha = .9f),
                radius = 2.dp.toPx(),
                center = Offset(size.width * .78f, size.height * .36f),
            )
        }
        Text(
            "M80",
            modifier = Modifier.padding(10.dp),
            style = RefugeTypography.caption(palette).copy(color = palette.text),
        )
    }
}

@Composable
fun RefugeImagePlaceholder(
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    label: String = "PACKAGE",
) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(RefugeRadius.image))
            .background(palette.backgroundEdge),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.linearGradient(
                    colors = listOf(palette.contentSurfaceStrong, palette.backgroundEdge),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
            )
            drawRoundRect(
                color = palette.accent.copy(alpha = .54f),
                topLeft = Offset(size.width * .26f, size.height * .28f),
                size = Size(size.width * .48f, size.height * .40f),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
            drawLine(
                palette.accent.copy(alpha = .54f),
                Offset(size.width * .35f, size.height * .40f),
                Offset(size.width * .65f, size.height * .40f),
                2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                palette.accent.copy(alpha = .54f),
                Offset(size.width * .35f, size.height * .52f),
                Offset(size.width * .58f, size.height * .52f),
                2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Text(
            label,
            modifier = Modifier.padding(10.dp),
            style = RefugeTypography.caption(palette),
        )
    }
}

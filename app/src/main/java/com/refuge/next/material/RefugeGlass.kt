package com.refuge.next.material

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius

@Composable
fun RefugeGlassSurface(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    radius: Dp = RefugeRadius.panel,
    fill: androidx.compose.ui.graphics.Color = palette.glass,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    RefugeLiquidGlass(
        backdrop = backdrop,
        palette = palette,
        modifier = modifier,
        radius = radius,
        padding = padding,
        surface = fill,
        surfaceAlpha = fill.alpha,
        content = content,
    )
}

@Composable
fun RefugeGlassControl(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    padding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    RefugeLiquidGlassButton(
        backdrop = backdrop,
        palette = palette,
        onClick = onClick,
        modifier = modifier,
        contentDescription = contentDescription,
        padding = padding,
        content = content,
    )
}

@Composable
fun RefugeQuietControl(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    padding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = { blur(1.dp.toPx()) },
                onDrawSurface = {
                    drawRect(palette.glass.copy(alpha = if (pressed) .10f else .045f))
                },
            )
            .semantics {
                role = Role.Button
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(padding),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun RefugeContentSurface(
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    radius: Dp = RefugeRadius.panel,
    fill: androidx.compose.ui.graphics.Color = palette.contentSurface,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .clip(shape)
            .background(fill)
            .padding(padding),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun RefugeModalSurface(
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    radius: Dp = RefugeRadius.floating,
    fill: androidx.compose.ui.graphics.Color = palette.contentSurfaceStrong,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    RefugeContentSurface(
        palette = palette,
        modifier = modifier.shadow(20.dp, RoundedCornerShape(radius), clip = false),
        radius = radius,
        fill = fill,
        padding = padding,
        content = content,
    )
}

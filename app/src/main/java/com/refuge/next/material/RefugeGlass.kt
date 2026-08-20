package com.refuge.next.material

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.refuge.next.design.RefugeTypography
import com.refuge.next.reference.ReferenceInteractiveHighlight

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

/** Moderate-weight liquid surface for major Store panels and transient sheets. */
@Composable
fun RefugeStandardGlassSurface(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    radius: Dp = RefugeRadius.floating,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    RefugeLiquidGlass(
        backdrop = backdrop,
        palette = palette,
        modifier = modifier,
        radius = radius,
        refractionHeight = 18.dp,
        refractionAmount = 24.dp,
        blurRadius = 7.dp,
        surface = palette.glassStrong,
        surfaceAlpha = .10f,
        chromaticAberration = true,
        padding = padding,
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
fun RefugeCompactUtilityPill(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RefugeQuietControl(
        backdrop = backdrop,
        palette = palette,
        onClick = onClick,
        contentDescription = label,
        modifier = modifier.height(30.dp),
        padding = PaddingValues(horizontal = 7.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = palette.textSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = RefugeTypography.secondary(palette).copy(color = palette.textSecondary))
        }
    }
}

@Composable
fun RefugeLightweightGlassSurface(
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    radius: Dp = RefugeRadius.panel,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val scope = rememberCoroutineScope()
    val highlight = remember(scope) { ReferenceInteractiveHighlight(scope) }
    val interactionSource = remember { MutableInteractionSource() }
    val baseModifier = modifier
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    palette.glassStrong.copy(alpha = if (palette.background.luminance() < .5f) .22f else .52f),
                    palette.contentSurface.copy(alpha = if (palette.background.luminance() < .5f) .70f else .78f),
                    palette.glass.copy(alpha = if (palette.background.luminance() < .5f) .12f else .38f),
                ),
            ),
        )
        .border(1.dp, palette.outline.copy(alpha = .72f), shape)
        .then(highlight.modifier)
        .then(highlight.gestureModifier)
        .then(
            if (onClick != null) {
                Modifier
                    .semantics {
                        role = Role.Button
                        if (contentDescription != null) this.contentDescription = contentDescription
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
            } else {
                Modifier
            },
        )
        .padding(padding)

    Box(baseModifier, contentAlignment = Alignment.Center, content = content)
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
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.glassStrong.copy(alpha = if (palette.background.luminance() < .5f) .18f else .48f),
                        fill,
                        fill.copy(alpha = fill.alpha * .86f),
                    ),
                ),
            )
            .border(1.dp, palette.outline.copy(alpha = .56f), shape)
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

package com.refuge.next.material

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius

/** Shared optical material used by every functional glass component and the optical test. */
@Composable
fun RefugeLiquidGlass(
    backdrop: Backdrop,
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    radius: Dp = RefugeRadius.control,
    padding: PaddingValues = PaddingValues(0.dp),
    refractionHeight: Dp = 14.dp,
    refractionAmount: Dp = 20.dp,
    blurRadius: Dp = 3.dp,
    surface: androidx.compose.ui.graphics.Color = palette.glass,
    surfaceAlpha: Float = .035f,
    interactionProgress: Float = 0f,
    chromaticAberration: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val progress = interactionProgress.coerceIn(0f, 1f)
    Box(
        modifier
            .clip(shape)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(blurRadius.toPx())
                    lens(
                        refractionHeight.toPx() * (1f + progress * .25f),
                        refractionAmount.toPx() * (1f + progress * .30f),
                        depthEffect = true,
                        chromaticAberration = chromaticAberration && progress > .01f,
                    )
                },
                highlight = {
                    Highlight.Default.copy(alpha = progress * .32f)
                },
                shadow = {
                    Shadow(alpha = .10f + progress * .08f)
                },
                innerShadow = {
                    InnerShadow(radius = 5.dp + 4.dp * progress, alpha = .10f + progress * .08f)
                },
                onDrawSurface = {
                    drawRect(surface.copy(alpha = surfaceAlpha + progress * .018f))
                },
            )
            .padding(padding),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun RefugeLiquidGlassButton(
    backdrop: Backdrop,
    palette: RefugePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    radius: Dp = RefugeRadius.control,
    padding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(if (pressed) 1f else 0f, label = "liquid-glass-press")
    RefugeLiquidGlass(
        backdrop = backdrop,
        palette = palette,
        modifier = modifier
            .semantics {
                role = Role.Button
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        radius = radius,
        padding = padding,
        refractionHeight = 12.dp,
        refractionAmount = 18.dp,
        blurRadius = 3.dp,
        surfaceAlpha = .028f,
        interactionProgress = progress,
        content = content,
    )
}

/**
 * Quiet list material: still uses the shared Liquid Glass lens pipeline, but
 * with a shallow blur/refraction and no per-row press animation. This keeps
 * long Store/Terminal LazyColumns responsive while preserving optical depth.
 */
@Composable
fun RefugeQuietLiquidGlassSurface(
    backdrop: Backdrop,
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    radius: Dp = RefugeRadius.panel,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val base = modifier
        .clip(shape)
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(1.5.dp.toPx())
                lens(7.dp.toPx(), 9.dp.toPx(), depthEffect = false)
            },
            highlight = { Highlight.Default.copy(alpha = .06f) },
            shadow = { Shadow(radius = 3.dp, alpha = .06f) },
            onDrawSurface = { drawRect(palette.contentSurface.copy(alpha = .20f)) },
        )
        .then(
            if (onClick != null) Modifier
                .semantics {
                    role = Role.Button
                    if (contentDescription != null) this.contentDescription = contentDescription
                }
                .clickable(interactionSource = null, indication = null, onClick = onClick)
            else Modifier,
        )
        .padding(padding)
    Box(base, contentAlignment = Alignment.Center, content = content)
}

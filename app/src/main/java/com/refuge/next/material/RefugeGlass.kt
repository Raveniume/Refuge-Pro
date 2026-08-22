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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.design.RefugeTypography
import com.refuge.next.motion.DampedDragAnimation
import com.refuge.next.reference.ReferenceInteractiveHighlight
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.util.lerp

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
    backdrop: Backdrop,
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
    backdrop: Backdrop,
    palette: RefugePalette,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RefugeCompactLiquidPill(
        backdrop = backdrop,
        palette = palette,
        icon = icon,
        label = label,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun RefugeLiquidToggle(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    checked: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    // Port of AndroidLiquidGlass' LiquidToggle: the thumb is a live lens over
    // the track, with pointer drag, velocity deformation, highlight and settle.
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val scope = rememberCoroutineScope()
    val trackWidth = 64.dp
    val thumbWidth = 40.dp
    val dragWidth = with(density) { (trackWidth - thumbWidth - 4.dp).toPx() }
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    val drag = remember(scope) {
        DampedDragAnimation(
            animationScope = scope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = .001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = { didDrag = false },
            onDragStopped = {
                val nextChecked = if (didDrag) targetValue >= .5f else !checked
                fraction = if (nextChecked) 1f else 0f
                if (nextChecked != checked) onClick()
                didDrag = false
                animateToValue(fraction)
            },
            onDrag = { _, amount ->
                didDrag = didDrag || amount.x != 0f
                val delta = amount.x / dragWidth
                fraction = (fraction + if (isLtr) delta else -delta).coerceIn(0f, 1f)
                updateValue(fraction)
            },
        )
    }
    LaunchedEffect(drag) {
        snapshotFlow { fraction }.collectLatest { drag.updateValue(it) }
    }
    LaunchedEffect(checked) {
        val target = if (checked) 1f else 0f
        if (target != fraction) {
            fraction = target
            drag.animateToValue(target)
        }
    }

    val trackBackdrop = rememberLayerBackdrop()
    val trackColor = if (palette.background.luminance() < .5f) {
        palette.outline.copy(alpha = .54f)
    } else {
        palette.outline.copy(alpha = .36f)
    }
    Box(
        modifier
            .size(width = 64.dp, height = 36.dp)
            .semantics {
                role = Role.Switch
                this.contentDescription = contentDescription
            }
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.layerBackdrop(trackBackdrop),
        ) {
            Box(
                Modifier
                    .clip(Capsule())
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx())
                            lens(6.dp.toPx(), 10.dp.toPx())
                        },
                        highlight = { Highlight.Default.copy(alpha = .08f + .12f * drag.pressProgress) },
                        shadow = { Shadow(alpha = .05f) },
                        onDrawSurface = {
                            drawRect(
                                lerp(trackColor, palette.positive, drag.value).copy(alpha = .58f),
                            )
                        },
                    )
                    .size(64.dp, 28.dp),
            )
        }
        Box(
            Modifier
                .graphicsLayer {
                    val padding = 2.dp.toPx()
                    translationX = if (isLtr) lerp(padding, padding + dragWidth, drag.value) else lerp(-padding, -(padding + dragWidth), drag.value)
                }
                .then(drag.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = drag.pressProgress
                            scale(
                                lerp(2f / 3f, .75f, progress),
                                lerp(0f, .75f, progress),
                            ) { drawBackdrop() }
                        },
                    ),
                    shape = { Capsule() },
                    effects = {
                        val progress = drag.pressProgress
                        vibrancy()
                        blur(8.dp.toPx() * (1f - progress))
                        lens(5.dp.toPx() * progress, 10.dp.toPx() * progress, chromaticAberration = progress > .01f)
                    },
                    highlight = { Highlight.Ambient.copy(alpha = drag.pressProgress) },
                    shadow = { Shadow(radius = 4.dp, color = Color.Black.copy(alpha = .08f)) },
                    innerShadow = { InnerShadow(radius = 4.dp * drag.pressProgress, alpha = drag.pressProgress) },
                    layerBlock = {
                        scaleX = drag.scaleX
                        scaleY = drag.scaleY
                        val velocity = drag.velocity / 50f
                        scaleX /= 1f - (velocity * .75f).coerceIn(-.2f, .2f)
                        scaleY *= 1f - (velocity * .25f).coerceIn(-.2f, .2f)
                    },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = .88f - .12f * drag.pressProgress)) },
                )
                .size(40.dp, 24.dp),
        )
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
                    palette.glassStrong.copy(alpha = if (palette.background.luminance() < .5f) .14f else .24f),
                    palette.contentSurface.copy(alpha = if (palette.background.luminance() < .5f) .34f else .42f),
                    palette.glass.copy(alpha = if (palette.background.luminance() < .5f) .08f else .18f),
                ),
            ),
        )
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
                        palette.glassStrong.copy(alpha = if (palette.background.luminance() < .5f) .10f else .18f),
                        fill.copy(alpha = if (palette.background.luminance() < .5f) .34f else .46f),
                        palette.glass.copy(alpha = if (palette.background.luminance() < .5f) .07f else .14f),
                    ),
                ),
            )
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

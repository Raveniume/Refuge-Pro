package com.refuge.next.reference

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
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
import com.kyant.shapes.RoundedCornerStyle
import com.refuge.next.motion.DampedDragAnimation
import com.refuge.next.material.LocalOpticalGlassEnabled
import com.refuge.next.material.LocalGlassControlSurface
import com.refuge.next.material.RefugeGlassStyle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sign
import kotlin.math.tanh

private val OfficialLightAccent = Color(0xFF0088FF)
private val OfficialDarkAccent = Color(0xFF0091FF)

/**
 * The official bottom-tab implementation scales the content rendered into
 * the selected lens while it is pressed. Keep this local neutral outside the
 * hidden sampling row so compact/top controls never inherit that behavior.
 */
internal val LocalLiquidBottomTabScale =
    staticCompositionLocalOf { { 1f } }

/**
 * Direct port of AndroidLiquidGlass' LiquidButton. The optional size values
 * are only exposed so a Refuge wrapper can separate visual bounds from its
 * hit target; the optical and pointer calculations remain official.
 */
@Composable
fun OfficialLiquidButtonPort(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    enablePressHighlight: Boolean = isInteractive,
    enabled: Boolean = true,
    handlesClick: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    visualHeight: Dp = 48.dp,
    contentPadding: Dp = 16.dp,
    shape: Shape = Capsule(RoundedCornerStyle.Continuous),
    horizontalArrangement: Arrangement.Horizontal =
        Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    content: @Composable RowScope.() -> Unit,
) {
    val readableSurface = if (surfaceColor.isSpecified) surfaceColor else LocalGlassControlSurface.current
    if (!LocalOpticalGlassEnabled.current) {
        Row(
            modifier
                .clip(shape)
                .background(
                    readableSurface,
                )
                .then(
                    if (handlesClick) {
                        Modifier.clickable(
                            enabled = enabled,
                            interactionSource = null,
                            indication = null,
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .height(visualHeight)
                .padding(horizontal = contentPadding),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        return
    }
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        ReferenceInteractiveHighlight(animationScope)
    }
    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    lens(16.dp.toPx(), 24.dp.toPx())
                },
                highlight = { RefugeGlassStyle.controlHighlight },
                shadow = { RefugeGlassStyle.controlShadow },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height
                        val progress = interactiveHighlight.progress
                        val scale = lerp(1f, 1f + 4.dp.toPx() / size.height, progress)
                        val maxOffset = size.minDimension
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(.032f * offset.x / maxOffset)
                        translationY = maxOffset * tanh(.032f * offset.y / maxOffset)
                        val maxDragScale = 4.dp.toPx() / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                            (width / height).coerceAtMost(1f)
                        scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                            (height / width).coerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = .75f))
                    }
                    if (!tint.isSpecified || surfaceColor.isSpecified) drawRect(readableSurface)
                    if (enablePressHighlight && interactiveHighlight.progress > .001f) {
                        drawRect(
                            Color.White.copy(alpha = .16f * interactiveHighlight.progress),
                            blendMode = BlendMode.Plus,
                        )
                    }
                },
            )
            .then(
                if (handlesClick) {
                    Modifier.clickable(
                        enabled = enabled,
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .then(
                if (enablePressHighlight && enabled) {
                    interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                },
            )
            .height(visualHeight)
            .padding(horizontal = contentPadding),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Direct AndroidLiquidGlass LiquidBottomTabs geometry and gesture pipeline. */
@Composable
fun OfficialLiquidBottomTabsPort(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    surfaceAlpha: Float = .94f,
    selectedSurfaceAlpha: Float = .68f,
    content: @Composable RowScope.(selectedIndex: Int, select: (Int) -> Unit) -> Unit,
) {
    OfficialLiquidTabsCore(
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        backdrop = backdrop,
        tabsCount = tabsCount,
        isDark = isDark,
        modifier = modifier,
        outerHeight = 64.dp,
        selectedHeight = 56.dp,
        pressedScale = 78f / 56f,
        containerRespondsToGesture = true,
        surfaceAlpha = surfaceAlpha,
        selectedSurfaceAlpha = selectedSurfaceAlpha,
        content = content,
    )
}

/** Same official lens pipeline with compact geometry for top segmented navigation. */
@Composable
fun OfficialLiquidSegmentedPort(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    outerHeight: Dp = 56.dp,
    surfaceAlpha: Float = .52f,
    selectedSurfaceAlpha: Float = .46f,
    content: @Composable RowScope.(selectedIndex: Int, select: (Int) -> Unit) -> Unit,
) {
    OfficialLiquidTabsCore(
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        backdrop = backdrop,
        tabsCount = tabsCount,
        isDark = isDark,
        // Keep the caller's bounds intact. The official lens owns its capsule
        // shape; clipping the whole host here cuts off the refractive edge.
        modifier = modifier,
        outerHeight = outerHeight,
        selectedHeight = outerHeight - 8.dp,
        pressedScale = 78f / 56f,
        containerRespondsToGesture = true,
        surfaceAlpha = surfaceAlpha,
        selectedSurfaceAlpha = selectedSurfaceAlpha,
        content = content,
    )
}

@Composable
private fun OfficialLiquidTabsCore(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    isDark: Boolean,
    modifier: Modifier,
    outerHeight: Dp,
    selectedHeight: Dp,
    pressedScale: Float,
    containerRespondsToGesture: Boolean,
    surfaceAlpha: Float = .94f,
    selectedSurfaceAlpha: Float = .68f,
    content: @Composable RowScope.(selectedIndex: Int, select: (Int) -> Unit) -> Unit,
) {
    require(tabsCount > 0) { "Liquid tabs require at least one tab" }
    if (!LocalOpticalGlassEnabled.current) {
        val containerColor = if (isDark) {
            Color(0xFF121212).copy(alpha = .18f)
        } else {
            Color(0xFFFAFAFA).copy(alpha = .28f)
        }
        Row(
            modifier
                .clip(Capsule(RoundedCornerStyle.Continuous))
                .background(containerColor)
                .height(outerHeight)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content(selectedIndex.coerceIn(0, tabsCount - 1)) { index ->
                onSelected(index.coerceIn(0, tabsCount - 1))
            }
        }
        return
    }
    val animationScope = rememberCoroutineScope()
    val accentColor = if (isDark) OfficialDarkAccent else OfficialLightAccent
    val containerColor = if (isDark) {
        Color(0xFF121212).copy(alpha = surfaceAlpha.coerceIn(0f, 1f))
    } else {
        Color(0xFFFAFAFA).copy(alpha = surfaceAlpha.coerceIn(0f, 1f))
    }
    val selectedSurfaceColor = if (isDark) Color(0xFF393C42) else Color(0xFFE3E7ED)
    BoxWithConstraints(
        modifier.clip(Capsule(RoundedCornerStyle.Continuous)),
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) { (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount }
        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) { 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
            }
        }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val touchSlopPx = LocalViewConfiguration.current.touchSlop
        var currentIndex by remember {
            mutableIntStateOf(selectedIndex.coerceIn(0, tabsCount - 1))
        }
        val drag = remember(animationScope, tabsCount, pressedScale) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIndex.coerceIn(0, tabsCount - 1).toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = .001f,
                initialScale = 1f,
                pressedScale = pressedScale,
                onDragStarted = {
                },
                onDragStopped = {
                    val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    animateToValue(target.toFloat())
                    currentIndex = target
                    onSelected(target)
                    animationScope.launch {
                        offsetAnimation.animateTo(0f, spring(1f, 300f, .5f))
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                },
            )
        }
        LaunchedEffect(selectedIndex) {
            val target = selectedIndex.coerceIn(0, tabsCount - 1)
            if (target != currentIndex) {
                currentIndex = target
                drag.animateToValue(target.toFloat())
            }
        }
        val interactiveHighlight = remember(animationScope) {
            ReferenceInteractiveHighlight(animationScope) { size, _ ->
                Offset(
                    if (isLtr) (drag.value + .5f) * tabWidth + panelOffset
                    else size.width - (drag.value + .5f) * tabWidth + panelOffset,
                    size.height / 2f,
                )
            }
        }
        val tabsBackdrop = rememberLayerBackdrop()
        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule(RoundedCornerStyle.Continuous) },
                    effects = {
                        vibrancy()
                        blur(18.dp.toPx())
                        lens(24.dp.toPx(), 24.dp.toPx())
                    },
                    layerBlock = if (containerRespondsToGesture) {
                        {
                            // The host surface responds to every press, including
                            // a tap on a non-selected segment. The selected lens
                            // keeps its own drag scale below, so both surfaces
                            // swell together during a press.
                            val progress = maxOf(drag.pressProgress, interactiveHighlight.progress)
                            val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, progress)
                            scaleX = scale
                            scaleY = scale
                        }
                    } else null,
                    onDrawSurface = {
                        drawRect(containerColor)
                    },
                )
                .then(interactiveHighlight.modifier)
                .height(outerHeight)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The visible labels are drawn once by the foreground pass below.
            // Keeping a second visible copy in the glass track causes text
            // ghosts when the lens is dragged by a small distance.
        }

        // Keep a second content capture for the selected lens, matching the
        // AndroidLiquidGlass reference. The selected cell is drawn above the
        // bar surface and samples both the page and the bar content, so its
        // refraction remains visible instead of being hidden behind the row.
        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                if (containerRespondsToGesture) lerp(1f, 1.2f, drag.pressProgress) else 1f
            },
        ) {
        Row(
            Modifier
                .alpha(0f)
                .clearAndSetSemantics {}
                .layerBackdrop(tabsBackdrop)
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule(RoundedCornerStyle.Continuous) },
                    effects = {
                        val progress = maxOf(drag.pressProgress, interactiveHighlight.progress)
                        vibrancy()
                        blur(18.dp.toPx())
                        lens(24.dp.toPx() * progress, 24.dp.toPx() * progress)
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = maxOf(drag.pressProgress, interactiveHighlight.progress))
                    },
                    onDrawSurface = { drawRect(containerColor) },
                )
                .then(interactiveHighlight.modifier)
                .height(outerHeight - 8.dp)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content(currentIndex) { index ->
                val target = index.coerceIn(0, tabsCount - 1)
                currentIndex = target
                drag.animateToValue(target.toFloat())
                onSelected(target)
            }
        }
        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    translationX = if (isLtr) {
                        drag.value * tabWidth + panelOffset
                    } else {
                        size.width - (drag.value + 1f) * tabWidth + panelOffset
                    }
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule(RoundedCornerStyle.Continuous) },
                    effects = {
                        val progress = drag.pressProgress
                        lens(
                            10.dp.toPx() * progress,
                            14.dp.toPx() * progress,
                            chromaticAberration = true,
                        )
                    },
                    highlight = { Highlight.Default.copy(alpha = drag.pressProgress) },
                    shadow = { Shadow(alpha = drag.pressProgress) },
                    innerShadow = { InnerShadow(radius = 8.dp * drag.pressProgress, alpha = drag.pressProgress) },
                    layerBlock = {
                        val pressScale = maxOf(drag.pressProgress, interactiveHighlight.progress)
                        val hostScale = lerp(1f, 1.2f, pressScale)
                        scaleX = drag.scaleX * hostScale
                        scaleY = drag.scaleY * hostScale
                        val velocity = drag.velocity / 10f
                        scaleX /= 1f - (velocity * .75f).fastCoerceIn(-.2f, .2f)
                        scaleY *= 1f - (velocity * .25f).fastCoerceIn(-.2f, .2f)
                    },
                    onDrawSurface = {
                        val progress = drag.pressProgress
                        drawRect(
                            selectedSurfaceColor.copy(alpha = selectedSurfaceAlpha.coerceIn(0f, 1f)),
                            alpha = 1f - progress,
                        )
                        drawRect(Color.Black.copy(alpha = .03f * progress))
                    },
                )
                .height(selectedHeight)
                .fillMaxWidth(1f / tabsCount),
        )
        // The lens is an optical surface, not a content occluder. Keep one
        // shared foreground pass for the tab glyphs and labels so the selected
        // item remains legible even when the material is opaque enough to hide
        // high-contrast page text underneath it.
        Row(
            Modifier
                .clearAndSetSemantics { }
                .then(interactiveHighlight.gestureModifier)
                .then(drag.modifier)
                .graphicsLayer { translationX = panelOffset }
                .height(outerHeight - 8.dp)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content(currentIndex) { index ->
                val target = index.coerceIn(0, tabsCount - 1)
                currentIndex = target
                drag.animateToValue(target.toFloat())
                onSelected(target)
            }
        }
        }
    }
}

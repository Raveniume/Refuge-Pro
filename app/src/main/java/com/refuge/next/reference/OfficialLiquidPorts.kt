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
    content: @Composable RowScope.(selectedIndex: Int, select: (Int) -> Unit) -> Unit,
) {
    OfficialLiquidTabsCore(
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        backdrop = backdrop,
        tabsCount = tabsCount,
        isDark = isDark,
        // The reference tab lens intentionally overscans in a bottom bar. A
        // compact top control has no such optical gutter, so keep every render
        // layer inside one stable continuous capsule.
        modifier = modifier.clip(Capsule(RoundedCornerStyle.Continuous)),
        outerHeight = outerHeight,
        selectedHeight = outerHeight - 8.dp,
        pressedScale = 1.10f,
        containerRespondsToGesture = false,
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
    val tabsBackdrop = rememberLayerBackdrop()
    val accentColor = if (isDark) OfficialDarkAccent else OfficialLightAccent
        val containerColor = if (isDark) {
            Color(0xFF1C1C1E).copy(alpha = .34f)
        } else {
            Color(0xFFECECF1).copy(alpha = .86f)
        }
    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        val tabWidth = with(density) { (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount }
        var gestureOffset by remember { mutableFloatStateOf(0f) }
        val settleOffset = remember { Animatable(0f) }
        val panelOffset by remember(density, containerRespondsToGesture) {
            derivedStateOf {
                if (!containerRespondsToGesture) return@derivedStateOf 0f
                val fraction = ((gestureOffset + settleOffset.value) / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) { 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
            }
        }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val touchSlopPx = LocalViewConfiguration.current.touchSlop
        val selectCurrent by rememberUpdatedState(onSelected)
        var currentIndex by remember(tabsCount) {
            mutableIntStateOf(selectedIndex.coerceIn(0, tabsCount - 1))
        }
        val drag = remember(animationScope, tabsCount, containerRespondsToGesture, pressedScale) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIndex.coerceIn(0, tabsCount - 1).toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = .001f,
                initialScale = 1f,
                pressedScale = pressedScale,
                springDuringDrag = containerRespondsToGesture,
                onDragStarted = {
                    animationScope.launch {
                        settleOffset.stop()
                    }
                },
                onDragStopped = {
                    val moved = abs(gestureOffset) >= touchSlopPx
                    // The lens is above the row and owns touches on the selected
                    // tab. A stationary release must publish a reselect too.
                    val target = if (moved) targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1) else currentIndex
                    animateToValue(target.toFloat())
                    currentIndex = target
                    selectCurrent(target)
                    val releasedOffset = settleOffset.value + gestureOffset
                    gestureOffset = 0f
                    if (releasedOffset != 0f) {
                        animationScope.launch {
                            settleOffset.snapTo(releasedOffset)
                            settleOffset.animateTo(0f, spring(1f, 300f, .5f))
                        }
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    gestureOffset += dragAmount.x
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
        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule(RoundedCornerStyle.Continuous) },
                    effects = {
                        vibrancy()
                        blur(16.dp.toPx())
                        lens(28.dp.toPx(), 32.dp.toPx())
                    },
                    highlight = { RefugeGlassStyle.barHighlight },
                    shadow = { RefugeGlassStyle.barShadow },
                    layerBlock = if (containerRespondsToGesture) {
                        {
                            val progress = drag.pressProgress
                            val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, progress)
                            scaleX = scale
                            scaleY = scale
                        }
                    } else null,
                    onDrawSurface = {
                        drawRect(containerColor)
                    },
                )
                .then(if (containerRespondsToGesture) interactiveHighlight.modifier else Modifier)
                .height(outerHeight)
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = .12f) else Color.Black.copy(alpha = .08f),
                    Capsule(RoundedCornerStyle.Continuous),
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content(currentIndex) { index ->
                val target = index.coerceIn(0, tabsCount - 1)
                currentIndex = target
                drag.animateToValue(target.toFloat())
                onSelected(target)
            }
        }

        // The selected lens still moves and the outer capsule keeps its press
        // highlight. Do not allocate a second full-size backdrop capture for
        // the transient lens while a root tab is changing. Android 15's
        // RenderThread can recurse through that nested capture during a fast
        // route switch and crash natively; the single outer glass layer gives
        // the same visual state without the unstable feedback graph.
        val selectionOpticsActive by remember(drag) {
            derivedStateOf { true }
        }
        // At rest the outer glass already contains the live page. The extra
        // tinted capture is only needed while the selected lens is deforming.
        if (selectionOpticsActive) {
            CompositionLocalProvider(
                LocalLiquidBottomTabScale provides {
                    if (containerRespondsToGesture) lerp(1f, 1.2f, drag.pressProgress) else 1f
                },
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics { }
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer { translationX = panelOffset }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule(RoundedCornerStyle.Continuous) },
                            effects = {
                                val progress = drag.pressProgress
                                vibrancy()
                                blur(8.dp.toPx())
                                lens(24.dp.toPx() * progress, 24.dp.toPx() * progress)
                            },
                            highlight = { RefugeGlassStyle.barHighlight.copy(alpha = .22f * drag.pressProgress) },
                            shadow = null,
                            onDrawSurface = { drawRect(containerColor) },
                        )
                        .then(interactiveHighlight.modifier)
                        // Keep the capture inside the outer bar's measured bounds.
                        .height(selectedHeight)
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
                .then(interactiveHighlight.gestureModifier)
                .then(drag.modifier)
                .then(if (selectionOpticsActive) Modifier.drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule(RoundedCornerStyle.Continuous) },
                    effects = {
                        val progress = drag.pressProgress
                        lens(10.dp.toPx() * progress, 14.dp.toPx() * progress, chromaticAberration = true)
                    },
                    highlight = { RefugeGlassStyle.controlHighlight.copy(alpha = .26f * drag.pressProgress) },
                    shadow = { RefugeGlassStyle.controlShadow.copy(alpha = drag.pressProgress) },
                    innerShadow = { InnerShadow(radius = 8.dp * drag.pressProgress, alpha = drag.pressProgress) },
                    layerBlock = {
                        scaleX = drag.scaleX
                        scaleY = drag.scaleY
                        val velocity = drag.velocity / 10f
                        scaleX /= 1f - (velocity * .75f).fastCoerceIn(-.2f, .2f)
                        scaleY *= 1f - (velocity * .25f).fastCoerceIn(-.2f, .2f)
                    },
                    onDrawSurface = {
                        val progress = drag.pressProgress
                        drawRect(
                            if (isDark) Color.White.copy(alpha = .10f) else Color.Black.copy(alpha = .065f),
                            alpha = 1f - progress,
                        )
                        drawRect(Color.Black.copy(alpha = .03f * progress))
                    },
                ) else Modifier.background(
                    if (isDark) Color.White.copy(alpha = .10f) else Color.Black.copy(alpha = .065f),
                    Capsule(RoundedCornerStyle.Continuous),
                ))
                .height(selectedHeight)
                .fillMaxWidth(1f / tabsCount),
        )
    }
}

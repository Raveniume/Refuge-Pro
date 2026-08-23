package com.refuge.next.reference

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.refuge.next.motion.DampedDragAnimation
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
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    visualHeight: Dp = 48.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        ReferenceInteractiveHighlight(animationScope)
    }
    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(12.dp.toPx(), 24.dp.toPx())
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height
                        val progress = interactiveHighlight.progress
                        val scale = lerp(1f, 1f + 4.dp.toPx() / size.height, progress)
                        val maxOffset = size.minDimension
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(.05f * offset.x / maxOffset)
                        translationY = maxOffset * tanh(.05f * offset.y / maxOffset)
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
                    if (surfaceColor.isSpecified) drawRect(surfaceColor)
                },
            )
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .then(
                if (enablePressHighlight) {
                    interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                },
            )
            .height(visualHeight)
            .padding(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
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
        modifier = modifier,
        outerHeight = outerHeight,
        selectedHeight = outerHeight - 8.dp,
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
    content: @Composable RowScope.(selectedIndex: Int, select: (Int) -> Unit) -> Unit,
) {
    require(tabsCount > 0) { "Liquid tabs require at least one tab" }
    val animationScope = rememberCoroutineScope()
    val tabsBackdrop = rememberLayerBackdrop()
    val accentColor = if (isDark) OfficialDarkAccent else OfficialLightAccent
    val containerColor = if (isDark) {
        Color(0xFF121212).copy(alpha = .4f)
    } else {
        Color(0xFFFAFAFA).copy(alpha = .4f)
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        val tabWidth = with(density) { (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount }
        var gestureOffset by remember { mutableFloatStateOf(0f) }
        val settleOffset = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = ((gestureOffset + settleOffset.value) / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) { 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
            }
        }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        var currentIndex by remember(tabsCount) {
            mutableIntStateOf(selectedIndex.coerceIn(0, tabsCount - 1))
        }
        val drag = remember(animationScope, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIndex.coerceIn(0, tabsCount - 1).toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = .001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {
                    animationScope.launch {
                        settleOffset.stop()
                        settleOffset.snapTo(0f)
                    }
                },
                onDragStopped = {
                    val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = target
                    animateToValue(target.toFloat())
                    onSelected(target)
                    val releasedOffset = gestureOffset
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
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx())
                        lens(24.dp.toPx(), 24.dp.toPx())
                    },
                    layerBlock = {
                        val progress = drag.pressProgress
                        val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) },
                )
                .then(interactiveHighlight.modifier)
                .height(outerHeight)
                .fillMaxWidth()
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

        Row(
            Modifier
                .clearAndSetSemantics { }
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        val progress = drag.pressProgress
                        vibrancy()
                        blur(8.dp.toPx())
                        lens(24.dp.toPx() * progress, 24.dp.toPx() * progress)
                    },
                    highlight = { Highlight.Default.copy(alpha = drag.pressProgress) },
                    onDrawSurface = { drawRect(containerColor) },
                )
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
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        val progress = drag.pressProgress
                        lens(10.dp.toPx() * progress, 14.dp.toPx() * progress, chromaticAberration = true)
                    },
                    highlight = { Highlight.Default.copy(alpha = drag.pressProgress) },
                    shadow = { Shadow(alpha = drag.pressProgress) },
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
                            if (isDark) Color.White.copy(alpha = .1f) else Color.Black.copy(alpha = .1f),
                            alpha = 1f - progress,
                        )
                        drawRect(Color.Black.copy(alpha = .03f * progress))
                    },
                )
                .height(selectedHeight)
                .fillMaxWidth(1f / tabsCount),
        )
    }
}

package com.refuge.next.reference

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
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

private val OfficialAccent = Color(0xFF0088FF)
private val OfficialDarkContainer = Color.White.copy(alpha = 0.03f)
private val OfficialLightContainer = Color.White.copy(alpha = 0.04f)

@Composable
fun ReferenceLiquidButton(
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    visualInset: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    minHeight: Dp = 42.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val highlight = remember(scope) { ReferenceInteractiveHighlight(scope) }
    Row(
        modifier
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(visualInset)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(2.dp.toPx())
                        lens(12.dp.toPx(), 24.dp.toPx())
                    },
                    highlight = { Highlight.Default.copy(alpha = .06f) },
                    shadow = { Shadow(alpha = .05f) },
                    layerBlock = {
                    val progress = highlight.progress
                    val scale = lerp(1f, 1f + 4.dp.toPx() / size.height, progress)
                    val maxOffset = size.minDimension
                    val offset = highlight.offset
                    translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
                    translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)
                    val maxDragScale = 4.dp.toPx() / size.height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                        (size.width / size.height).fastCoerceAtMost(1f)
                    scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                        (size.height / size.width).fastCoerceAtMost(1f)
                },
                onDrawSurface = {
                    if (tint.isSpecified) {
                        drawRect(tint)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                    if (surfaceColor.isSpecified) drawRect(surfaceColor)
                },
            )
            .then(highlight.modifier)
            .then(highlight.gestureModifier)
            .defaultMinSize(minHeight = minHeight)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun ReferenceLiquidSelectionBar(
    backdrop: Backdrop,
    isDark: Boolean,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    initialIndex: Int = 0,
    onSelected: (Int) -> Unit = {},
    content: @Composable RowScope.(selectedIndex: Int, select: (Int) -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val tabsBackdrop = rememberLayerBackdrop()
    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        val tabWidth = with(density) { (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            androidx.compose.runtime.derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) { 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
            }
        }
        var currentIndex by remember(tabsCount) { mutableIntStateOf(initialIndex.coerceIn(0, tabsCount - 1)) }
        val drag = remember(scope, tabsCount) {
            DampedDragAnimation(
                animationScope = scope,
                initialValue = initialIndex.coerceIn(0, tabsCount - 1).toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = target
                    animateToValue(target.toFloat())
                    onSelected(target)
                    scope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
                },
                onDrag = { _, amount ->
                    updateValue((targetValue + amount.x / tabWidth * if (isLtr) 1f else -1f).fastCoerceIn(0f, (tabsCount - 1).toFloat()))
                    scope.launch { offsetAnimation.snapTo(offsetAnimation.value + amount.x) }
                },
            )
        }
        LaunchedEffect(drag) {
            snapshotFlow { currentIndex }.drop(1).collectLatest { index ->
                drag.animateToValue(index.toFloat())
            }
        }
        val highlight = remember(scope) {
            ReferenceInteractiveHighlight(scope) { size, _ ->
                Offset((drag.value + 0.5f) * tabWidth + panelOffset, size.height / 2f)
            }
        }
        val container = if (isDark) OfficialDarkContainer else OfficialLightContainer
        val renderTabs: @Composable RowScope.((Int) -> Unit) -> Unit = { select ->
            content(currentIndex) { index ->
                currentIndex = index
                onSelected(index)
                select(index)
            }
        }

        Row(
            Modifier
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(12.dp) },
                    effects = {
                        vibrancy()
                        blur(5.dp.toPx())
                        lens(14.dp.toPx(), 18.dp.toPx())
                    },
                    highlight = { Highlight.Default.copy(alpha = .05f) },
                    shadow = { Shadow(alpha = .04f) },
                    layerBlock = {
                        val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, drag.pressProgress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(container) },
                )
                .then(highlight.modifier)
                .height(height)
                .fillMaxWidth()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            renderTabs { }
        }

        Row(
            Modifier
                .clearAndSetSemantics { }
                .alpha(0f)
                .layerBackdrop(tabsBackdrop)
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(12.dp) },
                    effects = {
                        val progress = drag.pressProgress
                        vibrancy()
                        blur(5.dp.toPx())
                        lens(14.dp.toPx() * progress, 18.dp.toPx() * progress)
                    },
                    highlight = { Highlight.Default.copy(alpha = .04f + .24f * drag.pressProgress) },
                    onDrawSurface = { drawRect(container) },
                )
                .height(height - 6.dp)
                .fillMaxWidth()
                .padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            renderTabs { }
        }

        Box(
            Modifier
                .graphicsLayer {
                    translationX = if (isLtr) {
                        drag.value * tabWidth + panelOffset
                    } else {
                        size.width - (drag.value + 1f) * tabWidth + panelOffset
                    }
                }
                .then(highlight.gestureModifier)
                .then(drag.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { RoundedCornerShape(18.dp) },
                    effects = {
                        lens(
                            8.dp.toPx() * drag.pressProgress,
                            12.dp.toPx() * drag.pressProgress,
                            chromaticAberration = drag.pressProgress > 0.01f,
                        )
                    },
                    highlight = { Highlight.Default.copy(alpha = drag.pressProgress) },
                    shadow = { Shadow(alpha = drag.pressProgress) },
                    innerShadow = { InnerShadow(radius = 8.dp * drag.pressProgress, alpha = drag.pressProgress) },
                    layerBlock = {
                        scaleX = drag.scaleX
                        scaleY = drag.scaleY
                        val velocity = drag.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        drawRect(
                            if (isDark) Color.White.copy(alpha = 0.008f) else Color.Black.copy(alpha = 0.006f),
                            alpha = 1f - drag.pressProgress,
                        )
                    },
                )
                    .height(height - 18.dp)
                    .fillMaxWidth(1f / tabsCount),
        )
    }
}

/**
 * Production root navigation derived directly from AndroidLiquidGlass'
 * LiquidBottomTabs geometry. Segmented controls intentionally keep their
 * compact geometry in [ReferenceLiquidSelectionBar].
 */
@Composable
fun ReferenceLiquidBottomTabs(
    backdrop: Backdrop,
    isDark: Boolean,
    tabsCount: Int,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(selectedIndex: Int, select: (Int) -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val tabsBackdrop = rememberLayerBackdrop()
    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        val tabWidth = with(density) { (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount }
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            androidx.compose.runtime.derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) { 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
            }
        }
        var currentIndex by remember(tabsCount) { mutableIntStateOf(selectedIndex.coerceIn(0, tabsCount - 1)) }
        val drag = remember(scope, tabsCount) {
            DampedDragAnimation(
                animationScope = scope,
                initialValue = selectedIndex.coerceIn(0, tabsCount - 1).toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = .001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = target
                    animateToValue(target.toFloat())
                    onSelected(target)
                    scope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, .5f)) }
                },
                onDrag = { _, amount ->
                    updateValue(
                        (targetValue + amount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    scope.launch { offsetAnimation.snapTo(offsetAnimation.value + amount.x) }
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
        val highlight = remember(scope) {
            ReferenceInteractiveHighlight(scope) { size, _ ->
                Offset(
                    if (isLtr) (drag.value + .5f) * tabWidth + panelOffset
                    else size.width - (drag.value + .5f) * tabWidth + panelOffset,
                    size.height / 2f,
                )
            }
        }
        val container = if (isDark) Color(0xFF121212).copy(alpha = .28f) else Color(0xFFFAFAFA).copy(alpha = .38f)
        val renderTabs: @Composable RowScope.((Int) -> Unit) -> Unit = { select ->
            content(currentIndex) { index ->
                currentIndex = index
                onSelected(index)
                select(index)
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
                    highlight = { Highlight.Default.copy(alpha = .06f) },
                    shadow = { Shadow(alpha = .05f) },
                    layerBlock = {
                        val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, drag.pressProgress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(container) },
                )
                .then(highlight.modifier)
                .height(54.dp)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { renderTabs { } }

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
                    highlight = { Highlight.Default.copy(alpha = .04f + .24f * drag.pressProgress) },
                    onDrawSurface = { drawRect(container) },
                )
                .height(50.dp)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { renderTabs { } }

        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    translationX = if (isLtr) drag.value * tabWidth + panelOffset
                    else size.width - (drag.value + 1f) * tabWidth + panelOffset
                }
                .then(highlight.gestureModifier)
                .then(drag.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        lens(
                            10.dp.toPx() * drag.pressProgress,
                            14.dp.toPx() * drag.pressProgress,
                            chromaticAberration = true,
                        )
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
                        drawRect(
                            if (isDark) Color.White.copy(alpha = .008f) else Color.Black.copy(alpha = .006f),
                            alpha = 1f - drag.pressProgress,
                        )
                    },
                )
                    .height(46.dp)
                    .fillMaxWidth(1f / tabsCount),
        )
    }
}

@Composable
fun ReferenceSegmentedControl(
    backdrop: Backdrop,
    isDark: Boolean,
    labels: List<String>,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
    onSelected: (Int) -> Unit = {},
) {
    ReferenceLiquidSelectionBar(
        backdrop = backdrop,
        isDark = isDark,
        tabsCount = labels.size,
        modifier = modifier,
        height = 48.dp,
        initialIndex = initialIndex,
        onSelected = onSelected,
    ) { selected, select ->
        labels.forEachIndexed { index, label ->
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .semantics { this.selected = index == selected }
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Tab,
                    ) { select(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                        color = if (index == selected) {
                            OfficialAccent
                    } else if (isDark) {
                        Color.White.copy(alpha = 0.78f)
                    } else {
                        Color.Black.copy(alpha = 0.72f)
                    },
                )
            }
        }
    }
}

@Composable
fun ReferenceSearchField(
    backdrop: Backdrop,
    isDark: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    searchIcon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val textColor = if (isDark) Color.White else Color.Black
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = textColor),
        modifier = modifier.semantics { contentDescription = "Search" },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(16.dp) },
                        effects = { vibrancy(); blur(2.dp.toPx()); lens(8.dp.toPx(), 12.dp.toPx()) },
                        onDrawSurface = { drawRect(if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.58f)) },
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(searchIcon, contentDescription = null, tint = if (isDark) Color.White.copy(alpha = 0.78f) else Color.Black.copy(alpha = 0.58f), modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f).padding(start = 8.dp)) {
                    if (value.isEmpty()) Text("Search", color = textColor.copy(alpha = 0.55f))
                    inner()
                }
            }
        },
    )
}

@Composable
fun ReferenceOpticalTest(
    backdrop: Backdrop,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val traceBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, traceBackdrop)
    val shape = RoundedCornerShape(20.dp)
    val trace = if (isDark) Color.White.copy(alpha = 0.64f) else Color.Black.copy(alpha = 0.48f)
    Box(
        modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize().layerBackdrop(traceBackdrop)) {
            drawRect(if (isDark) Color.Black.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.34f))
            drawLine(trace, Offset(-20f, size.height * 0.30f), Offset(size.width + 20f, size.height * 0.66f), 3.dp.toPx(), StrokeCap.Round)
            drawLine(trace.copy(alpha = trace.alpha * 0.58f), Offset(-20f, size.height * 0.78f), Offset(size.width + 20f, size.height * 0.42f), 2.dp.toPx(), StrokeCap.Round)
        }
        Box(
            Modifier
                .fillMaxWidth(0.62f)
                .height(88.dp)
                .drawBackdrop(
                    backdrop = combinedBackdrop,
                    shape = { RoundedCornerShape(24.dp) },
                    effects = {
                        vibrancy()
                        blur(2.dp.toPx())
                        lens(18.dp.toPx(), 28.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(if (isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.24f))
                    },
                ),
        )
    }
}

@Composable
fun RowScope.ReferenceSelectionItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val itemModifier = with(this@ReferenceSelectionItem) { Modifier.weight(1f) }
    Column(
        itemModifier
        .height(48.dp)
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val tint = if (selected) OfficialAccent else if (isDark) Color.White.copy(alpha = 0.78f) else Color.Black.copy(alpha = 0.72f)
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(if (selected) 21.dp else 19.dp))
        Text(label, color = tint, style = TextStyle(fontSize = 10.sp, lineHeight = 12.sp))
    }
}

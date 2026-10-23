package com.refuge.next.material

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeTypography
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Fixed central lens; the category strip slides underneath and snaps to its centre. */
@Composable
fun RefugeCenteredModeStrip(
    backdrop: Backdrop,
    palette: RefugePalette,
    labels: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?> = emptyList(),
    restingFraction: Float = .70f,
) {
    if (labels.isEmpty()) return
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceIn(labels.indices))
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by state.interactionSource.collectIsDraggedAsState()
    val interacting = pressed || dragged
    val pressProgress by animateFloatAsState(if (interacting) 1f else 0f,
        spring(1f, 1000f, .001f), label = "category-glass-press")
    val lensScaleX by animateFloatAsState(if (interacting) 78f / 56f else 1f,
        spring(.6f, 250f, .001f), label = "category-lens-x")
    val lensScaleY by animateFloatAsState(if (interacting) 78f / 56f else 1f,
        spring(.7f, 250f, .001f), label = "category-lens-y")
    val trackBackdrop = rememberLayerBackdrop()
    val lensBackdrop = rememberCombinedBackdrop(backdrop, trackBackdrop)
    val expanded = pressed || state.isScrollInProgress
    val widthFraction by animateFloatAsState(
        targetValue = if (expanded) 1f else restingFraction.coerceIn(.55f, 1f),
        animationSpec = tween(180),
        label = "centered-mode-strip-width",
    )
    val current by rememberUpdatedState(selected)
    val isDark = palette.background.luminance() < .5f
    val trackColor = if (isDark) Color(0xFF121212) else palette.contentSurface
    val lensColor = if (isDark) Color.White else Color.Black
    val cell = labels.maxOfOrNull { (it.length * 16 + 28 + if (icons.any { icon -> icon != null }) 30 else 0).dp }
        ?.coerceAtLeast(68.dp) ?: 68.dp

    LaunchedEffect(selected) {
        if (!state.isScrollInProgress && state.firstVisibleItemIndex != selected) {
            state.animateScrollToItem(selected.coerceIn(labels.indices))
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to state.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { (moving, index) ->
                if (!moving && index != current) onSelected(index.coerceIn(labels.indices))
            }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("refuge-liquid-mode-selector")
            .semantics { stateDescription = if (expanded) "expanded" else "collapsed" },
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            Modifier.fillMaxWidth(widthFraction).height(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            val sidePadding = ((maxWidth - cell) / 2).coerceAtLeast(0.dp)
            Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .testTag("mode-track")
                    .layerBackdrop(trackBackdrop)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule(RoundedCornerStyle.Continuous) },
                        effects = { vibrancy(); blur(8.dp.toPx()); lens(24.dp.toPx() * pressProgress, 24.dp.toPx() * pressProgress) },
                        highlight = { Highlight.Default.copy(alpha = pressProgress) },
                        shadow = { Shadow(alpha = .12f + pressProgress * .12f) },
                        innerShadow = {
                            InnerShadow(radius = 8.dp * pressProgress, alpha = pressProgress)
                        },
                        layerBlock = {
                            val scale = 1f + .2f * pressProgress
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = { drawRect(trackColor.copy(alpha = .4f)) },
                    ),
            )
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .width(cell)
                        .height(36.dp)
                        .testTag("mode-lens")
                        .drawBackdrop(
                            backdrop = lensBackdrop,
                            shape = { Capsule(RoundedCornerStyle.Continuous) },
                            effects = {
                                lens(10.dp.toPx() * pressProgress, 14.dp.toPx() * pressProgress, chromaticAberration = true)
                            },
                            highlight = { Highlight.Default.copy(alpha = pressProgress) },
                            shadow = { Shadow(alpha = pressProgress) },
                            innerShadow = { InnerShadow(radius = 8.dp * pressProgress, alpha = pressProgress) },
                            layerBlock = {
                                scaleX = lensScaleX
                                scaleY = lensScaleY
                            },
                            onDrawSurface = {
                                drawRect(lensColor.copy(alpha = .1f * (1f - pressProgress)))
                                drawRect(Color.Black.copy(alpha = .03f * pressProgress))
                            },
                        ),
                )
                LazyRow(
                    state = state,
                    flingBehavior = rememberSnapFlingBehavior(state),
                    contentPadding = PaddingValues(horizontal = sidePadding),
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    itemsIndexed(labels) { index, label ->
                        Box(
                            Modifier
                                .width(cell)
                                .height(44.dp)
                                .semantics {
                                    role = Role.Tab
                                    this.selected = selected == index
                                    contentDescription = label
                                }
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.Tab,
                                ) {
                                    onSelected(index)
                                    scope.launch { state.animateScrollToItem(index) }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                icons.getOrNull(index)?.let { icon ->
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (index == selected) palette.accent else palette.textSecondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(5.dp))
                                }
                                Text(
                                    label,
                                    style = RefugeTypography.body(palette).copy(
                                        color = if (index == selected) palette.accent else palette.textSecondary,
                                    ),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.refuge.next.material

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
    val expanded = pressed || state.isScrollInProgress
    val widthFraction by animateFloatAsState(
        targetValue = if (expanded) 1f else restingFraction.coerceIn(.55f, 1f),
        animationSpec = tween(180),
        label = "centered-mode-strip-width",
    )
    val current by rememberUpdatedState(selected)
    val isDark = palette.background.luminance() < .5f
    val trackColor = if (isDark) Color(0xFF19191C) else Color(0xFFF8F8FA)
    val lensColor = if (isDark) Color(0xFF393C42) else Color(0xFFE3E7ED)
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
            Box(
                Modifier
                    .fillMaxSize()
                    .testTag("mode-track")
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule(RoundedCornerStyle.Continuous) },
                        effects = { vibrancy(); blur(12.dp.toPx()); lens(18.dp.toPx(), 24.dp.toPx()) },
                        highlight = { Highlight.Default.copy(alpha = if (pressed) .42f else .18f) },
                        shadow = { Shadow(alpha = if (pressed) .34f else .12f) },
                        innerShadow = {
                            InnerShadow(radius = 6.dp, alpha = if (pressed) .24f else .10f)
                        },
                        layerBlock = {
                            val scale = 1f + if (pressed) 16.dp.toPx() / size.width else 0f
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = { drawRect(trackColor.copy(alpha = if (isDark) .38f else .52f)) },
                    ),
            ) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .width(cell)
                        .height(36.dp)
                        .testTag("mode-lens")
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule(RoundedCornerStyle.Continuous) },
                            effects = {
                                vibrancy()
                                blur(10.dp.toPx())
                                lens(16.dp.toPx(), 22.dp.toPx(), chromaticAberration = pressed)
                            },
                            highlight = { Highlight.Default.copy(alpha = if (pressed) .74f else .36f) },
                            shadow = { Shadow(alpha = if (pressed) .52f else .20f) },
                            innerShadow = { InnerShadow(radius = 5.dp, alpha = if (pressed) .38f else .16f) },
                            layerBlock = {
                                val scale = if (pressed) 78f / 56f else 1f
                                scaleX = scale
                                scaleY = scale
                            },
                            onDrawSurface = { drawRect(lensColor.copy(alpha = if (isDark) .48f else .58f)) },
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

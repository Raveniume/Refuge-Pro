package com.refuge.next.material

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.refuge.next.design.RefugeIconSize
import com.refuge.next.design.RefugePalette
import com.refuge.next.motion.DampedDragAnimation
import kotlin.math.abs
import kotlin.math.roundToInt

data class RefugeTab(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun RefugeBottomBar(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    selectedIndex: Int,
    tabs: List<RefugeTab>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animationScope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(58.dp),
    ) {
        val density = LocalDensity.current
        val tabWidth = (maxWidth - 8.dp) / tabs.size
        val tabWidthPx = with(density) { tabWidth.toPx() }
        val trackBackdrop = rememberLayerBackdrop()
        var downIndex by remember { mutableIntStateOf(selectedIndex) }
        var moved by remember { mutableStateOf(false) }
        val drag = remember(tabs.size, animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIndex.toFloat(),
                valueRange = 0f..(tabs.lastIndex).toFloat(),
                visibilityThreshold = .001f,
                initialScale = 1f,
                pressedScale = 1.06f,
                onDragStarted = { position ->
                    downIndex = (position.x / tabWidthPx).toInt().coerceIn(0, tabs.lastIndex)
                    moved = false
                },
                onDragStopped = {
                    val target = if (moved) {
                        targetValue.roundToInt().coerceIn(0, tabs.lastIndex)
                    } else {
                        downIndex
                    }
                    animateToValue(target.toFloat())
                    onSelected(target)
                },
                onDrag = { _, amount ->
                    moved = moved || abs(amount.x) > .5f
                    updateValue(targetValue + amount.x / tabWidthPx)
                },
            )
        }
        LaunchedEffect(selectedIndex) {
            if (selectedIndex != drag.targetValue.roundToInt()) {
                drag.animateToValue(selectedIndex.toFloat())
            }
        }

        RefugeLiquidGlass(
            backdrop = backdrop,
            palette = palette,
            modifier = Modifier.fillMaxWidth().height(52.dp).then(drag.modifier),
            radius = 26.dp,
            refractionHeight = 14.dp,
            refractionAmount = 20.dp,
            blurRadius = 4.dp,
            surfaceAlpha = .022f,
            padding = androidx.compose.foundation.layout.PaddingValues(4.dp),
        ) {
            RefugeLiquidGlass(
                backdrop = backdrop,
                palette = palette,
                modifier = Modifier.matchParentSize().alpha(0f).layerBackdrop(trackBackdrop),
                radius = 22.dp,
                refractionHeight = 12.dp,
                refractionAmount = 18.dp,
                blurRadius = 3.dp,
                surfaceAlpha = .016f,
            ) {}
            Row(
                Modifier.fillMaxWidth().height(44.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    RefugeBottomTab(
                        tab = tab,
                        palette = palette,
                        onClick = { onSelected(index) },
                    )
                }
            }
            val visualIndex = drag.value.roundToInt().coerceIn(0, tabs.lastIndex)
            RefugeLiquidGlass(
                backdrop = rememberCombinedBackdrop(backdrop, trackBackdrop),
                palette = palette,
                modifier = Modifier
                    .offset(x = tabWidth * drag.value)
                    .height(44.dp)
                    .fillMaxWidth(1f / tabs.size),
                radius = 22.dp,
                refractionHeight = 16.dp,
                refractionAmount = 22.dp,
                blurRadius = 4.dp,
                surface = palette.glassSelection,
                surfaceAlpha = .055f,
                interactionProgress = drag.pressProgress,
                chromaticAberration = true,
            ) {
                Icon(
                    imageVector = tabs[visualIndex].icon,
                    contentDescription = tabs[visualIndex].label,
                    tint = palette.accent,
                    modifier = Modifier.height(RefugeIconSize.medium),
                )
            }
        }
    }
}

@Composable
private fun RowScope.RefugeBottomTab(
    tab: RefugeTab,
    palette: RefugePalette,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .weight(1f)
            .height(44.dp)
            .semantics { contentDescription = tab.label }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = palette.textMuted,
            modifier = Modifier.height(RefugeIconSize.medium),
        )
    }
}

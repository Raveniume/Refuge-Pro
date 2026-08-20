package com.refuge.next.material

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeTypography
import com.refuge.next.motion.DampedDragAnimation
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun RefugeSegmentedControl(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animationScope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        val tabWidth = maxWidth / labels.size
        val density = LocalDensity.current
        val tabWidthPx = with(density) { tabWidth.toPx() }
        val trackBackdrop = rememberLayerBackdrop()
        var downIndex by remember { mutableIntStateOf(selectedIndex) }
        var moved by remember { mutableStateOf(false) }
        val drag = remember(labels.size, animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIndex.toFloat(),
                valueRange = 0f..(labels.lastIndex).toFloat(),
                visibilityThreshold = .001f,
                initialScale = 1f,
                pressedScale = 1.045f,
                onDragStarted = { position ->
                    downIndex = (position.x / tabWidthPx).toInt().coerceIn(0, labels.lastIndex)
                    moved = false
                },
                onDragStopped = {
                    val target = if (moved) {
                        targetValue.roundToInt().coerceIn(0, labels.lastIndex)
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
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .then(drag.modifier),
            radius = 23.dp,
            refractionHeight = 12.dp,
            refractionAmount = 18.dp,
            blurRadius = 3.dp,
            surfaceAlpha = .018f,
            padding = androidx.compose.foundation.layout.PaddingValues(3.dp),
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .alpha(0f)
                    .layerBackdrop(trackBackdrop),
            )
            Box(
                Modifier
                    .offset(x = tabWidth * drag.value)
                    .height(38.dp)
                    .fillMaxWidth(1f / labels.size),
            ) {
                RefugeLiquidGlass(
                    backdrop = rememberCombinedBackdrop(backdrop, trackBackdrop),
                    palette = palette,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    radius = 19.dp,
                    refractionHeight = 16.dp,
                    refractionAmount = 24.dp,
                    blurRadius = 4.dp,
                    surface = palette.glassSelection,
                    surfaceAlpha = .062f,
                    interactionProgress = drag.pressProgress,
                    chromaticAberration = false,
                ) {}
            }
            Row(
                Modifier.fillMaxWidth().height(38.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                labels.forEachIndexed { index, label ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) {
                                drag.animateToValue(index.toFloat())
                                onSelected(index)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material.Text(
                            text = label,
                            style = RefugeTypography.headline(palette).copy(
                                color = if (index == drag.value.roundToInt()) palette.text else palette.textSecondary,
                            ),
                        )
                    }
                }
            }
        }
    }
}

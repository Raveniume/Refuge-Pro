package com.refuge.next.material

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import com.kyant.backdrop.Backdrop
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.refuge.next.design.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Fixed central lens; the category strip slides underneath and snaps to its centre. */
@Composable
fun RefugeCenteredModeStrip(backdrop: Backdrop, palette: RefugePalette, labels: List<String>, selected: Int,
    onSelected: (Int) -> Unit, modifier: Modifier = Modifier,
    icons: List<ImageVector?> = emptyList(), restingFraction: Float = .70f) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceIn(labels.indices))
    val scope = rememberCoroutineScope()
    // Share one interaction source across the track cells so the whole
    // navbar expands on press and collapses after the drag/tap settles.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val expanded = pressed || state.isScrollInProgress
    val widthFraction by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 1f else restingFraction.coerceIn(.55f, 1f),
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "centered-mode-strip-width",
    )
    val callback by rememberUpdatedState(onSelected)
    val current by rememberUpdatedState(selected)
    val iconAllowance = if (icons.any { it != null }) 30 else 0
    val cell = labels.maxOfOrNull { (it.length * 16 + 20 + iconAllowance).dp }?.coerceAtLeast(68.dp) ?: 68.dp
    LaunchedEffect(selected) {
        if (!state.isScrollInProgress && state.firstVisibleItemIndex != selected) state.animateScrollToItem(selected)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to state.firstVisibleItemIndex }.distinctUntilChanged().collect { (moving, index) ->
            if (!moving && index != current) callback(index.coerceIn(labels.indices))
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("refuge-liquid-mode-selector")
            .semantics {
                stateDescription = if (expanded) "expanded" else "collapsed"
            },
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth(widthFraction).height(44.dp), contentAlignment = Alignment.Center) {
          val sidePadding = ((maxWidth - cell) / 2).coerceAtLeast(0.dp)
            RefugeLiquidGlass(backdrop, palette, Modifier.fillMaxSize().testTag("mode-track"), radius = 22.dp,
              surface = palette.contentSurfaceStrong, surfaceAlpha = .20f,
              interactionProgress = if (expanded) 1f else 0f) {
            RefugeLiquidGlass(backdrop, palette, Modifier.width(cell).height(36.dp).align(Alignment.Center).testTag("mode-lens"), radius = 18.dp,
                surface = palette.glassStrong, surfaceAlpha = .20f,
                interactionProgress = if (expanded) 1f else 0f) {}
            LazyRow(state = state, flingBehavior = rememberSnapFlingBehavior(state),
                contentPadding = PaddingValues(horizontal = sidePadding),
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)), verticalAlignment = Alignment.CenterVertically) {
                itemsIndexed(labels) { index, label ->
                    Box(Modifier.width(cell).height(44.dp).semantics {
                        role = Role.Tab
                        this.selected = selected == index
                        contentDescription = label
                    }
                        .clickable(interactionSource = interactionSource, indication = null) {
                            // Publish the selection immediately.  A quick series of
                            // taps may cancel the previous scroll animation; the
                            // selected tab must still follow the user's latest tap.
                            callback(index)
                            // A tap is a discrete selection. Updating the list
                            // position synchronously keeps repeated taps usable
                            // while the press animation is still settling.
                            scope.launch { state.scrollToItem(index) }
                        }, contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            icons.getOrNull(index)?.let { icon ->
                                Icon(icon, contentDescription = null,
                                    tint = if (index == selected) palette.accent else palette.textSecondary,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(label, style = RefugeTypography.body(palette).copy(color = if (index == selected) palette.accent else palette.textSecondary), maxLines = 1)
                        }
                    }
                }
            }
          }
        }
    }
}

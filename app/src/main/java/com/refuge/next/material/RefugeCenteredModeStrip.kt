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
import androidx.compose.ui.graphics.graphicsLayer
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
    val pressProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "centered-mode-strip-press",
    )
    val callback by rememberUpdatedState(onSelected)
    val current by rememberUpdatedState(selected)
    val isDark = palette.background.luminance() < .5f
    // The centered strip is used over both black and grouped light pages. A
    // neutral white glass surface keeps the backdrop visible while making the
    // functional layer legible in either appearance; using the dark content
    // surface here made the strip read as a flat black web control.
    val trackSurface = androidx.compose.ui.graphics.Color.White
    val trackAlpha = if (isDark) .14f else .42f
    val lensAlpha = if (isDark) .24f else .58f
    val trackEdge = androidx.compose.ui.graphics.Color.White.copy(alpha = if (isDark) .18f else .34f)
    val lensEdge = androidx.compose.ui.graphics.Color.White.copy(alpha = if (isDark) .30f else .52f)
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
        BoxWithConstraints(
          Modifier
            .fillMaxWidth(widthFraction)
            // Keep the optical control above the page while it responds to a
            // press. The reference component grows both the host and its lens;
            // scaling only the lens makes the bar look detached.
            .graphicsLayer {
                val scale = 1f + pressProgress * .035f
                scaleX = scale
                scaleY = scale
            }
            .height(44.dp),
          contentAlignment = Alignment.Center,
        ) {
          val sidePadding = ((maxWidth - cell) / 2).coerceAtLeast(0.dp)
            RefugeLiquidGlass(backdrop, palette,
              Modifier
                .fillMaxSize()
                .border(0.7.dp, trackEdge, RoundedCornerShape(22.dp))
                .testTag("mode-track"), radius = 22.dp,
              surface = trackSurface, surfaceAlpha = trackAlpha,
              blurRadius = 8.dp, refractionHeight = 16.dp, refractionAmount = 22.dp,
              highlightAlpha = .18f,
              interactionProgress = pressProgress) {
            RefugeLiquidGlass(backdrop, palette,
                Modifier
                    .width(cell)
                    .height(36.dp)
                    .align(Alignment.Center)
                    // The active lens is allowed to breathe past the track's
                    // optical edge, matching the press treatment used by the
                    // official liquid tabs while remaining within the scene.
                    .graphicsLayer {
                        val scale = 1f + pressProgress * .14f
                        scaleX = scale
                        scaleY = scale
                    }
                    .border(0.8.dp, lensEdge, RoundedCornerShape(18.dp))
                    .testTag("mode-lens"),
                radius = 18.dp,
                surface = trackSurface, surfaceAlpha = lensAlpha,
                blurRadius = 6.dp, refractionHeight = 14.dp, refractionAmount = 20.dp,
                highlightAlpha = .24f,
                interactionProgress = pressProgress) {}
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

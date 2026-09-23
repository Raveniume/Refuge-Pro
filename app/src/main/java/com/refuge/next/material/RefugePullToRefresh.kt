package com.refuge.next.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.LazyListState
import kotlin.math.min

/**
 * A small, native pull-to-refresh container for a LazyColumn.
 *
 * The list keeps its normal scroll behavior. Only a downward drag that starts
 * while the list is already at its first item is consumed by the refresh
 * affordance, so horizontal controls and regular upward scrolling are not
 * intercepted. The caller controls the loading state and can refresh from a
 * toolbar as well; the gesture is an additional path, not the only path.
 */
@Composable
fun RefugePullToRefresh(
    listState: LazyListState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorColor: Color = Color(0xFF0A84FF),
    content: @Composable BoxScope.() -> Unit,
) {
    val latestRefresh by rememberUpdatedState(onRefresh)
    val latestRefreshing by rememberUpdatedState(isRefreshing)
    var pullDistance by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val thresholdPx = with(density) { 72.dp.toPx() }
    val maxPullPx = with(density) { 112.dp.toPx() }

    fun atTop(): Boolean = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
    val connection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f && atTop() && !latestRefreshing) {
                    // Dampen the stretch so the content remains anchored to the
                    // top edge while the indicator follows the finger.
                    pullDistance = (pullDistance + available.y * .55f).coerceIn(0f, maxPullPx)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val shouldRefresh = pullDistance >= thresholdPx && !latestRefreshing
                pullDistance = 0f
                if (shouldRefresh) latestRefresh()
                return if (shouldRefresh) Velocity.Zero else available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Some Compose versions dispatch the release only through the
                // post-fling phase when the parent consumed the drag.
                val shouldRefresh = pullDistance >= thresholdPx && !latestRefreshing
                pullDistance = 0f
                if (shouldRefresh) latestRefresh()
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) pullDistance = 0f
    }

    val progress = (pullDistance / thresholdPx).coerceIn(0f, 1f)
    Box(
        modifier = modifier.nestedScroll(connection),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = min(pullDistance * .35f, 38f) },
            content = content,
        )
        if (isRefreshing || pullDistance > 0f) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    // The list itself applies statusBarsPadding, so the
                    // affordance must share that safe area.  Otherwise a
                    // background refresh paints over the system clock and
                    // looks clipped at the top edge.
                    .statusBarsPadding()
                    .offset(y = with(density) { min(pullDistance * .35f, 38.dp.toPx()).toDp() })
                    .size(40.dp)
                    .semantics { contentDescription = if (isRefreshing) "正在刷新" else "下拉刷新 ${(progress * 100).toInt()}%" },
                contentAlignment = Alignment.Center,
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = indicatorColor, strokeWidth = 2.dp)
                } else {
                    Icon(
                        RefugeIcons.refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = progress * 180f },
                    )
                }
            }
        }
    }
}

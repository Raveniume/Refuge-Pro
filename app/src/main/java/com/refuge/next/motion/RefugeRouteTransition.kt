package com.refuge.next.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds

/**
 * A single-tree route reveal. Liquid Glass pages allocate GPU backdrop layers;
 * keeping both the outgoing and incoming page alive during AnimatedContent can
 * compile two complete shader graphs at once and stall emulator render threads.
 * Route content is swapped without wrapping the complete screen in an alpha
 * graphics layer. A full-window RenderNode around scroll content, remote
 * images and backdrop controls blocks Android's RenderThread during cold
 * launch on the emulator.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> RefugeRouteTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    order: (T) -> Int = { it.hashCode() },
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier.clipToBounds(),
        transitionSpec = {
            val forward = order(targetState) >= order(initialState)
            val enter = slideInHorizontally(
                animationSpec = tween(220),
                initialOffsetX = { width -> if (forward) width / 10 else -width / 10 },
            ) + fadeIn(tween(180))
            val exit = slideOutHorizontally(
                animationSpec = tween(180),
                targetOffsetX = { width -> if (forward) -width / 12 else width / 12 },
            ) + fadeOut(tween(140))
            (enter togetherWith exit).using(SizeTransform(clip = false))
        },
        label = "root-route-transition",
    ) { route ->
        Box(Modifier.fillMaxSize()) { content(route) }
    }
}

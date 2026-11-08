package com.refuge.next.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/** Move the existing reading surface without duplicating its large data tree. */
@Composable
fun RefugeDetailMotion(destination: Any?, content: @Composable () -> Unit) {
    val offset = remember { Animatable(0f) }
    var previous by remember { mutableStateOf(destination) }
    LaunchedEffect(destination) {
        if (destination != previous) {
            offset.snapTo(if (destination == null) -60f else 90f)
            previous = destination
            offset.animateTo(0f, tween(240))
        }
    }
    Box(Modifier.fillMaxSize().offset { IntOffset(offset.value.roundToInt(), 0) }) { content() }
}

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
    animate: (from: T, to: T) -> Boolean = { _, _ -> true },
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier.clipToBounds(),
        transitionSpec = {
            if (!animate(initialState, targetState)) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val forward = order(targetState) >= order(initialState)
                val enter = slideInHorizontally(
                    animationSpec = tween(220),
                    initialOffsetX = { width -> if (forward) width / 10 else -width / 10 },
                ) + fadeIn(tween(180))
                val exit = slideOutHorizontally(
                    animationSpec = tween(180),
                    targetOffsetX = { width -> if (forward) -width / 12 else width / 12 },
                ) + fadeOut(tween(140))
                // AnimatedContent keeps both pages alive during the transition.
                // Raise the incoming page above the outgoing page explicitly;
                // otherwise the glass/backdrop layer of the old route can win
                // the draw order and leave the bottom tab selected on a stale
                // page after the animation settles.
                (enter togetherWith exit)
                    .using(SizeTransform(clip = false))
                    .also { it.targetContentZIndex = 1f }
            }
        },
        label = "root-route-transition",
    ) { route ->
        Box(Modifier.fillMaxSize()) { content(route) }
    }
}

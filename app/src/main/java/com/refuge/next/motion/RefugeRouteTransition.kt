package com.refuge.next.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
@Composable
fun <T> RefugeRouteTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    order: (T) -> Int = { it.hashCode() },
    content: @Composable (T) -> Unit,
) {
    Box(modifier.clipToBounds()) {
        key(targetState) {
            Box(Modifier.fillMaxSize()) { content(targetState) }
        }
    }
}

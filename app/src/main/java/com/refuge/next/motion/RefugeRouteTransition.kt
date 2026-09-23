package com.refuge.next.motion

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A single-tree route reveal. Liquid Glass pages allocate GPU backdrop layers;
 * keeping both the outgoing and incoming page alive during AnimatedContent can
 * compile two complete shader graphs at once and stall emulator render threads.
 * The selected route changes immediately. The incoming tree then settles a
 * short distance inside a clipped, fixed viewport, so the transition remains
 * responsive without compiling two full refraction graphs at once.
 */
@Composable
fun <T> RefugeRouteTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    order: (T) -> Int = { it.hashCode() },
    content: @Composable (T) -> Unit,
) {
    val currentOrder = order(targetState)
    var previousOrder by remember { mutableIntStateOf(currentOrder) }
    val direction = when {
        currentOrder > previousOrder -> 1f
        currentOrder < previousOrder -> -1f
        else -> 0f
    }
    SideEffect { previousOrder = currentOrder }
    val density = LocalDensity.current
    val animationsEnabled = ValueAnimator.areAnimatorsEnabled()

    Box(modifier.clipToBounds()) {
        key(targetState) {
            val reveal = remember { Animatable(if (animationsEnabled) 1f else 0f) }
            LaunchedEffect(Unit) {
                if (animationsEnabled) {
                    reveal.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    // Keep the transition in the layout/draw pipeline. A
                    // second graphics layer around a page that owns backdrop
                    // shaders can recurse through RenderThread on Android 15.
                    // The page remains a single glass tree while it slides in.
                    .offset {
                        IntOffset(
                            x = with(density) { (18.dp.toPx() * direction * reveal.value).roundToInt() },
                            y = 0,
                        )
                    }
                    .alpha(1f - reveal.value * .12f),
            ) {
                content(targetState)
            }
        }
    }
}

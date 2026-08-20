package com.refuge.next.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween

/** Keeps route surfaces clipped while content settles inside the incoming surface. */
@Composable
fun <T> RefugeRouteTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(tween(220)) + scaleIn(initialScale = .985f, animationSpec = tween(220)),
                initialContentExit = fadeOut(tween(120)),
                sizeTransform = SizeTransform(clip = true),
            )
        },
        label = "refuge-route-transition",
    ) { state -> content(state) }
}

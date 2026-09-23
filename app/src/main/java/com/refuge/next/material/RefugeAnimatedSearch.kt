package com.refuge.next.material

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun RefugeAnimatedSearch(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(180)) + expandVertically(
            animationSpec = tween(220),
            expandFrom = Alignment.Top,
        ),
        exit = fadeOut(tween(140)) + shrinkVertically(
            animationSpec = tween(180),
            shrinkTowards = Alignment.Top,
        ),
        content = { content() },
    )
}

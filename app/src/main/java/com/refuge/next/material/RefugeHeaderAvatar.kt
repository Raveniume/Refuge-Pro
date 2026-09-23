package com.refuge.next.material

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.refuge.next.R
import com.refuge.next.design.RefugePalette
import com.refuge.next.reference.ReferenceInteractiveHighlight

/** Shared header avatar with the official pointer-tracked liquid highlight. */
@Composable
fun RefugeHeaderAvatar(
    avatarUrl: String?,
    palette: RefugePalette,
    presenceColor: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val highlightScope = rememberCoroutineScope()
    val highlight = remember(highlightScope) { ReferenceInteractiveHighlight(highlightScope) }
    val interactionModifier = if (onClick != null) {
        Modifier
            .semantics {
                role = Role.Button
                contentDescription = "切换在线状态"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .then(highlight.gestureModifier)
    } else {
        Modifier.semantics { contentDescription = "用户头像" }
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .then(interactionModifier),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .size(46.dp)
                .shadow(5.dp, CircleShape),
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    placeholder = painterResource(R.drawable.user_profile_pic),
                    error = painterResource(R.drawable.user_profile_pic),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(CircleShape),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.user_profile_pic),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(CircleShape),
                )
            }
            if (onClick != null) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .then(highlight.modifier),
                )
            }
        }
        Box(
            Modifier
                .size(9.dp)
                .background(presenceColor, CircleShape)
                .border(.5.dp, palette.background.copy(alpha = .72f), CircleShape),
        )
    }
}

package com.refuge.next.material

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.refuge.next.design.RefugePalette

@Composable
fun ProfileLevelStars(level: String, palette: RefugePalette) {
    val value = (level.toIntOrNull() ?: 0).coerceIn(0, 5)
    Row(Modifier.clearAndSetSemantics { contentDescription = "等级 $value，共 5 星" },
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { index ->
            Icon(if (index < value) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
        }
    }
}

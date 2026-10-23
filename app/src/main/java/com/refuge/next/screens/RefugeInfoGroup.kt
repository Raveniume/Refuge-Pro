package com.refuge.next.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.refuge.next.design.*

/** Shared grouping for reading surfaces. Glass stays on the controls. */
@Composable
internal fun RefugeInfoGroup(
    palette: RefugePalette,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = RefugeTypography.caption(palette))
        Column(
            Modifier.fillMaxWidth()
                .background(palette.contentSurfaceStrong, refugeContinuousShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
internal fun RefugeInfoRows(palette: RefugePalette, rows: List<Pair<String, String>>) {
    val stacked = LocalDensity.current.fontScale > 1.3f
    val visible = rows.filter { it.second.isNotBlank() }
    Column(Modifier.fillMaxWidth()) {
        visible.forEachIndexed { index, (label, value) ->
            if (index > 0) Divider(color = palette.divider, thickness = .5.dp)
            if (stacked) {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(label, style = RefugeTypography.caption(palette))
                    Text(value, style = RefugeTypography.body(palette))
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(label, style = RefugeTypography.body(palette).copy(color = palette.textSecondary), modifier = Modifier.weight(1f))
                    Text(value, style = RefugeTypography.body(palette), textAlign = TextAlign.End, modifier = Modifier.weight(1.25f))
                }
            }
        }
    }
}

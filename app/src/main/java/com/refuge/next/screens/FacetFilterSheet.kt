@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)
package com.refuge.next.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.data.FacetSelection
import com.refuge.next.design.*
import com.refuge.next.material.*
import zone.ien.hig.section.*

@Composable
internal fun FacetFilterSheet(
    backdrop: LayerBackdrop, palette: RefugePalette, title: String,
    groups: Map<String, List<String>>, selection: FacetSelection,
    onChange: (FacetSelection) -> Unit, onDismiss: () -> Unit,
) {
    var group by remember { mutableStateOf<String?>(null) }
    val optionCount = if (group == null) groups.size else 1 + groups[group].orEmpty().size
    val compactHeight = (if (group == null) 170 else 150 + optionCount * 46).dp.coerceIn(300.dp, 620.dp)
    RefugeLiquidSheet(backdrop, palette, if (group == null) title else group!!, onDismiss, sheetHeight = compactHeight,
        surfaceRefraction = false,
        surfaceAlpha = 1f,
        actionOverContent = true,
        leadingAction = if (group != null) ({ local ->
            RefugeCircularHeaderButton(local, palette, RefugeIcons.back, "返回筛选", { group = null })
        }) else null,
        action = { local -> RefugeCompactUtilityPill(local, palette, RefugeIcons.check, "完成", onDismiss, Modifier.fillMaxWidth()) }) { local ->
        BackHandler(group != null) { group = null }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("已选 ${selection.values.sumOf { it.size }} 项", style = RefugeTypography.caption(palette), modifier = Modifier.weight(1f))
            TextButton({ onChange(emptyMap()) }) { Text("重置", color = palette.accent) }
        }
        CupertinoSection(color = palette.contentSurfaceStrong, contentPadding = PaddingValues(0.dp)) {
            if (group == null) groups.forEach { (key, _) ->
                SectionLink(onClick = { group = key }, indication = null,
                    caption = { Text(if (selection[key].isNullOrEmpty()) "全部" else "${selection[key]!!.size} 项", style = RefugeTypography.caption(palette)) },
                    title = { Text(key, style = RefugeTypography.body(palette)) })
            } else {
                val key = group!!
                (listOf("全部") + groups[key].orEmpty()).forEach { option ->
                    val selected = if (option == "全部") selection[key].isNullOrEmpty() else option in selection[key].orEmpty()
                    SectionLink(onClick = {
                        val values = if (option == "全部") emptySet() else selection[key].orEmpty().let { if (option in it) it - option else it + option }
                        onChange(selection + (key to values))
                    }, indication = null,
                        modifier = Modifier.semantics { this.selected = selected; role = Role.Checkbox },
                        chevron = { if (selected) Icon(RefugeIcons.check, "已选", tint = palette.accent, modifier = Modifier.size(20.dp)) },
                        title = { Text(translatedShipName(option), style = RefugeTypography.body(palette)) })
                }
            }
        }
        Spacer(Modifier.height(64.dp))
    }
}

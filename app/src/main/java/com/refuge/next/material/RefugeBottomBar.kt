package com.refuge.next.material

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.reference.ReferenceSelectionItem

data class RefugeTab(
    val label: String,
    val icon: ImageVector,
)

/** Legacy API retained for the lab; production optical behavior lives in the official port. */
@Composable
fun RefugeBottomBar(
    backdrop: LayerBackdrop,
    palette: com.refuge.next.design.RefugePalette,
    selectedIndex: Int,
    tabs: List<RefugeTab>,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    RefugeBottomTabs(
        backdrop = backdrop,
        isDark = palette.background.luminance() < .5f,
        tabsCount = tabs.size,
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        modifier = modifier,
    ) { selected, select ->
        tabs.forEachIndexed { index, tab ->
            ReferenceSelectionItem(tab.icon, tab.label, index == selected, palette.background.luminance() < .5f) { select(index) }
        }
    }
}

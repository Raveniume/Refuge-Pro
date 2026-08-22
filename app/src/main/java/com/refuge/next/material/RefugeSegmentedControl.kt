package com.refuge.next.material

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.design.RefugePalette

/** Legacy API retained for the lab; it now delegates to the shared moving lens. */
@Composable
fun RefugeSegmentedControl(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    RefugeLiquidSegmented(
        backdrop = backdrop,
        isDark = palette.background.luminance() < .5f,
        labels = labels,
        modifier = modifier,
        initialIndex = selectedIndex,
        onSelected = onSelected,
    )
}

package com.refuge.next.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeTypography
import com.refuge.next.reference.OfficialLiquidSegmentedPort

/** Retain the original glass segmented navigation for existing page call sites. */
@Composable
fun RefugeLiquidModeSelector(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?> = emptyList(),
) {
    if (labels.isEmpty()) return
    if (labels.size == 2) {
        RefugeTwoOptionSelector(
            backdrop = backdrop,
            palette = palette,
            labels = labels,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            icons = icons,
            modifier = modifier.testTag("refuge-liquid-mode-selector").testTag("mode-track"),
        )
        return
    }
    // Store and Terminal use the legacy centered track for every category
    // count. The selected item stays under the fixed middle lens while the
    // strip can be dragged, matching the reference interaction for 2, 3 and
    // long category lists alike.
    RefugeCenteredModeStrip(
        backdrop = backdrop,
        palette = palette,
        labels = labels,
        selected = selectedIndex.coerceIn(labels.indices),
        onSelected = onSelected,
        icons = icons,
        restingFraction = if (icons.any { it != null }) 1f else .70f,
        modifier = modifier.testTag("refuge-liquid-mode-selector").testTag("mode-track"),
    )
}

/** Two-option controls use a true equal-width segmented bar.  The centered
 * carousel is useful for long category lists, but it leaves an artificial
 * empty lens when there are only two choices (for example CCU objectives). */
@Composable
private fun RefugeTwoOptionSelector(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    icons: List<ImageVector?>,
    modifier: Modifier,
) {
    val selected = selectedIndex.coerceIn(0, 1)
    OfficialLiquidSegmentedPort(
        selectedIndex = selected,
        onSelected = onSelected,
        backdrop = backdrop,
        tabsCount = 2,
        isDark = palette.background.luminance() < .5f,
        modifier = modifier.testTag("mode-track"),
        outerHeight = 48.dp,
    ) { _, select ->
        labels.take(2).forEachIndexed { index, label ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics {
                        role = Role.Tab
                        this.selected = selected == index
                        contentDescription = label
                    }
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Tab,
                        onClick = { select(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    icons.getOrNull(index)?.let { icon ->
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = if (selected == index) palette.accent else palette.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        label,
                        style = RefugeTypography.body(palette).copy(
                            color = if (selected == index) palette.accent else palette.textSecondary,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

package com.refuge.next.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.reference.ReferenceLiquidBottomTabs
import com.refuge.next.reference.ReferenceLiquidButton
import com.refuge.next.reference.ReferenceLiquidSelectionBar
import com.refuge.next.reference.ReferenceSegmentedControl

/** Shared button entry point for all production liquid controls. */
@Composable
fun RefugeLiquidButton(
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit,
) = ReferenceLiquidButton(backdrop, onClick, modifier, tint = tint, content = content)

@Composable
fun RefugeLiquidIconButton(
    backdrop: Backdrop,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(44.dp),
    tint: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified,
) {
    RefugeLiquidButton(backdrop, onClick, modifier, tint = tint) {
        androidx.compose.material.Icon(
            icon,
            contentDescription,
            tint = if (iconTint.isSpecified) iconTint else androidx.compose.material.LocalContentColor.current,
            modifier = Modifier,
        )
    }
}

/** Official moving lens segmented primitive, with page-scoped sampling. */
@Composable
fun RefugeMovingLiquidLens(
    backdrop: Backdrop,
    isDark: Boolean,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    initialIndex: Int = 0,
    onSelected: (Int) -> Unit = {},
    content: @Composable RowScope.(selectedIndex: Int, select: (Int) -> Unit) -> Unit,
) = ReferenceLiquidSelectionBar(backdrop, isDark, tabsCount, modifier, height, initialIndex, onSelected, content)

@Composable
fun RefugeLiquidSegmented(
    backdrop: Backdrop,
    isDark: Boolean,
    labels: List<String>,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
    onSelected: (Int) -> Unit = {},
) = ReferenceSegmentedControl(backdrop, isDark, labels, modifier, initialIndex, onSelected)

@Composable
fun RefugeBottomTabs(
    backdrop: Backdrop,
    isDark: Boolean,
    tabsCount: Int,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(selectedIndex: Int, select: (Int) -> Unit) -> Unit,
) = ReferenceLiquidBottomTabs(backdrop, isDark, tabsCount, selectedIndex, onSelected, modifier, content)

/** Shared adaptive sheet entry point; modal content owns its optical root. */
@Composable
fun RefugeAdaptiveBottomSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable (LayerBackdrop) -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.(LayerBackdrop) -> Unit,
) = RefugeLiquidSheet(backdrop, palette, title, onDismiss, modifier, action, content)

data class RefugeFloatingAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

/** A single modal action toolbar; its children share one optical material. */
@Composable
fun RefugeFloatingActionGroup(
    backdrop: Backdrop,
    palette: RefugePalette,
    actions: List<RefugeFloatingAction>,
    modifier: Modifier = Modifier,
) {
    RefugeLiquidGlass(
        backdrop = backdrop,
        palette = palette,
        modifier = modifier,
        radius = RefugeRadius.floating,
        padding = PaddingValues(3.dp),
        refractionHeight = 12.dp,
        refractionAmount = 18.dp,
        blurRadius = 2.dp,
        surfaceAlpha = .025f,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { action ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = action.label
                        }
                        .clickable(enabled = action.enabled, onClick = action.onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(action.icon, null, tint = if (action.enabled) palette.text else palette.textMuted)
                        Text(action.label, style = com.refuge.next.design.RefugeTypography.caption(palette).copy(color = if (action.enabled) palette.text else palette.textMuted))
                    }
                }
            }
        }
    }
}

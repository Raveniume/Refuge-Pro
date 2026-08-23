package com.refuge.next.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
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
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.reference.OfficialLiquidBottomTabsPort
import com.refuge.next.reference.OfficialLiquidButtonPort
import com.refuge.next.reference.OfficialLiquidSegmentedPort
import androidx.compose.ui.unit.LayoutDirection

/** Shared button entry point for all production liquid controls. */
@Composable
fun RefugeLiquidButton(
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    visualInset: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    minHeight: Dp = 42.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val horizontalPadding = maxOf(
        contentPadding.calculateLeftPadding(LayoutDirection.Ltr),
        contentPadding.calculateRightPadding(LayoutDirection.Ltr),
    )
    OfficialLiquidButtonPort(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier.padding(visualInset),
        tint = tint,
        visualHeight = minHeight.coerceAtLeast(32.dp),
        contentPadding = horizontalPadding,
        content = content,
    )
}

@Composable
fun RefugeLiquidIconButton(
    backdrop: Backdrop,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(48.dp),
    tint: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified,
    isInteractive: Boolean = true,
    enablePressHighlight: Boolean = isInteractive,
) {
    OfficialLiquidButtonPort(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier,
        isInteractive = isInteractive,
        enablePressHighlight = enablePressHighlight,
        visualHeight = 36.dp,
        contentPadding = 0.dp,
        content = {
            androidx.compose.material.Icon(
                icon,
                contentDescription,
                tint = if (iconTint.isSpecified) iconTint else androidx.compose.material.LocalContentColor.current,
                modifier = Modifier.size(18.dp),
            )
        },
    )
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
) = OfficialLiquidSegmentedPort(
    selectedIndex = initialIndex,
    onSelected = onSelected,
    backdrop = backdrop,
    tabsCount = tabsCount,
    isDark = isDark,
    modifier = modifier,
    outerHeight = height,
    content = content,
)

@Composable
fun RefugeLiquidSegmented(
    backdrop: Backdrop,
    isDark: Boolean,
    labels: List<String>,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
    onSelected: (Int) -> Unit = {},
    scrollable: Boolean = labels.size > 4,
) {
    val scrollState = rememberScrollState()
    val minWidth = (labels.size * 82).dp
    val content: @Composable RowScope.(Int, (Int) -> Unit) -> Unit = { selected, select ->
        labels.forEachIndexed { index, label ->
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .semantics { this.contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (index == selected) {
                        if (isDark) Color(0xFF0091FF) else Color(0xFF0088FF)
                    } else if (isDark) Color.White.copy(alpha = .78f) else Color.Black.copy(alpha = .72f),
                    modifier = Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Tab,
                    ) { select(index) },
                )
            }
        }
    }
    if (scrollable) {
        // Keep a bounded viewport so LazyColumn does not measure the glass lens as a
        // zero-width unbounded child. The official lens itself remains the scrolled
        // content and keeps its real drag/refraction pipeline.
        Box(
            modifier = modifier
                .height(48.dp)
                .horizontalScroll(scrollState),
            contentAlignment = Alignment.CenterStart,
        ) {
            OfficialLiquidSegmentedPort(
                selectedIndex = initialIndex,
                onSelected = onSelected,
                backdrop = backdrop,
                tabsCount = labels.size,
                isDark = isDark,
                modifier = Modifier.width(minWidth).height(48.dp),
                outerHeight = 48.dp,
                content = content,
            )
        }
    } else {
        OfficialLiquidSegmentedPort(
            selectedIndex = initialIndex,
            onSelected = onSelected,
            backdrop = backdrop,
            tabsCount = labels.size,
            isDark = isDark,
            modifier = modifier,
            outerHeight = 48.dp,
            content = content,
        )
    }
}

@Composable
fun RefugeBottomTabs(
    backdrop: Backdrop,
    isDark: Boolean,
    tabsCount: Int,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(selectedIndex: Int, select: (Int) -> Unit) -> Unit,
) = OfficialLiquidBottomTabsPort(
    selectedIndex = selectedIndex,
    onSelected = onSelected,
    backdrop = backdrop,
    tabsCount = tabsCount,
    isDark = isDark,
    modifier = modifier,
    content = content,
)

/** Shared adaptive sheet entry point; modal content owns its optical root. */
@Composable
fun RefugeAdaptiveBottomSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetHeight: Dp? = null,
    actionBottomPadding: Dp = 14.dp,
    actionOverContent: Boolean = false,
    action: (@Composable (Backdrop) -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.(LayerBackdrop) -> Unit,
) = RefugeLiquidSheet(
    backdrop,
    palette,
    title,
    onDismiss,
    modifier,
    sheetHeight,
    actionBottomPadding,
    actionOverContent,
    action,
    content,
)

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
    OfficialLiquidButtonPort(
        onClick = {},
        backdrop = backdrop,
        modifier = modifier,
        isInteractive = false,
        enablePressHighlight = true,
        visualHeight = 42.dp,
        contentPadding = 2.dp,
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
                        .height(40.dp)
                        .semantics { role = Role.Button; contentDescription = action.label }
                        .clickable(
                            enabled = action.enabled,
                            interactionSource = null,
                            indication = null,
                            onClick = action.onClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(action.icon, null, tint = if (action.enabled) palette.text else palette.textMuted)
                }
            }
        }
    }
}

/** Compact functional pill with the official LiquidButton press and drag. */
@Composable
fun RefugeCompactLiquidPill(
    backdrop: Backdrop,
    palette: RefugePalette,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OfficialLiquidButtonPort(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier.semantics { role = Role.Button; contentDescription = label },
        tint = Color.Unspecified,
        visualHeight = 34.dp,
        contentPadding = 9.dp,
    ) {
        Icon(icon, null, tint = palette.textSecondary, modifier = Modifier.size(15.dp))
        Text(label, style = com.refuge.next.design.RefugeTypography.secondary(palette).copy(color = palette.textSecondary))
    }
}

/** One quiet content-layer lens for an inventory list; rows do not create glass. */
@Composable
fun InventoryGlassGroup(
    backdrop: Backdrop,
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = RefugeSpacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    RefugeLiquidGlass(
        backdrop = backdrop,
        palette = palette,
        modifier = modifier,
        radius = RefugeRadius.panel,
        refractionHeight = 14.dp,
        refractionAmount = 22.dp,
        blurRadius = 4.dp,
        surface = palette.contentSurface,
        surfaceAlpha = .055f,
        padding = padding,
    ) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

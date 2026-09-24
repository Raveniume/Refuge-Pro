package com.refuge.next.material

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.refugeContinuousShape
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
    enabled: Boolean = true,
    iconSize: Dp = 20.dp,
) {
    OfficialLiquidButtonPort(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier,
        isInteractive = isInteractive,
        enablePressHighlight = enablePressHighlight,
        enabled = enabled,
        tint = tint,
        visualHeight = 36.dp,
        contentPadding = 0.dp,
        content = {
            androidx.compose.material.Icon(
                icon,
                contentDescription,
                tint = if (enabled) {
                    if (iconTint.isSpecified) iconTint else androidx.compose.material.LocalContentColor.current
                } else {
                    paletteDisabledTint(iconTint)
                },
                modifier = Modifier.size(iconSize),
            )
        },
    )
}

/** Circular header control used for page navigation. Its measured bounds are
 * square, so the icon never causes the header capsule to change width. */
@Composable
fun RefugeCircularHeaderButton(
    backdrop: Backdrop,
    palette: RefugePalette,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(44.dp),
) {
    // Use the same continuous-corner geometry as sheets and modal surfaces.
    // At a square size this remains circular, while the curvature follows the
    // shared HIG silhouette instead of switching to a separate arc shape.
    val buttonShape = refugeContinuousShape(22.dp)
    OfficialLiquidButtonPort(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier.semantics { role = Role.Button; this.contentDescription = contentDescription },
        visualHeight = 44.dp,
        contentPadding = 0.dp,
        shape = buttonShape,
        content = {
            androidx.compose.material.Icon(icon, contentDescription, tint = palette.text, modifier = Modifier.size(20.dp))
        },
    )
}

private fun paletteDisabledTint(iconTint: Color): Color =
    if (iconTint.isSpecified) iconTint.copy(alpha = .30f) else Color.Gray.copy(alpha = .42f)

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
    height: Dp = 48.dp,
    icons: List<ImageVector?> = emptyList(),
) {
    val scrollState = rememberScrollState()
    val minWidth = (labels.size * 82).dp
    val content: @Composable RowScope.(Int, (Int) -> Unit) -> Unit = { selected, select ->
        labels.forEachIndexed { index, label ->
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .semantics { this.contentDescription = label; this.selected = index == selected; role = Role.Tab }
                    .clickable(interactionSource = null, indication = null, role = Role.Tab) { select(index) },
                contentAlignment = Alignment.Center,
            ) {
                val labelColor = if (index == selected) {
                    if (isDark) Color(0xFF0091FF) else Color(0xFF0088FF)
                } else if (isDark) Color.White.copy(alpha = .78f) else Color.Black.copy(alpha = .72f)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    icons.getOrNull(index)?.let { icon ->
                        Icon(icon, contentDescription = null, tint = labelColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(label, color = labelColor)
                }
            }
        }
    }
    if (scrollable) {
        // Keep the selected lens inside a bounded, clipped viewport. Without
        // centering the newly selected segment, the official lens can settle
        // beyond the visible half of a long terminal bar and appear to cut
        // through the page header.
        BoxWithConstraints(
            modifier = modifier
                .height(height)
                .clip(Capsule(RoundedCornerStyle.Continuous)),
            contentAlignment = Alignment.CenterStart,
        ) {
            val density = LocalDensity.current
            val cellWidth = minWidth / labels.size
            val viewportWidth = maxWidth
            LaunchedEffect(initialIndex, viewportWidth, minWidth) {
                val maxOffset = (minWidth - viewportWidth).coerceAtLeast(0.dp)
                val current = with(density) { scrollState.value.toDp() }
                val left = cellWidth * initialIndex
                val right = left + cellWidth
                val desired = when {
                    left < current -> left
                    right > current + viewportWidth -> right - viewportWidth
                    else -> current
                }.coerceIn(0.dp, maxOffset)
                scrollState.animateScrollTo(with(density) { desired.roundToPx() })
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .horizontalScroll(scrollState),
                contentAlignment = Alignment.CenterStart,
            ) {
                OfficialLiquidSegmentedPort(
                    selectedIndex = initialIndex,
                    onSelected = onSelected,
                    backdrop = backdrop,
                    tabsCount = labels.size,
                    isDark = isDark,
                    modifier = Modifier.width(minWidth).height(height),
                    outerHeight = height,
                    content = content,
                )
            }
        }
    } else {
        OfficialLiquidSegmentedPort(
            selectedIndex = initialIndex,
            onSelected = onSelected,
            backdrop = backdrop,
            tabsCount = labels.size,
            isDark = isDark,
            modifier = modifier,
            outerHeight = height,
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
    backdrop = backdrop,
    palette = palette,
    title = title,
    onDismiss = onDismiss,
    modifier = modifier,
    sheetHeight = sheetHeight,
    actionBottomPadding = actionBottomPadding,
    actionOverContent = actionOverContent,
    action = action,
    content = content,
)

data class RefugeFloatingAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    /** Optional semantic tint for a primary action (for example a discount badge). */
    val tint: Color = Color.Unspecified,
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
        isInteractive = true,
        enablePressHighlight = true,
        handlesClick = false,
        visualHeight = 48.dp,
        contentPadding = 0.dp,
        horizontalArrangement = Arrangement.Start,
    ) {
        actions.forEach { action ->
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = action.label
                        if (!action.enabled) disabled()
                    }
                    .clickable(
                        enabled = action.enabled,
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = action.onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    action.icon,
                    null,
                    tint = if (action.enabled) palette.text else palette.textMuted.copy(alpha = .72f),
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

/** One connected optical toolbar for page-header actions. */
@Composable
fun RefugeHeaderActionBar(
    backdrop: Backdrop,
    palette: RefugePalette,
    actions: List<RefugeFloatingAction>,
    modifier: Modifier = Modifier,
    badges: Map<Int, Int> = emptyMap(),
) {
    if (actions.isEmpty()) return

    // Keep the resting capsule compact. A pressed cell grows toward the
    // reference 44 dp touch size and the connected capsule follows it, which
    // fixes the old right-edge overflow while preserving a generous hit area.
    val interactionSources = remember(actions) { actions.map { MutableInteractionSource() } }
    val pressed = interactionSources.map { it.collectIsPressedAsState().value }
    val widths = actions.indices.map { index ->
        animateDpAsState(
            // Keep a little more air around adjacent glyphs in the compact
            // header capsule.  The resting width remains smaller than the
            // expanded touch target, while avoiding the cramped three-icon
            // silhouette on Store and Terminal.
            // Keep the resting capsule compact while preserving the 44dp
            // minimum touch target.  The previous 52dp cells made the two
            // right-side actions read as a wide toolbar instead of one
            // connected control.
            targetValue = if (pressed[index]) 48.dp else 44.dp,
            animationSpec = tween(durationMillis = 120),
            label = "header-action-width",
        ).value
    }
    val capsuleWidth = widths.fold(0.dp) { total, width -> total + width } +
        2.dp * (actions.size - 1).coerceAtLeast(0)
    Box(
        modifier
            .width(capsuleWidth)
            // The 42dp capsule sits between the 48dp avatar and the title's
            // line box while sharing the header's vertical center.
            .height(44.dp),
    ) {
        OfficialLiquidButtonPort(
            onClick = {}, backdrop = backdrop, modifier = Modifier.fillMaxSize(),
            handlesClick = false, isInteractive = true, enablePressHighlight = true,
            // Let the backdrop effect provide the material. An opaque theme
            // fill makes the connected header bar read as a flat black pill.
            surfaceColor = Color.Unspecified,
            visualHeight = 44.dp, contentPadding = 0.dp,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            actions.forEachIndexed { index, action ->
                Box(
                    Modifier
                        .width(widths[index]).height(44.dp)
                        .semantics { contentDescription = action.label; role = Role.Button }
                        .clickable(enabled = action.enabled, interactionSource = interactionSources[index], indication = null, onClick = action.onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(action.icon, null, Modifier.size(20.dp),
                        tint = if (!action.enabled) palette.textMuted else if (action.tint.isSpecified) action.tint else palette.text)
                }
            }
        }
        actions.indices.forEach { index ->
            badges[index]?.takeIf { it > 0 }?.let { count ->
                val rightEdge = widths.take(index + 1).fold(0.dp) { total, width -> total + width }
                Box(
                    Modifier.align(Alignment.TopStart).offset(x = rightEdge - 18.dp, y = 0.dp)
                        .zIndex(20f).size(18.dp).background(Color(0xFFFF8A00), CircleShape)
                        .border(1.dp, palette.background.copy(alpha = .78f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (count > 99) "99+" else count.toString(),
                        style = com.refuge.next.design.RefugeTypography.caption(palette).copy(
                            color = Color.White,
                            fontSize = if (count > 9) 7.sp else 9.sp,
                        ),
                        maxLines = 1,
                    )
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
    // The legacy list controls are 44 dp tall. Keep their visual height
    // separate from the larger primary actions used inside sheets.
    visualHeight: Dp = 34.dp,
) {
    OfficialLiquidButtonPort(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier
            .semantics { role = Role.Button; contentDescription = label },
        tint = Color.Unspecified,
        // Compact means visually light, not undersized. Keep the same 44dp
        // touch/readability baseline as the other text actions.
        visualHeight = visualHeight,
        contentPadding = 9.dp,
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = { fadeIn(tween(90)) togetherWith fadeOut(tween(90)) },
            label = "utility-pill-icon",
        ) { currentIcon ->
            Icon(currentIcon, null, tint = palette.textSecondary, modifier = Modifier.size(15.dp))
        }
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn(tween(90)) togetherWith fadeOut(tween(90)) },
            label = "utility-pill-label",
        ) { currentLabel ->
            Text(
                currentLabel,
                style = com.refuge.next.design.RefugeTypography.secondary(palette).copy(color = palette.textSecondary),
                maxLines = 1,
            )
        }
    }
}

/** One quiet content-layer lens for an inventory list; rows do not create glass. */
@Composable
fun InventoryGlassGroup(
    backdrop: Backdrop,
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = RefugeSpacing.md),
    roundTop: Boolean = true,
    roundBottom: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    RefugeGlassListGroup(
        backdrop = backdrop,
        palette = palette,
        modifier = modifier,
        padding = padding,
        roundTop = roundTop,
        roundBottom = roundBottom,
        content = content,
    )
}

/** Connected content surface; each row can be virtualized without a shader. */
@Composable
fun RefugeGlassListGroup(
    backdrop: Backdrop,
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = RefugeSpacing.md),
    roundTop: Boolean = true,
    roundBottom: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val topRadius = if (roundTop) RefugeRadius.panel else 0.dp
    val bottomRadius = if (roundBottom) RefugeRadius.panel else 0.dp
    val shape = refugeContinuousShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomEnd = bottomRadius,
        bottomStart = bottomRadius,
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(palette.contentSurface)
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

/** Lightweight row interaction that reuses its parent group's glass capture. */
@Composable
fun RefugeGlassListRow(
    palette: RefugePalette,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isLast: Boolean = false,
    dividerInset: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressAlpha = animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "glass-list-row-press",
    )
    // A white overlay has almost no perceptual change on the light canvas.
    // Reuse the semantic accent for a restrained, readable touch response.
    val pressColor = if (palette.background.luminance() < .5f) {
        palette.glassStrong
    } else {
        palette.accent
    }
    Box(
        modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(pressColor.copy(alpha = .12f * pressAlpha.value))
            }
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        content()
        if (!isLast) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .padding(start = dividerInset)
                    .height(1.dp)
                    .background(palette.text.copy(alpha = if (palette.background.luminance() < .5f) .06f else .12f)),
            )
        }
    }
}

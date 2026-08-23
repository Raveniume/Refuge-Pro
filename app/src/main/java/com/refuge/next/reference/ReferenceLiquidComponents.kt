package com.refuge.next.reference

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule

private val ReferenceAccent = Color(0xFF0088FF)

/** Compatibility name for the lab; the implementation is the official port. */
@Composable
fun ReferenceLiquidButton(
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
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
        surfaceColor = surfaceColor,
        visualHeight = minHeight.coerceAtLeast(32.dp),
        contentPadding = horizontalPadding,
        content = content,
    )
}

/** Official moving-lens segmented primitive shared by production and the lab. */
@Composable
fun ReferenceLiquidSelectionBar(
    backdrop: Backdrop,
    isDark: Boolean,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    initialIndex: Int = 0,
    onSelected: (Int) -> Unit = {},
    content: @Composable RowScope.(selectedIndex: Int, select: (Int) -> Unit) -> Unit,
) {
    OfficialLiquidSegmentedPort(
        selectedIndex = initialIndex,
        onSelected = onSelected,
        backdrop = backdrop,
        tabsCount = tabsCount,
        isDark = isDark,
        modifier = modifier,
        outerHeight = height,
        content = content,
    )
}

/** Production bottom navigation is a direct AndroidLiquidGlass component port. */
@Composable
fun ReferenceLiquidBottomTabs(
    backdrop: Backdrop,
    isDark: Boolean,
    tabsCount: Int,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(selectedIndex: Int, select: (Int) -> Unit) -> Unit,
) {
    OfficialLiquidBottomTabsPort(
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        backdrop = backdrop,
        tabsCount = tabsCount,
        isDark = isDark,
        modifier = modifier,
        content = content,
    )
}

@Composable
fun ReferenceSegmentedControl(
    backdrop: Backdrop,
    isDark: Boolean,
    labels: List<String>,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
    onSelected: (Int) -> Unit = {},
) {
    ReferenceLiquidSelectionBar(
        backdrop = backdrop,
        isDark = isDark,
        tabsCount = labels.size,
        modifier = modifier,
        height = 48.dp,
        initialIndex = initialIndex,
        onSelected = onSelected,
    ) { selected, select ->
        labels.forEachIndexed { index, label ->
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .semantics {
                        this.selected = index == selected
                        contentDescription = label
                    }
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Tab,
                    ) { select(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (index == selected) ReferenceAccent else if (isDark) {
                        Color.White.copy(alpha = .78f)
                    } else {
                        Color.Black.copy(alpha = .72f)
                    },
                )
            }
        }
    }
}

@Composable
fun ReferenceSearchField(
    backdrop: Backdrop,
    isDark: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    searchIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    val textColor = if (isDark) Color.White else Color.Black
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = textColor),
        modifier = modifier.semantics { contentDescription = "Search" },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(16.dp) },
                        effects = { vibrancy(); blur(2.dp.toPx()); lens(8.dp.toPx(), 12.dp.toPx()) },
                        onDrawSurface = {
                            drawRect(if (isDark) Color.White.copy(alpha = .12f) else Color.White.copy(alpha = .58f))
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(searchIcon, contentDescription = null, tint = if (isDark) Color.White.copy(alpha = .78f) else Color.Black.copy(alpha = .58f), modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f).padding(start = 8.dp)) {
                    if (value.isEmpty()) Text("Search", color = textColor.copy(alpha = .55f))
                    inner()
                }
            }
        },
    )
}

@Composable
fun ReferenceOpticalTest(
    backdrop: Backdrop,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val traceBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = rememberCombinedBackdrop(backdrop, traceBackdrop)
    val shape = RoundedCornerShape(20.dp)
    val trace = if (isDark) Color.White.copy(alpha = .64f) else Color.Black.copy(alpha = .48f)
    Box(
        modifier.fillMaxWidth().height(148.dp).clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize().layerBackdrop(traceBackdrop)) {
            drawRect(if (isDark) Color.Black.copy(alpha = .28f) else Color.White.copy(alpha = .34f))
            drawLine(trace, Offset(-20f, size.height * .30f), Offset(size.width + 20f, size.height * .66f), 3.dp.toPx(), StrokeCap.Round)
            drawLine(trace.copy(alpha = trace.alpha * .58f), Offset(-20f, size.height * .78f), Offset(size.width + 20f, size.height * .42f), 2.dp.toPx(), StrokeCap.Round)
        }
        Box(
            Modifier.fillMaxWidth(.62f).height(88.dp).drawBackdrop(
                backdrop = combinedBackdrop,
                shape = { RoundedCornerShape(24.dp) },
                effects = { vibrancy(); blur(2.dp.toPx()); lens(18.dp.toPx(), 28.dp.toPx()) },
                onDrawSurface = { drawRect(if (isDark) Color.White.copy(alpha = .07f) else Color.White.copy(alpha = .24f)) },
            ),
        )
    }
}

@Composable
fun RowScope.ReferenceSelectionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier.weight(1f).height(48.dp)
            .semantics { this.selected = selected; contentDescription = label }
            .clickable(interactionSource = null, indication = null, role = Role.Tab, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val tint = if (selected) ReferenceAccent else if (isDark) Color.White.copy(alpha = .78f) else Color.Black.copy(alpha = .72f)
        // Keep the idle glyph optically smaller than the selected lens glyph;
        // custom hollow terminal paths otherwise read larger than GridView.
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(if (selected) 21.dp else 17.dp))
        Text(label, color = tint, style = TextStyle(fontSize = 10.sp, lineHeight = 12.sp))
    }
}

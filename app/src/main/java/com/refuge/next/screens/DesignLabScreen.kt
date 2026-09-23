package com.refuge.next.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.R
import com.refuge.next.design.RefugeIconSize
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import com.refuge.next.material.RefugeDialog
import com.refuge.next.material.RefugeGlassControl
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeLiquidGlass
import com.refuge.next.material.RefugeLiquidGlassButton
import com.refuge.next.material.RefugeModalSurface
import com.refuge.next.material.RefugeOpticalTest
import com.refuge.next.material.RefugeQuietControl
import com.refuge.next.material.RefugeSegmentedControl

@Composable
fun DesignLabScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
) {
    var selectedSegment by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var showAlert by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = RefugeSpacing.page,
            top = RefugeSpacing.lg,
            end = RefugeSpacing.page,
            bottom = 110.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(RefugeSpacing.section),
    ) {
        item { LabHeader(backdrop, palette, isDark, onToggleTheme) }
        item {
            LabSectionTitle("Segmented Control", "Selection follows the Apple control geometry.", palette)
            Spacer(Modifier.height(RefugeSpacing.sm))
            RefugeSegmentedControl(
                backdrop = backdrop,
                palette = palette,
                labels = listOf("组件", "材质", "动效"),
                selectedIndex = selectedSegment,
                onSelected = { selectedSegment = it },
            )
            Spacer(Modifier.height(RefugeSpacing.sm))
            Text("Selected state is one moving lens, not a filled capsule.", style = RefugeTypography.secondary(palette))
        }
        item {
            LabSectionTitle("Liquid Glass lens", "The same pipeline is used by the real controls.", palette)
            Spacer(Modifier.height(RefugeSpacing.sm))
            RefugeOpticalTest(backdrop, palette)
            Spacer(Modifier.height(RefugeSpacing.xs))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OpticalLegend("NORMAL", palette.textMuted, palette)
                OpticalLegend("EDGE REFRACTION", palette.accent, palette)
                OpticalLegend("NORMAL", palette.textMuted, palette)
            }
        }
        item {
            LabSectionTitle("Search / Field", "Functional glass stays quiet and easy to scan.", palette)
            Spacer(Modifier.height(RefugeSpacing.sm))
            BasicTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                textStyle = RefugeTypography.body(palette),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    RefugeLiquidGlass(
                        backdrop = backdrop,
                        palette = palette,
                        modifier = Modifier.fillMaxWidth(),
                        radius = RefugeRadius.control,
                        padding = PaddingValues(horizontal = RefugeSpacing.md, vertical = 12.dp),
                        refractionHeight = 11.dp,
                        refractionAmount = 16.dp,
                        blurRadius = 3.dp,
                        surfaceAlpha = .026f,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(RefugeIcons.search, "搜索", tint = palette.accent, modifier = Modifier.size(RefugeIconSize.medium))
                            Spacer(Modifier.width(RefugeSpacing.sm))
                            Box(Modifier.weight(1f)) {
                                if (search.isEmpty()) Text("搜索舰船或项目", style = RefugeTypography.body(palette))
                                innerTextField()
                            }
                        }
                    }
                },
            )
            Spacer(Modifier.height(RefugeSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                RefugeQuietControl(backdrop, palette, { showSheet = true }, contentDescription = "筛选") {
                    Icon(RefugeIcons.filter, null, tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("筛选", style = RefugeTypography.secondary(palette).copy(color = palette.textSecondary))
                }
                RefugeQuietControl(backdrop, palette, { showAlert = true }, contentDescription = "排序：默认") {
                    Icon(RefugeIcons.sort, null, tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("排序：默认", style = RefugeTypography.secondary(palette).copy(color = palette.textSecondary))
                }
            }
        }
        item {
            LabSectionTitle("Floating Icon Button", "A single elevated action with a 44dp touch target.", palette)
            Spacer(Modifier.height(RefugeSpacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RefugeLiquidGlassButton(
                    backdrop = backdrop,
                    palette = palette,
                    onClick = { showAlert = true },
                    contentDescription = "打开通知",
                    modifier = Modifier.size(52.dp),
                    radius = 18.dp,
                    padding = PaddingValues(0.dp),
                ) {
                    Icon(RefugeIcons.notification, null, tint = palette.accent, modifier = Modifier.size(RefugeIconSize.medium))
                }
                Spacer(Modifier.width(RefugeSpacing.md))
                Text("Idle uses neutral refraction; edge color only responds to interaction.", style = RefugeTypography.secondary(palette))
            }
        }
        item {
            LabSectionTitle("Sheet / Alert", "Content is opaque enough to keep the underlying page out.", palette)
            Spacer(Modifier.height(RefugeSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                RefugeGlassControl(
                    backdrop = backdrop,
                    palette = palette,
                    onClick = { showSheet = true },
                    contentDescription = "打开 Sheet",
                    padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) { Text("Sheet", style = RefugeTypography.body(palette).copy(color = palette.text)) }
                RefugeGlassControl(
                    backdrop = backdrop,
                    palette = palette,
                    onClick = { showAlert = true },
                    contentDescription = "打开 Alert",
                    padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) { Text("Alert", style = RefugeTypography.body(palette).copy(color = palette.text)) }
            }
        }
    }

    if (showAlert) {
        RefugeDialog(
            backdrop = backdrop,
            palette = palette,
            title = "同步已准备",
            body = "这是独立的 alert material，动作仍然使用共享 Liquid Glass。",
            primaryLabel = "知道了",
            onDismiss = { showAlert = false },
            onPrimary = { showAlert = false },
        )
    }
    if (showSheet) DesignLabSheet(backdrop, palette) { showSheet = false }
}

@Composable
private fun LabHeader(backdrop: LayerBackdrop, palette: RefugePalette, isDark: Boolean, onToggleTheme: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(palette.contentSurfaceStrong), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.refuge_avatar_placeholder),
                contentDescription = "用户头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(42.dp).scale(2.14f).clip(CircleShape),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("组件实验室", style = RefugeTypography.largeTitle(palette))
            Text("iOS 27 reference · Refuge adaptation", style = RefugeTypography.secondary(palette))
        }
        RefugeLiquidGlassButton(
            backdrop = backdrop,
            palette = palette,
            onClick = onToggleTheme,
            contentDescription = "切换主题",
            modifier = Modifier.size(44.dp),
            radius = 16.dp,
            padding = PaddingValues(0.dp),
        ) {
            Icon(if (isDark) RefugeIcons.light else RefugeIcons.dark, null, tint = palette.accent)
        }
    }
}

@Composable
private fun LabSectionTitle(title: String, note: String, palette: RefugePalette) {
    Text(title, style = RefugeTypography.title(palette))
    Text(note, style = RefugeTypography.secondary(palette))
}

@Composable
private fun OpticalLegend(text: String, color: Color, palette: RefugePalette) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text, style = RefugeTypography.caption(palette).copy(color = color))
    }
}

@Composable
private fun DesignLabSheet(backdrop: LayerBackdrop, palette: RefugePalette, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(palette.scrim), contentAlignment = Alignment.BottomCenter) {
            RefugeModalSurface(
                palette = palette,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                fill = palette.contentSurfaceStrong,
                padding = PaddingValues(RefugeSpacing.xl),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md)) {
                    Box(Modifier.width(32.dp).height(3.dp).background(palette.textMuted, RoundedCornerShape(2.dp)))
                    Text("筛选", style = RefugeTypography.title(palette))
                    Text("Sheet content uses a stable base material and does not expose the page below.", style = RefugeTypography.body(palette))
                    RefugeGlassControl(
                        backdrop = backdrop,
                        palette = palette,
                        onClick = onDismiss,
                        contentDescription = "完成 Sheet",
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("完成", style = RefugeTypography.body(palette).copy(color = palette.text)) }
                }
            }
        }
    }
}

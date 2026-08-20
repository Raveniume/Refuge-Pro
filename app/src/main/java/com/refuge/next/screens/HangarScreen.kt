package com.refuge.next.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.data.HangarItem
import com.refuge.next.data.HangarRepository
import com.refuge.next.data.OwnedShip
import com.refuge.next.design.RefugeIconSize
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import com.refuge.next.material.RefugeContentSurface
import com.refuge.next.material.RefugeGlassControl
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeImagePlaceholder
import com.refuge.next.material.RefugeM80Thumbnail
import com.refuge.next.material.RefugeModalSurface
import com.refuge.next.material.RefugeSegmentedControl

@Composable
fun HangarScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    repository: HangarRepository,
    onOpenDesignLab: () -> Unit,
) {
    var ownedShips by remember { mutableStateOf(emptyList<OwnedShip>()) }
    var inventory by remember { mutableStateOf(emptyList<HangarItem>()) }
    var selectedSegment by remember { mutableIntStateOf(0) }
    var showFilter by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }

    LaunchedEffect(repository) {
        ownedShips = repository.ownedShips()
        inventory = repository.inventory()
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                start = RefugeSpacing.page,
                top = RefugeSpacing.lg,
                end = RefugeSpacing.page,
                bottom = 112.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.lg),
        ) {
            item {
                HangarHeader(
                    backdrop = backdrop,
                    palette = palette,
                    onOpenDesignLab = onOpenDesignLab,
                )
            }
            item {
                RefugeSegmentedControl(
                    backdrop = backdrop,
                    palette = palette,
                    labels = listOf("机库", "回购", "升级"),
                    selectedIndex = selectedSegment,
                    onSelected = { selectedSegment = it },
                )
            }
            items(ownedShips, key = { it.name }) { ship ->
                OwnedShipHero(
                    palette = palette,
                    ship = ship,
                    onClick = { showDetail = true },
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("舰船清单", style = RefugeTypography.headline(palette))
                        Text("${inventory.size} 个已同步项目", style = RefugeTypography.caption(palette))
                    }
                    Spacer(Modifier.width(RefugeSpacing.sm))
                    RefugeGlassControl(
                        backdrop = backdrop,
                        palette = palette,
                        onClick = { showFilter = true },
                        contentDescription = "筛选舰库",
                        modifier = Modifier.size(46.dp),
                        padding = PaddingValues(0.dp),
                    ) {
                        Icon(RefugeIcons.filter, null, tint = palette.text)
                    }
                    Spacer(Modifier.width(RefugeSpacing.xs))
                    RefugeGlassControl(
                        backdrop = backdrop,
                        palette = palette,
                        onClick = { showSort = true },
                        contentDescription = "排序舰库",
                        modifier = Modifier.size(46.dp),
                        padding = PaddingValues(0.dp),
                    ) {
                        Icon(RefugeIcons.sort, null, tint = palette.text)
                    }
                }
            }
            item {
                RefugeContentSurface(
                    palette = palette,
                    modifier = Modifier.fillMaxWidth(),
                    radius = RefugeRadius.panel,
                    fill = palette.contentSurface,
                    padding = PaddingValues(horizontal = RefugeSpacing.md),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        inventory.forEach { item ->
                            HangarInventoryRow(palette = palette, item = item)
                        }
                    }
                }
            }
        }
    }

    if (showFilter) FilterSheet(backdrop, palette, onDismiss = { showFilter = false })
    if (showSort) {
        RefugeModalDialog(
            backdrop = backdrop,
            palette = palette,
            title = "排序舰库",
            body = "默认顺序按同步时间与项目价值保持稳定。",
            primaryLabel = "完成",
            onDismiss = { showSort = false },
            onPrimary = { showSort = false },
        )
    }
    if (showDetail) {
        RefugeModalDialog(
            backdrop = backdrop,
            palette = palette,
            title = "M80",
            body = "详情、赠送、回收与升级动作将在业务 adapter 接入后启用。",
            primaryLabel = "关闭",
            onDismiss = { showDetail = false },
            onPrimary = { showDetail = false },
        )
    }
}

@Composable
private fun HangarHeader(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    onOpenDesignLab: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(palette.accentSoft)
                .border(1.dp, palette.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(RefugeIcons.profile, "用户资料", tint = palette.accent, modifier = Modifier.size(RefugeIconSize.medium))
        }
        Spacer(Modifier.width(RefugeSpacing.md))
        Column(Modifier.weight(1f)) {
            Text("我的机库", style = RefugeTypography.largeTitle(palette))
            Text("舰队资料 · 本地同步", style = RefugeTypography.secondary(palette))
        }
        RefugeGlassControl(
            backdrop = backdrop,
            palette = palette,
            onClick = onOpenDesignLab,
            contentDescription = "打开 Design Lab",
            modifier = Modifier.size(46.dp),
            padding = PaddingValues(0.dp),
        ) {
            Icon(RefugeIcons.more, null, tint = palette.textSecondary)
        }
    }
}

@Composable
private fun OwnedShipHero(
    palette: RefugePalette,
    ship: OwnedShip,
    onClick: () -> Unit,
) {
    RefugeContentSurface(
        palette = palette,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        radius = RefugeRadius.hero,
        fill = palette.contentSurfaceStrong,
        padding = PaddingValues(RefugeSpacing.sm),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RefugeM80Thumbnail(
                palette = palette,
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(Modifier.weight(1f)) {
                Text("FEATURED SHIP", style = RefugeTypography.caption(palette).copy(color = palette.accent))
                Spacer(Modifier.height(RefugeSpacing.xxs))
                Text(ship.name, style = RefugeTypography.title(palette))
                Text(ship.packageName, style = RefugeTypography.secondary(palette), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(RefugeSpacing.sm))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.md)) {
                    HeroMetric("舰值", ship.currentValue, palette)
                    HeroMetric("已付", ship.paidValue, palette)
                    HeroMetric("保险", ship.insurance, palette, palette.positive)
                }
            }
        }
    }
}

@Composable
private fun RowScope.HeroMetric(
    label: String,
    value: String,
    palette: RefugePalette,
    color: Color = palette.text,
) {
    Column(Modifier.weight(1f)) {
        Text(label, style = RefugeTypography.caption(palette))
        Text(value, style = RefugeTypography.value(palette).copy(color = color))
    }
}

@Composable
private fun HangarInventoryRow(
    palette: RefugePalette,
    item: HangarItem,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = RefugeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.title.contains("M80")) {
            RefugeM80Thumbnail(palette = palette, modifier = Modifier.size(68.dp))
        } else {
            RefugeImagePlaceholder(palette = palette, modifier = Modifier.size(68.dp))
        }
        Spacer(Modifier.width(RefugeSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = RefugeTypography.headline(palette),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(RefugeSpacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.price, style = RefugeTypography.value(palette))
                Spacer(Modifier.width(RefugeSpacing.md))
                Text(item.date, style = RefugeTypography.secondary(palette))
                Spacer(Modifier.weight(1f))
                InventoryAction(RefugeIcons.gift, "赠送", palette, item.isGiftable)
                InventoryAction(RefugeIcons.reclaim, "回收", palette, item.isReclaimable)
                InventoryAction(RefugeIcons.chevron, "查看详情", palette, true)
            }
            Spacer(Modifier.height(RefugeSpacing.sm))
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
        }
    }
}

@Composable
private fun InventoryAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    palette: RefugePalette,
    enabled: Boolean,
) {
    Box(
        Modifier
            .size(38.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (enabled) palette.textSecondary else palette.textMuted.copy(alpha = .42f), modifier = Modifier.size(RefugeIconSize.small))
    }
}

@Composable
private fun FilterSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(setOf("舰船", "可回收")) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            RefugeModalSurface(
                palette = palette,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 22.dp),
                radius = RefugeRadius.floating,
                fill = palette.backgroundLight,
                padding = PaddingValues(RefugeSpacing.xl),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md)) {
                    Box(Modifier.width(34.dp).height(4.dp).background(palette.outline, RoundedCornerShape(2.dp)))
                    Text("筛选舰库", style = RefugeTypography.title(palette))
                    Text("按项目类型和可用操作缩小清单", style = RefugeTypography.secondary(palette))
                    listOf("舰船", "可回收", "可赠送").forEach { label ->
                        val checked = label in selected
                        Row(
                            Modifier.fillMaxWidth().clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                selected = if (checked) selected - label else selected + label
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (checked) palette.accent else Color.Transparent)
                                    .border(1.dp, if (checked) palette.accent else palette.outline, RoundedCornerShape(7.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (checked) Icon(RefugeIcons.check, null, tint = palette.background, modifier = Modifier.size(RefugeIconSize.small))
                            }
                            Spacer(Modifier.width(RefugeSpacing.md))
                            Text(label, style = RefugeTypography.body(palette))
                        }
                    }
                    RefugeGlassControl(
                        backdrop = backdrop,
                        palette = palette,
                        onClick = onDismiss,
                        contentDescription = "完成筛选",
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("完成", style = RefugeTypography.body(palette).copy(color = palette.text))
                    }
                }
            }
        }
    }
}

@Composable
private fun RefugeModalDialog(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    title: String,
    body: String,
    primaryLabel: String,
    onDismiss: () -> Unit,
    onPrimary: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        RefugeModalSurface(
            palette = palette,
            modifier = Modifier.fillMaxWidth(.88f),
            fill = palette.backgroundLight,
            padding = PaddingValues(RefugeSpacing.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md)) {
                Text(title, style = RefugeTypography.title(palette))
                Text(body, style = RefugeTypography.body(palette))
                RefugeGlassControl(
                    backdrop = backdrop,
                    palette = palette,
                    onClick = onPrimary,
                ) { Text(primaryLabel, style = RefugeTypography.body(palette)) }
            }
        }
    }
}

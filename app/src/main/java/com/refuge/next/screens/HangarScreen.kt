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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.R
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
import com.refuge.next.material.RefugeModalSurface
import com.refuge.next.material.RefugeQuietControl
import com.refuge.next.reference.ReferenceLiquidSelectionBar
import com.refuge.next.reference.ReferenceSelectionItem
import com.refuge.next.reference.ReferenceSegmentedControl

@Composable
fun HangarScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    repository: HangarRepository,
    isDark: Boolean,
    selectedBottomTab: Int,
    onNavigate: (Int) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenDesignLab: () -> Unit,
) {
    var ownedShips by remember { mutableStateOf(emptyList<OwnedShip>()) }
    var inventory by remember { mutableStateOf(emptyList<HangarItem>()) }
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
                bottom = 132.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.lg),
        ) {
            item {
                HangarHeader(
                    backdrop = backdrop,
                    palette = palette,
                    onToggleTheme = onToggleTheme,
                    onOpenDesignLab = onOpenDesignLab,
                )
            }
            item {
                ReferenceSegmentedControl(
                    backdrop = backdrop,
                    isDark = isDark,
                    labels = listOf("机库", "回购", "升级"),
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
                HangarListHeader(
                    backdrop = backdrop,
                    palette = palette,
                    count = inventory.size,
                    onFilter = { showFilter = true },
                    onSort = { showSort = true },
                )
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
                        inventory.forEachIndexed { index, item ->
                            HangarInventoryRow(
                                palette = palette,
                                item = item,
                                isLast = index == inventory.lastIndex,
                            )
                        }
                    }
                }
            }
        }

        ReferenceLiquidSelectionBar(
            backdrop = backdrop,
            isDark = isDark,
            tabsCount = 3,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(.92f)
                .navigationBarsPadding()
                .padding(bottom = 10.dp),
        ) { selected, select ->
            LaunchedEffect(selected, selectedBottomTab) {
                if (selected != selectedBottomTab) onNavigate(selected)
            }
            listOf(
                RefugeIcons.home to "机库",
                RefugeIcons.design to "设计",
                RefugeIcons.tools to "工具",
            ).forEachIndexed { index, (icon, label) ->
                ReferenceSelectionItem(
                    icon = icon,
                    label = label,
                    selected = index == selectedBottomTab && index == selected,
                    isDark = isDark,
                    onClick = {
                        select(index)
                        onNavigate(index)
                    },
                )
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
    onToggleTheme: () -> Unit,
    onOpenDesignLab: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.user_profile_pic),
            contentDescription = "用户头像",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .graphicsLayer { scaleX = 1.9f; scaleY = 1.9f }
                .border(1.dp, palette.outline, CircleShape),
        )
        Spacer(Modifier.width(RefugeSpacing.md))
        Column(Modifier.weight(1f)) {
            Text("我的机库", style = RefugeTypography.largeTitle(palette))
            Text("舰队资料 · 本地同步", style = RefugeTypography.secondary(palette))
        }
        RefugeGlassControl(
            backdrop = backdrop,
            palette = palette,
            onClick = onToggleTheme,
            contentDescription = "切换明暗主题",
            modifier = Modifier.size(48.dp),
            padding = PaddingValues(0.dp),
        ) {
            Icon(
                if (palette.background == com.refuge.next.design.RefugeColors.dark.background) RefugeIcons.light else RefugeIcons.dark,
                "切换明暗主题",
                tint = palette.textSecondary,
            )
        }
        Spacer(Modifier.width(RefugeSpacing.xs))
        RefugeGlassControl(
            backdrop = backdrop,
            palette = palette,
            onClick = onOpenDesignLab,
            contentDescription = "打开 Design Lab",
            modifier = Modifier.size(48.dp),
            padding = PaddingValues(0.dp),
        ) {
            Icon(RefugeIcons.more, "更多操作", tint = palette.textSecondary)
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
            HangarImage(
                imageRes = ship.imageRes,
                contentDescription = "${ship.name} 图片",
                modifier = Modifier.size(92.dp),
            )
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(Modifier.weight(1f)) {
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
private fun HangarListHeader(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    count: Int,
    onFilter: () -> Unit,
    onSort: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("舰船清单", style = RefugeTypography.headline(palette))
        Spacer(Modifier.width(RefugeSpacing.xs))
        Text("$count 项", style = RefugeTypography.caption(palette))
        Spacer(Modifier.weight(1f))
        RefugeQuietControl(
            backdrop = backdrop,
            palette = palette,
            onClick = onFilter,
            contentDescription = "筛选",
            modifier = Modifier.height(40.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(RefugeIcons.filter, null, tint = palette.textSecondary, modifier = Modifier.size(RefugeIconSize.small))
                Text("筛选", style = RefugeTypography.secondary(palette).copy(color = palette.textSecondary))
            }
        }
        Spacer(Modifier.width(RefugeSpacing.xs))
        RefugeQuietControl(
            backdrop = backdrop,
            palette = palette,
            onClick = onSort,
            contentDescription = "排序：默认",
            modifier = Modifier.height(40.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(RefugeIcons.sort, null, tint = palette.textSecondary, modifier = Modifier.size(RefugeIconSize.small))
                Text("排序：默认", style = RefugeTypography.secondary(palette).copy(color = palette.textSecondary))
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
    isLast: Boolean,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(116.dp)
            .semantics { contentDescription = item.title },
    ) {
        Row(
            Modifier.fillMaxSize().padding(vertical = RefugeSpacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            HangarImage(
                imageRes = item.imageRes,
                contentDescription = "${item.title} 图片",
                modifier = Modifier.size(88.dp),
            )
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(Modifier.fillMaxSize()) {
                Text(
                    item.title,
                    style = RefugeTypography.headline(palette),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item.price, style = RefugeTypography.value(palette))
                    Spacer(Modifier.width(RefugeSpacing.md))
                    Text(item.date, style = RefugeTypography.secondary(palette), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.weight(1f))
                    InventoryAction(RefugeIcons.gift, "赠送", palette, item.isGiftable)
                    InventoryAction(RefugeIcons.reclaim, "回收", palette, item.isReclaimable)
                    InventoryAction(RefugeIcons.chevron, "查看详情", palette, true)
                }
            }
        }
        if (!isLast) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .padding(start = 104.dp)
                    .height(1.dp)
                    .background(palette.divider),
            )
        }
    }
}

@Composable
private fun HangarImage(
    imageRes: Int,
    contentDescription: String,
    modifier: Modifier,
) {
    Image(
        painter = painterResource(imageRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(RefugeRadius.image)),
    )
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
            .size(36.dp)
            .semantics {
                role = Role.Button
                contentDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            null,
            tint = if (enabled) palette.textSecondary else palette.textMuted.copy(alpha = .42f),
            modifier = Modifier.size(RefugeIconSize.small),
        )
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

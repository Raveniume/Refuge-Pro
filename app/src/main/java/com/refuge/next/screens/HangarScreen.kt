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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.refuge.next.material.PageGlassScope
import com.refuge.next.material.RefugeCompactUtilityPill
import com.refuge.next.material.RefugeGlassControl
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeLightweightGlassSurface
import com.refuge.next.material.RefugeModalSurface
import com.refuge.next.material.RefugeLiquidSheet
import com.refuge.next.material.RefugeLiquidSegmented
import com.refuge.next.material.RefugeFloatingAction
import com.refuge.next.material.RefugeFloatingActionGroup
import com.refuge.next.material.RefugeLiquidIconButton
import com.refuge.next.reference.ReferenceLiquidSelectionBar
import com.refuge.next.reference.ReferenceLiquidButton
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
    onOpenCcu: () -> Unit,
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
) {
    var ownedShips by remember { mutableStateOf(emptyList<OwnedShip>()) }
    var inventory by remember { mutableStateOf(emptyList<HangarItem>()) }
    var showFilter by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var selectedDetail by remember { mutableStateOf<HangarDetail?>(null) }
    var showLogs by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(0) }

    LaunchedEffect(repository) {
        ownedShips = repository.ownedShips()
        inventory = repository.inventory()
    }

    PageGlassScope(
        backdrop = backdrop,
        content = {
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
                    isOnline = isOnline,
                    onToggleOnline = onToggleOnline,
                )
            }
            item {
                RefugeLiquidSegmented(
                    backdrop = backdrop,
                    isDark = isDark,
                    labels = listOf("机库", "回购", "升级"),
                    initialIndex = selectedSection,
                    onSelected = { selectedSection = it },
                )
            }
            if (selectedSection == 0) {
                items(ownedShips, key = { it.name }) { ship ->
                    OwnedShipHero(
                        palette = palette,
                        ship = ship,
                        onClick = {
                            selectedDetail = detailForShip(ship)
                        },
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
                                    onClick = { selectedDetail = item.toHangarDetail() },
                                )
                            }
                        }
                    }
                }
                item {
                    RefugeLightweightGlassSurface(
                        palette = palette,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showLogs = true },
                        contentDescription = "机库日志",
                        padding = PaddingValues(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(RefugeIcons.log, null, tint = palette.accent)
                            Spacer(Modifier.width(RefugeSpacing.sm))
                            Column(Modifier.weight(1f)) {
                                Text("机库日志", style = RefugeTypography.body(palette).copy(color = palette.text))
                                Text("查看赠送、回收、购买与升级记录", style = RefugeTypography.caption(palette))
                            }
                            Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
                        }
                    }
                }
            } else if (selectedSection == 1) {
                items(rebuyItems, key = { it.title }) { item ->
                    HangarRebuyRow(palette, item) { selectedDetail = item.toHangarDetail() }
                }
            } else {
                item {
                    HangarUpgradePanel(backdrop, palette) { onOpenCcu() }
                }
            }
        }

        },
        overlay = { pageBackdrop ->
            RootBottomNav(pageBackdrop, isDark, selectedBottomTab, onNavigate)
        },
    )

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
    selectedDetail?.let { detail ->
        HangarDetailSheet(
            backdrop = backdrop,
            palette = palette,
            detail = detail,
            onDismiss = { selectedDetail = null },
            onUpgrade = {
                selectedDetail = null
                onOpenCcu()
            },
            onLog = {
                selectedDetail = null
                showLogs = true
            },
        )
    }
    if (showLogs) {
        ProductionListSheet(
            backdrop = backdrop,
            palette = palette,
            title = "机库日志",
            entries = listOf("CREATED · M80 · 2026-08-02", "GIFT · SteelTek 装备包 · 2026-08-16", "APPLIED_UPGRADE · M80 · 2026-08-18"),
            onDismiss = { showLogs = false },
        )
    }
}

private data class HangarDetail(
    val title: String,
    val subtitle: String,
    val price: String,
    val date: String,
    val imageRes: Int,
    val description: String,
    val isGiftable: Boolean,
    val isReclaimable: Boolean,
    val meltValue: String,
    val currentValue: String,
    val savings: String,
    val insurance: String,
    val includedItems: List<String>,
    val originalName: String = "—",
    val typeLabel: String = "本地机库项目",
    val upgradeFrom: String? = null,
    val upgradeTo: String? = null,
    val upgradeFromPrice: String? = null,
    val upgradeToPrice: String? = null,
)

private fun detailForShip(ship: OwnedShip) = HangarDetail(
    title = ship.name,
    subtitle = ship.packageName,
    price = ship.paidValue,
    date = "2026年08月02日",
    imageRes = ship.imageRes,
    description = "${ship.name} 已加入本地机库。保留原始购买、保险与礼包内容信息。",
    isGiftable = true,
    isReclaimable = true,
    meltValue = ship.paidValue,
    currentValue = ship.currentValue,
    savings = "$160",
    insurance = ship.insurance,
    includedItems = listOf("${ship.name} 游戏包", "数字下载", "${ship.insurance} 保险"),
    originalName = ship.name,
    typeLabel = "舰船 / 游戏包",
)

private fun HangarItem.toHangarDetail() = HangarDetail(
    title = title,
    subtitle = "$originalName · $typeLabel",
    price = price,
    date = date,
    imageRes = imageRes,
    description = "${title} · 已同步到本地机库。",
    isGiftable = isGiftable,
    isReclaimable = isReclaimable,
    meltValue = price,
    currentValue = currentValue,
    savings = savings,
    insurance = insurance,
    includedItems = includedItems.ifEmpty { listOf(title, "本地同步项目") },
    originalName = originalName,
    typeLabel = typeLabel,
    upgradeFrom = upgradeFrom,
    upgradeTo = upgradeTo,
    upgradeFromPrice = upgradeFromPrice,
    upgradeToPrice = upgradeToPrice,
)

private val rebuyItems = listOf(
    HangarItem("M50 - 公民新手包", "$60", "2026年07月18日", R.drawable.m80_hero, isGiftable = false, isReclaimable = false),
    HangarItem("装备包 - RSI", "$3.50", "2026年06月29日", R.drawable.ship_placeholder, isGiftable = false, isReclaimable = false),
    HangarItem("极光 Mk I ES", "$20", "2026年05月12日", R.drawable.ship_placeholder, isGiftable = false, isReclaimable = false),
)

@Composable
private fun HangarRebuyRow(palette: RefugePalette, item: HangarItem, onClick: () -> Unit) {
    RefugeLightweightGlassSurface(
        palette = palette,
        modifier = Modifier.fillMaxWidth().height(108.dp),
        radius = RefugeRadius.panel,
        onClick = onClick,
        contentDescription = item.title,
        padding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
            HangarImage(item.imageRes, "${item.title} 图片", Modifier.size(88.dp))
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(Modifier.fillMaxSize()) {
                Text(item.title, style = RefugeTypography.headline(palette), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(item.date, style = RefugeTypography.secondary(palette))
                    Spacer(Modifier.weight(1f))
                    Text(item.price, style = RefugeTypography.value(palette).copy(color = palette.accent))
                    Spacer(Modifier.width(RefugeSpacing.xs))
                    Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
                }
            }
        }
    }
}

@Composable
private fun HangarUpgradePanel(backdrop: LayerBackdrop, palette: RefugePalette, onOpen: () -> Unit) {
    RefugeContentSurface(palette = palette, modifier = Modifier.fillMaxWidth(), radius = RefugeRadius.panel, padding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md)) {
            Text("升级 / CCU", style = RefugeTypography.title(palette))
            Text("选择起始舰船和目标舰船，按已拥有 CCU 计算实际还需支付金额。", style = RefugeTypography.body(palette))
            RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.upgrade, "打开升级规划", onOpen)
        }
    }
}

@Composable
private fun HangarHeader(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    onToggleTheme: () -> Unit,
    onOpenDesignLab: () -> Unit,
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Image(
                painter = painterResource(R.drawable.user_profile_pic),
                contentDescription = "用户头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .graphicsLayer { scaleX = 1.9f; scaleY = 1.9f }
                    .semantics { contentDescription = "切换在线状态"; role = Role.Button }
                    .clickable(onClick = onToggleOnline),
            )
            Box(
                Modifier
                    .size(9.dp)
                    .background(if (isOnline) palette.positive else palette.textMuted, CircleShape)
                    .border(.5.dp, palette.background.copy(alpha = .72f), CircleShape),
            )
        }
        Spacer(Modifier.width(RefugeSpacing.md))
        Column(Modifier.weight(1f)) {
            Text("我的机库", style = RefugeTypography.largeTitle(palette))
        }
        RefugeLiquidIconButton(
            backdrop = backdrop,
            icon = if (palette.background == com.refuge.next.design.RefugeColors.dark.background) RefugeIcons.light else RefugeIcons.dark,
            contentDescription = "切换明暗主题",
            onClick = onToggleTheme,
            modifier = Modifier.size(44.dp),
            iconTint = palette.textSecondary,
        )
        Spacer(Modifier.width(RefugeSpacing.xs))
        RefugeLiquidIconButton(backdrop, RefugeIcons.more, "更多操作", onOpenDesignLab, Modifier.size(44.dp), iconTint = palette.textSecondary)
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
        RefugeCompactUtilityPill(
            backdrop = backdrop,
            palette = palette,
            icon = RefugeIcons.filter,
            label = "筛选",
            onClick = onFilter,
        )
        Spacer(Modifier.width(RefugeSpacing.xs))
        RefugeCompactUtilityPill(
            backdrop = backdrop,
            palette = palette,
            icon = RefugeIcons.sort,
            label = "排序：默认",
            onClick = onSort,
        )
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
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
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
private fun HangarDetailSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    detail: HangarDetail,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    onLog: () -> Unit,
) {
    RefugeLiquidSheet(
        backdrop = backdrop,
        palette = palette,
        title = "机库详情",
        onDismiss = onDismiss,
        action = { modalBackdrop ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
            RefugeCompactUtilityPill(
                modalBackdrop,
                palette,
                RefugeIcons.log,
                "日志",
                onLog,
                Modifier,
            )
            RefugeFloatingActionGroup(
                backdrop = modalBackdrop,
                palette = palette,
                actions = listOf(
                    RefugeFloatingAction(RefugeIcons.gift, "礼物", onDismiss),
                    RefugeFloatingAction(RefugeIcons.chevron, "跳转", onDismiss),
                    RefugeFloatingAction(RefugeIcons.upgrade, "升级", onUpgrade),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            RefugeCompactUtilityPill(
                modalBackdrop,
                palette,
                RefugeIcons.reclaim,
                "回收",
                onDismiss,
                Modifier,
            )
        },
    ) { modalBackdrop ->
        Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md)) {
                Row(verticalAlignment = Alignment.Top) {
                    HangarImage(detail.imageRes, "${detail.title} 图片", Modifier.size(112.dp))
                    Spacer(Modifier.width(RefugeSpacing.md))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xxs)) {
                        Text(detail.title, style = RefugeTypography.title(palette))
                        Text(detail.subtitle, style = RefugeTypography.secondary(palette))
                        Text("${detail.insurance} · ${detail.date}", style = RefugeTypography.caption(palette))
                    }
                }
                Text(detail.description, style = RefugeTypography.body(palette))
                DetailValueSummary(palette, detail)
                Text("内含项目", style = RefugeTypography.headline(palette))
                detail.includedItems.forEachIndexed { index, item ->
                    DetailIncludedRow(palette, detail.imageRes, item, index == detail.includedItems.lastIndex)
                }
                Text("其他信息", style = RefugeTypography.headline(palette))
                DetailMetadataRow(palette, "入库日期", detail.date)
                DetailMetadataRow(palette, "保险", detail.insurance)
                DetailMetadataRow(palette, "状态", "已同步到本地机库")
                if (detail.upgradeFrom != null && detail.upgradeTo != null) {
                    Text("升级路径", style = RefugeTypography.headline(palette))
                    DetailMetadataRow(palette, "从 ${detail.upgradeFrom}", detail.upgradeFromPrice ?: "—")
                    DetailMetadataRow(palette, "到 ${detail.upgradeTo}", detail.upgradeToPrice ?: "—")
                }
        }
    }
}

@Composable
private fun DetailValueSummary(palette: RefugePalette, detail: HangarDetail) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
        DetailMetric(palette, detail.meltValue, "可融")
        DetailMetric(palette, detail.currentValue, "当前舰值", palette.accent)
        DetailMetric(palette, detail.savings, "节省", palette.positive)
    }
}

@Composable
private fun RowScope.DetailMetric(
    palette: RefugePalette,
    value: String,
    label: String,
    color: Color = palette.text,
) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xxs)) {
        Text(value, style = RefugeTypography.value(palette).copy(color = color), maxLines = 1, softWrap = false)
        Text(label, style = RefugeTypography.caption(palette))
    }
}

@Composable
private fun DetailIncludedRow(
    palette: RefugePalette,
    imageRes: Int,
    title: String,
    isLast: Boolean,
) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = RefugeSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
            HangarImage(imageRes, "$title 图片", Modifier.size(52.dp))
            Spacer(Modifier.width(RefugeSpacing.sm))
            Text(title, style = RefugeTypography.body(palette).copy(color = palette.text), modifier = Modifier.weight(1f))
            Icon(RefugeIcons.chevron, null, tint = palette.textMuted, modifier = Modifier.size(RefugeIconSize.small))
        }
        if (!isLast) Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
    }
}

@Composable
private fun DetailMetadataRow(palette: RefugePalette, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = RefugeSpacing.xxs), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = RefugeTypography.secondary(palette))
        Spacer(Modifier.weight(1f))
        Text(value, style = RefugeTypography.body(palette).copy(color = palette.text))
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
    RefugeLiquidSheet(backdrop, palette, "筛选舰库", onDismiss) { modalBackdrop ->
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
                        .border(.5.dp, if (checked) palette.accent.copy(alpha = .42f) else palette.outline.copy(alpha = .18f), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (checked) Icon(RefugeIcons.check, null, tint = palette.background, modifier = Modifier.size(RefugeIconSize.small))
                }
                Spacer(Modifier.width(RefugeSpacing.md))
                Text(label, style = RefugeTypography.body(palette))
            }
        }
        RefugeGlassControl(modalBackdrop, palette, onDismiss, contentDescription = "完成筛选", modifier = Modifier.align(Alignment.End)) {
            Text("完成", style = RefugeTypography.body(palette).copy(color = palette.text))
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
    RefugeLiquidSheet(backdrop, palette, title, onDismiss) { modalBackdrop ->
        Text(body, style = RefugeTypography.body(palette))
        RefugeGlassControl(modalBackdrop, palette, onPrimary, contentDescription = primaryLabel) {
            Text(primaryLabel, style = RefugeTypography.body(palette))
        }
    }
}

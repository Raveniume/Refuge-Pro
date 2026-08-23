package com.refuge.next.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.mutableIntStateOf
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
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.R
import com.refuge.next.data.HangarItem
import com.refuge.next.data.HangarRepository
import com.refuge.next.data.BuybackItem
import com.refuge.next.data.BuybackRepository
import com.refuge.next.data.HangarLogRepository
import com.refuge.next.data.DestructiveAction
import com.refuge.next.data.SafeNoOpDestructiveActionExecutor
import com.refuge.next.data.OwnedShip
import com.refuge.next.data.CcuRepository
import com.refuge.next.data.CcuShip
import com.refuge.next.data.OwnedCcu
import com.refuge.next.data.calculateRemainingPayment
import com.refuge.next.data.eligibleTargetShips
import com.refuge.next.data.formatUsd
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
import com.refuge.next.material.RefugeQuietLiquidGlassSurface
import com.refuge.next.material.RefugeModalSurface
import com.refuge.next.material.RefugeLiquidSheet
import com.refuge.next.material.RefugeLiquidSegmented
import com.refuge.next.material.InventoryGlassGroup
import com.refuge.next.material.RefugeFloatingAction
import com.refuge.next.material.RefugeFloatingActionGroup
import com.refuge.next.material.RefugeLiquidIconButton
import com.refuge.next.reference.ReferenceLiquidSelectionBar
import com.refuge.next.reference.ReferenceLiquidButton
import com.refuge.next.reference.ReferenceSearchField
import com.refuge.next.reference.ReferenceSelectionItem
import com.refuge.next.reference.ReferenceSegmentedControl

@Composable
fun HangarScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    repository: HangarRepository,
    buybackRepository: BuybackRepository,
    hangarLogRepository: HangarLogRepository,
    ccuRepository: CcuRepository,
    isDark: Boolean,
    selectedBottomTab: Int,
    onNavigate: (Int) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenDesignLab: () -> Unit,
    onOpenCcu: () -> Unit,
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
) {
    var ownedShips by remember(repository) { mutableStateOf(repository.cachedOwnedShips()) }
    var inventory by remember(repository) { mutableStateOf(repository.cachedInventory()) }
    var showFilter by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var selectedDetail by remember { mutableStateOf<HangarDetail?>(null) }
    var showLogs by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var giftableOnly by remember { mutableStateOf(false) }
    var reclaimableOnly by remember { mutableStateOf(false) }
    var shipOnly by remember { mutableStateOf(false) }
    var newestFirst by remember { mutableStateOf(true) }
    var buybackItems by remember { mutableStateOf(emptyList<BuybackItem>()) }
    var logEntries by remember { mutableStateOf(emptyList<String>()) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    var ccuShips by remember { mutableStateOf(emptyList<CcuShip>()) }
    var ownedCcu by remember { mutableStateOf(emptyList<OwnedCcu>()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    val safeNoOp = remember { SafeNoOpDestructiveActionExecutor() }

    LaunchedEffect(repository, buybackRepository, hangarLogRepository, loadAttempt) {
        loading = true
        loadError = null
        runCatching {
            val ships = repository.ownedShips()
            val items = repository.inventory()
            val buyback = buybackRepository.items()
            val logs = hangarLogRepository.entries()
            HangarLoadedData(ships, items, buyback, logs)
        }.onSuccess { loaded ->
            ownedShips = loaded.ships
            inventory = loaded.items
            buybackItems = loaded.buyback
            logEntries = loaded.logs
        }.onFailure { loadError = it.message ?: "机库缓存读取失败" }
        loading = false
    }
    LaunchedEffect(ccuRepository) {
        runCatching { ccuRepository.ships() to ccuRepository.owned() }
            .onSuccess { (ships, owned) -> ccuShips = ships; ownedCcu = owned }
    }

    val visibleInventory = remember(inventory, query, giftableOnly, reclaimableOnly, shipOnly, newestFirst) {
        inventory
            .asSequence()
            .filter { query.isBlank() || it.title.contains(query, true) || it.originalName.contains(query, true) }
            .filter { !giftableOnly || it.isGiftable }
            .filter { !reclaimableOnly || it.isReclaimable }
            .filter { !shipOnly || it.typeLabel.contains("舰船", true) }
            .let { sequence -> if (newestFirst) sequence.sortedByDescending { it.date } else sequence.sortedBy { it.date } }
            .toList()
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
                    onOpenDesignLab = { showLogs = true },
                    isOnline = isOnline,
                    onToggleOnline = onToggleOnline,
                    onSearch = { showSearch = !showSearch },
                )
            }
            if (showSearch) {
                item {
                    ReferenceSearchField(
                        backdrop = backdrop,
                        isDark = isDark,
                        value = query,
                        onValueChange = { query = it },
                        searchIcon = RefugeIcons.search,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RefugeLiquidSegmented(
                        backdrop = backdrop,
                        isDark = isDark,
                        labels = listOf("机库", "回购", "升级"),
                        initialIndex = selectedSection,
                        onSelected = { selectedSection = it },
                        modifier = Modifier.fillMaxWidth(.90f),
                    )
                }
            }
            if (loading && ownedShips.isEmpty() && inventory.isEmpty()) {
                item { ProductionLoadingState(backdrop, palette, "正在读取机库资料") }
            } else if (loadError != null && ownedShips.isEmpty() && inventory.isEmpty()) {
                item { ProductionErrorState(backdrop, palette, loadError!!, onRetry = { loadAttempt++ }) }
            } else if (selectedSection == 0) {
                items(ownedShips, key = { it.name }) { ship ->
                    OwnedShipHero(
                        backdrop = backdrop,
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
                        count = visibleInventory.size,
                        onFilter = { showFilter = true },
                        onSort = { showSort = true },
                        newestFirst = newestFirst,
                    )
                }
                if (visibleInventory.isEmpty()) {
                    item {
                        ProductionEmptyState(
                            palette = palette,
                            label = if (inventory.isEmpty()) "暂无机库清单" else "没有匹配的机库项目",
                        )
                    }
                } else {
                    item {
                        InventoryGlassGroup(
                            backdrop = backdrop,
                            palette = palette,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            visibleInventory.forEachIndexed { index, inventoryItem ->
                                HangarInventoryRow(
                                    palette = palette,
                                    item = inventoryItem,
                                    isLast = index == visibleInventory.lastIndex,
                                    onClick = { selectedDetail = inventoryItem.toHangarDetail() },
                                    onGift = { safeNoOp.execute(DestructiveAction.GIFT); pendingAction = "赠送" },
                                    onReclaim = { safeNoOp.execute(DestructiveAction.RECLAIM); pendingAction = "回收" },
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
                if (buybackItems.isEmpty()) {
                    item { ProductionEmptyState(palette, "暂无回购项目") }
                } else {
                    items(buybackItems, key = { it.title }) { item ->
                        HangarRebuyRow(palette, item) { selectedDetail = item.toHangarDetail() }
                    }
                }
            } else {
                item {
                    HangarUpgradePanel(backdrop, palette, ccuShips, ownedCcu)
                }
            }
            }
        },
        overlay = { pageBackdrop ->
            RootBottomNav(pageBackdrop, isDark, selectedBottomTab, onNavigate)
        },
    )

    if (showFilter) {
        FilterSheet(
            backdrop = backdrop,
            palette = palette,
            giftableOnly = giftableOnly,
            reclaimableOnly = reclaimableOnly,
            shipOnly = shipOnly,
            onGiftableChanged = { giftableOnly = it },
            onReclaimableChanged = { reclaimableOnly = it },
            onShipOnlyChanged = { shipOnly = it },
            onDismiss = { showFilter = false },
        )
    }
    if (showSort) {
        HangarSortSheet(
            backdrop = backdrop,
            palette = palette,
            newestFirst = newestFirst,
            onSelected = { newestFirst = it; showSort = false },
            onDismiss = { showSort = false },
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
            onGift = { safeNoOp.execute(DestructiveAction.GIFT); pendingAction = "赠送" },
            onReclaim = { safeNoOp.execute(DestructiveAction.RECLAIM); pendingAction = "回收" },
            onJump = { pendingAction = "跳转到 RSI" },
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
            entries = logEntries,
            onDismiss = { showLogs = false },
        )
    }
    pendingAction?.let { action ->
        RefugeModalDialog(
            backdrop = backdrop,
            palette = palette,
            title = action,
            primaryLabel = "知道了",
            onDismiss = { pendingAction = null },
            onPrimary = { pendingAction = null },
        )
    }
}

private data class HangarDetail(
    val title: String,
    val subtitle: String,
    val price: String,
    val date: String,
    val imageRes: Int,
    val imageUrl: String? = null,
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

private data class HangarLoadedData(
    val ships: List<OwnedShip>,
    val items: List<HangarItem>,
    val buyback: List<BuybackItem>,
    val logs: List<String>,
)

private fun detailForShip(ship: OwnedShip) = HangarDetail(
    title = ship.name,
    subtitle = ship.packageName,
    price = ship.paidValue,
    date = "2026年08月02日",
    imageRes = ship.imageRes,
    imageUrl = ship.imageUrl,
    description = ship.name,
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
    imageUrl = imageUrl,
    description = title,
    isGiftable = isGiftable,
    isReclaimable = isReclaimable,
    meltValue = price,
    currentValue = currentValue,
    savings = savings,
    insurance = insurance,
    includedItems = includedItems.ifEmpty { listOf(title) },
    originalName = originalName,
    typeLabel = typeLabel,
    upgradeFrom = upgradeFrom,
    upgradeTo = upgradeTo,
    upgradeFromPrice = upgradeFromPrice,
    upgradeToPrice = upgradeToPrice,
)

private fun BuybackItem.toHangarDetail() = HangarDetail(
    title = title,
    subtitle = "$originalName · 回购项目",
    price = price,
    date = date,
    imageRes = imageRes,
    description = title,
    isGiftable = false,
    isReclaimable = false,
    meltValue = price,
    currentValue = price,
    savings = "$0",
    insurance = "—",
    includedItems = listOf(originalName),
)

@Composable
private fun HangarRebuyRow(palette: RefugePalette, item: BuybackItem, onClick: () -> Unit) {
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
private fun HangarUpgradePanel(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    ships: List<CcuShip>,
    owned: List<OwnedCcu>,
) {
    var seedIndex by remember(ships) { mutableIntStateOf(0) }
    val seed = ships.getOrNull(seedIndex.coerceIn(0, (ships.size - 1).coerceAtLeast(0)))
    val targets = seed?.let { eligibleTargetShips(it, ships) }.orEmpty()
    var targetIndex by remember(targets) { mutableIntStateOf(0) }
    val target = targets.getOrNull(targetIndex.coerceIn(0, (targets.size - 1).coerceAtLeast(0)))
    val remaining = if (seed != null && target != null) calculateRemainingPayment(seed, target, owned) else 0

    RefugeQuietLiquidGlassSurface(
        backdrop = backdrop,
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
        radius = RefugeRadius.panel,
        padding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
            Text("升级规划", style = RefugeTypography.title(palette))
            Row(horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                RefugeGlassControl(
                    backdrop = backdrop,
                    palette = palette,
                    onClick = { if (ships.isNotEmpty()) seedIndex = (seedIndex + 1) % ships.size },
                    modifier = Modifier.weight(1f),
                    contentDescription = "选择起始舰船",
                    padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) { Column { Text("起始舰船", style = RefugeTypography.caption(palette)); Text(seed?.name ?: "暂无", style = RefugeTypography.body(palette)) } }
                RefugeGlassControl(
                    backdrop = backdrop,
                    palette = palette,
                    onClick = { if (targets.isNotEmpty()) targetIndex = (targetIndex + 1) % targets.size },
                    modifier = Modifier.weight(1f),
                    contentDescription = "选择目标舰船",
                    padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) { Column { Text("目标舰船", style = RefugeTypography.caption(palette)); Text(target?.name ?: "暂无", style = RefugeTypography.body(palette)) } }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                InlineUpgradeMetric(palette, seed?.let { formatUsd(it.purchasePrice) } ?: "—", "舰船价值")
                InlineUpgradeMetric(palette, formatUsd(owned.sumOf { it.purchasePrice }), "已有 CCU")
                InlineUpgradeMetric(palette, formatUsd(remaining), "还需支付")
            }
        }
    }
}

@Composable
private fun RowScope.InlineUpgradeMetric(palette: RefugePalette, value: String, label: String) {
    Column(Modifier.weight(1f)) {
        Text(value, style = RefugeTypography.value(palette).copy(color = palette.accent), maxLines = 1, softWrap = false)
        Text(label, style = RefugeTypography.caption(palette))
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
    onSearch: () -> Unit,
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
        RefugeLiquidIconButton(backdrop, RefugeIcons.search, "搜索机库", onSearch, Modifier.size(44.dp), iconTint = palette.textSecondary)
        Spacer(Modifier.width(RefugeSpacing.xs))
        RefugeLiquidIconButton(backdrop, RefugeIcons.more, "更多操作", onOpenDesignLab, Modifier.size(44.dp), iconTint = palette.textSecondary)
    }
}

@Composable
private fun OwnedShipHero(
    backdrop: LayerBackdrop,
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
            if (ship.imageUrl.isNullOrBlank()) {
                HangarImage(ship.imageRes, "${ship.name} 图片", Modifier.size(92.dp))
            } else {
                AsyncImage(
                    model = ship.imageUrl,
                    contentDescription = "${ship.name} 图片",
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(ship.imageRes),
                    error = painterResource(ship.imageRes),
                    modifier = Modifier.size(92.dp).aspectRatio(1f).clip(RoundedCornerShape(RefugeRadius.image)),
                )
            }
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
    newestFirst: Boolean,
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
            label = if (newestFirst) "排序：最新" else "排序：最早",
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
    onGift: () -> Unit,
    onReclaim: () -> Unit,
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
            if (item.imageUrl.isNullOrBlank()) {
                HangarImage(item.imageRes, "${item.title} 图片", Modifier.size(88.dp))
            } else {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = "${item.title} 图片",
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(item.imageRes),
                    error = painterResource(item.imageRes),
                    modifier = Modifier.size(88.dp).aspectRatio(1f).clip(RoundedCornerShape(RefugeRadius.image)),
                )
            }
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
                    InventoryAction(RefugeIcons.gift, "赠送", palette, item.isGiftable, onGift)
                    InventoryAction(RefugeIcons.reclaim, "回收", palette, item.isReclaimable, onReclaim)
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
    onGift: () -> Unit,
    onReclaim: () -> Unit,
    onJump: () -> Unit,
) {
    RefugeLiquidSheet(
        backdrop = backdrop,
        palette = palette,
        title = "机库详情",
        onDismiss = onDismiss,
        sheetHeight = if (detail.includedItems.size <= 1) 620.dp else 736.dp,
        actionBottomPadding = 36.dp,
        actionOverContent = true,
        action = { modalBackdrop ->
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val sideWidth = (maxWidth * .127f).coerceIn(44.dp, 52.dp)
                val gap = (maxWidth * .10f).coerceAtLeast(24.dp)
                val centerWidth = (maxWidth * .40f).coerceIn(120.dp, 168.dp)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RefugeLiquidIconButton(
                        backdrop = modalBackdrop,
                        icon = RefugeIcons.log,
                        contentDescription = "日志",
                        onClick = onLog,
                        modifier = Modifier.size(sideWidth),
                        iconTint = palette.text,
                        isInteractive = false,
                        enablePressHighlight = true,
                    )
                    RefugeFloatingActionGroup(
                        backdrop = modalBackdrop,
                        palette = palette,
                        actions = listOf(
                            RefugeFloatingAction(RefugeIcons.hangarGift, "礼物", onGift),
                            RefugeFloatingAction(RefugeIcons.hangarOpenExternal, "跳转", onJump),
                            RefugeFloatingAction(RefugeIcons.hangarUpgrade, "升级", onUpgrade),
                        ),
                        modifier = Modifier.width(centerWidth),
                    )
                    RefugeLiquidIconButton(
                        backdrop = modalBackdrop,
                        icon = RefugeIcons.reclaim,
                        contentDescription = "回收",
                        onClick = onReclaim,
                        modifier = Modifier.size(sideWidth),
                        iconTint = palette.text,
                        isInteractive = false,
                        enablePressHighlight = true,
                    )
                }
            }
        },
    ) { modalBackdrop ->
        Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md)) {
                Row(verticalAlignment = Alignment.Top) {
                    if (detail.imageUrl.isNullOrBlank()) {
                        HangarImage(detail.imageRes, "${detail.title} 图片", Modifier.size(112.dp))
                    } else {
                        AsyncImage(
                            model = detail.imageUrl,
                            contentDescription = "${detail.title} 图片",
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(detail.imageRes),
                            error = painterResource(detail.imageRes),
                            modifier = Modifier.size(112.dp).aspectRatio(1f).clip(RoundedCornerShape(RefugeRadius.image)),
                        )
                    }
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
                DetailMetadataRow(palette, "状态", "已拥有")
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
    onClick: () -> Unit = {},
) {
    Box(
        Modifier
            .size(36.dp)
            .clickable(enabled = enabled, onClick = onClick)
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
    giftableOnly: Boolean,
    reclaimableOnly: Boolean,
    shipOnly: Boolean,
    onGiftableChanged: (Boolean) -> Unit,
    onReclaimableChanged: (Boolean) -> Unit,
    onShipOnlyChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    RefugeLiquidSheet(backdrop, palette, "筛选舰库", onDismiss) { modalBackdrop ->
        Text("按项目类型和可用操作缩小清单", style = RefugeTypography.secondary(palette))
        listOf(
            "仅显示舰船" to shipOnly,
            "可回收" to reclaimableOnly,
            "可赠送" to giftableOnly,
        ).forEach { (label, checked) ->
            Row(
                Modifier.fillMaxWidth().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    when (label) {
                        "仅显示舰船" -> onShipOnlyChanged(!checked)
                        "可回收" -> onReclaimableChanged(!checked)
                        else -> onGiftableChanged(!checked)
                    }
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
private fun HangarSortSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    newestFirst: Boolean,
    onSelected: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    RefugeLiquidSheet(backdrop, palette, "排序舰库", onDismiss) { modalBackdrop ->
        listOf("最新同步" to true, "最早同步" to false).forEach { (label, value) ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelected(value) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = RefugeTypography.body(palette), modifier = Modifier.weight(1f))
                if (newestFirst == value) Icon(RefugeIcons.check, null, tint = palette.accent, modifier = Modifier.size(RefugeIconSize.small))
            }
        }
        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.chevron, "完成", onDismiss, Modifier.align(Alignment.End))
    }
}

@Composable
private fun RefugeModalDialog(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    title: String,
    body: String? = null,
    primaryLabel: String,
    onDismiss: () -> Unit,
    onPrimary: () -> Unit,
) {
    RefugeLiquidSheet(backdrop, palette, title, onDismiss) { modalBackdrop ->
        if (!body.isNullOrBlank()) Text(body, style = RefugeTypography.body(palette))
        RefugeGlassControl(modalBackdrop, palette, onPrimary, contentDescription = primaryLabel) {
            Text(primaryLabel, style = RefugeTypography.body(palette))
        }
    }
}

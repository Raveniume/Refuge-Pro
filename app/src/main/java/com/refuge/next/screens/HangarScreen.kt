package com.refuge.next.screens

import com.refuge.next.data.matchesLegacyFilters
import com.refuge.next.data.hangarPriceCents
import com.refuge.next.data.displayImageUrl

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.R
import com.refuge.next.data.HangarItem
import com.refuge.next.data.HangarIncludedItem
import com.refuge.next.data.HangarRepository
import com.refuge.next.data.BuybackItem
import com.refuge.next.data.BuybackRepository
import com.refuge.next.data.HangarLogRepository
import com.refuge.next.data.HangarLogEntry
import com.refuge.next.data.DestructiveAction
import com.refuge.next.data.SafeNoOpDestructiveActionExecutor
import com.refuge.next.data.PledgeActionRequest
import com.refuge.next.data.OwnedShip
import com.refuge.next.data.CcuRepository
import com.refuge.next.data.CcuShip
import com.refuge.next.data.UserPresence
import com.refuge.next.design.RefugeIconSize
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import com.refuge.next.design.refugeContinuousShape
import com.refuge.next.material.PageGlassScope
import com.refuge.next.material.RefugeCompactUtilityPill
import com.refuge.next.material.RefugeGlassControl
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeLightweightGlassSurface
import com.refuge.next.material.RefugeContentSurface
import com.refuge.next.material.RefugeStandardGlassSurface
import com.refuge.next.material.RefugeGlassListGroup
import com.refuge.next.material.RefugeGlassListRow
import com.refuge.next.material.RefugeModalSurface
import com.refuge.next.material.RefugeLiquidSheet
import com.refuge.next.material.RefugeLiquidSegmented
import com.refuge.next.material.InventoryGlassGroup
import com.refuge.next.material.RefugeFloatingAction
import com.refuge.next.material.RefugeFloatingActionGroup
import com.refuge.next.material.RefugeHeaderActionBar
import com.refuge.next.material.RefugeHeaderAvatar
import com.refuge.next.material.RefugeLiquidIconButton
import com.refuge.next.material.RefugeAnimatedSearch
import com.refuge.next.material.RefugeLiquidGlassField
import com.refuge.next.material.RefugeLiquidGlass
import com.refuge.next.material.RefugePullToRefresh
import com.refuge.next.material.refugeTopEdgeFade
import com.refuge.next.reference.OfficialLiquidButtonPort
import androidx.compose.ui.platform.testTag
import com.refuge.next.design.translatedShipName
import com.refuge.next.material.RefugeRemoteImage
import com.refuge.next.reference.ReferenceLiquidSelectionBar
import com.refuge.next.reference.ReferenceLiquidButton
import com.refuge.next.reference.ReferenceSearchField
import com.refuge.next.reference.ReferenceSelectionItem
import com.refuge.next.reference.ReferenceSegmentedControl
import kotlinx.coroutines.launch
import kotlin.math.abs


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
    onOpenDesignLab: () -> Unit,
    onOpenOwnedCcu: (Long) -> Unit,
    isOnline: Boolean,
    presence: UserPresence,
    avatarUrl: String?,
    onToggleOnline: () -> Unit,
    onOverlayVisibilityChanged: (Boolean) -> Unit = {},
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var ownedShips by remember(repository) { mutableStateOf(repository.cachedOwnedShips()) }
    var inventory by remember(repository) { mutableStateOf(repository.cachedInventory()) }
    var showFilter by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var selectedDetail by remember { mutableStateOf<HangarDetail?>(null) }
    var initialDetailPage by remember { mutableStateOf(HangarDetailPage.DETAIL) }
    var showLogs by remember { mutableStateOf(false) }
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var hangarFilters by rememberSaveable { mutableStateOf(hashMapOf<String, Set<String>>()) }
    var priceSort by rememberSaveable { mutableStateOf("默认") }
    var giftableOnly by rememberSaveable { mutableStateOf(false) }
    var reclaimableOnly by rememberSaveable { mutableStateOf(false) }
    var shipOnly by rememberSaveable { mutableStateOf(false) }
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    var buybackItems by remember(buybackRepository) { mutableStateOf(buybackRepository.cachedItems()) }
    var logEntries by remember(hangarLogRepository) { mutableStateOf(hangarLogRepository.cachedEntries()) }
    var pendingAction by remember { mutableStateOf<String?>(null) }
    var loading by remember(repository) { mutableStateOf(ownedShips.isEmpty() && inventory.isEmpty()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    val safeNoOp = remember { SafeNoOpDestructiveActionExecutor() }
    val listState = com.refuge.next.navigation.rememberRootListState(0)
    val refreshCallback = remember { { loadAttempt++; Unit } }
    val scrollRegistry = com.refuge.next.navigation.LocalRootScrollRegistry.current
    DisposableEffect(Unit) {
        val registry = scrollRegistry
        registry?.registerRefresh(0, refreshCallback)
        onDispose { registry?.unregisterRefresh(0, refreshCallback) }
    }

    LaunchedEffect(showFilter, showSort, selectedDetail, showLogs, selectedSection) {
        onOverlayVisibilityChanged(
            !showFilter && !showSort && selectedDetail == null && !showLogs,
        )
    }

    LaunchedEffect(repository, buybackRepository, hangarLogRepository, loadAttempt) {
        loading = ownedShips.isEmpty() && inventory.isEmpty()
        loadError = null
        runCatching {
            val cachedItems = repository.awaitCachedInventory()
            val cachedShips = repository.awaitCachedOwnedShips()
            val cachedBuyback = buybackRepository.awaitCachedItems()
            val cachedLogs = hangarLogRepository.awaitCachedEntries()
            HangarLoadedData(cachedShips, cachedItems, cachedBuyback, cachedLogs)
        }.onSuccess { cached ->
            if (cached.items.isNotEmpty() || cached.ships.isNotEmpty()) {
                ownedShips = cached.ships
                inventory = cached.items
                loading = false
            }
            if (cached.buyback.isNotEmpty()) buybackItems = cached.buyback
            if (cached.logs.isNotEmpty()) logEntries = cached.logs
        }
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
            // A failed or empty live response must not erase the last useful
            // snapshot. The repository can legitimately return an empty list
            // while the account endpoint is unavailable during refresh.
            if (loaded.logs.isNotEmpty()) logEntries = loaded.logs
        }.onFailure { loadError = it.message ?: "机库缓存读取失败" }
        loading = false
        isRefreshing = false
    }
    val visibleInventory = remember(inventory, query, hangarFilters, priceSort, newestFirst) {
        inventory
            .asSequence()
            .filter { item -> query.isBlank() || (listOf(item.title, item.originalName) + item.includedItems + item.includedEntries.map { it.title }).any { it.contains(query, true) } }
            .filter { it.matchesLegacyFilters(hangarFilters) }
            .let { sequence -> when(priceSort) {
                "价格从高到低" -> sequence.sortedByDescending { hangarPriceCents(it.price) }
                "价格从低到高" -> sequence.sortedBy { hangarPriceCents(it.price) }
                else -> if (newestFirst) sequence.sortedByDescending { it.date } else sequence.sortedBy { it.date }
            } }
            .toList()
    }

    PageGlassScope(
        backdrop = backdrop,
        content = {
        RefugePullToRefresh(
            listState = listState,
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true; loadAttempt++ },
            indicatorColor = palette.accent,
            edgeColor = palette.background,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                // The refresh container owns the single edge veil. Applying a
                // second veil here starts below the status-bar inset and
                // darkens the avatar and header controls when the list is at
                // rest.
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                state = listState,
                contentPadding = PaddingValues(
                    start = RefugeSpacing.page,
                    top = RefugeSpacing.lg,
                    end = RefugeSpacing.page,
                    bottom = RefugeSpacing.rootNavigation,
                ),
                verticalArrangement = Arrangement.Top,
            ) {
            item {
                Column(Modifier.fillMaxWidth().padding(bottom = RefugeSpacing.lg)) {
                    HangarHeader(
                        backdrop = backdrop,
                        palette = palette,
                        onOpenDesignLab = { showLogs = true },
                        isOnline = isOnline,
                        presence = presence,
                        avatarUrl = avatarUrl,
                        onToggleOnline = onToggleOnline,
                        onSearch = { showSearch = !showSearch },
                    )
                    RefugeAnimatedSearch(showSearch, Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().padding(top = RefugeSpacing.lg)) {
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
                }
            }
            item {
                Box(
                    Modifier.fillMaxWidth().padding(bottom = RefugeSpacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    RefugeLiquidSegmented(
                        backdrop = backdrop,
                        isDark = isDark,
                        labels = listOf("机库", "回购", "升级"),
                        initialIndex = selectedSection,
                        onSelected = { index ->
                            selectedSection = index
                        },
                        modifier = Modifier.fillMaxWidth(.90f),
                        // Keep the top capsule between the title line and the
                        // 46dp avatar, matching the supplied reference.
                        height = 42.dp,
                    )
                }
            }
            if (loading && ownedShips.isEmpty() && inventory.isEmpty()) {
                item { ProductionLoadingState(backdrop, palette, "正在读取机库资料") }
            } else if (loadError != null && ownedShips.isEmpty() && inventory.isEmpty()) {
                item { ProductionErrorState(backdrop, palette, loadError!!, onRetry = { loadAttempt++ }) }
            } else if (selectedSection == 0) {
                if (ownedShips.isNotEmpty()) item {
                    Box(Modifier.fillMaxWidth().padding(bottom = RefugeSpacing.lg)) {
                        OwnedShipStack(
                            backdrop = backdrop,
                            palette = palette,
                            ships = ownedShips,
                            onOpen = { ship ->
                                selectedDetail = inventory.firstOrNull { item ->
                                    item.id == ship.sourceItemId
                                }?.toHangarDetail() ?: detailForShip(ship)
                            },
                        )
                    }
                }
                item {
                    Box(Modifier.fillMaxWidth().padding(bottom = RefugeSpacing.lg)) {
                        HangarListHeader(
                            backdrop = backdrop,
                            palette = palette,
                            count = visibleInventory.size,
                            onFilter = { showFilter = true },
                            onSort = { showSort = true },
                            newestFirst = newestFirst,
                        )
                    }
                }
                if (visibleInventory.isEmpty()) {
                    item {
                        ProductionEmptyState(
                            palette = palette,
                            label = if (inventory.isEmpty()) "暂无机库清单" else "没有匹配的机库项目",
                        )
                    }
                } else {
                    itemsIndexed(
                        items = visibleInventory,
                        key = { index, item -> if (item.id > 0) "inventory:${item.id}" else "inventory-local:$index" },
                        contentType = { _, _ -> "inventory" },
                    ) { index, inventoryItem ->
                        InventoryGlassGroup(
                            backdrop = backdrop,
                            palette = palette,
                            modifier = Modifier.fillMaxWidth(),
                            padding = PaddingValues(horizontal = 12.dp),
                            roundTop = index == 0,
                            roundBottom = index == visibleInventory.lastIndex,
                        ) {
                            HangarInventoryRow(
                                palette = palette,
                                item = inventoryItem,
                                isLast = index == visibleInventory.lastIndex,
                                onClick = { selectedDetail = inventoryItem.toHangarDetail() },
                                onGift = {
                                    initialDetailPage = if (inventoryItem.status.equals("Gifted", true)) HangarDetailPage.RECALL else HangarDetailPage.GIFT
                                    selectedDetail = inventoryItem.toHangarDetail()
                                },
                                onReclaim = {
                                    initialDetailPage = HangarDetailPage.RECLAIM
                                    selectedDetail = inventoryItem.toHangarDetail()
                                },
                            )
                        }
                    }
                }
                item {
                    Box(Modifier.fillMaxWidth().padding(top = RefugeSpacing.lg)) {
                        RefugeLightweightGlassSurface(
                            palette = palette,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showLogs = true },
                            contentDescription = "机库日志",
                            padding = PaddingValues(14.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(RefugeIcons.log, null, tint = palette.accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(RefugeSpacing.sm))
                                Column(Modifier.weight(1f)) {
                                    Text("机库日志", style = RefugeTypography.body(palette).copy(color = palette.text))
                                }
                                Icon(RefugeIcons.chevron, null, tint = palette.textMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            } else if (selectedSection == 1) {
                if (buybackItems.isEmpty()) {
                    item { ProductionEmptyState(palette, "暂无回购项目") }
                } else {
                    item {
                        Box(Modifier.fillMaxWidth().padding(bottom = RefugeSpacing.lg)) {
                            Text(
                                "回购清单 · ${buybackItems.sumOf { it.quantity }} 项 / ${buybackItems.size} 组",
                                style = RefugeTypography.headline(palette),
                            )
                        }
                    }
                    itemsIndexed(
                        items = buybackItems,
                        key = { index, item -> if (item.id > 0) "buyback:${item.id}:${item.date}" else "buyback-local:$index" },
                        contentType = { _, _ -> "buyback" },
                    ) { index, item ->
                        RefugeGlassListGroup(
                            backdrop = backdrop,
                            palette = palette,
                            modifier = Modifier.fillMaxWidth(),
                            padding = PaddingValues(horizontal = 10.dp),
                            roundTop = index == 0,
                            roundBottom = index == buybackItems.lastIndex,
                        ) {
                            HangarRebuyRow(
                                palette = palette,
                                item = item,
                                isLast = index == buybackItems.lastIndex,
                                onClick = { selectedDetail = item.toHangarDetail() },
                            )
                        }
                    }
                }
            } else {
                item {
                    HangarCcuInventoryPanel(
                        backdrop = backdrop,
                        palette = palette,
                        isDark = isDark,
                        ccuRepository = ccuRepository,
                        repository = repository,
                        inventory = inventory,
                        onOpenOwnedCcu = onOpenOwnedCcu,
                        refreshKey = selectedSection,
                        ownedSeeds = ownedShips.mapNotNull { ship ->
                            parseHangarPrice(ship.paidValue)?.let { paid ->
                                val original = parseHangarPrice(ship.currentValue) ?: paid
                                CcuShip(
                                    id = ship.shipId?.toString() ?: "hangar:${ship.sourceItemId}:${ship.name}",
                                    name = ship.name,
                                    purchasePrice = original,
                                    imageRes = ship.imageRes,
                                    owned = true,
                                    paidPrice = paid,
                                    originalPrice = original,
                                )
                            }
                        },
                    )
                }
            }
            }
            }
        },
        overlay = { _ -> },
    )

    if (showFilter) {
        FacetFilterSheet(backdrop, palette, "筛选机库", linkedMapOf(
            "价格排序" to listOf("价格从高到低", "价格从低到高"),
            "类型" to listOf("舰船", "涂装", "升级", "订阅"),
            "状态" to listOf("在库", "已礼物"),
            "保险" to listOf("永久保险", "10年及以上", "其他"),
            "价格" to listOf("非0", "0-100", "100-500", "500+"),
            "礼物" to listOf("可礼物", "不可礼物"), "融船" to listOf("可融"),
            "起始舰船" to inventory.mapNotNull { it.upgradeFrom }.distinct().sorted(),
            "目标舰船" to inventory.mapNotNull { it.upgradeTo }.distinct().sorted(),
        ), hangarFilters, { updated ->
            val chosen = updated["价格排序"].orEmpty()
            priceSort = (chosen - hangarFilters["价格排序"].orEmpty()).firstOrNull() ?: chosen.firstOrNull() ?: "默认"
            hangarFilters = HashMap(updated + ("价格排序" to if (priceSort == "默认") emptySet() else setOf(priceSort)))
        }, { showFilter = false })
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
        val reclaimItem = remember(detail.sourceId) {
            inventory.firstOrNull { it.id == detail.sourceId }?.let { com.refuge.next.data.reclaimStackFor(it, inventory) }
        }
        HangarDetailSheet(
            backdrop = backdrop,
            palette = palette,
            detail = detail,
            logEntries = logEntries.filter { it.belongsTo(detail.sourceId) },
            onDismiss = { selectedDetail = null; initialDetailPage = HangarDetailPage.DETAIL },
            initialPage = initialDetailPage,
            onUpgrade = {
                selectedDetail = null
                initialDetailPage = HangarDetailPage.DETAIL
                onOpenOwnedCcu(detail.sourceId)
            },
            onGift = { request ->
                safeNoOp.execute(request)
                pendingAction = "调试保护已拦截，未提交账户变更"
            },
            reclaimContent = { modalBackdrop ->
                HangarReclaimContent(modalBackdrop, palette,
                    reclaimItem, repository,
                    onInventoryChanged = { loadAttempt++ })
            },
            onJump = {
                runCatching { uriHandler.openUri("https://robertsspaceindustries.com/account/pledges?page=${detail.page.coerceAtLeast(0)}") }
                    .onFailure { pendingAction = "无法打开 RSI 机库页面" }
            },
        )
    }
    if (showLogs) {
        ProductionListSheet(
            backdrop = backdrop,
            palette = palette,
            title = "机库日志",
            entries = logEntries.map(HangarLogEntry::summary),
            leadingIcon = RefugeIcons.log,
            sheetHeight = 780.dp,
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
    val includedEntries: List<HangarIncludedItem> = emptyList(),
    val originalName: String = "—",
    val typeLabel: String = "本地机库项目",
    val upgradeFrom: String? = null,
    val upgradeTo: String? = null,
    val upgradeFromPrice: String? = null,
    val upgradeToPrice: String? = null,
    val status: String = "—",
    val canUpgrade: Boolean = false,
    val isUpgrade: Boolean = false,
    val sourceId: Long = 0,
    val page: Int = 0,
)

private data class HangarLoadedData(
    val ships: List<OwnedShip>,
    val items: List<HangarItem>,
    val buyback: List<BuybackItem>,
    val logs: List<HangarLogEntry>,
)

private enum class HangarDetailPage { DETAIL, LOG, GIFT, RECALL, RECLAIM }

private fun detailForShip(ship: OwnedShip) = HangarDetail(
    title = ship.name,
    subtitle = ship.packageName,
    price = ship.paidValue,
    date = "—",
    imageRes = ship.imageRes,
    imageUrl = ship.imageUrl,
    description = ship.name,
    isGiftable = false,
    isReclaimable = false,
    meltValue = ship.paidValue,
    currentValue = ship.currentValue,
    savings = usdDifference(ship.currentValue, ship.paidValue),
    insurance = ship.insurance,
    includedItems = emptyList(),
    originalName = ship.name,
    typeLabel = "舰船 / 游戏包",
)

private fun usdDifference(current: String, paid: String): String {
    fun amount(value: String) = Regex("[0-9]+(?:\\.[0-9]+)?").find(value)?.value?.toDoubleOrNull() ?: 0.0
    val difference = (amount(current) - amount(paid)).coerceAtLeast(0.0)
    return if (difference % 1.0 == 0.0) "$${difference.toInt()}" else String.format(java.util.Locale.US, "$%.2f", difference)
}

private fun parseHangarPrice(value: String): Int? = Regex("[0-9]+(?:\\.[0-9]+)?")
    .find(value.replace(",", ""))?.value?.toBigDecimalOrNull()
    ?.movePointRight(2)?.toInt()
    ?.takeIf { it > 0 }

private fun HangarItem.toHangarDetail() = HangarDetail(
    title = title,
    subtitle = "$originalName · $typeLabel",
    price = price,
    date = date,
    imageRes = imageRes,
    imageUrl = displayImageUrl,
    description = title,
    isGiftable = isGiftable,
    isReclaimable = isReclaimable,
    meltValue = price,
    currentValue = currentValue,
    savings = savings,
    insurance = insurance,
    includedItems = includedItems.ifEmpty { listOf(title) },
    includedEntries = includedEntries,
    originalName = originalName,
    typeLabel = typeLabel,
    upgradeFrom = upgradeFrom,
    upgradeTo = upgradeTo,
    upgradeFromPrice = upgradeFromPrice,
    upgradeToPrice = upgradeToPrice,
    status = status,
    canUpgrade = canUpgrade,
    isUpgrade = isUpgrade,
    sourceId = id,
    page = page,
)

private fun BuybackItem.toHangarDetail() = HangarDetail(
    title = title,
    subtitle = "$originalName · 回购项目",
    price = price,
    date = date,
    imageRes = imageRes,
    imageUrl = imageUrl,
    description = title,
    isGiftable = false,
    isReclaimable = false,
    meltValue = price,
    currentValue = price,
    savings = "$0",
    insurance = "—",
    includedItems = listOf(originalName),
    originalName = originalName,
    isUpgrade = isUpgrade,
    sourceId = id,
)

@Composable
private fun HangarRebuyRow(
    palette: RefugePalette,
    item: BuybackItem,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val displayTitle = translatedShipName(item.title, item.originalName)
    RefugeGlassListRow(
        palette = palette,
        onClick = onClick,
        contentDescription = displayTitle,
        isLast = isLast,
        dividerInset = 100.dp,
        modifier = Modifier.fillMaxWidth().height(108.dp),
    ) {
        Row(Modifier.fillMaxSize().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
            if (item.imageUrl.isNullOrBlank()) {
                HangarImage(item.imageRes, "$displayTitle 图片", Modifier.size(88.dp))
            } else {
                RefugeRemoteImage(
                    model = item.imageUrl,
                    fallback = painterResource(item.imageRes),
                    contentDescription = "$displayTitle 图片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(88.dp).aspectRatio(1f).clip(refugeContinuousShape(RefugeRadius.image)),
                )
            }
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(Modifier.fillMaxSize()) {
                Text(
                    displayTitle,
                    style = RefugeTypography.headline(palette),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.quantity > 1) {
                    Text("数量 ×${item.quantity}", style = RefugeTypography.caption(palette).copy(color = palette.accent))
                }
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
private fun HangarHeader(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    onOpenDesignLab: () -> Unit,
    isOnline: Boolean,
    presence: UserPresence,
    avatarUrl: String?,
    onToggleOnline: () -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RefugeHeaderAvatar(
            avatarUrl = avatarUrl,
            palette = palette,
            presenceColor = hangarPresenceColor(palette, presence),
            onClick = onToggleOnline,
        )
        Spacer(Modifier.width(RefugeSpacing.md))
        Column(Modifier.weight(1f)) {
            Text("我的机库", style = RefugeTypography.largeTitle(palette))
        }
        RefugeHeaderActionBar(
            backdrop = backdrop,
            palette = palette,
            actions = listOf(
                RefugeFloatingAction(RefugeIcons.log, "机库日志", onOpenDesignLab),
                RefugeFloatingAction(RefugeIcons.search, "搜索机库", onSearch),
            ),
        )
    }
}

private fun hangarPresenceColor(palette: RefugePalette, presence: UserPresence): Color = when (presence) {
    UserPresence.ONLINE -> palette.positive
    UserPresence.AWAY -> Color(0xFFFFB020)
    UserPresence.DO_NOT_DISTURB -> Color(0xFFFF5C5C)
    UserPresence.PLAYING -> palette.accent
    UserPresence.INVISIBLE -> palette.textMuted
}

@Composable
private fun OwnedShipStack(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    ships: List<OwnedShip>,
    onOpen: (OwnedShip) -> Unit,
) {
    var selected by remember(ships) { mutableIntStateOf(0) }
    var dragOffsetPx by remember(ships) { mutableFloatStateOf(0f) }
    var dragDirection by remember(ships) { mutableIntStateOf(0) }
    var isSettling by remember(ships) { mutableStateOf(false) }
    var suppressCardClick by remember(ships) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val activeIndex = selected.coerceIn(ships.indices)
    val active = ships[activeIndex]
    val nextIndex = if (ships.size > 1) (activeIndex + 1) % ships.size else activeIndex
    val previousIndex = if (ships.size > 1) (activeIndex - 1 + ships.size) % ships.size else activeIndex
    val effectiveDirection = if (dragDirection > 0) 1 else -1
    val queuedIndex = if (effectiveDirection > 0) previousIndex else nextIndex
    val density = LocalDensity.current
    // Keep the viewport and both slots fixed. The active card owns the upper
    // slot; the adjacent card is revealed below it by a compact overlap. A
    // fixed geometry is important here: changing the measured height while a
    // finger is moving is what caused the old card/list boundary to jump.
    // Keep equal 8dp optical insets around the square image while making the
    // hero compact enough that the list remains in the first viewport.
    val cardHeight = 120.dp
    val thumbnailSize = 104.dp
    val cardInset = 8.dp
    // Keep only a compact depth cue below the active card. The rear card is
    // narrower as well, so the stack reads as one control rather than two
    // competing full-width panels.
    val stackOffset = 8.dp
    val viewportHeight = if (ships.size > 1) cardHeight + stackOffset else cardHeight
    val cardHeightPx = with(density) { cardHeight.toPx() }
    val stackOffsetPx = with(density) { stackOffset.toPx() }
    val viewportHeightPx = with(density) { viewportHeight.toPx() }
    val maxDragPx = with(density) { (cardHeight * .52f).toPx() }
    val commitThresholdPx = with(density) { (cardHeight * .21f).toPx() }
    val fadeBandPx = with(density) { 28.dp.toPx() }

    suspend fun settleStack(direction: Int, destination: Int, commit: Boolean) {
        if (isSettling) return
        isSettling = true
        val target = if (commit) {
            // dragOffsetPx is relative to the active card's resting offset.
            // Move the outgoing card fully beyond the fixed viewport without
            // ever changing or remeasuring the viewport itself.
            if (direction < 0) -cardHeightPx else cardHeightPx
        } else {
            0f
        }
        var completed = false
        try {
            animate(
                initialValue = dragOffsetPx,
                targetValue = target,
                animationSpec = tween(durationMillis = if (commit) 220 else 170),
            ) { value, _ ->
                dragOffsetPx = value
            }
            completed = true
        } finally {
            // The incoming card is already at the active slot before the
            // index changes, so resetting the gesture state cannot flash an
            // intermediate layout or move the parent bounds.
            Snapshot.withMutableSnapshot {
                if (completed && commit) selected = destination
                dragOffsetPx = 0f
                dragDirection = 0
                isSettling = false
            }
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(viewportHeight)
            .clipToBounds()
            .pointerInput(ships.size, activeIndex, isSettling) {
                var gestureDirection = 0
                detectVerticalDragGestures(
                    onDragStart = {
                        gestureDirection = 0
                        suppressCardClick = true
                    },
                    onVerticalDrag = { change, amount ->
                        if (!isSettling && ships.size > 1) {
                            change.consume()
                            dragOffsetPx = (dragOffsetPx + amount).coerceIn(-maxDragPx, maxDragPx)
                            gestureDirection = when {
                                dragOffsetPx < 0f -> -1
                                dragOffsetPx > 0f -> 1
                                else -> 0
                            }
                            dragDirection = gestureDirection
                        }
                    },
                    onDragEnd = {
                        if (!isSettling && ships.size > 1) {
                            val direction = gestureDirection.takeIf { it != 0 }
                                ?: if (dragOffsetPx < 0f) -1 else 1
                            val destination = if (direction < 0) nextIndex else previousIndex
                            val commit = abs(dragOffsetPx) >= commitThresholdPx
                            scope.launch {
                                settleStack(direction, destination, commit)
                                suppressCardClick = false
                            }
                        } else {
                            suppressCardClick = false
                        }
                    },
                    onDragCancel = {
                        if (!isSettling && dragOffsetPx != 0f) {
                            val direction = gestureDirection.takeIf { it != 0 }
                                ?: if (dragOffsetPx < 0f) -1 else 1
                            scope.launch {
                                settleStack(direction, activeIndex, false)
                                suppressCardClick = false
                            }
                        } else {
                            suppressCardClick = false
                        }
                    },
                )
            },
    ) {
        if (ships.size > 1) {
            val completionDistancePx = cardHeightPx
            val dragProgress = (abs(dragOffsetPx) / completionDistancePx.coerceAtLeast(1f)).coerceIn(0f, 1f)
            // At rest the next card is below the active one. For a downward
            // gesture, keep the previous card at the active slot and reveal it
            // only where the outgoing card has moved away. Moving it in from a
            // negative Y position lets the viewport cut through its rounded
            // top edge and produces a square flash.
            val incomingTopPx = when {
                effectiveDirection < 0 -> stackOffsetPx * (1f - dragProgress)
                effectiveDirection > 0 -> dragOffsetPx - cardHeightPx
                else -> stackOffsetPx
            }
            OwnedShipHero(
                backdrop = backdrop,
                palette = palette,
                ship = ships[queuedIndex],
                onClick = {
                    if (!isSettling && !suppressCardClick && dragOffsetPx == 0f) {
                        dragDirection = -1
                        scope.launch { settleStack(-1, nextIndex, true) }
                    }
                },
                modifier = Modifier
                    // The rear card is a quiet depth cue, not a second full
                    // width panel. Centering it prevents edge protrusion.
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .height(cardHeight)
                    .zIndex(0f)
                    .graphicsLayer {
                        translationY = incomingTopPx
                        val rearScale = 0.94f + 0.06f * dragProgress
                        scaleX = rearScale
                        scaleY = rearScale
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .stackEdgeDistanceFade(
                        // Preserve the resting depth cue. The local edge fade
                        // is needed only while the two cards are moving.
                        strength = { if (dragProgress <= .001f) 0f else .24f * (1f - dragProgress) },
                        fadeTop = { effectiveDirection > 0 },
                        cardTopPx = { incomingTopPx },
                        viewportHeightPx = viewportHeightPx,
                        fadeBandPx = with(density) { 18.dp.toPx() },
                    )
                    .stackOverlapReveal(
                        direction = { effectiveDirection },
                        activeTopPx = { dragOffsetPx },
                        incomingTopPx = { incomingTopPx },
                        fadeBandPx = with(density) { 12.dp.toPx() },
                    ),
                imageSize = thumbnailSize,
                contentInset = cardInset,
            )
        }
        OwnedShipHero(
            backdrop = backdrop,
            palette = palette,
            ship = active,
            onClick = {
                if (!isSettling && !suppressCardClick && dragOffsetPx == 0f) onOpen(active)
            },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .zIndex(1f)
                    // Translation has to wrap the offscreen edge mask. If it
                    // sits inside that layer, Compose clips the moving card to
                    // its old bounds and exposes a square horizontal edge.
                    .graphicsLayer {
                        translationY = dragOffsetPx
                    }
                    .stackEdgeDistanceFade(
                        strength = {
                            val distance = if (dragOffsetPx < 0f) {
                            cardHeightPx + stackOffsetPx
                        } else {
                            cardHeightPx
                        }
                        (abs(dragOffsetPx) / distance.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    },
                    fadeTop = { dragOffsetPx < 0f },
                    cardTopPx = { dragOffsetPx },
                    viewportHeightPx = viewportHeightPx,
                    fadeBandPx = fadeBandPx,
                ),
            imageSize = thumbnailSize,
            contentInset = cardInset,
        )
        if (ships.size > 1) {
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                ships.indices.forEach { index ->
                    Box(
                        Modifier
                            .size(4.dp, if (index == activeIndex) 14.dp else 6.dp)
                            .background(
                                if (index == activeIndex) palette.accent else palette.textMuted.copy(alpha = .55f),
                                refugeContinuousShape(50.dp),
                            ),
                    )
                }
            }
        }
    }
}

/**
 * A glass card cannot be composited behind another glass card as if both were
 * opaque: text and imagery from the back card otherwise remain visible through
 * the front card. Keep only the region physically uncovered by the moving card
 * and soften that boundary locally, without fading the whole card.
 */
private fun Modifier.stackOverlapReveal(
    direction: () -> Int,
    activeTopPx: () -> Float,
    incomingTopPx: () -> Float,
    fadeBandPx: Float,
): Modifier = graphicsLayer {
    // drawBackdrop can sample beyond its rounded outline. Constrain the
    // offscreen mask itself to the same continuous card silhouette so a drag
    // never exposes the rectangular render layer around the glass.
    shape = refugeContinuousShape(RefugeRadius.floating)
    clip = true
    compositingStrategy = CompositingStrategy.Offscreen
}
    .drawWithContent {
        drawContent()
        // At rest the lower strip of the rear card is the intended stack cue.
        // Masking it at a zero offset erases the cue completely.
        if (abs(activeTopPx()) < .5f) return@drawWithContent
        val movingUp = direction() < 0
        val activeBoundary = if (movingUp) activeTopPx() + size.height else activeTopPx()
        val localBoundary = activeBoundary - incomingTopPx()
        // The transition belongs entirely to the uncovered side. Centering it
        // on the active boundary leaves half of the rear glass visible through
        // the front card and creates the apparent card interpenetration.
        val startY = (if (movingUp) localBoundary else localBoundary - fadeBandPx)
            .coerceIn(0f, size.height)
        val endY = (if (movingUp) localBoundary + fadeBandPx else localBoundary)
            .coerceIn(0f, size.height)
            .coerceAtLeast((startY + 1f).coerceAtMost(size.height))
        val mask = if (movingUp) {
            listOf(Color.Transparent, Color.White)
        } else {
            listOf(Color.White, Color.Transparent)
        }
        drawRect(
            brush = Brush.verticalGradient(mask, startY = startY, endY = endY),
            blendMode = BlendMode.DstIn,
        )
    }

private fun Modifier.stackEdgeDistanceFade(
    strength: () -> Float,
    fadeTop: () -> Boolean,
    cardTopPx: () -> Float,
    viewportHeightPx: Float,
    fadeBandPx: Float,
): Modifier {
    return graphicsLayer {
        shape = refugeContinuousShape(RefugeRadius.floating)
        clip = true
        compositingStrategy = CompositingStrategy.Offscreen
    }
        .drawWithContent {
            drawContent()
            val fadeStrength = strength().coerceIn(0f, 1f)
            if (fadeStrength <= .001f) return@drawWithContent
            val top = cardTopPx()
            // Only attenuate the portion that approaches the fixed viewport
            // edge. A steeper response keeps the card body fully legible while
            // preventing a clipped horizontal seam during a vertical swipe.
            val edgeAlpha = (1f - fadeStrength * 3f).coerceIn(0f, 1f)
            val visibleStart = (-top).coerceIn(0f, size.height)
            val visibleEnd = (viewportHeightPx - top).coerceIn(0f, size.height)
            val startY: Float
            val endY: Float
            val colors: List<Color>
            // DstIn reads only alpha; the visible top veil uses the current
            // canvas color in refugeTopEdgeFade.
            val maskColor = Color.White
            if (fadeTop()) {
                if (visibleStart >= size.height) return@drawWithContent
                startY = visibleStart
                endY = (visibleStart + fadeBandPx).coerceAtMost(size.height).coerceAtLeast(startY + 1f)
                colors = listOf(maskColor.copy(alpha = edgeAlpha), maskColor)
            } else {
                if (visibleEnd <= 0f) return@drawWithContent
                endY = visibleEnd
                startY = (visibleEnd - fadeBandPx).coerceAtLeast(0f).coerceAtMost(endY - 1f)
                colors = listOf(maskColor, maskColor.copy(alpha = edgeAlpha))
            }
            drawRect(
                Brush.verticalGradient(
                    colors = colors,
                    startY = startY,
                    endY = endY,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
}

@Composable
internal fun OwnedShipHero(
    backdrop: com.kyant.backdrop.Backdrop,
    palette: RefugePalette,
    ship: OwnedShip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageSize: androidx.compose.ui.unit.Dp = 132.dp,
    contentInset: androidx.compose.ui.unit.Dp = 10.dp,
) {
    val scope = rememberCoroutineScope()
    val highlight = remember(scope) { com.refuge.next.reference.ReferenceInteractiveHighlight(scope) }
    RefugeContentSurface(
        palette = palette,
        modifier = modifier
            .semantics { role = Role.Button; contentDescription = ship.name }
            // Keep the card's measured box and its hit box fixed; pressing M80
            // only changes the in-place highlight and never its layout size.
            .then(highlight.modifier)
            .then(highlight.gestureModifier)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        radius = RefugeRadius.floating,
        // Keep the square artwork equally inset from the left, top, and
        // bottom while preserving its size in the more compact hero card.
        padding = PaddingValues(contentInset),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (ship.imageUrl.isNullOrBlank()) {
                HangarImage(ship.imageRes, "${ship.name} 图片", Modifier.size(imageSize))
            } else {
                RefugeRemoteImage(
                    model = ship.imageUrl!!,
                    fallback = painterResource(ship.imageRes),
                    contentDescription = "${ship.name} 图片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(imageSize).aspectRatio(1f).clip(refugeContinuousShape(RefugeRadius.image)),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(translatedShipName(ship.name), style = RefugeTypography.title(palette).copy(fontSize = 18.sp, lineHeight = 23.sp))
                Text(
                    translatedShipName(ship.packageName),
                    style = RefugeTypography.body(palette),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(RefugeSpacing.sm))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                    HeroMetric("价值", ship.currentValue, palette)
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
            icon = if (newestFirst) RefugeIcons.sortDescending else RefugeIcons.sortAscending,
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
        Text(label, style = RefugeTypography.secondary(palette), maxLines = 1)
        Text(
            value,
            style = RefugeTypography.value(palette).copy(color = color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
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
    val displayTitle = translatedShipName(item.title, item.originalName)
    Box(
        Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = displayTitle },
    ) {
        Row(
            Modifier.fillMaxSize().padding(vertical = RefugeSpacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            if (item.displayImageUrl.isNullOrBlank()) {
                HangarImage(item.imageRes, "$displayTitle 图片", Modifier.size(88.dp))
            } else {
                RefugeRemoteImage(
                    model = item.displayImageUrl,
                    fallback = painterResource(item.imageRes),
                    contentDescription = "$displayTitle 图片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(88.dp).aspectRatio(1f).clip(refugeContinuousShape(RefugeRadius.image)),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.height(88.dp).fillMaxWidth()) {
                Text(
                    displayTitle,
                    style = RefugeTypography.headline(palette),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                    Row(
                        Modifier.fillMaxWidth().height(36.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                    Row(
                        Modifier.weight(1f).fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            item.price,
                            style = RefugeTypography.value(palette),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.width(44.dp),
                        )
                        Spacer(Modifier.width(RefugeSpacing.xs))
                        Text(
                            hangarDisplayDate(item.date),
                            style = RefugeTypography.caption(palette).copy(fontSize = 11.sp, lineHeight = 15.sp),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        // Three compact actions keep independent hit targets while the
                        // date column receives every remaining pixel. This
                        // preserves the complete yyyy-MM-dd value on compact
                        // screens instead of clipping it to "2026-".
                        Modifier.width(90.dp).fillMaxHeight(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val gifted = item.status.equals("Gifted", true)
                        InventoryAction(if (gifted) RefugeIcons.recallGift else RefugeIcons.hangarGift,
                            if (gifted) "召回" else "赠送", palette, gifted || item.isGiftable, onGift)
                        InventoryAction(RefugeIcons.reclaim, "回收", palette, item.isReclaimable, onReclaim)
                        InventoryAction(RefugeIcons.chevron, "查看详情", palette, true, onClick)
                    }
                }
            }
        }
        if (!isLast) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .padding(start = 100.dp)
                    .height(1.dp)
                    .background(palette.divider),
            )
        }
    }
}

private fun hangarDisplayDate(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return "—"
    val iso = Regex("^(\\d{4}-\\d{2}-\\d{2})").find(value)?.groupValues?.get(1)
    if (iso != null) return iso
    val chinese = Regex("^(\\d{4})年(\\d{1,2})月(\\d{1,2})日").find(value)
    if (chinese != null) {
        return "%04d-%02d-%02d".format(
            java.util.Locale.US,
            chinese.groupValues[1].toInt(),
            chinese.groupValues[2].toInt(),
            chinese.groupValues[3].toInt(),
        )
    }
    return value
}

@Composable
private fun HangarDetailSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    detail: HangarDetail,
    logEntries: List<HangarLogEntry>,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    onGift: (PledgeActionRequest) -> Unit,
    reclaimContent: @Composable (LayerBackdrop) -> Unit,
    onJump: () -> Unit,
    initialPage: HangarDetailPage = HangarDetailPage.DETAIL,
) {
    var page by remember(detail) { mutableStateOf(initialPage) }
    val included = detail.includedEntries.distinctBy { Triple(it.title, it.kind, it.subtitle) }
    val plainIncluded = detail.includedItems.filter { text ->
        text.isNotBlank() && included.none { it.title.equals(text.trim(), true) }
    }.distinct()
    val pageHeight = when (page) {
        HangarDetailPage.DETAIL -> (470.dp + 92.dp * (included.size + plainIncluded.size).toFloat()).coerceIn(560.dp, 780.dp)
        // Keep a single item timeline visually balanced while still allowing
        // long item histories to grow until the sheet's scroll limit.
        HangarDetailPage.LOG -> (350.dp + (logEntries.size * 106).dp).coerceIn(520.dp, 780.dp)
        HangarDetailPage.GIFT -> 620.dp
        HangarDetailPage.RECALL -> 390.dp
        HangarDetailPage.RECLAIM -> 450.dp
    }
    RefugeLiquidSheet(
        backdrop = backdrop,
        palette = palette,
        title = "",
        modifier = Modifier.testTag("hangar-detail-sheet"),
        onDismiss = onDismiss,
        sheetHeight = pageHeight,
        actionBottomPadding = 36.dp,
        actionOverContent = true,
        contentUnderHandle = true,
        transparentActionArea = false,
        surfaceRefraction = false,
        action = if (page != HangarDetailPage.DETAIL) null else { modalBackdrop ->
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val sideWidth = 48.dp
                val gap = (maxWidth * .10f).coerceAtLeast(24.dp)
                val centerWidth = (maxWidth * .40f).coerceIn(132.dp, 168.dp)
                val gifted = detail.status.equals("Gifted", true)
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically) {
                    StableDetailIconButton(modalBackdrop, palette, RefugeIcons.log, "日志",
                        { page = HangarDetailPage.LOG }, Modifier.size(sideWidth))
                    RefugeFloatingActionGroup(modalBackdrop, palette, listOf(
                        RefugeFloatingAction(if (gifted) RefugeIcons.recallGift else RefugeIcons.hangarGift,
                            if (gifted) "召回" else "礼物",
                            { page = if (gifted) HangarDetailPage.RECALL else HangarDetailPage.GIFT }, gifted || detail.isGiftable),
                        RefugeFloatingAction(RefugeIcons.hangarOpenExternal, "跳转", onJump),
                        RefugeFloatingAction(RefugeIcons.hangarUpgrade, "升级", onUpgrade, detail.isUpgrade),
                    ), Modifier.width(centerWidth))
                    StableDetailIconButton(modalBackdrop, palette, RefugeIcons.reclaim, "回收",
                        { page = HangarDetailPage.RECLAIM }, Modifier.size(sideWidth),
                        enabled = detail.isReclaimable)
                }
            }
        },
    ) { modalBackdrop ->
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally { it } + fadeIn(tween(180))) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut(tween(140)))
                } else {
                    (slideInHorizontally { -it } + fadeIn(tween(180))) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut(tween(140)))
                }
            },
            label = "hangar-detail-pages",
        ) { currentPage ->
            Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                if (currentPage != HangarDetailPage.DETAIL) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        StableDetailIconButton(
                            backdrop = modalBackdrop,
                            icon = RefugeIcons.back,
                            contentDescription = "返回详情",
                            onClick = { page = HangarDetailPage.DETAIL },
                            modifier = Modifier.size(48.dp),
                            palette = palette,
                            iconSize = 20.dp,
                        )
                        Spacer(Modifier.width(RefugeSpacing.sm))
                        Text(
                            when (currentPage) {
                                HangarDetailPage.LOG -> "机库日志"
                                HangarDetailPage.GIFT -> "赠送物品"
                                HangarDetailPage.RECALL -> "召回礼物"
                                HangarDetailPage.RECLAIM -> "回收物品"
                                else -> "物品详情"
                            },
                            style = RefugeTypography.title(palette),
                        )
                    }
                }
                when (currentPage) {
                    HangarDetailPage.DETAIL -> HangarDetailContent(
                        palette = palette,
                        detail = detail,
                        included = included,
                        plainIncluded = plainIncluded,
                    )
                    HangarDetailPage.LOG -> HangarInlineLogContent(palette, logEntries)
                    HangarDetailPage.GIFT -> HangarInlineGiftContent(modalBackdrop, palette, detail, onGift)
                    HangarDetailPage.RECALL -> HangarInlineRecallContent(modalBackdrop, palette, detail, onGift)
                    HangarDetailPage.RECLAIM -> reclaimContent(modalBackdrop)
                }
                // This is scrollable trailing space, not a fixed mask. At rest
                // content continues behind the floating buttons; the final row
                // can still scroll above them for unobstructed reading.
                if (currentPage == HangarDetailPage.DETAIL) Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StableDetailIconButton(
    backdrop: Backdrop,
    palette: RefugePalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
) {
    OfficialLiquidButtonPort(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier.semantics {
            role = Role.Button
            this.contentDescription = contentDescription
            if (!enabled) disabled()
        },
        enabled = enabled,
        isInteractive = true,
        enablePressHighlight = true,
        visualHeight = 48.dp,
        contentPadding = 0.dp,
        shape = CircleShape,
        content = {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) palette.text else palette.textMuted.copy(alpha = .42f),
                modifier = Modifier.size(iconSize),
            )
        },
    )
}

@Composable
private fun HangarDetailContent(
    palette: RefugePalette, detail: HangarDetail,
    included: List<HangarIncludedItem>, plainIncluded: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
            RefugeRemoteImage(model = detail.imageUrl, fallback = painterResource(detail.imageRes),
                contentDescription = detail.title, contentScale = ContentScale.Crop,
                modifier = Modifier.size(120.dp).clip(refugeContinuousShape(10.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(translatedShipName(detail.title, detail.originalName), style = RefugeTypography.title(palette))
                if (detail.originalName.isNotBlank() && detail.originalName != "—") {
                    // The second line is the original English pledge name, never translated twice.
                    Text(detail.originalName, style = RefugeTypography.body(palette))
                }
            }
        }
        DetailDivider(palette)
        DetailValueSummary(palette, detail)
        DetailDivider(palette)
        if (detail.isUpgrade) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RefugeRemoteImage(model = detail.imageUrl, fallback = painterResource(detail.imageRes),
                    contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(120.dp, 80.dp).clip(refugeContinuousShape(10.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UpgradeShipPriceRow(palette, "从", detail.upgradeFrom, detail.upgradeFromPrice)
                    UpgradeShipPriceRow(palette, "到", detail.upgradeTo, detail.upgradeToPrice)
                }
            }
            DetailDivider(palette)
            val contents = plainIncluded.filterNot { it.equals(detail.originalName, true) || it == detail.title }
            if (contents.isNotEmpty()) Text("升级包含", style = RefugeTypography.headline(palette))
            contents.forEach { Text(translatedShipName(it), style = RefugeTypography.body(palette)) }
            // Empty upgrade contents are intentionally left blank; the route above
            // already communicates the source and target ships.
        } else {
            included.forEach { item ->
                DetailIncludedRow(palette, detail.imageRes, item.title, item.imageUrl,
                    item.subtitle.ifBlank { item.kind }, item.value)
            }
            if (included.isNotEmpty() && plainIncluded.isNotEmpty()) DetailDivider(palette)
            plainIncluded.forEach { Text(translatedShipName(it.trim()), style = RefugeTypography.body(palette)) }
        }
        Spacer(Modifier.height(112.dp))
    }
}

@Composable
private fun UpgradeShipPriceRow(palette: RefugePalette, prefix: String, ship: String?, price: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(prefix, style = RefugeTypography.caption(palette))
        Text(translatedShipName(ship ?: "—"), modifier = Modifier.weight(1f),
            style = RefugeTypography.body(palette))
        Text(price ?: "—", style = RefugeTypography.body(palette), maxLines = 1, softWrap = false)
    }
}

@Composable
private fun HangarInlineLogContent(palette: RefugePalette, entries: List<HangarLogEntry>) {
    if (entries.isEmpty()) {
        Text("暂无记录", style = RefugeTypography.body(palette))
    } else {
        entries.forEach { entry ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    Modifier.size(42.dp).background(entry.logColor(palette), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(entry.logIcon(), null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(RefugeSpacing.md))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val displayEntry = entry.copy(
                        name = translatedShipName(entry.name),
                        reason = entry.reason?.let { translatedShipName(it) },
                    )
                    Text(displayEntry.localizedTitle(), style = RefugeTypography.headline(palette))
                    val description = displayEntry.localizedDescription()
                    if (description != displayEntry.localizedTitle()) Text(description, style = RefugeTypography.body(palette))
                    Row(Modifier.fillMaxWidth()) {
                        Text(entry.formattedTime(), style = RefugeTypography.caption(palette))
                        Spacer(Modifier.weight(1f))
                        entry.priceCents?.let { Text("价值 $${it / 100.0}", style = RefugeTypography.caption(palette).copy(color = palette.positive)) }
                    }
                }
            }
        }
    }
}

private fun HangarLogEntry.logIcon() = when (type) {
    "APPLIED_UPGRADE", "CONSUMED" -> RefugeIcons.hangarUpgrade
    "RECLAIMED" -> RefugeIcons.reclaim
    "GIFT", "GIFT_CLAIMED", "GIFT_CANCELLED" -> RefugeIcons.hangarGift
    "GIVEAWAY" -> RefugeIcons.giveaway
    else -> RefugeIcons.ship
}

private fun HangarLogEntry.logColor(palette: RefugePalette) = when (type) {
    "RECLAIMED" -> palette.positive
    "APPLIED_UPGRADE", "CONSUMED" -> Color(0xFFFF3B30)
    "GIVEAWAY" -> Color(0xFFFF9500)
    else -> Color(0xFFFF5E3A)
}

@Composable
private fun HangarInlineGiftContent(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    detail: HangarDetail,
    onConfirm: (PledgeActionRequest) -> Unit,
) {
    var email by remember(detail.sourceId) { mutableStateOf("") }
    var recipient by remember(detail.sourceId) { mutableStateOf("避难所用户") }
    var password by remember(detail.sourceId) { mutableStateOf("") }
    var error by remember(detail.sourceId) { mutableStateOf<String?>(null) }
    var prepared by remember(detail.sourceId) { mutableStateOf<PledgeActionRequest?>(null) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val request = prepared
    if (request != null) {
        Text("确认赠送信息", style = RefugeTypography.headline(palette))
        DetailMetadataRow(palette, "礼物物品", translatedShipName(detail.title, detail.originalName))
        DetailMetadataRow(palette, "机库编号", request.pledgeId.toString())
        DetailMetadataRow(palette, "数量", "1")
        DetailMetadataRow(palette, "收件人邮箱", request.recipientEmail.orEmpty())
        DetailMetadataRow(palette, "收件人名称", request.recipientName.orEmpty())
        Text("对方领取后将无法撤回。", style = RefugeTypography.body(palette))
        RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.hangarGift, "确认赠送", { onConfirm(request) }, Modifier.fillMaxWidth())
        RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.back, "修改收件信息", { prepared = null }, Modifier.fillMaxWidth())
        return
    }
    Text("礼物物品：${translatedShipName(detail.title, detail.originalName)}", style = RefugeTypography.headline(palette))
    RefugeLiquidGlassField(
        value = email,
        onValueChange = { email = it; error = null },
        backdrop = backdrop,
        palette = palette,
        label = "收件人邮箱",
        modifier = Modifier.fillMaxWidth(),
    )
    RefugeLiquidGlassField(
        value = recipient,
        onValueChange = { recipient = it; error = null },
        backdrop = backdrop,
        palette = palette,
        label = "收件人名称",
        modifier = Modifier.fillMaxWidth(),
    )
    RefugeLiquidGlassField(
        value = password,
        onValueChange = { password = it; error = null },
        backdrop = backdrop,
        palette = palette,
        label = "当前账户密码",
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
    DetailMetadataRow(palette, "数量", "1")
    error?.let { Text(it, style = RefugeTypography.caption(palette).copy(color = palette.error)) }
    RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.hangarGift, "核对赠送信息", {
        error = when {
            detail.sourceId <= 0 || !detail.isGiftable || detail.status.equals("Gifted", true) -> "该物品当前不可赠送"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "请输入有效的收件人邮箱"
            recipient.trim().isEmpty() -> "请输入收件人名称"
            password.isBlank() -> "请输入当前账户密码"
            else -> null
        }
        if (error == null) {
            focus.clearFocus()
            keyboard?.hide()
            prepared = PledgeActionRequest.gift(detail.sourceId, detail.status, detail.isGiftable,
                email, recipient, password)
        }
    }, Modifier.fillMaxWidth())
}

@Composable
private fun HangarInlineRecallContent(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    detail: HangarDetail,
    onConfirm: (PledgeActionRequest) -> Unit,
) {
    Text("召回 ${translatedShipName(detail.title, detail.originalName)}", style = RefugeTypography.headline(palette))
    Text("确认召回后，原礼物链接将失效。", style = RefugeTypography.body(palette))
    DetailMetadataRow(palette, "机库编号", detail.sourceId.toString())
    val request = remember(detail.sourceId, detail.status) {
        runCatching { PledgeActionRequest.recall(detail.sourceId, detail.status) }.getOrNull()
    }
    if (request == null) {
        Text("该物品当前不可召回", style = RefugeTypography.body(palette))
    } else {
        RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.recallGift, "确认召回", { onConfirm(request) }, Modifier.fillMaxWidth())
    }
}

private fun HangarLogEntry.localizedTitle(): String = name.ifBlank { "机库项目" }

private fun HangarLogEntry.localizedDescription(): String = when (type) {
    "CREATED" -> "购买了 $name (#${target.orEmpty()})"
    "RECLAIMED" -> "回收了 $name (#${target.orEmpty()})"
    "CONSUMED" -> "消耗了 $name (#${target.orEmpty()})"
    "APPLIED_UPGRADE" -> "使用 ${reason.orEmpty()} (#${source.orEmpty()}) 升级了 $name (#${target.orEmpty()})"
    "BUYBACK" -> "回购了 $name (#${target.orEmpty()})"
    "GIFT" -> "赠送了 $name (#${target.orEmpty()})"
    "GIFT_CLAIMED" -> "$operator 领取了 $name (#${target.orEmpty()})"
    "GIFT_CANCELLED" -> "取消赠送 $name (#${target.orEmpty()})"
    "NAME_CHANGE" -> "将名称改为 ${reason.orEmpty()}"
    "NAME_CHANGE_RECLAIMED" -> "取消名称 ${reason.orEmpty()}"
    "GIVEAWAY" -> "获得了 $name (#${target.orEmpty()})"
    else -> rawContent.ifBlank { name }
}

private fun HangarLogEntry.formattedTime(): String = if (timeMillis <= 0) "时间未知" else
    java.text.SimpleDateFormat("yyyy年MM月dd日, HH:mm", java.util.Locale.SIMPLIFIED_CHINESE).format(java.util.Date(timeMillis))

private fun HangarLogEntry.summary(): String = buildString {
    append(localizedTitle()).append('\n')
    append(localizedDescription()).append('\n')
    append(formattedTime())
    priceCents?.let { append('\n').append("价值 $").append(String.format(java.util.Locale.US, "%.2f", it / 100.0)) }
}

private fun localizedHangarStatus(status: String): String = when (status.trim().lowercase()) {
    "attributed" -> "已入库"
    "gifted" -> "已赠送"
    "reclaimed" -> "已回收"
    "consumed" -> "已消耗"
    "locked" -> "已锁定"
    else -> status.ifBlank { "—" }
}

@Composable
private fun HangarInlineReclaimContent(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    detail: HangarDetail,
    onConfirm: () -> Unit,
) {
    RefugeStandardGlassSurface(
        backdrop = backdrop,
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
        radius = RefugeRadius.panel,
        padding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
            Text(translatedShipName(detail.title, detail.originalName), style = RefugeTypography.title(palette), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("可融价值", style = RefugeTypography.body(palette))
                Spacer(Modifier.weight(1f))
                Text(detail.meltValue, style = RefugeTypography.detailValue(palette).copy(color = palette.positive))
            }
            DetailDivider(palette)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("回收数量", style = RefugeTypography.body(palette))
                Spacer(Modifier.weight(1f))
                Text("1", style = RefugeTypography.value(palette))
            }
        }
    }
    Text("确认后物品会转换为信用点并永久消失，此操作不可撤销。", style = RefugeTypography.detailCaption(palette))
    RefugeCompactUtilityPill(
        backdrop = backdrop,
        palette = palette,
        icon = RefugeIcons.reclaim,
        label = "验证回收请求",
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun HangarDetail.isContainerTitle(candidate: String): Boolean {
    val normalized = candidate.trim()
    return normalized.equals(title.trim(), ignoreCase = true) ||
        normalized.equals(originalName.trim(), ignoreCase = true)
}

@Composable
private fun DetailDivider(palette: RefugePalette) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
}

@Composable
private fun DetailValueSummary(palette: RefugePalette, detail: HangarDetail) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = RefugeSpacing.xs, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs),
    ) {
        DetailMetric(palette, detail.meltValue, "可融")
        DetailMetric(palette, detail.currentValue, "价值", palette.accent)
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
    Column(
        Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xxs),
    ) {
        Text(
            value,
            style = RefugeTypography.detailValue(palette).copy(
                color = color,
                fontSize = 21.sp,
                lineHeight = 26.sp,
            ),
            maxLines = 1,
            softWrap = false,
        )
        Text(
            label,
            style = RefugeTypography.detailCaption(palette).copy(
                color = palette.textSecondary,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            ),
        )
    }
}

@Composable
private fun DetailIncludedRow(
    palette: RefugePalette,
    imageRes: Int,
    title: String,
    imageUrl: String? = null,
    subtitle: String = "",
    value: String = "—",
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
            if (imageUrl.isNullOrBlank()) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = "$title 图片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(120.dp, 80.dp).clip(refugeContinuousShape(10.dp)),
                )
            } else {
                RefugeRemoteImage(
                    model = imageUrl,
                    fallback = painterResource(imageRes),
                    contentDescription = "$title 图片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(120.dp, 80.dp).clip(refugeContinuousShape(10.dp)),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    translatedShipName(title),
                    style = RefugeTypography.detailBody(palette).copy(
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        translatedShipName(subtitle),
                        style = RefugeTypography.detailCaption(palette).copy(
                            color = palette.textSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (value != "—" && value.isNotBlank()) Text(value, style = RefugeTypography.body(palette).copy(color = palette.positive))
            }
    }
}

@Composable
private fun DetailInventoryDivider(palette: RefugePalette, inset: Boolean = false) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = if (inset) 130.dp else 0.dp)
            .height(1.dp)
            .background(palette.divider),
    )
}

@Composable
private fun DetailMetadataRow(palette: RefugePalette, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = RefugeSpacing.xxs), verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = RefugeTypography.detailCaption(palette), modifier = Modifier.weight(.32f))
        Text(value, style = RefugeTypography.detailBody(palette).copy(textAlign = androidx.compose.ui.text.style.TextAlign.End),
            modifier = Modifier.weight(.68f))
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
            .clip(refugeContinuousShape(RefugeRadius.image)),
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
            .size(30.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = label
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            null,
            // Keep unavailable actions visibly disabled without disappearing
            // into the refracted surface behind the row.
            tint = if (enabled) palette.textSecondary else palette.textMuted.copy(alpha = .68f),
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
                        .clip(refugeContinuousShape(7.dp))
                        .background(if (checked) palette.accent else Color.Transparent)
                        .border(.5.dp, if (checked) palette.accent.copy(alpha = .42f) else palette.outline.copy(alpha = .18f), refugeContinuousShape(7.dp)),
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
    RefugeLiquidSheet(backdrop, palette, "排序舰库", onDismiss, sheetHeight = 248.dp) { modalBackdrop ->
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

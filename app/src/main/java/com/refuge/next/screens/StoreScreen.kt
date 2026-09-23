package com.refuge.next.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.R
import com.refuge.next.data.StoreCategory
import com.refuge.next.data.StoreProduct
import com.refuge.next.data.StoreRepository
import com.refuge.next.data.CartRepository
import com.refuge.next.data.CcuPurchaseRepository
import com.refuge.next.data.countCurrentCcuDiscountShips
import com.refuge.next.data.formatUsd
import com.refuge.next.data.DestructiveAction
import com.refuge.next.data.SafeNoOpDestructiveActionExecutor
import com.refuge.next.data.UserPresence
import com.refuge.next.design.RefugeIconSize
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import com.refuge.next.design.translatedShipName
import com.refuge.next.design.refugeContinuousShape
import com.refuge.next.material.RefugeCompactUtilityPill
import com.refuge.next.material.PageGlassScope
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeFloatingAction
import com.refuge.next.material.RefugeHeaderActionBar
import com.refuge.next.material.RefugeLightweightGlassSurface
import com.refuge.next.material.RefugeGlassListGroup
import com.refuge.next.material.RefugeGlassListRow
import com.refuge.next.material.RefugeLiquidSheet
import com.refuge.next.material.RefugeLiquidModeSelector
import com.refuge.next.material.RefugeAnimatedSearch
import com.refuge.next.material.RefugeHeaderAvatar
import com.refuge.next.material.RefugePullToRefresh
import com.refuge.next.reference.ReferenceLiquidButton
import com.refuge.next.reference.ReferenceSearchField

private val StoreDiscountOrange = Color(0xFFFF8A00)
private const val GAME_PACKAGE_NOTICE = "This is not a GAME PACKAGE. Please note a GAME PACKAGE is required to play the game and fly or access your ships."

private fun displayStorefrontText(raw: String): String =
    com.refuge.next.data.storefrontText(raw)
        .replace(GAME_PACKAGE_NOTICE, "", ignoreCase = true)
        .replace("此商品不含游戏资格，进入游戏需要另购游戏包。", "")
        .trim()
internal enum class StoreSortOrder(val label: String) {
    DEFAULT("默认"), DESCENDING("价格从高到低"), ASCENDING("价格从低到高")
}

@Composable
fun StoreScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    repository: StoreRepository,
    cartRepository: CartRepository,
    ccuPurchaseRepository: CcuPurchaseRepository,
    isDark: Boolean,
    selectedBottomTab: Int,
    onNavigate: (Int) -> Unit,
    onOpenCcu: () -> Unit,
    isOnline: Boolean,
    presence: UserPresence,
    avatarUrl: String?,
    onToggleOnline: () -> Unit,
) {
    var products by remember(repository) { mutableStateOf(repository.cachedProducts()) }
    var isLoading by remember(repository) { mutableStateOf(products.isEmpty()) }
    var selectedCategory by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showCart by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<StoreProduct?>(null) }
    var priceBand by rememberSaveable { mutableStateOf("全部") }
    var warbondOnly by rememberSaveable { mutableStateOf(false) }
    var sortOrder by rememberSaveable { mutableStateOf(StoreSortOrder.DEFAULT) }
    var cartRevision by remember { mutableIntStateOf(0) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var pendingNotice by remember { mutableStateOf<String?>(null) }
    val safeNoOp = remember { SafeNoOpDestructiveActionExecutor() }
    val listState = com.refuge.next.navigation.rememberRootListState(1)

    LaunchedEffect(repository, loadAttempt) {
        isLoading = products.isEmpty()
        loadError = null
        runCatching { repository.awaitCachedProducts() }.onSuccess { cached ->
            if (cached.isNotEmpty()) {
                products = cached
                isLoading = false
            }
        }
        runCatching { repository.products() }
            .onSuccess { products = it }
            .onFailure { loadError = it.message ?: "商店目录读取失败" }
        isLoading = false
        isRefreshing = false
    }

    val category = StoreCategory.entries[selectedCategory]
    var ccuDiscountCount by remember(ccuPurchaseRepository) { mutableIntStateOf(ccuPurchaseRepository.cachedCatalog()?.let(::countCurrentCcuDiscountShips) ?: 0) }
    com.refuge.next.material.RefreshWhileVisible(ccuPurchaseRepository) {
        runCatching { ccuPurchaseRepository.catalog() }
            .onSuccess { current -> current?.let { ccuDiscountCount = countCurrentCcuDiscountShips(it) } }
    }
    val cartLines = remember(cartRevision) { cartRepository.lines() }
    val visibleProducts = remember(products, selectedCategory, search, priceBand, warbondOnly, sortOrder) {
        val query = search.trim()
        products
            .asSequence()
            .filter { it.category == category }
            .filter {
                query.isBlank() ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.metadata.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
            }
            .filter {
                when (priceBand) {
                    "0-100" -> it.priceCents <= 10000
                    "100-500" -> it.priceCents > 10000 && it.priceCents <= 50000
                    "500+" -> it.priceCents > 50000
                    else -> true
                }
            }
            .filter { !warbondOnly || it.isWarbond }
            .let { sequence -> when (sortOrder) {
                StoreSortOrder.DEFAULT -> sequence
                StoreSortOrder.DESCENDING -> sequence.sortedByDescending { it.priceCents }
                StoreSortOrder.ASCENDING -> sequence.sortedBy { it.priceCents }
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
            modifier = Modifier.fillMaxSize(),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(
                start = RefugeSpacing.page,
                top = RefugeSpacing.lg,
                end = RefugeSpacing.page,
                bottom = 132.dp,
            ),
            verticalArrangement = Arrangement.Top,
        ) {
            item {
                Box(Modifier.fillMaxWidth().padding(bottom = 8.dp)) { StoreHeader(
                    backdrop = backdrop,
                    palette = palette,
                    showSearch = showSearch,
                    onUpgrade = onOpenCcu,
                    onCart = { showCart = true },
                    onSearch = { showSearch = !showSearch },
                    isOnline = isOnline,
                    presence = presence,
                    avatarUrl = avatarUrl,
                    onToggleOnline = onToggleOnline,
                    ccuDiscountCount = ccuDiscountCount,
                ) }
            }
            item {
                RefugeAnimatedSearch(showSearch, Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        ReferenceSearchField(
                            backdrop = backdrop,
                            isDark = isDark,
                            value = search,
                            onValueChange = { search = it },
                            searchIcon = RefugeIcons.search,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    RefugeLiquidModeSelector(
                        backdrop = backdrop,
                        palette = palette,
                        labels = StoreCategory.entries.map { it.label },
                        selectedIndex = selectedCategory,
                        onSelected = { selectedCategory = it },
                        modifier = Modifier.fillMaxWidth(.90f),
                    )
                }
            }
            item {
                Box(Modifier.fillMaxWidth().padding(bottom = RefugeSpacing.md)) { StoreToolbar(
                    backdrop = backdrop,
                    palette = palette,
                    count = if (isLoading) null else visibleProducts.size,
                    filterActive = priceBand != "全部" || warbondOnly,
                    sortOrder = sortOrder,
                    onFilter = { showFilter = true },
                    onSort = { showSort = true },
                ) }
            }
            if (isLoading && products.isEmpty()) {
                item { ProductionLoadingState(backdrop, palette, "正在读取商店目录") }
            } else if (loadError != null && products.isEmpty()) {
                item { ProductionErrorState(backdrop, palette, loadError!!, onRetry = { loadAttempt++ }) }
            } else if (visibleProducts.isEmpty()) {
                item { StoreEmptyState(palette, search, category.label) }
            } else {
                itemsIndexed(
                    items = visibleProducts,
                    key = { _, product -> "store:${product.id}" },
                    contentType = { _, _ -> "store-product" },
                ) { index, product ->
                    RefugeGlassListGroup(
                        backdrop = backdrop,
                        palette = palette,
                        modifier = Modifier.fillMaxWidth(),
                        padding = PaddingValues(horizontal = 10.dp),
                        roundTop = index == 0,
                        roundBottom = index == visibleProducts.lastIndex,
                    ) {
                        StoreProductRow(
                            palette = palette,
                            product = product,
                            isLast = index == visibleProducts.lastIndex,
                            onClick = { selectedProduct = product },
                        )
                    }
                }
            }
        }
        }

        },
        overlay = { _ -> },
    )

    if (showFilter) {
        StoreFilterSheet(
            backdrop = backdrop,
            palette = palette,
            selectedBand = priceBand,
            warbondOnly = warbondOnly,
            onBandSelected = { priceBand = it },
            onWarbondChanged = { warbondOnly = it },
            onDismiss = { showFilter = false },
        )
    }
    if (showSort) {
        StoreSortSheet(
            backdrop = backdrop,
            palette = palette,
            order = sortOrder,
            onSelected = {
                sortOrder = it
                showSort = false
            },
            onDismiss = { showSort = false },
        )
    }
    if (showCart) {
        StoreCartSheet(
            backdrop = backdrop,
            palette = palette,
            lines = cartLines,
            onRemove = { id -> cartRepository.remove(id); cartRevision++ },
            onClear = { cartRepository.clear(); cartRevision++ },
            onCheckout = {
                pendingNotice = safeNoOp.execute(DestructiveAction.RSI_PURCHASE).message
                showCart = false
            },
            onDismiss = { showCart = false },
        )
    }
    selectedProduct?.let { product ->
        StoreProductSheet(
            backdrop = backdrop,
            palette = palette,
            product = product,
            onAddToCart = {
                cartRepository.add(product)
                cartRevision++
            },
            onOpenUpgrade = if (product.category == StoreCategory.SHIPS) {
                {
                    selectedProduct = null
                    onOpenCcu()
                }
            } else {
                null
            },
            onDismiss = { selectedProduct = null },
        )
    }
    pendingNotice?.let { notice ->
        StoreNoticeSheet(backdrop, palette, "安全结算预览", notice) { pendingNotice = null }
    }
}

@Composable
fun StoreScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    repository: StoreRepository,
    cartRepository: CartRepository,
    isDark: Boolean,
    selectedBottomTab: Int,
    onNavigate: (Int) -> Unit,
    onOpenCcu: () -> Unit,
    isOnline: Boolean,
    presence: UserPresence,
    avatarUrl: String?,
    onToggleOnline: () -> Unit,
) = StoreScreen(
    backdrop = backdrop, palette = palette, repository = repository,
    cartRepository = cartRepository, ccuPurchaseRepository = EmptyCcuPurchaseRepository,
    isDark = isDark, selectedBottomTab = selectedBottomTab, onNavigate = onNavigate,
    onOpenCcu = onOpenCcu, isOnline = isOnline, presence = presence,
    avatarUrl = avatarUrl, onToggleOnline = onToggleOnline,
)

private object EmptyCcuPurchaseRepository : CcuPurchaseRepository {
    override suspend fun catalog(): org.json.JSONObject? = null
    override suspend fun sourceIds(toId: Int): Set<Int> = emptySet()
}

@Composable
private fun StoreHeader(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    showSearch: Boolean,
    onUpgrade: () -> Unit,
    onCart: () -> Unit,
    onSearch: () -> Unit,
    isOnline: Boolean,
    presence: UserPresence,
    avatarUrl: String?,
    onToggleOnline: () -> Unit,
    ccuDiscountCount: Int,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RefugeHeaderAvatar(
            avatarUrl = avatarUrl,
            palette = palette,
            presenceColor = storePresenceColor(palette, presence),
            onClick = onToggleOnline,
        )
        Spacer(Modifier.width(RefugeSpacing.md))
        Column(Modifier.weight(1f)) {
            Text("商店", style = RefugeTypography.largeTitle(palette), maxLines = 1)
        }
        StoreHeaderActions(
            backdrop = backdrop,
            palette = palette,
            ccuDiscountCount = ccuDiscountCount,
            onUpgrade = onUpgrade,
            onCart = onCart,
            onSearch = onSearch,
        )
    }
}

@Composable
private fun StoreHeaderActions(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    ccuDiscountCount: Int,
    onUpgrade: () -> Unit,
    onCart: () -> Unit,
    onSearch: () -> Unit,
) {
    RefugeHeaderActionBar(
        backdrop = backdrop,
        palette = palette,
        actions = listOf(
            RefugeFloatingAction(
                icon = RefugeIcons.hangarUpgrade,
                label = "升级",
                onClick = onUpgrade,
            ),
            RefugeFloatingAction(RefugeIcons.cart, "购物车", onCart),
            RefugeFloatingAction(RefugeIcons.search, "搜索商品", onSearch),
        ),
        badges = if (ccuDiscountCount > 0) mapOf(0 to ccuDiscountCount) else emptyMap(),
    )
}

private fun storePresenceColor(palette: RefugePalette, presence: UserPresence): Color = when (presence) {
    UserPresence.ONLINE -> palette.positive
    UserPresence.AWAY -> Color(0xFFFFB020)
    UserPresence.DO_NOT_DISTURB -> Color(0xFFFF5C5C)
    UserPresence.PLAYING -> palette.accent
    UserPresence.INVISIBLE -> palette.textMuted
}

@Composable
private fun StoreToolbar(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    count: Int?,
    filterActive: Boolean,
    sortOrder: StoreSortOrder,
    onFilter: () -> Unit,
    onSort: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(count?.let { "$it 项" } ?: "同步中", style = RefugeTypography.caption(palette))
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
            RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.filter, if (filterActive) "筛选 · 已选" else "筛选", onFilter)
            RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.sort, when (sortOrder) {
                StoreSortOrder.DEFAULT -> "排序：默认"
                StoreSortOrder.DESCENDING -> "排序：高到低"
                StoreSortOrder.ASCENDING -> "排序：低到高"
            }, onSort)
        }
    }
}

@Composable
private fun StoreProductRow(
    palette: RefugePalette,
    product: StoreProduct,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val shipReference = rememberShipReference(product.title)
    RefugeGlassListRow(
        palette = palette,
        onClick = onClick,
        contentDescription = product.title,
        isLast = isLast,
        dividerInset = 104.dp,
        modifier = Modifier.fillMaxWidth().height(if (product.isWarbond) 132.dp else 108.dp),
    ) {
        Row(Modifier.fillMaxSize().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = "${product.title} 图片",
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ship_placeholder),
                error = painterResource(R.drawable.ship_placeholder),
                modifier = Modifier.size(92.dp).clip(refugeContinuousShape(RefugeRadius.image)),
            )
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(Modifier.fillMaxSize()) {
                Text(translatedShipName(product.title), style = RefugeTypography.headline(palette), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(RefugeSpacing.xxs))
                Text(translatedShipName(shipReference?.manufacturer ?: product.metadata), style = RefugeTypography.secondary(palette), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (product.isWarbond) {
                    Spacer(Modifier.height(RefugeSpacing.xxs))
                    Text(
                        "Warbond",
                        style = RefugeTypography.caption(palette).copy(color = palette.background),
                        modifier = Modifier
                            .clip(refugeContinuousShape(7.dp))
                            .background(StoreDiscountOrange)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(product.category.label, style = RefugeTypography.caption(palette))
                    Spacer(Modifier.weight(1f))
                    Text(
                        product.priceLabel,
                        style = RefugeTypography.value(palette).copy(
                            color = if (product.isWarbond) StoreDiscountOrange else palette.accent,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreLoadingRow(palette: RefugePalette) {
    RefugeLightweightGlassSurface(
        palette = palette,
        modifier = Modifier.fillMaxWidth().height(108.dp),
        radius = RefugeRadius.panel,
        padding = PaddingValues(10.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp).clip(refugeContinuousShape(RefugeRadius.image)).background(palette.glassStrong.copy(alpha = .20f)))
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                Box(Modifier.width(190.dp).height(14.dp).clip(refugeContinuousShape(8.dp)).background(palette.glassStrong.copy(alpha = .24f)))
                Box(Modifier.width(130.dp).height(11.dp).clip(refugeContinuousShape(8.dp)).background(palette.glassStrong.copy(alpha = .18f)))
            }
            Spacer(Modifier.weight(1f))
            CircularProgressIndicator(Modifier.size(18.dp), color = palette.accent, strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun StoreEmptyState(palette: RefugePalette, query: String, category: String) {
    RefugeLightweightGlassSurface(
        palette = palette,
        modifier = Modifier.fillMaxWidth().height(120.dp),
        radius = RefugeRadius.panel,
        padding = PaddingValues(RefugeSpacing.xl),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
            Icon(RefugeIcons.search, null, tint = palette.textMuted, modifier = Modifier.size(RefugeIconSize.medium))
            Text(if (query.isBlank()) "$category 暂无匹配商品" else "没有匹配“$query”的商品", style = RefugeTypography.body(palette))
        }
    }
}

@Composable
private fun StoreFilterSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    selectedBand: String,
    warbondOnly: Boolean,
    onBandSelected: (String) -> Unit,
    onWarbondChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    StoreSheetFrame(backdrop, palette, "筛选商品", onDismiss) {
        Text("价格区间", style = RefugeTypography.secondary(palette))
        Row(horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
            listOf("全部", "0-100", "100-500", "500+").forEach { band ->
                StoreChoicePill(palette, band, band == selectedBand) { onBandSelected(band) }
            }
        }
        Row(
            Modifier.fillMaxWidth().clickable { onWarbondChanged(!warbondOnly) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (warbondOnly) RefugeIcons.check else RefugeIcons.filter, null, tint = if (warbondOnly) palette.accent else palette.textMuted)
            Spacer(Modifier.width(RefugeSpacing.sm))
            Text("仅显示战争债券", style = RefugeTypography.body(palette))
        }
    }
}

@Composable
private fun StoreSortSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    order: StoreSortOrder,
    onSelected: (StoreSortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    StoreSheetFrame(backdrop, palette, "排序商品", onDismiss) {
        StoreSortOrder.entries.forEach { choice ->
            StoreChoiceRow(palette, choice.label, order == choice) { onSelected(choice) }
        }
    }
}

@Composable
private fun StoreProductSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    product: StoreProduct,
    onAddToCart: () -> Unit,
    onOpenUpgrade: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val shipReference = rememberShipReference(product.title)
    var addedCount by remember(product.id) { mutableIntStateOf(0) }
    RefugeLiquidSheet(
        backdrop = backdrop,
        palette = palette,
        title = translatedShipName(product.title),
        onDismiss = onDismiss,
        sheetHeight = 720.dp,
        actionOverContent = false,
        actionBottomPadding = 12.dp,
        transparentActionArea = false,
        surfaceRefraction = false,
        action = { actionBackdrop ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReferenceLiquidButton(
                    backdrop = actionBackdrop,
                    onClick = {
                        onAddToCart()
                        addedCount++
                    },
                    modifier = Modifier.weight(1f),
                    minHeight = 56.dp,
                ) {
                    Icon(
                        if (addedCount > 0) RefugeIcons.check else RefugeIcons.cart,
                        contentDescription = null,
                        tint = palette.text,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        if (addedCount > 0) "已加入 $addedCount 件" else "加入购物车",
                        style = RefugeTypography.body(palette).copy(color = palette.text),
                        maxLines = 1,
                    )
                }
                if (onOpenUpgrade != null) {
                    ReferenceLiquidButton(
                        backdrop = actionBackdrop,
                        onClick = onOpenUpgrade,
                        modifier = Modifier.weight(1f),
                        minHeight = 56.dp,
                    ) {
                        Icon(
                            RefugeIcons.hangarUpgrade,
                            contentDescription = null,
                            tint = palette.text,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            "选择升级",
                            style = RefugeTypography.body(palette).copy(color = palette.text),
                            maxLines = 1,
                        )
                    }
                }
            }
        },
    ) { _ ->
        AsyncImage(
            model = product.imageUrl,
            contentDescription = "${product.title} 大图",
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ship_placeholder),
            error = painterResource(R.drawable.ship_placeholder),
            modifier = Modifier.fillMaxWidth().height(196.dp).clip(refugeContinuousShape(RefugeRadius.panel)),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.category.label, style = RefugeTypography.caption(palette))
                Text(translatedShipName(product.metadata), style = RefugeTypography.secondary(palette), maxLines = 2)
            }
            Text(
                product.priceLabel,
                style = RefugeTypography.value(palette).copy(
                    color = if (product.isWarbond) StoreDiscountOrange else palette.accent,
                ),
            )
        }
        if (product.isWarbond) {
            Text(
                "战争债券",
                style = RefugeTypography.caption(palette).copy(color = StoreDiscountOrange),
                modifier = Modifier
                    .clip(refugeContinuousShape(7.dp))
                    .border(1.dp, StoreDiscountOrange.copy(alpha = .86f), refugeContinuousShape(7.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
        shipReference?.let { reference ->
            Text(translatedShipName(reference.manufacturer), style = RefugeTypography.headline(palette))
            if (reference.description.isNotBlank()) Text(translatedShipName(reference.description), style = RefugeTypography.body(palette))
            Column(Modifier.fillMaxWidth().clip(refugeContinuousShape(16.dp)).background(palette.contentSurfaceStrong).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                reference.details.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label, style = RefugeTypography.caption(palette), modifier = Modifier.weight(1f))
                        Text(translatedShipName(value), style = RefugeTypography.body(palette), modifier = Modifier.weight(1.5f))
                    }
                }
            }
        }
        val plainDescription = remember(product.description) { displayStorefrontText(product.description) }
        if (plainDescription.isNotBlank() && plainDescription != shipReference?.description) {
            Text(translatedShipName(plainDescription), style = RefugeTypography.body(palette))
        }
        Spacer(Modifier.height(76.dp))
    }
}

@Composable
private fun StoreCartSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    lines: List<com.refuge.next.data.CartLine>,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit,
    onDismiss: () -> Unit,
) {
    RefugeLiquidSheet(
        backdrop = backdrop,
        palette = palette,
        title = "购物车",
        onDismiss = onDismiss,
        sheetHeight = 720.dp,
        actionOverContent = false,
        actionBottomPadding = 12.dp,
        transparentActionArea = false,
        action = { actionBackdrop ->
            ReferenceLiquidButton(
                backdrop = actionBackdrop,
                onClick = if (lines.isEmpty()) onDismiss else onCheckout,
                modifier = Modifier.fillMaxWidth(),
                minHeight = 56.dp,
            ) {
                Icon(
                    if (lines.isEmpty()) RefugeIcons.check else RefugeIcons.cart,
                    contentDescription = null,
                    tint = palette.text,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    if (lines.isEmpty()) "完成" else "安全结算预览",
                    style = RefugeTypography.body(palette).copy(color = palette.text),
                )
            }
        },
    ) { modalBackdrop ->
        if (lines.isEmpty()) {
            Text("购物车为空", style = RefugeTypography.body(palette))
        } else {
            lines.forEach { line ->
                RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(translatedShipName(line.product.title), style = RefugeTypography.body(palette))
                            Text("${line.quantity} × ${line.product.priceLabel}", style = RefugeTypography.caption(palette))
                        }
                        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.more, "移除", { onRemove(line.product.id) })
                    }
                }
            }
            val total = lines.sumOf { it.product.priceCents * it.quantity }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("合计", style = RefugeTypography.headline(palette))
                Spacer(Modifier.weight(1f))
                Text(formatUsd(total), style = RefugeTypography.value(palette).copy(color = palette.accent))
            }
            RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.reclaim, "清空", onClear)
        }
        Spacer(Modifier.height(76.dp))
    }
}

@Composable
private fun StoreNoticeSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    StoreSheetFrame(backdrop, palette, title, onDismiss) {
        Text(body, style = RefugeTypography.body(palette))
    }
}

@Composable
private fun StoreSheetFrame(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    title: String,
    onDismiss: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.(LayerBackdrop) -> Unit,
) {
    RefugeLiquidSheet(
        backdrop = backdrop,
        palette = palette,
        title = title,
        onDismiss = onDismiss,
        actionOverContent = false,
        transparentActionArea = false,
        action = { actionBackdrop ->
            ReferenceLiquidButton(
                backdrop = actionBackdrop,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                minHeight = 52.dp,
            ) {
                Text("完成", style = RefugeTypography.body(palette).copy(color = palette.text))
            }
        },
    ) { modalBackdrop ->
        content(modalBackdrop)
        Spacer(Modifier.height(68.dp))
    }
}

@Composable
private fun StoreChoicePill(palette: RefugePalette, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(refugeContinuousShape(50.dp))
            .background(if (selected) palette.accentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = RefugeTypography.caption(palette).copy(color = if (selected) palette.accent else palette.textSecondary))
    }
}

@Composable
private fun StoreChoiceRow(palette: RefugePalette, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = RefugeTypography.body(palette))
        Spacer(Modifier.weight(1f))
        if (selected) Icon(RefugeIcons.check, null, tint = palette.accent, modifier = Modifier.size(RefugeIconSize.small))
    }
}

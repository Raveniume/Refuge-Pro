package com.refuge.next.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.refuge.next.data.StoreCategory
import com.refuge.next.data.StoreProduct
import com.refuge.next.data.StoreRepository
import com.refuge.next.design.RefugeIconSize
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import com.refuge.next.material.RefugeCompactUtilityPill
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeLightweightGlassSurface
import com.refuge.next.material.RefugeModalSurface
import com.refuge.next.material.RefugeStandardGlassSurface
import com.refuge.next.reference.ReferenceLiquidButton
import com.refuge.next.reference.ReferenceSearchField
import com.refuge.next.reference.ReferenceSegmentedControl

@Composable
fun StoreScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    repository: StoreRepository,
    isDark: Boolean,
    selectedBottomTab: Int,
    onNavigate: (Int) -> Unit,
    onOpenCcu: () -> Unit,
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
) {
    var products by remember { mutableStateOf(emptyList<StoreProduct>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showCart by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<StoreProduct?>(null) }
    var priceBand by remember { mutableStateOf("全部") }
    var warbondOnly by remember { mutableStateOf(false) }
    var sortDescending by remember { mutableStateOf(false) }

    LaunchedEffect(repository) {
        isLoading = true
        products = repository.products()
        isLoading = false
    }

    val category = StoreCategory.entries[selectedCategory]
    val visibleProducts = remember(products, selectedCategory, search, priceBand, warbondOnly, sortDescending) {
        products
            .asSequence()
            .filter { it.category == category }
            .filter { search.isBlank() || it.title.contains(search, ignoreCase = true) || it.metadata.contains(search, ignoreCase = true) }
            .filter {
                when (priceBand) {
                    "0-50" -> it.priceCents <= 5000
                    "50-150" -> it.priceCents in 5000..15000
                    "150+" -> it.priceCents >= 15000
                    else -> true
                }
            }
            .filter { !warbondOnly || it.isWarbond }
            .let { sequence -> if (sortDescending) sequence.sortedByDescending { it.priceCents } else sequence.sortedBy { it.priceCents } }
            .toList()
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
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md),
        ) {
            item {
                StoreHeader(
                    backdrop = backdrop,
                    palette = palette,
                    showSearch = showSearch,
                    onUpgrade = onOpenCcu,
                    onCart = { showCart = true },
                    onSearch = { showSearch = !showSearch },
                    isOnline = isOnline,
                    onToggleOnline = onToggleOnline,
                )
            }
            if (showSearch) {
                item {
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
            item {
                ReferenceSegmentedControl(
                    backdrop = backdrop,
                    isDark = isDark,
                    labels = StoreCategory.entries.map { it.label },
                    initialIndex = selectedCategory,
                    onSelected = { selectedCategory = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                StoreToolbar(
                    backdrop = backdrop,
                    palette = palette,
                    count = if (isLoading) null else visibleProducts.size,
                    filterActive = priceBand != "全部" || warbondOnly,
                    sortDescending = sortDescending,
                    onFilter = { showFilter = true },
                    onSort = { showSort = true },
                )
            }
            if (isLoading) {
                items(5, key = { "loading-$it" }) { StoreLoadingRow(palette) }
            } else if (visibleProducts.isEmpty()) {
                item { StoreEmptyState(palette, search, category.label) }
            } else {
                items(visibleProducts, key = { it.id }) { product ->
                    StoreProductGlassRow(
                        palette = palette,
                        product = product,
                        onClick = { selectedProduct = product },
                    )
                }
            }
        }

        RootBottomNav(backdrop, isDark, selectedBottomTab, onNavigate)
    }

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
            descending = sortDescending,
            onSelected = {
                sortDescending = it
                showSort = false
            },
            onDismiss = { showSort = false },
        )
    }
    if (showCart) {
        StoreNoticeSheet(
            backdrop = backdrop,
            palette = palette,
            title = "购物车",
            body = "购物车会在 checkout adapter 接入后同步 RSI 账户。",
            onDismiss = { showCart = false },
        )
    }
    selectedProduct?.let { product ->
        StoreProductSheet(
            backdrop = backdrop,
            palette = palette,
            product = product,
            onDismiss = { selectedProduct = null },
        )
    }
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
    onToggleOnline: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.user_profile_pic),
                contentDescription = "用户头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .graphicsLayer { scaleX = 1.9f; scaleY = 1.9f }
                    .semantics { contentDescription = "切换在线状态"; role = Role.Button }
                    .clickable(onClick = onToggleOnline),
            )
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(Modifier.weight(1f)) {
                Text("商店", style = RefugeTypography.largeTitle(palette), maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(if (isOnline) palette.positive else palette.textMuted, CircleShape))
                    Spacer(Modifier.width(RefugeSpacing.xxs))
                    Text(if (isOnline) "在线 · 本地同步" else "离线 · 本地同步", style = RefugeTypography.secondary(palette), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            StoreHeaderAction(backdrop, palette, RefugeIcons.upgrade, "升级", onUpgrade)
            Spacer(Modifier.width(RefugeSpacing.xs))
            StoreHeaderAction(backdrop, palette, RefugeIcons.cart, "购物车", onCart)
            Spacer(Modifier.width(RefugeSpacing.xs))
            StoreHeaderAction(backdrop, palette, if (showSearch) RefugeIcons.more else RefugeIcons.search, "搜索商品", onSearch)
        }
    }
}

@Composable
private fun StoreHeaderAction(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    ReferenceLiquidButton(
        backdrop = backdrop,
        onClick = onClick,
        modifier = Modifier.size(44.dp),
    ) {
        Icon(icon, label, tint = palette.textSecondary, modifier = Modifier.size(RefugeIconSize.medium))
    }
}

@Composable
private fun StoreToolbar(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    count: Int?,
    filterActive: Boolean,
    sortDescending: Boolean,
    onFilter: () -> Unit,
    onSort: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
            RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.filter, if (filterActive) "筛选 · 已选" else "筛选", onFilter)
            RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.sort, if (sortDescending) "排序：高到低" else "排序：默认", onSort)
        }
        Spacer(Modifier.weight(1f))
        Text(count?.let { "$it 项" } ?: "同步中", style = RefugeTypography.caption(palette))
    }
}

@Composable
private fun StoreProductGlassRow(
    palette: RefugePalette,
    product: StoreProduct,
    onClick: () -> Unit,
) {
    RefugeLightweightGlassSurface(
        palette = palette,
        modifier = Modifier.fillMaxWidth().height(108.dp),
        radius = RefugeRadius.panel,
        onClick = onClick,
        contentDescription = product.title,
        padding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = "${product.title} 图片",
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ship_placeholder),
                error = painterResource(R.drawable.ship_placeholder),
                modifier = Modifier.size(92.dp).clip(RoundedCornerShape(RefugeRadius.image)),
            )
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(Modifier.fillMaxSize()) {
                Text(product.title, style = RefugeTypography.headline(palette), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(RefugeSpacing.xxs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(product.metadata, style = RefugeTypography.secondary(palette), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (product.isWarbond) {
                        Spacer(Modifier.width(RefugeSpacing.xs))
                        Text("战争债券", style = RefugeTypography.caption(palette).copy(color = palette.warning))
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(product.category.label, style = RefugeTypography.caption(palette))
                    Spacer(Modifier.weight(1f))
                    Text(product.priceLabel, style = RefugeTypography.value(palette).copy(color = palette.accent))
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
            Box(Modifier.size(92.dp).clip(RoundedCornerShape(RefugeRadius.image)).background(palette.glassStrong.copy(alpha = .20f)))
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                Box(Modifier.width(190.dp).height(14.dp).clip(RoundedCornerShape(8.dp)).background(palette.glassStrong.copy(alpha = .24f)))
                Box(Modifier.width(130.dp).height(11.dp).clip(RoundedCornerShape(8.dp)).background(palette.glassStrong.copy(alpha = .18f)))
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
            listOf("全部", "0-50", "50-150", "150+").forEach { band ->
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
    descending: Boolean,
    onSelected: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    StoreSheetFrame(backdrop, palette, "排序商品", onDismiss) {
        StoreChoiceRow(palette, "默认", !descending) { onSelected(false) }
        StoreChoiceRow(palette, "价格从高到低", descending) { onSelected(true) }
    }
}

@Composable
private fun StoreProductSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    product: StoreProduct,
    onDismiss: () -> Unit,
) {
    StoreSheetFrame(backdrop, palette, product.title, onDismiss) {
        AsyncImage(
            model = product.imageUrl,
            contentDescription = "${product.title} 大图",
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ship_placeholder),
            error = painterResource(R.drawable.ship_placeholder),
            modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(RefugeRadius.panel)),
        )
        Text(product.metadata, style = RefugeTypography.secondary(palette))
        Text(product.description, style = RefugeTypography.body(palette))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(product.priceLabel, style = RefugeTypography.value(palette).copy(color = palette.accent))
            Spacer(Modifier.weight(1f))
            RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.cart, "加入购物车", onDismiss)
        }
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
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(palette.scrim), contentAlignment = Alignment.BottomCenter) {
            RefugeModalSurface(
                palette = palette,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 20.dp),
                radius = RefugeRadius.floating,
                fill = palette.contentSurfaceStrong,
                padding = PaddingValues(RefugeSpacing.xl),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md)) {
                    Box(Modifier.width(34.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(palette.outline))
                    Text(title, style = RefugeTypography.title(palette))
                    content()
                    RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.chevron, "完成", onDismiss, modifier = Modifier.align(Alignment.End))
                }
            }
        }
    }
}

@Composable
private fun StoreChoicePill(palette: RefugePalette, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) palette.accentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(label, style = RefugeTypography.caption(palette).copy(color = if (selected) palette.accent else palette.textSecondary))
    }
}

@Composable
private fun StoreChoiceRow(palette: RefugePalette, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = RefugeTypography.body(palette))
        Spacer(Modifier.weight(1f))
        if (selected) Icon(RefugeIcons.check, null, tint = palette.accent, modifier = Modifier.size(RefugeIconSize.small))
    }
}

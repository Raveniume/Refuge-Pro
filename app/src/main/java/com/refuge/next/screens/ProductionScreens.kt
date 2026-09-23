package com.refuge.next.screens

import com.refuge.next.data.facetValue
import com.refuge.next.data.terminalFacetKeys
import com.refuge.next.data.accepts

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.saveable.rememberSaveable
import com.refuge.next.design.translatedShipName
import com.refuge.next.design.translatedGameItemName
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.refuge.next.R
import com.refuge.next.BuildConfig
import com.refuge.next.data.ProductionTerminalRepository
import com.refuge.next.data.CcuRepository
import com.refuge.next.data.CcuRoutePlan
import com.refuge.next.data.CcuShip
import com.refuge.next.data.OwnedCcu
import com.refuge.next.data.ProfileData
import com.refuge.next.data.ProfileRepository
import com.refuge.next.data.UtilityRepository
import com.refuge.next.data.eligibleTargetShips
import com.refuge.next.data.planCcuRoute
import com.refuge.next.data.OwnedShip
import com.refuge.next.data.TerminalCategory
import com.refuge.next.data.TerminalItem
import com.refuge.next.data.TerminalRepository
import com.refuge.next.data.TerminalLoadoutStats
import com.refuge.next.data.calculateTerminalLoadoutStats
import com.refuge.next.data.ToolItem
import com.refuge.next.data.UserPresence
import com.refuge.next.data.ToolDetail
import com.refuge.next.data.DestructiveAction
import com.refuge.next.data.SafeNoOpDestructiveActionExecutor
import com.refuge.next.data.ProductionCacheManifest
import com.refuge.next.data.formatUsd
import com.refuge.next.design.RefugeIconSize
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import com.refuge.next.design.refugeContinuousShape
import com.refuge.next.material.RefugeCompactUtilityPill
import com.refuge.next.material.RefugeContentSurface
import com.refuge.next.material.RefugeGlassControl
import com.refuge.next.material.RefugeGlassSurface
import com.refuge.next.material.PageGlassScope
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeImagePlaceholder
import com.refuge.next.material.RefugeRemoteImage
import com.refuge.next.material.RefugeLightweightGlassSurface
import com.refuge.next.material.RefugeQuietLiquidGlassSurface
import com.refuge.next.material.RefugeGlassListRow
import com.refuge.next.material.RefugeLiquidToggle
import com.refuge.next.material.RefugeLiquidSheet
import com.refuge.next.material.RefugeLiquidSegmented
import com.refuge.next.material.RefugeLiquidModeSelector
import com.refuge.next.material.RefugeLiquidIconButton
import com.refuge.next.material.RefugeCircularHeaderButton
import com.refuge.next.material.RefugeFloatingAction
import com.refuge.next.material.RefugeHeaderActionBar
import com.refuge.next.material.RefugeHeaderAvatar
import com.refuge.next.material.RefugePullToRefresh
import com.refuge.next.material.refugeTopEdgeFade
import com.refuge.next.material.RefugeAnimatedSearch
import com.refuge.next.material.RefugeModalSurface
import com.refuge.next.material.RefugeStandardGlassSurface
import com.refuge.next.reference.ReferenceLiquidButton
import com.refuge.next.reference.OfficialLiquidButtonPort
import com.refuge.next.reference.ReferenceLiquidBottomTabs
import com.refuge.next.reference.ReferenceLiquidSelectionBar
import com.refuge.next.reference.ReferenceSearchField
import com.refuge.next.reference.ReferenceSegmentedControl
import com.refuge.next.reference.ReferenceSelectionItem

private data class RootTab(
    val route: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
)

private val rootTabs = listOf(
    RootTab(0, RefugeIcons.home, RefugeIcons.homeSelected, "机库"),
    RootTab(1, RefugeIcons.storeOutline, RefugeIcons.storeSelected, "商店"),
    RootTab(2, RefugeIcons.terminal, RefugeIcons.terminalSelected, "终端"),
    RootTab(4, RefugeIcons.profile, RefugeIcons.profileSelected, "我的"),
)


private fun presenceColor(palette: RefugePalette, presence: UserPresence): Color = when (presence) {
    UserPresence.ONLINE -> palette.positive
    UserPresence.AWAY -> Color(0xFFFFB020)
    UserPresence.DO_NOT_DISTURB -> Color(0xFFFF5C5C)
    UserPresence.PLAYING -> palette.accent
    UserPresence.INVISIBLE -> palette.textMuted
}

@Composable
fun PresencePickerSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    selected: UserPresence,
    onSelected: (UserPresence) -> Unit,
    onDismiss: () -> Unit,
) {
    RefugeLiquidSheet(
        backdrop = backdrop,
        palette = palette,
        title = "在线状态",
        onDismiss = onDismiss,
        // Presence is a compact picker. A fixed content-sized sheet keeps the
        // profile page visible instead of opening a full-height modal.
        sheetHeight = 320.dp,
    ) { _ ->
        UserPresence.entries.forEach { presence ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelected(presence) }.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).background(presenceColor(palette, presence), CircleShape))
                Spacer(Modifier.width(RefugeSpacing.md))
                Text(presence.label, style = RefugeTypography.body(palette).copy(color = palette.text))
                Spacer(Modifier.weight(1f))
                if (presence == selected) Box(Modifier.size(7.dp).background(palette.accent, CircleShape))
            }
        }
    }
}

@Composable
fun BoxScope.RootBottomNav(
    backdrop: Backdrop,
    isDark: Boolean,
    selected: Int,
    onNavigate: (Int) -> Unit,
) {
    val selectedIndex = rootTabs.indexOfFirst { it.route == selected }.coerceAtLeast(0)
    ReferenceLiquidBottomTabs(
        backdrop = backdrop,
        isDark = isDark,
        tabsCount = rootTabs.size,
        selectedIndex = selectedIndex,
        onSelected = { index -> onNavigate(rootTabs[index.coerceIn(rootTabs.indices)].route) },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
    ) { selectedIndex, select ->
        rootTabs.forEachIndexed { index, tab ->
            ReferenceSelectionItem(
                icon = if (index == selectedIndex) tab.selectedIcon else tab.icon,
                label = tab.label,
                selected = index == selectedIndex,
                isDark = isDark,
                onClick = { select(index) },
            )
        }
    }
}

@Composable
fun ProductionHeader(
    palette: RefugePalette,
    title: String,
    presence: UserPresence,
    avatarUrl: String? = null,
    onAvatarClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        RefugeHeaderAvatar(
            avatarUrl = avatarUrl,
            palette = palette,
            presenceColor = presenceColor(palette, presence),
            onClick = onAvatarClick,
        )
        Spacer(Modifier.width(RefugeSpacing.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(title, style = RefugeTypography.largeTitle(palette), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

@Composable
fun TerminalScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    isDark: Boolean,
    selectedBottomTab: Int,
    onNavigate: (Int) -> Unit,
    repository: TerminalRepository = remember { ProductionTerminalRepository() },
    isOnline: Boolean,
    presence: UserPresence,
    avatarUrl: String?,
    onToggleOnline: () -> Unit,
    onOverlayVisibilityChanged: (Boolean) -> Unit = {},
) {
    var categoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var items by remember(repository) { mutableStateOf(repository.cachedItems()) }
    var loading by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var sortDescending by rememberSaveable { mutableStateOf(false) }
    var terminalFilters by rememberSaveable(categoryIndex) { mutableStateOf(hashMapOf<String, Set<String>>()) }
    var pricedOnly by rememberSaveable { mutableStateOf(false) }
    var taggedOnly by rememberSaveable { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<TerminalItem?>(null) }
    var showLoadout by rememberSaveable { mutableStateOf(false) }
    var loadoutShipId by rememberSaveable { mutableStateOf<String?>(null) }
    val loadoutShip = items.firstOrNull { it.id == loadoutShipId }
    // Strings/maps are saved with this root route, including empty slots. Keep
    // each vessel's draft when closing the sheet or visiting another root tab.
    var loadouts by rememberSaveable { mutableStateOf(hashMapOf<String, HashMap<String, String>>()) }
    var loadoutPicker by remember { mutableStateOf<TerminalCategory?>(null) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(selectedItem != null, showLoadout, loadoutPicker != null, showFilter) {
        onOverlayVisibilityChanged(selectedItem == null && !showLoadout && loadoutPicker == null && !showFilter)
    }
    val listState = com.refuge.next.navigation.rememberRootListState(2)

    LaunchedEffect(repository, loadAttempt) {
        loading = items.isEmpty()
        loadError = null
        runCatching { repository.awaitCachedItems() }.onSuccess { cached ->
            if (cached.isNotEmpty()) {
                items = cached
                loading = false
            }
        }
        // Keep the cached list interactive while the large Wiki refresh runs
        // on the repository-owned IO scope, then publish its completed result.
        repository.refreshInBackground()
        runCatching { repository.items() }
            .onSuccess { refreshed -> if (refreshed.isNotEmpty()) items = refreshed }
            .onFailure { if (items.isEmpty()) loadError = it.message ?: "终端资料读取失败" }
        loading = false
        isRefreshing = false
    }
    LaunchedEffect(repository, selectedItem?.id) {
        val cached = selectedItem ?: return@LaunchedEffect
        runCatching { repository.detail(cached) }
            .onSuccess { enriched ->
                if (selectedItem?.id == enriched.id) selectedItem = enriched
            }
    }
    LaunchedEffect(items) {
        if (loadoutShipId !in items.map { it.id }) {
            loadoutShipId = items.firstOrNull { it.category == TerminalCategory.VEHICLES }?.id
        }
    }
    val category = TerminalCategory.entries[categoryIndex]
    val visible = remember(items, categoryIndex, query, sortDescending, terminalFilters) {
        val filtered = items.filter {
            it.category == category &&
                (query.isBlank() || it.name.contains(query, true) || it.manufacturer.contains(query, true)) &&
                terminalFacetKeys(category).all { key -> terminalFilters.accepts(key, listOfNotNull(it.facetValue(key))) }
        }
        if (sortDescending) filtered.sortedByDescending { it.name } else filtered.sortedBy { it.name }
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
            Modifier.fillMaxSize().statusBarsPadding().refugeTopEdgeFade(palette.background),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = RefugeSpacing.rootNavigation),
            verticalArrangement = Arrangement.Top,
            state = listState,
        ) {
            item {
                ProductionHeader(
                    palette = palette,
                    title = "终端",
                    presence = presence,
                    avatarUrl = avatarUrl,
                    onAvatarClick = onToggleOnline,
                    actions = {
                        RefugeHeaderActionBar(
                            backdrop,
                            palette,
                            listOf(
                                RefugeFloatingAction(RefugeIcons.loadout, "改船", { showLoadout = true }),
                                RefugeFloatingAction(RefugeIcons.search, "搜索", { showSearch = !showSearch }),
                            ),
                        )
                    },
                )
            }
            item {
                // Draw above rows while the expand animation is running.
                // Later LazyColumn children otherwise cover the field's lower
                // edge and make the search/header appear to穿模.
                RefugeAnimatedSearch(
                    visible = showSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(4f),
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        ReferenceSearchField(
                            backdrop,
                            isDark,
                            query,
                            { query = it },
                            RefugeIcons.search,
                            Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                Box(
                    Modifier.fillMaxWidth().padding(
                        // The expanded field gets the same breathing room on
                        // both sides; keeping the category bar at 8 dp avoids
                        // the search field touching the next control.
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    RefugeLiquidModeSelector(
                        backdrop = backdrop,
                        palette = palette,
                        labels = TerminalCategory.entries.map { it.label },
                        selectedIndex = categoryIndex,
                        onSelected = { categoryIndex = it },
                        modifier = Modifier.fillMaxWidth(.90f),
                    )
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = RefugeSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${category.label}资料", style = RefugeTypography.headline(palette))
                    Spacer(Modifier.weight(1f))
                    RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.filter, "筛选", { showFilter = true })
                    Spacer(Modifier.width(RefugeSpacing.xs))
                    RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.sort, if (sortDescending) "排序：Z-A" else "排序：默认", { sortDescending = !sortDescending })
                }
            }
            if (loading && items.isEmpty()) {
                item { ProductionLoadingState(backdrop, palette, "正在读取终端资料") }
            } else if (loadError != null && items.isEmpty()) {
                item { ProductionErrorState(backdrop, palette, loadError!!, onRetry = { loadAttempt++ }) }
            } else if (visible.isEmpty()) {
                item { ProductionEmptyState(palette, "暂无${category.label}资料") }
            } else {
                itemsIndexed(
                    items = visible,
                    key = { _, item -> "terminal:${item.id}" },
                    contentType = { _, _ -> "terminal" },
                ) { index, item ->
                    TerminalListChunk(
                        backdrop = backdrop,
                        palette = palette,
                        modifier = Modifier.fillMaxWidth(),
                        roundTop = index == 0,
                        roundBottom = index == visible.lastIndex,
                    ) {
                        TerminalRow(
                            palette = palette,
                            item = item,
                            isLast = index == visible.lastIndex,
                            onClick = { selectedItem = item },
                        )
                    }
                }
            }
        }
        }
        },
        overlay = { _ -> },
    )

    selectedItem?.let { TerminalDetailSheet(backdrop, palette, it) { selectedItem = null } }
    if (showLoadout) {
        NativeLoadoutScreen(backdrop = backdrop, palette = palette, isDark = isDark, onDismiss = { showLoadout = false })
        /* Legacy terminal loadout remains compiled for migration previews. */
        /*
        TerminalLoadoutSheet(
            repository = repository,
            backdrop = backdrop,
            palette = palette,
            isDark = isDark,
            items = items,
            ship = loadoutShip,
            overrides = loadouts[loadoutShipId].orEmpty(),
            onShipSelected = { loadoutShipId = it.id },
            onOverridesChanged = { updated ->
                loadoutShipId?.let { id -> loadouts = HashMap(loadouts).apply { put(id, HashMap(updated)) } }
            },
            picker = loadoutPicker,
            onOpenPicker = { loadoutPicker = it },
            onClosePicker = { loadoutPicker = null },
            onDismiss = { showLoadout = false },
        )*/
    }
    if (showFilter) FacetFilterSheet(backdrop, palette, "终端筛选",
        terminalFacetKeys(category).associateWith { key -> items.filter { it.category == category }.mapNotNull { it.facetValue(key) }.distinct().sorted() },
        terminalFilters, { terminalFilters = HashMap(it) }, { showFilter = false })
}

@Composable
private fun HeaderAction(backdrop: LayerBackdrop, palette: RefugePalette, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    RefugeCircularHeaderButton(backdrop, palette, icon, label, onClick)
}

private val loadoutCategories = listOf(
    TerminalCategory.SHIP_COMPONENTS to "舰载组件",
    TerminalCategory.SHIELDS to "护盾",
    TerminalCategory.COOLERS to "冷却器",
    TerminalCategory.POWER_PLANTS to "发电机",
    TerminalCategory.QUANTUM_DRIVES to "量子引擎",
)

@Composable
private fun TerminalLoadoutSheet(
    backdrop: LayerBackdrop, palette: RefugePalette, isDark: Boolean, items: List<TerminalItem>,
    ship: TerminalItem?, overrides: Map<String, String>,
    onShipSelected: (TerminalItem) -> Unit, onOverridesChanged: (Map<String, String>) -> Unit,
    picker: TerminalCategory?, onOpenPicker: (TerminalCategory) -> Unit, onClosePicker: () -> Unit,
    onDismiss: () -> Unit, repository: TerminalRepository,
) {
    var detailedShip by remember(ship?.id) { mutableStateOf(ship) }
    var loading by remember(ship?.id) { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var activePort by remember(ship?.id) { mutableStateOf<com.refuge.next.data.TerminalPort?>(null) }
    var search by remember(picker, activePort?.id) { mutableStateOf("") }
    LaunchedEffect(repository, ship?.id, attempt) {
        val selected = ship ?: return@LaunchedEffect
        loading = true
        try { detailedShip = repository.detail(selected) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { detailedShip = selected }
        finally { loading = false }
    }
    val vessel = detailedShip
    val catalog = remember(items, vessel) { items.associateBy { it.id } + vessel?.ports.orEmpty().mapNotNull { it.equippedItem }.associateBy { it.id } }
    val ports = remember(vessel) { vessel?.ports.orEmpty().filter { com.refuge.next.data.terminalPortCategory(it) != null } }
    val selectedIds = overrides.mapValues { it.value.takeIf(String::isNotEmpty) }
    val stats = remember(vessel, overrides, catalog) { vessel?.let { com.refuge.next.data.calculateTerminalPortPerformance(it, selectedIds, catalog) } }
    RefugeLiquidSheet(backdrop, palette, "改船 / 配装", onDismiss, sheetHeight = 780.dp) { modalBackdrop ->
        LoadoutOption(palette, vessel?.let { translatedGameItemName(it.name, it.className) } ?: "选择舰船", "", vessel != null) {
            activePort = null
            onOpenPicker(TerminalCategory.VEHICLES)
        }
        if (loading) Text("正在读取原厂挂点与性能…", style = RefugeTypography.caption(palette))
        Text("配装预览 · " + (vessel?.sourceVersion ?: "版本未知"), style = RefugeTypography.headline(palette))
        RefugeContentSurface(palette, Modifier.fillMaxWidth(), radius = RefugeRadius.panel, padding = PaddingValues(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    LoadoutMetric(palette, "总质量 kg", stats?.massTotalKg?.cleanNumber() ?: "—")
                    LoadoutMetric(palette, "护盾容量", stats?.shieldHp?.cleanNumber() ?: "—")
                    LoadoutMetric(palette, "货舱 SCU", stats?.cargoScu?.cleanNumber() ?: "—")
                }
                Row(Modifier.fillMaxWidth()) {
                    LoadoutMetric(palette, "供电段", stats?.powerGeneration?.cleanNumber() ?: "—")
                    LoadoutMetric(palette, "冷却段", stats?.coolingGeneration?.cleanNumber() ?: "—")
                    LoadoutMetric(palette, "驾驶员 DPS", stats?.pilotDps?.cleanNumber() ?: "—")
                }
            }
        }
        if (!loading && ports.isEmpty()) {
            Text("尚无完整挂点数据，无法验证配装。", style = RefugeTypography.body(palette))
            LoadoutOption(palette, "重新读取", "获取舰船详情", false) { attempt++ }
        }
        ports.forEach { port ->
            val id = if (overrides.containsKey(port.id)) overrides[port.id] else port.equippedItemId
            val equipped = catalog[id] ?: port.equippedItem?.takeIf { it.id == id }
            val editable = com.refuge.next.data.terminalPortCanEdit(port)
            Text(port.id.substringAfter(':').replace('/', ' '), style = RefugeTypography.caption(palette))
            LoadoutOption(
                palette, equipped?.let { translatedGameItemName(it.name, it.className) } ?: if (id.isNullOrEmpty()) "空槽" else "组件资料缺失",
                listOfNotNull(com.refuge.next.data.terminalPortCategory(port)?.label, port.minSize?.let { "S" + it },
                    if (editable) "点击更换" else "固定组件 / 保留装配结构").joinToString(" · "),
                overrides.containsKey(port.id),
            ) { if (editable && !loading) { activePort = port; onOpenPicker(com.refuge.next.data.terminalPortCategory(port)!!) } }
            equipped?.performance?.let { p ->
                val values = listOfNotNull(p.burstDps?.let { "单件爆发 DPS " + it.cleanNumber() },
                    p.sustainedDps?.takeIf { equipped.category == TerminalCategory.SHIP_COMPONENTS }?.let { "单件持续 DPS " + it.cleanNumber() },
                    p.shieldHp?.let { "容量 " + it.cleanNumber() }, p.quantumSpeed?.let { "量子速度 " + it.cleanNumber() + " m/s" })
                if (values.isNotEmpty()) Text(values.joinToString(" · "), style = RefugeTypography.caption(palette))
            }
        }
        if (overrides.isNotEmpty()) LoadoutOption(palette, "恢复原厂配装", "清除本舰船的本地更改", false) { onOverridesChanged(emptyMap()) }
        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.chevron, "完成", onDismiss, Modifier.align(Alignment.End))
    }
    if (picker != null) {
        val port = activePort
        val options = remember(items, picker, port, search, vessel?.sourceVersion) {
            items.filter { option ->
                (if (picker == TerminalCategory.VEHICLES) option.category == picker else port != null && com.refuge.next.data.terminalPortAccepts(port, option, vessel?.sourceVersion)) &&
                    (search.isBlank() || option.name.contains(search, true) || option.manufacturer.contains(search, true) || option.className.orEmpty().contains(search, true))
            }.sortedBy { it.name }
        }
        RefugeLiquidSheet(backdrop, palette, if (picker == TerminalCategory.VEHICLES) "选择舰船" else "更换组件", onClosePicker,
            sheetHeight = 760.dp, contentScrollable = false) {
            androidx.compose.material.TextField(value = search, onValueChange = { search = it },
                label = { Text("搜索名称 / 制造商") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("匹配 " + options.size + " 项", style = RefugeTypography.caption(palette))
            if (picker != TerminalCategory.VEHICLES && port != null) {
                LoadoutOption(palette, "恢复此槽原厂组件", "仅修改本地预览", false) { onOverridesChanged(overrides - port.id); onClosePicker() }
                LoadoutOption(palette, "卸下此槽组件", "保留空槽", false) { onOverridesChanged(overrides + (port.id to "")); onClosePicker() }
            }
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (options.isEmpty()) item { Text("没有匹配组件；仅显示尺寸、类型、标签和版本可验证的选项。", style = RefugeTypography.body(palette)) }
                items(options, key = { it.id }) { option ->
                    LoadoutOption(palette, translatedGameItemName(option.name, option.className),
                        listOfNotNull(translatedShipName(option.manufacturer), option.size?.let { "S" + it }).joinToString(" · "),
                        option.id == if (picker == TerminalCategory.VEHICLES) ship?.id else (overrides[port?.id] ?: port?.equippedItemId)) {
                        if (picker == TerminalCategory.VEHICLES) onShipSelected(option)
                        else if (port != null) onOverridesChanged(overrides + (port.id to option.id))
                        onClosePicker()
                    }
                }
            }
        }
    }
}


@Composable
private fun LoadoutOption(
    palette: RefugePalette,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    RefugeLightweightGlassSurface(
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentDescription = title,
        padding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = RefugeTypography.body(palette).copy(color = if (selected) palette.accent else palette.text), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) Text(subtitle, style = RefugeTypography.caption(palette), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (selected) Icon(RefugeIcons.success, null, tint = palette.accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RowScope.LoadoutMetric(palette: RefugePalette, label: String, value: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = RefugeTypography.value(palette).copy(color = palette.accent), maxLines = 1)
        Text(label, style = RefugeTypography.caption(palette))
    }
}

private fun Double.cleanNumber(): String = String.format(java.util.Locale.US, if (this % 1.0 == 0.0) "%,.0f" else "%,.1f", this)

/** Try every source-provided image before displaying a placeholder. */
@Composable
private fun TerminalRemoteImage(
    item: TerminalItem,
    fallback: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    loadingColor: Color = Color.White,
) {
    val shipReference = rememberShipReference(item.name)
    val urls = remember(item.imageUrl, item.imageUrls, shipReference) {
        (shipReference?.images.orEmpty() + listOfNotNull(item.imageUrl) + item.imageUrls).mapNotNull { com.refuge.next.data.normalizeImageUrl(it) }.distinct()
    }
    var index by remember(urls) { mutableIntStateOf(0) }
    AsyncImage(
        model = urls.getOrNull(index), contentDescription = contentDescription,
        modifier = modifier, contentScale = contentScale,
        placeholder = fallback, error = fallback, fallback = fallback,
        onError = { error ->
            android.util.Log.w("RefugeImage", "Terminal image unavailable: ${urls.getOrNull(index)?.substringBefore("?")}", error.result.throwable)
            if (index < urls.lastIndex) index++
        },
    )
}

/**
 * Rows share one opaque visual group, but are measured and recycled separately.
 */
@Composable
private fun TerminalListChunk(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    roundTop: Boolean,
    roundBottom: Boolean,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val shape = refugeContinuousShape(
        topStart = if (roundTop) RefugeRadius.panel else 0.dp,
        topEnd = if (roundTop) RefugeRadius.panel else 0.dp,
        bottomEnd = if (roundBottom) RefugeRadius.panel else 0.dp,
        bottomStart = if (roundBottom) RefugeRadius.panel else 0.dp,
    )
    Box(
        modifier
            .clip(shape)
            .background(palette.contentSurface)
            .padding(horizontal = 10.dp),
    ) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun TerminalRow(
    palette: RefugePalette,
    item: TerminalItem,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    RefugeGlassListRow(
        palette = palette,
        onClick = onClick,
        contentDescription = item.name,
        isLast = isLast,
        dividerInset = 108.dp,
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
            if (!item.imageUrl.isNullOrBlank() || rememberShipReference(item.name)?.images?.isNotEmpty() == true) {
TerminalRemoteImage(
                    item = item,
                    contentDescription = "${item.name} 图片",
                    fallback = painterResource(R.drawable.ship_placeholder),
                    contentScale = ContentScale.Crop,
                    loadingColor = palette.accent,
                    modifier = Modifier.size(92.dp).clip(refugeContinuousShape(RefugeRadius.image)),
                )
            } else {
                RefugeImagePlaceholder(palette, Modifier.size(92.dp), item.category.label)
            }
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(
                Modifier.weight(1f).heightIn(min = 100.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(translatedGameItemName(item.name, item.className), style = RefugeTypography.title(palette), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        translatedShipName(item.manufacturer),
                        style = RefugeTypography.body(palette).copy(fontSize = 14.sp, lineHeight = 19.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val tagLine = item.tags.filter(::isTerminalValuePresent).take(2).joinToString(" · ")
                    if (tagLine.isNotBlank()) {
                        Text(
                            translatedShipName(tagLine),
                            style = RefugeTypography.body(palette).copy(color = palette.accent),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(
                        item.value,
                        style = RefugeTypography.body(palette),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(.62f),
                    )
                    Text(
                        item.usd,
                        style = RefugeTypography.value(palette).copy(
                            color = palette.accent,
                            fontSize = 16.sp,
                            lineHeight = 21.sp,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(.38f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalSkeleton(palette: RefugePalette) {
    RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth().height(104.dp), padding = PaddingValues(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(88.dp).background(palette.glassStrong.copy(alpha = .2f), refugeContinuousShape(RefugeRadius.image)))
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                Box(Modifier.width(170.dp).height(14.dp).background(palette.glassStrong.copy(alpha = .24f), refugeContinuousShape(8.dp)))
                Box(Modifier.width(110.dp).height(11.dp).background(palette.glassStrong.copy(alpha = .18f), refugeContinuousShape(8.dp)))
            }
        }
    }
}

@Composable
fun ProfileScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    isDark: Boolean,
    selectedBottomTab: Int,
    onNavigate: (Int) -> Unit,
    profileRepository: ProfileRepository,
    utilityRepository: UtilityRepository,
    onToggleTheme: () -> Unit,
    onOpenLogin: () -> Unit,
    isOnline: Boolean,
    presence: UserPresence,
    initialProfile: ProfileData,
    fleetSummary: String = "舰队资料",
    onToggleOnline: () -> Unit,
    onOverlayVisibilityChanged: (Boolean) -> Unit = {},
) {
    var profileData by remember(profileRepository) { mutableStateOf(initialProfile) }
    var profileToolGroups by remember { mutableStateOf(emptyList<Pair<String, List<ToolItem>>>()) }
    var profileLoading by remember { mutableStateOf(!profileData.isAuthenticated) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(initialProfile) {
        if (initialProfile.isAuthenticated) profileData = initialProfile
    }
    LaunchedEffect(profileRepository, utilityRepository, loadAttempt) {
        profileLoading = !profileData.isAuthenticated
        profileError = null
        // The utility directory is local/instant. Publish it before the slower
        // authenticated account refresh so returning to Profile never blanks
        // already usable routes while RSI is loading in the background.
        runCatching { utilityRepository.groups() }.onSuccess { profileToolGroups = it }
        runCatching { profileRepository.awaitCachedProfile() }.onSuccess { cached ->
            if (cached.isAuthenticated) {
                profileData = cached
                profileLoading = false
            }
        }
        runCatching { profileRepository.profile() }
            .onSuccess { profileData = it }
            .onFailure { profileError = it.message ?: "账户资料读取失败" }
        profileLoading = false
    }
    val profile = profileData.copy(isOnline = isOnline)
    var selectedTool by remember { mutableStateOf<ToolItem?>(null) }
    var selectedToolDetail by remember { mutableStateOf<ToolDetail?>(null) }
    LaunchedEffect(selectedTool) {
        onOverlayVisibilityChanged(selectedTool == null)
    }
    LaunchedEffect(selectedTool, utilityRepository) {
        selectedToolDetail = null
        selectedToolDetail = selectedTool?.let { utilityRepository.detail(it.id) }
    }
    PageGlassScope(
        backdrop = backdrop,
        content = {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().refugeTopEdgeFade(palette.background),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = RefugeSpacing.rootNavigation),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md),
            state = com.refuge.next.navigation.rememberRootListState(4),
        ) {
            item {
                ProductionHeader(
                    palette = palette,
                    title = "我的",
                    presence = presence,
                    avatarUrl = profile.avatarUrl,
                    onAvatarClick = onToggleOnline,
                    actions = {
                        HeaderAction(backdrop, palette, RefugeIcons.profile, "连接 RSI 账户", onOpenLogin)
                    },
                )
            }
            if (profileLoading && !profileData.isAuthenticated) {
                item { ProductionLoadingState(backdrop, palette, "正在读取账户资料") }
            } else if (profileError != null && !profileData.isAuthenticated) {
                item { ProductionErrorState(backdrop, palette, profileError!!, onRetry = { loadAttempt++ }) }
            } else {
                item { ProfileHero(backdrop, palette, profile, presence, fleetSummary) }
                item { ProfileStats(backdrop, palette, profile) }
                item { ProfileAccountGroup(backdrop, palette, profile) }
                item {
                    ProfileUtilities(backdrop, palette, profileToolGroups) { tool ->
                        // The original app's ship/equipment utility entries open
                        // the searchable terminal directly; keep that behavior
                        // instead of trapping the user in a static info sheet.
                        if (tool.id == "ships" || tool.id == "equipment") onNavigate(2)
                        else selectedTool = tool
                    }
                }
                item { ProfileSettingsButton(backdrop, palette) { onNavigate(5) } }
            }
        }
        },
        overlay = { _ -> },
    )
    selectedTool?.let { tool ->
        selectedToolDetail?.let { detail ->
            ToolDataSheet(backdrop, palette, isDark, tool, detail, utilityRepository) { selectedTool = null }
        }
    }
}

@Composable
private fun ProfileHero(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    profile: ProfileData,
    presence: UserPresence,
    fleetSummary: String,
) {
    RefugeStandardGlassSurface(
        backdrop = backdrop,
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
        radius = RefugeRadius.panel,
        edgeAlpha = 0f,
        padding = PaddingValues(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (profile.avatarUrl.isNullOrBlank()) Image(
                painter = painterResource(R.drawable.refuge_avatar_placeholder),
                contentDescription = "用户头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
            ) else AsyncImage(
                model = profile.avatarUrl,
                placeholder = painterResource(R.drawable.refuge_avatar_placeholder),
                error = painterResource(R.drawable.refuge_avatar_placeholder),
                contentDescription = "用户头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(CircleShape),
            )
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xxs)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.handle, style = RefugeTypography.title(palette))
                    Spacer(Modifier.width(RefugeSpacing.xs))
                    Box(Modifier.size(8.dp).background(presenceColor(palette, presence), CircleShape))
                }
                // Keep the identity block aligned to the reference layout:
                // handle, fleet/title, level, then presence as four stable rows.
                // Do not infer a fleet from the local hangar or game-package
                // flag. Only public RSI organization data belongs in this row.
                val fleetLabel = profile.organizationName ?: if (profile.isAuthenticated) "无公开主组织" else "未连接 RSI"
                val titleLabel = profile.organizationRank ?: "无公开头衔"
                Text("$fleetLabel · $titleLabel", style = RefugeTypography.secondary(palette), maxLines = 1, overflow = TextOverflow.Ellipsis)
                com.refuge.next.material.ProfileLevelStars(profile.level, palette)
                Text(
                    if (profile.isAuthenticated) presence.label else "未连接 RSI",
                    style = RefugeTypography.caption(palette).copy(color = presenceColor(palette, presence)),
                )
            }
        }
    }
}

@Composable
private fun ProfileStats(backdrop: LayerBackdrop, palette: RefugePalette, profile: ProfileData) {
    RefugeStandardGlassSurface(
        backdrop = backdrop,
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
        radius = RefugeRadius.panel,
        edgeAlpha = 0f,
        padding = PaddingValues(vertical = 14.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Billing total is shown first; the hangar label retains the
                // original product wording while using the meltable value.
                ProfileStatCell(palette, profile.totalSpent, "消费额")
                Box(Modifier.width(1.dp).height(32.dp).background(palette.divider))
                ProfileStatCell(palette, profile.hangarValue, "机库价值")
                Box(Modifier.width(1.dp).height(32.dp).background(palette.divider))
                ProfileStatCell(palette, profile.credit, "信用点")
            }
        }
    }
}

@Composable
private fun RowScope.ProfileStatCell(palette: RefugePalette, value: String, label: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xxs)) {
        Text(value, style = RefugeTypography.value(palette))
        Text(label, style = RefugeTypography.caption(palette))
    }
}

@Composable
private fun ProfileAccountGroup(backdrop: LayerBackdrop, palette: RefugePalette, profile: ProfileData) {
    Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
        Text("账户", style = RefugeTypography.headline(palette))
        RefugeStandardGlassSurface(
            backdrop = backdrop,
            palette = palette,
            modifier = Modifier.fillMaxWidth(),
            radius = RefugeRadius.panel,
            edgeAlpha = 0f,
            padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Column {
                AccountRow(palette, RefugeIcons.notification, "注册时间", profile.registerDate)
                AccountRow(palette, RefugeIcons.success, "UEC", profile.uec)
                AccountRow(palette, RefugeIcons.design, "REC", profile.rec)
                AccountRow(palette, RefugeIcons.store, "当前机库价值", profile.currentValue)
                AccountRow(palette, RefugeIcons.gift, "邀请码", profile.referralCode)
            }
        }
    }
}

@Composable
private fun ProfileUtilities(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    groups: List<Pair<String, List<ToolItem>>>,
    onToolClick: (ToolItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
        Text("实用工具", style = RefugeTypography.headline(palette))
        if (groups.isEmpty()) {
            ProductionEmptyState(palette, "暂无可用工具")
        } else groups.forEach { (group, tools) ->
            Text(group, style = RefugeTypography.caption(palette))
            tools.chunked(2).forEach { rowTools ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                    rowTools.forEach { tool ->
                        RefugeGlassControl(
                            backdrop = backdrop,
                            palette = palette,
                            onClick = { onToolClick(tool) },
                            modifier = Modifier.weight(1f).heightIn(min = 68.dp),
                            contentDescription = tool.title,
                            padding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                            edgeAlpha = 0f,
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(toolIcon(tool), null, tint = palette.accent, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(RefugeSpacing.sm))
                                Column(Modifier.weight(1f)) {
                                    Text(tool.title, style = RefugeTypography.body(palette).copy(color = palette.text), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    if (rowTools.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileSettingsButton(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    onClick: () -> Unit,
) {
    RefugeGlassControl(
        backdrop = backdrop,
        palette = palette,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentDescription = "设置",
        padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        edgeAlpha = 0f,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(RefugeIcons.settings, null, tint = palette.accent, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(Modifier.weight(1f)) {
                Text("设置", style = RefugeTypography.body(palette).copy(color = palette.text))
                Text("主题、缓存与关于", style = RefugeTypography.caption(palette))
            }
            Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
        }
    }
}

@Composable
private fun AccountRow(palette: RefugePalette, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = palette.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(RefugeSpacing.md))
        Text(title, style = RefugeTypography.body(palette))
        Spacer(Modifier.weight(1f))
        Text(value, style = RefugeTypography.secondary(palette).copy(color = palette.text))
    }
}

@Composable
fun ToolsScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    isDark: Boolean,
    selectedBottomTab: Int,
    onNavigate: (Int) -> Unit,
    utilityRepository: UtilityRepository,
    isOnline: Boolean,
    presence: UserPresence,
    avatarUrl: String?,
    onToggleOnline: () -> Unit,
) {
    var selectedTool by remember { mutableStateOf<ToolItem?>(null) }
    var selectedToolDetail by remember { mutableStateOf<ToolDetail?>(null) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var groups by remember { mutableStateOf(emptyList<Pair<String, List<ToolItem>>>()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(utilityRepository, loadAttempt) {
        loading = true
        loadError = null
        runCatching { utilityRepository.groups() }
            .onSuccess { groups = it }
            .onFailure { loadError = it.message ?: "工具目录读取失败" }
        loading = false
    }
    LaunchedEffect(selectedTool, utilityRepository) {
        selectedToolDetail = null
        selectedToolDetail = selectedTool?.let { utilityRepository.detail(it.id) }
    }
    val visibleGroups = remember(groups, query) {
        groups.mapNotNull { (group, tools) ->
            val visibleTools = tools.filter {
                query.isBlank() || it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)
            }
            visibleTools.takeIf { it.isNotEmpty() }?.let { group to it }
        }
    }
    PageGlassScope(
        backdrop = backdrop,
        content = {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().refugeTopEdgeFade(palette.background),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = RefugeSpacing.rootNavigation),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.lg),
        ) {
            item {
                ProductionHeader(
                    palette,
                    "工具",
                    presence = presence,
                    avatarUrl = avatarUrl,
                    onAvatarClick = onToggleOnline,
                    actions = { HeaderAction(backdrop, palette, RefugeIcons.search, "搜索", { showSearch = !showSearch }) },
                )
            }
            item {
                RefugeAnimatedSearch(showSearch, Modifier.fillMaxWidth()) {
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
            if (loading && groups.isEmpty()) {
                item { ProductionLoadingState(backdrop, palette, "正在读取工具目录") }
            } else if (loadError != null && groups.isEmpty()) {
                item { ProductionErrorState(backdrop, palette, loadError!!, onRetry = { loadAttempt++ }) }
            }
            if (!loading && loadError == null && visibleGroups.isEmpty()) {
                item { ProductionEmptyState(palette, if (query.isBlank()) "暂无可用工具" else "没有匹配的工具") }
            }
            visibleGroups.forEach { (group, visibleTools) ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                        Text(group, style = RefugeTypography.headline(palette))
                        RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                                visibleTools.chunked(2).forEach { rowTools ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                                        rowTools.forEach { tool ->
                                            ToolRow(
                                                palette = palette,
                                                tool = tool,
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                if (tool.id == "ships" || tool.id == "equipment") onNavigate(2)
                                                else selectedTool = tool
                                            }
                                        }
                                        if (rowTools.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        },
        overlay = { _ -> },
    )
    selectedTool?.let { tool ->
        selectedToolDetail?.let { detail ->
            ToolDataSheet(backdrop, palette, isDark, tool, detail, utilityRepository) { selectedTool = null }
        }
    }
}

@Composable
private fun ToolRow(
    palette: RefugePalette,
    tool: ToolItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(modifier.heightIn(min = 84.dp).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 9.dp).semantics { role = Role.Button; contentDescription = tool.title }, verticalAlignment = Alignment.CenterVertically) {
        Icon(toolIcon(tool), null, tint = palette.textSecondary, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(RefugeSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text(tool.title, style = RefugeTypography.body(palette).copy(color = palette.text), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(RefugeIcons.chevron, null, tint = palette.textMuted, modifier = Modifier.size(16.dp))
    }
}

private fun toolIcon(tool: ToolItem) = when (tool.id) {
    "crowdfunding" -> RefugeIcons.analytics
    "player-search" -> RefugeIcons.personSearch
    "social" -> RefugeIcons.people
    "gift-redeem" -> RefugeIcons.gift
    "ships" -> RefugeIcons.ship
    "equipment" -> RefugeIcons.inventory
    "referrals", "referral-reverse" -> RefugeIcons.personAdd
    "web-hangar", "web-buyback", "my-fleet", "referral-program", "spectrum", "service-center", "roadmap", "service-status" -> RefugeIcons.hangarOpenExternal
    else -> RefugeIcons.description
}

@Composable
private fun ToolDataSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    isDark: Boolean,
    tool: ToolItem,
    detail: ToolDetail,
    repository: UtilityRepository,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var playerHandle by remember(tool.id) { mutableStateOf("") }
    var submittedHandle by remember(tool.id) { mutableStateOf<String?>(null) }
    var liveDetail by remember(tool.id) { mutableStateOf(detail) }
    var querying by remember(tool.id) { mutableStateOf(false) }
    var queryError by remember(tool.id) { mutableStateOf<String?>(null) }
    var selectedReward by remember(tool.id) { mutableStateOf<String?>(null) }
    var guardNotice by remember(tool.id) { mutableStateOf<String?>(null) }
    val safeNoOp = remember { SafeNoOpDestructiveActionExecutor() }
    LaunchedEffect(detail) { liveDetail = detail }
    LaunchedEffect(submittedHandle) {
        val handle = submittedHandle ?: return@LaunchedEffect
        querying = true
        queryError = null
        runCatching { repository.query(tool.id, handle) }
            .onSuccess { liveDetail = it }
            .onFailure { queryError = it.message ?: "查询失败" }
        querying = false
    }
    RefugeLiquidSheet(backdrop, palette, tool.title, onDismiss) { modalBackdrop ->
        if (tool.id == "player-search") {
            ReferenceSearchField(
                backdrop = modalBackdrop,
                isDark = isDark,
                value = playerHandle,
                onValueChange = { playerHandle = it },
                searchIcon = RefugeIcons.personSearch,
                modifier = Modifier.fillMaxWidth(),
            )
            ReferenceLiquidButton(
                backdrop = modalBackdrop,
                onClick = { if (!querying) submittedHandle = playerHandle.trim() },
                modifier = Modifier.fillMaxWidth(),
                minHeight = 54.dp,
            ) {
                Icon(RefugeIcons.search, null, tint = palette.text)
                Text(if (querying) "查询中" else "查询", style = RefugeTypography.body(palette).copy(color = palette.text))
            }
        }
        queryError?.let { Text(it, style = RefugeTypography.caption(palette).copy(color = palette.error)) }
        if (tool.id == "gift-redeem") {
            liveDetail.rows.forEach { (_, value) ->
                RefugeGlassControl(
                    backdrop = modalBackdrop,
                    palette = palette,
                    onClick = { selectedReward = value; guardNotice = null },
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = value,
                    padding = PaddingValues(11.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(value, style = RefugeTypography.body(palette).copy(color = palette.text), modifier = Modifier.weight(1f))
                        if (selectedReward == value) Icon(RefugeIcons.check, null, tint = palette.accent)
                    }
                }
            }
            selectedReward?.let {
                ReferenceLiquidButton(
                    backdrop = modalBackdrop,
                    onClick = { guardNotice = safeNoOp.execute(DestructiveAction.GIFT).message },
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = 50.dp,
                ) {
                    Text("验证领取请求", style = RefugeTypography.body(palette).copy(color = palette.text))
                }
            }
            guardNotice?.let { Text(it, style = RefugeTypography.caption(palette)) }
        } else {
            liveDetail.rows.forEach { (label, value) ->
                RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = RefugeTypography.body(palette).copy(color = palette.text), modifier = Modifier.weight(1f))
                        Text(value, style = RefugeTypography.secondary(palette))
                    }
                }
            }
        }
        val actionUrl = if (tool.id == "player-search" && playerHandle.isNotBlank()) {
            "https://robertsspaceindustries.com/citizens/${android.net.Uri.encode(playerHandle.trim())}"
        } else liveDetail.externalUrl
        actionUrl?.let { url ->
            ReferenceLiquidButton(
                backdrop = modalBackdrop,
                onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } },
                modifier = Modifier.fillMaxWidth(),
                minHeight = 54.dp,
            ) {
                Icon(RefugeIcons.hangarOpenExternal, null, tint = palette.text)
                Text("打开外部入口", style = RefugeTypography.body(palette).copy(color = palette.text))
            }
        }
    }
}

@Composable
fun SettingsScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    isDark: Boolean,
    translationEnabled: Boolean,
    onToggleTranslation: () -> Unit,
    selectedBottomTab: Int,
    onNavigate: (Int) -> Unit,
    onToggleTheme: () -> Unit,
    onClearCache: suspend () -> Unit,
    cacheInfo: ProductionCacheManifest,
    isOnline: Boolean,
    presence: UserPresence,
    avatarUrl: String?,
    onToggleOnline: () -> Unit,
) {
    var showAbout by remember { mutableStateOf(false) }
    var cacheCleared by remember { mutableStateOf(false) }
    var clearingCache by remember { mutableStateOf(false) }
    var cacheError by remember { mutableStateOf<String?>(null) }
    val settingsScope = rememberCoroutineScope()
    var showLicense by remember { mutableStateOf(false) }
    PageGlassScope(
        backdrop = backdrop,
        content = {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().refugeTopEdgeFade(palette.background),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = RefugeSpacing.rootNavigation),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.lg),
        ) {
            item {
                ProductionHeader(palette, "设置", presence = presence, avatarUrl = avatarUrl, onAvatarClick = onToggleOnline, actions = {
                    HeaderAction(backdrop, palette, RefugeIcons.back, "返回", { onNavigate(4) })
                })
            }
            item {
                SettingsGroup(palette, "外观") {
                    SettingsToggleRow(backdrop, palette, "深色主题", if (isDark) "已开启" else "已关闭", isDark, onToggleTheme)
                    DividerLine(palette)
                    SettingsToggleRow(
                        backdrop = backdrop,
                        palette = palette,
                        title = "使用汉化后的名字",
                        subtitle = if (translationEnabled) "显示汉化名称" else "显示英文原名",
                        checked = translationEnabled,
                        onClick = onToggleTranslation,
                    )
                }
            }
            item {
                SettingsGroup(palette, "数据") {
                    SettingsActionRow(palette, "清理缓存", when {
                        clearingCache -> "正在清理图片缓存"
                        cacheError != null -> cacheError!!
                        cacheCleared -> "图片缓存已清理"
                        else -> "清理图片缓存，保留机库与账户"
                    }) {
                        if (!clearingCache) settingsScope.launch {
                            clearingCache = true
                            cacheError = null
                            runCatching { onClearCache() }
                                .onSuccess { cacheCleared = true }
                                .onFailure { cacheError = "清理失败，请重试" }
                            clearingCache = false
                        }
                    }
                    DividerLine(palette)
                    SettingsActionRow(palette, "数据版本", cacheInfo.label) { showAbout = true }
                }
            }
            item {
                SettingsGroup(palette, "关于") {
                    SettingsActionRow(palette, "版本", BuildConfig.VERSION_NAME) { showAbout = true }
                    DividerLine(palette)
                    SettingsActionRow(palette, "开源协议", "GNU AGPLv3") { showLicense = true }
                }
            }
        }
        },
        overlay = { _ -> },
    )
    if (showAbout) ProductionNoticeSheet(backdrop, palette, "RefugeNext", "${cacheInfo.source}\n${cacheInfo.label}") { showAbout = false }
    if (showLicense) ProductionNoticeSheet(backdrop, palette, "GNU AGPLv3",
        "GNU Affero General Public License, version 3\nhttps://www.gnu.org/licenses/agpl-3.0.html") { showLicense = false }
}

@Composable
private fun SettingsGroup(palette: RefugePalette, title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
        Text(title, style = RefugeTypography.headline(palette))
        RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) { Column { content() } }
    }
}

@Composable
private fun SettingsToggleRow(backdrop: LayerBackdrop, palette: RefugePalette, title: String, subtitle: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = onClick),
        ) {
            Text(title, style = RefugeTypography.body(palette).copy(color = palette.text))
            Text(subtitle, style = RefugeTypography.caption(palette))
        }
        zone.ien.hig.CupertinoSwitch(checked = checked, onCheckedChange = { onClick() }, modifier = Modifier.semantics { contentDescription = title })
    }
}

@Composable
private fun SettingsActionRow(palette: RefugePalette, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = RefugeTypography.body(palette).copy(color = palette.text)); Text(subtitle, style = RefugeTypography.caption(palette)) }
        Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
    }
}

@Composable
fun CcuScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    isDark: Boolean,
    selectedBottomTab: Int,
    onNavigate: (Int) -> Unit,
    rootTab: Int,
    ccuRepository: CcuRepository,
    ownedShips: List<OwnedShip> = emptyList(),
    isOnline: Boolean,
    presence: UserPresence,
    avatarUrl: String?,
    onToggleOnline: () -> Unit,
    onOverlayVisibilityChanged: (Boolean) -> Unit = {},
) {
    var ships by remember(ccuRepository) { mutableStateOf(ccuRepository.cachedShips()) }
    var seed by remember { mutableStateOf<CcuShip?>(null) }
    var target by remember { mutableStateOf<CcuShip?>(null) }
    var showSeed by remember { mutableStateOf(false) }
    var showTarget by remember { mutableStateOf(false) }
    var showOwned by remember { mutableStateOf(false) }
    var owned by remember(ccuRepository) { mutableStateOf(ccuRepository.cachedOwned()) }
    var loading by remember(ccuRepository) { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    var calculated by remember { mutableStateOf(false) }
    var objective by remember { mutableIntStateOf(0) }
    LaunchedEffect(showSeed, showTarget, showOwned) {
        onOverlayVisibilityChanged(!showSeed && !showTarget && !showOwned)
    }
    LaunchedEffect(ccuRepository, selectedBottomTab, loadAttempt) {
        loading = false
        loadError = null
        runCatching {
            ccuRepository.ships() to ccuRepository.owned()
        }.onSuccess { (loadedShips, loadedOwned) ->
            ships = loadedShips
            owned = loadedOwned
            if (seed?.id !in loadedShips.map { it.id }) seed = null
            if (target?.id !in loadedShips.map { it.id }) target = null
        }.onFailure { loadError = it.message ?: "CCU 目录读取失败" }
        loading = false
    }
    val ownedShipOptions = remember(ownedShips) {
        ownedShips.mapNotNull { ship ->
            val paid = parseUsdCents(ship.paidValue) ?: return@mapNotNull null
            val original = parseUsdCents(ship.currentValue) ?: paid
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
    }
    val allPlannerShips = remember(ships, ownedShipOptions) {
        (ownedShipOptions + ships)
            .distinctBy { it.id.ifBlank(it::normalizedAlias) }
            .sortedBy { it.purchasePrice }
    }
    val targetCatalog = remember(ships, ownedShipOptions) {
        ships.filterNot { candidate -> ownedShipOptions.any { it.sameIdentityAs(candidate) } }
    }
    val availableTargets = remember(seed, targetCatalog) { seed?.let { eligibleTargetShips(it, targetCatalog) } ?: targetCatalog }
    val availableStarts = remember(target, allPlannerShips) {
        target?.let { destination -> allPlannerShips.filter { it.purchasePrice < destination.purchasePrice } } ?: allPlannerShips
    }
    LaunchedEffect(targetCatalog) {
        if (target != null && targetCatalog.none { it.sameIdentityAs(target!!) }) target = null
    }
    LaunchedEffect(seed, target) { calculated = false }
    val plan = remember(seed, target, ships, owned, calculated, objective) {
        val currentSeed = seed
        val currentTarget = target
        if (!calculated || currentSeed == null || currentTarget == null) null
        else planCcuRoute(currentSeed, currentTarget, allPlannerShips, owned, if (objective == 0) com.refuge.next.data.CcuOptimization.TOTAL_COST else com.refuge.next.data.CcuOptimization.NEW_SPEND)
    }
    val additional = plan?.remainingPayment ?: 0
    val shipValue = plan?.shipValue ?: seed?.purchasePrice ?: 0

    PageGlassScope(
        backdrop = backdrop,
        content = {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding().refugeTopEdgeFade(palette.background),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = RefugeSpacing.rootNavigation),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md),
        ) {
            item { ProductionHeader(palette, "升级规划", presence = presence, avatarUrl = avatarUrl, onAvatarClick = onToggleOnline, actions = { HeaderAction(backdrop, palette, RefugeIcons.back, "返回", { onNavigate(rootTab) }) }) }
            if (loading && ships.isEmpty() && owned.isEmpty()) {
                item { ProductionLoadingState(backdrop, palette, "正在读取舰船与 CCU 目录") }
            } else if (loadError != null && ships.isEmpty() && owned.isEmpty()) {
                item { ProductionErrorState(backdrop, palette, loadError!!, onRetry = { loadAttempt++ }) }
            } else item {
            PlannerControls(backdrop, palette, owned.size, objective, { objective = it; calculated = false },
                { showOwned = true }, seed?.let { translatedShipName(it.name) }, target?.let { translatedShipName(it.name) },
                    { showSeed = true }, { showTarget = true })
            }
            item {
                RefugeStandardGlassSurface(
                    backdrop = backdrop,
                    palette = palette,
                    modifier = Modifier.fillMaxWidth(),
                    radius = RefugeRadius.panel,
                    edgeAlpha = 0f,
                    padding = PaddingValues(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                        Text("成本分析", style = RefugeTypography.headline(palette))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                            CostCell(palette, formatUsd(shipValue), "舰船价值", RefugeIcons.flightTakeoff)
                            CostCell(palette, formatUsd(plan?.ownedPurchasePrice ?: 0), "路线 CCU", RefugeIcons.inventory)
                            CostCell(palette, formatUsd(additional), "还需花费", RefugeIcons.wallet)
                        }
                        if (plan != null) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("标准差额 ${formatUsd(plan.standardPayment)}", style = RefugeTypography.caption(palette))
                                Text("最高节省 ${formatUsd(plan.maximumSavings)}", style = RefugeTypography.caption(palette).copy(color = palette.positive))
                            }
                        }
                    }
                }
            }
            item {
                RefugeCompactUtilityPill(
                    backdrop = backdrop,
                    palette = palette,
                    icon = RefugeIcons.upgrade,
                    label = "规划升级路线",
                    onClick = { calculated = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (plan != null) {
                item { PlannedRoutePreview(palette, plan) }
            }
        }
        },
        overlay = { _ -> },
    )
    if (showSeed) ShipSelectorSheet(backdrop, palette, isDark, "选择起始舰船", availableStarts, onDismiss = { showSeed = false }) {
        seed = it
        if (target != null && target!!.purchasePrice <= it.purchasePrice) target = null
        showSeed = false
    }
    if (showTarget) ShipSelectorSheet(backdrop, palette, isDark, "选择目标舰船", availableTargets, onDismiss = { showTarget = false }) {
        target = it
        if (seed != null && seed!!.purchasePrice >= it.purchasePrice) seed = null
        showTarget = false
    }
    if (showOwned) {
        OwnedCcuSheet(backdrop, palette, owned, onRemove = { removed -> owned = owned.filterNot { it.id == removed.id } }, onDismiss = { showOwned = false })
    }
}

/** Planner used by the Hangar segment; owned upgrades open separately. */
@Composable
fun HangarUpgradePanel(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    isDark: Boolean,
    ccuRepository: CcuRepository,
    refreshKey: Int = 0,
    ownedSeeds: List<CcuShip>,
    onOwnedInventory: (() -> Unit)? = null,
    inventoryCount: Int? = null,
) {
    var catalog by remember(ccuRepository) { mutableStateOf(ccuRepository.cachedShips()) }
    var owned by remember(ccuRepository) { mutableStateOf(ccuRepository.cachedOwned()) }
    var start by remember { mutableStateOf<CcuShip?>(null) }
    var target by remember { mutableStateOf<CcuShip?>(null) }
    var showStart by remember { mutableStateOf(false) }
    var showTarget by remember { mutableStateOf(false) }
    var showOwned by remember { mutableStateOf(false) }
    var calculated by remember { mutableStateOf(false) }
    var objective by remember { mutableIntStateOf(0) }
    var loading by remember(ccuRepository) { mutableStateOf(false) }
    LaunchedEffect(ccuRepository, refreshKey) {
        loading = false
        runCatching { ccuRepository.ships() to ccuRepository.owned() }
            .onSuccess { (ships, entries) -> catalog = ships; owned = entries }
        loading = false
    }
    val ships = remember(catalog, ownedSeeds) {
        (ownedSeeds + catalog)
            .distinctBy { it.id.ifBlank(it::normalizedAlias) }
            .sortedBy { it.purchasePrice }
    }
    val targetCatalog = remember(catalog, ownedSeeds) {
        catalog
            .distinctBy { it.id.ifBlank(it::normalizedAlias) }
            .filterNot { candidate -> ownedSeeds.any { ownedShip -> candidate.sameIdentityAs(ownedShip) } }
            .sortedBy { it.purchasePrice }
    }
    val availableTargets = remember(start, targetCatalog) {
        start?.let { current -> targetCatalog.filter { it.purchasePrice > current.purchasePrice } } ?: targetCatalog
    }
    val availableStarts = remember(target, ships) {
        target?.let { destination -> ships.filter { it.purchasePrice < destination.purchasePrice } } ?: ships
    }
    LaunchedEffect(targetCatalog) {
        if (target != null && targetCatalog.none { it.sameIdentityAs(target!!) }) target = null
    }
    LaunchedEffect(start, target) {
        if (start != null && target != null && target!!.purchasePrice <= start!!.purchasePrice) target = null
        calculated = false
    }
    val plan = remember(calculated, start, target, ships, owned, objective) { if (calculated && start != null && target != null) {
        planCcuRoute(start!!, target!!, ships, owned, if (objective == 0) com.refuge.next.data.CcuOptimization.TOTAL_COST else com.refuge.next.data.CcuOptimization.NEW_SPEND)
    } else null }
    Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md)) {
        if (loading && ships.isEmpty()) {
            ProductionLoadingState(backdrop, palette, "正在读取升级目录")
        } else {
            PlannerControls(
                backdrop = backdrop,
                palette = palette,
                count = inventoryCount ?: owned.size,
                objective = objective,
                onObjective = { objective = it; calculated = false },
                onOwned = { onOwnedInventory?.invoke() ?: run { showOwned = true } },
                start = start?.displayName(),
                target = target?.displayName(),
                onStart = { showStart = true },
                onTarget = { showTarget = true },
                heading = "升级规划",
                startLabel = "选择种子舰船",
                targetLabel = "选择目标舰船",
                // Keep both selectors visible, matching the legacy planner
                // layout and allowing the target to be chosen first.
                showTargetAfterStart = false,
            )
            val canCalculate = start != null && target != null
            OfficialLiquidButtonPort(
                onClick = { calculated = true },
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth(),
                enabled = canCalculate,
                tint = if (canCalculate) palette.accent else Color.Unspecified,
                visualHeight = 56.dp,
                contentPadding = 16.dp,
            ) {
                Icon(
                    RefugeIcons.upgrade,
                    contentDescription = null,
                    tint = if (canCalculate) Color.White else palette.textMuted,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    "计算升级路线",
                    style = RefugeTypography.body(palette).copy(
                        color = if (canCalculate) Color.White else palette.textMuted,
                    ),
                )
            }
            if (start != null && target != null) {
                RefugeStandardGlassSurface(
                    backdrop = backdrop,
                    palette = palette,
                    modifier = Modifier.fillMaxWidth(),
                    radius = RefugeRadius.panel,
                    edgeAlpha = 0f,
                    padding = PaddingValues(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                        Text("成本分析", style = RefugeTypography.headline(palette))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                            CostCell(palette, formatUsd(plan?.shipValue ?: start!!.paidPrice ?: start!!.purchasePrice), "飞船价值", RefugeIcons.flightTakeoff)
                            CostCell(palette, formatUsd(plan?.ownedPurchasePrice ?: 0), "已拥有 CCU", RefugeIcons.inventory)
                            CostCell(palette, formatUsd(plan?.remainingPayment ?: 0), "还需花费", RefugeIcons.wallet)
                        }
                        if (plan != null) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("标准差额 ${formatUsd(plan.standardPayment)}", style = RefugeTypography.caption(palette))
                                Text("最高节省 ${formatUsd(plan.maximumSavings)}", style = RefugeTypography.caption(palette).copy(color = palette.positive))
                            }
                        }
                    }
                }
            }
            if (calculated && start != null && target != null && plan == null) {
                Text("没有可连接的升级路径", style = RefugeTypography.secondary(palette).copy(color = palette.textMuted))
            }
            plan?.let { PlannedRoutePreview(palette, it) }
        }
    }
    if (showStart) ShipSelectorSheet(
        backdrop = backdrop,
        palette = palette,
        isDark = isDark,
        title = "选择起始舰船",
        ships = availableStarts,
        emptyMessage = "没有低于目标原价的起始舰船",
        onDismiss = { showStart = false },
    ) { selected ->
        start = selected
        if (target != null && target!!.purchasePrice <= selected.purchasePrice) target = null
        showStart = false
    }
    if (showTarget) ShipSelectorSheet(
        backdrop = backdrop,
        palette = palette,
        isDark = isDark,
        title = "选择目标舰船",
        ships = availableTargets,
        emptyMessage = "没有高于起始舰船原价的目标舰船",
        onDismiss = { showTarget = false },
    ) { selected ->
        target = selected
        if (start != null && start!!.purchasePrice >= selected.purchasePrice) start = null
        showTarget = false
    }
    if (showOwned) {
        OwnedCcuSheet(
            backdrop = backdrop,
            palette = palette,
            owned = owned,
            onRemove = { removed -> owned = owned.filterNot { it.id == removed.id } },
            onDismiss = { showOwned = false },
        )
    }
}

@Composable
private fun PlannerControls(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    count: Int,
    objective: Int,
    onObjective: (Int) -> Unit,
    onOwned: () -> Unit,
    start: String?,
    target: String?,
    onStart: () -> Unit,
    onTarget: () -> Unit,
    heading: String = "升级规划",
    startLabel: String = "起始舰船",
    targetLabel: String = "目标舰船",
    showTargetAfterStart: Boolean = false,
) {
    RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    RefugeIcons.plannerRoute,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(heading, style = RefugeTypography.headline(palette), modifier = Modifier.weight(1f))
                Row(Modifier.heightIn(min = 44.dp).clickable(onClick = onOwned), verticalAlignment = Alignment.CenterVertically) {
                    Icon(RefugeIcons.inventory, null, tint = palette.accent, modifier = Modifier.size(20.dp))
                    Text(" 机库 CCU $count", style = RefugeTypography.body(palette))
                    Icon(RefugeIcons.chevron, null, tint = palette.textMuted, modifier = Modifier.size(16.dp))
                }
            }
            ShipSelectorGlassField(
                backdrop = backdrop,
                palette = palette,
                label = startLabel,
                value = start ?: "选择舰船",
                onClick = onStart,
                accessibilityLabel = "起始舰船",
            )
            if (!showTargetAfterStart || start != null || target != null) {
                ShipSelectorGlassField(
                    backdrop = backdrop,
                    palette = palette,
                    label = targetLabel,
                    value = target ?: "选择舰船",
                    onClick = onTarget,
                    accessibilityLabel = "目标舰船",
                )
            }
            Text("优化目标", style = RefugeTypography.caption(palette))
            RefugeLiquidModeSelector(
                backdrop = backdrop,
                palette = palette,
                labels = listOf("最低完整成本", "最低新增支出"),
                selectedIndex = objective,
                onSelected = onObjective,
                icons = listOf(RefugeIcons.analytics, RefugeIcons.inventory),
            )
        }
    }
}

@Composable
private fun CcuShip.displayName(): String = translatedShipName(name) + if (owned) " [机库中]" else ""

internal fun CcuShip.normalizedAlias(): String = name.lowercase(java.util.Locale.US)
        .replace(Regex("(?i)\\b(origin|rsi|aegis|anvil|drake|crusader|argo|banu|cnou|esperia)\\b"), "")
        .replace("exploration module", "explorer")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

internal fun CcuShip.sameIdentityAs(other: CcuShip): Boolean {
    val leftId = id.trim().lowercase()
    val rightId = other.id.trim().lowercase()
    if (leftId.isNotBlank() && rightId.isNotBlank() &&
        !leftId.startsWith("hangar:") && !rightId.startsWith("hangar:")) return leftId == rightId
    val alias = normalizedAlias()
    return alias.isNotBlank() && alias == other.normalizedAlias()
}

private fun parseUsdCents(value: String): Int? = Regex("[0-9]+(?:\\.[0-9]+)?")
    .find(value.replace(",", ""))
    ?.value
    ?.toBigDecimalOrNull()
    ?.movePointRight(2)
    ?.toInt()
    ?.takeIf { it > 0 }

@Composable
private fun PlannedRoutePreview(palette: RefugePalette, plan: CcuRoutePlan) {
    RefugeContentSurface(
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
        radius = RefugeRadius.panel,
        padding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
            Text("当前最优路线", style = RefugeTypography.headline(palette))
            Text(
                "${translatedShipName(plan.seed.name)} → ${translatedShipName(plan.target.name)} · 标准差额 ${formatUsd(plan.standardPayment)} · 节省 ${formatUsd(plan.maximumSavings)}",
                style = RefugeTypography.caption(palette).copy(color = if (plan.maximumSavings > 0) palette.positive else palette.textMuted),
            )
            plan.steps.forEach { step ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(translatedShipName(step.from.name), style = RefugeTypography.body(palette).copy(color = palette.text), modifier = Modifier.weight(1f))
                    Icon(RefugeIcons.chevron, null, tint = palette.textMuted, modifier = Modifier.size(20.dp))
                    Text(translatedShipName(step.to.name), style = RefugeTypography.body(palette).copy(color = palette.text), modifier = Modifier.weight(1f))
                    Text(
                        if (step.owned != null) "已拥有" else formatUsd(step.purchasePrice),
                        style = RefugeTypography.secondary(palette).copy(color = if (step.owned != null) palette.positive else palette.accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannedCcuRouteSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    plan: CcuRoutePlan,
    onDismiss: () -> Unit,
) {
    RefugeLiquidSheet(backdrop, palette, "升级链", onDismiss) { modalBackdrop ->
        plan.steps.forEach { step ->
            RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xxs)) {
                    Text("${translatedShipName(step.from.name)} → ${translatedShipName(step.to.name)}", style = RefugeTypography.body(palette).copy(color = palette.text))
                    Text(
                        if (step.owned != null) "已拥有 · ${formatUsd(step.purchasePrice)}" else "需要购买 · ${formatUsd(step.purchasePrice)}",
                        style = RefugeTypography.caption(palette).copy(color = if (step.owned != null) palette.positive else palette.accent),
                    )
                }
            }
        }
        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.chevron, "完成", onDismiss, Modifier.align(Alignment.End))
    }
}

@Composable
private fun ShipSelectorGlassField(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    label: String,
    value: String,
    onClick: () -> Unit,
    accessibilityLabel: String = label,
) {
    RefugeGlassControl(
        backdrop = backdrop,
        palette = palette,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        contentDescription = accessibilityLabel,
        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        edgeAlpha = .08f,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(RefugeIcons.rocket, null, tint = palette.textSecondary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (value == "选择舰船") {
                    Text(label, style = RefugeTypography.body(palette).copy(color = palette.textSecondary))
                } else {
                    Text(label, style = RefugeTypography.caption(palette))
                    Text(value, style = RefugeTypography.body(palette).copy(color = palette.text))
                }
            }
            Icon(RefugeIcons.chevron, null, tint = palette.textMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RowScope.CostCell(
    palette: RefugePalette,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) Icon(icon, null, tint = palette.accent, modifier = Modifier.size(18.dp))
        Text(value, style = RefugeTypography.value(palette).copy(color = palette.accent), maxLines = 1, softWrap = false)
        Text(label, style = RefugeTypography.caption(palette), maxLines = 1, softWrap = false)
    }
}

@Composable
private fun ShipSelectorSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    isDark: Boolean,
    title: String,
    ships: List<CcuShip>,
    emptyMessage: String = "没有匹配的舰船",
    onDismiss: () -> Unit,
    onSelected: (CcuShip) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleShips = remember(ships, query) {
        ships.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
            .sortedWith(compareByDescending<CcuShip> { it.owned }.thenBy { it.purchasePrice })
    }
    RefugeLiquidSheet(
        backdrop = backdrop,
        palette = palette,
        title = title,
        onDismiss = onDismiss,
        sheetHeight = 760.dp,
        // The selector owns its LazyColumn so the sheet must not wrap it in a
        // second verticalScroll. This keeps large RSI catalogues virtualized.
        contentScrollable = false,
    ) { modalBackdrop ->
        ReferenceSearchField(
            backdrop = modalBackdrop,
            isDark = isDark,
            value = query,
            onValueChange = { query = it },
            searchIcon = RefugeIcons.search,
            modifier = Modifier.fillMaxWidth(),
        )
        if (ships.isEmpty()) {
            Text(emptyMessage, style = RefugeTypography.body(palette))
        } else if (visibleShips.isEmpty()) {
            Text("没有匹配的舰船", style = RefugeTypography.body(palette))
        } else {
            val hangarShips = visibleShips.filter { it.owned }
            val availableShips = visibleShips.filterNot { it.owned }
            // Give the virtualized list a real finite viewport. A LazyColumn
            // nested in the sheet's non-scrollable content must never be
            // measured with an unbounded height, otherwise opening the
            // selector can stall on large RSI catalogues.
            Box(
                Modifier
                    .fillMaxWidth()
                    // Keep the nested list bounded even while the sheet is
                    // being measured by Dialog. This prevents selector stalls
                    // on large RSI catalogues.
                    .height(520.dp),
            ) {
                val viewportHeight = 520.dp
                RefugeStandardGlassSurface(
                    backdrop = modalBackdrop,
                    palette = palette,
                    modifier = Modifier.fillMaxWidth().height(viewportHeight),
                    radius = RefugeRadius.panel,
                    padding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (hangarShips.isNotEmpty()) {
                            item(key = "section:hangar") {
                                Text("机库已有舰船", style = RefugeTypography.detailSection(palette))
                            }
                            hangarShips.forEachIndexed { index, ship ->
                                item(key = "hangar:$index:${ship.id}:${ship.name}") {
                                    ShipSelectorRow(modalBackdrop, palette, ship, onSelected)
                                }
                            }
                        }
                        if (availableShips.isNotEmpty()) {
                            item(key = "section:available") {
                                Text("可选舰船", style = RefugeTypography.detailSection(palette))
                            }
                            availableShips.forEachIndexed { index, ship ->
                                item(key = "catalog:$index:${ship.id}:${ship.name}") {
                                    ShipSelectorRow(modalBackdrop, palette, ship, onSelected)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShipSelectorRow(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    ship: CcuShip,
    onSelected: (CcuShip) -> Unit,
) {
    RefugeGlassListRow(
        palette = palette,
        onClick = { onSelected(ship) },
        contentDescription = ship.displayName(),
        modifier = Modifier.heightIn(min = 56.dp),
        isLast = true,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ship.displayName(), style = RefugeTypography.body(palette).copy(color = palette.text))
                if (ship.owned) {
                    Text(
                        "继承已付 ${formatUsd(ship.paidPrice ?: ship.purchasePrice)}",
                        style = RefugeTypography.caption(palette).copy(color = palette.positive),
                    )
                }
            }
            Text(formatUsd(ship.purchasePrice), style = RefugeTypography.secondary(palette))
        }
    }
}

@Composable
private fun OwnedCcuSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    owned: List<OwnedCcu>,
    onRemove: (OwnedCcu) -> Unit,
    onDismiss: () -> Unit,
) {
    RefugeLiquidSheet(backdrop, palette, "当前拥有 CCU", onDismiss) { modalBackdrop ->
        if (owned.isEmpty()) {
            Text("当前没有已拥有 CCU", style = RefugeTypography.body(palette))
        } else {
            owned.forEach { entry ->
                RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(translatedShipName(entry.title), style = RefugeTypography.body(palette).copy(color = palette.text))
                            Text("${formatUsd(entry.purchasePrice)} · ${translatedShipName(entry.appliedTo)}", style = RefugeTypography.caption(palette))
                        }
                        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.reclaim, "移除", { onRemove(entry) })
                    }
                }
            }
        }
        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.chevron, "完成", onDismiss, Modifier.align(Alignment.End))
    }
}

@Composable
private fun OwnedCcuInlineList(
    palette: RefugePalette,
    owned: List<OwnedCcu>,
    selectedCount: Int,
    onOpen: () -> Unit,
) {
    RefugeLightweightGlassSurface(
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        contentDescription = "当前拥有 CCU",
        padding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("当前拥有 CCU", style = RefugeTypography.body(palette).copy(color = palette.text))
                    Text("已拥有链 ${owned.size} 条 · 当前路线使用 ${selectedCount} 条", style = RefugeTypography.caption(palette))
                }
                Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
            }
            if (owned.isEmpty()) {
                Text("暂无已拥有 CCU", style = RefugeTypography.caption(palette).copy(color = palette.textMuted))
            } else {
                owned.take(4).forEach { entry ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(RefugeIcons.inventory, null, tint = palette.accent, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(RefugeSpacing.sm))
                        Column(Modifier.weight(1f)) {
                            Text(translatedShipName(entry.title), style = RefugeTypography.secondary(palette), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(translatedShipName(entry.appliedTo), style = RefugeTypography.caption(palette), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(formatUsd(entry.purchasePrice), style = RefugeTypography.caption(palette).copy(color = palette.textSecondary))
                    }
                }
                if (owned.size > 4) Text("还有 ${owned.size - 4} 条", style = RefugeTypography.caption(palette).copy(color = palette.accent))
            }
        }
    }
}

@Composable
private fun CcuChainSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    owned: OwnedCcu,
    chain: List<com.refuge.next.data.CcuChainStep>,
    error: String?,
    onDismiss: () -> Unit,
) {
    RefugeLiquidSheet(backdrop, palette, "升级链详情", onDismiss) { modalBackdrop ->
        Text(translatedShipName(owned.title), style = RefugeTypography.headline(palette))
        if (error != null) {
            ProductionErrorState(modalBackdrop, palette, error, onRetry = onDismiss)
        } else if (chain.isEmpty()) {
            Text("正在读取升级链…", style = RefugeTypography.secondary(palette))
        } else {
            chain.forEach { step ->
                RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(translatedShipName(step.from), style = RefugeTypography.body(palette).copy(color = palette.text))
                        Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
                        Text(translatedShipName(step.to), style = RefugeTypography.body(palette).copy(color = palette.text), modifier = Modifier.weight(1f))
                        Text(formatUsd(step.purchasePrice), style = RefugeTypography.value(palette).copy(color = palette.accent))
                    }
                }
            }
        }
        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.chevron, "完成", onDismiss, Modifier.align(Alignment.End))
    }
}

@Composable
private fun TerminalDetailSheet(backdrop: LayerBackdrop, palette: RefugePalette, item: TerminalItem, onDismiss: () -> Unit) {
    val reference = rememberShipReference(item.name)
    val specificationSections = remember(item, reference) {
        val sections = item.terminalSpecificationSections()
        val existing = sections.flatMap { it.rows }.map { it.first }.toSet()
        val supplemental = reference?.details.orEmpty().filterNot {
            it.first in existing || (it.first == "长 × 宽 × 高" && existing.containsAll(listOf("长度", "宽度", "高度")))
        }
        if (supplemental.isEmpty()) sections else sections + TerminalSpecificationSection("舰船规格", supplemental)
    }
    val description = remember(item.description, reference) { item.readableTerminalDescription().ifBlank { reference?.description.orEmpty() } }
    val prices = remember(item.value, item.usd) {
        buildList {
            item.value.takeIf(::isTerminalValuePresent)?.let { add("游戏内价格" to it) }
            item.usd.takeIf(::isTerminalValuePresent)?.let { add("官网参考价" to it) }
        }
    }
    val performanceMetrics = remember(item, specificationSections) {
        item.terminalPerformanceMetrics().filterNot { (_, value) ->
            specificationSections.any { section -> section.rows.any { (_, existing) -> existing == value } }
        }
    }
    val visibleRows = specificationSections.sumOf { it.rows.size }
    val estimatedHeight = (
        430 +
            visibleRows.coerceAtMost(8) * 42 +
            specificationSections.size * 34 +
            if (description.isNotBlank()) 126 else 0
        ).dp.coerceIn(620.dp, 780.dp)

    RefugeLiquidSheet(
        backdrop = backdrop,
        palette = palette,
        title = "终端详情",
        onDismiss = onDismiss,
        sheetHeight = estimatedHeight,
        respectTopSafeArea = true,
        // The sheet reserves the action rail in its scroll viewport. This
        // keeps the final specification row readable above “完成”.
        actionOverContent = false,
        transparentActionArea = false,
        surfaceRefraction = false,
        action = { actionBackdrop ->
            ReferenceLiquidButton(
                backdrop = actionBackdrop,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                minHeight = 52.dp,
            ) {
                Text("完成", style = RefugeTypography.title(palette))
            }
        },
    ) { _ ->
        Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md)) {
            if (!item.imageUrl.isNullOrBlank() || rememberShipReference(item.name)?.images?.isNotEmpty() == true) {
TerminalRemoteImage(
                    item = item,
                    contentDescription = "${item.name} 图片",
                    fallback = painterResource(R.drawable.ship_placeholder),
                    contentScale = ContentScale.Fit,
                    loadingColor = palette.accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(refugeContinuousShape(RefugeRadius.image))
                        .background(palette.contentSurface),
                )
            } else {
                RefugeImagePlaceholder(
                    palette = palette,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    label = item.category.label,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                Text(
                    translatedGameItemName(item.name, item.className),
                    style = RefugeTypography.detailTitle(palette),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                val metadata = listOf(translatedShipName(item.manufacturer), item.category.label)
                    .filter(::isTerminalValuePresent)
                    .joinToString(" · ")
                if (metadata.isNotBlank()) {
                    Text(metadata, style = RefugeTypography.detailSubtitle(palette))
                }
                if (item.tags.isNotEmpty()) {
                    Text(
                        translatedShipName(item.tags.filter(::isTerminalValuePresent).take(3).map(::translatedWikiValue).joinToString("  ·  ")),
                        style = RefugeTypography.detailCaption(palette).copy(color = palette.accent),
                    )
                }
            }

            if (prices.isNotEmpty()) {
                TerminalDetailCard(palette, "价格") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.md),
                    ) {
                        prices.forEach { (label, value) ->
                            val hasAuec = value.endsWith("aUEC", ignoreCase = true)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    if (hasAuec) value.dropLast(4).trim() else value,
                                    style = RefugeTypography.detailValue(palette).copy(color = palette.accent, fontSize = 24.sp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(if (hasAuec) "$label · aUEC" else label, style = RefugeTypography.detailCaption(palette))
                            }
                        }
                    }
                }
            }

            if (performanceMetrics.isNotEmpty()) {
                TerminalPerformanceCard(palette, performanceMetrics)
            }

            specificationSections.forEach { section ->
                TerminalSpecificationCard(palette, section.title, section.rows)
            }

            if (description.isNotBlank()) {
                TerminalDetailCard(palette, "介绍") {
                    Text(
                        translatedShipName(description),
                        style = RefugeTypography.detailBody(palette),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }
        }
    }
}

internal fun TerminalItem.terminalPerformanceMetrics(): List<Pair<String, String>> {
    val keys = listOf(
        "质量" to listOf("质量", "mass"),
        "货舱" to listOf("货舱", "cargo"),
        "电力" to listOf("电力", "power", "energy"),
        "护盾" to listOf("护盾", "shield"),
        "伤害" to listOf("伤害", "damage", "dps"),
        "速度" to listOf("速度", "speed"),
        "加速度" to listOf("加速度", "acceleration"),
        "量子驱动" to listOf("量子", "quantum"),
    )
    return keys.mapNotNull { (title, aliases) ->
        details.firstOrNull { (label, value) ->
            isTerminalValuePresent(value) && aliases.any { label.contains(it, ignoreCase = true) }
        }?.let { title to it.second }
    }
}

@Composable
private fun TerminalPerformanceCard(
    palette: RefugePalette,
    metrics: List<Pair<String, String>>,
) {
    TerminalDetailCard(palette, "性能") {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            metrics.chunked(2).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.md),
                ) {
                    row.forEach { (label, value) ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(label, style = RefugeTypography.detailCaption(palette).copy(color = palette.textSecondary))
                            Text(value, style = RefugeTypography.detailBody(palette), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TerminalDetailCard(
    palette: RefugePalette,
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = RefugeTypography.caption(palette), modifier = Modifier.padding(start = 16.dp, top = 8.dp))
        Box(Modifier.fillMaxWidth().clip(refugeContinuousShape(16.dp)).background(palette.contentSurfaceStrong)) {
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}

internal data class TerminalSpecificationSection(
    val title: String,
    val rows: List<Pair<String, String>>,
)

internal fun TerminalItem.terminalSpecificationSections(): List<TerminalSpecificationSection> {
    val rows = details
        .map { (label, value) -> label.trim() to translatedWikiValue(value.trim()) }
        .filter { (label, value) ->
            isTerminalValuePresent(label) && isTerminalValuePresent(value) && label != "制造商"
        }
        .distinct()
    if (rows.isEmpty()) return emptyList()

    val physical = mutableListOf<Pair<String, String>>()
    val ammunition = mutableListOf<Pair<String, String>>()
    val compatibility = mutableListOf<Pair<String, String>>()
    val systems = mutableListOf<Pair<String, String>>()
    val performance = mutableListOf<Pair<String, String>>()
    val basic = mutableListOf<Pair<String, String>>()
    val source = mutableListOf<Pair<String, String>>()
    rows.forEach { row ->
        val label = row.first
        when {
            label == "Star Citizen Wiki" -> source += row
            listOf("质量", "长度", "宽度", "高度", "体积", "尺寸数据").any(label::contains) -> physical += row
            listOf("弹匣", "弹容量", "弹药速度", "弹药寿命", "穿透").any(label::contains) -> ammunition += row
            listOf("附件", "挂载", "接口", "插槽").any(label::contains) -> compatibility += row
            listOf("功率用量", "冷却用量", "生成功率", "生成冷却", "EM", "IR", "耐久", "自修复", "修复耗时", "可维修", "可回收").any(label::contains) -> systems += row
            listOf("伤害", "DPS", "射程", "射速", "散布", "热量", "过热", "冷却", "输出", "再生", "速度", "吸收", "燃料", "航行", "加速度", "启动").any(label::contains) -> performance += row
            else -> basic += row
        }
    }

    val performanceTitle = when (category) {
        TerminalCategory.PERSONAL, TerminalCategory.SHIP_COMPONENTS -> "武器性能"
        TerminalCategory.SHIELDS -> "护盾性能"
        TerminalCategory.COOLERS -> "冷却性能"
        TerminalCategory.POWER_PLANTS -> "电站性能"
        TerminalCategory.QUANTUM_DRIVES -> "量子性能"
        else -> "核心性能"
    }
    return buildList {
        if (basic.isNotEmpty()) add(TerminalSpecificationSection("基本信息", basic))
        if (performance.isNotEmpty()) add(TerminalSpecificationSection(performanceTitle, performance))
        if (ammunition.isNotEmpty()) add(TerminalSpecificationSection("弹药", ammunition))
        if (compatibility.isNotEmpty()) add(TerminalSpecificationSection("兼容与挂载", compatibility))
        if (systems.isNotEmpty()) add(TerminalSpecificationSection("系统与耐久", systems))
        if (physical.isNotEmpty()) add(TerminalSpecificationSection("物理属性", physical))
        if (source.isNotEmpty()) add(TerminalSpecificationSection("来源", source))
    }
}

private fun translatedWikiValue(value: String): String = when (value.trim().lowercase()) {
    "spacecraft" -> "飞船"
    "ground vehicle" -> "地面载具"
    "heavy fighter" -> "重型战斗机"
    "fighter" -> "战斗机"
    "combat" -> "战斗"
    "small (s2)" -> "小型（S2）"
    "no" -> "否"
    "yes" -> "是"
    "time-limited sales" -> "限时销售"
    else -> value
}

private fun TerminalItem.readableTerminalDescription(): String {
    val normalized = description.replace("\r\n", "\n").trim()
    if (normalized.isBlank()) return ""
    val blocks = normalized.split(Regex("\\n\\s*\\n+"))
        .map(String::trim)
        .filter(String::isNotBlank)
    val narrativeIndex = blocks.indexOfFirst { block ->
        block.length >= 80 || block.lineSequence().any { it.length >= 60 }
    }
    return if (narrativeIndex >= 0) blocks.drop(narrativeIndex).joinToString("\n\n") else normalized
}

private fun isTerminalValuePresent(value: String): Boolean =
    value.isNotBlank() && value != "—" && !value.equals("null", ignoreCase = true)

@Composable
private fun TerminalSpecificationCard(
    palette: RefugePalette,
    title: String,
    rows: List<Pair<String, String>>,
) {
    val context = LocalContext.current
    TerminalDetailCard(palette, title) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                Row(
                    Modifier.fillMaxWidth()
                        .then(if (label == "Star Citizen Wiki") Modifier.clickable {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value))) }
                        } else Modifier)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        if (label == "Star Citizen Wiki") "资料来源" else label,
                        style = RefugeTypography.detailCaption(palette).copy(color = palette.textSecondary),
                        modifier = Modifier.weight(.42f),
                    )
                    Spacer(Modifier.width(RefugeSpacing.sm))
                    Text(
                        if (label == "Star Citizen Wiki") "Star Citizen Wiki ↗" else translatedGameItemName(value),
                        style = RefugeTypography.detailBody(palette).copy(
                            color = if (label == "Star Citizen Wiki") palette.accent else palette.text,
                        ),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(.58f),
                    )
                }
                if (index != rows.lastIndex) DividerLine(palette)
            }
        }
    }
}

@Composable
private fun ProductionNoticeBlock(palette: RefugePalette, title: String, body: String) {
    RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) { Text(title, style = RefugeTypography.headline(palette)); Text(body, style = RefugeTypography.secondary(palette)) }
    }
}

@Composable
private fun ProductionNoticeSheet(backdrop: LayerBackdrop, palette: RefugePalette, title: String, body: String, onDismiss: () -> Unit) {
    RefugeLiquidSheet(backdrop, palette, title, onDismiss) { modalBackdrop ->
        Text(body, style = RefugeTypography.body(palette))
        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.chevron, "完成", onDismiss, Modifier.align(Alignment.End))
    }
}

@Composable
fun ProductionListSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    title: String,
    entries: List<String>,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    sheetHeight: androidx.compose.ui.unit.Dp? = null,
    onDismiss: () -> Unit,
) {
    RefugeLiquidSheet(backdrop, palette, title, onDismiss, sheetHeight = sheetHeight) { modalBackdrop ->
        if (entries.isEmpty()) {
            Text("暂无记录", style = RefugeTypography.body(palette))
        } else {
            entries.forEach { entry ->
                val lines = entry.lines()
                val isReclaim = entry.contains("回收")
                val isUpgrade = entry.contains("升级") || entry.contains("消耗")
                val isGiveaway = entry.contains("获得") || entry.contains("使用")
                val eventColor = when {
                    isReclaim -> palette.positive
                    isUpgrade && !isGiveaway -> Color(0xFFFF3B30)
                    isGiveaway -> Color(0xFFFFA000)
                    else -> Color(0xFFFF5E3A)
                }
                val eventIcon = when {
                    isReclaim -> RefugeIcons.reclaim
                    isUpgrade && !isGiveaway -> RefugeIcons.hangarUpgrade
                    isGiveaway -> RefugeIcons.giveaway
                    else -> RefugeIcons.ship
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        Modifier.size(42.dp).background(
                            eventColor,
                            CircleShape,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            eventIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(RefugeSpacing.md))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        lines.firstOrNull()?.let { Text(it, style = RefugeTypography.headline(palette)) }
                        lines.drop(1).dropLast(if (lines.lastOrNull()?.startsWith("价值 ") == true) 1 else 0).forEachIndexed { index, line ->
                            Text(line, style = if (index == lines.size - 2) RefugeTypography.caption(palette) else RefugeTypography.body(palette))
                        }
                        lines.lastOrNull()?.takeIf { it.startsWith("价值 ") }?.let {
                            Text(it, style = RefugeTypography.caption(palette).copy(color = palette.positive), modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
            }
        }
        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.chevron, "完成", onDismiss, Modifier.align(Alignment.End))
    }
}

@Composable
private fun TerminalFilterSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    pricedOnly: Boolean,
    taggedOnly: Boolean,
    onPricedChanged: (Boolean) -> Unit,
    onTaggedChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    RefugeLiquidSheet(backdrop, palette, "终端筛选", onDismiss) { modalBackdrop ->
        SettingsToggleRow(modalBackdrop, palette, "仅显示有 USD 价格", if (pricedOnly) "开启" else "关闭", pricedOnly) { onPricedChanged(!pricedOnly) }
        SettingsToggleRow(modalBackdrop, palette, "仅显示有标签", if (taggedOnly) "开启" else "关闭", taggedOnly) { onTaggedChanged(!taggedOnly) }
        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.chevron, "完成", onDismiss, Modifier.align(Alignment.End))
    }
}

@Composable
private fun DividerLine(palette: RefugePalette) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
}

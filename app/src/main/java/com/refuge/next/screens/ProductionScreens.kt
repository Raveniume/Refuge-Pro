package com.refuge.next.screens

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.platform.LocalContext
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
import com.kyant.backdrop.Backdrop
import com.refuge.next.R
import com.refuge.next.data.ProductionTerminalRepository
import com.refuge.next.data.CcuPlan
import com.refuge.next.data.CcuRepository
import com.refuge.next.data.CcuShip
import com.refuge.next.data.OwnedCcu
import com.refuge.next.data.ProfileData
import com.refuge.next.data.ProfileRepository
import com.refuge.next.data.UtilityRepository
import com.refuge.next.data.calculateRemainingPayment
import com.refuge.next.data.calculateShipValue
import com.refuge.next.data.eligibleTargetShips
import com.refuge.next.data.TerminalCategory
import com.refuge.next.data.TerminalItem
import com.refuge.next.data.TerminalRepository
import com.refuge.next.data.ToolItem
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
import com.refuge.next.material.RefugeCompactUtilityPill
import com.refuge.next.material.RefugeContentSurface
import com.refuge.next.material.RefugeGlassControl
import com.refuge.next.material.RefugeGlassSurface
import com.refuge.next.material.PageGlassScope
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeImagePlaceholder
import com.refuge.next.material.RefugeLightweightGlassSurface
import com.refuge.next.material.RefugeQuietLiquidGlassSurface
import com.refuge.next.material.RefugeLiquidToggle
import com.refuge.next.material.RefugeLiquidSheet
import com.refuge.next.material.RefugeBottomTabs
import com.refuge.next.material.RefugeLiquidSegmented
import com.refuge.next.material.RefugeLiquidIconButton
import com.refuge.next.material.RefugeModalSurface
import com.refuge.next.material.RefugeStandardGlassSurface
import com.refuge.next.reference.ReferenceLiquidButton
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

@Composable
fun BoxScope.RootBottomNav(
    backdrop: Backdrop,
    isDark: Boolean,
    selected: Int,
    onNavigate: (Int) -> Unit,
) {
    val selectedIndex = rootTabs.indexOfFirst { it.route == selected }.coerceAtLeast(0)
    RefugeBottomTabs(
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
private fun ProductionHeader(
    palette: RefugePalette,
    title: String,
    isOnline: Boolean = true,
    onAvatarClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp), contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = painterResource(R.drawable.user_profile_pic),
                contentDescription = "用户头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .graphicsLayer { scaleX = 1.9f; scaleY = 1.9f }
                    .semantics {
                        role = Role.Button
                        contentDescription = "切换在线状态"
                    }
                    .clickable(enabled = onAvatarClick != null) { onAvatarClick?.invoke() },
            )
            Box(Modifier.size(9.dp).background(if (isOnline) palette.positive else palette.textMuted, CircleShape).border(.5.dp, palette.background.copy(alpha = .72f), CircleShape))
        }
        Spacer(Modifier.width(RefugeSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = RefugeTypography.largeTitle(palette), maxLines = 1)
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
    onToggleOnline: () -> Unit,
) {
    var categoryIndex by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf(emptyList<TerminalItem>()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var sortDescending by remember { mutableStateOf(false) }
    var pricedOnly by remember { mutableStateOf(false) }
    var taggedOnly by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<TerminalItem?>(null) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository, loadAttempt) {
        loading = true
        loadError = null
        runCatching { repository.items() }
            .onSuccess { items = it }
            .onFailure { loadError = it.message ?: "终端缓存读取失败" }
        loading = false
    }
    val category = TerminalCategory.entries[categoryIndex]
    val visible = remember(items, categoryIndex, query, sortDescending, pricedOnly, taggedOnly) {
        val filtered = items.filter {
            it.category == category &&
                (query.isBlank() || it.name.contains(query, true) || it.manufacturer.contains(query, true)) &&
                (!pricedOnly || it.usd != "—") &&
                (!taggedOnly || it.tags.isNotEmpty())
        }
        if (sortDescending) filtered.sortedByDescending { it.name } else filtered.sortedBy { it.name }
    }

    PageGlassScope(
        backdrop = backdrop,
        content = {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = 142.dp),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md),
        ) {
            item {
                ProductionHeader(
                    palette = palette,
                    title = "终端",
                    isOnline = isOnline,
                    onAvatarClick = onToggleOnline,
                    actions = {
                        HeaderAction(backdrop, palette, RefugeIcons.search, "搜索", { showSearch = !showSearch })
                        Spacer(Modifier.width(RefugeSpacing.xs))
                        HeaderAction(backdrop, palette, RefugeIcons.more, "版本", { showFilter = true })
                    },
                )
            }
            if (showSearch) {
                item {
                    ReferenceSearchField(backdrop, isDark, query, { query = it }, RefugeIcons.search, Modifier.fillMaxWidth())
                }
            }
            item {
                RefugeLiquidSegmented(
                    backdrop = backdrop,
                    isDark = isDark,
                    labels = TerminalCategory.entries.map { it.label },
                    initialIndex = categoryIndex,
                    onSelected = { categoryIndex = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${category.label}资料", style = RefugeTypography.headline(palette))
                    Spacer(Modifier.weight(1f))
                    RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.filter, "筛选", { showFilter = true })
                    Spacer(Modifier.width(RefugeSpacing.xs))
                    RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.sort, if (sortDescending) "排序：Z-A" else "排序：默认", { sortDescending = !sortDescending })
                }
            }
            if (loading) {
                item { ProductionLoadingState(backdrop, palette, "正在读取终端资料") }
            } else if (loadError != null) {
                item { ProductionErrorState(backdrop, palette, loadError!!, onRetry = { loadAttempt++ }) }
            } else if (visible.isEmpty()) {
                item { ProductionEmptyState(palette, "暂无${category.label}资料") }
            } else {
                items(visible, key = { it.id }) { item ->
                    TerminalRow(backdrop, palette, item) { selectedItem = item }
                }
            }
        }
        },
        overlay = { pageBackdrop ->
            RootBottomNav(pageBackdrop, isDark, selectedBottomTab, onNavigate)
        },
    )

    selectedItem?.let { TerminalDetailSheet(backdrop, palette, it) { selectedItem = null } }
    if (showFilter) TerminalFilterSheet(backdrop, palette, pricedOnly, taggedOnly, { pricedOnly = it }, { taggedOnly = it }, { showFilter = false })
}

@Composable
private fun HeaderAction(backdrop: LayerBackdrop, palette: RefugePalette, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    RefugeLiquidIconButton(backdrop, icon, label, onClick, Modifier.size(44.dp), iconTint = palette.textSecondary)
}

@Composable
private fun TerminalRow(backdrop: LayerBackdrop, palette: RefugePalette, item: TerminalItem, onClick: () -> Unit) {
    RefugeQuietLiquidGlassSurface(
        backdrop = backdrop,
        palette = palette,
        modifier = Modifier.fillMaxWidth().height(104.dp),
        radius = RefugeRadius.panel,
        onClick = onClick,
        contentDescription = item.name,
        padding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
            if (item.id == "v-m80") {
                Image(painter = painterResource(R.drawable.m80_hero), contentDescription = "${item.name} 图片", contentScale = ContentScale.Crop, modifier = Modifier.size(88.dp).clip(RoundedCornerShape(RefugeRadius.image)))
            } else {
                RefugeImagePlaceholder(palette, Modifier.size(88.dp), item.category.label)
            }
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(Modifier.fillMaxSize()) {
                Text(item.name, style = RefugeTypography.headline(palette), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.manufacturer, style = RefugeTypography.secondary(palette))
                Row(horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xxs)) {
                    item.tags.take(2).forEach { tag ->
                        Text(tag, style = RefugeTypography.caption(palette).copy(color = palette.accent))
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(item.value, style = RefugeTypography.caption(palette))
                    Spacer(Modifier.weight(1f))
                    Text(item.usd, style = RefugeTypography.value(palette).copy(color = palette.accent))
                }
            }
        }
    }
}

@Composable
private fun TerminalSkeleton(palette: RefugePalette) {
    RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth().height(104.dp), padding = PaddingValues(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(88.dp).background(palette.glassStrong.copy(alpha = .2f), RoundedCornerShape(RefugeRadius.image)))
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                Box(Modifier.width(170.dp).height(14.dp).background(palette.glassStrong.copy(alpha = .24f), RoundedCornerShape(8.dp)))
                Box(Modifier.width(110.dp).height(11.dp).background(palette.glassStrong.copy(alpha = .18f), RoundedCornerShape(8.dp)))
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
    onToggleOnline: () -> Unit,
) {
    var profileData by remember { mutableStateOf(ProfileData()) }
    var profileToolGroups by remember { mutableStateOf(emptyList<Pair<String, List<ToolItem>>>()) }
    var profileLoading by remember { mutableStateOf(true) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(profileRepository, utilityRepository, loadAttempt) {
        profileLoading = true
        profileError = null
        runCatching {
            profileRepository.profile() to utilityRepository.groups()
        }.onSuccess { (profile, groups) ->
            profileData = profile
            profileToolGroups = groups
        }.onFailure { profileError = it.message ?: "账户缓存读取失败" }
        profileLoading = false
    }
    val profile = profileData.copy(isOnline = isOnline)
    var selectedTool by remember { mutableStateOf<ToolItem?>(null) }
    var selectedToolDetail by remember { mutableStateOf<ToolDetail?>(null) }
    LaunchedEffect(selectedTool, utilityRepository) {
        selectedToolDetail = selectedTool?.let { utilityRepository.detail(it.id) }
    }
    PageGlassScope(
        backdrop = backdrop,
        content = {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = 142.dp),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md),
        ) {
            item {
                ProductionHeader(
                    palette = palette,
                    title = "我的",
                    isOnline = isOnline,
                    onAvatarClick = onToggleOnline,
                    actions = {
                        HeaderAction(backdrop, palette, if (isDark) RefugeIcons.light else RefugeIcons.dark, "切换主题", onToggleTheme)
                        Spacer(Modifier.width(RefugeSpacing.xs))
                        HeaderAction(backdrop, palette, RefugeIcons.profile, "连接 RSI 账户", onOpenLogin)
                    },
                )
            }
            if (profileLoading) {
                item { ProductionLoadingState(backdrop, palette, "正在读取账户资料") }
            } else if (profileError != null) {
                item { ProductionErrorState(backdrop, palette, profileError!!, onRetry = { loadAttempt++ }) }
            } else {
                item { ProfileHero(backdrop, palette, profile) }
                item { ProfileStats(backdrop, palette, profile) }
                item { ProfileAccountGroup(backdrop, palette, profile) }
                item { ProfileOrganization(backdrop, palette) }
                item { ProfileUtilities(backdrop, palette, profileToolGroups) { selectedTool = it } }
                item { ProfileSettingsButton(backdrop, palette) { onNavigate(5) } }
            }
        }
        },
        overlay = { pageBackdrop ->
            RootBottomNav(pageBackdrop, isDark, selectedBottomTab, onNavigate)
        },
    )
    selectedTool?.let { tool ->
        selectedToolDetail?.let { detail ->
            ToolDataSheet(backdrop, palette, tool, detail) { selectedTool = null }
        }
    }
}

@Composable
private fun ProfileHero(backdrop: LayerBackdrop, palette: RefugePalette, profile: ProfileData) {
    RefugeGlassSurface(
        backdrop = backdrop,
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
        radius = RefugeRadius.panel,
        padding = PaddingValues(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.user_profile_pic),
                contentDescription = "用户头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(RefugeRadius.image))
                    .graphicsLayer { scaleX = 1.9f; scaleY = 1.9f },
            )
            Spacer(Modifier.width(RefugeSpacing.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xxs)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.handle, style = RefugeTypography.title(palette))
                    Spacer(Modifier.width(RefugeSpacing.xs))
                    Box(Modifier.size(8.dp).background(if (profile.isOnline) palette.positive else palette.textMuted, CircleShape))
                }
                Text("${profile.city} · ${profile.rank}", style = RefugeTypography.secondary(palette))
                Text("Online · 使用本地资料", style = RefugeTypography.caption(palette).copy(color = if (profile.isOnline) palette.positive else palette.textMuted))
            }
            Text("4.8", style = RefugeTypography.value(palette).copy(color = palette.accent))
        }
    }
}

@Composable
private fun ProfileStats(backdrop: LayerBackdrop, palette: RefugePalette, profile: ProfileData) {
    RefugeGlassSurface(
        backdrop = backdrop,
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
        radius = RefugeRadius.panel,
        padding = PaddingValues(vertical = 14.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            ProfileStatCell(palette, profile.totalSpent, "消费额")
            ProfileStatCell(palette, profile.hangarValue, "机库价值")
            ProfileStatCell(palette, profile.credit, "信用点")
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
        RefugeGlassSurface(
            backdrop = backdrop,
            palette = palette,
            modifier = Modifier.fillMaxWidth(),
            radius = RefugeRadius.panel,
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
            tools.forEach { tool ->
                RefugeGlassControl(
                    backdrop = backdrop,
                    palette = palette,
                    onClick = { onToolClick(tool) },
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = tool.title,
                    padding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(toolIcon(tool), null, tint = palette.accent, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(RefugeSpacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(tool.title, style = RefugeTypography.body(palette).copy(color = palette.text))
                            Text(tool.subtitle, style = RefugeTypography.caption(palette))
                        }
                        Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
                    }
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
private fun ProfileOrganization(backdrop: LayerBackdrop, palette: RefugePalette) {
    Row(Modifier.fillMaxWidth().padding(vertical = RefugeSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Icon(RefugeIcons.home, null, tint = palette.textSecondary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(RefugeSpacing.md))
        Column {
            Text("星环城", style = RefugeTypography.title(palette))
            Text("社区等级 4 · Experienced", style = RefugeTypography.secondary(palette))
        }
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
    onToggleOnline: () -> Unit,
) {
    var selectedTool by remember { mutableStateOf<ToolItem?>(null) }
    var selectedToolDetail by remember { mutableStateOf<ToolDetail?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
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
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = 142.dp),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.lg),
        ) {
            item {
                ProductionHeader(
                    palette,
                    "工具",
                    isOnline = isOnline,
                    onAvatarClick = onToggleOnline,
                    actions = { HeaderAction(backdrop, palette, RefugeIcons.search, "搜索", { showSearch = !showSearch }) },
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
            if (loading) {
                item { ProductionLoadingState(backdrop, palette, "正在读取工具目录") }
            } else if (loadError != null) {
                item { ProductionErrorState(backdrop, palette, loadError!!, onRetry = { loadAttempt++ }) }
            }
            if (!loading && loadError == null && visibleGroups.isEmpty()) {
                item { ProductionEmptyState(palette, if (query.isBlank()) "暂无可用工具" else "没有匹配的工具") }
            }
            visibleGroups.forEach { (group, visibleTools) ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                        Text(group, style = RefugeTypography.headline(palette))
                        RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)) {
                            Column {
                                visibleTools.forEachIndexed { index, tool ->
                                    ToolRow(palette, tool) { selectedTool = tool }
                                    if (index != visibleTools.lastIndex) DividerLine(palette)
                                }
                            }
                        }
                    }
                }
            }
        }
        },
        overlay = { pageBackdrop ->
            RootBottomNav(pageBackdrop, isDark, selectedBottomTab, onNavigate)
        },
    )
    selectedTool?.let { tool ->
        selectedToolDetail?.let { detail ->
            ToolDataSheet(backdrop, palette, tool, detail) { selectedTool = null }
        }
    }
}

@Composable
private fun ToolRow(palette: RefugePalette, tool: ToolItem, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp).semantics { role = Role.Button; contentDescription = tool.title }, verticalAlignment = Alignment.CenterVertically) {
        Icon(toolIcon(tool), null, tint = palette.textSecondary, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(RefugeSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(tool.title, style = RefugeTypography.body(palette).copy(color = palette.text))
            Text(tool.subtitle, style = RefugeTypography.caption(palette))
        }
        Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
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
    "test-center" -> RefugeIcons.science
    "web-hangar", "web-buyback", "my-fleet", "referral-program", "spectrum", "service-center", "roadmap", "service-status" -> RefugeIcons.hangarOpenExternal
    else -> RefugeIcons.description
}

@Composable
private fun ToolDataSheet(backdrop: LayerBackdrop, palette: RefugePalette, tool: ToolItem, detail: ToolDetail, onDismiss: () -> Unit) {
    val context = LocalContext.current
    RefugeLiquidSheet(backdrop, palette, tool.title, onDismiss) { modalBackdrop ->
        Text(tool.subtitle, style = RefugeTypography.secondary(palette))
        detail.rows.forEach { (label, value) ->
            RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = RefugeTypography.body(palette).copy(color = palette.text), modifier = Modifier.weight(1f))
                    Text(value, style = RefugeTypography.secondary(palette))
                }
            }
        }
        detail.externalUrl?.let { url ->
            RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.hangarOpenExternal, "打开外部入口", {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            }, Modifier.align(Alignment.End))
        }
    }
}

@Composable
fun SettingsScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    isDark: Boolean,
    selectedBottomTab: Int,
    onNavigate: (Int) -> Unit,
    onToggleTheme: () -> Unit,
    syncLogs: Boolean,
    localOnly: Boolean,
    onToggleSyncLogs: () -> Unit,
    onToggleLocalOnly: () -> Unit,
    onClearCache: () -> Unit,
    cacheInfo: ProductionCacheManifest,
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
) {
    var showAbout by remember { mutableStateOf(false) }
    var cacheCleared by remember { mutableStateOf(false) }
    PageGlassScope(
        backdrop = backdrop,
        content = {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = 142.dp),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.lg),
        ) {
            item {
                ProductionHeader(palette, "设置", isOnline = isOnline, onAvatarClick = onToggleOnline, actions = {
                    HeaderAction(backdrop, palette, RefugeIcons.chevron, "返回", { onNavigate(4) })
                })
            }
            item {
                SettingsGroup(palette, "外观") {
                    SettingsToggleRow(backdrop, palette, "深色主题", if (isDark) "已开启" else "已关闭", isDark, onToggleTheme)
                    DividerLine(palette)
                    SettingsActionRow(palette, "Liquid Glass", "Reference V4 optical pipeline") {}
                }
            }
            item {
                SettingsGroup(palette, "数据") {
                    SettingsToggleRow(backdrop, palette, "实时同步日志", if (syncLogs) "开启" else "关闭", syncLogs, onToggleSyncLogs)
                    DividerLine(palette)
                    SettingsToggleRow(backdrop, palette, "仅使用本地资料", if (localOnly) "开启" else "关闭", localOnly, onToggleLocalOnly)
                    DividerLine(palette)
                    SettingsActionRow(palette, "清理缓存", if (cacheCleared) "已清理本地临时标记" else "不会删除机库或账户数据") {
                        onClearCache()
                        cacheCleared = true
                    }
                    DividerLine(palette)
                    SettingsActionRow(palette, "数据版本", cacheInfo.label) {}
                }
            }
            item {
                SettingsGroup(palette, "关于") {
                    SettingsActionRow(palette, "版本", "0.1.0 · Compose production migration") { showAbout = true }
                    DividerLine(palette)
                    SettingsActionRow(palette, "开源协议", "GNU AGPLv3") {}
                    DividerLine(palette)
                    SettingsActionRow(palette, "测试中心", "验证共享组件和运行状态") { onNavigate(3) }
                }
            }
        }
        },
        overlay = { pageBackdrop ->
            RootBottomNav(pageBackdrop, isDark, selectedBottomTab, onNavigate)
        },
    )
    if (showAbout) ProductionNoticeSheet(backdrop, palette, "RefugeNext", "Liquid Glass production migration is active.\n\n${cacheInfo.source}\n${cacheInfo.label}") { showAbout = false }
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
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = RefugeTypography.body(palette).copy(color = palette.text)); Text(subtitle, style = RefugeTypography.caption(palette)) }
        RefugeLiquidToggle(backdrop, palette, checked, onClick, title)
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
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
) {
    var ships by remember { mutableStateOf(emptyList<CcuShip>()) }
    var seed by remember { mutableStateOf<CcuShip?>(null) }
    var target by remember { mutableStateOf<CcuShip?>(null) }
    var showSeed by remember { mutableStateOf(false) }
    var showTarget by remember { mutableStateOf(false) }
    var showOwned by remember { mutableStateOf(false) }
    var owned by remember { mutableStateOf(emptyList<OwnedCcu>()) }
    var selectedOwned by remember { mutableStateOf<OwnedCcu?>(null) }
    var chain by remember { mutableStateOf(emptyList<com.refuge.next.data.CcuChainStep>()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    var pendingNotice by remember { mutableStateOf<String?>(null) }
    var chainError by remember { mutableStateOf<String?>(null) }
    val safeNoOp = remember { SafeNoOpDestructiveActionExecutor() }
    LaunchedEffect(ccuRepository, loadAttempt) {
        loading = true
        loadError = null
        runCatching {
            ccuRepository.ships() to ccuRepository.owned()
        }.onSuccess { (loadedShips, loadedOwned) ->
            ships = loadedShips
            owned = loadedOwned
            seed = loadedShips.getOrNull(1) ?: loadedShips.firstOrNull()
            target = loadedShips.firstOrNull()
        }.onFailure { loadError = it.message ?: "CCU 目录读取失败" }
        loading = false
    }
    val availableTargets = remember(seed, ships) { seed?.let { eligibleTargetShips(it, ships) } ?: emptyList() }
    val availableTargetIds = remember(availableTargets) { availableTargets.map { it.id } }
    LaunchedEffect(seed, availableTargets) {
        if (target?.id !in availableTargetIds) {
            target = availableTargets.firstOrNull()
        }
    }
    val additional = target?.let { currentSeed -> seed?.let { calculateRemainingPayment(it, currentSeed, owned) } } ?: 0
    val plan = target?.let { currentTarget -> seed?.let { CcuPlan(it, currentTarget, owned, additional) } }
    val shipValue = plan?.shipValue ?: seed?.let { calculateShipValue(it.purchasePrice, owned.map { it.purchasePrice }, additional) } ?: 0

    PageGlassScope(
        backdrop = backdrop,
        content = {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = 142.dp),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md),
        ) {
            item { ProductionHeader(palette, "升级规划", isOnline = isOnline, onAvatarClick = onToggleOnline, actions = { HeaderAction(backdrop, palette, RefugeIcons.chevron, "返回", { onNavigate(rootTab) }) }) }
            if (loading) {
                item { ProductionLoadingState(backdrop, palette, "正在读取舰船与 CCU 目录") }
            } else if (loadError != null) {
                item { ProductionErrorState(backdrop, palette, loadError!!, onRetry = { loadAttempt++ }) }
            } else if (seed == null) {
                item { ProductionEmptyState(palette, "暂无可用的 CCU 目录") }
            } else item {
                Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                    Text("选择舰船", style = RefugeTypography.headline(palette))
                    ShipSelectorGlassField(backdrop, palette, "起始舰船", seed?.name ?: "选择舰船") { showSeed = true }
                    ShipSelectorGlassField(backdrop, palette, "目标舰船", target?.name ?: "无更高原价目标") { showTarget = true }
                }
            }
            item {
                RefugeContentSurface(palette = palette, modifier = Modifier.fillMaxWidth(), radius = RefugeRadius.panel, padding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                        Text("成本分析", style = RefugeTypography.headline(palette))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                            CostCell(palette, formatUsd(shipValue), "飞船价值")
                            CostCell(palette, formatUsd(owned.sumOf { it.purchasePrice }), "已有 CCU")
                            CostCell(palette, formatUsd(additional), "还需花费")
                        }
                    }
                }
            }
            item {
                RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth().clickable { showOwned = true }, padding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("当前拥有 CCU", style = RefugeTypography.body(palette).copy(color = palette.text)); Text("${owned.size} 条已拥有路径", style = RefugeTypography.caption(palette)) }
                        Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                    RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.upgrade, "安全购买意图", {
                        pendingNotice = safeNoOp.execute(DestructiveAction.UPGRADE_PURCHASE).message
                    }, Modifier.weight(1f))
                    if (plan != null) {
                        RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.description, "查看升级链", {
                            selectedOwned = owned.firstOrNull()
                        }, Modifier.weight(1f))
                    }
                }
            }
            item { ProductionNoticeBlock(palette, "规则", "飞船价值 = 种子舰船实际购买价 + 已拥有 CCU purchase price + 还需支付金额") }
        }
        },
        overlay = { pageBackdrop ->
            RootBottomNav(pageBackdrop, isDark, selectedBottomTab, onNavigate)
        },
    )
    if (showSeed) ShipSelectorSheet(backdrop, palette, "选择起始舰船", ships, onDismiss = { showSeed = false }) { seed = it; showSeed = false }
    if (showTarget) ShipSelectorSheet(backdrop, palette, "选择目标舰船", availableTargets, onDismiss = { showTarget = false }) { target = it; showTarget = false }
    if (showOwned) {
        OwnedCcuSheet(backdrop, palette, owned, onRemove = { removed -> owned = owned.filterNot { it.id == removed.id } }, onChain = { chain = emptyList(); chainError = null; selectedOwned = it; showOwned = false }, onDismiss = { showOwned = false })
    }
    selectedOwned?.let { ownedCcu ->
        if (chain.isEmpty() && chainError == null) {
            LaunchedEffect(ownedCcu, ccuRepository) {
                runCatching { ccuRepository.chain(ownedCcu) }
                    .onSuccess { chain = it }
                    .onFailure { chainError = it.message ?: "升级链读取失败" }
            }
        }
        CcuChainSheet(backdrop, palette, ownedCcu, chain, chainError) { selectedOwned = null; chain = emptyList(); chainError = null }
    }
    pendingNotice?.let { notice ->
        ProductionNoticeSheet(backdrop, palette, "安全动作", notice) { pendingNotice = null }
    }
}

@Composable
private fun ShipSelectorGlassField(backdrop: LayerBackdrop, palette: RefugePalette, label: String, value: String, onClick: () -> Unit) {
    RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), radius = RefugeRadius.control, onClick = onClick, contentDescription = label, padding = PaddingValues(horizontal = 14.dp, vertical = 11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(label, style = RefugeTypography.caption(palette)); Text(value, style = RefugeTypography.body(palette).copy(color = palette.text)) }
            Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
        }
    }
}

@Composable
private fun RowScope.CostCell(palette: RefugePalette, value: String, label: String) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = RefugeTypography.value(palette).copy(color = palette.accent), maxLines = 1, softWrap = false)
        Text(label, style = RefugeTypography.caption(palette), maxLines = 1, softWrap = false)
    }
}

@Composable
private fun ShipSelectorSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    title: String,
    ships: List<CcuShip>,
    onDismiss: () -> Unit,
    onSelected: (CcuShip) -> Unit,
) {
    RefugeLiquidSheet(backdrop, palette, title, onDismiss) {
        if (ships.isEmpty()) {
            Text("没有原价更高的目标舰船", style = RefugeTypography.body(palette))
        } else {
            ships.forEach { ship ->
                RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth().clickable { onSelected(ship) }, padding = PaddingValues(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ship.name, style = RefugeTypography.body(palette).copy(color = palette.text), modifier = Modifier.weight(1f))
                        Text(formatUsd(ship.purchasePrice), style = RefugeTypography.secondary(palette))
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnedCcuSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    owned: List<OwnedCcu>,
    onRemove: (OwnedCcu) -> Unit,
    onChain: (OwnedCcu) -> Unit,
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
                            Text(entry.title, style = RefugeTypography.body(palette).copy(color = palette.text))
                            Text("${formatUsd(entry.purchasePrice)} · ${entry.appliedTo}", style = RefugeTypography.caption(palette))
                        }
                        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.description, "链路", { onChain(entry) })
                        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.reclaim, "移除", { onRemove(entry) })
                    }
                }
            }
        }
        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.chevron, "完成", onDismiss, Modifier.align(Alignment.End))
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
        Text(owned.title, style = RefugeTypography.headline(palette))
        if (error != null) {
            ProductionErrorState(modalBackdrop, palette, error, onRetry = onDismiss)
        } else if (chain.isEmpty()) {
            Text("正在读取升级链…", style = RefugeTypography.secondary(palette))
        } else {
            chain.forEach { step ->
                RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(step.from, style = RefugeTypography.body(palette).copy(color = palette.text))
                        Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
                        Text(step.to, style = RefugeTypography.body(palette).copy(color = palette.text), modifier = Modifier.weight(1f))
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
    ProductionNoticeSheet(backdrop, palette, item.name, "${item.manufacturer} · ${item.category.label}\n\n${item.description}\n\n${item.value}    ${item.usd}", onDismiss)
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
fun ProductionListSheet(backdrop: LayerBackdrop, palette: RefugePalette, title: String, entries: List<String>, onDismiss: () -> Unit) {
    RefugeLiquidSheet(backdrop, palette, title, onDismiss) { modalBackdrop ->
        if (entries.isEmpty()) {
            Text("暂无记录", style = RefugeTypography.body(palette))
        } else {
            entries.forEach { entry -> RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(11.dp)) { Text(entry, style = RefugeTypography.body(palette)) } }
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

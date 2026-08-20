package com.refuge.next.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.refuge.next.data.CachedTerminalRepository
import com.refuge.next.data.CcuPlan
import com.refuge.next.data.CcuShip
import com.refuge.next.data.OwnedCcu
import com.refuge.next.data.ProfileData
import com.refuge.next.data.TerminalCategory
import com.refuge.next.data.TerminalItem
import com.refuge.next.data.TerminalRepository
import com.refuge.next.data.ToolItem
import com.refuge.next.data.formatUsd
import com.refuge.next.data.toolGroups
import com.refuge.next.design.RefugeIconSize
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import com.refuge.next.material.RefugeCompactUtilityPill
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeImagePlaceholder
import com.refuge.next.material.RefugeLightweightGlassSurface
import com.refuge.next.material.RefugeStandardGlassSurface
import com.refuge.next.reference.ReferenceLiquidButton
import com.refuge.next.reference.ReferenceLiquidSelectionBar
import com.refuge.next.reference.ReferenceSearchField
import com.refuge.next.reference.ReferenceSegmentedControl
import com.refuge.next.reference.ReferenceSelectionItem

private val rootTabs = listOf(
    RefugeIcons.home to "机库",
    RefugeIcons.store to "商店",
    RefugeIcons.design to "终端",
    RefugeIcons.tools to "工具",
    RefugeIcons.profile to "我的",
)

@Composable
fun BoxScope.RootBottomNav(
    backdrop: LayerBackdrop,
    isDark: Boolean,
    selected: Int,
    onNavigate: (Int) -> Unit,
) {
    ReferenceLiquidSelectionBar(
        backdrop = backdrop,
        isDark = isDark,
        tabsCount = rootTabs.size,
        initialIndex = selected,
        onSelected = onNavigate,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(.94f)
            .navigationBarsPadding()
            .padding(bottom = 10.dp),
    ) { selectedIndex, select ->
        rootTabs.forEachIndexed { index, (icon, label) ->
            ReferenceSelectionItem(
                icon = icon,
                label = label,
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
    subtitle: String,
    onAvatarClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.user_profile_pic),
            contentDescription = "用户头像",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .graphicsLayer { scaleX = 1.9f; scaleY = 1.9f }
                .clickable(enabled = onAvatarClick != null) { onAvatarClick?.invoke() },
        )
        Spacer(Modifier.width(RefugeSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = RefugeTypography.largeTitle(palette), maxLines = 1)
            Text(subtitle, style = RefugeTypography.secondary(palette), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    repository: TerminalRepository = remember { CachedTerminalRepository() },
) {
    var categoryIndex by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf(emptyList<TerminalItem>()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<TerminalItem?>(null) }

    LaunchedEffect(repository) {
        loading = true
        items = repository.items()
        loading = false
    }
    val category = TerminalCategory.entries[categoryIndex]
    val visible = remember(items, categoryIndex, query) {
        items.filter { it.category == category && (query.isBlank() || it.name.contains(query, true) || it.manufacturer.contains(query, true)) }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = 142.dp),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md),
        ) {
            item {
                ProductionHeader(
                    palette = palette,
                    title = "终端",
                    subtitle = "舰船与装备资料 · 本地缓存",
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
                ReferenceSegmentedControl(
                    backdrop = backdrop,
                    isDark = isDark,
                    labels = TerminalCategory.entries.map { it.label },
                    initialIndex = categoryIndex,
                    onSelected = { categoryIndex = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${category.label}资料", style = RefugeTypography.headline(palette))
                    Spacer(Modifier.weight(1f))
                    RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.filter, "筛选", { showFilter = true })
                    Spacer(Modifier.width(RefugeSpacing.xs))
                    RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.sort, "排序：默认", {})
                }
            }
            if (loading) {
                items(4, key = { "terminal-loading-$it" }) { TerminalSkeleton(palette) }
            } else if (visible.isEmpty()) {
                item { ProductionEmptyState(palette, "暂无${category.label}资料") }
            } else {
                items(visible, key = { it.id }) { item ->
                    TerminalRow(palette, item) { selectedItem = item }
                }
            }
        }
        RootBottomNav(backdrop, isDark, selectedBottomTab, onNavigate)
    }

    selectedItem?.let { TerminalDetailSheet(backdrop, palette, it) { selectedItem = null } }
    if (showFilter) {
        ProductionNoticeSheet(backdrop, palette, "终端选项", "版本 · 当前缓存 1.0.7\n数据按类别加载，舰载类别使用独立稳定列表。") { showFilter = false }
    }
}

@Composable
private fun HeaderAction(backdrop: LayerBackdrop, palette: RefugePalette, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    ReferenceLiquidButton(backdrop, onClick, Modifier.size(44.dp)) {
        Icon(icon, label, tint = palette.textSecondary, modifier = Modifier.size(RefugeIconSize.medium))
    }
}

@Composable
private fun TerminalRow(palette: RefugePalette, item: TerminalItem, onClick: () -> Unit) {
    RefugeLightweightGlassSurface(
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
    onToggleTheme: () -> Unit,
) {
    val profile = remember { ProfileData() }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = 142.dp),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md),
        ) {
            item {
                ProductionHeader(
                    palette = palette,
                    title = "我的",
                    subtitle = "${profile.handle} · 本地资料",
                    actions = {
                        HeaderAction(backdrop, palette, if (isDark) RefugeIcons.light else RefugeIcons.dark, "切换主题", onToggleTheme)
                        Spacer(Modifier.width(RefugeSpacing.xs))
                        HeaderAction(backdrop, palette, RefugeIcons.more, "设置", { onNavigate(5) })
                    },
                )
            }
            item { ProfileHero(backdrop, palette, profile) }
            item { ProfileStats(palette, profile) }
            item { ProfileAccountGroup(backdrop, palette, profile) }
            item { ProfileOrganization(backdrop, palette) }
        }
        RootBottomNav(backdrop, isDark, selectedBottomTab, onNavigate)
    }
}

@Composable
private fun ProfileHero(backdrop: LayerBackdrop, palette: RefugePalette, profile: ProfileData) {
    RefugeStandardGlassSurface(palette = palette, backdrop = backdrop, modifier = Modifier.fillMaxWidth(), radius = RefugeRadius.hero, padding = PaddingValues(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.user_profile_pic),
                contentDescription = "用户头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
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
private fun ProfileStats(palette: RefugePalette, profile: ProfileData) {
    RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), radius = RefugeRadius.panel, padding = PaddingValues(vertical = 16.dp)) {
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
        RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
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
    RefugeStandardGlassSurface(backdrop = backdrop, palette = palette, modifier = Modifier.fillMaxWidth(), radius = RefugeRadius.panel, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(palette.accentSoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(RefugeIcons.home, null, tint = palette.accent)
            }
            Spacer(Modifier.width(RefugeSpacing.md))
            Column {
                Text("星环城", style = RefugeTypography.title(palette))
                Text("社区等级 4 · Experienced", style = RefugeTypography.secondary(palette))
            }
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
) {
    var selectedTool by remember { mutableStateOf<ToolItem?>(null) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = 142.dp),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.lg),
        ) {
            item { ProductionHeader(palette, "工具", "查询、资料与测试中心", actions = { HeaderAction(backdrop, palette, RefugeIcons.search, "搜索", {}) }) }
            toolGroups.forEach { (group, tools) ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                        Text(group, style = RefugeTypography.headline(palette))
                        RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                            Column {
                                tools.forEachIndexed { index, tool ->
                                    ToolRow(palette, tool) { selectedTool = tool }
                                    if (index != tools.lastIndex) DividerLine(palette)
                                }
                            }
                        }
                    }
                }
            }
        }
        RootBottomNav(backdrop, isDark, selectedBottomTab, onNavigate)
    }
    selectedTool?.let { tool ->
        ProductionNoticeSheet(backdrop, palette, tool.title, "${tool.subtitle}\n\n此入口已接入新的业务 adapter，执行结果将在数据源可用时更新。") { selectedTool = null }
    }
}

@Composable
private fun ToolRow(palette: RefugePalette, tool: ToolItem, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp).semantics { role = Role.Button; contentDescription = tool.title }, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).background(palette.accentSoft, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
            Icon(if (tool.id == "test-center") RefugeIcons.success else RefugeIcons.tools, null, tint = palette.accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(RefugeSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(tool.title, style = RefugeTypography.body(palette).copy(color = palette.text))
            Text(tool.subtitle, style = RefugeTypography.caption(palette))
        }
        Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
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
) {
    var syncLogs by remember { mutableStateOf(true) }
    var localOnly by remember { mutableStateOf(true) }
    var showAbout by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = 142.dp),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.lg),
        ) {
            item {
                ProductionHeader(palette, "设置", "RefugeNext · Design System", actions = {
                    HeaderAction(backdrop, palette, RefugeIcons.chevron, "返回", { onNavigate(4) })
                })
            }
            item {
                SettingsGroup(palette, "外观") {
                    SettingsToggleRow(palette, "深色主题", if (isDark) "已开启" else "已关闭", isDark, onToggleTheme)
                    DividerLine(palette)
                    SettingsActionRow(palette, "Liquid Glass", "Reference V4 optical pipeline") {}
                }
            }
            item {
                SettingsGroup(palette, "数据") {
                    SettingsToggleRow(palette, "实时同步日志", if (syncLogs) "开启" else "关闭", syncLogs) { syncLogs = !syncLogs }
                    DividerLine(palette)
                    SettingsToggleRow(palette, "仅使用本地资料", if (localOnly) "开启" else "关闭", localOnly) { localOnly = !localOnly }
                    DividerLine(palette)
                    SettingsActionRow(palette, "清理缓存", "不会删除机库或账户数据") {}
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
        RootBottomNav(backdrop, isDark, selectedBottomTab, onNavigate)
    }
    if (showAbout) ProductionNoticeSheet(backdrop, palette, "RefugeNext", "Liquid Glass production migration is active.\n\nReference wallpaper is temporary and will be replaced by the final Refuge production background.") { showAbout = false }
}

@Composable
private fun SettingsGroup(palette: RefugePalette, title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
        Text(title, style = RefugeTypography.headline(palette))
        RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) { Column { content() } }
    }
}

@Composable
private fun SettingsToggleRow(palette: RefugePalette, title: String, subtitle: String, checked: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = RefugeTypography.body(palette).copy(color = palette.text)); Text(subtitle, style = RefugeTypography.caption(palette)) }
        Box(Modifier.size(46.dp, 28.dp).background(if (checked) palette.accent else palette.outline, RoundedCornerShape(16.dp)).padding(3.dp)) {
            Box(Modifier.size(22.dp).align(if (checked) Alignment.CenterEnd else Alignment.CenterStart).background(if (checked) palette.background else palette.textMuted, CircleShape))
        }
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
) {
    val ships = remember {
        listOf(
            CcuShip("m80", "M80", 30000, R.drawable.m80_hero),
            CcuShip("aurora", "极光 Mk I ES", 2000, R.drawable.ship_placeholder),
            CcuShip("atls", "ATLS", 4000, R.drawable.ship_placeholder),
        )
    }
    var seed by remember { mutableStateOf(ships[1]) }
    var target by remember { mutableStateOf(ships[0]) }
    var showSeed by remember { mutableStateOf(false) }
    var showTarget by remember { mutableStateOf(false) }
    var showOwned by remember { mutableStateOf(false) }
    val owned = remember { listOf(OwnedCcu("ccu-1", "Aurora → M80 CCU", 500, "M80")) }
    val additional = (target.purchasePrice - seed.purchasePrice).coerceAtLeast(0)
    val plan = CcuPlan(seed, target, owned, additional)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = 142.dp),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md),
        ) {
            item { ProductionHeader(palette, "升级规划", "CCU route · 本地计算", actions = { HeaderAction(backdrop, palette, RefugeIcons.chevron, "返回", { onNavigate(0) }) }) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                    Text("选择舰船", style = RefugeTypography.headline(palette))
                    ShipSelectorGlassField(backdrop, palette, "起始舰船", seed.name) { showSeed = true }
                    ShipSelectorGlassField(backdrop, palette, "目标舰船", target.name) { showTarget = true }
                }
            }
            item {
                RefugeStandardGlassSurface(backdrop, palette, Modifier.fillMaxWidth(), radius = RefugeRadius.panel, padding = PaddingValues(14.dp)) {
                    Text("成本分析", style = RefugeTypography.headline(palette))
                    Spacer(Modifier.height(RefugeSpacing.sm))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
                        CostCell(palette, formatUsd(plan.shipValue), "飞船价值")
                        CostCell(palette, formatUsd(owned.sumOf { it.purchasePrice }), "已有 CCU")
                        CostCell(palette, formatUsd(additional), "还需花费")
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
            item { ProductionNoticeBlock(palette, "规则", "飞船价值 = 种子舰船实际购买价 + 已拥有 CCU purchase price + 还需支付金额") }
        }
        RootBottomNav(backdrop, isDark, selectedBottomTab, onNavigate)
    }
    if (showSeed) ShipSelectorSheet(backdrop, palette, "选择起始舰船", ships, onDismiss = { showSeed = false }) { seed = it; showSeed = false }
    if (showTarget) ShipSelectorSheet(backdrop, palette, "选择目标舰船", ships, onDismiss = { showTarget = false }) { target = it; showTarget = false }
    if (showOwned) {
        ProductionListSheet(backdrop, palette, "当前拥有 CCU", owned.map { "${it.title} · ${formatUsd(it.purchasePrice)} · ${it.appliedTo}" }) { showOwned = false }
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
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(palette.scrim), contentAlignment = Alignment.BottomCenter) {
            RefugeStandardGlassSurface(backdrop, palette, Modifier.fillMaxWidth().padding(14.dp), radius = RefugeRadius.floating, padding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                    Text(title, style = RefugeTypography.title(palette))
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
private fun ProductionEmptyState(palette: RefugePalette, text: String) {
    RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth().height(120.dp), padding = PaddingValues(20.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(RefugeSpacing.xs)) {
            Icon(RefugeIcons.search, null, tint = palette.textMuted)
            Text(text, style = RefugeTypography.body(palette))
        }
    }
}

@Composable
private fun ProductionNoticeSheet(backdrop: LayerBackdrop, palette: RefugePalette, title: String, body: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(palette.scrim), contentAlignment = Alignment.BottomCenter) {
            RefugeStandardGlassSurface(backdrop, palette, Modifier.fillMaxWidth().padding(14.dp), radius = RefugeRadius.floating, padding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md)) {
                    Box(Modifier.width(34.dp).height(4.dp).background(palette.outline, RoundedCornerShape(2.dp)))
                    Text(title, style = RefugeTypography.title(palette))
                    Text(body, style = RefugeTypography.body(palette))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.chevron, "完成", onDismiss)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductionListSheet(backdrop: LayerBackdrop, palette: RefugePalette, title: String, entries: List<String>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(palette.scrim), contentAlignment = Alignment.BottomCenter) {
            RefugeStandardGlassSurface(backdrop, palette, Modifier.fillMaxWidth().padding(14.dp), radius = RefugeRadius.floating, padding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                    Text(title, style = RefugeTypography.title(palette))
                    entries.forEach { entry -> RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(11.dp)) { Text(entry, style = RefugeTypography.body(palette)) } }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.chevron, "完成", onDismiss) }
                }
            }
        }
    }
}

@Composable
private fun DividerLine(palette: RefugePalette) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
}

@Composable
private fun rememberStaticBackdrop(): LayerBackdrop {
    // Major Profile surfaces use their own optical backdrop supplied by the scene in production.
    // A remembered empty layer keeps this helper composable-safe for nested reusable cards.
    return com.kyant.backdrop.backdrops.rememberLayerBackdrop()
}

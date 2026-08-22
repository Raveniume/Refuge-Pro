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
import com.refuge.next.data.calculateRemainingPayment
import com.refuge.next.data.calculateShipValue
import com.refuge.next.data.eligibleTargetShips
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
import com.refuge.next.material.RefugeContentSurface
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeImagePlaceholder
import com.refuge.next.material.RefugeLightweightGlassSurface
import com.refuge.next.material.RefugeLiquidToggle
import com.refuge.next.material.RefugeLiquidSheet
import com.refuge.next.material.RefugeModalSurface
import com.refuge.next.material.RefugeStandardGlassSurface
import com.refuge.next.reference.ReferenceLiquidButton
import com.refuge.next.reference.ReferenceLiquidBottomTabs
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
    ReferenceLiquidBottomTabs(
        backdrop = backdrop,
        isDark = isDark,
        tabsCount = rootTabs.size,
        selectedIndex = selected,
        onSelected = onNavigate,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(.90f)
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
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

    LaunchedEffect(repository) {
        loading = true
        items = repository.items()
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
                    RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.sort, if (sortDescending) "排序：Z-A" else "排序：默认", { sortDescending = !sortDescending })
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
    if (showFilter) TerminalFilterSheet(backdrop, palette, pricedOnly, taggedOnly, { pricedOnly = it }, { taggedOnly = it }, { showFilter = false })
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
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
) {
    val profile = remember(isOnline) { ProfileData(isOnline = isOnline) }
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
                    isOnline = isOnline,
                    onAvatarClick = onToggleOnline,
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
    Row(Modifier.fillMaxWidth().padding(vertical = RefugeSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
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

@Composable
private fun ProfileStats(palette: RefugePalette, profile: ProfileData) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
        Row(Modifier.fillMaxWidth().padding(vertical = RefugeSpacing.md)) {
            ProfileStatCell(palette, profile.totalSpent, "消费额")
            ProfileStatCell(palette, profile.hangarValue, "机库价值")
            ProfileStatCell(palette, profile.credit, "信用点")
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.divider))
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
            RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), radius = RefugeRadius.panel, padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
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
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
) {
    var selectedTool by remember { mutableStateOf<ToolItem?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = 142.dp),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.lg),
        ) {
            item {
                ProductionHeader(
                    palette,
                    "工具",
                    "查询、资料与测试中心",
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
            toolGroups.forEach { (group, tools) ->
                val visibleTools = tools.filter {
                    query.isBlank() || it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)
                }
                if (visibleTools.isNotEmpty()) {
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
        }
        RootBottomNav(backdrop, isDark, selectedBottomTab, onNavigate)
    }
    selectedTool?.let { tool ->
        if (tool.id == "social") {
            SocialToolSheet(backdrop, palette) { selectedTool = null }
        } else if (tool.id == "gift-redeem" || tool.id == "referral-reverse") {
            ToolDataSheet(backdrop, palette, tool) { selectedTool = null }
        } else {
            ProductionNoticeSheet(backdrop, palette, tool.title, "${tool.subtitle}\n\n此入口已接入新的业务 adapter，执行结果将在数据源可用时更新。") { selectedTool = null }
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
    else -> RefugeIcons.description
}

@Composable
private fun SocialToolSheet(backdrop: LayerBackdrop, palette: RefugePalette, onDismiss: () -> Unit) {
    RefugeLiquidSheet(backdrop, palette, "社交", onDismiss) { modalBackdrop ->
        Text("组织与邀请", style = RefugeTypography.secondary(palette))
        listOf(
            "组织" to "星环城 · 社区等级 4",
            "待处理邀请" to "2 条",
            "最近联系" to "NocturnePilot · 在线",
        ).forEach { (label, value) ->
            RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = RefugeTypography.body(palette).copy(color = palette.text), modifier = Modifier.weight(1f))
                    Text(value, style = RefugeTypography.secondary(palette))
                }
            }
        }
    }
}

@Composable
private fun ToolDataSheet(backdrop: LayerBackdrop, palette: RefugePalette, tool: ToolItem, onDismiss: () -> Unit) {
    val rows = if (tool.id == "gift-redeem") {
        listOf("待兑换礼包" to "2 条", "最近礼物码" to "RAVEN-7K2Q", "状态" to "本地待处理")
    } else {
        listOf("邀请人" to "Raveniume", "关系记录" to "3 条", "最近同步" to "2026-08-20")
    }
    RefugeLiquidSheet(backdrop, palette, tool.title, onDismiss) { modalBackdrop ->
        Text(tool.subtitle, style = RefugeTypography.secondary(palette))
        rows.forEach { (label, value) ->
            RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = RefugeTypography.body(palette).copy(color = palette.text), modifier = Modifier.weight(1f))
                    Text(value, style = RefugeTypography.secondary(palette))
                }
            }
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
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
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
                ProductionHeader(palette, "设置", "RefugeNext · Design System", isOnline = isOnline, onAvatarClick = onToggleOnline, actions = {
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
                    SettingsToggleRow(backdrop, palette, "实时同步日志", if (syncLogs) "开启" else "关闭", syncLogs) { syncLogs = !syncLogs }
                    DividerLine(palette)
                    SettingsToggleRow(backdrop, palette, "仅使用本地资料", if (localOnly) "开启" else "关闭", localOnly) { localOnly = !localOnly }
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
private fun SettingsToggleRow(backdrop: LayerBackdrop, palette: RefugePalette, title: String, subtitle: String, checked: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
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
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
) {
    val ships = remember {
        listOf(
            CcuShip("m80", "M80", 30000, R.drawable.m80_hero),
            CcuShip("aurora", "极光 Mk I ES", 2000, R.drawable.ship_placeholder),
            CcuShip("atls", "ATLS", 4000, R.drawable.ship_placeholder),
        )
    }
    var seed by remember { mutableStateOf(ships[1]) }
    var target by remember { mutableStateOf<CcuShip?>(ships[0]) }
    var showSeed by remember { mutableStateOf(false) }
    var showTarget by remember { mutableStateOf(false) }
    var showOwned by remember { mutableStateOf(false) }
    val owned = remember { listOf(OwnedCcu("ccu-1", "Aurora → M80 CCU", 500, "M80")) }
    val availableTargets = remember(seed, ships) { eligibleTargetShips(seed, ships) }
    val availableTargetIds = remember(availableTargets) { availableTargets.map { it.id } }
    LaunchedEffect(seed, availableTargets) {
        if (target?.id !in availableTargetIds) {
            target = availableTargets.firstOrNull()
        }
    }
    val additional = target?.let { calculateRemainingPayment(seed, it, owned) } ?: 0
    val plan = target?.let { CcuPlan(seed, it, owned, additional) }
    val shipValue = plan?.shipValue ?: calculateShipValue(seed.purchasePrice, owned.map { it.purchasePrice }, additional)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = RefugeSpacing.page, top = RefugeSpacing.lg, end = RefugeSpacing.page, bottom = 142.dp),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md),
        ) {
            item { ProductionHeader(palette, "升级规划", "CCU route · 本地计算", isOnline = isOnline, onAvatarClick = onToggleOnline, actions = { HeaderAction(backdrop, palette, RefugeIcons.chevron, "返回", { onNavigate(rootTab) }) }) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                    Text("选择舰船", style = RefugeTypography.headline(palette))
                    ShipSelectorGlassField(backdrop, palette, "起始舰船", seed.name) { showSeed = true }
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
            item { ProductionNoticeBlock(palette, "规则", "飞船价值 = 种子舰船实际购买价 + 已拥有 CCU purchase price + 还需支付金额") }
        }
        RootBottomNav(backdrop, isDark, selectedBottomTab, onNavigate)
    }
    if (showSeed) ShipSelectorSheet(backdrop, palette, "选择起始舰船", ships, onDismiss = { showSeed = false }) { seed = it; showSeed = false }
    if (showTarget) ShipSelectorSheet(backdrop, palette, "选择目标舰船", availableTargets, onDismiss = { showTarget = false }) { target = it; showTarget = false }
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
    RefugeLiquidSheet(backdrop, palette, title, onDismiss) { modalBackdrop ->
        Text(body, style = RefugeTypography.body(palette))
        RefugeCompactUtilityPill(modalBackdrop, palette, RefugeIcons.chevron, "完成", onDismiss, Modifier.align(Alignment.End))
    }
}

@Composable
fun ProductionListSheet(backdrop: LayerBackdrop, palette: RefugePalette, title: String, entries: List<String>, onDismiss: () -> Unit) {
    RefugeLiquidSheet(backdrop, palette, title, onDismiss) { modalBackdrop ->
        entries.forEach { entry -> RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(11.dp)) { Text(entry, style = RefugeTypography.body(palette)) } }
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

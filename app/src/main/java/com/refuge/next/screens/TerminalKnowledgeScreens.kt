@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)

package com.refuge.next.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import zone.ien.hig.MenuPickerAction
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.data.*
import com.refuge.next.design.*
import com.refuge.next.material.*
import com.refuge.next.reference.ReferenceSearchField
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@Composable
internal fun TerminalKnowledgeNavigator(backdrop: LayerBackdrop, palette: RefugePalette, isDark: Boolean,
    initial: String, initialQuery: String, onClose: () -> Unit) {
    var stack by rememberSaveable { mutableStateOf(arrayListOf("$initial|${android.net.Uri.encode(initialQuery)}")) }
    val holder = rememberSaveableStateHolder()
    val route = stack.last()
    val value = android.net.Uri.decode(route.substringAfter('|'))
    fun back() { if (stack.size == 1) onClose() else stack = ArrayList(stack.dropLast(1)) }
    fun push(type: String, value: String) { stack = ArrayList(stack + "$type|${android.net.Uri.encode(value)}") }
    holder.SaveableStateProvider("${stack.size}:$route") {
        if (route.startsWith("blueprints|")) BlueprintDatabaseScreen(backdrop, palette, isDark, value,
            onMission = { push("mission", it) }, onBack = ::back)
        else MissionDatabaseScreen(backdrop, palette, isDark,
            initialQuery = if (route.startsWith("missions|")) value else "",
            initialMission = if (route.startsWith("mission|")) value else null,
            onBlueprint = { push("blueprints", it) }, onBack = ::back)
    }
}

@Composable
internal fun KnowledgePage(backdrop: LayerBackdrop, palette: RefugePalette, title: String, onBack: () -> Unit,
    action: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(palette.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RefugeInlineBackButton(palette, onBack)
            Text(title, style = RefugeTypography.title(palette), modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            action()
        }
        content()
    }
}

@Composable
private fun KnowledgeCard(palette: RefugePalette, title: String, subtitle: String = "", onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}) {
    RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), onClick = onClick,
        contentDescription = if (onClick != null) title else null, padding = PaddingValues(16.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MissionRichText(title, palette, RefugeTypography.headline(palette), Modifier.weight(1f))
                if (onClick != null) {
                    Spacer(Modifier.width(12.dp))
                    Icon(RefugeIcons.chevron, null, tint = palette.textMuted, modifier = Modifier.size(16.dp))
                }
            }
            if (subtitle.isNotBlank()) Text(subtitle, style = RefugeTypography.caption(palette))
            content()
        }
    }
}

@Composable
private fun KnowledgeFilter(backdrop: LayerBackdrop, palette: RefugePalette, values: List<Pair<String, String>>,
    selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val label = values.firstOrNull { it.first == selected }?.second ?: values.firstOrNull()?.second.orEmpty()
    Box(modifier) {
        RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.filter, label,
            { expanded = true }, Modifier.fillMaxWidth(), visualHeight = 40.dp)
        zone.ien.hig.CupertinoDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            backdrop = backdrop, containerColor = palette.contentSurfaceStrong, width = 240.dp) {
            values.forEach { (id, title) ->
                MenuPickerAction(isSelected = id == selected, onClick = { onSelect(id); expanded = false }) {
                    Text(title, color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun MissionDatabaseScreen(backdrop: LayerBackdrop, palette: RefugePalette, isDark: Boolean,
    initialQuery: String = "", initialMission: String? = null, onBlueprint: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { TerminalKnowledgeRepository(context) }
    var catalog by remember { mutableStateOf<MissionCatalog?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var selection by rememberSaveable(initialMission) { mutableStateOf(initialMission) }
    var system by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("") }
    var chinese by rememberSaveable { mutableStateOf(true) }
    var rewardSort by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val detailState = key(selection) { rememberLazyListState() }
    LaunchedEffect(attempt) {
        loading = true; error = null
        try { catalog = repository.missions(attempt > 0) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: "任务载入失败" }
        finally { loading = false }
    }
    val current = catalog
    // Lazy layout may measure its outgoing content before recomposition removes
    // it. Capture the destination so that content does not suddenly become an
    // empty list and clamp the catalog's saved scroll position to zero.
    val destination = selection
    val selected = destination?.let { current?.find(it) }
    val visible = remember(current, query, system, type, chinese, rewardSort) {
        current?.missions.orEmpty().filter { row ->
            (system.isBlank() || system in row.strings("availableSystems")) &&
                (type.isBlank() || row.optString("missionType") == type) &&
                (query.isBlank() || listOf(current?.text(row, "title", chinese).orEmpty(), row.optString("title"),
                    row.optString("contractorName"), current?.text(row, "description", chinese).orEmpty()).any { it.contains(query, true) })
        }.let { if (rewardSort) it.sortedByDescending { row -> row.optDouble("rewardUEC", 0.0) } else it }
    }
    KnowledgePage(backdrop, palette, if (selected == null) "任务数据库" else "任务详情",
        onBack = { if (selection != null && initialMission == null) selection = null else onBack() }, action = {
            RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.language, if (chinese) "EN" else "中文",
                { chinese = !chinese }, visualHeight = 40.dp)
            Spacer(Modifier.width(8.dp))
            RefugeCircularHeaderButton(backdrop, palette, RefugeIcons.refresh, "更新任务", { attempt++ })
        }) {
        // Do not measure a restored scroll state against a temporary loading
        // list. Its small item count would clamp the saved reading position.
        if (current == null) {
            if (loading) ProductionLoadingState(backdrop, palette, "正在读取任务")
            error?.let { ProductionErrorState(backdrop, palette, it, { attempt++ }) }
            return@KnowledgePage
        }
        key(destination) {
        LazyColumn(Modifier.weight(1f).testTag("mission-database"),
            state = if (destination == null) listState else detailState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("SCMDB · ${current?.version.orEmpty()}" + if (current != null && current.language == null) " · 中文包不可用，显示原文" else "",
                style = RefugeTypography.caption(palette)) }
            if (loading) item { ProductionLoadingState(backdrop, palette, "正在读取任务") }
            error?.let { item { ProductionErrorState(backdrop, palette, it, { attempt++ }) } }
            if (destination != null && current != null && selected == null) item {
                ProductionEmptyState(palette, "当前版本未找到这条任务，可返回任务列表搜索")
            }
            if (selected != null && current != null) {
                item { KnowledgeCard(palette, current.text(selected, "title", chinese), selected.optString("title")) {
                    MissionRichText(current.text(selected, "description", chinese), palette, RefugeTypography.body(palette))
                    if (current.text(selected, "description", chinese).contains('[')) {
                        Text("标记中的地点与目标，由游戏内接取任务时确定。", style = RefugeTypography.caption(palette))
                    }
                } }
                item { KnowledgeCard(palette, "任务信息") {
                    val information = listOf("报酬" to selected.number("rewardUEC")?.let { "${String.format(Locale.US, "%,.0f", it)} aUEC" }.orEmpty().ifBlank { "未提供" },
                        "委托方" to current.text(selected, "contractorName", chinese),
                        "类型" to current.text(selected, "missionType", chinese),
                        "星系" to selected.strings("availableSystems").joinToString(" · "),
                        "合法性" to if (selected.optBoolean("illegal")) "非法任务" else "合法任务",
                        "共享" to if (selected.optBoolean("canBeShared")) "可以共享" else "不可共享",
                        "限时" to selected.number("timeToComplete")?.let { "$it 分钟" }.orEmpty(),
                        "最低声望" to current.translated(selected.obj("minStanding").optString("nameKey"), selected.obj("minStanding").optString("name"), chinese),
                        "接取条件" to selected.obj("prerequisites").objects("location").joinToString { it.optString("name") }
                    )
                    RefugeInfoGroup(palette, "委托概况") { RefugeInfoRows(palette, information.take(4)) }
                    RefugeInfoGroup(palette, "接取与执行") { RefugeInfoRows(palette, information.drop(4)) }
                } }
                listOf("locations" to "任务地点", "destinations" to "目的地").forEach { (field, title) ->
                    val places = current.locations(selected, field)
                    if (places.isNotEmpty()) item { KnowledgeCard(palette, title) { Text(places.joinToString("\n"), style = RefugeTypography.body(palette)) } }
                }
                selected.objects("blueprintRewards").forEach { reward ->
                    val pool = current.source.obj("blueprintPools").obj(reward.optString("blueprintPool"))
                    item { Text("蓝图奖励 · 奖池触发率 ${reward.optDouble("chance", 0.0) * 100}%", style = RefugeTypography.headline(palette)) }
                    items(pool.objects("blueprints")) { blueprint ->
                        KnowledgeCard(palette, blueprint.optString("name"), "查看制造材料与来源", { onBlueprint(blueprint.optString("name")) })
                    }
                }
            } else if (destination == null) {
                item { ReferenceSearchField(backdrop, isDark, query, { query = it }, RefugeIcons.search, Modifier.fillMaxWidth()) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KnowledgeFilter(backdrop, palette, listOf("" to "全部星系") + current?.missions.orEmpty()
                            .flatMap { it.strings("availableSystems") }.distinct().sorted().map { it to it },
                            system, { system = it }, Modifier.weight(1f))
                        KnowledgeFilter(backdrop, palette, listOf("" to "全部类型") + current?.missions.orEmpty()
                            .filter { it.optString("missionType").isNotBlank() }.distinctBy { it.optString("missionType") }
                            .map { it.optString("missionType") to current!!.text(it, "missionType", chinese) }.sortedBy { it.second },
                            type, { type = it }, Modifier.weight(1f))
                    }
                }
                item { Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${visible.size} 条任务", style = RefugeTypography.caption(palette), modifier = Modifier.weight(1f))
                    RefugeCompactUtilityPill(backdrop, palette, RefugeIcons.sort,
                        if (rewardSort) "报酬从高到低" else "默认排序", { rewardSort = !rewardSort })
                } }
                if (!loading && error == null && visible.isEmpty()) item { ProductionEmptyState(palette, "没有匹配的任务") }
                items(visible, key = { it.optString("id") }) { row ->
                    KnowledgeCard(palette, current!!.text(row, "title", chinese),
                        listOf(current.text(row, "contractorName", chinese), row.strings("availableSystems").joinToString(" / "),
                            row.number("rewardUEC")?.let { String.format(Locale.US, "%,.0f aUEC", it) }.orEmpty()).filter(String::isNotBlank).joinToString(" · "),
                        { selection = row.optString("id") })
                }
            }
        }
        }
    }
}

@Composable
internal fun BlueprintDatabaseScreen(backdrop: LayerBackdrop, palette: RefugePalette, isDark: Boolean,
    initialQuery: String = "", onMission: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { TerminalKnowledgeRepository(context) }
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var category by rememberSaveable { mutableStateOf("") }
    var rewardOnly by rememberSaveable { mutableStateOf(false) }
    var page by remember { mutableIntStateOf(1) }
    var rows by remember { mutableStateOf(emptyList<JSONObject>()) }
    var total by remember { mutableIntStateOf(0) }
    var selection by rememberSaveable { mutableStateOf<Long?>(null) }
    var detail by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var attempt by remember { mutableIntStateOf(0) }
    // The reading position belongs to a destination, not to the shared list
    // container. A newly opened detail starts at its title; Back restores the
    // catalog position, including after visiting a linked reward mission.
    val listState = rememberLazyListState()
    val detailState = key(selection) { rememberLazyListState() }
    LaunchedEffect(query, category, rewardOnly, page, selection, attempt) {
        error = null; loading = true
        try {
            if (selection != null) { detail = null; detail = repository.blueprint(selection!!) }
            else {
                delay(300)
                val result = repository.blueprints(query, category, page, rewardOnly)
                rows = if (page == 1) result.rows else (rows + result.rows).distinctBy { it.optLong("id") }
                total = result.total
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: "蓝图载入失败" }
        finally { loading = false }
    }
    val destination = selection
    KnowledgePage(backdrop, palette, if (destination == null) "制造蓝图" else "蓝图详情",
        { if (selection != null) selection = null else onBack() }) {
        if ((destination != null && detail == null) || (destination == null && rows.isEmpty() && loading)) {
            if (loading) ProductionLoadingState(backdrop, palette, "正在读取蓝图")
            error?.let { ProductionErrorState(backdrop, palette, it, { attempt++ }) }
            return@KnowledgePage
        }
        key(destination) {
        LazyColumn(Modifier.weight(1f).testTag("blueprint-database"),
            state = if (destination == null) listState else detailState,
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("SCM · ${detail?.optString("dataVersion").orEmpty()}", style = RefugeTypography.caption(palette)) }
            if (destination == null) {
                item { ReferenceSearchField(backdrop, isDark, query, { query = it; page = 1; rows = emptyList() }, RefugeIcons.search, Modifier.fillMaxWidth()) }
                item { KnowledgeFilter(backdrop, palette, listOf("" to "全部分类", "FPSWeapons" to "武器", "FPSArmours" to "护甲", "Ammo" to "弹夹", "ShipWeapons" to "舰船武器", "Components" to "组件"), category,
                    { category = it; page = 1; rows = emptyList() }, Modifier.fillMaxWidth()) }
                item { Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$total 个蓝图", style = RefugeTypography.caption(palette), modifier = Modifier.weight(1f))
                    Text("任务奖励", style = RefugeTypography.body(palette))
                    Spacer(Modifier.width(12.dp))
                    RefugeLiquidToggle(backdrop, palette, rewardOnly,
                        { rewardOnly = !rewardOnly; page = 1; rows = emptyList() }, "只显示任务奖励蓝图")
                } }
                items(rows, key = { it.optLong("id") }) { row ->
                    KnowledgeCard(palette, row.blueprintTitle(), "${blueprintCategoryLabel(row.optString("categoryName"))} · ${row.optInt("craftTimeSeconds")} 秒", { selection = row.optLong("id") })
                }
                if (!loading && error == null && rows.isEmpty()) item { ProductionEmptyState(palette, "没有匹配的蓝图") }
                if (!loading && rows.size < total) item {
                    RefugeGlassControl(backdrop, palette, { page++ }, Modifier.fillMaxWidth(), "加载更多") {
                        Text("加载更多", style = RefugeTypography.body(palette).copy(color = palette.accent))
                    }
                }
            } else detail?.let { blueprint ->
                item { KnowledgeCard(palette, blueprint.blueprintTitle(), blueprint.optString("blueprintName")) {
                    blueprint.optString("thumbnailUrlHd").takeIf(String::isNotBlank)?.let { url ->
                        var imageFailed by remember(url) { mutableStateOf(false) }
                        if (imageFailed) Text("图片暂不可用", style = RefugeTypography.caption(palette))
                        else coil3.compose.AsyncImage(url, blueprint.blueprintTitle(),
                            modifier = Modifier.fillMaxWidth().height(140.dp), onError = { imageFailed = true })
                    }
                    Text("制造时间：${blueprint.optInt("craftTimeSeconds")} 秒", style = RefugeTypography.body(palette))
                } }
                items(blueprint.objects("slots"), key = { it.optInt("slotIndex") }) { slot ->
                    BlueprintMaterialCard(backdrop, palette, slot)
                }
                val ids = runCatching { JSONArray(blueprint.optString("rewardMissionIds")) }.getOrNull()
                val titles = runCatching { JSONArray(blueprint.optString("rewardMissionsCn").ifBlank { blueprint.optString("rewardMissions") }) }.getOrNull()
                if (ids != null && ids.length() > 0) {
                    item { Text("获取途径 · 奖励任务", style = RefugeTypography.headline(palette)) }
                    items((0 until ids.length()).toList()) { index ->
                        KnowledgeCard(palette, titles?.optString(index).orEmpty().ifBlank { "查看任务" }, onClick = { onMission(ids.getString(index)) })
                    }
                }
            }
            if (loading) item { ProductionLoadingState(backdrop, palette, "正在读取蓝图") }
            error?.let { item { ProductionErrorState(backdrop, palette, it, { attempt++ }) } }
        }
        }
    }
}

@Composable
private fun BlueprintMaterialCard(backdrop: LayerBackdrop, palette: RefugePalette, slot: JSONObject) {
    var choice by rememberSaveable(slot.optInt("id")) { mutableIntStateOf(0) }
    var quality by rememberSaveable(slot.optInt("id")) { mutableIntStateOf(500) }
    val options = slot.objects("options")
    val option = options.getOrNull(choice) ?: return
    val minimum = option.optInt("minQuality").coerceIn(0, 1000)
    val effectiveQuality = quality.coerceIn(minimum, 1000)
    val slotName = slot.optString("slotDisplayNameCn").ifBlank { slot.optString("slotDisplayName") }
    val materialName = option.optString("materialNameCn").ifBlank { option.optString("materialName") }
    val amount = option.number("standardCargoUnits")?.let { String.format(Locale.US, "%.2f SCU", it) }
        ?: "${option.optInt("quantity", 1)} 个"
    RefugeLightweightGlassSurface(
        palette,
        modifier = Modifier.fillMaxWidth().background(palette.accent.copy(alpha = .10f), refugeContinuousShape(12.dp)),
        padding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(slotName, style = RefugeTypography.headline(palette), modifier = Modifier.weight(1f))
                if (options.size > 1) {
                    KnowledgeFilter(backdrop, palette,
                        options.mapIndexed { index, value -> "$index" to value.optString("materialNameCn").ifBlank { value.optString("materialName") } },
                        "$choice", { choice = it.toInt() }, Modifier.widthIn(min = 110.dp, max = 180.dp))
                }
            }
            Row(Modifier.fillMaxWidth().background(palette.accent.copy(alpha = .16f), refugeContinuousShape(8.dp)).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(materialName, style = RefugeTypography.body(palette), modifier = Modifier.weight(1f))
                Text("|", color = palette.textMuted)
                Spacer(Modifier.width(8.dp))
                Text(amount, style = RefugeTypography.detailValue(palette).copy(color = palette.accent))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("品质 $effectiveQuality", style = RefugeTypography.caption(palette), modifier = Modifier.weight(1f))
                Text("最低 $minimum", style = RefugeTypography.caption(palette))
            }
            if (minimum < 1000) RefugeLiquidSlider({ effectiveQuality.toFloat() }, { quality = it.toInt() },
                valueRange = minimum.toFloat()..1000f, visibilityThreshold = 1f,
                backdrop = backdrop, palette = palette, label = "材料品质")
            val modifiers = blueprintModifiers(option, effectiveQuality)
            if (modifiers.isNotEmpty()) {
                Text("属性变化", style = RefugeTypography.caption(palette))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    modifiers.forEach { (name, value) ->
                        Text("$name ${String.format(Locale.US, "%+.2f%%", value)}", style = RefugeTypography.caption(palette).copy(color = if (value >= 0) palette.accent else palette.error), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun blueprintCategoryLabel(value: String) = when (value) {
    "FPSWeapons" -> "武器"
    "FPSArmours" -> "护甲"
    "Ammo" -> "弹夹"
    "ShipWeapons" -> "舰船武器"
    "Components" -> "组件"
    else -> value
}

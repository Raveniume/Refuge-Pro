@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)

package com.refuge.next.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.refuge.next.data.*
import com.refuge.next.design.*
import com.refuge.next.material.*
import com.refuge.next.reference.ReferenceSearchField
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt
import zone.ien.hig.MenuAction
import zone.ien.hig.MenuPickerAction

@Composable
internal fun NativeLoadoutScreen(backdrop: LayerBackdrop, palette: RefugePalette, isDark: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { ErkulLoadoutRepository.shared(context) }
    var branch by rememberSaveable { mutableStateOf("LIVE") }
    var catalog by remember { mutableStateOf(repository.cachedCatalog(branch)) }
    var shipId by rememberSaveable { mutableStateOf(repository.cachedCatalog(branch)?.ships?.firstOrNull()?.optString("className").orEmpty()) }
    var ship by remember { mutableStateOf(catalog?.let { repository.cachedShip(it, shipId) }) }
    var buildSlot by rememberSaveable { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf(JSONObject()) }
    var busy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var picker by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf<String?>(null) }
    var planName by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var compare by rememberSaveable { mutableStateOf(false) }
    var showPerformance by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(branch, attempt) {
        error = null
        // Publish the disk snapshot first. The editor must stay usable when
        // RSI/Erkul is unavailable; the network refresh replaces this frame
        // only after the local data is already on screen.
        runCatching { repository.catalog(branch) }
            .onSuccess { loaded ->
                catalog = loaded
                busy = false
                if (loaded.ships.none { it.optString("className") == shipId }) {
                    shipId = loaded.ships.firstOrNull()?.optString("className").orEmpty()
                }
            }
            .onFailure { failure ->
                if (catalog == null) error = failure.message ?: "目录载入失败"
            }
        runCatching { repository.catalog(branch, refresh = true) }
            .onSuccess { loaded ->
                catalog = loaded
                if (loaded.ships.none { it.optString("className") == shipId }) {
                    shipId = loaded.ships.firstOrNull()?.optString("className").orEmpty()
                }
            }
            .onFailure { failure ->
                if (catalog == null) error = failure.message ?: "目录载入失败"
            }
        busy = false
    }
    LaunchedEffect(catalog, shipId) {
        val current = catalog ?: return@LaunchedEffect
        val summary = current.ships.firstOrNull { it.optString("className") == shipId } ?: return@LaunchedEffect
        busy = true; error = null
        repository.cachedShip(current, shipId)?.let { ship = it; busy = false }
        try { ship = repository.ship(current, summary) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: "舰船载入失败" }
        finally { busy = false }
    }
    LaunchedEffect(catalog?.version, shipId, buildSlot) {
        val current = catalog ?: return@LaunchedEffect
        // Components are active by default. Older drafts may still contain
        // the removed disabled list; ignore it so every installed component
        // participates in the simulated metrics.
        draft = JSONObject(repository.loadDraft(branch, current.version, shipId, buildSlot).toString()).apply {
            remove("disabled")
        }
    }
    fun update(next: JSONObject) {
        draft = next
        catalog?.let { repository.saveDraft(branch, it.version, shipId, buildSlot, next) }
    }
    val slots = remember(ship, catalog, draft) { if (ship != null && catalog != null) resolveErkulSlots(ship!!, catalog!!, draft.obj("overrides")) else emptyList() }
    val metrics = remember(ship, slots, draft) { ship?.let { calculateErkulMetrics(it, slots, draft) } }
    val stock = remember(ship, catalog) { if (ship != null && catalog != null) calculateErkulMetrics(ship!!, resolveErkulSlots(ship!!, catalog!!, JSONObject()), JSONObject()) else null }
    BackHandler { if (picker != null) picker = null else onDismiss() }
    // Keep the editor in the activity composition. A platform Dialog creates
    // a second window and removes the live page during its opening frame.
    RefugeLiquidSheet(
        backdrop = backdrop,
        palette = palette,
        title = "",
        onDismiss = onDismiss,
        sheetHeight = 780.dp,
        contentScrollable = false,
        contentUnderHandle = true,
        modifier = Modifier.zIndex(5f),
    ) { modalBackdrop ->
            val canvasColor = if (palette.background.luminance() < .5f) androidx.compose.ui.graphics.Color.Black else palette.background
            Column(Modifier.fillMaxWidth().background(canvasColor).systemBarsPadding().testTag("native-loadout")) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RefugeCircularHeaderButton(backdrop, palette, RefugeIcons.back, "返回", onDismiss)
                    Text("改船", style = RefugeTypography.title(palette), modifier = Modifier.weight(1f).padding(start = 8.dp))
                    Box {
                        RefugeHeaderActionBar(backdrop, palette, listOf(
                            RefugeFloatingAction(RefugeIcons.refresh, "重置配装", { update(JSONObject()) }),
                            RefugeFloatingAction(RefugeIcons.more, "更多操作", { menu = "more" }),
                        ))
                        zone.ien.hig.CupertinoDropdownMenu(expanded = menu == "more", onDismissRequest = { menu = null },
                            backdrop = backdrop, containerColor = palette.contentSurfaceStrong, width = 220.dp,
                            modifier = Modifier.widthIn(max = 284.dp),
                            offset = androidx.compose.ui.unit.DpOffset(0.dp, 52.dp)) {
                            MenuAction({ attempt++; menu = null }, onClickLabel = "更新数据") { Text("更新数据", color = palette.text) }
                            MenuAction({ planName = ""; menu = "save" }, onClickLabel = "保存方案") { Text("保存方案", color = palette.text) }
                            MenuAction({ menu = "load" }, onClickLabel = "调用方案") { Text("调用方案", color = palette.text) }
                            MenuAction({ menu = "settings" }, onClickLabel = "配装设置") { Text("配装设置", color = palette.text) }
                        }
                    }
                }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        val reference = rememberShipReference(ship?.itemName().orEmpty())
                        RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), onClick = { picker = "ship" }, contentDescription = "选择舰船", padding = PaddingValues(14.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                reference?.images?.firstOrNull()?.let { url ->
                                    coil3.compose.AsyncImage(url, "舰船图片", contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(refugeContinuousShape(14.dp)))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(translatedShipName(ship?.itemName() ?: "选择舰船"), style = RefugeTypography.title(palette))
                                        Text(translatedShipName(reference?.manufacturer.orEmpty()), style = RefugeTypography.caption(palette))
                                    }
                                    Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
                                }
                                ship?.let { vessel ->
                                    Text("S${vessel.optInt("size")} · ${vessel.obj("vehicle").optInt("crewSize")} 人 · ${vessel.obj("precomputed").optInt("cargo")} SCU", style = RefugeTypography.caption(palette))
                                }
                                Text(if (branch == "LIVE") "LIVE 数据" else "PTU 数据", style = RefugeTypography.caption(palette))
                            }
                        }
                    }
                    if (busy && ship == null) item { ProductionLoadingState(backdrop, palette, "载入舰船") }
                    // Keep the last usable ship visible during a transient
                    // catalog refresh failure. An error card is useful only
                    // when there is no ship to interact with at all.
                    if (ship == null) error?.let { message ->
                        item { ProductionErrorState(backdrop, palette, message, { attempt++ }) }
                    }
                    if (ship != null && metrics != null) {
                        item { ErkulPowerCard(palette, ship!!, slots, draft, metrics!!, ::update, showTelemetry = true) }
                        item {
                            RefugeLightweightGlassSurface(
                                palette = palette,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showPerformance = true },
                                contentDescription = "性能总览",
                                padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("性能总览", style = RefugeTypography.title(palette))
                                        Text("DPS · 护盾 · 电力 · 散热", style = RefugeTypography.caption(palette))
                                    }
                                    metrics?.let { summary ->
                                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("${summary.burst.metric()} DPS", style = RefugeTypography.body(palette).copy(color = palette.accent))
                                            Text("散热 ${coolingLoadLabel(summary)} · ${summary.cooling.metric(" /s")}", style = RefugeTypography.caption(palette))
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Icon(RefugeIcons.chevron, null, tint = palette.textMuted)
                                }
                            }
                        }
                        slots.filter { it.port.obj("flags").optBoolean("editable") }.groupBy(::erkulSlotGroup).forEach { (group, groupSlots) ->
                            item { Text(group, style = RefugeTypography.title(palette)) }
                            items(groupSlots, key = { it.path }) { slot ->
                                RefugeLightweightGlassSurface(
                                    palette,
                                    Modifier.fillMaxWidth(),
                                    onClick = { picker = slot.path },
                                    contentDescription = "更换 ${erkulPortLabel(slot)}",
                                    padding = PaddingValues(12.dp),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(erkulPortLabel(slot), style = RefugeTypography.caption(palette))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                                                Text(translatedGameItemName(slot.item?.itemName() ?: "空挂点", slot.item?.optString("className")),
                                                    style = RefugeTypography.title(palette))
                                                Text("S${slot.port.optInt("minSize")}–S${slot.port.optInt("maxSize")} · 点击更换", style = RefugeTypography.caption(palette))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
            if (menu == "settings") {
                RefugeLiquidSheet(backdrop, palette, "配装设置", { menu = null }, sheetHeight = 320.dp) { local ->
                    Text("游戏版本", style = RefugeTypography.headline(palette))
                    RefugeLiquidModeSelector(local, palette, listOf("LIVE", "PTU"), if (branch == "LIVE") 0 else 1, {
                        branch = if (it == 0) "LIVE" else "PTU"; menu = null
                    })
                }
            }
            if (showPerformance && ship != null && metrics != null) {
                LoadoutPerformanceSheet(
                    backdrop = backdrop,
                    palette = palette,
                    metrics = metrics!!,
                    stock = stock,
                    ship = ship!!,
                    compare = compare,
                    onCompareChanged = { compare = it },
                    onDismiss = { showPerformance = false },
                )
            }
            if (menu == "save") {
                zone.ien.hig.CupertinoAlertDialog(onDismissRequest = { menu = null }, title = { Text("保存方案", color = palette.text) },
                    containerColor = palette.contentSurfaceStrong,
                    message = { zone.ien.hig.CupertinoTextField(planName, { planName = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        placeholder = { Text("方案名称", color = palette.textMuted) }, singleLine = true) },
                    buttons = {
                        action(onClick = { menu = null }, style = zone.ien.hig.AlertActionStyle.Cancel) { Text("取消", color = palette.accent) }
                        action(enabled = planName.isNotBlank() && catalog != null, onClick = {
                        catalog?.let { repository.savePlan(branch, it.version, shipId, planName, draft) }
                        menu = null; notice = "已保存"
                    }) { Text("保存", color = if (planName.isNotBlank()) palette.accent else palette.textMuted) }
                    })
            }
            if (menu == "load") {
                val saved = catalog?.let { repository.savedPlans(branch, it.version, shipId) }.orEmpty()
                RefugeLiquidSheet(backdrop, palette, "调用方案", { menu = null }, sheetHeight = 520.dp) { _ ->
                    if (saved.isEmpty()) Text("暂无保存方案", style = RefugeTypography.body(palette))
                    saved.forEach { (name, value) -> LoadoutCard(backdrop, palette, name, "", onClick = { update(value); menu = null }) }
                }
            }
            notice?.let { message ->
                LaunchedEffect(message) { kotlinx.coroutines.delay(1600); notice = null }
                Box(Modifier.fillMaxSize().padding(bottom = 32.dp), contentAlignment = Alignment.BottomCenter) {
                    Text(message, style = RefugeTypography.body(palette), modifier = Modifier.background(palette.contentSurface).padding(12.dp))
                }
            }
            val currentCatalog = catalog
            if (picker != null && currentCatalog != null) {
                val activeSlot = slots.firstOrNull { it.path == picker }
                NativeLoadoutPicker(backdrop, palette, isDark, currentCatalog, activeSlot, picker == "ship", onDismiss = { picker = null }, onPick = { selected ->
                    if (picker == "ship") shipId = selected!!.getString("className") else if (activeSlot != null) {
                        val overrides = JSONObject(draft.obj("overrides").toString())
                        overrides.keys().asSequence().filter { it.startsWith(activeSlot.path + "/") }.toList().forEach { overrides.remove(it) }
                        overrides.put(activeSlot.path, selected?.optString("className") ?: "")
                        update(JSONObject(draft.toString()).put("overrides", overrides))
                    }
                    picker = null
                })
            }
        }
    }

/** Compatibility entry retained for isolated rendering fixtures. */
@Composable
internal fun NativeLoadoutScreen(palette: RefugePalette, isDark: Boolean, onDismiss: () -> Unit) {
    NativeLoadoutScreen(rememberLayerBackdrop(), palette, isDark, onDismiss)
}

@Composable
private fun LoadoutCard(backdrop: com.kyant.backdrop.backdrops.LayerBackdrop, palette: RefugePalette, title: String, subtitle: String, imageUrl: String? = null, onClick: () -> Unit) {
    RefugeGlassControl(backdrop, palette, onClick = onClick, modifier = Modifier.fillMaxWidth(), contentDescription = title, padding = PaddingValues(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (imageUrl != null) coil3.compose.AsyncImage(imageUrl, null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(refugeContinuousShape(10.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(translatedShipName(title), style = RefugeTypography.title(palette))
                if (subtitle.isNotBlank()) Text(translatedShipName(subtitle), style = RefugeTypography.caption(palette))
            }
            Icon(RefugeIcons.chevron, null, tint = palette.textMuted, modifier = Modifier.size(18.dp))
        }
    }
}

private fun Double?.metric(unit: String = ""): String = if (this == null) "—" else String.format(Locale.US, if (this % 1.0 == 0.0) "%,.0f" else "%,.1f", this) + unit

private fun coolingLoadLabel(metrics: ErkulMetrics): String {
    if (metrics.cooling <= 0.0) return "0%"
    return "${((metrics.coolingUsed / metrics.cooling).coerceAtLeast(0.0) * 100.0).roundToInt()}%"
}

@Composable
private fun LoadoutPerformanceSheet(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    palette: RefugePalette,
    metrics: ErkulMetrics,
    stock: ErkulMetrics?,
    ship: JSONObject,
    compare: Boolean,
    onCompareChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    RefugeLiquidSheet(backdrop, palette, "性能总览", onDismiss, sheetHeight = 780.dp) { _ ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("当前配装", style = RefugeTypography.headline(palette))
            }
            Text("对比原厂", style = RefugeTypography.body(palette))
            Spacer(Modifier.width(8.dp))
            zone.ien.hig.CupertinoSwitch(compare, onCheckedChange = onCompareChanged)
        }
        listOf("火力", "防御", "电力与散热", "航行").forEachIndexed { section, title ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = RefugeTypography.headline(palette))
                LoadoutMetricsCard(palette, metrics, if (compare) stock else null, ship, section)
            }
        }
        val coolingRatio = if (metrics.cooling > 0.0) (metrics.coolingUsed / metrics.cooling).coerceAtLeast(0.0).toFloat() else 0f
        RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("散热负载", style = RefugeTypography.body(palette), modifier = Modifier.weight(1f))
                    Text(coolingLoadLabel(metrics), style = RefugeTypography.detailValue(palette).copy(color = if (coolingRatio > 1f) palette.error else palette.accent))
                }
                LinearProgressIndicator(coolingRatio.coerceAtMost(1f), Modifier.fillMaxWidth().height(5.dp), if (coolingRatio > 1f) palette.error else palette.accent, palette.glassStrong)
                Text("${metrics.coolingUsed.metric(" /s")} / ${metrics.cooling.metric(" /s")}", style = RefugeTypography.caption(palette))
            }
    }
}

}

@Composable
private fun LoadoutMetricsCard(palette: RefugePalette, m: ErkulMetrics, stock: ErkulMetrics?, ship: JSONObject, section: Int) {
    val rows: List<Triple<String, String, String?>> = when(section) {
        0 -> listOf(Triple("总爆发 DPS", m.burst.metric(), stock?.burst?.metric()), Triple("驾驶员 DPS", m.pilotBurst.metric(), stock?.pilotBurst?.metric()), Triple("持续 DPS（电容／热循环）", m.sustained.metric(), stock?.sustained?.metric()))
        1 -> listOf(Triple("护盾容量", m.shield.metric(" HP"), stock?.shield?.metric(" HP")), Triple("护盾回复", m.shieldRegen.metric(" HP/s"), stock?.shieldRegen?.metric(" HP/s")), Triple("船体生命值", ship.obj("precomputed").obj("hp").number("total").metric(" HP"), null))
        2 -> listOf(Triple("发电量", m.power.metric(" 格"), stock?.power?.metric(" 格")), Triple("满载电力需求", m.powerUsed.metric(" 格"), stock?.powerUsed?.metric(" 格")), Triple("额定散热", m.cooling.metric(" /s"), stock?.cooling?.metric(" /s")), Triple("热负载", m.coolingUsed.metric(" /s"), stock?.coolingUsed?.metric(" /s")))
        else -> listOf(Triple("整备质量", m.mass.metric(" kg"), stock?.mass?.metric(" kg")), Triple("SCM 速度", ship.obj("precomputed").obj("flight").number("scmSpeed").metric(" m/s"), null), Triple("最大速度", ship.obj("precomputed").obj("flight").number("maxSpeed").metric(" m/s"), null), Triple("量子速度", m.quantumSpeed?.div(1e6).metric(" Mm/s"), stock?.quantumSpeed?.div(1e6).metric(" Mm/s")), Triple("量子航程", m.quantumRange.metric(" Gm"), stock?.quantumRange?.metric(" Gm")))
    }
    RefugeLightweightGlassSurface(palette, Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEach { (label, value, before) ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(label, style = RefugeTypography.caption(palette))
                    Text(value, style = RefugeTypography.detailValue(palette).copy(color = palette.accent))
                    before?.let { Text("原厂 $it", style = RefugeTypography.caption(palette)) }
                }
            }
            if (section == 2 && m.powerUsed > m.power) Text("电力不足", color = palette.error, style = RefugeTypography.body(palette))
        }
    }
}

@Composable
private fun NativeLoadoutPicker(backdrop: com.kyant.backdrop.backdrops.LayerBackdrop, palette: RefugePalette, isDark: Boolean,
    catalog: ErkulCatalog, slot: ErkulSlot?, ships: Boolean, onDismiss: () -> Unit, onPick: (JSONObject?) -> Unit) {
    val translation = LocalRefugeTranslation.current
    var query by rememberSaveable { mutableStateOf("") }
    var manufacturer by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("") }
    var manufacturerMenu by remember { mutableStateOf(false) }
    var classMenu by remember { mutableStateOf(false) }
    val all = remember(catalog, slot, ships) { if (ships) catalog.ships else catalog.components.values.filter { slot != null && erkulSlotAccepts(slot, it) } }
    fun maker(item: JSONObject) = item.optString("manufacturerName").ifBlank { item.obj("manufacturer").optString("className") }
    val manufacturers = remember(all) { all.map(::maker).filter { it.isNotBlank() }.distinct().sorted() }
    val classes = remember(all) { all.map { it.obj("i18n").optString("class") }.filter { it.isNotBlank() }.distinct().sorted() }
    val visible = remember(all, query, manufacturer, selectedClass) { all.filter { (query.isBlank() || it.itemName().contains(query,true) || translation?.translateGameItem(it.itemName())?.contains(query, true) == true) &&
        (manufacturer.isBlank() || maker(it) == manufacturer) && (selectedClass.isBlank() || it.obj("i18n").optString("class") == selectedClass) }.sortedBy { it.itemName() } }
    RefugeLiquidSheet(backdrop, palette, if (ships) "选择舰船" else "更换组件", onDismiss, sheetHeight = 780.dp, contentScrollable = false) { modalBackdrop ->
        zone.ien.hig.CupertinoSearchTextField(value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(), textStyle = RefugeTypography.body(palette),
            placeholder = { Text("搜索舰船或组件", style = RefugeTypography.caption(palette)) }, cancelButton = null)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                RefugeCompactUtilityPill(
                    modalBackdrop, palette, RefugeIcons.filter,
                    manufacturer.ifBlank { "全部厂商" },
                    onClick = { manufacturerMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                zone.ien.hig.CupertinoDropdownMenu(
                    expanded = manufacturerMenu,
                    onDismissRequest = { manufacturerMenu = false },
                    backdrop = modalBackdrop,
                    containerColor = palette.contentSurfaceStrong,
                    width = 240.dp,
                ) {
                    MenuPickerAction(
                        isSelected = manufacturer.isBlank(),
                        onClick = { manufacturer = ""; manufacturerMenu = false },
                    ) { Text("全部厂商", color = palette.text) }
                    manufacturers.forEach { option ->
                        MenuPickerAction(
                            isSelected = manufacturer == option,
                            onClick = { manufacturer = option; manufacturerMenu = false },
                        ) { Text(translatedShipName(option), color = palette.text) }
                    }
                }
            }
            if (classes.isNotEmpty()) Box(Modifier.weight(1f)) {
                RefugeCompactUtilityPill(
                    modalBackdrop, palette, RefugeIcons.filter,
                    selectedClass.ifBlank { "全部类别" },
                    onClick = { classMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                zone.ien.hig.CupertinoDropdownMenu(
                    expanded = classMenu,
                    onDismissRequest = { classMenu = false },
                    backdrop = modalBackdrop,
                    containerColor = palette.contentSurfaceStrong,
                    width = 240.dp,
                ) {
                    MenuPickerAction(
                        isSelected = selectedClass.isBlank(),
                        onClick = { selectedClass = ""; classMenu = false },
                    ) { Text("全部类别", color = palette.text) }
                    classes.forEach { option ->
                        MenuPickerAction(
                            isSelected = selectedClass == option,
                            onClick = { selectedClass = option; classMenu = false },
                        ) { Text(translatedShipName(option), color = palette.text) }
                    }
                }
            }
        }
        Text("${visible.size} 个" + if (ships) "舰船" else "兼容组件", style = RefugeTypography.caption(palette))
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!ships) item { LoadoutCard(backdrop, palette, "卸下组件", "", onClick = { onPick(null) }) }
            items(visible, key = { it.getString("className") }) { item ->
                LoadoutCard(backdrop, palette, item.itemName(), listOf(maker(item), item.number("size")?.let { "S${it.toInt()}" }.orEmpty(), item.optString("grade"), item.obj("i18n").optString("class")).filter { it.isNotBlank() }.joinToString(" · "),
                    imageUrl = if (ships) rememberShipReference(item.itemName())?.images?.firstOrNull() else null) { onPick(item) }
            }
        }
    }
}

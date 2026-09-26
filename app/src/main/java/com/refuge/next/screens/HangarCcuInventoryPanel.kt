package com.refuge.next.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.data.*
import com.refuge.next.design.*
import com.refuge.next.material.*
import com.refuge.next.reference.ReferenceSearchField

@Composable
internal fun HangarCcuInventoryPanel(
    backdrop: LayerBackdrop, palette: RefugePalette, isDark: Boolean,
    ccuRepository: CcuRepository, refreshKey: Int, ownedSeeds: List<CcuShip>,
    repository: HangarRepository, inventory: List<HangarItem>, onOpenOwnedCcu: (Long) -> Unit,
    onShipSelector: (ShipSelectorRequest) -> Unit,
) {
    var showInventory by remember { mutableStateOf(false) }
    var snapshot by remember(repository) { mutableStateOf(inventory) }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableIntStateOf(0) }
    var eligibleOnly by remember { mutableStateOf(false) }
    LaunchedEffect(inventory) { snapshot = inventory }
    LaunchedEffect(showInventory) {
        if (showInventory) runCatching { repository.refreshInventory() }
            .onSuccess { snapshot = it }.onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
                android.util.Log.w("RefugeCcu", "CCU refresh failed", it)
            }
    }
    val owned = remember(snapshot) { snapshot.filter { it.isUpgrade } }
    HangarUpgradePanel(backdrop, palette, isDark, ccuRepository, refreshKey, ownedSeeds,
        onOwnedInventory = { showInventory = true }, onShipSelector = onShipSelector,
        inventoryCount = owned.sumOf { it.quantity })
    if (showInventory) {
        val translation = LocalRefugeTranslation.current
        val visible = remember(owned, query, sort, eligibleOnly, translation) {
            val rows = owned.filter { item ->
                (!eligibleOnly || item.canUpgrade) && listOf(item.title, item.originalName,
                    item.upgradeFrom.orEmpty(), item.upgradeTo.orEmpty()).any {
                    it.contains(query.trim(), true) || translation?.translate(it)?.contains(query.trim(), true) == true
                }
            }
            when (sort) {
                1 -> rows.sortedBy { it.upgradeTo ?: it.title }
                2 -> rows.sortedByDescending { parseUpgradeUsdCents(it.price) ?: -1 }
                else -> rows.sortedBy { it.upgradeFrom ?: it.title }
            }
        }
        RefugeLiquidSheet(backdrop, palette, "机库 CCU", { showInventory = false }, sheetHeight = 740.dp,
            contentScrollable = false) { local ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReferenceSearchField(local, isDark, query, { query = it }, RefugeIcons.search, Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${visible.sumOf { it.quantity }} 件", style = RefugeTypography.caption(palette), modifier = Modifier.weight(1f))
                    TextButton({ sort = (sort + 1) % 3 }) { Text(listOf("来源", "目标", "回收价值")[sort], color = palette.accent) }
                    TextButton({ eligibleOnly = !eligibleOnly }) { Text(if (eligibleOnly) "仅可应用" else "全部", color = palette.accent) }
                }
                if (visible.isEmpty()) Text("没有匹配的 CCU", style = RefugeTypography.body(palette))
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 510.dp)) {
                    itemsIndexed(visible, key = { _, item -> item.id }) { index, item ->
                        RefugeGlassListGroup(local, palette, roundTop = index == 0, roundBottom = index == visible.lastIndex,
                            padding = PaddingValues(horizontal = 16.dp)) {
                            RefugeGlassListRow(palette, onClick = {
                                showInventory = false
                                onOpenOwnedCcu(item.id)
                            }, contentDescription = item.title, isLast = index == visible.lastIndex) {
                                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(translatedShipName(item.upgradeFrom ?: item.originalName), style = RefugeTypography.body(palette), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(RefugeIcons.chevron, null, tint = palette.textMuted, modifier = Modifier.size(18.dp))
                                        Text(translatedShipName(item.upgradeTo ?: item.title), style = RefugeTypography.headline(palette), modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${item.quantity} 件", style = RefugeTypography.caption(palette))
                                        Text("回收 ${item.price}", style = RefugeTypography.caption(palette))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

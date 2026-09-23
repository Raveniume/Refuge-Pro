package com.refuge.next.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.refuge.next.design.translatedShipName
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.refuge.next.R
import com.refuge.next.data.DestructiveAction
import com.refuge.next.data.HangarItem
import com.refuge.next.data.HangarRepository
import com.refuge.next.data.RsiAuthDataSource
import com.refuge.next.data.CcuPurchaseRepository
import com.refuge.next.data.SafeMutationGuard
import com.refuge.next.data.UserPresence
import com.refuge.next.data.UpgradePriceQuote
import com.refuge.next.data.upgradeDiscount
import com.refuge.next.design.RefugeIconSize
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import com.refuge.next.design.refugeContinuousShape
import com.refuge.next.material.PageGlassScope
import com.refuge.next.material.RefugeFloatingAction
import com.refuge.next.material.RefugeGlassControl
import com.refuge.next.material.RefugeHeaderActionBar
import com.refuge.next.material.RefugeIcons
import com.refuge.next.material.RefugeLiquidGlass
import com.refuge.next.material.RefugeLiquidSheet
import com.refuge.next.material.RefugeStandardGlassSurface
import com.refuge.next.reference.OfficialLiquidButtonPort
import com.refuge.next.reference.ReferenceLiquidButton
import com.refuge.next.reference.ReferenceSearchField
import org.json.JSONObject
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val UpgradeDiscountOrange = Color(0xFFFF8A00)

internal data class UpgradeSku(
    val id: Int,
    val title: String,
    val price: Int,
    val body: String,
    val available: Boolean,
    val originalTitle: String = title,
) {
    val isWarbond: Boolean
        get() = originalTitle.contains("warbond", ignoreCase = true) || title.contains("warbond", ignoreCase = true) ||
            title.contains("战争债券") ||
            body.contains("warbond", ignoreCase = true) ||
            body.contains("战争债券")

    val displayTitle: String
        get() = when {
            title.contains("warbond edition", ignoreCase = true) -> "战争债券版"
            title.contains("standard edition", ignoreCase = true) -> "标准版"
            title.isBlank() -> "默认版本"
            else -> title
        }
}

internal data class UpgradeShipOption(
    val id: Int,
    val name: String,
    val msrp: Int,
    val focus: String,
    val manufacturer: String,
    val imageUrl: String?,
    val skus: List<UpgradeSku>,
    val originalName: String = name,
) {
    val availableSkus: List<UpgradeSku>
        get() = skus.filter { it.available && it.id > 0 && it.price > 0 }

    val lowestAvailablePrice: Int?
        get() = availableSkus.map(UpgradeSku::price).filter { it > 0 }.minOrNull()

    val discount: Int
        get() = lowestAvailablePrice?.let { upgradeDiscount(msrp, it) } ?: 0

    val hasDiscount: Boolean
        get() = discount > 0
}

private enum class UpgradeStage { TARGET, SKU, SOURCE, REVIEW }

/** Store entry: read-only selector parity for purchasing a new CCU. */
@Composable
fun StoreUpgradePurchaseScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    isDark: Boolean,
    rootTab: Int,
    onNavigate: (Int) -> Unit,
    auth: RsiAuthDataSource,
    purchaseRepository: CcuPurchaseRepository,
    hangarRepository: com.refuge.next.data.HangarRepository,
    translationRepository: com.refuge.next.data.TranslationRepository,
    presence: UserPresence,
    avatarUrl: String?,
    onAvatarClick: () -> Unit,
) {
    // Keep the synchronous first composition cheap. The repository exposes a
    // parsed cache snapshot through an IO-bound await; parsing the nested SKU
    // tree here used to happen on the Compose thread and could freeze the
    // selector while the app was still drawing its root page.
    var catalog by remember(purchaseRepository) { mutableStateOf(emptyList<UpgradeShipOption>()) }
    var ownedShips by remember(hangarRepository) { mutableStateOf(hangarRepository.cachedOwnedShips()) }
    LaunchedEffect(hangarRepository) {
        ownedShips = hangarRepository.awaitCachedOwnedShips()
        runCatching { hangarRepository.ownedShips() }.onSuccess { ownedShips = it }
    }
    var target by remember { mutableStateOf<UpgradeShipOption?>(null) }
    var sku by remember { mutableStateOf<UpgradeSku?>(null) }
    var sourceIds by remember { mutableStateOf(emptySet<Int>()) }
    var source by remember { mutableStateOf<UpgradeShipOption?>(null) }
    var stage by remember { mutableStateOf(UpgradeStage.TARGET) }
    var query by remember { mutableStateOf("") }
    var loading by remember(purchaseRepository) { mutableStateOf(catalog.isEmpty()) }
    var catalogError by remember { mutableStateOf<String?>(null) }
    var catalogAttempt by remember { mutableIntStateOf(0) }
    var sourceAttempt by remember { mutableIntStateOf(0) }
    var quoteAttempt by remember { mutableIntStateOf(0) }
    var catalogFresh by remember { mutableStateOf(false) }
    var quote by remember { mutableStateOf<UpgradePriceQuote?>(null) }
    var quoteLoading by remember { mutableStateOf(false) }
    var quoteError by remember { mutableStateOf<String?>(null) }
    var sourceLoading by remember { mutableStateOf(false) }
    var sourceError by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val guard = remember { SafeMutationGuard() }

    com.refuge.next.material.RefreshWhileVisible(purchaseRepository to catalogAttempt) {
        val cached = withContext(Dispatchers.IO) {
            purchaseRepository.awaitCachedCatalog()?.let { parseUpgradeCatalog(it, translationRepository) }.orEmpty()
        }
        if (cached.isNotEmpty()) catalog = cached
        loading = catalog.isEmpty()
        catalogError = null
        catalogFresh = false
        val refreshed = runCatching { purchaseRepository.catalog() }
        val parsed = withContext(Dispatchers.IO) {
            refreshed.getOrNull()?.let { parseUpgradeCatalog(it, translationRepository) }.orEmpty()
        }
        if (parsed.isNotEmpty()) { catalog = parsed; catalogFresh = true }
        else if (refreshed.isSuccess) catalogError = "RSI 未返回有效目录"
        refreshed.exceptionOrNull()?.let { error ->
            catalogError = error.message ?: "升级目录读取失败"
        }
        loading = false
    }
    LaunchedEffect(ownedShips, catalog) {
        val selected = target ?: return@LaunchedEffect
        val refreshed = catalog.firstOrNull { it.id == selected.id }
        if (refreshed?.availableSkus.isNullOrEmpty()) {
            target = null
            sku = null
            source = null
            stage = UpgradeStage.TARGET
        } else {
            target = refreshed
            if (sku != null && refreshed!!.availableSkus.none { it.id == sku?.id }) {
                sku = null
                source = null
                stage = UpgradeStage.SKU
            }
        }
    }
    LaunchedEffect(sku?.id, purchaseRepository, sourceAttempt) {
        source = null
        sourceError = null
        val selectedSku = sku ?: run {
            sourceIds = emptySet()
            sourceLoading = false
            return@LaunchedEffect
        }
        // Publish the previous source snapshot synchronously, then replace it
        // with RSI's refreshed result. The selector never opens as a blank page.
        sourceIds = purchaseRepository.cachedSourceIds(selectedSku.id)
        sourceLoading = sourceIds.isEmpty()
        runCatching { purchaseRepository.sourceIds(selectedSku.id) }
            .onSuccess { refreshed -> sourceIds = refreshed }
            .onFailure { failure ->
                sourceError = failure.message ?: "可升级来源读取失败"
            }
        sourceLoading = false
    }

    LaunchedEffect(source?.id, target?.id, sku?.id, purchaseRepository, quoteAttempt) {
        quote = null
        quoteError = null
        val from = source
        val to = target
        val edition = sku
        if (from == null || to == null || edition == null) { quoteLoading = false; return@LaunchedEffect }
        quoteLoading = true
        try { quote = purchaseRepository.quote(from.id, to.id, edition.id) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (failure: Exception) {
            // Keep the review step usable with the locally cached catalogue.
            val localDifference = (edition.price - from.msrp).coerceAtLeast(0)
            if (localDifference > 0) {
                quote = UpgradePriceQuote(from.id, to.id, edition.id, localDifference)
            } else {
                quoteError = failure.message ?: "升级报价读取失败"
            }
        }
        finally { quoteLoading = false }
    }
    val complete = quote?.let { it.sourceShipId == source?.id && it.targetShipId == target?.id && it.skuId == sku?.id } == true
    val visibleTargets = remember(catalog, query, ownedShips) {
        catalog
            .asSequence()
            .filter { it.availableSkus.isNotEmpty() }
            .filter { option ->
                query.isBlank() || option.name.contains(query.trim(), true) || option.originalName.contains(query.trim(), true) ||
                    option.focus.contains(query, true) || option.manufacturer.contains(query, true)
            }
            .toList()
    }
    val sourceChoices = remember(catalog, sourceIds, target?.id, query) {
        catalog
            .asSequence()
            .filter { it.id in sourceIds && it.id != target?.id && it.msrp < (target?.msrp ?: Int.MAX_VALUE) }
            .filter { option ->
                query.isBlank() || option.name.contains(query.trim(), true) || option.originalName.contains(query.trim(), true) ||
                    option.focus.contains(query, true) || option.manufacturer.contains(query, true)
            }
            .sortedBy { it.msrp }
            .toList()
    }

    RefugeLiquidSheet(
        backdrop = backdrop,
        palette = palette,
        title = "购买舰船升级",
        onDismiss = { onNavigate(rootTab) },
        leadingAction = { actionBackdrop ->
            com.refuge.next.material.RefugeCircularHeaderButton(
                backdrop = actionBackdrop,
                palette = palette,
                icon = RefugeIcons.back,
                contentDescription = "返回商店",
                onClick = { onNavigate(rootTab) },
            )
        },
        sheetHeight = 780.dp,
        actionOverContent = false,
        actionBottomPadding = 12.dp,
        transparentActionArea = false,
        contentScrollable = false,
        action = { actionBackdrop ->
            OfficialLiquidButtonPort(
                onClick = {
                    if (complete) notice = guard.execute(DestructiveAction.UPGRADE_PURCHASE).message
                },
                backdrop = actionBackdrop,
                modifier = Modifier.fillMaxWidth(),
                enabled = complete,
                tint = if (complete) palette.accent else Color.Unspecified,
                surfaceColor = if (complete) Color.Unspecified else palette.glassStrong.copy(alpha = .16f),
                visualHeight = 56.dp,
                contentPadding = 16.dp,
            ) {
                Icon(
                    RefugeIcons.hangarUpgrade,
                    contentDescription = null,
                    tint = if (complete) Color.White else palette.textMuted.copy(alpha = .78f),
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    when {
                        notice != null -> "报价已就绪"
                        complete -> "查看报价"
                        else -> "选择升级"
                    },
                    style = RefugeTypography.body(palette).copy(
                        color = if (complete) Color.White else palette.textMuted.copy(alpha = .78f),
                    ),
                    maxLines = 1,
                )
            }
        },
    ) { modalBackdrop ->
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 682.dp),
            contentPadding = PaddingValues(bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm),
        ) {
            if (loading && catalog.isEmpty()) item {
                ProductionLoadingState(modalBackdrop, palette, "正在读取 RSI 可售升级")
            } else if (catalogError != null && catalog.isEmpty()) item {
                ProductionErrorState(modalBackdrop, palette, catalogError!!, onRetry = { catalogAttempt++ })
            } else {
                // A stale catalog remains usable while the refresh retries in
                // the background. Keep that state quiet so the selector never
                // turns a transient network condition into a cache message.
                if (stage != UpgradeStage.TARGET) item {
                    androidx.compose.material.TextButton(onClick = {
                        stage = if (stage == UpgradeStage.SOURCE) UpgradeStage.SKU else UpgradeStage.TARGET
                        query = ""
                    }) { Text("返回上一步", color = palette.accent) }
                }
                when (stage) {
                    UpgradeStage.TARGET -> {
                        item { FlowTitle(palette, "1", "选择目标舰船") }
                        item {
                            ReferenceSearchField(
                                backdrop = modalBackdrop,
                                isDark = isDark,
                                value = query,
                                onValueChange = { query = it },
                                searchIcon = RefugeIcons.search,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (visibleTargets.isEmpty()) item { ProductionEmptyState(palette, "没有匹配的可售舰船") }
                        items(visibleTargets, key = { "target:${it.id}" }) { ship ->
                            val currentPrice = ship.lowestAvailablePrice ?: ship.msrp
                            UpgradeChoice(
                                backdrop = modalBackdrop,
                                palette = palette,
                                title = translatedShipName(ship.name),
                                subtitle = listOf(translatedShipName(ship.manufacturer), translatedShipName(ship.focus)).filter(String::isNotBlank).joinToString(" · "),
                                trailing = dollars(currentPrice),
                                originalTrailing = ship.msrp.takeIf { ship.discount > 0 }?.let(::dollars),
                                selected = target?.id == ship.id,
                                enabled = true,
                                emphasized = ship.hasDiscount,
                                badge = ship.discount.takeIf { it > 0 }?.let { "省 ${dollars(it)}" }
                                    ?: if (ship.hasDiscount) "战争债券" else null,
                                imageUrl = ship.imageUrl,
                            ) {
                                target = ship
                                sku = null
                                source = null
                                sourceIds = emptySet()
                                notice = null
                                query = ""
                                stage = UpgradeStage.SKU
                            }
                        }
                    }

                    UpgradeStage.SKU -> {
                        val selectedTarget = target
                        if (selectedTarget == null) {
                            item { ProductionEmptyState(palette, "请先选择目标舰船") }
                        } else {
                            item { FlowTitle(palette, "2", "选择目标版本") }
                            items(selectedTarget.skus, key = { "sku:${it.id}" }) { option ->
                                val discounted = upgradeDiscount(selectedTarget.msrp, option.price) > 0
                                UpgradeChoice(
                                    backdrop = modalBackdrop,
                                    palette = palette,
                                    title = option.displayTitle,
                                    subtitle = translatedShipName(option.body),
                                    trailing = dollars(option.price),
                                    originalTrailing = selectedTarget.msrp.takeIf { discounted }?.let(::dollars),
                                    selected = sku?.id == option.id,
                                    enabled = option.available && option.id > 0 && option.price > 0,
                                    emphasized = discounted,
                                    badge = when {
                                        !option.available -> "暂不可用"
                                        option.isWarbond -> "Warbond"
                                        discounted -> "优惠"
                                        else -> null
                                    },
                                ) {
                                    sku = option
                                    source = null
                                    notice = null
                                    query = ""
                                    stage = UpgradeStage.SOURCE
                                }
                            }
                        }
                    }

                    UpgradeStage.SOURCE -> {
                        item { FlowTitle(palette, "3", "选择来源舰船") }
                        if (sourceLoading && sourceIds.isEmpty()) {
                            item { ProductionLoadingState(modalBackdrop, palette, "正在读取可升级来源") }
                        } else if (sourceError != null && sourceIds.isEmpty()) {
                            item { ProductionErrorState(modalBackdrop, palette, sourceError!!, onRetry = { sourceAttempt++ }) }
                        } else {
                            // Keep the previous source list interactive while a
                            // refresh fails; show an error only when there is
                            // no usable source list at all.
                            item {
                                ReferenceSearchField(
                                    backdrop = modalBackdrop,
                                    isDark = isDark,
                                    value = query,
                                    onValueChange = { query = it },
                                    searchIcon = RefugeIcons.search,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (sourceChoices.isEmpty()) item { ProductionEmptyState(palette, "该版本暂无可升级来源舰船") }
                            items(sourceChoices, key = { "source:${it.id}" }) { ship ->
                                UpgradeChoice(
                                    backdrop = modalBackdrop,
                                    palette = palette,
                                    title = translatedShipName(ship.name),
                                    subtitle = listOf(translatedShipName(ship.manufacturer), translatedShipName(ship.focus)).filter(String::isNotBlank).joinToString(" · "),
                                    trailing = dollars(ship.msrp),
                                    selected = source?.id == ship.id,
                                    enabled = true,
                                    imageUrl = ship.imageUrl,
                                ) {
                                    source = ship
                                    notice = null
                                    query = ""
                                    stage = UpgradeStage.REVIEW
                                }
                            }
                        }
                    }

                    UpgradeStage.REVIEW -> {
                        val selectedTarget = target
                        val selectedSku = sku
                        val selectedSource = source
                        if (selectedTarget == null || selectedSku == null || selectedSource == null) {
                            item { ProductionEmptyState(palette, "请选择完整升级路径") }
                        } else {
                            item { FlowTitle(palette, "4", "升级价格") }
                            item {
                                UpgradeCostPreview(
                                    backdrop = modalBackdrop,
                                    palette = palette,
                                    source = selectedSource,
                                    target = selectedTarget,
                                    sku = selectedSku,
                                    quote = quote?.takeIf { it.sourceShipId == selectedSource.id && it.targetShipId == selectedTarget.id && it.skuId == selectedSku.id },
                                )
                            }
                            if (quoteLoading) item { Text("正在读取 RSI 升级差价…", style = RefugeTypography.secondary(palette)) }
                            quoteError?.let { message -> item { ProductionErrorState(modalBackdrop, palette, message, onRetry = { quoteAttempt++ }) } }
                            notice?.let { message ->
                                item {
                                    Text(
                                        message,
                                        style = RefugeTypography.caption(palette).copy(color = palette.textSecondary),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Hangar detail entry: select an eligible pledge for an already-owned CCU. */
@Composable
fun HangarOwnedCcuApplyScreen(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    isDark: Boolean,
    rootTab: Int,
    onNavigate: (Int) -> Unit,
    repository: HangarRepository,
    ownedCcuId: Long,
    presence: UserPresence,
    avatarUrl: String?,
    onAvatarClick: () -> Unit,
) {
    var upgrade by remember { mutableStateOf<HangarItem?>(null) }
    var candidates by remember { mutableStateOf(emptyList<HangarItem>()) }
    var selected by remember { mutableStateOf<HangarItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val guard = remember { SafeMutationGuard() }
    var loadAttempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(repository, ownedCcuId, loadAttempt) {
        loading = true
        error = null
        selected = null
        candidates = repository.cachedUpgradeTargets(ownedCcuId)
        runCatching { repository.inventory() }.onSuccess { inventory ->
            upgrade = inventory.firstOrNull { it.id == ownedCcuId && it.isUpgrade }
            if (upgrade == null) error = "未找到该已拥有 CCU"
            else runCatching { repository.upgradeTargets(ownedCcuId) }
                .onSuccess { candidates = it }
                .onFailure { error = it.message ?: "无法获取可升级机库物品" }
        }.onFailure { error = it.message ?: "机库读取失败" }
        loading = false
    }
    PageGlassScope(
        backdrop = backdrop,
        content = {
            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(RefugeSpacing.page, RefugeSpacing.lg, RefugeSpacing.page, 142.dp),
                verticalArrangement = Arrangement.spacedBy(RefugeSpacing.md),
            ) {
                item {
                    ProductionHeader(palette, "应用已拥有 CCU", presence, avatarUrl, onAvatarClick) {
                        HeaderBack(backdrop, palette) { onNavigate(0) }
                    }
                }
                if (loading) item { ProductionLoadingState(backdrop, palette, "正在核对机库升级") }
                else if (error != null) item { ProductionErrorState(backdrop, palette, error!!, onRetry = { loadAttempt++ }) }
                else upgrade?.let { ccu ->
                    item { FlowPreview(backdrop, palette, translatedShipName(ccu.title, ccu.originalName), "${translatedShipName(ccu.upgradeFrom ?: "—")} → ${translatedShipName(ccu.upgradeTo ?: "—")}") }
                    item { FlowTitle(palette, "1", "请选择要升级的机库物品") }
                    if (candidates.isEmpty()) item { ProductionEmptyState(palette, "没有符合来源舰船的可用项") }
                    else items(candidates, key = { "candidate:${it.id}" }) { item ->
                        UpgradeChoice(
                            backdrop = backdrop,
                            palette = palette,
                            title = translatedShipName(item.title, item.originalName),
                            subtitle = item.price,
                            selected = selected?.id == item.id,
                            enabled = true,
                        ) { selected = item }
                    }
                    selected?.let { pledge ->
                        item { FlowPreview(backdrop, palette, "即将应用至", translatedShipName(pledge.title, pledge.originalName)) }
                        item {
                            ReferenceLiquidButton(backdrop, onClick = {
                                notice = guard.execute(DestructiveAction.APPLY_OWNED_CCU).message
                            }, modifier = Modifier.fillMaxWidth(), minHeight = 52.dp) {
                                Text("验证应用请求", style = RefugeTypography.body(palette).copy(color = palette.text))
                            }
                        }
                    }
                    notice?.let { message -> item { Text(message, style = RefugeTypography.caption(palette)) } }
                }
            }
        },
        overlay = { _ -> },
    )
}

@Composable
private fun HeaderBack(backdrop: LayerBackdrop, palette: RefugePalette, onClick: () -> Unit) {
    RefugeHeaderActionBar(
        backdrop = backdrop,
        palette = palette,
        actions = listOf(RefugeFloatingAction(RefugeIcons.back, "返回", onClick)),
    )
}

@Composable
private fun FlowTitle(palette: RefugePalette, step: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .clip(refugeContinuousShape(14.dp))
                .background(palette.accent.copy(alpha = .14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(step, style = RefugeTypography.caption(palette).copy(color = palette.accent))
        }
        Spacer(Modifier.width(RefugeSpacing.sm))
        Text(title, style = RefugeTypography.headline(palette))
    }
}

@Composable
private fun UpgradeChoice(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    title: String,
    subtitle: String,
    trailing: String? = null,
    originalTrailing: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    emphasized: Boolean = false,
    badge: String? = null,
    imageUrl: String? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember(title) { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val highlightAlpha by animateFloatAsState(
        targetValue = if (pressed && enabled) .14f else 0f,
        animationSpec = tween(120),
        label = "upgrade-choice-press",
    )
    val textColor = when {
        !enabled -> palette.textMuted.copy(alpha = .64f)
        emphasized -> UpgradeDiscountOrange
        selected -> palette.accent
        else -> palette.text
    }
    RefugeLiquidGlass(
        backdrop = backdrop,
        palette = palette,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .semantics {
                role = Role.Button
                contentDescription = title
                this.selected = selected
                if (!enabled) disabled()
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .then(if (selected) Modifier.border(1.dp, palette.accent.copy(alpha = .6f), refugeContinuousShape(RefugeRadius.control)) else Modifier),
        radius = RefugeRadius.control,
        surface = if (selected) palette.accentSoft else palette.glass,
        surfaceAlpha = if (selected) .24f else .10f,
        edgeAlpha = if (selected) .42f else .14f,
    ) {
        if (highlightAlpha > .001f) {
            Box(Modifier.matchParentSize().background(Color.White.copy(alpha = highlightAlpha)))
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.ship_placeholder),
                    error = painterResource(R.drawable.ship_placeholder),
                    modifier = Modifier.size(46.dp).clip(refugeContinuousShape(10.dp)),
                )
                Spacer(Modifier.width(10.dp))
            } else {
                Icon(
                    if (selected) RefugeIcons.success else RefugeIcons.hangarUpgrade,
                    contentDescription = null,
                    tint = if (enabled) textColor.copy(alpha = .92f) else palette.textMuted.copy(alpha = .55f),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = RefugeTypography.body(palette).copy(color = textColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = RefugeTypography.caption(palette).copy(
                            color = if (enabled) palette.textMuted else palette.textMuted.copy(alpha = .58f),
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (badge != null) {
                Box(
                    Modifier
                        .clip(refugeContinuousShape(9.dp))
                        .background(
                            if (enabled && emphasized) UpgradeDiscountOrange
                            else palette.glassStrong.copy(alpha = .16f),
                        )
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                ) {
                    Text(
                        badge,
                        style = RefugeTypography.caption(palette).copy(
                            color = if (enabled && emphasized) palette.background else palette.textMuted,
                        ),
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            if (trailing != null) {
                Column(horizontalAlignment = Alignment.End) {
                    originalTrailing?.let {
                        Text(
                            it,
                            style = RefugeTypography.caption(palette).copy(
                                color = palette.textMuted,
                                textDecoration = TextDecoration.LineThrough,
                            ),
                        )
                    }
                    Text(
                        trailing,
                        style = RefugeTypography.value(palette).copy(color = textColor),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowPreview(backdrop: LayerBackdrop, palette: RefugePalette, title: String, subtitle: String) {
    RefugeGlassControl(backdrop, palette, {}, Modifier.fillMaxWidth(), title, PaddingValues(14.dp)) {
        Column {
            Text(title, style = RefugeTypography.headline(palette))
            Text(subtitle, style = RefugeTypography.secondary(palette))
        }
    }
}

@Composable
private fun UpgradeCostPreview(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    source: UpgradeShipOption,
    target: UpgradeShipOption,
    sku: UpgradeSku,
    quote: UpgradePriceQuote?,
) {
    // The catalogue SKU price is the selected target edition's full price.
    // Subtracting the source MSRP yields the upgrade price; adding both values
    // would count the target ship twice and substantially overstate checkout.
    val standardDifference = (target.msrp - source.msrp).coerceAtLeast(0)
    val total = quote?.upgradePrice
    val savings = total?.let { (standardDifference - it).coerceAtLeast(0) } ?: 0
    RefugeStandardGlassSurface(
        backdrop = backdrop,
        palette = palette,
        modifier = Modifier.fillMaxWidth(),
        radius = RefugeRadius.panel,
        padding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("升级路径", style = RefugeTypography.caption(palette))
                    Text("${translatedShipName(source.name)} → ${translatedShipName(target.name)}", style = RefugeTypography.headline(palette))
                    Text(translatedShipName(sku.title), style = RefugeTypography.secondary(palette))
                }
                if (target.imageUrl != null) {
                    AsyncImage(
                        model = target.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.ship_placeholder),
                        error = painterResource(R.drawable.ship_placeholder),
                        modifier = Modifier.size(62.dp).clip(refugeContinuousShape(12.dp)),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.outline.copy(alpha = .28f)))
            UpgradeCostLine(palette, "目标版本", dollars(sku.price), emphasized = sku.isWarbond)
            UpgradeCostLine(palette, "来源舰船 MSRP", dollars(source.msrp))
            UpgradeCostLine(palette, "标准 MSRP 差额", dollars(standardDifference))
            if (savings > 0) {
                UpgradeCostLine(palette, "较标准差额节省", "-${dollars(savings)}", emphasized = true)
            }
            UpgradeCostLine(palette, "RSI 升级差价", total?.let(::dollars) ?: "待核验", emphasized = total != null)
        }
    }
}

@Composable
private fun UpgradeCostLine(palette: RefugePalette, label: String, value: String, emphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = RefugeTypography.secondary(palette), modifier = Modifier.weight(1f))
        Text(
            value,
            style = RefugeTypography.body(palette).copy(color = if (emphasized) UpgradeDiscountOrange else palette.text),
        )
    }
}

internal fun UpgradeShipOption.matchesOwnedShip(owned: com.refuge.next.data.OwnedShip): Boolean {
    if (owned.shipId != null) return id == owned.shipId
    fun alias(value: String) = com.refuge.next.data.CcuShip("", value, 0, 0).normalizedAlias()
    val ownedAlias = alias(owned.name)
    return ownedAlias.isNotBlank() && (ownedAlias == alias(originalName) || ownedAlias == alias(name))
}

private fun parseUpgradeCatalog(root: JSONObject, translation: com.refuge.next.data.TranslationRepository): List<UpgradeShipOption> {
    val ships = root.optJSONObject("data")?.optJSONArray("ships")
        ?: root.optJSONArray("ships")
        ?: return emptyList()
    return buildList {
        for (index in 0 until ships.length()) {
            val ship = ships.optJSONObject(index) ?: continue
            val id = ship.optInt("id", 0)
            val name = ship.optString("name")
            if (id <= 0 || name.isBlank()) continue
            val skuArray = ship.optJSONArray("skus")
            val skus = buildList {
                if (skuArray != null) for (skuIndex in 0 until skuArray.length()) {
                    val sku = skuArray.optJSONObject(skuIndex) ?: continue
                    add(UpgradeSku(
                        id = sku.optInt("id", 0),
                        title = sku.optString("title").ifBlank { name },
                        price = sku.optInt("price", 0),
                        body = sku.optString("body").trim(),
                        available = sku.optBoolean("available", false) &&
                            (!sku.has("availableStock") || sku.optBoolean("unlimitedStock", false) || sku.optInt("availableStock", 0) > 0),
                        originalTitle = sku.optString("title"),
                    ))
                }
            }
            add(
                UpgradeShipOption(
                    id = id,
                    name = name,
                    originalName = name,
                    msrp = ship.optInt("msrp", 0),
                    focus = ship.optString("focus").trim(),
                    manufacturer = ship.optJSONObject("manufacturer")?.optString("name").orEmpty().trim(),
                    imageUrl = normalizeUpgradeImageUrl(
                        ship.optJSONObject("medias")?.optString("productThumbMediumAndSmall")
                            ?.takeIf(String::isNotBlank)
                            ?: ship.optString("link").takeIf {
                                it.matches(Regex("(?i).+\\.(jpe?g|png|webp)(\\?.*)?$"))
                            },
                    ),
                    skus = skus,
                ),
            )
        }
    }.sortedBy { it.name }
}

private fun parseSourceIds(root: JSONObject): Set<Int> {
    val ships = root.optJSONObject("data")?.optJSONObject("from")?.optJSONArray("ships") ?: return emptySet()
    return buildSet { for (index in 0 until ships.length()) ships.optJSONObject(index)?.optInt("id", 0)?.takeIf { it > 0 }?.let(::add) }
}

private fun normalizeUpgradeImageUrl(raw: String?): String? {
    val value = raw?.trim()?.takeIf(String::isNotBlank) ?: return null
    return when {
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> "https://robertsspaceindustries.com$value"
        else -> value
    }
}

private fun dollars(cents: Int): String = if (cents % 100 == 0) {
    "\$${cents / 100}"
} else {
    String.format(Locale.US, "\$%.2f", cents / 100.0)
}

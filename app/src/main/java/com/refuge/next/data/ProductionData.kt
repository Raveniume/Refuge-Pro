package com.refuge.next.data

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.PriorityQueue

data class TerminalItem(
    val id: String,
    val name: String,
    val manufacturer: String,
    val category: TerminalCategory,
    val tags: List<String>,
    val value: String,
    val usd: String,
    val description: String,
    val imageUrl: String? = null,
    val details: List<Pair<String, String>> = emptyList(),
    val size: Int? = null,
    val sourceType: String? = null,
    val sourceSubType: String? = null,
    val performance: TerminalPerformance? = null,
    val ports: List<TerminalPort> = emptyList(),
    val sourceVersion: String? = null,
    val portTags: Set<String> = emptySet(),
    val imageUrls: List<String> = emptyList(),
    /** Source identifier for the legacy item_names lookup; name remains the English display name. */
    val className: String? = null,
    val filterAttributes: Map<String, String> = emptyMap(),
)

/** A read-only, local loadout assembled from the terminal snapshot. */
data class TerminalLoadoutStats(
    val massKg: Double? = null,
    val power: Double? = null,
    val shield: Double? = null,
    val damage: Double? = null,
    val cargo: Double? = null,
)

private fun terminalMetric(item: TerminalItem?, vararg labels: String): Double? {
    if (item == null) return null
    val row = item.details.firstOrNull { (label, _) ->
        labels.any { key -> label.contains(key, ignoreCase = true) }
    } ?: return null
    return Regex("[-+]?[0-9]+(?:[.,][0-9]+)?")
        .find(row.second.replace(",", ""))
        ?.value?.replace(',', '.')?.toDoubleOrNull()
}

fun calculateTerminalLoadoutStats(
    ship: TerminalItem?,
    components: Iterable<TerminalItem>,
): TerminalLoadoutStats {
    val all = listOfNotNull(ship) + components.toList()
    fun sum(vararg labels: String): Double? {
        val values = all.mapNotNull { terminalMetric(it, *labels) }
        return values.takeIf { it.isNotEmpty() }?.sum()
    }
    return TerminalLoadoutStats(
        massKg = sum("mass", "质量"),
        power = sum("power", "电力", "energy"),
        shield = sum("shield", "护盾"),
        damage = sum("damage", "伤害", "dps"),
        cargo = sum("cargo", "货舱", "货物"),
    )
}

enum class TerminalCategory(val label: String) {
    VEHICLES("载具"),
    SHIP_COMPONENTS("舰载"),
    PERSONAL("单兵"),
    ATTACHMENTS("配件"),
    SHIELDS("护盾"),
    COOLERS("冷却器"),
    POWER_PLANTS("发电机"),
    QUANTUM_DRIVES("量子"),
}

interface TerminalRepository {
    fun cachedItems(): List<TerminalItem> = emptyList()
    suspend fun awaitCachedItems(): List<TerminalItem> = cachedItems()
    /** Starts a refresh without making the current page wait for the network. */
    fun refreshInBackground() = Unit
    suspend fun items(): List<TerminalItem>
    /** Returns cached content immediately by default; live sources can enrich one row on demand. */
    suspend fun detail(item: TerminalItem): TerminalItem = item
}

class ProductionTerminalRepository(
    private val source: ProductionCacheDataSource? = null,
) : TerminalRepository {
    override fun cachedItems(): List<TerminalItem> = source?.peekTerminalItems().orEmpty()

    override suspend fun awaitCachedItems(): List<TerminalItem> = withContext(Dispatchers.IO) {
        source?.terminalItems().orEmpty()
    }

    override suspend fun items(): List<TerminalItem> = withContext(Dispatchers.IO) {
        source?.terminalItems().orEmpty()
    }
}

data class ProfileData(
    val handle: String = "RSI 账户",
    val displayName: String? = null,
    val city: String = "—",
    val rank: String = "—",
    val organizationId: String? = null,
    val organizationName: String? = null,
    val organizationRank: String? = null,
    val organizationLevel: Int = 0,
    val organizationImage: String? = null,
    val level: String = "—",
    // RSI account.status is not Spectrum presence. UI presence comes from UserStatusSource.
    val isOnline: Boolean? = null,
    val totalSpent: String = "—",
    val hangarValue: String = "—",
    val credit: String = "—",
    val registerDate: String = "—",
    val uec: String = "—",
    val rec: String = "—",
    val currentValue: String = "—",
    val referralCode: String = "—",
    val avatarUrl: String? = null,
    val email: String? = null,
    val username: String? = null,
    val hasGamePackage: Boolean? = null,
    val isAuthenticated: Boolean = false,
)

interface ProfileRepository {
    fun cachedProfile(): ProfileData = ProfileData()
    suspend fun awaitCachedProfile(): ProfileData = cachedProfile()
    suspend fun profile(): ProfileData
}

/** Read-only adapter for the imported account/cache contract. */
class ProductionProfileRepository(
    private val source: ProductionCacheDataSource? = null,
) : ProfileRepository {
    /** Synchronous callers must never parse the bundled JSON on the UI thread. */
    override fun cachedProfile(): ProfileData = source?.peekProfile() ?: ProfileData()
    override suspend fun awaitCachedProfile(): ProfileData = withContext(Dispatchers.IO) {
        source?.profile() ?: ProfileData()
    }
    override suspend fun profile(): ProfileData = withContext(Dispatchers.IO) {
        source?.profile() ?: ProfileData()
    }
}

data class ToolItem(val id: String, val title: String, val subtitle: String)

interface UtilityRepository {
    suspend fun groups(): List<Pair<String, List<ToolItem>>>
    suspend fun detail(toolId: String): ToolDetail = productionToolDetails[toolId] ?: ToolDetail(
        toolId = toolId,
        rows = listOf("状态" to "本地只读", "数据源" to "生产缓存 adapter"),
    )
    suspend fun query(toolId: String, input: String): ToolDetail = detail(toolId)
}

data class ToolDetail(
    val toolId: String,
    val rows: List<Pair<String, String>>,
    val externalUrl: String? = null,
)

class ProductionUtilityRepository(
    private val source: ProductionCacheDataSource? = null,
) : UtilityRepository {
    override suspend fun groups(): List<Pair<String, List<ToolItem>>> = withContext(Dispatchers.IO) {
        source?.toolGroups() ?: productionToolGroups
    }
    override suspend fun detail(toolId: String): ToolDetail = withContext(Dispatchers.IO) {
        source?.toolDetail(toolId) ?: productionToolDetails[toolId]
            ?: super@ProductionUtilityRepository.detail(toolId)
    }
}

interface CcuRepository {
    fun cachedShips(): List<CcuShip> = emptyList()
    fun cachedOwned(): List<OwnedCcu> = emptyList()
    suspend fun awaitCachedShips(): List<CcuShip> = cachedShips()
    suspend fun ships(): List<CcuShip>
    suspend fun owned(): List<OwnedCcu>
    suspend fun chain(owned: OwnedCcu): List<CcuChainStep> = listOf(
        CcuChainStep(owned.title.substringBefore(" → "), owned.appliedTo, owned.purchasePrice),
    )
}

/** Local CCU catalog boundary; the planner never calls the legacy paid service. */
class ProductionCcuRepository(
    private val source: ProductionCacheDataSource? = null,
    private val m80Image: Int = com.refuge.next.R.drawable.m80_hero,
    private val fallbackImage: Int = com.refuge.next.R.drawable.ship_placeholder,
) : CcuRepository {
    override fun cachedShips(): List<CcuShip> = source?.peekCcuShips(m80Image, fallbackImage).orEmpty()

    override fun cachedOwned(): List<OwnedCcu> = source?.peekOwnedCcu().orEmpty()

    override suspend fun awaitCachedShips(): List<CcuShip> = withContext(Dispatchers.IO) {
        source?.ccuShips(m80Image, fallbackImage).orEmpty()
    }

    override suspend fun ships(): List<CcuShip> = withContext(Dispatchers.IO) {
        source?.ccuShips(m80Image, fallbackImage).orEmpty()
    }

    override suspend fun owned(): List<OwnedCcu> = withContext(Dispatchers.IO) {
        source?.ownedCcu().orEmpty()
    }
}

/** Read-only cache boundary for the store's authenticated CCU purchase flow. */
interface CcuPurchaseRepository {
    suspend fun quote(sourceShipId: Int, targetShipId: Int, skuId: Int): UpgradePriceQuote = error("此数据源不支持实时 CCU 报价")
    fun cachedCatalog(): JSONObject? = null
    suspend fun awaitCachedCatalog(): JSONObject? = cachedCatalog()
    fun cachedSourceIds(toId: Int): Set<Int> = emptySet()
    suspend fun catalog(): JSONObject?
    suspend fun sourceIds(toId: Int): Set<Int>
}

data class CurrentCcuSkuPrice(
    val price: Int,
    val available: Boolean,
    val unlimitedStock: Boolean = false,
    val availableStock: Int = 0,
)

data class CurrentCcuShipPricing(val msrp: Int, val skus: List<CurrentCcuSkuPrice>)

/** Count target ships with a currently available SKU below the target MSRP. */
internal fun countCurrentCcuDiscountShips(pricing: List<CurrentCcuShipPricing>): Int =
    pricing.count { ship ->
        ship.msrp > 0 && ship.skus.any { sku ->
            sku.available && (sku.unlimitedStock || sku.availableStock > 0) &&
                sku.price > 0 && sku.price < ship.msrp
        }
    }

/** JSON adapter used by the authenticated store screen. */
fun countCurrentCcuDiscountShips(root: org.json.JSONObject): Int {
    val ships = root.optJSONObject("data")?.optJSONArray("ships") ?: root.optJSONArray("ships") ?: return 0
    val pricing = buildList {
      for (index in 0 until ships.length()) {
        val ship = ships.optJSONObject(index) ?: continue
        val msrp = ship.optInt("msrp", 0)
        val skus = ship.optJSONArray("skus") ?: continue
        add(CurrentCcuShipPricing(msrp, buildList {
          for (skuIndex in 0 until skus.length()) {
                val sku = skus.optJSONObject(skuIndex) ?: continue
                val price = sku.optInt("price", 0)
                add(CurrentCcuSkuPrice(price, sku.optBoolean("available", false), sku.optBoolean("unlimitedStock", false), sku.optInt("availableStock", 0)))
          }
        }))
      }
    }
    return countCurrentCcuDiscountShips(pricing)
}

data class CcuShip(
    val id: String,
    val name: String,
    val purchasePrice: Int,
    val imageRes: Int,
    val owned: Boolean = false,
    val paidPrice: Int? = null,
    val originalPrice: Int = purchasePrice,
)

data class OwnedCcu(
    val id: String,
    val title: String,
    val purchasePrice: Int,
    val appliedTo: String,
    val fromShip: String = title.substringBefore(" → ").substringBefore(" to ").trim(),
    val toShip: String = appliedTo,
)

data class CcuChainStep(
    val from: String,
    val to: String,
    val purchasePrice: Int,
)

private val productionToolGroups: List<Pair<String, List<ToolItem>>> = listOf(
    "账户与查询" to listOf(
        ToolItem("crowdfunding", "众筹查询", "查看项目进度和支持统计"),
        ToolItem("player-search", "玩家搜索", "通过 Handle 查询公开资料"),
        ToolItem("social", "社交", "组织、邀请与玩家关系"),
        ToolItem("gift-redeem", "礼物兑换", "兑换待领取的礼物码与礼包"),
    ),
    "资料中心" to listOf(
        ToolItem("ships", "舰船一览", "浏览舰船与制造商信息"),
        ToolItem("equipment", "装备资料", "查找护盾、武器和配件"),
        ToolItem("referrals", "邀请查询", "查看邀请状态和奖励"),
        ToolItem("referral-reverse", "邀请反查", "通过邀请人或被邀请人反查关系"),
    ),
    "RSI 快捷入口" to listOf(
        ToolItem("web-hangar", "网页机库", "打开 RSI 网页机库"),
        ToolItem("web-buyback", "网页回购", "打开 RSI 网页回购"),
        ToolItem("my-fleet", "我的舰队", "打开组织舰队页面"),
        ToolItem("referral-program", "邀请计划", "打开 RSI 邀请计划"),
        ToolItem("spectrum", "光谱论坛", "打开 Spectrum 社区"),
        ToolItem("service-center", "服务中心", "打开 RSI 服务中心"),
        ToolItem("roadmap", "路线图", "打开 RSI 路线图"),
        ToolItem("service-status", "服务状态", "打开 RSI 服务状态"),
    ),
)

private val productionToolDetails: Map<String, ToolDetail> = mapOf(
    "crowdfunding" to ToolDetail("crowdfunding", listOf("状态" to "等待在线数据")),
    "player-search" to ToolDetail("player-search", listOf("查询范围" to "公开 Handle", "状态" to "输入 Handle 后查询")),
    "social" to ToolDetail("social", listOf("数据源" to "RSI Spectrum", "状态" to "在线入口"), "https://robertsspaceindustries.com/spectrum/community/SC"),
    "gift-redeem" to ToolDetail("gift-redeem", listOf("状态" to "通过 RSI 账户页面处理", "账户变更" to "不会自动执行"), "https://robertsspaceindustries.com/account/pledges"),
    "ships" to ToolDetail("ships", listOf("资料分类" to "舰船", "入口" to "终端")),
    "equipment" to ToolDetail("equipment", listOf("资料分类" to "装备、护盾、武器", "入口" to "终端")),
    "referrals" to ToolDetail("referrals", listOf("数据源" to "RSI 邀请计划", "状态" to "在线入口"), "https://robertsspaceindustries.com/referral-program"),
    "referral-reverse" to ToolDetail("referral-reverse", listOf("操作" to "在注册页输入邀请码查询目标用户", "模式" to "匿名只读入口"), "https://robertsspaceindustries.com/en/enlist"),
    "rsi" to ToolDetail("rsi", listOf("入口" to "RSI 资料", "外部跳转" to "未启用", "账户变更" to "不会执行")),
    "web-hangar" to ToolDetail("web-hangar", listOf("入口" to "RSI 网页机库", "账户变更" to "不会执行"), "https://robertsspaceindustries.com/account/pledges"),
    "web-buyback" to ToolDetail("web-buyback", listOf("入口" to "RSI 网页回购", "账户变更" to "不会执行"), "https://robertsspaceindustries.com/account/buy-back-pledges"),
    "my-fleet" to ToolDetail("my-fleet", listOf("入口" to "RSI 组织舰队", "账户变更" to "不会执行"), "https://robertsspaceindustries.com/account/organization"),
    "referral-program" to ToolDetail("referral-program", listOf("入口" to "RSI 邀请计划", "账户变更" to "不会执行"), "https://robertsspaceindustries.com/referral-program"),
    "spectrum" to ToolDetail("spectrum", listOf("入口" to "Spectrum 社区", "账户变更" to "不会执行"), "https://robertsspaceindustries.com/spectrum/community/SC"),
    "service-center" to ToolDetail("service-center", listOf("入口" to "RSI 服务中心", "账户变更" to "不会执行"), "https://support.robertsspaceindustries.com/"),
    "roadmap" to ToolDetail("roadmap", listOf("入口" to "RSI 路线图", "账户变更" to "不会执行"), "https://robertsspaceindustries.com/roadmap/progress-tracker/teams/info"),
    "service-status" to ToolDetail("service-status", listOf("入口" to "RSI 服务状态", "账户变更" to "不会执行"), "https://status.robertsspaceindustries.com/"),
)

fun eligibleTargetShips(seed: CcuShip, ships: Iterable<CcuShip>): List<CcuShip> =
    ships.filter { it.purchasePrice > seed.purchasePrice }

data class CcuRouteStep(
    val from: CcuShip,
    val to: CcuShip,
    val purchasePrice: Int,
    val owned: OwnedCcu? = null,
)

data class CcuRoutePlan(
    val seed: CcuShip,
    val target: CcuShip,
    val steps: List<CcuRouteStep>,
) {
    val selectedOwned: List<OwnedCcu>
        get() = steps.mapNotNull { it.owned }
    val ownedPurchasePrice: Int
        get() = selectedOwned.sumOf { it.purchasePrice }
    val remainingPayment: Int
        get() = steps.filter { it.owned == null }.sumOf { it.purchasePrice }
    val standardPayment: Int
        get() = (target.purchasePrice - seed.purchasePrice).coerceAtLeast(0)
    val maximumSavings: Int
        get() = (standardPayment - remainingPayment).coerceAtLeast(0)
    val shipValue: Int
        get() = calculateShipValue(seed.paidPrice ?: seed.purchasePrice, selectedOwned.map { it.purchasePrice }, remainingPayment)
}

private data class PlannerEdge(
    val from: CcuShip,
    val to: CcuShip,
    val purchasePrice: Int,
    val remainingSpend: Int,
    val owned: OwnedCcu? = null,
)

private data class PlannerScore(val remainingSpend: Int, val purchaseCost: Int, val steps: Int)

private data class PlannerQueueEntry(val shipId: String, val score: PlannerScore)

private fun comparePlannerScore(left: PlannerScore, right: PlannerScore): Int =
    compareValuesBy(left, right, PlannerScore::remainingSpend, PlannerScore::purchaseCost, PlannerScore::steps)

private fun normalizeCcuShipName(value: String): String = value
    .lowercase()
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()

/**
 * Offline remaining-spend planner. Normal upgrades cost the MSRP delta while
 * an owned CCU costs zero now; its purchase price is retained only for the
 * final paid-value calculation. Unrelated inventory never affects the route.
 */
enum class CcuOptimization { TOTAL_COST, NEW_SPEND }

fun planCcuRoute(
    seed: CcuShip,
    target: CcuShip,
    ships: Iterable<CcuShip>,
    owned: Iterable<OwnedCcu>,
    optimization: CcuOptimization = CcuOptimization.NEW_SPEND,
): CcuRoutePlan? {
    if (target.purchasePrice <= seed.purchasePrice) return null
    fun compare(left: PlannerScore, right: PlannerScore): Int = if (optimization == CcuOptimization.TOTAL_COST)
        compareValuesBy(left, right, PlannerScore::purchaseCost, PlannerScore::remainingSpend, PlannerScore::steps)
    else comparePlannerScore(left, right)
    val orderedShips = (ships + listOf(seed, target))
        .distinctBy { it.id }
        .filter { it.purchasePrice > 0 }
        .sortedWith(compareBy<CcuShip> { it.purchasePrice }.thenBy { it.id })
    val shipById = orderedShips.associateBy { it.id }
    val shipByName = orderedShips.associateBy { normalizeCcuShipName(it.name) }
    val ownedEdges = owned.mapNotNull { ccu ->
        val from = shipByName[normalizeCcuShipName(ccu.fromShip)] ?: return@mapNotNull null
        val to = shipByName[normalizeCcuShipName(ccu.toShip)] ?: return@mapNotNull null
        if (to.purchasePrice <= from.purchasePrice) return@mapNotNull null
        PlannerEdge(from, to, ccu.purchasePrice.coerceAtLeast(0), 0, ccu)
    }.groupBy { it.from.id }

    val startScore = PlannerScore(0, 0, 0)
    val scores = mutableMapOf(seed.id to startScore)
    val previous = mutableMapOf<String, PlannerEdge>()
    val queue = PriorityQueue<PlannerQueueEntry> { left, right -> compare(left.score, right.score) }
    queue += PlannerQueueEntry(seed.id, startScore)

    while (queue.isNotEmpty()) {
        val current = queue.remove()
        val accepted = scores[current.shipId] ?: continue
        if (comparePlannerScore(current.score, accepted) != 0) continue
        if (current.shipId == target.id) break
        val from = shipById[current.shipId] ?: continue
        val normalEdges = orderedShips.asSequence()
            .filter { it.purchasePrice > from.purchasePrice && it.purchasePrice <= target.purchasePrice }
            .map { to ->
                val delta = to.purchasePrice - from.purchasePrice
                PlannerEdge(from, to, delta, delta)
            }
        val candidates = normalEdges + ownedEdges[from.id].orEmpty().asSequence()
            .filter { it.to.purchasePrice <= target.purchasePrice }
        candidates.forEach { edge ->
            val next = PlannerScore(
                remainingSpend = accepted.remainingSpend + edge.remainingSpend,
                purchaseCost = accepted.purchaseCost + edge.purchasePrice,
                steps = accepted.steps + 1,
            )
            val existing = scores[edge.to.id]
            if (existing == null || compare(next, existing) < 0) {
                scores[edge.to.id] = next
                previous[edge.to.id] = edge
                queue += PlannerQueueEntry(edge.to.id, next)
            }
        }
    }

    if (target.id !in scores) return null
    val steps = mutableListOf<CcuRouteStep>()
    var cursor = target.id
    while (cursor != seed.id) {
        val edge = previous[cursor] ?: return null
        steps += CcuRouteStep(edge.from, edge.to, edge.purchasePrice, edge.owned)
        cursor = edge.from.id
    }
    return CcuRoutePlan(seed, target, steps.asReversed())
}

fun calculateRemainingPayment(
    seed: CcuShip,
    target: CcuShip,
    ships: Iterable<CcuShip>,
    owned: Iterable<OwnedCcu>,
): Int = planCcuRoute(seed, target, ships, owned)?.remainingPayment
    ?: (target.purchasePrice - seed.purchasePrice).coerceAtLeast(0)

fun calculateRemainingPayment(seed: CcuShip, target: CcuShip, owned: Iterable<OwnedCcu>): Int =
    calculateRemainingPayment(seed, target, listOf(seed, target), owned)

/** Business value uses actual purchase prices, never MSRP or credited delta values. */
fun calculateShipValue(
    seedPurchasePrice: Int,
    ownedCcuPurchasePrices: Iterable<Int>,
    remainingPayment: Int,
): Int = seedPurchasePrice + ownedCcuPurchasePrices.sum() + remainingPayment

fun formatUsd(cents: Int): String = "$${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

enum class DestructiveAction {
    GIFT,
    RECALL,
    RECLAIM,
    UPGRADE_PURCHASE,
    APPLY_OWNED_CCU,
    CART_ADD,
    CART_CHECKOUT,
    RSI_PURCHASE,
}

data class SafeActionResult(
    val action: DestructiveAction,
    val executed: Boolean,
    val message: String,
)

interface DestructiveActionExecutor {
    fun execute(action: DestructiveAction): SafeActionResult
}

/** Debug/runtime QA boundary: never submits account or purchase mutations. */
class SafeMutationGuard : DestructiveActionExecutor {
    fun execute(request: PledgeActionRequest): SafeActionResult = execute(request.action)

    override fun execute(action: DestructiveAction): SafeActionResult {
        val message = "SafeMutationGuard blocked ${action.name}; no remote mutation was sent"
        // android.util.Log is a platform stub in local JVM tests. The guard's
        // safety contract must not depend on logging being available.
        runCatching { Log.i("SafeMutationGuard", message) }
        return SafeActionResult(action = action, executed = false, message = message)
    }
}

typealias SafeNoOpDestructiveActionExecutor = SafeMutationGuard

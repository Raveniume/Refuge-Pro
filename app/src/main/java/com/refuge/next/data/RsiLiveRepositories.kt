package com.refuge.next.data

import android.content.Context
import android.text.Html
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Live, read-only source for the local planner. It shares RSI's public upgrade
 * catalogue and the already parsed owned-CCU inventory, but never invokes a
 * paid planner or a mutation endpoint. */
class RsiLiveCcuRepository(
    context: Context,
    private val auth: RsiAuthDataSource,
    private val hangar: HangarRepository,
    private val fallback: CcuRepository,
    private val fallbackImage: Int,
) : CcuRepository {
    private val shipSnapshotFile = File(context.filesDir, "ccu_ships.json")
    @Volatile private var cachedShips: List<CcuShip> = emptyList()
    private val snapshotLoad = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
        readShipSnapshot().also { snapshot ->
            if (snapshot.isNotEmpty()) cachedShips = snapshot
        }
    }
    private val cachedOwned = ConcurrentHashMap<String, List<OwnedCcu>>()
    private val shipRefresh = RepositoryRefresh<List<CcuShip>>(minimumIntervalMillis = 1_000L)
    private val ownedRefresh = RepositoryRefresh<List<OwnedCcu>>(minimumIntervalMillis = 1_000L)

    override fun cachedShips(): List<CcuShip> = cachedShips.ifEmpty { fallback.cachedShips() }

    override suspend fun awaitCachedShips(): List<CcuShip> {
        snapshotLoad.await()
        return cachedShips.ifEmpty { fallback.awaitCachedShips() }
    }

    override fun cachedOwned(): List<OwnedCcu> {
        val key = auth.currentAccountSnapshotKey() ?: return emptyList()
        return cachedOwned[key]
            ?: ownedFrom(hangar.cachedInventory()).takeIf { it.isNotEmpty() }?.also { cachedOwned[key] = it }
            ?: emptyList()
    }

    override suspend fun ships(): List<CcuShip> {
        snapshotLoad.await()
        return shipRefresh.await("public") {
            withContext(Dispatchers.IO) {
                runCatching {
                    val rows = auth.shipUpgradeCatalog().optJSONObject("data")?.optJSONArray("ships")
                        ?: error("RSI upgrade ship catalogue is unavailable")
                    buildList {
                        for (index in 0 until rows.length()) {
                            val ship = rows.optJSONObject(index) ?: continue
                            val id = ship.optInt("id", 0)
                            val name = ship.optString("name").trim()
                            val msrp = ship.optInt("msrp", 0)
                            if (id > 0 && name.isNotBlank() && msrp > 0) {
                                add(CcuShip(id.toString(), name, msrp, fallbackImage))
                            }
                        }
                    }.also {
                        if (it.isEmpty()) error("RSI upgrade ship catalogue is empty")
                        cachedShips = it
                        writeShipSnapshot(it)
                        Log.i("RefugeCcuAudit", "catalog=${it.size} source=RSI initShipUpgrade")
                    }
                }.getOrElse { error ->
                    Log.w("RefugeCcuAudit", "catalog refresh failed; retaining last successful data", error)
                    cachedShips()
                }
            }
        }
    }

    override suspend fun owned(): List<OwnedCcu> {
        val accountKey = auth.currentAccountSnapshotKey() ?: return emptyList()
        val result = ownedRefresh.await(accountKey) {
            withContext(Dispatchers.IO) { runCatching {
        val inventory = hangar.inventory()
        if (auth.currentAccountSnapshotKey() != accountKey) {
            return@runCatching cachedOwned[accountKey].orEmpty()
        }
        ownedFrom(inventory).also {
            cachedOwned[accountKey] = it
            Log.i("RefugeCcuAudit", "owned=${it.size} source=RSI hangar pledges")
        }
    }.getOrElse { error ->
        Log.w("RefugeCcuAudit", "owned CCU refresh failed; retaining last successful data", error)
        cachedOwned[accountKey].orEmpty()
    } }
        }
        return if (auth.currentAccountSnapshotKey() == accountKey) result else cachedOwned()
    }

    override suspend fun chain(owned: OwnedCcu): List<CcuChainStep> = withContext(Dispatchers.IO) {
        val item = hangar.inventory().firstOrNull { it.id.toString() == owned.id }
        val from = item?.upgradeFrom ?: owned.title.substringBefore(" → ").substringBefore(" to ")
        val to = item?.upgradeTo ?: owned.appliedTo
        listOf(CcuChainStep(from.ifBlank { "—" }, to.ifBlank { "—" }, owned.purchasePrice))
    }

    private fun parseUsdCents(value: String): Int = Regex("[0-9]+(?:\\.[0-9]+)?")
        .find(value.replace(",", ""))?.value?.toBigDecimalOrNull()
        ?.movePointRight(2)?.toInt() ?: 0

    private fun ownedFrom(items: List<HangarItem>): List<OwnedCcu> = items.filter { it.isUpgrade }.map { item ->
        OwnedCcu(
            id = item.id.toString(),
            title = item.title,
            purchasePrice = parseUsdCents(item.price),
            appliedTo = item.upgradeTo ?: "—",
            fromShip = item.upgradeFrom ?: item.title.substringBefore(" → ").substringBefore(" to ").trim(),
            toShip = item.upgradeTo ?: "—",
        )
    }

    private fun readShipSnapshot(): List<CcuShip> = runCatching {
        if (!shipSnapshotFile.isFile) return@runCatching emptyList()
        val rows = JSONArray(shipSnapshotFile.readText())
        buildList {
            for (index in 0 until rows.length()) rows.optJSONObject(index)?.let { row ->
                val id = row.optString("id")
                val name = row.optString("name")
                val price = row.optInt("purchasePrice")
                if (id.isNotBlank() && name.isNotBlank() && price > 0) add(CcuShip(id, name, price, fallbackImage))
            }
        }
    }.onFailure { Log.w("RefugeCcuAudit", "catalog snapshot read failed", it) }.getOrDefault(emptyList())

    private fun writeShipSnapshot(ships: List<CcuShip>) {
        runCatching {
            val rows = JSONArray()
            ships.forEach { ship ->
                rows.put(JSONObject().put("id", ship.id).put("name", ship.name).put("purchasePrice", ship.purchasePrice))
            }
            shipSnapshotFile.writeText(rows.toString())
        }.onFailure { Log.w("RefugeCcuAudit", "catalog snapshot write failed", it) }
    }
}

/**
 * Cache-first source for the Store "purchase upgrade" sheet. The catalogue is
 * kept as the original read-only JSON because the purchase screen needs SKU
 * availability and prices that the local planner intentionally does not own.
 */
class RsiLiveCcuPurchaseRepository(
    context: Context,
    private val auth: RsiAuthDataSource,
    private val fallback: CcuRepository? = null,
) : CcuPurchaseRepository {
    private val catalogFile = File(context.filesDir, "ccu_purchase_current_v1.json")
    private val sourceCache = ConcurrentHashMap<Int, Set<Int>>()
    @Volatile private var catalogRaw: String? = null
    @Volatile private var catalogJson: JSONObject? = null
    private val snapshotLoad = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
        readCatalog().also { raw ->
            if (!raw.isNullOrBlank()) {
                catalogRaw = raw
                catalogJson = runCatching { JSONObject(raw) }.getOrNull()?.takeIf(::hasShips)
            }
        }
    }
    private val catalogRefresh = RepositoryRefresh<JSONObject?>(minimumIntervalMillis = 1_000L)
    private val sourceRefresh = RepositoryRefresh<Set<Int>>(minimumIntervalMillis = 1_000L)

    /** Non-blocking first-frame view. JSON is parsed once by snapshot/network
     * work on Dispatchers.IO; synchronous Compose getters never reparse it. */
    override fun cachedCatalog(): JSONObject? = catalogJson ?: fallback?.cachedShips()?.takeIf { it.isNotEmpty() }?.let(::localCatalog)

    override suspend fun awaitCachedCatalog(): JSONObject? {
        snapshotLoad.await()
        return catalogJson ?: fallback?.awaitCachedShips()?.takeIf { it.isNotEmpty() }?.let(::localCatalog)
    }

    override fun cachedSourceIds(toId: Int): Set<Int> = sourceCache[toId].orEmpty()

    override suspend fun catalog(): JSONObject? {
        snapshotLoad.await()
        return catalogRefresh.await("public") {
            withContext(Dispatchers.IO) {
                runCatching {
                    currentUpgradeCatalog(auth.shipUpgradeCatalog(), auth.filterShipUpgrades()).also { response ->
                        if (!hasShips(response)) error("RSI 升级目录为空")
                        catalogRaw = response.toString()
                        catalogJson = response
                        writeCatalog(catalogRaw!!)
                    }
                }.getOrElse { error ->
                    Log.w("RefugeCcuPurchase", "catalog refresh failed; retaining cache", error)
                    catalogJson ?: fallback?.awaitCachedShips()?.takeIf { it.isNotEmpty() }?.let(::localCatalog)
                }
            }
        }
    }

    override suspend fun sourceIds(toId: Int): Set<Int> = sourceRefresh.await(toId.toString()) { withContext(Dispatchers.IO) {
        runCatching {
            val response = auth.filterShipUpgrades(toId = toId)
            val ships = response.optJSONObject("data")?.optJSONObject("from")?.optJSONArray("ships")
                ?: error("RSI 未返回可升级来源")
            buildSet {
                if (ships != null) for (index in 0 until ships.length()) {
                    ships.optJSONObject(index)?.optInt("id", 0)?.takeIf { it > 0 }?.let(::add)
                }
            }.also { sourceCache[toId] = it }
        }.getOrElse { error ->
            Log.w("RefugeCcuPurchase", "source refresh failed; retaining cache", error)
            sourceCache[toId] ?: (fallback?.awaitCachedShips().orEmpty().mapNotNull { it.id.toIntOrNull() }.toSet()).also { sourceCache[toId] = it }
        }
    } }

    /** Build the same small shape consumed by the selector when RSI is offline.
     * The bundled CCU ship list remains actionable instead of leaving the
     * upgrade flow stuck behind a network-only loading state. */
    private fun localCatalog(ships: List<CcuShip>): JSONObject {
        val rows = org.json.JSONArray()
        ships.forEach { ship ->
            val id = ship.id.toIntOrNull() ?: return@forEach
            val sku = org.json.JSONObject()
                .put("id", id)
                .put("title", ship.name)
                .put("body", "")
                .put("price", ship.purchasePrice)
                .put("available", true)
                .put("unlimitedStock", true)
            rows.put(org.json.JSONObject()
                .put("id", id)
                .put("name", ship.name)
                .put("msrp", ship.purchasePrice)
                .put("focus", "")
                .put("skus", org.json.JSONArray().put(sku)))
        }
        return org.json.JSONObject().put("data", org.json.JSONObject().put("ships", rows))
    }

    private fun readCatalog(): String? = runCatching {
        catalogFile.takeIf(File::isFile)?.readText()?.takeIf(String::isNotBlank)
    }.onFailure { Log.w("RefugeCcuPurchase", "catalog cache read failed", it) }
        .getOrNull()
        ?.takeIf { runCatching { hasShips(JSONObject(it)) }.getOrDefault(false) }

    override suspend fun quote(sourceShipId: Int, targetShipId: Int, skuId: Int): UpgradePriceQuote {
        val response = auth.filterShipUpgrades(fromId = sourceShipId, toId = targetShipId)
        val ships = response.optJSONObject("data")?.optJSONObject("to")?.optJSONArray("ships")
            ?: error("RSI 未返回升级报价")
        for (index in 0 until ships.length()) {
            val target = ships.optJSONObject(index) ?: continue
            if (target.optInt("id") != targetShipId) continue
            val skus = target.optJSONArray("skus") ?: continue
            for (skuIndex in 0 until skus.length()) {
                val sku = skus.optJSONObject(skuIndex) ?: continue
                if (sku.optInt("id") != skuId) continue
                check(sku.optBoolean("available", false) &&
                    (sku.optBoolean("unlimitedStock", false) || sku.optInt("availableStock", 0) > 0)) { "该版本当前不可售" }
                val price = sku.opt("upgradePrice") as? Number ?: error("RSI 未返回升级差价")
                check(price.toDouble() > 0 && price.toDouble() == price.toInt().toDouble()) { "RSI 升级差价无效" }
                return UpgradePriceQuote(sourceShipId, targetShipId, skuId, price.toInt())
            }
        }
        error("所选升级路径当前不可用")
    }

    private fun hasShips(root: JSONObject): Boolean = root.optJSONObject("data")?.optJSONArray("ships")?.length()?.let { it > 0 }
        ?: root.optJSONArray("ships")?.length()?.let { it > 0 }
        ?: false

    private fun writeCatalog(raw: String) {
        runCatching { catalogFile.writeText(raw) }
            .onFailure { Log.w("RefugeCcuPurchase", "catalog cache write failed", it) }
    }
}

/** Online-first adapter for the same account/pledge endpoint used by RefugeNext. */
class RsiLiveHangarRepository(
    context: Context,
    private val auth: RsiAuthDataSource,
    private val fallback: HangarRepository,
    private val fallbackImage: Int,
    private val m80Image: Int,
    private val translation: TranslationRepository,
) : HangarRepository {
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val snapshots = AccountSnapshotCache(
        context.filesDir,
        "hangar_items",
        ::readHangarSnapshot,
        List<HangarItem>::isNotEmpty,
    )
    private val refresh = RepositoryRefresh<List<HangarItem>>(minimumIntervalMillis = 1_000L)
    @Volatile private var shipValues: Map<String, Int>? = null
    @Volatile private var shipIds: Map<String, Int> = emptyMap()
    @Volatile private var shipImages: Map<String, String> = emptyMap()
    private val upgradeTargetIds = ConcurrentHashMap<Pair<String, Long>, Set<Long>>()
    private val featuredAuditLogged = java.util.concurrent.atomic.AtomicBoolean(false)
    private val reclaimMutex = Mutex()
    private val inventoryEpoch = java.util.concurrent.atomic.AtomicLong(0)

    init {
        auth.currentAccountSnapshotKey()?.let { snapshots.preload(it, preloadScope) }
    }

    override fun cachedOwnedShips(): List<OwnedShip> {
        val snapshot = currentAccountInventory(memoryOnly = true)
        // The first composition must never parse the account JSON on the
        // Compose thread. `init` preloads this snapshot on IO; until it is
        // ready, the bundled projection supplies the immediate frame.
        val hydrated = snapshot
        return if (hydrated != null) resolveOwnedShips(hydrated) else fallback.cachedOwnedShips()
    }

    override fun cachedInventory(): List<HangarItem> {
        val accountKey = auth.currentAccountSnapshotKey()
        val snapshot = currentAccountInventory(memoryOnly = true)
            // Disk hydration is performed by the repository preload job. A
            // synchronous fallback here would block first draw on a large
            // hangar snapshot and can trigger an Android input ANR.
        return snapshot ?: fallback.cachedInventory()
    }

    override suspend fun awaitCachedOwnedShips(): List<OwnedShip> {
        val snapshot = awaitCurrentAccountInventory()
        return if (snapshot != null) resolveOwnedShips(snapshot) else fallback.awaitCachedOwnedShips()
    }

    override suspend fun awaitCachedInventory(): List<HangarItem> = awaitCurrentAccountInventory()
        ?: fallback.awaitCachedInventory()

    override suspend fun ownedShips(): List<OwnedShip> {
        val items = inventory()
        return resolveOwnedShips(items)
    }

    override fun cachedUpgradeTargets(upgradeId: Long): List<HangarItem> {
        val account = auth.currentAccountSnapshotKey() ?: return emptyList()
        val ids = upgradeTargetIds[account to upgradeId] ?: return emptyList()
        return cachedInventory().filter { it.id in ids }
    }

    override suspend fun upgradeTargets(upgradeId: Long): List<HangarItem> = withContext(Dispatchers.IO) {
        require(upgradeId > 0)
        val account = auth.currentAccountSnapshotKey() ?: error("请先登录 RSI")
        val response = auth.accountPost("api/account/chooseUpgradeTarget", JSONObject().put("upgrade_id", upgradeId.toString()))
        check(response.optInt("success") == 1) {
            response.optString("msg").ifBlank { "无法获取可升级机库物品" }
        }
        val ids = parseUpgradeTargetIds(response.optJSONObject("data")?.optString("rendered").orEmpty())
        upgradeTargetIds[account to upgradeId] = ids
        inventory().filter { it.id in ids }
    }

    private suspend fun enrichShipValues(items: List<HangarItem>): List<HangarItem> {
        val values = shipValues ?: runCatching {
            val ships = auth.shipUpgradeCatalog().optJSONObject("data")?.optJSONArray("ships") ?: error("升级舰船目录为空")
            shipImages = buildMap {
                for (index in 0 until ships.length()) {
                    val ship = ships.optJSONObject(index) ?: continue
                    val image = usableHangarImage(ship.optJSONObject("medias")?.optString("productThumbMediumAndSmall"))
                    if (image != null) put(normalizeShipName(ship.optString("name")), rsiAssetUrl(image))
                }
            }
            shipIds = buildMap {
                for (index in 0 until ships.length()) {
                    val ship = ships.optJSONObject(index) ?: continue
                    val id = ship.optInt("id")
                    val name = ship.optString("name")
                    if (id > 0 && name.isNotBlank()) put(normalizeShipName(name), id)
                }
            }
            buildMap {
                for (index in 0 until ships.length()) {
                    val ship = ships.optJSONObject(index) ?: continue
                    val name = ship.optString("name").trim()
                    val msrp = ship.optInt("msrp", 0)
                    if (name.isNotBlank() && msrp > 0) put(normalizeShipName(name), msrp)
                }
            }
        }.onFailure { Log.w("RefugeHangarAudit", "ship MSRP refresh failed", it) }
            .getOrDefault(emptyMap()).also { shipValues = it }
        if (values.isEmpty()) return items
        return items.map { item ->
            val resolvedValue = item.resolvedFinalShip?.let { values[normalizeShipName(it)] }
            val fromValue = item.upgradeFrom?.let { values[normalizeShipName(it)] }
            val toValue = item.upgradeTo?.let { values[normalizeShipName(it)] }
            val marketCents = when {
                item.isUpgrade && fromValue != null && toValue != null -> (toValue - fromValue).coerceAtLeast(0)
                !item.isUpgrade && resolvedValue != null -> resolvedValue
                else -> null
            }
            val market = marketCents?.let(::formatCents) ?: item.currentValue
            item.copy(
                shipId = item.resolvedFinalShip?.let { shipIds[normalizeShipName(it)] } ?: item.shipId,
                imageUrl = if (item.isUpgrade) item.displayImageUrl
                    ?: item.upgradeTo?.let { shipImages[normalizeShipName(it)] } else item.imageUrl,
                currentValue = market,
                savings = usdDifference(market, item.price),
                upgradeFromPrice = fromValue?.let(::formatCents),
                upgradeToPrice = toValue?.let(::formatCents),
                includedEntries = item.includedEntries.map { entry ->
                    val childValue = if (entry.kind.equals("ship", true) || entry.kind == "舰船") values[normalizeShipName(entry.title)] else null
                    if (childValue == null) entry else entry.copy(value = formatCents(childValue))
                },
            )
        }
    }

    private fun normalizeShipName(value: String): String = value.lowercase(Locale.US)
        .replace(Regex("(?i)\\b(origin|rsi|aegis|anvil|drake|crusader|argo|banu|cnou|esperia)\\b"), "")
        .replace("exploration module", "explorer")
        .replace(Regex("\\s+"), " ").trim()

    private fun formatCents(value: Int): String = if (value % 100 == 0) "$${value / 100}" else String.format(Locale.US, "$%.2f", value / 100.0)

    private fun usdDifference(current: String, paid: String): String {
        fun amount(value: String) = Regex("[0-9]+(?:\\.[0-9]+)?").find(value.replace(",", ""))?.value?.toDoubleOrNull()
        val currentAmount = amount(current) ?: return "—"
        val paidAmount = amount(paid) ?: return "—"
        val difference = (currentAmount - paidAmount).coerceAtLeast(0.0)
        return if (difference % 1.0 == 0.0) "$${difference.toInt()}" else String.format(Locale.US, "$%.2f", difference)
    }

    private fun resolveOwnedShips(items: List<HangarItem>): List<OwnedShip> {
        if (featuredAuditLogged.compareAndSet(false, true)) {
            val accepted = items.filter(::isHeroShipCandidate)
            Log.i(
                "RefugeFeaturedAudit",
                "inventory=${items.size} featured=${accepted.size} " +
                    "ships=${accepted.joinToString { "${it.resolvedFinalShip}[${it.id}]" }} " +
                    "acceptedUpgradeResults=${accepted.count { it.isUpgrade }} " +
                    "rejectedWithoutTypedShip=${items.count { !isHeroShipCandidate(it) }}",
            )
        }
        return items
        .filter(::isHeroShipCandidate)
        .map { ship ->
            val finalShip = heroShipName(ship)!!
            val m80 = finalShip.equals("M80", true)
            OwnedShip(
                name = finalShip,
                packageName = ship.typeLabel,
                currentValue = ship.currentValue,
                paidValue = ship.price,
                insurance = ship.insurance,
                imageRes = if (m80) m80Image else fallbackImage,
                imageUrl = ship.includedEntries.firstOrNull { it.kind.equals("ship", true) || it.kind == "舰船" }?.imageUrl ?: ship.imageUrl,
                sourceItemId = ship.id,
                shipId = ship.shipId,
            )
        }
    }

    override suspend fun inventory(): List<HangarItem> {
        val accountKey = auth.currentAccountSnapshotKey() ?: return emptyList()
        val result = refresh.await("$accountKey:${inventoryEpoch.get()}") { loadInventory(accountKey) }
        return if (auth.currentAccountSnapshotKey() == accountKey) result else cachedInventory()
    }

    override suspend fun refreshInventory(): List<HangarItem> = withContext(Dispatchers.IO) {
        val account = auth.currentAccountSnapshotKey() ?: error("请先登录 RSI")
        inventoryEpoch.incrementAndGet()
        val first = auth.getPage("account/pledges?page=0")
        fun verifyPage(html: String) {
            check(html.contains("js-pledge-name") ||
                Regex("(?is)(no pledges|no items|hangar is empty|you have no)").containsMatchIn(html)) {
                "RSI 未返回可核验的机库清单，请重新登录后刷新"
            }
        }
        verifyPage(first)
        val lastPage = Regex("(?is)/account/pledges\\?page=(\\d+)").findAll(first)
            .mapNotNull { it.groupValues[1].toIntOrNull() }.maxOrNull() ?: 0
        check(lastPage <= 50) { "机库超过分页读取上限，无法完整核验" }
        val rows = buildList {
            addAll(parseRows(first, 0))
            for (page in 1..lastPage) {
                val html = auth.getPage("account/pledges?page=$page")
                verifyPage(html)
                addAll(parseRows(html, page))
            }
        }
        check(rows.all { it.id > 0 } && rows.map { it.id }.distinct().size == rows.size) { "机库编号缺失或重复" }
        val enriched = enrichShipValues(rows)
        check(auth.currentAccountSnapshotKey() == account) { "账户已变更，请重新刷新" }
        snapshots.put(account, enriched)
        writeHangarSnapshot(snapshots.file(account), enriched)
        inventoryEpoch.incrementAndGet()
        enriched
    }

    override suspend fun reclaim(request: HangarReclaimRequest, deviceVerified: Boolean): HangarReclaimResult =
        reclaimMutex.withLock {
            val account = auth.currentAccountSnapshotKey() ?: error("请先登录 RSI")
            // Finish accounting and refreshing even if the detail sheet is dismissed mid-request.
            withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                runHangarReclaim(request, deviceVerified,
                    validate = { ids ->
                        val fresh = refreshInventory()
                        check(auth.currentAccountSnapshotKey() == account) { "账户已变更，请重新确认" }
                        check(ids.all { id -> fresh.any { it.id == id && it.isReclaimable } }) {
                            "所选物品已变化或不可回收，请刷新后重新确认"
                        }
                    },
                    send = { _, body ->
                        val response = auth.reclaimPledge(body, account)
                        if (response.optInt("success") == 1) null
                        else response.optString("msg").ifBlank { "RSI 拒绝回收，已停止后续请求" }
                    },
                    onConfirmed = { id ->
                        val remaining = snapshots.get(account).orEmpty().filterNot { it.id == id }
                        snapshots.put(account, remaining)
                        writeHangarSnapshot(snapshots.file(account), remaining)
                        upgradeTargetIds.keys.removeAll { it.first == account }
                        inventoryEpoch.incrementAndGet()
                    },
                    refresh = {
                        check(auth.currentAccountSnapshotKey() == account) { "账户已变更，请重新刷新" }
                        refreshInventory()
                    },
                )
            }
        }

    private suspend fun loadInventory(accountKey: String): List<HangarItem> = withContext(Dispatchers.IO) {
        val loaded = runCatching {
            val first = withTimeoutOrNull(7_000) { auth.getPage("account/pledges?page=0") }
                ?: return@runCatching emptyList()
            val totalPages = Regex("(?is)/account/pledges\\?page=(\\d+)")
                .findAll(first).mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() }
                .maxOrNull()?.coerceIn(1, 50) ?: 1
            val pages = coroutineScope {
                    (1..totalPages).map { page ->
                        async(Dispatchers.IO) {
                            runCatching { withTimeoutOrNull(7_000) { auth.getPage("account/pledges?page=$page") } }
                                .getOrNull()?.let { page to it }
                        }
                    }.awaitAll()
            }
            val parsed = pages.filterNotNull().sortedBy { it.first }
                .flatMap { (page, html) -> parseRows(html, page) }
            // Each page is requested exactly once. Preserve every parsed
            // pledge row and expose the counts instead of silently deduping
            // legitimate repeated purchases.
            Log.i("RefugeHangarAudit", "pages=$totalPages raw=${parsed.size} parsed=${parsed.size} repository=${parsed.size}")
            parsed
        }.getOrElse { error ->
            Log.w("RefugeHangarAudit", "refresh failed; retaining last successful snapshot", error)
            emptyList()
        }
        if (auth.currentAccountSnapshotKey() != accountKey) {
            return@withContext snapshots.get(accountKey).orEmpty()
        }
        if (loaded.isNotEmpty()) {
            val enriched = enrichShipValues(loaded)
            if (auth.currentAccountSnapshotKey() != accountKey) {
                return@withContext snapshots.get(accountKey).orEmpty()
            }
            snapshots.put(accountKey, enriched)
            writeHangarSnapshot(snapshots.file(accountKey), enriched)
            return@withContext enriched
        }
        snapshots.get(accountKey).orEmpty()
    }

    private fun currentAccountInventory(memoryOnly: Boolean = false): List<HangarItem>? =
        auth.currentAccountSnapshotKey()?.let { accountKey ->
            if (memoryOnly) snapshots.peek(accountKey) else snapshots.get(accountKey)
        }

    private suspend fun awaitCurrentAccountInventory(): List<HangarItem>? =
        auth.currentAccountSnapshotKey()?.let { accountKey ->
            snapshots.await(accountKey).takeIf { auth.currentAccountSnapshotKey() == accountKey }
        }

    private fun readHangarSnapshot(file: File): List<HangarItem>? = runCatching {
        if (!file.isFile) return@runCatching null
        val rows = JSONArray(file.readText())
        buildList {
            for (index in 0 until rows.length()) rows.optJSONObject(index)?.let { row ->
                val includedEntries = row.optJSONArray("includedEntries")?.let { values ->
                    buildList {
                        for (entryIndex in 0 until values.length()) values.optJSONObject(entryIndex)?.let { entry ->
                            add(
                                HangarIncludedItem(
                                    title = entry.optString("title"),
                                    imageUrl = entry.optString("imageUrl").takeIf { it.isNotBlank() },
                                    kind = entry.optString("kind"),
                                    subtitle = entry.optString("subtitle"),
                                    value = entry.optString("value", "—"),
                                ),
                            )
                        }
                    }
                }.orEmpty()
                val finalShip = row.optString("resolvedFinalShip").takeIf { it.isNotBlank() }
                add(
                    HangarItem(
                        title = row.optString("title"),
                        price = row.optString("price", "—"),
                        date = row.optString("date", "—"),
                        imageRes = if (finalShip.equals("M80", true) || row.optString("title").contains("M80", true)) m80Image else fallbackImage,
                        isGiftable = row.optBoolean("isGiftable"),
                        isReclaimable = row.optBoolean("isReclaimable"),
                        originalName = row.optString("originalName", "—"),
                        typeLabel = row.optString("typeLabel", "机库项目"),
                        insurance = row.optString("insurance", "—"),
                        currentValue = row.optString("currentValue", row.optString("price", "—")),
                        savings = row.optString("savings", "$0"),
                        includedItems = row.optJSONArray("includedItems")?.let { values ->
                            buildList { for (itemIndex in 0 until values.length()) add(values.optString(itemIndex)) }
                        }.orEmpty(),
                        upgradeFrom = row.optString("upgradeFrom").takeIf { it.isNotBlank() },
                        upgradeTo = row.optString("upgradeTo").takeIf { it.isNotBlank() },
                        upgradeFromPrice = row.optString("upgradeFromPrice").takeIf { it.isNotBlank() },
                        upgradeToPrice = row.optString("upgradeToPrice").takeIf { it.isNotBlank() },
                        includedEntries = includedEntries,
                        containedShip = row.optString("containedShip").takeIf { it.isNotBlank() },
                        imageUrl = row.optString("imageUrl").takeIf { it.isNotBlank() },
                        id = row.optLong("id"),
                        status = row.optString("status", "—"),
                        canUpgrade = row.optBoolean("canUpgrade"),
                        isUpgrade = row.optBoolean("isUpgrade"),
                        resolvedFinalShip = finalShip,
                        page = row.optInt("page"),
                        shipId = row.optInt("shipId").takeIf { it > 0 },
                        quantity = row.optInt("quantity", 1).coerceAtLeast(1),
                        idList = row.optJSONArray("idList")?.let { ids ->
                            buildList { for (index in 0 until ids.length()) ids.optLong(index).takeIf { it > 0 }?.let(::add) }
                        }?.takeIf { it.isNotEmpty() } ?: listOf(row.optLong("id")).filter { it > 0 },
                    ),
                )
            }
        }.takeIf { it.isNotEmpty() }
    }.onFailure { Log.w("RefugeHangarAudit", "snapshot read failed", it) }.getOrNull()

    private fun writeHangarSnapshot(file: File, items: List<HangarItem>) {
        runCatching {
            val rows = JSONArray()
            items.forEach { item ->
                val includedEntries = JSONArray()
                item.includedEntries.forEach { entry ->
                    includedEntries.put(
                        JSONObject()
                            .put("title", entry.title)
                            .put("imageUrl", entry.imageUrl ?: "")
                            .put("kind", entry.kind)
                            .put("subtitle", entry.subtitle)
                            .put("value", entry.value),
                    )
                }
                rows.put(
                    JSONObject()
                        .put("title", item.title)
                        .put("price", item.price)
                        .put("date", item.date)
                        .put("isGiftable", item.isGiftable)
                        .put("isReclaimable", item.isReclaimable)
                        .put("originalName", item.originalName)
                        .put("typeLabel", item.typeLabel)
                        .put("insurance", item.insurance)
                        .put("currentValue", item.currentValue)
                        .put("savings", item.savings)
                        .put("quantity", item.quantity)
                        .put("idList", JSONArray(item.idList))
                        .put("includedItems", JSONArray(item.includedItems))
                        .put("upgradeFrom", item.upgradeFrom ?: "")
                        .put("upgradeTo", item.upgradeTo ?: "")
                        .put("upgradeFromPrice", item.upgradeFromPrice ?: "")
                        .put("upgradeToPrice", item.upgradeToPrice ?: "")
                        .put("includedEntries", includedEntries)
                        .put("containedShip", item.containedShip ?: "")
                        .put("imageUrl", item.imageUrl ?: "")
                        .put("id", item.id)
                        .put("status", item.status)
                        .put("canUpgrade", item.canUpgrade)
                        .put("isUpgrade", item.isUpgrade)
                        .put("resolvedFinalShip", item.resolvedFinalShip ?: "")
                        .put("page", item.page)
                        .put("shipId", item.shipId),
                )
            }
            file.writeText(rows.toString())
        }.onFailure { Log.w("RefugeHangarAudit", "snapshot write failed", it) }
    }

    private fun parseRows(html: String, page: Int): List<HangarItem> {
        val namePattern = Regex("(?is)<input[^>]+class=[\"'][^\"']*js-pledge-name[^\"']*[\"'][^>]+value=[\"']([^\"']+)")
        return namePattern.findAll(html).mapNotNull { match ->
            // Keep extraction inside the current pledge row. A large sliding
            // window can accidentally read the previous row's price/date,
            // which made the M80 card inherit a paint's $7.50 value.
            val rowStart = html.lastIndexOf("<li", match.range.first).takeIf { it >= 0 } ?: (match.range.first - 2800).coerceAtLeast(0)
            // RSI has used both <li> and <div class="row"> wrappers over
            // time. The next pledge name is the stable boundary and keeps
            // date/value extraction inside the same pledge.
            // Avoid matching the similarly named js-pledge-nameable-ships
            // script that appears before the date inside the same row.
            val nextName = namePattern.find(html, match.range.last + 1)?.range?.first ?: -1
            val rowEnd = if (nextName >= 0) nextName else html.length
            val start = rowStart
            val end = rowEnd
            val window = html.substring(start, end)
            val name = match.groupValues[1]
            val pledgeId = inputValue(window, "js-pledge-id")?.toLongOrNull() ?: 0L
            val price = inputValue(window, "js-pledge-value")?.let(::price) ?: "—"
            val date = text(window, "date-col")
                ?.takeUnless { it.equals("Created:", true) || it.isBlank() }
                ?: Regex("(?is)date-col.*?((?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2},\\s+\\d{4})")
                    .find(window)?.groupValues?.getOrNull(1)
                ?: Regex("(?i)(January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2},\\s+\\d{4}")
                    .find(window)?.value
                ?: Regex("\\b\\d{4}[-/]\\d{2}[-/]\\d{2}\\b").find(window)?.value
                ?: Regex("(?i)(?:datetime|data-date|data-created|created)[^=]*=[\\\"']([^\\\"']+)").find(window)?.groupValues?.getOrNull(1)
                ?: "—"
            val decoded = Html.fromHtml(name, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            val includedEntries = extractIncludedEntries(window)
            val alsoContainsItems = extractAlsoContainsTitles(window)
            val containedShip = includedEntries.firstOrNull { it.kind.equals("ship", true) }?.title
            // Cosmetic packages can mention their target ship (for example an M80
            // paint pack). They remain inventory rows and must never become a hero
            // ship card merely because the items-col contains a ship name.
            val cosmetic = isCosmeticTitle(decoded)
            val upgradeData = inputValue(window, "js-upgrade-data")
            val isUpgrade = upgradeData != null
            val upgradePair = upgradeData?.let(::upgradeEndpoints)
            val finalShip = when {
                isUpgrade -> upgradePair?.second
                !cosmetic -> containedShip
                else -> null
            }
            val ship = finalShip != null
            val type = when {
                ship -> "舰船 / 游戏包"
                cosmetic -> "涂装"
                decoded.contains("armor", true) || decoded.contains("gear", true) || decoded.contains("装备") -> "装备"
                else -> "机库项目"
            }
            HangarItem(
                title = decoded,
                price = price,
                date = cleanDate(date),
                imageRes = if (isM80(decoded) || finalShip.equals("M80", true)) m80Image else fallbackImage,
                originalName = decoded,
                typeLabel = type,
                insurance = parseInsurance(includedEntries, window),
                isGiftable = window.contains("js-gift", true),
                isReclaimable = window.contains("js-reclaim", true),
                currentValue = price,
                containedShip = containedShip,
                includedItems = alsoContainsItems,
                includedEntries = includedEntries,
                imageUrl = Regex("(?is)background-image\\s*:\\s*url\\(['\\\"]?([^'\\\")]+)").find(window)?.groupValues?.getOrNull(1)?.let(::rsiAssetUrl),
                id = pledgeId,
                status = text(window, "availability") ?: "—",
                canUpgrade = window.contains("js-apply-upgrade", true),
                isUpgrade = isUpgrade,
                upgradeFrom = upgradePair?.first,
                upgradeTo = upgradePair?.second,
                resolvedFinalShip = finalShip,
                page = page,
            )
        }.toList()
    }

    private fun inputValue(html: String, className: String): String? {
        val match = Regex("(?is)<input[^>]*class=[\"'][^\"']*\\b$className\\b[^\"']*[\"'][^>]*value=[\"']([^\"']+)").find(html)
        return match?.groupValues?.getOrNull(1)
    }

    private fun text(html: String, className: String): String? = Regex("(?is)<[^>]*class=[\"'][^\"']*\\b$className\\b[^\"']*[\"'][^>]*>(.*?)</[^>]+>")
        .find(html)?.groupValues?.getOrNull(1)?.replace(Regex("<[^>]+>"), "")?.trim()

    private fun price(raw: String): String = Regex("[0-9]+(?:\\.[0-9]+)?").find(raw.replace(",", ""))?.value?.toDoubleOrNull()?.let {
        if (it % 1.0 == 0.0) String.format(Locale.US, "$%.0f", it)
        else String.format(Locale.US, "$%.2f", it)
    } ?: raw

    private fun cleanDate(raw: String): String {
        val cleaned = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("(?i)created:\\s*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (Regex("\\d{4}年\\d{2}月\\d{2}日").matches(cleaned)) return cleaned
        return runCatching {
            java.time.LocalDate.parse(
                cleaned,
                java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US),
            ).format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA))
        }.getOrDefault(cleaned)
    }

    private fun isM80(title: String) = title.contains("M80", true)
    private fun isUpgradeTitle(title: String) = Regex("(?i)\\b(upgrade|ccu)\\b|升级").containsMatchIn(title)

    private fun isCosmeticTitle(title: String) = Regex(
        "(?i)(paint|paints|livery|skin|涂装|油漆|纹理|涂层)",
    ).containsMatchIn(title)

    private fun extractIncludedEntries(html: String): List<HangarIncludedItem> {
        val starts = Regex("(?is)<div[^>]+class=[\"'][^\"']*\\bitem\\b[^\"']*[\"'][^>]*>").findAll(html).toList()
        val strict = starts.mapIndexedNotNull { index, match ->
            val end = starts.getOrNull(index + 1)?.range?.first ?: html.length
            val itemHtml = html.substring(match.range.first, end)
            val imageUrl = Regex("(?is)background-image\\s*:\\s*url\\(['\"]?([^'\")]+)")
                .find(itemHtml)?.groupValues?.getOrNull(1)?.let(::rsiAssetUrl)
                ?: return@mapIndexedNotNull null
            val rawTitle = Regex("(?is)<div[^>]+class=[\"'][^\"']*\\btitle\\b[^\"']*[\"'][^>]*>(.*?)</div>")
                .find(itemHtml)?.groupValues?.getOrNull(1).orEmpty()
            val title = cleanIncludedTitle(rawTitle)
            title.takeIf { it.isNotBlank() }?.let {
                HangarIncludedItem(
                    title = it,
                    imageUrl = imageUrl,
                    kind = text(itemHtml, "kind").orEmpty(),
                    subtitle = text(itemHtml, "liner").orEmpty(),
                )
            }
        }
        return strict
    }

    private fun extractAlsoContainsTitles(html: String): List<String> =
        Regex("(?is)<div[^>]+class=[\"'][^\"']*\\btitle\\b[^\"']*[\"'][^>]*>(.*?)</div>")
            .findAll(html)
            .map { cleanIncludedTitle(it.groupValues[1]) }
            .filter { it.isNotBlank() }
            .toList()

    private fun cleanIncludedTitle(raw: String): String =
        Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace(Regex("\\[\\]\\s*null", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bAttributed\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun upgradeEndpoints(raw: String): Pair<String?, String?> {
        val decoded = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
        return runCatching {
            val json = JSONObject(decoded)
            val matches = json.optJSONArray("match_items")
            val targets = json.optJSONArray("target_items")
            matches?.optJSONObject(0)?.optString("name")?.takeIf { it.isNotBlank() } to
                targets?.optJSONObject(0)?.optString("name")?.takeIf { it.isNotBlank() }
        }.getOrElse { null to null }
    }

    private fun parseInsurance(items: List<HangarIncludedItem>, raw: String): String {
        val text = (items.joinToString(" ") { "${it.title} ${it.subtitle}" } + " " +
            Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()).replace(Regex("\\s+"), " ")
        if (Regex("(?i)lifetime insurance|\\bLTI\\b").containsMatchIn(text)) return "LTI"
        val match = Regex("(?i)(\\d+)\\s*[- ]?(month|year)s?\\s+insurance").find(text) ?: return "—"
        val amount = match.groupValues[1].toIntOrNull() ?: return "—"
        return if (match.groupValues[2].startsWith("year", true)) "${amount}Y" else if (amount % 12 == 0) "${amount / 12}Y" else "${amount}M"
    }

}

/** Online account projection used by the Profile page; scoped cache remains the safe fallback. */
class RsiLiveProfileRepository(
    context: Context,
    private val auth: RsiAuthDataSource,
    private val fallback: ProfileRepository,
    private val hangar: HangarRepository,
) : ProfileRepository {
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val snapshots = AccountSnapshotCache(
        context.filesDir,
        "profile",
        ::readProfileSnapshot,
        ::hasMeaningfulProfileSnapshot,
        { legacy, scoped -> retainMeaningfulProfileSnapshot(legacy, scoped) },
    )
    private val refresh = RepositoryRefresh<ProfileData>(minimumIntervalMillis = PROFILE_REFRESH_DEBOUNCE_MILLIS)

    init {
        auth.currentAccountSnapshotKey()?.let { snapshots.preload(it, preloadScope) }
    }

    override fun cachedProfile(): ProfileData {
        val session = auth.session() ?: return ProfileData()
        val accountKey = auth.currentAccountSnapshotKey()
        val cached = accountKey?.let { profileFallback(it, snapshots.peek(it)) } ?: ProfileData()
        return cached.copy(
            isAuthenticated = session.isAuthenticated,
        )
    }

    override suspend fun awaitCachedProfile(): ProfileData {
        val session = auth.session() ?: return ProfileData()
        val accountKey = auth.currentAccountSnapshotKey()
            ?: return ProfileData(isAuthenticated = session.isAuthenticated)
        val scoped = snapshots.await(accountKey)
            .takeIf { auth.currentAccountSnapshotKey() == accountKey }
        // The bundled fallback is parsed by its own IO-bound await path. Do
        // not call fallback.cachedProfile() here: this method is reached from
        // a Compose coroutine and must remain cache-first without blocking the
        // main thread on JSON parsing.
        val bundled = fallback.awaitCachedProfile()
        return profileFallback(accountKey, scoped, bundled).copy(isAuthenticated = session.isAuthenticated)
    }

    override suspend fun profile(): ProfileData {
        val accountKey = auth.currentAccountSnapshotKey() ?: return ProfileData()
        val result = refresh.await(accountKey) { loadProfile(accountKey) }
        return if (auth.currentAccountSnapshotKey() == accountKey) result else cachedProfile()
    }

    private suspend fun loadProfile(accountKey: String): ProfileData = withContext(Dispatchers.IO) {
        val session = auth.session()
            ?: return@withContext snapshots.get(accountKey) ?: ProfileData()
        // A scoped refresh can be partial (for example the account GraphQL may
        // be unavailable while the hangar page still succeeds). Start with the
        // verified bundled snapshot and let the scoped file override only
        // meaningful fields, so a partial refresh never turns the header back
        // into the generic "RSI 账户" placeholder.
        val bundled = fallback.awaitCachedProfile()
        val fallbackProfile = profileFallback(accountKey, snapshots.get(accountKey), bundled)
        val account = runCatching {
            auth.accountGraphql().optJSONObject("data")?.optJSONObject("account")
        }.onFailure { error ->
            Log.w("RefugeProfileAudit", "account GraphQL unavailable; continuing with account pages", error)
        }.getOrNull() ?: JSONObject()
        val organizationGraphql = account.optJSONObject("badgeIcons")
            ?.optJSONObject("organization")
        val organizationNameFromGraphql = organizationGraphql?.optString("name")
            ?.takeIf { it.isNotBlank() }
        val organizationImageFromGraphql = organizationGraphql?.optString("icon")
            ?.takeIf { it.isNotBlank() }
            ?.let(::rsiAssetUrl)
        val inventory = runCatching { hangar.inventory() }
            .onFailure { error -> Log.w("RefugeProfileAudit", "hangar inventory unavailable", error) }
            .getOrDefault(hangar.cachedInventory())
        val ships = runCatching { hangar.ownedShips() }
            .onFailure { error -> Log.w("RefugeProfileAudit", "owned ships unavailable", error) }
            .getOrDefault(hangar.cachedOwnedShips())
        // Melt value includes every hangar line, including CCUs and gear.
        val meltTotal = inventory.asSequence()
            .mapNotNull { numericUsd(it.price) }
            .sum()
        // Original value uses enriched ship MSRP and CCU MSRP deltas.
        val retailTotal = inventory.asSequence()
            .mapNotNull { numericUsd(it.currentValue)?.takeIf { value -> value > 0.0 } ?: numericUsd(it.price) }
            .sum()
        val hasInventory = inventory.isNotEmpty()
        val hasGamePackage = account.optBoolean("hasGamePackage", false).takeIf { account.has("hasGamePackage") }
        val hangarValue = if (!hasInventory) {
            if (hasGamePackage == true) "—" else fallbackProfile.hangarValue
        } else {
            formatUsdValue(meltTotal)
        }
        val retailValue = if (!hasInventory) {
            if (hasGamePackage == true) "—" else fallbackProfile.currentValue
        } else {
            formatUsdValue(retailTotal)
        }
        val billing = runCatching { auth.getPage("account/billing") }
            .onFailure { error -> Log.w("RefugeProfileAudit", "billing page unavailable", error) }
            .getOrDefault("")
        val handle = resolveRsiProfileHandle(
            graphqlNickname = account.optString("nickname").takeIf { it.isNotBlank() },
            graphqlUsername = account.optString("username").takeIf { it.isNotBlank() },
            accountHtml = billing,
            cachedHandle = fallbackProfile.handle,
        )
        val citizen = handle?.let { candidate ->
            runCatching { auth.getPage("citizens/${android.net.Uri.encode(candidate)}") }
                .onFailure { error -> Log.w("RefugeProfileAudit", "citizen dossier unavailable", error) }
                .getOrNull()
        }
        val citizenHtml = citizen.orEmpty()
        val citizenProfile = parseRsiCitizenProfile(citizenHtml)
        val citizenDossierAvailable = isRsiCitizenDossier(citizenHtml)
        val credit = runCatching { auth.creditGraphql() }
            .onFailure { error -> Log.w("RefugeProfileAudit", "credit GraphQL unavailable", error) }
            .getOrDefault(JSONObject())
        val totalSpent = Regex("(?is)<div[^>]+class=[\"'][^\"']*spent-line[^\"']*[\"'][^>]*>.*?<em>\\s*\\$?([0-9,.]+)")
            .findAll(billing).lastOrNull()?.groupValues?.getOrNull(1)?.let { "$$it" }
            ?: fallbackProfile.totalSpent
        val creditValue = ledgerValue(credit, "ledgerCredit", monetary = true)
            .takeUnless { it == "—" } ?: fallbackProfile.credit
        val uecValue = ledgerValue(credit, "ledgerUec").takeUnless { it == "—" } ?: fallbackProfile.uec
        val recValue = ledgerValue(credit, "ledgerRec").takeUnless { it == "—" } ?: fallbackProfile.rec
        val avatar = account.optString("avatar").ifBlank {
            citizenProfile.avatarPath.orEmpty()
        }.takeIf { it.isNotBlank() }?.let(::rsiAssetUrl) ?: fallbackProfile.avatarUrl
        val organizationName = if (citizenDossierAvailable) {
            citizenProfile.organizationName
        } else {
            organizationNameFromGraphql ?: fallbackProfile.organizationName
        }
        val organizationImage = if (citizenDossierAvailable) {
            citizenProfile.organizationImagePath?.let(::rsiAssetUrl)
        } else {
            organizationImageFromGraphql ?: fallbackProfile.organizationImage
        }
        val organizationIdFromGraphql = organizationGraphql?.optString("url")
            ?.let { Regex("(?i)/orgs/([^/?#]+)").find(it)?.groupValues?.getOrNull(1) }
            ?.takeIf { it.isNotBlank() }
        val organizationId = if (citizenDossierAvailable) {
            citizenProfile.organizationId
        } else {
            organizationIdFromGraphql ?: fallbackProfile.organizationId
        }
        val organizationRank = if (citizenDossierAvailable) {
            citizenProfile.organizationRank
        } else {
            fallbackProfile.organizationRank
        }
        val organizationLevel = if (citizenDossierAvailable) {
            citizenProfile.organizationLevel
        } else {
            fallbackProfile.organizationLevel
        }
        val register = citizenProfile.enlisted ?: account.optString("createdAt").takeIf { it.isNotBlank() }
            ?: fallbackProfile.registerDate
        val profile = sanitizeCachedProfile(fallbackProfile.copy(
            handle = handle ?: fallbackProfile.handle,
            displayName = account.optString("displayname").takeIf { it.isNotBlank() } ?: fallbackProfile.displayName,
            rank = organizationRank ?: fallbackProfile.rank,
            organizationId = organizationId,
            organizationName = organizationName,
            organizationRank = organizationRank,
            organizationLevel = organizationLevel,
            organizationImage = organizationImage,
            level = organizationLevel.takeIf { it > 0 }?.toString()
                ?: if (citizenDossierAvailable) "—" else fallbackProfile.level,
            registerDate = register,
            totalSpent = totalSpent,
            hangarValue = hangarValue,
            currentValue = retailValue,
            credit = creditValue,
            uec = uecValue,
            rec = recValue,
            referralCode = account.optString("referral_code").takeIf { it.isNotBlank() } ?: fallbackProfile.referralCode,
            avatarUrl = avatar,
            email = account.optString("email").takeIf { it.isNotBlank() } ?: fallbackProfile.email,
            username = account.optString("username").takeIf { it.isNotBlank() } ?: fallbackProfile.username,
            hasGamePackage = hasGamePackage ?: fallbackProfile.hasGamePackage,
            isAuthenticated = session.isAuthenticated,
        ))
        if (auth.currentAccountSnapshotKey() != accountKey) {
            return@withContext snapshots.get(accountKey) ?: ProfileData()
        }
        Log.i(
            "RefugeProfileAudit",
            "totalSpent=${profile.totalSpent}[account/billing] " +
                "hangarValue=${profile.hangarValue}[all inventory melt sum] " +
                "currentValue=${profile.currentValue}[ship MSRP + CCU delta + other original value] ships=${ships.size} items=${inventory.size} " +
                "credit=${profile.credit}[ledgerCredit] uec=${profile.uec}[ledgerUec] rec=${profile.rec}[ledgerRec] " +
                "register=${profile.registerDate}[citizen dossier] referral=${if (profile.referralCode == "—") "unavailable" else "available"}[account GraphQL] " +
                "organization=${if (profile.organizationName == null) "unavailable" else "available"}[${if (citizenDossierAvailable) "citizen dossier" else "account GraphQL fallback"}]",
        )
        val result = profile
        if (result.isAuthenticated && result.handle.isNotBlank() && result.handle != "RSI 账户") {
            val sanitized = retainRsiProfileRefresh(
                previous = snapshots.get(accountKey),
                refreshed = sanitizeCachedProfile(result),
                citizenDossierAvailable = citizenDossierAvailable,
            )
            if (auth.currentAccountSnapshotKey() != accountKey) {
                return@withContext snapshots.get(accountKey)
                    ?: ProfileData()
            }
            snapshots.put(accountKey, sanitized)
            writeProfileSnapshot(snapshots.file(accountKey), sanitized)
            return@withContext sanitized
        }
        result
    }

    private fun readProfileSnapshot(file: File): ProfileData? = runCatching {
        if (!file.isFile) return@runCatching null
        val row = JSONObject(file.readText())
        sanitizeCachedProfile(ProfileData(
            handle = row.optString("handle", "RSI 账户"),
            displayName = row.optString("displayName").takeIf { it.isNotBlank() },
            city = row.optString("city", "—"),
            rank = row.optString("rank", "—"),
            organizationId = row.optString("organizationId").takeIf { it.isNotBlank() },
            organizationName = row.optString("organizationName").takeIf { it.isNotBlank() },
            organizationRank = row.optString("organizationRank").takeIf { it.isNotBlank() },
            organizationLevel = row.optInt("organizationLevel", 0),
            organizationImage = row.optString("organizationImage").takeIf { it.isNotBlank() },
            level = row.optString("level", "—"),
            totalSpent = row.optString("totalSpent", "—"),
            hangarValue = row.optString("hangarValue", "—"),
            credit = row.optString("credit", "—"),
            registerDate = row.optString("registerDate", "—"),
            uec = row.optString("uec", "—"),
            rec = row.optString("rec", "—"),
            currentValue = row.optString("currentValue", "—"),
            referralCode = row.optString("referralCode", "—"),
            avatarUrl = row.optString("avatarUrl").takeIf { it.isNotBlank() },
            email = row.optString("email").takeIf { it.isNotBlank() },
            username = row.optString("username").takeIf { it.isNotBlank() },
            hasGamePackage = row.opt("hasGamePackage") as? Boolean,
            isAuthenticated = row.optBoolean("isAuthenticated", false),
        ))
    }.onFailure { Log.w("RefugeProfileAudit", "profile snapshot read failed", it) }.getOrNull()

    private fun writeProfileSnapshot(file: File, profile: ProfileData) {
        runCatching {
            file.writeText(
                JSONObject()
                    .put("schemaVersion", PROFILE_SNAPSHOT_SCHEMA_VERSION)
                    .put("handle", profile.handle)
                    .put("displayName", profile.displayName ?: "")
                    .put("city", profile.city)
                    .put("rank", profile.rank)
                    .put("organizationId", profile.organizationId ?: "")
                    .put("organizationName", profile.organizationName ?: "")
                    .put("organizationRank", profile.organizationRank ?: "")
                    .put("organizationLevel", profile.organizationLevel)
                    .put("organizationImage", profile.organizationImage ?: "")
                    .put("level", profile.level)
                    .put("totalSpent", profile.totalSpent)
                    .put("hangarValue", profile.hangarValue)
                    .put("credit", profile.credit)
                    .put("registerDate", profile.registerDate)
                    .put("uec", profile.uec)
                    .put("rec", profile.rec)
                    .put("currentValue", profile.currentValue)
                    .put("referralCode", profile.referralCode)
                    .put("avatarUrl", profile.avatarUrl ?: "")
                    .put("email", profile.email ?: "")
                    .put("username", profile.username ?: "")
                    .put("hasGamePackage", profile.hasGamePackage)
                    .put("isAuthenticated", profile.isAuthenticated)
                    .toString(),
            )
        }.onFailure { Log.w("RefugeProfileAudit", "profile snapshot write failed", it) }
    }

    private fun profileFallback(
        accountKey: String,
        scoped: ProfileData?,
        bundledProfile: ProfileData? = null,
    ): ProfileData {
        // Keep the account key in the helper signature so callers cannot
        // accidentally merge a snapshot from another signed-in account.
        if (auth.currentAccountSnapshotKey() != accountKey) return ProfileData()
        val bundled = sanitizeCachedProfile(bundledProfile ?: fallback.cachedProfile())
        val accountScoped = scoped?.let(::sanitizeCachedProfile) ?: ProfileData()
        return retainMeaningfulProfileSnapshot(bundled, accountScoped)
    }

    private fun numericUsd(value: String): Double? = Regex("[0-9]+(?:\\.[0-9]+)?")
        .find(value.replace(",", ""))?.value?.toDoubleOrNull()

    private fun formatUsdValue(value: Double): String = if (value % 1.0 == 0.0) {
        String.format(Locale.US, "$%.0f", value)
    } else {
        String.format(Locale.US, "$%.2f", value)
    }

    private fun ledgerValue(root: JSONObject, key: String, monetary: Boolean = false): String {
        val value = root.optJSONObject("data")?.optJSONObject("customer")
            ?.optJSONObject(key)?.optJSONObject("amount")?.opt("value") ?: return "—"
        if (monetary) {
            val cents = when (value) {
                is Number -> value.toLong()
                else -> value.toString().toLongOrNull()
            } ?: return "—"
            return if (cents % 100L == 0L) "$${cents / 100L}"
            else String.format(Locale.US, "$%.2f", cents / 100.0)
        }
        return when (value) {
            is Number -> String.format(Locale.US, "%,d", value.toLong())
            else -> value.toString()
        }
    }

    private companion object {
        const val PROFILE_REFRESH_DEBOUNCE_MILLIS = 2_000L
        const val PROFILE_SNAPSHOT_SCHEMA_VERSION = 2
    }
}

class RsiLiveBuybackRepository(
    context: Context,
    private val auth: RsiAuthDataSource,
    private val fallback: BuybackRepository,
    private val fallbackImage: Int,
    private val translation: TranslationRepository,
) : BuybackRepository {
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val snapshots = AccountSnapshotCache(
        context.filesDir,
        "buyback_items",
        ::readSnapshot,
        List<BuybackItem>::isNotEmpty,
    )
    private val refresh = RepositoryRefresh<List<BuybackItem>>(minimumIntervalMillis = 1_000L)

    init {
        auth.currentAccountSnapshotKey()?.let { snapshots.preload(it, preloadScope) }
    }

    override fun cachedItems(): List<BuybackItem> = auth.currentAccountSnapshotKey()
        ?.let(snapshots::peek)
        ?.takeIf { it.isNotEmpty() }
        ?: fallback.cachedItems()

    override suspend fun awaitCachedItems(): List<BuybackItem> {
        val accountKey = auth.currentAccountSnapshotKey() ?: return emptyList()
        return snapshots.await(accountKey)
            ?.takeIf { auth.currentAccountSnapshotKey() == accountKey && it.isNotEmpty() }
            ?: fallback.awaitCachedItems()
    }

    override suspend fun items(): List<BuybackItem> {
        val accountKey = auth.currentAccountSnapshotKey() ?: return emptyList()
        val result = refresh.await(accountKey) { loadBuyback(accountKey) }
        return if (auth.currentAccountSnapshotKey() == accountKey) result else cachedItems()
    }

    private suspend fun loadBuyback(accountKey: String): List<BuybackItem> = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = mutableListOf<BuybackItem>()
            val pageCounts = mutableListOf<Int>()
            // Match the original repository contract: RSI pages are 1-based,
            // 100 rows per page, and pagination ends at the first empty page.
            for (page in 1..50) {
                var html = ""
                var rows = emptyList<MatchResult>()
                val attempts = if (page == 1) 3 else 1
                for (attempt in 0 until attempts) {
                    html = auth.getPage("account/buy-back-pledges?page=$page&pagesize=100")
                    rows = buybackRowRegex.findAll(html).toList()
                    if (rows.isNotEmpty()) break
                    if (attempt + 1 < attempts) delay(650)
                }
                pageCounts += rows.size
                if (rows.isEmpty()) {
                    if (page == 1) {
                        Log.w(
                            "RefugeBuybackAudit",
                            "empty first page after $attempts attempts html=${html.length} " +
                                "articles=${Regex("(?i)<article").findAll(html).count()} " +
                                "pledgeMarkers=${Regex("(?i)pledge").findAll(html).count()}; retaining snapshot",
                        )
                    }
                    break
                }
                rows.mapNotNullTo(parsed) { match ->
                    val row = match.groupValues[1]
                    val title = Regex("(?is)<h1[^>]*>(.*?)</h1>").find(row)?.groupValues?.getOrNull(1)
                        ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim() }
                        ?.takeIf { it.isNotBlank() } ?: return@mapNotNullTo null
                    val dds = Regex("(?is)<dd[^>]*>(.*?)</dd>").findAll(row)
                        .map { Html.fromHtml(it.groupValues[1], Html.FROM_HTML_MODE_LEGACY).toString().trim() }
                        .toList()
                    val image = Regex("(?is)<img[^>]+src=[\"']([^\"']+)").find(row)?.groupValues?.getOrNull(1)?.let(::rsiAssetUrl)
                    val button = Regex("(?is)<[^>]+class=[\"'][^\"']*holosmallbtn[^\"']*[\"'][^>]*>").find(row)?.value.orEmpty()
                    val href = Regex("(?is)href=[\"']([^\"']+)").find(button)?.groupValues?.getOrNull(1).orEmpty()
                    val id = href.substringAfterLast('/').toLongOrNull()
                        ?: attribute(button, "data-pledgeid").toLongOrNull()
                        ?: 0L
                    val isUpgrade = title.contains("upgrade", true) || title.contains("升级")
                    BuybackItem(
                        id = id,
                        title = title,
                        price = "—",
                        date = dds.firstOrNull()?.let(::cleanBuybackDate) ?: "—",
                        imageRes = fallbackImage,
                        originalName = title,
                        isUpgrade = isUpgrade || row.contains("data-fromshipid", true),
                        imageUrl = image,
                        contains = dds.drop(1),
                        fromShipId = attribute(button, "data-fromshipid").toLongOrNull() ?: 0,
                        toShipId = attribute(button, "data-toshipid").toLongOrNull() ?: 0,
                        toSkuId = attribute(button, "data-toskuid").toLongOrNull() ?: 0,
                    )
                }
            }
            val stacked = stackBuyback(parsed)
            Log.i("RefugeBuybackAudit", "pages=$pageCounts raw=${parsed.size} parsed=${parsed.size} repository=${stacked.sumOf { it.quantity }} groups=${stacked.size}")
            if (stacked.isNotEmpty()) {
                if (auth.currentAccountSnapshotKey() != accountKey) {
                    return@runCatching snapshots.get(accountKey).orEmpty()
                }
                snapshots.put(accountKey, stacked)
                writeSnapshot(snapshots.file(accountKey), stacked)
                stacked
            } else {
                snapshots.get(accountKey).orEmpty()
            }
        }.getOrElse {
            Log.w("RefugeBuybackAudit", "refresh failed; retaining stale data", it)
            snapshots.get(accountKey).orEmpty()
        }
    }

    private fun attribute(html: String, name: String): String =
        Regex("(?is)\\b${Regex.escape(name)}=[\"']([^\"']*)").find(html)?.groupValues?.getOrNull(1).orEmpty()

    private fun stackBuyback(items: List<BuybackItem>): List<BuybackItem> = items
        .groupBy { item ->
            if (item.isUpgrade) "Upgrade:${item.fromShipId}:${item.toShipId}:${item.toSkuId}"
            else "Item:${item.originalName}:${item.contains.joinToString("|")}"
        }
        .values
        .map { matches ->
            val first = matches.first()
            first.copy(
                quantity = matches.size,
                idList = matches.map { it.id }.filter { it > 0 },
            )
        }

    private fun translateBuybackTitle(title: String): String = when {
        title.startsWith("Subscribers Store - ", true) -> title.removePrefix("Subscribers Store - ")
        else -> title
    }

    private fun cleanBuybackDate(value: String): String = runCatching {
        val parsed = java.time.LocalDate.parse(value, java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US))
        parsed.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA))
    }.getOrDefault(value)

    private fun readSnapshot(file: File): List<BuybackItem>? = runCatching {
        if (!file.isFile) return@runCatching null
        val rows = JSONArray(file.readText())
        buildList {
            for (index in 0 until rows.length()) rows.optJSONObject(index)?.let { row ->
                add(
                    BuybackItem(
                        id = row.optLong("id"),
                        title = row.optString("title"),
                        price = row.optString("price", "—"),
                        date = row.optString("date", "—"),
                        imageRes = fallbackImage,
                        originalName = row.optString("originalName", "—"),
                        isUpgrade = row.optBoolean("isUpgrade"),
                        imageUrl = row.optString("imageUrl").takeIf { it.isNotBlank() },
                        contains = row.optJSONArray("contains")?.let { values ->
                            buildList { for (i in 0 until values.length()) add(values.optString(i)) }
                        }.orEmpty(),
                        quantity = row.optInt("quantity", 1).coerceAtLeast(1),
                        idList = row.optJSONArray("idList")?.let { values ->
                            buildList { for (i in 0 until values.length()) values.optLong(i).takeIf { it > 0 }?.let(::add) }
                        }.orEmpty(),
                        fromShipId = row.optLong("fromShipId"),
                        toShipId = row.optLong("toShipId"),
                        toSkuId = row.optLong("toSkuId"),
                    ),
                )
            }
        }.takeIf { it.isNotEmpty() }
    }.onFailure { Log.w("RefugeBuybackAudit", "snapshot read failed", it) }.getOrNull()

    private fun writeSnapshot(file: File, items: List<BuybackItem>) {
        runCatching {
            val rows = JSONArray()
            items.forEach { item ->
                rows.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("title", item.title)
                        .put("price", item.price)
                        .put("date", item.date)
                        .put("originalName", item.originalName)
                        .put("isUpgrade", item.isUpgrade)
                        .put("imageUrl", item.imageUrl ?: "")
                        .put("contains", JSONArray(item.contains))
                        .put("quantity", item.quantity)
                        .put("idList", JSONArray(item.idList))
                        .put("fromShipId", item.fromShipId)
                        .put("toShipId", item.toShipId)
                        .put("toSkuId", item.toSkuId),
                )
            }
            file.writeText(rows.toString())
        }.onFailure { Log.w("RefugeBuybackAudit", "snapshot write failed", it) }
    }

    private companion object {
        val buybackRowRegex = Regex("(?is)<article[^>]+class=[\"'][^\"']*\\bpledge\\b[^\"']*[\"'][^>]*>(.*?)</article>")
    }
}

/** Persistent cache-first port of the original `api/account/pledgeLog` flow. */
class RsiLiveHangarLogRepository(
    context: Context,
    private val auth: RsiAuthDataSource,
    private val translation: TranslationRepository? = null,
    private val fallback: HangarLogRepository? = null,
) : HangarLogRepository {
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val snapshots = AccountSnapshotCache(
        context.filesDir,
        "hangar_logs",
        ::readSnapshot,
        List<HangarLogEntry>::isNotEmpty,
    )
    private val refresh = RepositoryRefresh<List<HangarLogEntry>>(minimumIntervalMillis = 1_000L)

    init {
        auth.currentAccountSnapshotKey()?.let { snapshots.preload(it, preloadScope) }
    }

    override fun cachedEntries(): List<HangarLogEntry> = auth.currentAccountSnapshotKey()
        ?.let(snapshots::peek)
        ?.takeIf { it.isNotEmpty() }
        ?: fallback?.cachedEntries().orEmpty()

    override suspend fun awaitCachedEntries(): List<HangarLogEntry> {
        val accountKey = auth.currentAccountSnapshotKey() ?: return emptyList()
        return snapshots.await(accountKey)
            ?.takeIf { auth.currentAccountSnapshotKey() == accountKey && it.isNotEmpty() }
            ?: fallback?.awaitCachedEntries().orEmpty()
    }

    override suspend fun entries(): List<HangarLogEntry> {
        val accountKey = auth.currentAccountSnapshotKey() ?: return emptyList()
        val result = refresh.await(accountKey) { loadLogs(accountKey) }
        return if (auth.currentAccountSnapshotKey() == accountKey) result else cachedEntries()
    }

    private suspend fun loadLogs(accountKey: String): List<HangarLogEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val result = mutableListOf<HangarLogEntry>()
            var page = 1
            var pageCount = 1
            while (page <= pageCount && page <= 200) {
                val response = auth.accountPost("api/account/pledgeLog", JSONObject().put("page", page))
                val data = response.optJSONObject("data") ?: error("机库日志响应为空")
                pageCount = data.optInt("pagecount", page).coerceAtLeast(page)
                val parsed = parseRenderedLog(data.optString("rendered"))
                if (parsed.isEmpty() && page == 1) error("机库日志第一页为空")
                result += parsed
                page++
            }
            Log.i("RefugeHangarLogAudit", "pages=${page - 1}/$pageCount entries=${result.size}")
            result.takeIf { it.isNotEmpty() && auth.currentAccountSnapshotKey() == accountKey }?.also {
                snapshots.put(accountKey, it)
                writeSnapshot(snapshots.file(accountKey), it)
            } ?: snapshots.get(accountKey).orEmpty()
        }.getOrElse { error ->
            Log.w("RefugeHangarLogAudit", "refresh failed; retaining snapshot", error)
            snapshots.get(accountKey).orEmpty()
        }
    }

    private fun parseRenderedLog(rendered: String): List<HangarLogEntry> {
        if (rendered.isBlank()) return emptyList()
        val entryStart = Regex("(?is)<[^>]+class=[\"'][^\"']*pledge-log-entry[^\"']*[\"'][^>]*>")
        val starts = entryStart.findAll(rendered).toList()
        val strict = starts.mapIndexedNotNull { index, match ->
            val end = starts.getOrNull(index + 1)?.range?.first ?: rendered.length
            val block = rendered.substring(match.range.first, end)
            val text = Regex("(?is)<p[^>]*>(.*?)</p>").find(block)?.groupValues?.getOrNull(1)
                ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString() }
                ?.replace(Regex("\\s+"), " ")?.trim()
            val name = Regex("(?is)<span[^>]*>(.*?)</span>").find(block)?.groupValues?.getOrNull(1)
                ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString() }
                ?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
            parseLogEntry(text.orEmpty(), name, index)
        }
        if (strict.isNotEmpty()) return strict
        val wrappers = Regex("(?is)<(?:li|article|div)[^>]*>(.*?)</(?:li|article|div)>")
            .findAll(rendered)
            .map { match ->
                Html.fromHtml(match.groupValues[1], Html.FROM_HTML_MODE_LEGACY)
                    .toString().replace(Regex("\\s+"), " ").trim()
            }
            .filter { it.contains("#") && it.contains("-") }
            .distinct()
            .toList()
        return wrappers.mapIndexedNotNull { index, text -> parseLegacyLog(text, index) }
    }

    private fun parseLogEntry(text: String, name: String, index: Int): HangarLogEntry? {
        if (text.isBlank()) return null
        val messageParts = text.split(" - ")
        val timeText = messageParts.firstOrNull().orEmpty().trim()
        val message = messageParts.drop(1).joinToString(" - ").trim()
        val content = message.removePrefix(name).trim()
        val patterns = listOf(
            "CREATED" to Regex("^#(\\d+?) - Created by ([\\w\\d-]+?) - order #([A-Z0-9]+?), value: \\$([0-9.]+?) USD$", RegexOption.IGNORE_CASE),
            "RECLAIMED" to Regex("^#(\\d+?) - Reclaimed by ([\\w\\d-]+?) for \\$([0-9.]+?) USD$", RegexOption.IGNORE_CASE),
            "CONSUMED" to Regex("^#(\\d+?) - Consumed by ([\\w\\d-]+?) on pledge #(\\d+?), value: \\$([0-9.]+?) USD$", RegexOption.IGNORE_CASE),
            "APPLIED_UPGRADE" to Regex("^#(\\d+?) - Upgrade applied: #(\\d+?) ([^,]+?), new value: \\$([0-9.]+?) USD$", RegexOption.IGNORE_CASE),
            "BUYBACK" to Regex("^#(\\d+?) - Buy-back by ([\\w\\d-]+?) - order #([\\w\\d]+?)$", RegexOption.IGNORE_CASE),
            "GIFT" to Regex("^#(\\d+?) - Gifted to ([^,]+?), value: \\$([0-9.]+?) USD$", RegexOption.IGNORE_CASE),
            "GIFT_CLAIMED" to Regex("^#(\\d+) - Claimed as a gift by ([\\w\\d-]+?), value: \\$([0-9.]+?) USD$", RegexOption.IGNORE_CASE),
            "GIFT_CANCELLED" to Regex("^#(\\d+?) - Gift cancelled by ([\\d\\w-]+?), value: \\$([0-9.]+?) USD$", RegexOption.IGNORE_CASE),
            "NAME_CHANGE" to Regex("^#(\\d+) - Name Reservation: \\((.+)\\) on item (.+)$", RegexOption.IGNORE_CASE),
            "NAME_CHANGE_RECLAIMED" to Regex("^#(\\d+) - Name Release: \\(([^)]+)\\) on item (\\S+) Reclaimed$", RegexOption.IGNORE_CASE),
            "GIVEAWAY" to Regex("^#(\\d+?) - (.*?)$", RegexOption.IGNORE_CASE),
        )
        var type = "UNKNOWN"
        var target: String? = null
        var source: String? = null
        var operator: String? = null
        var order: String? = null
        var reason: String? = null
        var priceCents: Int? = null
        patterns.firstNotNullOfOrNull { (candidateType, regex) -> regex.matchEntire(content)?.let { candidateType to it } }
            ?.let { (candidateType, match) ->
                type = candidateType
                target = match.groupValues.getOrNull(1)
                when (candidateType) {
                    "CREATED" -> { operator = match.groupValues[2]; order = match.groupValues[3]; priceCents = cents(match.groupValues[4]) }
                    "RECLAIMED" -> { operator = match.groupValues[2]; priceCents = cents(match.groupValues[3]) }
                    "CONSUMED" -> { operator = match.groupValues[2]; source = match.groupValues[3]; priceCents = cents(match.groupValues[4]) }
                    "APPLIED_UPGRADE" -> { source = match.groupValues[2]; reason = match.groupValues[3]; priceCents = cents(match.groupValues[4]) }
                    "BUYBACK" -> { operator = match.groupValues[2]; order = match.groupValues[3] }
                    "GIFT", "GIFT_CLAIMED", "GIFT_CANCELLED" -> { operator = match.groupValues[2]; priceCents = cents(match.groupValues[3]) }
                    "NAME_CHANGE", "NAME_CHANGE_RECLAIMED" -> { source = match.groupValues[2]; reason = match.groupValues[3] }
                    "GIVEAWAY" -> reason = match.groupValues[2]
                }
            }
        val timeMillis = runCatching {
            java.text.SimpleDateFormat("MMM d yyyy, h:mm a", java.util.Locale.US)
                .parse(timeText.replace(" am", " AM", true).replace(" pm", " PM", true))?.time ?: 0L
        }.getOrDefault(0L)
        return HangarLogEntry(
            id = "$type#$target#$timeMillis#$content#$index",
            timeMillis = timeMillis,
            type = type,
            name = name.ifBlank { content.substringBefore("(#").ifBlank { "机库项目" } },
            priceCents = priceCents,
            source = source,
            target = target,
            operator = operator ?: "CIG",
            reason = reason,
            order = order,
            rawContent = content,
        )
    }

    private fun cents(value: String): Int? = value.toBigDecimalOrNull()?.movePointRight(2)?.toInt()

    private fun parseLegacyLog(text: String, index: Int): HangarLogEntry {
        val message = text.split(" - ").drop(1).joinToString(" - ").trim()
        val name = Regex("^(.*?)\\s+#\\d+\\s+-\\s+", RegexOption.IGNORE_CASE)
            .find(message)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        return parseLogEntry(text, name, index)
            ?: HangarLogEntry(id = "legacy-$index", name = name.ifBlank { text }, rawContent = text)
    }

    private fun readSnapshot(file: File): List<HangarLogEntry>? = runCatching {
        if (!file.isFile) return@runCatching emptyList()
        val rows = JSONArray(file.readText())
        buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index)
                if (row == null) {
                    rows.optString(index).takeIf { it.isNotBlank() }?.let { add(parseLegacyLog(it, index)) }
                } else add(HangarLogEntry(
                    id = row.optString("id"), timeMillis = row.optLong("timeMillis"), type = row.optString("type", "UNKNOWN"),
                    name = row.optString("name"), priceCents = row.optInt("priceCents").takeIf { row.has("priceCents") },
                    source = row.optString("source").takeIf { it.isNotBlank() }, target = row.optString("target").takeIf { it.isNotBlank() },
                    operator = row.optString("operator", "CIG"), reason = row.optString("reason").takeIf { it.isNotBlank() },
                    order = row.optString("order").takeIf { it.isNotBlank() }, rawContent = row.optString("rawContent"),
                ))
            }
        }
    }.onFailure { Log.w("RefugeHangarLogAudit", "snapshot read failed", it) }.getOrDefault(emptyList())

    private fun writeSnapshot(file: File, entries: List<HangarLogEntry>) {
        val rows = JSONArray()
        entries.forEach { entry -> rows.put(JSONObject()
            .put("id", entry.id).put("timeMillis", entry.timeMillis).put("type", entry.type).put("name", entry.name)
            .put("priceCents", entry.priceCents).put("source", entry.source).put("target", entry.target)
            .put("operator", entry.operator).put("reason", entry.reason).put("order", entry.order).put("rawContent", entry.rawContent)) }
        runCatching { file.writeText(rows.toString()) }
            .onFailure { Log.w("RefugeHangarLogAudit", "snapshot write failed", it) }
    }
}

/**
 * Public Star Citizen Wiki adapter matching the eight datasets used by the
 * original Flutter database. A successful snapshot remains visible while a
 * later refresh is in flight; production never falls back to fabricated rows.
 */
class WikiTerminalRepository(
    context: Context,
    private val translation: TranslationRepository,
    private val fallback: TerminalRepository? = null,
) : TerminalRepository {
    private val client = OkHttpClient.Builder().build()
    @Volatile private var lastSuccessful: List<TerminalItem> = emptyList()
    private val snapshotFile = File(context.filesDir, "terminal_items.json")
    private val wikiDetailDirectory = File(context.filesDir, "wiki-details").apply { mkdirs() }
    private val snapshotLoad = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
        readTerminalSnapshot().also { if (it.isNotEmpty()) lastSuccessful = it }
    }
    private val refresh = RepositoryRefresh<List<TerminalItem>>(minimumIntervalMillis = 1_000L)

    private data class Dataset(
        val category: TerminalCategory,
        val endpoint: String,
        val type: String? = null,
    )

    private val datasets = listOf(
        Dataset(TerminalCategory.VEHICLES, "vehicles"),
        Dataset(TerminalCategory.SHIP_COMPONENTS, "vehicle-weapons"),
        Dataset(TerminalCategory.PERSONAL, "weapons"),
        Dataset(TerminalCategory.ATTACHMENTS, "weapon-attachments"),
        Dataset(TerminalCategory.SHIELDS, "vehicle-items", "Shield"),
        Dataset(TerminalCategory.COOLERS, "vehicle-items", "Cooler"),
        Dataset(TerminalCategory.POWER_PLANTS, "vehicle-items", "PowerPlant"),
        Dataset(TerminalCategory.QUANTUM_DRIVES, "vehicle-items", "QuantumDrive"),
    )

    // The terminal is opened infrequently and its public datasets are large.
    // Do not start eight paginated requests while the application is booting;
    // the screen publishes the disk snapshot first and calls this explicitly.

    override fun cachedItems(): List<TerminalItem> = lastSuccessful.ifEmpty { fallback?.cachedItems().orEmpty() }

    override suspend fun awaitCachedItems(): List<TerminalItem> = snapshotLoad.await()
        .ifEmpty { fallback?.awaitCachedItems().orEmpty() }

    override fun refreshInBackground() {
        refresh.start(
            TERMINAL_REFRESH_KEY,
            force = terminalSnapshotNeedsRefresh(),
        ) { loadRemoteItems() }
    }

    override suspend fun items(): List<TerminalItem> =
        refresh.await(
            TERMINAL_REFRESH_KEY,
            force = terminalSnapshotNeedsRefresh(),
        ) { loadRemoteItems() }
            .ifEmpty { awaitCachedItems() }

    override suspend fun detail(item: TerminalItem): TerminalItem = withContext(Dispatchers.IO) {
        val wiki = fetchStarCitizenToolsDetails(item)
        val dataset = datasets.firstOrNull { it.category == item.category }
        val apiEnriched = if (dataset == null || !item.id.matches(Regex("[A-Za-z0-9-]+"))) item else runCatching {
            val request = Request.Builder()
                .url("https://api.star-citizen.wiki/api/${dataset.endpoint}/${item.id}")
                .header("Accept", "application/json")
                .header("User-Agent", "RefugeNext/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("${dataset.endpoint} 详情请求失败：${response.code}")
                val root = JSONObject(response.body?.string().orEmpty())
                val entry = root.optJSONObject("data") ?: error("${dataset.endpoint} 详情为空")
                val remote = terminalItem(entry, item.category, fullPorts = true) ?: error("${dataset.endpoint} 详情无法解析")
                remote.copy(
                    name = remote.name.ifBlank { item.name },
                    manufacturer = remote.manufacturer.takeUnless { it == "—" }.orEmpty().ifBlank { item.manufacturer },
                    tags = (remote.tags + item.tags).distinct().take(3),
                    value = remote.value.takeUnless { it == "—" } ?: item.value,
                    usd = remote.usd.takeUnless { it == "—" } ?: item.usd,
                    description = remote.description.ifBlank { item.description },
                    imageUrl = terminalImage(entry, preferOriginal = true) ?: remote.imageUrl ?: item.imageUrl,
                    imageUrls = (TerminalWikiParser.imageUrls(entry.terminalMap(), preferOriginal = true) + item.imageUrls).distinct(),
                    details = (remote.details + item.details).distinct(),
                )
            }
        }.getOrElse { item }
        val enriched = mergeWikiDetails(apiEnriched, wiki)
        if (enriched != item) {
            val current = lastSuccessful
            if (current.any { it.id == enriched.id }) {
                lastSuccessful = current.map { if (it.id == enriched.id) enriched else it }
            }
        }
        enriched
    }

    private data class StarCitizenToolsDetails(
        val description: String = "",
        val imageUrl: String? = null,
        val rows: List<Pair<String, String>> = emptyList(),
        val sourceUrl: String? = null,
    )

    private fun mergeWikiDetails(item: TerminalItem, wiki: StarCitizenToolsDetails): TerminalItem {
        if (wiki.description.isBlank() && wiki.rows.isEmpty() && wiki.imageUrl == null) return item
        val existing = item.details.associateBy { it.first }.toMutableMap()
        wiki.rows.forEach { (label, value) -> existing.putIfAbsent(label, label to value) }
        val sourceRows = wiki.sourceUrl?.let { listOf("Star Citizen Wiki" to it) }.orEmpty()
        return item.copy(
            description = item.description.ifBlank { wiki.description },
            imageUrl = item.imageUrl ?: wiki.imageUrl,
            imageUrls = (item.imageUrls + listOfNotNull(wiki.imageUrl)).distinct(),
            details = (existing.values + sourceRows).distinct(),
        )
    }

    /** Adds the community Wiki infobox and lead to every terminal item, with a per-item disk cache. */
    private fun fetchStarCitizenToolsDetails(item: TerminalItem): StarCitizenToolsDetails {
        val title = item.name.replace('_', ' ').trim()
        if (title.isBlank()) return StarCitizenToolsDetails()
        val key = title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(120)
        if (key.isBlank()) return StarCitizenToolsDetails()
        val cache = File(wikiDetailDirectory, "$key.json")
        val modified = cache.lastModified()
        if (cache.isFile && System.currentTimeMillis() - modified < 24 * 60 * 60 * 1000L) {
            readWikiDetailsCache(cache, title)?.let { return it }
        }
        val pageUrl = "https://starcitizen.tools/${java.net.URLEncoder.encode(title.replace(' ', '_'), Charsets.UTF_8).replace("+", "_")}"
        val live = runCatching {
            val request = Request.Builder().url(pageUrl)
                .header("User-Agent", "RefugeNext/1.0 (in-app reference reader)")
                .header("Accept", "text/html")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Wiki page ${response.code}")
                val html = response.body?.string().orEmpty()
                parseStarCitizenToolsPage(html, title).also { parsed ->
                    if (parsed.description.isNotBlank() || parsed.rows.isNotEmpty() || parsed.imageUrl != null) {
                        writeWikiDetailsCache(cache, parsed)
                    }
                }
            }
        }.onFailure { Log.w("RefugeTerminal", "Star Citizen Wiki detail failed for $title", it) }
            .getOrNull()
        if (live != null && (live.description.isNotBlank() || live.rows.isNotEmpty() || live.imageUrl != null)) return live
        val className = item.className?.replace('_', ' ')?.trim()?.takeIf { it.isNotBlank() && !it.equals(title, true) }
        if (className != null) {
            val alternate = "https://starcitizen.tools/${java.net.URLEncoder.encode(className.replace(' ', '_'), Charsets.UTF_8).replace("+", "_")}"
            runCatching {
                client.newCall(Request.Builder().url(alternate).header("User-Agent", "RefugeNext/1.0 (in-app reference reader)").build()).execute().use { response ->
                    if (!response.isSuccessful) error("Wiki page ${response.code}")
                    val html = response.body?.string().orEmpty()
                    parseStarCitizenToolsPage(html, className).also { writeWikiDetailsCache(cache, it) }
                }
            }.getOrNull()?.let { return it }
        }
        sequenceOf(title, className)
            .filterNotNull()
            .distinct()
            .mapNotNull(::searchStarCitizenToolsPage)
            .firstOrNull { it.description.isNotBlank() || it.rows.isNotEmpty() || it.imageUrl != null }
            ?.let { return it.also { details -> writeWikiDetailsCache(cache, details) } }
        return readWikiDetailsCache(cache, title) ?: StarCitizenToolsDetails()
    }

    private fun searchStarCitizenToolsPage(query: String): StarCitizenToolsDetails? {
        val encoded = java.net.URLEncoder.encode(query, Charsets.UTF_8)
        val searchUrl = "https://starcitizen.tools/api.php?action=query&list=search&srnamespace=0&srlimit=5&srsearch=$encoded&format=json"
        val candidate = runCatching {
            val request = Request.Builder().url(searchUrl)
                .header("User-Agent", "RefugeNext/1.0 (in-app reference reader)")
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Wiki search ${response.code}")
                val results = JSONObject(response.body?.string().orEmpty())
                    .optJSONObject("query")?.optJSONArray("search") ?: JSONArray()
                (0 until results.length()).mapNotNull { index ->
                    results.optJSONObject(index)?.optString("title")?.takeIf { it.isNotBlank() }
                }.maxByOrNull { wikiTitleSimilarity(query, it) }
            }
        }.getOrNull() ?: return null
        if (wikiTitleSimilarity(query, candidate) < .72) return null
        val pageUrl = "https://starcitizen.tools/${java.net.URLEncoder.encode(candidate.replace(' ', '_'), Charsets.UTF_8).replace("+", "_")}"
        return runCatching {
            val request = Request.Builder().url(pageUrl)
                .header("User-Agent", "RefugeNext/1.0 (in-app reference reader)")
                .header("Accept", "text/html")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Wiki search result ${response.code}")
                parseStarCitizenToolsPage(response.body?.string().orEmpty(), candidate)
            }
        }.onFailure { Log.w("RefugeTerminal", "Star Citizen Wiki search page failed for $query", it) }
            .getOrNull()
    }

    private fun readWikiDetailsCache(file: File, title: String): StarCitizenToolsDetails? = runCatching {
        val raw = file.readText()
        if (!raw.trimStart().startsWith("{")) {
            parseStarCitizenToolsPage(raw, title).also { parsed ->
                if (parsed.description.isNotBlank() || parsed.rows.isNotEmpty() || parsed.imageUrl != null) {
                    writeWikiDetailsCache(file, parsed)
                }
            }
        } else {
            val json = JSONObject(raw)
            val rows = json.optJSONArray("rows") ?: JSONArray()
            val parsedRows = buildList {
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    val label = row.optString("label").trim()
                    val value = row.optString("value").trim()
                    if (label.isNotBlank() && value.isNotBlank()) add(label to value)
                }
            }
            StarCitizenToolsDetails(
                description = json.optString("description"),
                imageUrl = json.optString("imageUrl").takeIf { it.isNotBlank() },
                rows = parsedRows,
                sourceUrl = json.optString("sourceUrl").takeIf { it.isNotBlank() },
            )
        }
    }.getOrNull()

    private fun writeWikiDetailsCache(file: File, details: StarCitizenToolsDetails) {
        val rows = JSONArray().apply {
            details.rows.forEach { (label, value) ->
                put(JSONObject().put("label", label).put("value", value))
            }
        }
        val json = JSONObject()
            .put("description", details.description)
            .put("imageUrl", details.imageUrl)
            .put("sourceUrl", details.sourceUrl)
            .put("rows", rows)
        file.writeText(json.toString())
    }

    private fun parseStarCitizenToolsPage(html: String, title: String): StarCitizenToolsDetails {
        if (html.isBlank()) return StarCitizenToolsDetails()
        val document = Jsoup.parse(html, "https://starcitizen.tools/")
        val content = document.selectFirst(".mw-parser-output") ?: document
        val description = content.select("p").asSequence()
            .map { it.text().trim() }
            .firstOrNull { it.length >= 50 && !it.startsWith("This article") && !it.startsWith("Redirect") }
            .orEmpty()
        val rows = linkedMapOf<String, String>()
        content.select(".t-infobox-item").forEach { node ->
            val label = node.selectFirst(".t-infobox-item-label")?.text()?.trim().orEmpty()
            val valueNode = node.selectFirst(".t-infobox-item-content") ?: return@forEach
            val value = valueNode.text().replace(Regex("\\s+"), " ").trim()
            if (label.isNotBlank() && value.isNotBlank() && !value.equals("unknown", true)) {
                rows.putIfAbsent(wikiLabel(label), value)
            }
        }
        val image = content.select(".t-infobox img[src], .mw-parser-output .thumb img[src], .mw-parser-output figure img[src]")
            .asSequence().mapNotNull { element -> normalizeWikiUrl(element.absUrl("src").ifBlank { element.attr("src") }) }
            .firstOrNull { it.contains("media.starcitizen.tools") }
        val canonicalTitle = document.selectFirst("link[rel=canonical]")?.attr("href")
            ?.takeIf { it.startsWith("https://starcitizen.tools/") }
        val sourceUrl = canonicalTitle ?: "https://starcitizen.tools/${java.net.URLEncoder.encode(title.replace(' ', '_'), Charsets.UTF_8).replace("+", "_")}"
        return StarCitizenToolsDetails(description, image, rows.toList(), sourceUrl)
    }

    private fun wikiLabel(label: String): String = when (label.trim().lowercase()) {
        "type" -> "类型"
        "career" -> "职能"
        "manufacturer" -> "制造商"
        "role" -> "定位"
        "size" -> "尺寸"
        "model" -> "型号"
        "crew" -> "乘员"
        "cargo" -> "货舱"
        "inventory" -> "储物"
        "external cargo" -> "外部货物"
        "max container" -> "最大货箱"
        "stations" -> "工作站"
        "beds" -> "床位"
        "standalone" -> "标准售价"
        "warbond" -> "Warbond 售价"
        "availability" -> "可购买时间"
        "claim time" -> "索赔时间"
        "expedite time" -> "加急时间"
        "expedite fee" -> "加急费用"
        "buy" -> "可购买"
        "rent" -> "可租赁"
        "released" -> "发布年份"
        "announced" -> "公布日期"
        "concept sale" -> "概念销售"
        "flight ready in" -> "可飞行版本"
        "uuid" -> "UUID"
        "class name" -> "内部名称"
        "version" -> "游戏版本"
        "official sites" -> "官方链接"
        "community sites" -> "社区工具"
        "mass" -> "质量"
        "length" -> "长度"
        "width" -> "宽度"
        "height" -> "高度"
        else -> label.trim()
    }

    private fun normalizeWikiUrl(raw: String): String? = runCatching {
        val uri = java.net.URI(raw)
        when {
            uri.scheme == "https" -> uri.toASCIIString()
            raw.startsWith("//") -> "https:$raw"
            raw.startsWith("/") -> "https://starcitizen.tools$raw"
            else -> null
        }
    }.getOrNull()?.takeIf { it.startsWith("https://") }

    /** The repository owns this job, so leaving the Terminal composition cannot
     * cancel parsing before the complete snapshot is written. */
    private suspend fun loadRemoteItems(): List<TerminalItem> = withContext(Dispatchers.IO) {
        snapshotLoad.await()
        runCatching {
            val remote = coroutineScope {
                datasets.chunked(2).flatMap { batch -> batch.map { dataset -> async { fetchDataset(dataset) } }.awaitAll().flatten() }
            }.distinctBy { it.id }
            check(remote.isNotEmpty()) { "Wiki 数据为空" }
            lastSuccessful = remote
            writeTerminalSnapshot(remote)
            val audit = TerminalCategory.entries.joinToString { category ->
                val rows = remote.filter { it.category == category }
                "${category.name}=${rows.size}/images=${rows.count { !it.imageUrl.isNullOrBlank() }}"
            }
            Log.i("RefugeTerminalAudit", "repository=${remote.size} $audit")
            remote
        }.onFailure { Log.w("RefugeTerminal", "Wiki terminal refresh failed", it) }
            .getOrElse { lastSuccessful }
    }

    private fun terminalSnapshotNeedsRefresh(): Boolean {
        val modified = snapshotFile.lastModified()
        return !snapshotFile.isFile ||
            (System.currentTimeMillis() - modified).coerceAtLeast(0L) >= TERMINAL_DAILY_REFRESH_MILLIS
    }

    private fun fetchDataset(dataset: Dataset): List<TerminalItem> {
        val result = mutableListOf<TerminalItem>()
        var page = 1
        var lastPage = 1
        do {
            val filter = dataset.type?.let { "&filter%5Btype%5D=$it" }.orEmpty()
            val url = "https://api.star-citizen.wiki/api/${dataset.endpoint}?page%5Bnumber%5D=$page&page%5Bsize%5D=40$filter"
            val request = Request.Builder().url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "RefugeNext/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("${dataset.endpoint} 请求失败：${response.code}")
                val root = JSONObject(response.body?.string().orEmpty())
                val data = root.optJSONArray("data") ?: error("${dataset.endpoint} 数据为空")
                val meta = root.optJSONObject("meta")
                lastPage = meta?.optInt("last_page", meta.optInt("lastPage", page)) ?: page
                for (index in 0 until data.length()) {
                    data.optJSONObject(index)?.let { entry ->
                        terminalItem(entry, dataset.category)?.let(result::add)
                    }
                }
                if (data.length() == 0) break
            }
            page++
        } while (page <= lastPage && page <= 100)
        Log.i("RefugeTerminalAudit", "category=${dataset.category.name} pages=${page - 1} parsed=${result.size}")
        return result
    }

    private fun terminalItem(entry: JSONObject, category: TerminalCategory, fullPorts: Boolean = false): TerminalItem? {
        val originalName = entry.optString("name").ifBlank { entry.optString("game_name") }.ifBlank { return null }
        val manufacturer = entry.optJSONObject("manufacturer")?.optString("name").orEmpty()
            .ifBlank { entry.optString("manufacturer_name") }
            .takeUnless { it.contains("PLACEHOLDER", ignoreCase = true) }
            .orEmpty()
            .ifBlank { "—" }
        val typeLabel = terminalScalar(entry.opt("type_label")).orEmpty().ifBlank {
            terminalScalar(entry.opt("classification_label")).orEmpty().ifBlank { terminalScalar(entry.opt("role")).orEmpty() }
        }
        val size = terminalScalar(entry.opt("size"))?.let { value ->
            if (value.toDoubleOrNull() != null) "S$value" else value
        }
        val role = entry.optString("career").ifBlank { entry.optString("role") }
        val msrp = entry.opt("msrp")?.toString()?.toDoubleOrNull()?.takeIf { it > 0 }?.let { "\$${it.toInt()}" } ?: "—"
        return TerminalWikiParser.enrich(TerminalItem(
            id = entry.optString("uuid").ifBlank { entry.optString("id") }.ifBlank { "${category.name}:${entry.optString("slug")}:$originalName" },
            name = originalName,
            className = entry.optString("class_name").takeIf { it.isNotBlank() },
            manufacturer = manufacturer,
            category = category,
            tags = listOfNotNull(
                role.takeIf { it.isNotBlank() },
                typeLabel.takeIf { it.isNotBlank() },
                size,
            ).distinct().take(3),
            value = maximumPurchasePrice(entry),
            usd = msrp,
            description = localized(entry.opt("description")).ifBlank { localized(entry.opt("game_description")) },
            imageUrl = terminalImage(entry),
            details = terminalDetails(entry, category),
        ), entry.terminalMap(), fullPorts = fullPorts)
    }


    private fun terminalDetails(entry: JSONObject, category: TerminalCategory): List<Pair<String, String>> {
        val details = mutableListOf<Pair<String, String>>()
        fun add(label: String, value: Any?, suffix: String = "") {
            terminalScalar(value)?.let { details += label to "$it$suffix" }
        }
        fun addRange(label: String, value: JSONObject?, suffix: String = "") {
            value ?: return
            val minimum = terminalScalar(value.opt("min")) ?: terminalScalar(value.opt("minimum"))
            val maximum = terminalScalar(value.opt("max")) ?: terminalScalar(value.opt("maximum"))
            val rendered = when {
                minimum == null && maximum == null -> null
                minimum == null -> maximum
                maximum == null || minimum == maximum -> minimum
                else -> "$minimum - $maximum"
            }
            rendered?.let { details += label to "$it$suffix" }
        }
        add("类型", entry.opt("type_label").takeUnless { terminalScalar(it).isNullOrBlank() } ?: entry.opt("classification_label"))
        add("分类", entry.opt("classification_label").takeUnless { terminalScalar(it).isNullOrBlank() } ?: entry.opt("classification"))
        add("子类型", entry.opt("sub_type_label").takeUnless { terminalScalar(it).isNullOrBlank() } ?: entry.opt("sub_type"))
        add("尺寸", entry.opt("size"))
        add("等级", entry.opt("grade"))
        add("稀有度", entry.opt("rarity"))
        add("质量", entry.opt("mass"), " kg")
        entry.optJSONObject("vehicle_weapon")?.let { weapon ->
            add("武器类型", weapon.opt("type"))
            add("弹容量", weapon.opt("capacity"))
            add("射程", weapon.opt("range"), " m")
            add("每发伤害", weapon.opt("damage_per_shot"))
            add("射速", weapon.opt("rpm"), " RPM")
            weapon.optJSONObject("damage")?.let { damage ->
                add("持续 DPS", damage.opt("sustained_60s"))
                add("爆发 DPS", damage.opt("burst"))
            }
            weapon.optJSONObject("heat")?.let { heat ->
                add("每发热量", heat.opt("per_shot"))
                add("过热弹数", heat.opt("overheat_max_shots"))
                add("冷却速率", heat.opt("cooling_per_second"))
                add("过热冷却时间", heat.opt("overheat_cooldown"), " s")
            }
            weapon.optJSONObject("ammunition")?.let { ammo ->
                add("弹药速度", ammo.opt("speed"), " m/s")
                add("弹药寿命", ammo.opt("lifetime"), " s")
            }
        }
        entry.optJSONObject("vehicle_item")?.let { item ->
            add("部件类型", item.opt("type"))
            add("最大输出", item.opt("power_output"))
            add("冷却速率", item.opt("cooling_rate"))
        }
        entry.optJSONObject("personal_weapon")?.let { weapon ->
            add("武器类别", weapon.opt("class"))
            add("武器类型", weapon.opt("type"))
            add("弹匣容量", weapon.opt("magazine_size").takeUnless { terminalScalar(it).isNullOrBlank() } ?: weapon.opt("capacity"))
            add("弹道射程", weapon.opt("range").takeUnless { terminalScalar(it).isNullOrBlank() } ?: weapon.opt("effective_range"), " m")
            add("每发伤害", weapon.opt("damage_per_shot"))
            add("每发弹丸", weapon.opt("pellets_per_shot"))
            add("射速", weapon.opt("rpm").takeUnless { terminalScalar(it).isNullOrBlank() } ?: weapon.opt("rof"), " RPM")
            add("射击模式", weapon.opt("fire_mode"))
            weapon.optJSONObject("damage")?.let { damage ->
                add("持续 DPS", damage.opt("dps_total"))
                add("单次总伤害", damage.opt("alpha_total"))
                add("最大弹匣伤害", damage.opt("max").takeUnless { terminalScalar(it).isNullOrBlank() } ?: damage.opt("maximum"))
            }
            weapon.optJSONObject("spread")?.let { spread ->
                val minimum = terminalScalar(spread.opt("min").takeUnless { terminalScalar(it).isNullOrBlank() } ?: spread.opt("minimum"))
                val maximum = terminalScalar(spread.opt("max").takeUnless { terminalScalar(it).isNullOrBlank() } ?: spread.opt("maximum"))
                if (minimum != null || maximum != null) details += "腰射散布" to listOfNotNull(minimum, maximum).joinToString(" - ")
            }
            val ammunition = weapon.optJSONObject("ammunition") ?: entry.optJSONObject("ammunition")
            ammunition?.let { ammo ->
                add("弹药速度", ammo.opt("speed"), " m/s")
                add("弹药寿命", ammo.opt("lifetime"), " s")
                add("最大穿透厚度", ammo.opt("max_penetration_thickness"), " m")
                ammo.optJSONObject("damage_drop_min_distance")?.let { falloff ->
                    add("物理衰减起点", falloff.opt("physical"), " m")
                }
                ammo.optJSONObject("damage_drop_min_damage")?.let { falloff ->
                    add("最低物理伤害", falloff.opt("physical"))
                }
            }
        }
        entry.optJSONObject("shield")?.let { shield ->
            add("护盾容量", shield.opt("max_health"))
            add("护盾再生", shield.opt("regen_rate"), "/s")
            add("完全再生时间", shield.opt("regen_time"), " s")
            shield.optJSONObject("regen_delay")?.let { delay ->
                add("受击再生延迟", delay.opt("damage"), " s")
                add("破盾再生延迟", delay.opt("downed"), " s")
            }
            shield.optJSONObject("reserve_pool")?.let { reserve ->
                add("储备池再生", reserve.opt("regen_rate"), "/s")
                add("储备池再生时间", reserve.opt("regen_time"), " s")
            }
            shield.optJSONObject("absorption")?.let { absorption ->
                addRange("物理吸收", absorption.optJSONObject("physical"))
                addRange("能量吸收", absorption.optJSONObject("energy"))
                addRange("畸变吸收", absorption.optJSONObject("distortion"))
            }
        }
        entry.optJSONObject("cooler")?.let { cooler ->
            add("冷却速率", cooler.opt("cooling_rate"))
            add("冷却段生成", cooler.opt("coolant_segment_generation"))
            add("IR 抑制系数", cooler.opt("suppression_ir_factor"))
            add("热抑制系数", cooler.opt("suppression_heat_factor"))
        }
        entry.optJSONObject("power_plant")?.let { powerPlant ->
            add("功率输出", powerPlant.opt("power_output"))
            add("功率段生成", powerPlant.opt("power_segment_generation"))
        }
        entry.optJSONObject("quantum_drive")?.let { quantumDrive ->
            add("量子燃料需求", quantumDrive.opt("quantum_fuel_requirement"))
            add("断连距离", quantumDrive.opt("disconnect_range_formatted"))
            add("燃料消耗", quantumDrive.opt("fuel_consumption_scu_per_gm"), " SCU/Gm")
            add("燃料效率", quantumDrive.opt("fuel_efficiency"))
            quantumDrive.optJSONObject("travel_time_10gm")?.let { travel ->
                add("10 Gm 航行时间", travel.opt("formatted"))
            }
            quantumDrive.optJSONObject("standard_jump")?.let { jump ->
                add("量子速度", jump.opt("drive_speed_formatted"))
                add("一级加速度", jump.opt("stage_one_accel_rate_formatted"))
                add("二级加速度", jump.opt("stage_two_accel_rate_formatted"))
                add("校准后启动", jump.opt("spool_up_time"), " s")
                add("冷却时间", jump.opt("cooldown_time"), " s")
            }
        }
        entry.optJSONObject("resource_network")?.let { network ->
            network.optJSONObject("usage")?.let { usage ->
                addRange("功率用量", usage.optJSONObject("power"), " 段")
                addRange("冷却用量", usage.optJSONObject("coolant"), " 段")
            }
            network.optJSONObject("generation")?.let { generation ->
                add("生成功率", generation.opt("power"), " 段")
                add("生成冷却", generation.opt("coolant"), " 段")
            }
            network.optJSONObject("repair")?.let { repair ->
                add("自修复次数", repair.opt("max_repair_count"))
                add("修复耗时", repair.opt("time_to_repair"), " s")
            }
        }
        entry.optJSONObject("emission")?.let { emission ->
            add("IR 信号", emission.opt("ir"))
            val emMinimum = terminalScalar(emission.opt("em_min"))
            val emMaximum = terminalScalar(emission.opt("em_max"))
            if (emMinimum != null || emMaximum != null) {
                details += "EM 信号" to when {
                    emMinimum == null -> emMaximum.orEmpty()
                    emMaximum == null || emMinimum == emMaximum -> emMinimum
                    else -> "$emMinimum - $emMaximum"
                }
            }
            add("EM 衰减", emission.opt("em_decay"))
        }
        entry.optJSONObject("durability")?.let { durability ->
            add("结构耐久", durability.opt("health"))
            add("可维修", durability.opt("repairable"))
            add("可回收", durability.opt("salvageable"))
        }
        val genericLabels = mapOf(
            "Attachments" to "附件槽",
            "Class" to "类别",
            "Effective Range" to "有效射程",
            "Item Type" to "物品类型",
            "Magazine Size" to "弹匣容量",
            "Manufacturer" to "制造商",
            "Rate Of Fire" to "射速",
        )
        entry.optJSONArray("description_data")?.let { rows ->
            for (index in 0 until rows.length()) {
                rows.optJSONObject(index)?.let { row ->
                    val rawLabel = row.optString("name").trim()
                    val value = terminalScalar(row.opt("value")) ?: terminalScalar(row.opt("type"))
                    if (rawLabel.isNotBlank() && value != null) {
                        details += (genericLabels[rawLabel] ?: rawLabel) to value
                    }
                }
            }
        }
        entry.optJSONObject("dimension")?.let { dimension ->
            add("体积", dimension.opt("volume"), " SCU")
            add("长度", dimension.opt("length"), " m")
            add("宽度", dimension.opt("width"), " m")
            add("高度", dimension.opt("height"), " m")
        }
        return details.distinct()
    }

    private fun terminalScalar(value: Any?): String? {
        val text = when (value) {
            null, JSONObject.NULL -> ""
            is JSONObject -> localized(value)
            is Number -> value.toString().removeSuffix(".0")
            is Boolean -> if (value) "是" else "否"
            else -> value.toString()
        }.trim()
        return text.takeIf { it.isNotBlank() && it != "null" && it != "—" }
    }


    private fun localized(node: Any?): String = when (node) {
        is JSONObject -> listOf("zh_CN", "en_EN", "en_US", "en").firstNotNullOfOrNull { key ->
            node.optString(key).takeIf { it.isNotBlank() }
        } ?: node.optString("text")
        is String -> node
        else -> ""
    }

    private fun maximumPurchasePrice(entry: JSONObject): String {
        val purchase = entry.optJSONObject("uex_prices")?.optJSONArray("purchase") ?: return "—"
        val prices = (0 until purchase.length()).mapNotNull { index ->
            purchase.optJSONObject(index)?.optDouble("price_buy", 0.0)?.takeIf { it > 0 }
        }
        return selectTerminalPurchasePrice(prices)
            ?.let { "${String.format(Locale.US, "%,.0f", it)} aUEC" }
            ?: "—"
    }

    private fun terminalImage(entry: JSONObject, preferOriginal: Boolean = false): String? =
        TerminalWikiParser.imageUrls(entry.terminalMap(), preferOriginal).firstOrNull()

    private fun readTerminalSnapshot(): List<TerminalItem> = runCatching {
        if (!snapshotFile.isFile) return@runCatching emptyList()
        val rows = JSONArray(snapshotFile.readText())
        buildList {
            for (index in 0 until rows.length()) rows.optJSONObject(index)?.let { row ->
                val category = runCatching { TerminalCategory.valueOf(row.optString("category")) }.getOrNull() ?: return@let
                add(
                    TerminalItem(
                        id = row.optString("id"),
                        name = row.optString("name"),
                        className = row.optString("className").takeIf { it.isNotBlank() },
                        manufacturer = row.optString("manufacturer", "—"),
                        category = category,
                        tags = row.optJSONArray("tags")?.let { values ->
                            buildList { for (i in 0 until values.length()) add(values.optString(i)) }
                        }.orEmpty(),
                        value = row.optString("value", "—"),
                        usd = row.optString("usd", "—"),
                        description = row.optString("description"),
                        imageUrl = normalizeImageUrl(row.optString("imageUrl")),
                        details = row.optJSONArray("details")?.let { values ->
                            buildList {
                                for (i in 0 until values.length()) {
                                    values.optJSONObject(i)?.let { add(it.optString("label") to it.optString("value")) }
                                }
                            }
                        }.orEmpty(),
                    ).let { item ->
                        row.optJSONObject("loadoutData")?.let {
                            TerminalWikiParser.enrich(item, it.terminalMap(), fullPorts = true)
                        } ?: item
                    },
                )
            }
        }
    }.onFailure { Log.w("RefugeTerminal", "terminal snapshot read failed", it) }.getOrDefault(emptyList())

    private fun writeTerminalSnapshot(items: List<TerminalItem>) {
        runCatching {
            val rows = JSONArray()
            items.forEach { item ->
                rows.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("name", item.name)
                        .put("className", item.className ?: "")
                        .put("manufacturer", item.manufacturer)
                        .put("category", item.category.name)
                        .put("tags", JSONArray(item.tags))
                        .put("value", item.value)
                        .put("usd", item.usd)
                        .put("description", item.description)
                        .put("imageUrl", item.imageUrl ?: "")
                        .put("loadoutData", JSONObject(TerminalWikiParser.snapshot(item)))
                        .put("details", JSONArray().apply {
                            item.details.forEach { (label, value) -> put(JSONObject().put("label", label).put("value", value)) }
                        }),
                )
            }
            snapshotFile.writeText(rows.toString())
        }.onFailure { Log.w("RefugeTerminal", "terminal snapshot write failed", it) }
    }

    private companion object {
        const val TERMINAL_REFRESH_KEY = "terminal"
        const val TERMINAL_DAILY_REFRESH_MILLIS = 24L * 60L * 60L * 1_000L
    }
}

/** Matches the legacy terminal: show the highest listed UEX purchase price. */
internal fun selectTerminalPurchasePrice(prices: List<Double>): Double? = prices.maxOrNull()

internal fun wikiTitleSimilarity(left: String, right: String): Double {
    fun tokens(value: String) = value.lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim().split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
    val a = tokens(left)
    val b = tokens(right)
    if (a.isEmpty() || b.isEmpty()) return 0.0
    return a.intersect(b).size.toDouble() / a.union(b).size
}

/** Public RSI GraphQL catalog adapter; cache is retained when the storefront changes schema. */
class RsiLiveStoreRepository(
    context: Context,
    private val auth: RsiAuthDataSource,
    private val fallback: StoreRepository,
    private val translation: TranslationRepository,
    private val clock: Clock = Clock.systemUTC(),
) : StoreRepository {
    @Volatile private var lastSuccessful: List<StoreProduct> = emptyList()
    private val snapshotFile = File(context.filesDir, "store_products_usd_v2.json")
    private val snapshotDateFile = File(context.filesDir, "store_products_usd_v2.beijing-date")
    @Volatile private var snapshotBeijingDate: LocalDate? = null
    private val snapshotLoad = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
        snapshotBeijingDate = readStoreSnapshotDate()
        readStoreSnapshot().also { if (it.isNotEmpty()) lastSuccessful = it }
    }
    private val refresh = RepositoryRefresh<List<StoreProduct>>()

    override fun cachedProducts(): List<StoreProduct> = lastSuccessful.ifEmpty { fallback.cachedProducts() }
    override suspend fun awaitCachedProducts(): List<StoreProduct> = snapshotLoad.await().ifEmpty { fallback.cachedProducts() }

    override suspend fun products(): List<StoreProduct> {
        snapshotLoad.await()
        val now = clock.instant()
        val date = storeSnapshotDate(now)
        return refresh.await(
            key = date.toString(),
            force = shouldForceStoreRefresh(snapshotBeijingDate, now),
        ) { loadRemoteProducts() }
    }

    /** Runs independently of a route composition, so switching tabs cannot cancel
     * the catalog refresh before its snapshot is written. */
    private suspend fun loadRemoteProducts(): List<StoreProduct> = withContext(Dispatchers.IO) { runCatching {
        snapshotLoad.await()
        val sources = listOf(
            StoreCategory.SHIPS to listOf("72"),
            StoreCategory.PAINTS to listOf("268"),
            StoreCategory.GEAR to listOf("289", "3", "41", "60", "67"),
            StoreCategory.PACKAGES to listOf("45", "9"),
            StoreCategory.BUNDLES to listOf("270", "46"),
            StoreCategory.SUBSCRIPTIONS to listOf("65"),
        )
        val categoryRows = coroutineScope {
            sources.map { (category, ids) ->
                async(Dispatchers.IO) {
                    val rows = buildList {
                        for (page in 1..50) {
                            val root = auth.storeCatalogPage(page, ids)
                            root.optJSONArray("errors")?.takeIf { it.length() > 0 }?.let { error(it.toString()) }
                            val resources = root.optJSONObject("data")?.optJSONObject("store")
                                ?.optJSONObject("listing")?.optJSONArray("resources")
                                ?: error("RSI 商店目录为空：${category.label}")
                            if (resources.length() == 0) break
                            for (index in 0 until resources.length()) {
                                resources.optJSONObject(index)?.let { add(it) }
                            }
                        }
                    }
                    Log.i("RefugeStoreAudit", "category=${category.name} raw=${rows.size} productIds=$ids")
                    category to rows
                }
            }.awaitAll()
        }
        val remote = buildList {
            categoryRows.forEach { (requestedCategory, resources) ->
                resources.forEachIndexed { index, item ->
                val title = item.optString("title").ifBlank { item.optString("name") }
                if (title.isBlank()) return@forEachIndexed
                val tags = item.optJSONArray("tags")
                val tagText = tags?.let { array -> (0 until array.length()).mapNotNull { array.optJSONObject(it)?.optString("name") }.joinToString(" ") }.orEmpty()
                val type = (item.optString("type") + " " + tagText).lowercase()
                val category = when {
                    "paint" in type || "涂装" in title -> StoreCategory.PAINTS
                    "gear" in type || "equipment" in type || "装备" in title -> StoreCategory.GEAR
                    "package" in type || "游戏包" in title -> StoreCategory.PACKAGES
                    "vehicle" in type || "载具" in title -> StoreCategory.VEHICLES
                    else -> requestedCategory
                }
                val amount = nativeStorePriceCents(item)
                val image = item.optJSONObject("media")?.optJSONObject("thumbnail")?.optString("storeSmall").orEmpty()
                    .takeIf { it.isNotBlank() }
                    ?.let(::rsiAssetUrl)
                    .orEmpty()
                add(StoreProduct(
                    id = item.optString("id").ifBlank { "remote-$index" },
                    title = title,
                    category = category,
                    metadata = item.optString("subtitle").ifBlank { tagText.ifBlank { "RSI 商品" } },
                    // RSI GraphQL exposes monetary amounts in minor units.
                    // The Flutter CatalogProperty uses this integer directly.
                    priceCents = amount,
                    imageUrl = image,
                    description = item.optString("body").ifBlank { item.optString("subtitle") },
                    isWarbond = item.optBoolean("isWarbond", false),
                    isPackage = item.optBoolean("isPackage", false),
                ))
                }
            }
        }
        if (remote.isEmpty()) error("RSI 商店目录没有返回商品")
        lastSuccessful = remote
        writeStoreSnapshot(remote)
        Log.i("RefugeStoreAudit", "repository=${remote.size}")
        remote
    }.getOrElse { error ->
        Log.w("RefugeStoreAudit", "refresh failed; retaining last successful data", error)
        lastSuccessful.ifEmpty { fallback.products() }
    } }

    private fun readStoreSnapshot(): List<StoreProduct> = runCatching {
        if (!snapshotFile.isFile) return@runCatching emptyList()
        val rows = JSONArray(snapshotFile.readText())
        buildList {
            for (index in 0 until rows.length()) rows.optJSONObject(index)?.let { row ->
                val category = runCatching { StoreCategory.valueOf(row.optString("category")) }.getOrNull() ?: return@let
                add(
                    StoreProduct(
                        id = row.optString("id"),
                        title = row.optString("title"),
                        category = category,
                        metadata = row.optString("metadata"),
                        priceCents = row.optInt("priceCents"),
                        imageUrl = row.optString("imageUrl"),
                        description = row.optString("description"),
                        isWarbond = row.optBoolean("isWarbond"),
                        isPackage = row.optBoolean("isPackage"),
                    ),
                )
            }
        }
    }.onFailure { Log.w("RefugeStoreAudit", "store snapshot read failed", it) }.getOrDefault(emptyList())

    private fun writeStoreSnapshot(items: List<StoreProduct>) {
        runCatching {
            val rows = JSONArray()
            items.forEach { item ->
                rows.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("title", item.title)
                        .put("category", item.category.name)
                        .put("metadata", item.metadata)
                        .put("priceCents", item.priceCents)
                        .put("imageUrl", item.imageUrl)
                        .put("description", item.description)
                        .put("isWarbond", item.isWarbond)
                        .put("isPackage", item.isPackage),
                )
            }
            snapshotFile.writeText(rows.toString())
            storeSnapshotDate(clock.instant()).also { refreshedDate ->
                snapshotDateFile.writeText(refreshedDate.toString())
                snapshotBeijingDate = refreshedDate
            }
        }.onFailure { Log.w("RefugeStoreAudit", "store snapshot write failed", it) }
    }

    private fun readStoreSnapshotDate(): LocalDate? = runCatching {
        snapshotDateFile.takeIf(File::isFile)?.readText()?.trim()?.let(LocalDate::parse)
    }.onFailure { Log.w("RefugeStoreAudit", "store snapshot date read failed", it) }.getOrNull()
}

private fun rsiAssetUrl(value: String): String = when {
    value.startsWith("http://", true) || value.startsWith("https://", true) -> value
    value.startsWith("//") -> "https:$value"
    value.startsWith("/") -> "https://robertsspaceindustries.com$value"
    else -> "https://media.robertsspaceindustries.com/$value"
}

/** Read-only live implementations for the original Utility routes. */
class RsiLiveUtilityRepository(
    private val auth: RsiAuthDataSource,
    private val fallback: UtilityRepository,
) : UtilityRepository {
    private val client = OkHttpClient.Builder().build()

    override suspend fun groups(): List<Pair<String, List<ToolItem>>> = withContext(Dispatchers.IO) {
        // The bundled projection lazily parses JSON. Keep that fallback off the
        // Compose dispatcher when Profile is opened before a live snapshot exists.
        fallback.groups()
    }

    override suspend fun detail(toolId: String): ToolDetail = withContext(Dispatchers.IO) {
        when (toolId) {
            "crowdfunding" -> crowdfunding()
            "referrals" -> referralSummary()
            "gift-redeem" -> promotionCodes()
            "social" -> socialSummary()
            else -> fallback.detail(toolId)
        }
    }

    override suspend fun query(toolId: String, input: String): ToolDetail = withContext(Dispatchers.IO) {
        when (toolId) {
            "player-search" -> playerSearch(input)
            else -> detail(toolId)
        }
    }

    private suspend fun crowdfunding(): ToolDetail = withContext(Dispatchers.IO) {
        val latest = getJson("https://api.star-citizen.wiki/api/stats/latest").optJSONObject("data")
            ?: error("众筹数据为空")
        val history = getJson("https://api.star-citizen.wiki/api/stats?page%5Bnumber%5D=1&page%5Bsize%5D=31")
            .optJSONArray("data")
        val newest = latest.optString("funds").toDoubleOrNull()
        val oldest = history?.optJSONObject((history.length() - 1).coerceAtLeast(0))?.optString("funds")?.toDoubleOrNull()
        ToolDetail(
            "crowdfunding",
            listOf(
                "众筹总额" to newest?.let { String.format(Locale.US, "\$%,.2f", it) }.orUnavailable(),
                "公民数" to String.format(Locale.US, "%,d", latest.optLong("fans")),
                "舰队规模" to String.format(Locale.US, "%,d", latest.optLong("fleet")),
                "近30天增量" to if (newest != null && oldest != null) String.format(Locale.US, "+\$%,.2f", newest - oldest) else "—",
                "统计时间" to latest.optString("timestamp").ifBlank { "—" },
            ),
        )
    }

    private suspend fun playerSearch(input: String): ToolDetail {
        val handle = input.trim().takeIf { it.isNotBlank() } ?: error("请输入玩家 Handle")
        val html = auth.getPage("citizens/${android.net.Uri.encode(handle)}")
        if (html.contains("venturing unknown space", true)) error("未找到该玩家")
        // Port the original player_info_parser's DOM semantics. Values from the
        // profile, service-record and organization columns must not be flattened
        // into one positional list because optional Location changes the offsets.
        val profileBlock = Regex(
            "(?is)<div[^>]+class=[\"'][^\"']*\\bprofile\\b[^\"']*\\bleft-col\\b[^\"']*[\"'][^>]*>(.*?)(?=<div[^>]+class=[\"'][^\"']*\\bmain-org\\b)",
        ).find(html)?.groupValues?.getOrNull(1).orEmpty()
        val displayName = Regex(
            "(?is)<p[^>]+class=[\"'][^\"']*\\bentry\\b[^\"']*[\"'][^>]*>\\s*<strong[^>]+class=[\"'][^\"']*\\bvalue\\b[^\"']*[\"'][^>]*>(.*?)</strong>",
        ).find(profileBlock)?.groupValues?.getOrNull(1)?.let(::cleanHtml).orEmpty()
        val publicHandle = labeledValue(profileBlock, "Handle name").ifBlank { handle }
        val enlisted = labeledValue(html, "Enlisted")
        val location = labeledValue(html, "Location")
        val fluency = labeledValue(html, "Fluency")
        val orgBlock = Regex(
            "(?is)<div[^>]+class=[\"'][^\"']*\\bmain-org\\b[^\"']*\\bright-col\\b[^\"']*[\"'][^>]*>(.*?)(?=<div[^>]+class=[\"'][^\"']*\\bbox-footer\\b|<div[^>]+class=[\"'][^\"']*\\bleft-col\\b)",
        ).find(html)?.groupValues?.getOrNull(1).orEmpty()
        val organization = Regex(
            "(?is)<a[^>]+class=[\"'][^\"']*\\bvalue\\b[^\"']*[\"'][^>]*>(.*?)</a>",
        ).find(orgBlock)?.groupValues?.getOrNull(1)?.let(::cleanHtml).orEmpty()
        val organizationRank = labeledValue(orgBlock, "Organization rank")
        return ToolDetail(
            "player-search",
            listOf(
                "显示名" to displayName.orUnavailable(),
                "Handle" to publicHandle,
                "入伍时间" to enlisted.orUnavailable(),
                "所在地" to location.orUnavailable(),
                "语言" to fluency.orUnavailable(),
                "主组织" to organization.ifBlank { "无公开主组织" },
                "组织等级" to organizationRank.orUnavailable(),
            ),
            externalUrl = "https://robertsspaceindustries.com/citizens/${android.net.Uri.encode(handle)}",
        )
    }

    private fun labeledValue(html: String, label: String): String = Regex(
        "(?is)<span[^>]+class=[\"'][^\"']*\\blabel\\b[^\"']*[\"'][^>]*>\\s*${Regex.escape(label)}\\s*</span>.*?" +
            "<(?:strong|span|a)[^>]+class=[\"'][^\"']*\\bvalue\\b[^\"']*[\"'][^>]*>(.*?)</(?:strong|span|a)>",
    ).find(html)?.groupValues?.getOrNull(1)?.let(::cleanHtml).orEmpty()

    private suspend fun referralSummary(): ToolDetail = runCatching {
        val account = auth.accountGraphql().optJSONObject("data")?.optJSONObject("account") ?: error("账户数据为空")
        val campaignRows = coroutineScope {
            listOf("1", "2").flatMap { campaign ->
                listOf(false, true).map { converted ->
                    async(Dispatchers.IO) { auth.referralRecruits(converted, campaign) }
                }
            }.awaitAll()
        }
        val referralLists = campaignRows.mapNotNull { it.optJSONObject("data")?.optJSONObject("referralRecruitsList") }
        val recruits = referralLists.sumOf { it.optInt("recruitsCount", 0) } / 2
        val prospects = referralLists.sumOf { it.optInt("prospectsCount", 0) } / 2
        val people = referralLists.flatMap { list ->
            val data = list.optJSONArray("data") ?: return@flatMap emptyList()
            buildList {
                for (index in 0 until data.length()) {
                    val person = data.optJSONObject(index) ?: continue
                    val name = person.optString("nickname").ifBlank { person.optString("displayName") }
                    if (name.isNotBlank()) add(name to person.optString("enlistedOn").ifBlank { "—" })
                }
            }
        }.distinctBy { it.first }
        ToolDetail(
            "referrals",
            listOf(
                "邀请码" to account.optString("referral_code").ifBlank { "—" },
                "已被邀请" to if (account.optBoolean("hasReferred", false)) "是" else "否",
                "邀请人代码" to account.optString("referrerReferralCode").ifBlank { "—" },
                "已转化邀请" to recruits.toString(),
                "待转化邀请" to prospects.toString(),
            ) + people.take(10).mapIndexed { index, person -> "邀请成员 ${index + 1}" to "${person.first} · ${person.second}" },
            externalUrl = "https://robertsspaceindustries.com/referral-program",
        )
    }.getOrElse { fallback.detail("referrals") }

    private suspend fun socialSummary(): ToolDetail = runCatching {
        val response = auth.spectrumIdentify()
        val data = response.optJSONObject("data") ?: error("Spectrum 好友数据为空")
        val friends = data.optJSONArray("friends") ?: org.json.JSONArray()
        var online = 0
        var playing = 0
        val rows = buildList {
            for (index in 0 until friends.length()) {
                val friend = friends.optJSONObject(index) ?: continue
                val presence = friend.optJSONObject("presence")?.optString("status").orEmpty()
                if (presence.equals("playing", true) || presence.equals("in_game", true)) playing++
                if (presence.isNotBlank() && !presence.equals("offline", true)) online++
                val name = friend.optString("nickname").ifBlank { friend.optString("displayname") }
                add("好友 ${size + 1}" to "$name · ${presence.ifBlank { "离线" }}")
            }
        }
        ToolDetail(
            "social",
            listOf("好友总数" to friends.length().toString(), "在线" to online.toString(), "游戏中" to playing.toString()) + rows,
            externalUrl = "https://robertsspaceindustries.com/spectrum/community/SC",
        )
    }.getOrElse { error ->
        Log.w("RefugeUtilityAudit", "Spectrum identify failed", error)
        fallback.detail("social")
    }

    private suspend fun promotionCodes(): ToolDetail = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("https://biaoju.site:6188/promotion/all")
            .header("User-Agent", "RefugeNext/1.0").build()
        val rows = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("礼物目录请求失败：${response.code}")
            val array = org.json.JSONArray(response.body?.string().orEmpty())
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add("可领取奖励 ${index + 1}" to item.optString("chinese_title").ifBlank { item.optString("code") })
                }
            }
        }
        ToolDetail("gift-redeem", rows.ifEmpty { listOf("状态" to "当前无可领取奖励") })
    }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .header("User-Agent", "RefugeNext/1.0").build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("请求失败：${response.code}")
            JSONObject(response.body?.string().orEmpty())
        }
    }

    private fun cleanHtml(value: String): String = Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
        .toString().replace(Regex("\\s+"), " ").trim()

    private fun String?.orUnavailable(): String = this?.takeIf { it.isNotBlank() } ?: "—"
}

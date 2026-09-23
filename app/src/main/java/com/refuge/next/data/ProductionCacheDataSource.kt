package com.refuge.next.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Parses the versioned legacy-cache import used by production repositories.
 * The parser is isolated from Compose so a network/cache refresh can replace
 * this source without changing page contracts.
 */
class ProductionCacheDataSource(
    context: Context,
    private val assetPath: String = "cache/production_cache.json",
) {
    private val appContext = context.applicationContext
    private val document: JSONObject by lazy {
        appContext.assets.open(assetPath).bufferedReader().use { JSONObject(it.readText()) }
    }

    /**
     * Keep parsed immutable projections in memory. The bundled cache is small
     * today, but every repository can ask for the same projection during the
     * first composition; reparsing JSON there turns a cache hit into main
     * thread work. `cached*` callers remain synchronous for the existing API,
     * while refresh paths can explicitly warm the projections on IO.
     */
    private val cachedOwnedShips = ConcurrentHashMap<Long, List<OwnedShip>>()
    private val cachedHangarItems = ConcurrentHashMap<Long, List<HangarItem>>()
    private val cachedBuyback = ConcurrentHashMap<Long, List<BuybackItem>>()
    @Volatile private var cachedLogs: List<String>? = null
    @Volatile private var cachedStoreProducts: List<StoreProduct>? = null
    @Volatile private var cachedTerminalItems: List<TerminalItem>? = null
    @Volatile private var cachedProfile: ProfileData? = null
    @Volatile private var cachedToolGroups: List<Pair<String, List<ToolItem>>>? = null
    private val cachedCcuShips = ConcurrentHashMap<Long, List<CcuShip>>()
    @Volatile private var cachedOwnedCcu: List<OwnedCcu>? = null
    private val warmScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var warmJob: Job? = null

    // These values version the bundled asset itself. Reading them from the same
    // JSON during composition would parse the whole file before the cache warm
    // job gets a chance to run. Preserve custom-asset behavior for diagnostics.
    fun manifest(): ProductionCacheManifest = if (assetPath == DEFAULT_ASSET_PATH) {
        productionCacheManifest
    } else {
        ProductionCacheManifest(
            version = document.optString("version", productionCacheManifest.version),
            source = document.optString("source", productionCacheManifest.source),
            refreshedAt = document.optString("refreshedAt", productionCacheManifest.refreshedAt),
        )
    }

    fun ownedShips(m80Image: Int, fallbackImage: Int): List<OwnedShip> = cachedOwnedShips[imageKey(m80Image, fallbackImage)] ?: document
        .getJSONObject("hangar")
        .getJSONArray("ownedShips")
        .mapObjects { entry ->
            OwnedShip(
                name = entry.getString("name"),
                packageName = entry.getString("packageName"),
                currentValue = entry.getString("currentValue"),
                paidValue = entry.getString("paidValue"),
                insurance = entry.getString("insurance"),
                imageRes = entry.imageResource(m80Image, fallbackImage),
            )
        }.also { cachedOwnedShips[imageKey(m80Image, fallbackImage)] = it }

    /**
     * Non-blocking projection used by synchronous cache getters. A null value
     * means the background warm-up has not completed yet; callers must not
     * parse the bundled JSON on the Compose thread to fill that gap.
     */
    fun peekOwnedShips(m80Image: Int, fallbackImage: Int): List<OwnedShip>? =
        cachedOwnedShips[imageKey(m80Image, fallbackImage)]

    fun hangarItems(m80Image: Int, fallbackImage: Int): List<HangarItem> = cachedHangarItems[imageKey(m80Image, fallbackImage)] ?: document
        .getJSONObject("hangar")
        .getJSONArray("inventory")
        .mapObjects { entry ->
            HangarItem(
                title = entry.getString("title"),
                price = entry.getString("price"),
                date = entry.getString("date"),
                imageRes = entry.imageResource(m80Image, fallbackImage),
                isGiftable = entry.optBoolean("giftable", true),
                isReclaimable = entry.optBoolean("reclaimable", true),
                originalName = entry.optString("originalName", "—"),
                typeLabel = entry.optString("typeLabel", "本地机库项目"),
                insurance = entry.optString("insurance", "—"),
                currentValue = entry.optString("currentValue", entry.getString("price")),
                savings = entry.optString("savings", "$0"),
                quantity = entry.optInt("quantity", 1).coerceAtLeast(1),
                idList = entry.optJSONArray("idList")?.let { ids ->
                    buildList { for (index in 0 until ids.length()) ids.optLong(index).takeIf { it > 0 }?.let(::add) }
                }?.takeIf { it.isNotEmpty() } ?: if (entry.optLong("id") > 0) listOf(entry.optLong("id")) else emptyList(),
                includedItems = entry.optJSONArray("includedItems")?.toStringList().orEmpty(),
                upgradeFrom = entry.optNullableString("upgradeFrom"),
                upgradeTo = entry.optNullableString("upgradeTo"),
                upgradeFromPrice = entry.optNullableString("upgradeFromPrice"),
                upgradeToPrice = entry.optNullableString("upgradeToPrice"),
            )
        }.also { cachedHangarItems[imageKey(m80Image, fallbackImage)] = it }

    fun peekHangarItems(m80Image: Int, fallbackImage: Int): List<HangarItem>? =
        cachedHangarItems[imageKey(m80Image, fallbackImage)]

    fun buyback(m80Image: Int, fallbackImage: Int): List<BuybackItem> = cachedBuyback[imageKey(m80Image, fallbackImage)] ?: document
        .getJSONArray("buyback")
        .mapObjects { entry ->
            BuybackItem(
                title = entry.getString("title"),
                price = entry.getString("price"),
                date = entry.getString("date"),
                imageRes = entry.imageResource(m80Image, fallbackImage),
                originalName = entry.optString("originalName", "—"),
                isUpgrade = entry.optBoolean("isUpgrade", false),
            )
        }.also { cachedBuyback[imageKey(m80Image, fallbackImage)] = it }

    fun peekBuyback(m80Image: Int, fallbackImage: Int): List<BuybackItem>? =
        cachedBuyback[imageKey(m80Image, fallbackImage)]

    fun logs(): List<String> = cachedLogs ?: document.getJSONArray("logs").toStringList().also { cachedLogs = it }

    fun peekLogs(): List<String>? = cachedLogs

    fun storeProducts(): List<StoreProduct> = cachedStoreProducts ?: document
        .getJSONArray("store")
        .mapObjects { entry ->
            StoreProduct(
                id = entry.getString("id"),
                title = entry.getString("title"),
                category = StoreCategory.valueOf(entry.getString("category")),
                metadata = entry.getString("metadata"),
                priceCents = entry.getInt("priceCents"),
                imageUrl = entry.getString("imageUrl"),
                description = entry.getString("description"),
                isWarbond = entry.optBoolean("isWarbond", false),
                isPackage = entry.optBoolean("isPackage", false),
            )
        }.also { cachedStoreProducts = it }

    fun peekStoreProducts(): List<StoreProduct>? = cachedStoreProducts

    fun terminalItems(): List<TerminalItem> = cachedTerminalItems ?: document
        .getJSONArray("terminal")
        .mapObjects { entry ->
            TerminalItem(
                id = entry.getString("id"),
                name = entry.getString("name"),
                manufacturer = entry.getString("manufacturer"),
                className = entry.optNullableString("className") ?: entry.optNullableString("class_name"),
                category = TerminalCategory.valueOf(entry.getString("category")),
                tags = entry.getJSONArray("tags").toStringList(),
                value = entry.getString("value"),
                usd = entry.getString("usd"),
                description = entry.getString("description"),
                imageUrl = entry.optNullableString("imageUrl"),
                details = entry.optJSONArray("details")?.let { values ->
                    buildList {
                        for (index in 0 until values.length()) {
                            values.optJSONObject(index)?.let { row ->
                                val label = row.optString("label").trim()
                                val value = row.optString("value").trim()
                                if (label.isNotBlank() && value.isNotBlank()) add(label to value)
                            }
                        }
                    }
                }.orEmpty(),
            )
        }.also { cachedTerminalItems = it }

    fun peekTerminalItems(): List<TerminalItem>? = cachedTerminalItems

    fun profile(): ProfileData = cachedProfile ?: document.getJSONObject("profile").let { entry ->
        sanitizeCachedProfile(ProfileData(
            handle = entry.optString("handle", "RSI 账户"),
            displayName = entry.optString("displayName").takeIf { it.isNotBlank() },
            city = entry.optString("city", "—"),
            rank = entry.optString("rank", "—"),
            organizationId = entry.optString("organizationId").takeIf { it.isNotBlank() },
            organizationName = entry.optString("organizationName").takeIf { it.isNotBlank() },
            organizationRank = entry.optString("organizationRank").takeIf { it.isNotBlank() },
            organizationLevel = entry.optInt("organizationLevel", 0),
            organizationImage = entry.optString("organizationImage").takeIf { it.isNotBlank() },
            level = entry.optString("level", "—"),
            totalSpent = entry.optString("totalSpent", "—"),
            hangarValue = entry.optString("hangarValue", "—"),
            credit = entry.optString("credit", "—"),
            registerDate = entry.optString("registerDate", "—"),
            uec = entry.optString("uec", "—"),
            rec = entry.optString("rec", "—"),
            currentValue = entry.optString("currentValue", "—"),
            referralCode = entry.optString("referralCode", "—"),
        )).also { cachedProfile = it }
    }

    fun peekProfile(): ProfileData? = cachedProfile

    fun toolGroups(): List<Pair<String, List<ToolItem>>> = cachedToolGroups ?: document
        .getJSONArray("toolGroups")
        .mapObjects { group ->
            group.getString("title") to group.getJSONArray("items").mapObjects { item ->
                ToolItem(item.getString("id"), item.getString("title"), item.getString("subtitle"))
            }
        }.also { cachedToolGroups = it }

    fun peekToolGroups(): List<Pair<String, List<ToolItem>>>? = cachedToolGroups

    fun toolDetail(toolId: String): ToolDetail? = document
        .getJSONObject("toolDetails")
        .optJSONObject(toolId)
        ?.let { entry ->
            ToolDetail(
                toolId = toolId,
                rows = entry.getJSONArray("rows").mapObjects { row ->
                    row.getString("label") to row.getString("value")
                },
                externalUrl = entry.optNullableString("externalUrl"),
            )
        }

    fun ccuShips(m80Image: Int, fallbackImage: Int): List<CcuShip> = cachedCcuShips[imageKey(m80Image, fallbackImage)] ?: document
        .getJSONObject("ccu")
        .getJSONArray("ships")
        .mapObjects { entry ->
            CcuShip(
                id = entry.getString("id"),
                name = entry.getString("name"),
                purchasePrice = entry.getInt("purchasePrice"),
                imageRes = entry.imageResource(m80Image, fallbackImage),
            )
        }.also { cachedCcuShips[imageKey(m80Image, fallbackImage)] = it }

    fun peekCcuShips(m80Image: Int, fallbackImage: Int): List<CcuShip>? =
        cachedCcuShips[imageKey(m80Image, fallbackImage)]

    fun ownedCcu(): List<OwnedCcu> = cachedOwnedCcu ?: document
        .getJSONObject("ccu")
        .getJSONArray("owned")
        .mapObjects { entry ->
            OwnedCcu(
                id = entry.getString("id"),
                title = entry.getString("title"),
                purchasePrice = entry.getInt("purchasePrice"),
                appliedTo = entry.getString("appliedTo"),
                fromShip = entry.optString("fromShip").ifBlank {
                    entry.getString("title").substringBefore(" → ").substringBefore(" to ").trim()
                },
                toShip = entry.optString("toShip").ifBlank { entry.getString("appliedTo") },
            )
        }.also { cachedOwnedCcu = it }

    fun peekOwnedCcu(): List<OwnedCcu>? = cachedOwnedCcu

    /** Warm every bundled projection off the main thread before a route asks
     * for a synchronous cached value. This never performs network work. */
    suspend fun warmCaches(m80Image: Int, fallbackImage: Int) = withContext(Dispatchers.IO) {
        ownedShips(m80Image, fallbackImage)
        hangarItems(m80Image, fallbackImage)
        buyback(m80Image, fallbackImage)
        logs()
        storeProducts()
        terminalItems()
        profile()
        toolGroups()
        ccuShips(m80Image, fallbackImage)
        ownedCcu()
    }

    /**
     * Start cache parsing without making the caller wait. Multiple fallback
     * repositories share this data source, so only one warm-up job is needed.
     */
    fun warmCachesInBackground(m80Image: Int, fallbackImage: Int) {
        synchronized(this) {
            // Avoid a non-local return from the inline synchronized block;
            // newer Kotlin compilers reject it when this function is called
            // through an inline repository projection.
            if (warmJob?.isActive != true) {
                warmJob = warmScope.launch { runCatching { warmCaches(m80Image, fallbackImage) } }
            }
        }
    }

    private companion object {
        const val DEFAULT_ASSET_PATH = "cache/production_cache.json"
    }
}

private fun imageKey(primary: Int, fallback: Int): Long =
    (primary.toLong() shl 32) xor (fallback.toLong() and 0xffffffffL)

private fun JSONObject.imageResource(m80Image: Int, fallbackImage: Int): Int =
    if (optString("image", "fallback") == "m80") m80Image else fallbackImage

private fun JSONObject.optNullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

private fun JSONArray.toStringList(): List<String> = buildList {
    for (index in 0 until length()) add(getString(index))
}

private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> = buildList {
    for (index in 0 until length()) add(transform(getJSONObject(index)))
}

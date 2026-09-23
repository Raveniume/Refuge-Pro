package com.refuge.next.data

data class OwnedShip(
    val name: String,
    val packageName: String,
    val currentValue: String,
    val paidValue: String,
    val insurance: String,
    val imageRes: Int,
    val imageUrl: String? = null,
    val sourceItemId: Long = 0,
    val shipId: Int? = null,
)

data class HangarItem(
    val title: String,
    val price: String,
    val date: String,
    val imageRes: Int,
    val isGiftable: Boolean = true,
    val isReclaimable: Boolean = true,
    val originalName: String = "—",
    val typeLabel: String = "本地机库项目",
    val insurance: String = "—",
    val currentValue: String = price,
    val savings: String = "$0",
    val includedItems: List<String> = emptyList(),
    val upgradeFrom: String? = null,
    val upgradeTo: String? = null,
    val upgradeFromPrice: String? = null,
    val upgradeToPrice: String? = null,
    val includedEntries: List<HangarIncludedItem> = emptyList(),
    /** Ship contained by a package row when RSI does not expose a typed child item. */
    val containedShip: String? = null,
    val imageUrl: String? = null,
    val id: Long = 0,
    val status: String = "—",
    val canUpgrade: Boolean = false,
    val isUpgrade: Boolean = false,
    val resolvedFinalShip: String? = containedShip,
    val page: Int = 0,
    val shipId: Int? = null,
    val quantity: Int = 1,
    val idList: List<Long> = if (id > 0) listOf(id) else emptyList(),
)

data class HangarIncludedItem(
    val title: String,
    val imageUrl: String? = null,
    val kind: String = "",
    val subtitle: String = "",
    val value: String = "—",
)

/**
 * Featured cards represent ships currently owned through a standalone pledge
 * or package. An unapplied CCU is an inventory asset, not an owned ship, even
 * when its destination can be resolved to a ship.
 */
fun heroShipName(item: HangarItem): String? = item.containedShip
    ?.takeUnless { item.isUpgrade }
    ?.trim()
    ?.takeIf { it.isNotBlank() }

fun isHeroShipCandidate(item: HangarItem): Boolean = heroShipName(item) != null

interface HangarRepository {
    suspend fun refreshInventory(): List<HangarItem> = error("当前数据源不支持实时机库刷新")
    suspend fun reclaim(request: HangarReclaimRequest, deviceVerified: Boolean): HangarReclaimResult =
        error("当前数据源不支持回收")
    suspend fun ownedShips(): List<OwnedShip>
    suspend fun inventory(): List<HangarItem>
    fun cachedOwnedShips(): List<OwnedShip> = emptyList()
    fun cachedInventory(): List<HangarItem> = emptyList()
    suspend fun awaitCachedOwnedShips(): List<OwnedShip> = cachedOwnedShips()
    suspend fun awaitCachedInventory(): List<HangarItem> = cachedInventory()
    fun cachedUpgradeTargets(upgradeId: Long): List<HangarItem> = emptyList()
    suspend fun upgradeTargets(upgradeId: Long): List<HangarItem> = error("Upgrade eligibility is unavailable")
}

/** Versioned read-only adapter for the bundled legacy cache import. */
class ProductionHangarRepository(
    private val fallbackImage: Int,
    private val m80Image: Int = fallbackImage,
    private val source: ProductionCacheDataSource? = null,
) : HangarRepository {
    init {
        // Begin parsing the bundled cache on IO as soon as the shared fallback
        // repository is constructed. Cached getters stay synchronous for the
        // existing contract, but normally hit the warmed projections by the
        // time the first route composes.
        source?.warmCachesInBackground(m80Image, fallbackImage)
    }

    override suspend fun ownedShips() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        source?.ownedShips(m80Image, fallbackImage).orEmpty()
    }

    override suspend fun inventory() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        source?.hangarItems(m80Image, fallbackImage).orEmpty()
    }

    override fun cachedOwnedShips() = source?.peekOwnedShips(m80Image, fallbackImage).orEmpty()
    override fun cachedInventory() = source?.peekHangarItems(m80Image, fallbackImage).orEmpty()
    override suspend fun awaitCachedOwnedShips() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        ownedShipsSnapshot()
    }
    override suspend fun awaitCachedInventory() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        inventorySnapshot()
    }

    private fun ownedShipsSnapshot() = source?.ownedShips(m80Image, fallbackImage).orEmpty()

    private fun inventorySnapshot() = source?.hangarItems(m80Image, fallbackImage) ?: emptyList()
}

data class BuybackItem(
    val title: String,
    val price: String,
    val date: String,
    val imageRes: Int,
    val originalName: String = "—",
    val isUpgrade: Boolean = false,
    val imageUrl: String? = null,
    val contains: List<String> = emptyList(),
    val id: Long = 0,
    val quantity: Int = 1,
    val idList: List<Long> = if (id > 0) listOf(id) else emptyList(),
    val fromShipId: Long = 0,
    val toShipId: Long = 0,
    val toSkuId: Long = 0,
)

interface BuybackRepository {
    suspend fun items(): List<BuybackItem>
    fun cachedItems(): List<BuybackItem> = emptyList()
    suspend fun awaitCachedItems(): List<BuybackItem> = cachedItems()
}

/** Read-only adapter for the bundled legacy buyback cache contract. */
class ProductionBuybackRepository(
    private val m80Image: Int,
    private val fallbackImage: Int,
    private val source: ProductionCacheDataSource? = null,
) : BuybackRepository {
    override suspend fun items(): List<BuybackItem> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        source?.buyback(m80Image, fallbackImage).orEmpty()
    }
    override fun cachedItems(): List<BuybackItem> = source?.peekBuyback(m80Image, fallbackImage).orEmpty()
    override suspend fun awaitCachedItems(): List<BuybackItem> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        source?.buyback(m80Image, fallbackImage).orEmpty()
    }
}

interface HangarLogRepository {
    fun cachedEntries(): List<HangarLogEntry> = emptyList()
    suspend fun awaitCachedEntries(): List<HangarLogEntry> = cachedEntries()
    suspend fun entries(): List<HangarLogEntry>
}

data class HangarLogEntry(
    val id: String,
    val timeMillis: Long = 0,
    val type: String = "UNKNOWN",
    val name: String,
    val priceCents: Int? = null,
    val source: String? = null,
    val target: String? = null,
    val operator: String = "CIG",
    val reason: String? = null,
    val order: String? = null,
    val rawContent: String = "",
) {
    fun belongsTo(itemId: Long): Boolean = itemId > 0 && target?.toLongOrNull() == itemId
}

/** Read-only adapter for the imported legacy log records. */
class ProductionHangarLogRepository(
    private val source: ProductionCacheDataSource? = null,
) : HangarLogRepository {
    override fun cachedEntries(): List<HangarLogEntry> = source?.peekLogs().orEmpty().mapIndexed { index, entry ->
        HangarLogEntry(id = "legacy-$index", name = entry, rawContent = entry)
    }
    override suspend fun awaitCachedEntries(): List<HangarLogEntry> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        source?.logs().orEmpty().mapIndexed { index, entry ->
            HangarLogEntry(id = "legacy-$index", name = entry, rawContent = entry)
        }
    }
    override suspend fun entries(): List<HangarLogEntry> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        source?.logs().orEmpty().mapIndexed { index, entry ->
            HangarLogEntry(id = "legacy-$index", name = entry, rawContent = entry)
        }
    }
}

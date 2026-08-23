package com.refuge.next.data

data class OwnedShip(
    val name: String,
    val packageName: String,
    val currentValue: String,
    val paidValue: String,
    val insurance: String,
    val imageRes: Int,
    val imageUrl: String? = null,
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
)

data class HangarIncludedItem(
    val title: String,
    val imageUrl: String? = null,
)

interface HangarRepository {
    suspend fun ownedShips(): List<OwnedShip>
    suspend fun inventory(): List<HangarItem>
    fun cachedOwnedShips(): List<OwnedShip> = emptyList()
    fun cachedInventory(): List<HangarItem> = emptyList()
}

/** Versioned read-only adapter for the bundled legacy cache import. */
class ProductionHangarRepository(
    private val fallbackImage: Int,
    private val m80Image: Int = fallbackImage,
    private val source: ProductionCacheDataSource? = null,
) : HangarRepository {
    override suspend fun ownedShips() = source?.ownedShips(m80Image, fallbackImage) ?: listOf(
        OwnedShip("M80", "游戏包 - 公民新手包", "$300", "$140", "LTI", m80Image),
    )

    override suspend fun inventory() = source?.hangarItems(m80Image, fallbackImage) ?: listOf(
        HangarItem("装备包 - SteelTek - 掳绑包", "$30", "2026年08月16日", fallbackImage, originalName = "SteelTek Armor Set", typeLabel = "装备 / 包含物品", includedItems = listOf("SteelTek 装备包", "数字物品")),
        HangarItem("涂装包 - M80 - Dynasty Paint", "$7.50", "2026年08月12日", fallbackImage, originalName = "M80 Dynasty Paint", typeLabel = "Paint", includedItems = listOf("M80 专用涂装")),
        HangarItem("毛线帽套装 - 莫基节新手指导奖励", "$0", "2026年08月07日", fallbackImage, originalName = "MobiGlas Tutorial Reward", typeLabel = "个人物品", isGiftable = false, isReclaimable = false, includedItems = listOf("毛线帽套装")),
        HangarItem("M80 - 公民新手包", "$140", "2026年08月02日", m80Image, originalName = "Origin M80 Starter Package", typeLabel = "游戏包 / 舰船", insurance = "LTI", currentValue = "$300", savings = "$160", includedItems = listOf("M80", "星际公民数字下载", "LTI 保险"), upgradeFrom = "Aurora ES", upgradeTo = "M80", upgradeFromPrice = "$20", upgradeToPrice = "$300"),
        HangarItem("舰船组件 - 轻型量子驱动", "$25", "2026年07月22日", fallbackImage, originalName = "Light Quantum Drive", typeLabel = "Weapon / Component", includedItems = listOf("量子驱动", "S1 组件")),
    )

    override fun cachedOwnedShips() = ownedShipsSnapshot()
    override fun cachedInventory() = inventorySnapshot()

    private fun ownedShipsSnapshot() = source?.ownedShips(m80Image, fallbackImage) ?: listOf(
        OwnedShip("M80", "游戏包 - 公民新手包", "$300", "$140", "LTI", m80Image),
    )

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
)

interface BuybackRepository {
    suspend fun items(): List<BuybackItem>
}

/** Read-only adapter for the bundled legacy buyback cache contract. */
class ProductionBuybackRepository(
    private val m80Image: Int,
    private val fallbackImage: Int,
    private val source: ProductionCacheDataSource? = null,
) : BuybackRepository {
    override suspend fun items(): List<BuybackItem> = source?.buyback(m80Image, fallbackImage) ?: listOf(
        BuybackItem("M50 - 公民新手包", "$60", "2026年07月18日", m80Image, "Origin M50 Starter Package"),
        BuybackItem("装备包 - RSI", "$3.50", "2026年06月29日", fallbackImage, "RSI Equipment Pack"),
        BuybackItem("极光 Mk I ES", "$20", "2026年05月12日", fallbackImage, "Aurora Mk I ES"),
    )
}

interface HangarLogRepository {
    suspend fun entries(): List<String>
}

/** Read-only adapter for the imported legacy log records. */
class ProductionHangarLogRepository(
    private val source: ProductionCacheDataSource? = null,
) : HangarLogRepository {
    override suspend fun entries(): List<String> = source?.logs() ?: listOf(
        "CREATED · M80 · 2026-08-02",
        "GIFT · SteelTek 装备包 · 2026-08-16",
        "APPLIED_UPGRADE · M80 · 2026-08-18",
    )
}

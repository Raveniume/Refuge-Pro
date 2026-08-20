package com.refuge.next.data

data class OwnedShip(
    val name: String,
    val packageName: String,
    val currentValue: String,
    val paidValue: String,
    val insurance: String,
    val imageRes: Int,
)

data class HangarItem(
    val title: String,
    val price: String,
    val date: String,
    val imageRes: Int,
    val isGiftable: Boolean = true,
    val isReclaimable: Boolean = true,
)

interface HangarRepository {
    suspend fun ownedShips(): List<OwnedShip>
    suspend fun inventory(): List<HangarItem>
}

/**
 * Preview adapter for Slice 1. The production adapter will map the existing
 * Flutter API/cache contracts into these immutable presentation models.
 */
class PreviewHangarRepository(
    private val fallbackImage: Int,
) : HangarRepository {
    override suspend fun ownedShips() = listOf(
        OwnedShip("M80", "游戏包 - 公民新手包", "$300", "$140", "LTI", fallbackImage),
    )

    override suspend fun inventory() = listOf(
        HangarItem("装备包 - SteelTek - 掳绑包", "$30", "2026年08月16日", fallbackImage),
        HangarItem("涂装包 - M80 - Dynasty Paint", "$7.50", "2026年08月12日", fallbackImage),
        HangarItem("毛线帽套装 - 莫基节新手指导奖励", "$0", "2026年08月07日", fallbackImage),
        HangarItem("M80 - 公民新手包", "$140", "2026年08月02日", fallbackImage),
        HangarItem("舰船组件 - 轻型量子驱动", "$25", "2026年07月22日", fallbackImage),
    )
}

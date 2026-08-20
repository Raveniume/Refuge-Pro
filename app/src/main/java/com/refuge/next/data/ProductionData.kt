package com.refuge.next.data

import kotlinx.coroutines.delay

data class TerminalItem(
    val id: String,
    val name: String,
    val manufacturer: String,
    val category: TerminalCategory,
    val tags: List<String>,
    val value: String,
    val usd: String,
    val description: String,
)

enum class TerminalCategory(val label: String) {
    VEHICLES("载具"),
    SHIP_COMPONENTS("舰载"),
    PERSONAL("单兵"),
    ATTACHMENTS("配件"),
    SHIELDS("护盾"),
}

interface TerminalRepository {
    suspend fun items(): List<TerminalItem>
}

class CachedTerminalRepository : TerminalRepository {
    override suspend fun items(): List<TerminalItem> {
        delay(360)
        return terminalSnapshot
    }
}

private val terminalSnapshot = listOf(
    TerminalItem("v-m80", "M80", "Origin", TerminalCategory.VEHICLES, listOf("战斗", "高速"), "—", "$300", "Origin M80 是一艘以速度和机动性为核心的轻型战斗舰。"),
    TerminalItem("v-aurora", "极光 Mk I ES", "RSI", TerminalCategory.VEHICLES, listOf("入门", "多用途"), "—", "$20", "Aurora 系列提供稳定的基础运输与探索能力。"),
    TerminalItem("v-atls", "ATLS", "Argo", TerminalCategory.VEHICLES, listOf("工业", "装卸"), "—", "$40", "面向工业任务的动力装甲平台。"),
    TerminalItem("c-quantum", "VK-00 量子引擎", "Wei-Tek", TerminalCategory.SHIP_COMPONENTS, listOf("量子", "S1"), "38,400 aUEC", "$12", "适配小型舰船的高推力量子引擎。"),
    TerminalItem("c-shield", "FR-66 护盾", "Aegis", TerminalCategory.SHIP_COMPONENTS, listOf("护盾", "S2"), "12,850 aUEC", "$8", "兼顾再生速度和容量的舰载护盾。"),
    TerminalItem("p-pistol", "Arclight II 手枪", "BEHR", TerminalCategory.PERSONAL, listOf("手枪", "能量"), "4,200 aUEC", "$3", "紧凑的个人能量武器。"),
    TerminalItem("a-mount", "S1 武器挂架", "Greycat", TerminalCategory.ATTACHMENTS, listOf("挂架", "S1"), "1,600 aUEC", "$2", "用于小型武器接口的标准挂架。"),
    TerminalItem("s-parapet", "Parapet 护盾", "Dale", TerminalCategory.SHIELDS, listOf("护盾", "S3"), "32,700 aUEC", "$18", "为中型舰船提供额外抗性。"),
)

data class ProfileData(
    val handle: String = "Raveniume",
    val city: String = "星环城",
    val rank: String = "Experienced",
    val isOnline: Boolean = true,
    val totalSpent: String = "$140",
    val hangarValue: String = "$300",
    val credit: String = "$60",
    val registerDate: String = "2023-06-18",
    val uec: String = "128,400",
    val rec: String = "2,960",
    val currentValue: String = "$300",
    val referralCode: String = "RAVEN-7K2Q",
)

data class ToolItem(val id: String, val title: String, val subtitle: String)

val toolGroups: List<Pair<String, List<ToolItem>>> = listOf(
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
    "测试中心" to listOf(
        ToolItem("test-center", "测试中心", "验证本地缓存、Glass 和运行状态"),
        ToolItem("rsi", "RSI 快捷入口", "打开常用 RSI 资料入口"),
    ),
)

data class CcuShip(val id: String, val name: String, val purchasePrice: Int, val imageRes: Int)

data class OwnedCcu(val id: String, val title: String, val purchasePrice: Int, val appliedTo: String)

fun eligibleTargetShips(seed: CcuShip, ships: Iterable<CcuShip>): List<CcuShip> =
    ships.filter { it.purchasePrice > seed.purchasePrice }

data class CcuPlan(
    val seed: CcuShip,
    val target: CcuShip,
    val owned: List<OwnedCcu>,
    val additionalCost: Int,
) {
    val shipValue: Int
        get() = calculateShipValue(seed.purchasePrice, owned.map { it.purchasePrice }, additionalCost)
}

fun calculateRemainingPayment(seed: CcuShip, target: CcuShip, owned: Iterable<OwnedCcu>): Int =
    (target.purchasePrice - seed.purchasePrice - owned.sumOf { it.purchasePrice }).coerceAtLeast(0)

/** Business value uses actual purchase prices, never MSRP or credited delta values. */
fun calculateShipValue(
    seedPurchasePrice: Int,
    ownedCcuPurchasePrices: Iterable<Int>,
    remainingPayment: Int,
): Int = seedPurchasePrice + ownedCcuPurchasePrices.sum() + remainingPayment

fun formatUsd(cents: Int): String = "$${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

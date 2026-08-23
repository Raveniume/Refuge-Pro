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
    val imageUrl: String? = null,
)

enum class TerminalCategory(val label: String) {
    VEHICLES("载具"),
    SHIP_COMPONENTS("舰载"),
    PERSONAL("单兵"),
    ATTACHMENTS("配件"),
    SHIELDS("护盾"),
    VEHICLE_WEAPONS("舰武"),
    COOLERS("冷却"),
    POWER_PLANTS("电站"),
    QUANTUM_DRIVES("量子"),
}

interface TerminalRepository {
    suspend fun items(): List<TerminalItem>
}

class ProductionTerminalRepository(
    private val source: ProductionCacheDataSource? = null,
) : TerminalRepository {
    override suspend fun items(): List<TerminalItem> {
        delay(360)
        return source?.terminalItems() ?: productionTerminalCache
    }
}

private val productionTerminalCache = listOf(
    TerminalItem("v-m80", "M80", "Origin", TerminalCategory.VEHICLES, listOf("战斗", "高速"), "—", "$300", "Origin M80 是一艘以速度和机动性为核心的轻型战斗舰。"),
    TerminalItem("v-aurora", "极光 Mk I ES", "RSI", TerminalCategory.VEHICLES, listOf("入门", "多用途"), "—", "$20", "Aurora 系列提供稳定的基础运输与探索能力。"),
    TerminalItem("v-atls", "ATLS", "Argo", TerminalCategory.VEHICLES, listOf("工业", "装卸"), "—", "$40", "面向工业任务的动力装甲平台。"),
    TerminalItem("c-quantum", "VK-00 量子引擎", "Wei-Tek", TerminalCategory.SHIP_COMPONENTS, listOf("量子", "S1"), "38,400 aUEC", "$12", "适配小型舰船的高推力量子引擎。"),
    TerminalItem("c-shield", "FR-66 护盾", "Aegis", TerminalCategory.SHIP_COMPONENTS, listOf("护盾", "S2"), "12,850 aUEC", "$8", "兼顾再生速度和容量的舰载护盾。"),
    TerminalItem("p-pistol", "Arclight II 手枪", "BEHR", TerminalCategory.PERSONAL, listOf("手枪", "能量"), "4,200 aUEC", "$3", "紧凑的个人能量武器。"),
    TerminalItem("a-mount", "S1 武器挂架", "Greycat", TerminalCategory.ATTACHMENTS, listOf("挂架", "S1"), "1,600 aUEC", "$2", "用于小型武器接口的标准挂架。"),
    TerminalItem("s-parapet", "Parapet 护盾", "Dale", TerminalCategory.SHIELDS, listOf("护盾", "S3"), "32,700 aUEC", "$18", "为中型舰船提供额外抗性。"),
    TerminalItem("w-mantis", "Mantis 导弹发射器", "Behring", TerminalCategory.VEHICLE_WEAPONS, listOf("导弹", "S2"), "18,200 aUEC", "$14", "适配中型挂点的标准导弹发射器。"),
    TerminalItem("w-bulldog", "S2 Bulldog 加特林", "Apocalypse Arms", TerminalCategory.VEHICLE_WEAPONS, listOf("机炮", "S2"), "21,500 aUEC", "$16", "面向近距离持续火力的舰载武器。"),
    TerminalItem("cool-iceman", "Iceman 冷却器", "CoolCore", TerminalCategory.COOLERS, listOf("冷却", "S1"), "9,800 aUEC", "$7", "为小型舰船提供稳定的热管理能力。"),
    TerminalItem("power-guardian", "Guardian 电站", "J-Precision", TerminalCategory.POWER_PLANTS, listOf("电站", "S2"), "16,400 aUEC", "$11", "平衡功率输出与组件负载的舰载电站。"),
    TerminalItem("qd-hem", "Hemera 量子引擎", "Wei-Tek", TerminalCategory.QUANTUM_DRIVES, listOf("量子", "S2"), "44,000 aUEC", "$22", "面向中型舰船的长距离量子引擎。"),
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
    val avatarUrl: String? = null,
    val email: String? = null,
    val username: String? = null,
    val hasGamePackage: Boolean? = null,
    val isAuthenticated: Boolean = false,
)

interface ProfileRepository {
    suspend fun profile(): ProfileData
}

/** Read-only adapter for the imported account/cache contract. */
class ProductionProfileRepository(
    private val source: ProductionCacheDataSource? = null,
) : ProfileRepository {
    override suspend fun profile(): ProfileData = source?.profile() ?: ProfileData()
}

data class ToolItem(val id: String, val title: String, val subtitle: String)

interface UtilityRepository {
    suspend fun groups(): List<Pair<String, List<ToolItem>>>
    suspend fun detail(toolId: String): ToolDetail = productionToolDetails[toolId] ?: ToolDetail(
        toolId = toolId,
        rows = listOf("状态" to "本地只读", "数据源" to "生产缓存 adapter"),
    )
}

data class ToolDetail(
    val toolId: String,
    val rows: List<Pair<String, String>>,
    val externalUrl: String? = null,
)

class ProductionUtilityRepository(
    private val source: ProductionCacheDataSource? = null,
) : UtilityRepository {
    override suspend fun groups(): List<Pair<String, List<ToolItem>>> = source?.toolGroups() ?: productionToolGroups
    override suspend fun detail(toolId: String): ToolDetail = source?.toolDetail(toolId) ?: productionToolDetails[toolId]
        ?: super.detail(toolId)
}

interface CcuRepository {
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
    override suspend fun ships(): List<CcuShip> = source?.ccuShips(m80Image, fallbackImage) ?: listOf(
        CcuShip("m80", "M80", 30_000, com.refuge.next.R.drawable.m80_hero),
        CcuShip("aurora", "极光 Mk I ES", 2_000, com.refuge.next.R.drawable.ship_placeholder),
        CcuShip("atls", "ATLS", 4_000, com.refuge.next.R.drawable.ship_placeholder),
    )

    override suspend fun owned(): List<OwnedCcu> = source?.ownedCcu() ?: listOf(
        OwnedCcu("ccu-1", "Aurora → M80 CCU", 500, "M80"),
    )
}

data class CcuShip(val id: String, val name: String, val purchasePrice: Int, val imageRes: Int)

data class OwnedCcu(val id: String, val title: String, val purchasePrice: Int, val appliedTo: String)

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
    "测试中心" to listOf(
        ToolItem("test-center", "测试中心", "验证本地缓存、Glass 和运行状态"),
        ToolItem("rsi", "RSI 快捷入口", "打开常用 RSI 资料入口"),
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
    "crowdfunding" to ToolDetail("crowdfunding", listOf("当前支持项目" to "3 个", "累计支持" to "$140", "最近同步" to "2026-08-20")),
    "player-search" to ToolDetail("player-search", listOf("查询范围" to "公开 Handle", "最近查询" to "NocturnePilot", "状态" to "本地只读")),
    "social" to ToolDetail("social", listOf("组织" to "星环城 · 社区等级 4", "待处理邀请" to "2 条", "最近联系" to "NocturnePilot · 在线")),
    "gift-redeem" to ToolDetail("gift-redeem", listOf("待兑换礼包" to "2 条", "最近礼物码" to "RAVEN-7K2Q", "状态" to "本地待处理")),
    "ships" to ToolDetail("ships", listOf("资料分类" to "舰船", "条目" to "本地目录", "入口" to "终端")),
    "equipment" to ToolDetail("equipment", listOf("资料分类" to "装备、护盾、武器", "条目" to "本地目录", "入口" to "终端")),
    "referrals" to ToolDetail("referrals", listOf("邀请人数" to "3", "已完成" to "2", "最近同步" to "2026-08-20")),
    "referral-reverse" to ToolDetail("referral-reverse", listOf("邀请人" to "Raveniume", "关系记录" to "3 条", "最近同步" to "2026-08-20")),
    "test-center" to ToolDetail("test-center", listOf("Glass pipeline" to "PASS", "本地缓存" to "PASS", "破坏性请求" to "拦截")),
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

enum class DestructiveAction {
    GIFT,
    RECLAIM,
    UPGRADE_PURCHASE,
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
class SafeNoOpDestructiveActionExecutor : DestructiveActionExecutor {
    override fun execute(action: DestructiveAction) = SafeActionResult(
        action = action,
        executed = false,
        message = "safeNoOp intercepted ${action.name}; no remote mutation was sent",
    )
}

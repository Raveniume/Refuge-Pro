package com.refuge.next.data

import kotlinx.coroutines.delay

enum class StoreCategory(val label: String) {
    SHIPS("舰船"),
    PAINTS("涂装"),
    GEAR("装备"),
    PACKAGES("游戏包"),
    VEHICLES("载具"),
    BUNDLES("组合包"),
    SUBSCRIPTIONS("订阅"),
}

data class StoreProduct(
    val id: String,
    val title: String,
    val category: StoreCategory,
    val metadata: String,
    val priceCents: Int,
    val imageUrl: String,
    val description: String,
    val isWarbond: Boolean = false,
    val isPackage: Boolean = false,
) {
    val priceLabel: String
        get() = if (priceCents % 100 == 0) {
            "$${priceCents / 100}"
        } else {
            "$${"%.2f".format(priceCents / 100.0)}"
        }
}

interface StoreRepository {
    suspend fun products(): List<StoreProduct>
}

data class CartLine(
    val product: StoreProduct,
    val quantity: Int,
)

interface CartRepository {
    fun lines(): List<CartLine>
    fun add(product: StoreProduct)
    fun remove(productId: String)
    fun clear()
}

/** Local cart keeps product selection functional without performing checkout. */
class InMemoryCartRepository : CartRepository {
    private val quantities = linkedMapOf<String, Int>()
    private var productsById = emptyMap<String, StoreProduct>()

    override fun lines(): List<CartLine> = quantities.mapNotNull { (id, quantity) ->
        productsById[id]?.let { CartLine(it, quantity) }
    }

    override fun add(product: StoreProduct) {
        productsById = productsById + (product.id to product)
        quantities[product.id] = (quantities[product.id] ?: 0) + 1
    }

    override fun remove(productId: String) {
        val current = quantities[productId] ?: return
        if (current <= 1) quantities.remove(productId) else quantities[productId] = current - 1
    }

    override fun clear() {
        quantities.clear()
    }
}

/** Read-only adapter for the versioned RSI catalog import. */
class ProductionCatalogStoreRepository(
    private val source: ProductionCacheDataSource? = null,
) : StoreRepository {
    override suspend fun products(): List<StoreProduct> {
        delay(420)
        return source?.storeProducts() ?: productionCatalogCache
    }
}

private fun product(
    id: String,
    title: String,
    category: StoreCategory,
    metadata: String,
    priceCents: Int,
    imageUrl: String,
    description: String,
    isWarbond: Boolean = false,
    isPackage: Boolean = false,
) = StoreProduct(
    id = id,
    title = title,
    category = category,
    metadata = metadata,
    priceCents = priceCents,
    imageUrl = if (imageUrl.startsWith("/")) {
        "https://robertsspaceindustries.com$imageUrl"
    } else {
        imageUrl
    },
    description = description,
    isWarbond = isWarbond,
    isPackage = isPackage,
)

private val productionCatalogCache = listOf(
    product("10898", "PTV小车", StoreCategory.SHIPS, "Greycat · 独立载具", 1500, "/media/5rg8z7erquf0wr/store_small/Buggy.jpg", "适合在大型机库和地面设施中快速通行。"),
    product("628", "极光 Mk I ES", StoreCategory.SHIPS, "RSI · 独立舰船", 2000, "https://media.robertsspaceindustries.com/4t9yddbsf0muk/store_small.jpeg", "轻量、可靠的入门级多用途舰船。"),
    product("14170", "极光 Mk I MR", StoreCategory.SHIPS, "RSI · 独立舰船", 3000, "https://media.robertsspaceindustries.com/cm90yxr38vd5h/store_small.jpeg", "为基础巡逻与任务执行准备的 Aurora 型号。"),
    product("17064", "脉冲 LX", StoreCategory.SHIPS, "Mirai · 独立载具", 3000, "https://media.robertsspaceindustries.com/1d5jzghu8jgtr/store_small.jpg", "高机动个人载具，提供紧凑的探索能力。"),
    product("616", "野马阿尔法", StoreCategory.SHIPS, "CO · 独立舰船", 3000, "/media/cpq6ly29wmi1br/store_small/56745675467.jpg", "兼顾运输与基础战斗的经济型舰船。"),
    product("17381", "ATLS", StoreCategory.SHIPS, "Argo · 工业载具", 4000, "https://media.robertsspaceindustries.com/1iqxjatfusi4e/store_small.jpg", "用于货运装卸和工业任务的动力装甲平台。"),

    product("18699", "魔像 - 强心涂装", StoreCategory.PAINTS, "Drake · 涂装", 300, "https://media.robertsspaceindustries.com/6jeplyamn64ls/store_small.jpg", "明亮红色主色搭配黑色细节。"),
    product("15914", "复仇者 - 铁纹涂装", StoreCategory.PAINTS, "Aegis · 涂装", 300, "https://media.robertsspaceindustries.com/pkrb5ki4bt540/store_small.jpg", "为 Avenger 系列提供工业质感外观。"),
    product("13537", "复仇者 - 橄榄绿涂装", StoreCategory.PAINTS, "Aegis · 涂装", 300, "https://media.robertsspaceindustries.com/5lvt2b008irtk/store_small.jpg", "低调橄榄绿色战术涂装。"),
    product("18994", "极光 Mk II - 隐匿涂装", StoreCategory.PAINTS, "RSI · 涂装", 300, "https://media.robertsspaceindustries.com/eyt5fi92fvnlo/store_small.jpg", "为 Aurora Mk II 设计的低可视度外观。"),
    product("18993", "极光 Mk II - 里海涂装", StoreCategory.PAINTS, "RSI · 涂装", 300, "https://media.robertsspaceindustries.com/qn1uk60v6vmbt/store_small.jpg", "Aurora Mk II 专用里海配色。"),

    product("13246", "RSI 装备包", StoreCategory.GEAR, "RSI · 装备", 350, "https://media.robertsspaceindustries.com/gayeu1u8zmf62/store_small.jpg", "RSI 品牌帽衫与配套装备组合。"),
    product("17070", "盾博尔装备包", StoreCategory.GEAR, "Tumbril · 装备", 350, "https://media.robertsspaceindustries.com/e9on9selll94t/store_small.jpg", "Tumbril 品牌服装与随身装备。"),
    product("13253", "起源装备包", StoreCategory.GEAR, "Origin · 装备", 350, "https://media.robertsspaceindustries.com/zd94me24sdev7/store_small.jpg", "Origin 风格的品牌服装组合。"),
    product("18158", "MISC 装备包", StoreCategory.GEAR, "MISC · 装备", 350, "https://media.robertsspaceindustries.com/w6txzhg9iw957/store_small.jpg", "MISC 品牌装备组合。"),
    product("17046", "德雷克装备包", StoreCategory.GEAR, "Drake · 装备", 350, "https://media.robertsspaceindustries.com/n7jddmu2hdiyg/store_small.jpg", "Drake 风格品牌装备组合。"),

    product("18975", "公民新手包", StoreCategory.PACKAGES, "RSI Aurora Mk II · 游戏包", 6000, "https://media.robertsspaceindustries.com/w9qkf63nkfcyo/store_small.jpg", "包含游戏访问资格、入门舰船和基础保险。", isPackage = true),
    product("18307", "通用新手包", StoreCategory.PACKAGES, "游戏包", 6000, "https://media.robertsspaceindustries.com/ysurm8lg4cw2c/store_small.jpg", "面向新玩家的通用游戏包。", isPackage = true),
    product("18754", "打捞者新手包", StoreCategory.PACKAGES, "工业 · 游戏包", 7500, "https://media.robertsspaceindustries.com/7u3ybzv7nbyg9/store_small.jpg", "包含面向打捞职业的入门舰船与游戏资格。", isPackage = true),
    product("18046", "矿工新手包", StoreCategory.PACKAGES, "工业 · 游戏包", 7500, "https://media.robertsspaceindustries.com/1veqrzmppmfdu/store_small.jpg", "面向采矿职业路径的起始组合。", isPackage = true),
    product("19453", "ATLS 双人包", StoreCategory.PACKAGES, "Argo · 组合包", 7500, "https://media.robertsspaceindustries.com/vkokp4dy83852/store_small.jpg", "包含 Argo ATLS 与 ATLS GEO。", isWarbond = true, isPackage = true),
    product("19454", "魔像双船包", StoreCategory.PACKAGES, "Drake · 组合包", 13000, "https://media.robertsspaceindustries.com/t8q4acfs4cypb/store_small.jpg", "Drake 魔像系列双船组合。", isWarbond = true, isPackage = true),
)

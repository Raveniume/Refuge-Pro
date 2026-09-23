package com.refuge.next.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    fun cachedProducts(): List<StoreProduct> = emptyList()
    suspend fun awaitCachedProducts(): List<StoreProduct> = cachedProducts()
    suspend fun products(): List<StoreProduct>
}

internal val refugeBeijingZone: ZoneId = ZoneId.of("Asia/Shanghai")

internal fun storeSnapshotDate(instant: Instant): LocalDate =
    instant.atZone(refugeBeijingZone).toLocalDate()

internal fun shouldForceStoreRefresh(snapshotDate: LocalDate?, now: Instant): Boolean =
    snapshotDate != storeSnapshotDate(now)

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
    override fun cachedProducts(): List<StoreProduct> = source?.peekStoreProducts().orEmpty()
    override suspend fun awaitCachedProducts(): List<StoreProduct> = withContext(Dispatchers.IO) {
        source?.storeProducts().orEmpty()
    }
    override suspend fun products(): List<StoreProduct> = withContext(Dispatchers.IO) {
        delay(420)
        source?.storeProducts().orEmpty()
    }
}

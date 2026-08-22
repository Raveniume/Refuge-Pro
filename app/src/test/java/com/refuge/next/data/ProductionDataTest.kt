package com.refuge.next.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductionDataTest {
    @Test
    fun shipValueUsesActualPurchasePrices() {
        assertEquals(
            22_000,
            calculateShipValue(
                seedPurchasePrice = 2_000,
                ownedCcuPurchasePrices = listOf(2_000, 5_000),
                remainingPayment = 13_000,
            ),
        )
    }

    @Test
    fun ccuPlanDelegatesToTheSameFormula() {
        val seed = CcuShip("seed", "Seed", 2_000, 0)
        val target = CcuShip("target", "Target", 20_000, 0)
        val plan = CcuPlan(seed, target, listOf(OwnedCcu("ccu", "Seed to Target", 5_000, "Target")), 13_000)

        assertEquals(20_000, plan.shipValue)
    }

    @Test
    fun remainingPaymentSubtractsOwnedCcuPurchasePrices() {
        val seed = CcuShip("seed", "Seed", 2_000, 0)
        val target = CcuShip("target", "Target", 20_000, 0)
        val owned = listOf(OwnedCcu("ccu", "Seed to Mid", 5_000, "Target"))

        assertEquals(13_000, calculateRemainingPayment(seed, target, owned))
    }

    @Test
    fun targetSelectorOnlyOffersShipsWithHigherPurchasePrice() {
        val seed = CcuShip("seed", "Seed", 20_000, 0)
        val ships = listOf(
            seed,
            CcuShip("lower", "Lower", 10_000, 0),
            CcuShip("higher", "Higher", 30_000, 0),
        )

        assertEquals(listOf("higher"), eligibleTargetShips(seed, ships).map { it.id })
    }

    @Test
    fun localCartAggregatesAndRemovesLinesWithoutCheckout() {
        val cart = InMemoryCartRepository()
        val product = StoreProduct(
            id = "sku",
            title = "Test ship",
            category = StoreCategory.SHIPS,
            metadata = "Test",
            priceCents = 1_500,
            imageUrl = "",
            description = "",
        )

        cart.add(product)
        cart.add(product)
        assertEquals(2, cart.lines().single().quantity)
        cart.remove(product.id)
        assertEquals(1, cart.lines().single().quantity)
        cart.clear()
        assertEquals(emptyList<CartLine>(), cart.lines())
    }

    @Test
    fun terminalCacheCoversLegacyDatabaseCategories() = kotlinx.coroutines.runBlocking {
        val categories = ProductionTerminalRepository().items().map { it.category }.toSet()
        assertEquals(TerminalCategory.entries.toSet(), categories)
    }

    @Test
    fun utilityDetailsComeFromTheProductionRepositoryBoundary() = kotlinx.coroutines.runBlocking {
        val detail = ProductionUtilityRepository().detail("crowdfunding")
        assertEquals("3 个", detail.rows.first { it.first == "当前支持项目" }.second)
        assertEquals(null, detail.externalUrl)
    }

    @Test
    fun externalUtilityEntriesRetainTheirSafeNavigationContract() = kotlinx.coroutines.runBlocking {
        val detail = ProductionUtilityRepository().detail("web-hangar")
        assertEquals("https://robertsspaceindustries.com/account/pledges", detail.externalUrl)
        assertEquals("不会执行", detail.rows.first { it.first == "账户变更" }.second)
    }

    @Test
    fun ownedCcuChainIsAvailableWithoutRemotePlanning() = kotlinx.coroutines.runBlocking {
        val owned = ProductionCcuRepository().owned().single()
        val chain = ProductionCcuRepository().chain(owned)
        assertEquals("Aurora", chain.single().from)
        assertEquals("M80", chain.single().to)
    }

    @Test
    fun userStatusIsOneSharedToggleSource() {
        val source = UserStatusSource(initialOnline = true)
        source.toggle()
        assertEquals(false, source.isOnline)
        source.toggle()
        assertEquals(true, source.isOnline)
    }

    @Test
    fun destructiveActionsAreAlwaysIntercepted() {
        val result = SafeNoOpDestructiveActionExecutor().execute(DestructiveAction.RSI_PURCHASE)
        assertEquals(false, result.executed)
        assertEquals(DestructiveAction.RSI_PURCHASE, result.action)
    }
}

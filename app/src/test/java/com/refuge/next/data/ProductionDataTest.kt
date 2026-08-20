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
}

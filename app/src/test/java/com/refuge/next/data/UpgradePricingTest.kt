package com.refuge.next.data

import org.junit.Assert.*
import org.junit.Test

class UpgradePricingTest {
    @Test fun discountRequiresPositiveLowerEditionPrice() {
        assertEquals(2500, upgradeDiscount(10000, 7500))
        assertEquals(0, upgradeDiscount(10000, 0))
        assertEquals(0, upgradeDiscount(10000, 10000))
        assertEquals(0, upgradeDiscount(10000, 12000))
    }

    @Test fun usdParsingRejectsAmbiguousOrNegativeValues() {
        assertEquals(12345, parseUpgradeUsdCents("US$123.45"))
        assertEquals(null, parseUpgradeUsdCents("free"))
        assertEquals(null, parseUpgradeUsdCents("-1.00"))
        assertEquals(null, parseUpgradeUsdCents("1.234"))
    }

    @Test fun headerCountUsesAvailablePositiveCcuDifferenceOnly() {
        val catalog = listOf(
            CurrentCcuShipPricing(10000, listOf(CurrentCcuSkuPrice(7500, true, unlimitedStock = true))),
            CurrentCcuShipPricing(10000, listOf(CurrentCcuSkuPrice(7500, false, unlimitedStock = true))),
            CurrentCcuShipPricing(10000, listOf(CurrentCcuSkuPrice(10000, true, unlimitedStock = true))),
            CurrentCcuShipPricing(10000, listOf(CurrentCcuSkuPrice(9000, true, availableStock = 0))),
        )
        assertEquals(1, countCurrentCcuDiscountShips(catalog))
    }
}

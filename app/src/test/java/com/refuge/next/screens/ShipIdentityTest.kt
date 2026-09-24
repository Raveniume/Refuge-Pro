package com.refuge.next.screens

import com.refuge.next.data.CcuShip
import org.junit.Assert.*
import org.junit.Test

class ShipIdentityTest {
    private fun ship(id: String, name: String) = CcuShip(id, name, 100, 0)

    @Test fun chineseNamesMustNotCollapseToTheSameIdentity() {
        assertFalse(ship("1", "英仙座").sameIdentityAs(ship("2", "北极星")))
        assertFalse(ship("", "---").sameIdentityAs(ship("", "")))
    }
    @Test fun stableIdSurvivesLocalizationAndAliasSurvivesBrandPrefix() {
        assertTrue(ship("1", "Perseus").sameIdentityAs(ship("1", "英仙座")))
        assertTrue(ship("hangar:123", "RSI Perseus").sameIdentityAs(ship("2", "Perseus")))
        assertFalse(ship("hangar:123", "Aurora MR").sameIdentityAs(ship("3", "Aurora LN")))
        assertFalse(ship("1", "Perseus").sameIdentityAs(ship("2", "Perseus")))
    }
    @Test fun purchaseTargetsMatchCatalogIdentityBeforeAliases() {
        val candidate = UpgradeShipOption(22, "英仙座", 80000, "", "RSI", null, emptyList(), "Perseus")
        val owned = com.refuge.next.data.OwnedShip("RSI Perseus", "", "", "", "", 0)
        assertTrue(candidate.matchesOwnedShip(owned))
        assertTrue(candidate.matchesOwnedShip(owned.copy(name = "Localized elsewhere", shipId = 22)))
        assertFalse(candidate.matchesOwnedShip(owned.copy(shipId = 23)))
        assertFalse(candidate.matchesOwnedShip(owned.copy(name = "北极星")))
    }

    @Test fun seedSelectorKeepsOwnedAliasesAndOnlyOffersLowerPricedShips() {
        val owned = CcuShip("hangar:123", "RSI Perseus", 80_000, 0, owned = true)
        val catalogAlias = CcuShip("22", "Perseus", 80_000, 0)
        val lower = CcuShip("lower", "Aurora MR", 25_000, 0)
        val higher = CcuShip("higher", "Polaris", 100_000, 0)

        assertEquals(listOf("lower", "hangar:123"), plannerStartOptions(higher, listOf(owned, catalogAlias, lower, higher)).map { it.id })
        assertEquals(listOf("lower", "hangar:123", "higher"), distinctPlannerShips(listOf(owned, catalogAlias, lower, higher)).map { it.id })
    }

    @Test fun ownedSeedKeepsItsPaidValueWhenCatalogHasSameShipAtAnotherPrice() {
        val owned = CcuShip("hangar:123", "RSI Perseus", 80_000, 0, owned = true, paidPrice = 45_000)
        val catalog = CcuShip("22", "Perseus", 100_000, 0)

        val options = distinctPlannerShips(listOf(owned, catalog))

        assertEquals(1, options.size)
        assertEquals("hangar:123", options.single().id)
        assertEquals(45_000, options.single().paidPrice)
    }
}

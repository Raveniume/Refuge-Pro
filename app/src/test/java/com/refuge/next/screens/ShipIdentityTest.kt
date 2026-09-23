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
}

package com.refuge.next.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RevisionDataTest {
    @Test fun storeMarkupPreservesLinkTextWithoutHtmlOrEntities() {
        assertEquals("This is not a GAME PACKAGE. A & B", storefrontText("<p>This is not a <a href=\"https://example.com\">GAME PACKAGE</a>.</p><p>A &amp; B</p>"))
        assertEquals("GAME PACKAGE", storefrontText("&lt;a href=\"https://example.com\"&gt;GAME PACKAGE&lt;/a&gt;"))
    }
    private fun item(type: String, power: Int, floor: Double = 1.0, generation: Boolean = false) = JSONObject("""{
        "type":"$type", "resource":{"states":[{"name":"Online","flows":[{
        "${if (generation) "produces" else "consumes"}":[{"resource":"Power","unitKind":"powerSegment","units":$power}],"minimumFraction":$floor}]}]}}
    """)
    private fun slot(path: String, item: JSONObject) = ErkulSlot(path, JSONObject(), item, item, true, 0)
    private val ship = JSONObject("""{"vehicle":{"powerPools":{"pools":[{"itemType":"WeaponGun","poolSize":4}]}}}""")
    private fun equipment() = listOf(slot("power", item("PowerPlant", 10, generation = true)),
        slot("life", item("LifeSupportGenerator", 1)), slot("cooler", item("Cooler", 3, 0.3333333)),
        slot("shield", item("Shield", 4, .5)), slot("weapon", item("WeaponGun", 4)), slot("quantum", item("QuantumDrive", 2)))
    @Test fun allocationRespectsBudgetAndMinimumBlocksAfterEveryAdjustment() {
        var draft = JSONObject()
        repeat(60) { index ->
            val before = allocateErkulPower(ship, equipment(), draft)
            draft = setPowerGroup(before, draft, listOf("散热", "武器", "护盾")[index % 3], index % 7)
            val after = allocateErkulPower(ship, equipment(), draft)
            assertTrue(after.used <= after.capacity)
            after.consumers.forEach { consumer -> assertTrue(after.supplied(consumer.path) == 0 || after.supplied(consumer.path) in consumer.minimum..consumer.maximum) }
        }
        assertEquals(1, minimumPower(3, .3333333))
    }
    @Test fun navDisablesShieldsAndWeaponsAndEnablesQuantum() {
        val allocation = allocateErkulPower(ship, equipment(), JSONObject().put("flightMode", "NAV"))
        assertEquals(0, allocation.supplied("shield"))
        assertEquals(0, allocation.supplied("weapons"))
        assertTrue(allocation.supplied("quantum") > 0)
        val offline = allocateErkulPower(ship, equipment(), JSONObject("""{"disabled":["power"]}"""))
        assertEquals(0, offline.used)
    }
    @Test fun erkulCapacitySharesMultipleGeneratorsLikePerseus() {
        fun generator(path: String) = slot(path, JSONObject("""
            {"type":"PowerPlant","size":3,"resource":{"states":[{"name":"Online","flows":[
            {"produces":[{"resource":"Power","unitKind":"powerSegment","units":24}]}
            ]}]}}
        """))
        val generators = listOf(generator("power-1"), generator("power-2"))
        assertEquals(30, erkulPowerCapacitySegments(generators))
    }

    @Test fun weaponHeatUsesOnlineAndFireActionRates() {
        val weapon = JSONObject("""
            {"weapon":{"connection":{"heatRateOnline":2},"fireActions":[
              {"fireRate":600,"heatPerShot":0.5,"pelletCount":1}
            ]}}
        """)
        assertEquals(7.0, heatGenerationPerSecond(weapon), 0.001)
    }
    @Test fun plannerObjectivesChooseDifferentRoutesWhenSunkCostIsHigh() {
        val a = CcuShip("a", "A", 1000, 0)
        val b = CcuShip("b", "B", 2000, 0)
        val c = CcuShip("c", "C", 3000, 0)
        val owned = listOf(OwnedCcu("owned", "A to C", 4000, "C", "A", "C"))
        assertEquals(0, planCcuRoute(a,c,listOf(a,b,c),owned,CcuOptimization.NEW_SPEND)!!.remainingPayment)
        assertEquals(2000, planCcuRoute(a,c,listOf(a,b,c),owned,CcuOptimization.TOTAL_COST)!!.remainingPayment)
    }
}

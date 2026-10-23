package com.refuge.next.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TerminalKnowledgeTest {
    @Test fun hullCountsTraverseAllRootPartsAndNestedChildren() {
        val ship = JSONObject(javaClass.getResource("/erkul/avenger-stalker-4.10.2.json")!!.readText())
        val counts = erkulHullPartCounts(ship)
        assertEquals(2, counts["vital"])
        assertEquals(5, counts["secondary"])
        assertEquals(6, counts["breakable"])
        assertEquals(0, counts["subpart"] ?: 0)
    }
    @Test fun localizationUsesKeysPreservesPlaceholdersAndFallsBack() {
        val catalog = MissionCatalog("test", JSONObject("""{"contracts":[],"locationPools":{"a":{"name":"Stanton"}}}"""),
            JSONObject("""{"keys":{"title":{"tr":"送至 [DESTINATION]"},"desc":{"tr":"<EM4>材料</EM4>\\n下一行"}}}"""))
        val row = JSONObject("""{"title":"Deliver","titleKey":"@title","description":"Details","descriptionLocKey":"desc","locations":["a","missing"]}""")
        assertEquals("送至 [DESTINATION]", catalog.text(row, "title"))
        assertEquals("Deliver", catalog.text(row, "title", false))
        assertEquals("材料\n下一行", catalog.text(row, "description"))
        assertEquals(listOf("Stanton"), catalog.locations(row, "locations"))
        assertEquals("English", catalog.translated("absent", "English"))
    }
    @Test fun blueprintQualityInterpolatesWithinTheSelectedRange() {
        val option = JSONObject("""{"modifiers":[{"property":"HP","property_cn":"完整性","modifierType":"multiplicative","startQuality":0,"endQuality":500,"modifierAtStart":0.8,"modifierAtEnd":1.0},{"property":"HP","property_cn":"完整性","modifierType":"multiplicative","startQuality":501,"endQuality":1000,"modifierAtStart":1.0,"modifierAtEnd":1.2}]}""")
        assertEquals(-20.0, blueprintModifiers(option, 0).single().second, .001)
        assertEquals(0.0, blueprintModifiers(option, 500).single().second, .001)
        assertEquals(20.0, blueprintModifiers(option, 1000).single().second, .001)
        assertTrue(blueprintModifiers(option, -1).isEmpty())
    }
    @Test fun referenceShipMatchesLiveErkulStockFiguresAndCanPowerWeaponsBackOn() {
        // Public CDN schema 8, 4.10.2-LIVE.12913470, captured 2026-10-23.
        // https://cdn.erkul.games/LIVE/ships/aegs_avenger_stalker.810b2f53.bin
        val ship = JSONObject(javaClass.getResource("/erkul/avenger-stalker-4.10.2.json")!!.readText())
        val catalog = ErkulCatalog("LIVE", "4.10.2", JSONObject(), listOf(ship), emptyMap(), emptyMap())
        val slots = resolveErkulSlots(ship, catalog, JSONObject())
        val stock = calculateErkulMetrics(ship, slots, JSONObject())
        val weaponSlots = slots.filter { it.item?.optString("type") == "WeaponGun" }
        assertEquals(500.7, weaponSlots.sumOf { weaponAlphaMax(it.item!!)!! }, .01)
        assertEquals(2360L, kotlin.math.round(stock.burst).toLong())
        // Erkul's public calculator uses the energy-pool cycle for the two
        // Omnisky cannons and complete-shot heat accounting for the Revenant.
        assertEquals("segments=${allocateErkulPower(ship, slots, JSONObject()).segments}; perWeapon=${weaponSlots.joinToString { "${it.item!!.itemName()}=${weaponSustained(it.item!!, 1.0)}" }}",
            1459.0, stock.sustained, .5)
        assertEquals(4488.0, stock.shield, .1)
        assertEquals(852.0, stock.shieldRegen, .1)
        assertEquals(48986.3, stock.mass, .1)
        assertEquals(120.0, stock.quantumRange!!, .1)
        // Match the reference's allocation: 4 weapon, 2 engine, 2 shield,
        // 3 radar, 1 life-support, 2 on one cooler. Barrel heat must not be
        // added to coolant; the unused cooler must not emit IR.
        var cooler = 0
        val manual = JSONObject().put("weapons", 4)
        slots.forEach { slot -> manual.put(slot.path, when (slot.item?.optString("type")) {
            "FlightController" -> 2
            "Shield", "LifeSupportGenerator" -> 1
            "Radar" -> 3
            "Cooler" -> if (cooler++ == 0) 2 else 0
            else -> 0
        }) }
        val referenceDraft = JSONObject().put("powerSegments", manual)
        val reference = calculateErkulMetrics(ship, slots, referenceDraft)
        assertEquals(19.1, reference.coolingUsed, .001)
        assertEquals(34.0 * 2 / 3 * .85, reference.cooling, .001)
        val signature = erkulSignatures(ship, activeErkulSlots(slots, referenceDraft),
            allocateErkulPower(ship, slots, referenceDraft), reference.cooling, reference.coolingUsed)
        assertEquals(7260.0 * 2 / 3 * .85 * 19.1 / reference.cooling * 1.1, signature.infrared, .001)
        assertEquals(ship.obj("vehicle").obj("crossSection").getDouble("z") * 1.1, signature.crossSectionZ, .001)
        assertEquals(stock.burst * .85, calculateErkulMetrics(ship, slots, JSONObject().put("gimbalMode", "GIMBAL")).burst, .1)
        val consumers = allocateErkulPower(ship, slots, JSONObject()).consumers.filter { it.group == "武器" }
        val off = togglePowerGroup(JSONObject(), consumers, true)
        assertEquals(0.0, calculateErkulMetrics(ship, slots, off).burst, .001)
        val on = togglePowerGroup(off, consumers, false)
        assertEquals(stock.burst, calculateErkulMetrics(ship, slots, on).burst, .001)
    }

    @Test fun weaponSourceGroupsUseRealErkulContainerAndSeatMetadata() {
        fun slot(root: JSONObject, control: JSONObject? = null) = ErkulSlot(
            path = "arbitrary_path",
            port = JSONObject(),
            item = JSONObject().put("type", "WeaponGun"),
            original = null,
            pilot = false,
            depth = 1,
            rootItem = root,
            rootPort = JSONObject().apply { if (control != null) put("control", control) },
        )
        assertEquals("pdc", erkulWeaponLocation(slot(JSONObject("""{"type":"Turret","subType":"PdcTurret"}"""))))
        assertEquals("mannedTurret", erkulWeaponLocation(slot(JSONObject("""{"type":"Turret","subType":"MannedTurret"}"""))))
        assertEquals("remoteOperator", erkulWeaponLocation(slot(JSONObject("""{"type":"Turret","seats":["remote_operator"]}"""),
            JSONObject("""{"pilot":false,"seats":["remote_operator"]}"""))))
        assertEquals("pilot", erkulWeaponLocation(slot(JSONObject("""{"type":"WeaponGun"}"""), JSONObject("""{"pilot":true}"""))))
    }
}

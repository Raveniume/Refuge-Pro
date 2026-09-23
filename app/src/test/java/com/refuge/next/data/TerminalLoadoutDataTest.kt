package com.refuge.next.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalLoadoutDataTest {
    private val shieldPort = TerminalPort(
        id = "0:shield",
        name = "hardpoint_shield_left",
        type = "Shield",
        minSize = 2,
        maxSize = 3,
        editable = true,
        compatibleTypes = listOf(TerminalPortType("Shield")),
    )

    private fun shield(id: String, size: Int, version: String = "4.10.0"): TerminalItem = TerminalItem(
        id = id, name = id, manufacturer = "Test", category = TerminalCategory.SHIELDS,
        tags = emptyList(), value = "—", usd = "—", description = "", size = size,
        sourceType = "Shield", sourceVersion = version,
    )

    @Test
    fun portFilterRequiresSizeTypeVersionAndEditableFlag() {
        assertTrue(terminalPortAccepts(shieldPort, shield("valid", 3)))
        assertFalse(terminalPortAccepts(shieldPort, shield("wrong-size", 1)))
        assertFalse(terminalPortAccepts(shieldPort, shield("wrong-version", 3, "4.9.0"), "4.10.0"))
        assertFalse(terminalPortAccepts(shieldPort.copy(editable = false), shield("fixed", 3)))
    }

    @Test
    fun changedPerformanceDoesNotInventUnknownComponentValues() {
        val ship = TerminalItem(
            id = "ship", name = "Ship", manufacturer = "Test", category = TerminalCategory.VEHICLES,
            tags = emptyList(), value = "—", usd = "—", description = "",
            sourceVersion = "4.10.0",
            performance = TerminalPerformance(massTotalKg = 10_000.0, shieldHp = 2_000.0),
            ports = listOf(shieldPort.copy(equippedItemId = "old")),
        )
        val replacement = shield("new", 3).copy(performance = TerminalPerformance(shieldHp = 2_500.0))
        val result = calculateTerminalPortPerformance(ship, mapOf(shieldPort.id to replacement.id), mapOf(replacement.id to replacement))

        assertEquals(null, result.shieldHp)
        assertEquals(null, result.massTotalKg)
        assertEquals(null, result.pilotDps)
    }
}

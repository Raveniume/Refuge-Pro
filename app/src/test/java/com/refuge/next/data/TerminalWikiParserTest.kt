package com.refuge.next.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalWikiParserTest {
    @Test
    fun imageParserUsesAllSourceVariantsAndRejectsJsonNull() {
        val images = TerminalWikiParser.imageUrls(
            mapOf(
                "images" to listOf(
                    mapOf("thumbnail_url" to "null", "original_url" to "//cdn.example.test/ship.jpg"),
                    mapOf("thumbnail_url" to "https://img.example.test/ship.webp"),
                ),
            ),
        )

        assertEquals("https://cdn.example.test/ship.jpg", images.first())
        assertTrue(images.contains("https://img.example.test/ship.webp"))
        assertNull(normalizeImageUrl("null"))
    }

    @Test
    fun parserKeepsEnglishNameAndBuildsStableNestedPortIds() {
        val item = TerminalWikiParser.enrich(
            TerminalItem("ship", "Avenger Stalker", "Aegis", TerminalCategory.VEHICLES, emptyList(), "—", "—", ""),
            mapOf(
                "version" to "4.10.0",
                "mass" to 48_986,
                "mass_total" to 55_317,
                "images" to listOf(mapOf("original_url" to "https://img.example.test/ship.jpg")),
                "ports" to listOf(
                    mapOf(
                        "name" to "turret_left", "editable" to true,
                        "compatible_types" to listOf(mapOf("type" to "Turret", "sub_types" to listOf("GunTurret"))),
                        "ports" to listOf(mapOf("name" to "gun", "editable" to true, "type" to "WeaponGun")),
                    ),
                ),
            ),
            fullPorts = true,
        )

        assertEquals("Avenger Stalker", item.name)
        assertEquals(55_317.0, item.performance?.massTotalKg!!, 0.01)
        assertEquals(listOf("0:turret_left", "0:turret_left/0:gun"), item.ports.map { it.id })
        assertEquals("https://img.example.test/ship.jpg", item.imageUrl)
    }

    @Test
    fun wikiTitleMatchingAllowsPunctuationButRejectsUnrelatedSearchResults() {
        assertEquals(1.0, wikiTitleSimilarity("A03 Canuto Sniper Rifle", "A03 \"Canuto\" Sniper Rifle"), 0.0)
        assertTrue(wikiTitleSimilarity("A03 Canuto Sniper Rifle", "A03 Canuto Sniper Rifle Weapon") >= .72)
        assertTrue(wikiTitleSimilarity("A03 Canuto Sniper Rifle", "Pioneer Mining Laser") < .72)
    }
}

package com.refuge.next.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductionTranslationRepositoryTest {
    @Test fun completeDescriptionsMatchBeforeSplittingAndNormalizeBothSides() {
        val descriptions = LegacyNameTranslator(mapOf("Titan’s hold - carry cargo." to "泰坦货舱可装载货物。"), emptyMap())
        assertEquals("泰坦货舱可装载货物。", descriptions.translate("Titan's hold - carry cargo."))
        assertEquals("泰坦货舱可装载货物。", descriptions.translate("  Titan’s hold - carry cargo.  "))
    }

    private val translator = LegacyNameTranslator(
        translations = mapOf("Aurora MR" to "极光 MR", "Avenger Titan" to "复仇者泰坦", "Upgrade" to "升级"),
        itemNames = mapOf("item_NameBeamGun" to "光束枪", "item_Name_Shield_A" to "护盾 A"),
    )

    @Test
    fun disabledNamesBypassLookupAndNormalization() {
        val forbidden = object : TranslationRepository {
            override fun translate(value: String): String = error("Disabled lookup")
            override fun translateGameItem(value: String): String = error("Disabled item lookup")
        }
        val raw = "  RSI ‘Polaris’*\n"
        assertEquals(raw, displayName(raw, false, forbidden))
        assertEquals(raw, displayName(raw, false, forbidden, gameItem = true, className = "BeamGun_SCItem"))
    }

    @Test
    fun togglingUsesTheSameOriginalRatherThanThePreviousTranslation() {
        val source = sourceName("已经汉化的旧标题", "Aurora MR")
        assertEquals("极光 MR", displayName(source, true, translator))
        assertEquals("Aurora MR", displayName(source, false, translator))
        assertEquals("极光 MR", displayName(source, true, translator))
    }

    @Test
    fun oldSnapshotsRequireAnExplicitOriginalField() {
        assertEquals("Aurora MR", sourceName("极光 MR", "Aurora MR"))
        assertEquals("极光 MR", sourceName("极光 MR"))
        assertEquals("极光 MR", sourceName("极光 MR", "—"))
        assertEquals("极光 MR", sourceName("极光 MR", " "))
        assertEquals("极光 MR", displayName(sourceName("极光 MR"), false, translator))
    }

    @Test
    fun bothLegacyItemPrefixesIgnoreCaseAndStripScItemSuffix() {
        assertEquals("光束枪", translator.translateGameItem("BEAMGUN_SCItem"))
        assertEquals("护盾 A", translator.translateGameItem("Shield_A_scitem"))
    }

    @Test
    fun classLookupNeverReplacesEnglishDisplayNameWhenDisabledOrMissing() {
        assertEquals("光束枪", displayName("Beam Gun", true, translator, true, "BeamGun_SCItem"))
        assertEquals("Beam Gun", displayName("Beam Gun", false, translator, true, "BeamGun_SCItem"))
        assertEquals("Beam Gun", displayName("Beam Gun", true, translator, true, "Missing_SCItem"))
        assertEquals("极光 MR", displayName("Aurora MR", true, translator, true, "Missing_SCItem"))
    }

    @Test
    fun missingTablesAndUnknownNamesFallBackToExactSource() {
        val raw = " Unknown ‘Item’*\n"
        val empty = LegacyNameTranslator(emptyMap(), emptyMap())
        assertEquals(raw, empty.translate(raw))
        assertEquals(raw, empty.translateGameItem(raw))
        assertEquals("  \n", empty.translate("  \n"))
        assertEquals(raw, displayName(raw, true, null))
    }

    @Test
    fun correctionsAndUpgradeNamesRemainSupported() {
        assertEquals("RSI 英仙座", translator.translate("Perseus"))
        assertEquals("RSI 北极星", translator.translate("RSI Polaris"))
        val source = "Upgrade - Aurora MR to Avenger Titan Warbond Edition"
        assertEquals("升级 - 极光 MR 到 复仇者泰坦 战争债券版", displayName(source, true, translator))
        assertEquals(source, displayName(source, false, translator))
    }
}

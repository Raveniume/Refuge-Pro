package com.refuge.next.screens

import com.refuge.next.data.TerminalCategory
import com.refuge.next.data.TerminalItem
import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalPresentationTest {
    @Test
    fun categoryLabelsMatchTheLegacyTerminalContract() {
        assertEquals(
            listOf("载具", "舰载", "单兵", "配件", "护盾", "冷却器", "发电机", "量子"),
            TerminalCategory.entries.map { it.label },
        )
    }

    @Test
    fun specificationGroupingPreservesDifferentValuesWithTheSameLabel() {
        val item = TerminalItem(
            id = "test-weapon",
            name = "Test Weapon",
            manufacturer = "Test Manufacturer",
            category = TerminalCategory.SHIP_COMPONENTS,
            tags = emptyList(),
            value = "—",
            usd = "—",
            description = "",
            details = listOf(
                "射程" to "1,000 m",
                "射程" to "1,500 m",
                "射程" to "1,000 m",
                "质量" to "10 kg",
                "制造商" to "Test Manufacturer",
                "空字段" to "—",
            ),
        )

        val sections = item.terminalSpecificationSections()

        assertEquals(
            listOf("射程" to "1,000 m", "射程" to "1,500 m"),
            sections.single { it.title == "武器性能" }.rows,
        )
        assertEquals(
            listOf("质量" to "10 kg"),
            sections.single { it.title == "物理属性" }.rows,
        )
        assertEquals(3, sections.sumOf { it.rows.size })
    }

    @Test
    fun weaponDetailUsesLegacyStyleCategoriesWithoutDroppingRows() {
        val rows = listOf(
            "武器类型" to "激光加农炮",
            "持续 DPS" to "1,285",
            "射速" to "220 RPM",
            "弹匣容量" to "80",
            "最大穿透厚度" to "0.35 m",
            "功率用量" to "2 - 4 段",
            "结构耐久" to "4,500",
            "质量" to "210 kg",
            "长度" to "0.75 m",
        )
        val item = TerminalItem(
            id = "test-cannon",
            name = "Test Cannon",
            manufacturer = "Test Manufacturer",
            category = TerminalCategory.SHIP_COMPONENTS,
            tags = emptyList(),
            value = "—",
            usd = "—",
            description = "",
            details = rows,
        )

        val sections = item.terminalSpecificationSections()

        assertEquals(listOf("武器类型" to "激光加农炮"), sections.single { it.title == "基本信息" }.rows)
        assertEquals(
            listOf("持续 DPS" to "1,285", "射速" to "220 RPM"),
            sections.single { it.title == "武器性能" }.rows,
        )
        assertEquals(
            listOf("弹匣容量" to "80", "最大穿透厚度" to "0.35 m"),
            sections.single { it.title == "弹药" }.rows,
        )
        assertEquals(
            listOf("功率用量" to "2 - 4 段", "结构耐久" to "4,500"),
            sections.single { it.title == "系统与耐久" }.rows,
        )
        assertEquals(
            listOf("质量" to "210 kg", "长度" to "0.75 m"),
            sections.single { it.title == "物理属性" }.rows,
        )
        assertEquals(rows.size, sections.sumOf { it.rows.size })
    }
}

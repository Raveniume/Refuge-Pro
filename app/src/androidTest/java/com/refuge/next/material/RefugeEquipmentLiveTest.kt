package com.refuge.next.material

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.view.KeyEvent
import com.refuge.next.data.*
import com.refuge.next.design.RefugeColors
import com.refuge.next.screens.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real public LIVE catalog, production editor and persisted local draft. No RSI writes. */
class RefugeEquipmentLiveTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun equipmentCanBeChangedInspectedReopenedAndRestoredInBothThemes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = ErkulLoadoutRepository.shared(context)
        val catalog = runBlocking { repository.catalog("LIVE") }
        val id = "aegs_avenger_stalker"
        val ship = runBlocking { repository.ship(catalog, catalog.ships.first { it.optString("className") == id }) }
        val stock = resolveErkulSlots(ship, catalog, JSONObject())
        val weapon = stock.first { it.item?.optString("type") == "WeaponGun" && it.port.obj("flags").optBoolean("editable") }
        val replacement = catalog.components.values.first { erkulSlotAccepts(weapon, it) && it.optString("className") != weapon.item?.optString("className") }
        val coolers = stock.filter { it.item?.optString("type") == "Cooler" }
        assertEquals("Stalker must expose both physical cooler hardpoints", 2, coolers.size)
        val original = repository.loadDraft("LIVE", catalog.version, id, 0)
        repository.saveDraft("LIVE", catalog.version, id, 0, JSONObject())
        val dark = mutableStateOf(false)
        val generation = mutableIntStateOf(0)
        try {
            compose.setContent {
                val palette = if (dark.value) RefugeColors.dark else RefugeColors.light
                key(generation.intValue) { RefugeScene(palette) { NativeLoadoutScreen(it, palette, dark.value, id, {}) } }
            }
            fun card(path: String) = compose.onNodeWithTag("equipment:$path")
            fun reveal(path: String) { compose.onNodeWithTag("loadout-list").performScrollToNode(hasTestTag("equipment:$path")) }
            fun action(path: String, text: String) = compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag("equipment:$path")))
            compose.waitUntil(90_000) { compose.onAllNodesWithText("载入舰船").fetchSemanticsNodes().isEmpty() }
            reveal(weapon.path)
            save("equipment-light-armament")
            action(weapon.path, "更换").performClick()
            compose.onNodeWithText("更换组件").assertIsDisplayed()
            compose.onNode(hasSetTextAction()).performTextInput(replacement.itemName())
            compose.onNodeWithContentDescription(replacement.itemName()).performClick()
            reveal(weapon.path)
            card(weapon.path).assertIsDisplayed()
            compose.runOnIdle {
                assertEquals(replacement.optString("className"), repository.loadDraft("LIVE", catalog.version, id, 0).obj("overrides").optString(weapon.path))
            }
            // Refresh must check versions without resetting a customized draft.
            compose.waitUntil(90_000) {
                compose.onAllNodesWithText("载入舰船").fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithContentDescription("更新改船数据").performClick()
            compose.waitUntil(90_000) {
                compose.onAllNodesWithText("已是最新版本", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                    compose.onAllNodesWithText("已更新 ·", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            compose.runOnIdle {
                assertEquals("Refresh must retain the selected component", replacement.optString("className"),
                    repository.loadDraft("LIVE", catalog.version, id, 0).obj("overrides").optString(weapon.path))
            }
            save("equipment-refresh-retains-draft")
            action(weapon.path, "详情").performClick()
            compose.onNodeWithText("组件详情").assertIsDisplayed()
            compose.onNodeWithContentDescription("返回").assertDoesNotExist()
            save("equipment-component-light")
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            compose.runOnIdle { dark.value = true; generation.intValue++ }
            compose.waitForIdle()
            reveal(weapon.path)
            compose.onNode(hasText(replacement.itemName()) and hasAnyAncestor(hasTestTag("equipment:${weapon.path}"))).assertIsDisplayed()
            action(weapon.path, "卸下").performClick()
            action(weapon.path, "恢复原厂组件").performClick()
            action(weapon.path, "停用").performClick()
            action(weapon.path, "启用").performClick()
            save("equipment-dark-armament")
            coolers.forEach { cooler ->
                reveal(cooler.path)
                card(cooler.path).assertIsDisplayed()
                action(cooler.path, "更换").assertIsEnabled()
            }
            save("equipment-dark-coolers")
            compose.runOnIdle { dark.value = false }
            save("equipment-light-coolers")
            compose.onNodeWithContentDescription("更多操作").performClick()
            compose.onNodeWithText("配装设置").performClick()
            compose.onNodeWithText("游戏版本").assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            compose.onNodeWithContentDescription("更多操作").performClick()
            compose.onNodeWithText("性能总览").performClick()
            compose.onNodeWithText("当前配装").assertIsDisplayed()
            compose.onNodeWithContentDescription("对比原厂").performClick().assertIsOn()
            compose.onNodeWithContentDescription("对比原厂").performClick().assertIsOff()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            compose.onNodeWithContentDescription("更多操作").performClick()
            compose.onNodeWithText("保存方案").performClick()
            compose.onNode(hasSetTextAction()).performTextInput("QA-customization")
            compose.onNodeWithText("保存", substring = false).performClick()
            compose.onNodeWithContentDescription("更多操作").performClick()
            compose.onNodeWithText("调用方案").performClick()
            compose.onNodeWithContentDescription("QA-customization").performClick()
            compose.onNodeWithTag("native-loadout").assertIsDisplayed()
            evidenceFile("equipment-live-result.txt").writeText("${catalog.version}\nChanged ${weapon.path} to ${replacement.itemName()}; reopened, removed, restored, toggled.\nCoolers: ${coolers.joinToString { it.path }}\n")
        } finally {
            repository.saveDraft("LIVE", catalog.version, id, 0, original)
            context.getSharedPreferences("named-loadouts", android.content.Context.MODE_PRIVATE).edit()
                .remove("LIVE:${catalog.version}:$id:QA-customization").commit()
        }
    }
    private fun save(name: String) {
        compose.waitForIdle()
        // Unmerged semantics export can trigger lazy semantics calculation on
        // the instrumentation thread while the live catalogue recomposes.
        // System capture observes the presented frame without drawing the Owner.
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        evidenceFile("$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

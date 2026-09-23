package com.refuge.next.material

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.refuge.next.data.*
import com.refuge.next.design.*
import com.refuge.next.screens.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class RefugeSeptemberRepairTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun hangarUpgradeOpensOwnedInventoryOnlyFromSecondLevel() {
        val row = HangarItem("Upgrade - Aurora MR to Avenger Titan", "$5", "2026-09-13", com.refuge.next.R.drawable.ship_placeholder,
            originalName = "Upgrade - Aurora MR to Avenger Titan", upgradeFrom = "Aurora MR", upgradeTo = "Avenger Titan", id = 99, isUpgrade = true)
        val repository = object : HangarRepository {
            override suspend fun ownedShips() = emptyList<OwnedShip>()
            override suspend fun inventory() = listOf(row)
            override suspend fun refreshInventory() = listOf(row)
        }
        val ccu = object : CcuRepository {
            override suspend fun ships() = emptyList<CcuShip>()
            override suspend fun owned() = emptyList<OwnedCcu>()
        }
        var openedId = 0L
        compose.setContent { RefugeScene(RefugeColors.dark) { backdrop ->
            Box(Modifier.padding(top = 60.dp)) {
                HangarCcuInventoryPanel(backdrop, RefugeColors.dark, true, ccu, 0, emptyList(), repository, listOf(row), { openedId = it })
            }
        } }
        compose.onNodeWithText("升级规划").assertIsDisplayed()
        compose.onNodeWithText("Aurora MR").assertDoesNotExist()
        compose.onNodeWithText(" 机库 CCU 1").performClick()
        compose.onNodeWithText("Aurora MR").assertIsDisplayed()
        compose.onNodeWithContentDescription(row.title).performClick()
        assertEquals(99L, openedId)
        compose.onNodeWithText("Aurora MR").assertDoesNotExist()
    }
    @Test fun categoryStripSnapsUnderFixedCentralLens() {
        var selected by mutableIntStateOf(0)
        compose.setContent { RefugeScene(RefugeColors.dark) { backdrop ->
            Box(Modifier.padding(16.dp).padding(top = 60.dp)) {
                RefugeLiquidModeSelector(backdrop, RefugeColors.dark, StoreCategory.entries.map { it.label }, selected, { selected = it })
            }
        } }
        val track = compose.onNodeWithTag("mode-track")
        val original = track.fetchSemanticsNode().boundsInRoot
        track.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertTrue(selected > 0)
        track.performTouchInput { swipeRight() }
        compose.waitForIdle()
        val after = track.fetchSemanticsNode().boundsInRoot
        assertEquals(original.width, after.width, 1f)
        compose.onNodeWithText(StoreCategory.entries[selected].label).assertIsSelected()
        save("centered-strip")
    }
    @Test fun filterUsesSecondLevelRowsAndPreservesSelections() {
        var selection by mutableStateOf<FacetSelection>(emptyMap())
        compose.setContent { RefugeScene(RefugeColors.dark) { backdrop ->
            FacetFilterSheet(backdrop, RefugeColors.dark, "终端筛选", mapOf("护盾类型" to listOf("Bubble", "Quadrant"), "厂商" to listOf("Origin")),
                selection, { selection = it }, {})
        } }
        compose.onNodeWithText("护盾类型").performClick()
        compose.onNodeWithText("Bubble").performClick()
        assertEquals(setOf("Bubble"), selection["护盾类型"])
        compose.onNodeWithContentDescription("返回筛选").performClick()
        compose.onNodeWithText("1 项").assertIsDisplayed()
        save("filter-groups")
        compose.onNodeWithText("重置").performClick()
        assertTrue(selection.isEmpty())
    }
    @Test fun nativeOutfitterLoadsPowerAndSettingsWithoutWebView() {
        compose.setContent { RefugeScene(RefugeColors.dark) { _ -> NativeLoadoutScreen(RefugeColors.dark, true, {}) } }
        compose.waitUntil(90_000) { compose.onAllNodesWithText("电力分配").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("电力分配").performScrollTo().assertIsDisplayed()
        save("native-power")
        compose.onNodeWithContentDescription("配装设置").performClick()
        compose.onNodeWithText("游戏版本").assertIsDisplayed()
        compose.onNodeWithText("LIVE").assertIsDisplayed()
        compose.onNodeWithText("PTU").assertIsDisplayed()
        save("native-settings")
    }
    private fun save(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(350)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null), "sept13-$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
    }
}

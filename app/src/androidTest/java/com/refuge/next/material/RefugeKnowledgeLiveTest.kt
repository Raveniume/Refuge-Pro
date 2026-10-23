package com.refuge.next.material

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.refuge.next.data.*
import com.refuge.next.design.RefugeColors
import com.refuge.next.screens.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Read-only network checks on the shared device; no account or purchase actions. */
class RefugeKnowledgeLiveTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun publicCataloguesLoadAndResolveRealDetails() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val knowledge = TerminalKnowledgeRepository(context)
        val missions = knowledge.missions(refresh = true)
        assertTrue(missions.missions.size > 100)
        assertNotNull(missions.language)
        val blueprints = knowledge.blueprints("", "FPSWeapons", 1, false)
        assertTrue(blueprints.total > 0)
        val detail = knowledge.blueprint(blueprints.rows.first().getLong("id"))
        assertTrue(detail.objects("slots").isNotEmpty())
        val erkul = ErkulLoadoutRepository.shared(context)
        val catalog = erkul.catalog("LIVE", refresh = true)
        assertFalse(catalog.version.startsWith("offline"))
        assertTrue(catalog.ships.size > 100)
        val vessel = erkul.ship(catalog, catalog.ships.first { it.optString("className") == "aegs_avenger_stalker" })
        assertEquals(2360.0, vessel.obj("precomputed").obj("dps").getDouble("burst"), .1)
        evidenceFile("live-catalogues.txt").writeText("Missions ${missions.version}: ${missions.missions.size}\nBlueprints FPSWeapons: ${blueprints.total}\nErkul ${catalog.version}: ${catalog.ships.size}\nAvenger Stalker burst: 2360\n")
    }

    @Test fun nativeMissionSearchAndDetails() {
        compose.setContent { RefugeScene(RefugeColors.light) {
            MissionDatabaseScreen(it, RefugeColors.light, false, onBlueprint = {}, onBack = {})
        } }
        compose.waitUntil(90_000) { compose.onAllNodesWithText("SCMDB ·", substring = true).fetchSemanticsNodes().any { node ->
            node.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }.any { it.text.contains("4.") }
        } }
        compose.onNodeWithTag("mission-database").performScrollToIndex(4)
        val listTitle = compose.onNodeWithText("[DESTINATION] 跑腿")
        val originalTop = listTitle.fetchSemanticsNode().boundsInRoot.top
        save("missions-native-light")
        listTitle.performTouchInput { click() }
        compose.onNodeWithText("任务详情").assertIsDisplayed()
        compose.onNodeWithText("SCMDB ·", substring = true).assertIsDisplayed()
        compose.onNodeWithText("[DESTINATION] 跑腿").assertIsDisplayed()
        compose.onNodeWithText("当前版本未找到这条任务", substring = true).assertDoesNotExist()
        compose.onNodeWithText("任务信息").assertExists()
        compose.onNodeWithTag("mission-database").performTouchInput { swipeUp() }
        save("mission-detail-native-light")
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("任务数据库").assertIsDisplayed()
        assertEquals(originalTop, listTitle.fetchSemanticsNode().boundsInRoot.top, 2f)
    }

    @Test fun blueprintListOpensNativeMaterialsAndKeepsBackNavigation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expected = runBlocking { TerminalKnowledgeRepository(context).blueprints("A03", "", 1, false).rows.first().blueprintTitle() }
        compose.setContent { RefugeScene(RefugeColors.dark) {
            BlueprintDatabaseScreen(it, RefugeColors.dark, true, "A03", onMission = {}, onBack = {})
        } }
        compose.waitUntil(90_000) { compose.onAllNodesWithText(expected).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("blueprint-database").performScrollToIndex(4)
        val originalTop = compose.onNodeWithText(expected).fetchSemanticsNode().boundsInRoot.top
        save("blueprints-native-dark")
        compose.onNodeWithText(expected).performClick()
        compose.waitUntil(60_000) { compose.onAllNodesWithText("制造时间：", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(expected).assertIsDisplayed()
        compose.onNodeWithTag("blueprint-database").performScrollToIndex(2)
        save("blueprint-materials-native-dark")
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("制造蓝图").assertIsDisplayed()
        compose.waitUntil(60_000) { compose.onAllNodesWithText(expected).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(originalTop, compose.onNodeWithText(expected).fetchSemanticsNode().boundsInRoot.top, 2f)
    }

    @Test fun loadoutIsIndependentPageShowsDenseLiveDashboardInBothThemes() {
        val dark = mutableStateOf(false)
        compose.setContent {
            val palette = if (dark.value) RefugeColors.dark else RefugeColors.light
            RefugeScene(palette) { NativeLoadoutScreen(it, palette, dark.value, "aegs_avenger_stalker", onDismiss = {}) }
        }
        compose.waitUntil(90_000) { compose.onAllNodesWithText("火力 · Damage").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("refuge-sheet").assertDoesNotExist()
        compose.onNodeWithText("栏位 4").assertDoesNotExist()
        compose.onNodeWithText("Erkul LIVE ·", substring = true).assertExists()
        save("loadout-independent-light")
        compose.onNodeWithContentDescription("选择舰船").performClick()
        compose.onNodeWithTag("native-loadout").assertDoesNotExist()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithTag("native-loadout").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("火力 · Damage"))
        save("dashboard-light-damage")
        compose.onNodeWithText("Pilot").assertExists()
        compose.onNodeWithText("Manned turret").assertExists()
        compose.onNodeWithText("Remote turret").assertExists()
        compose.onNodeWithText("PDS").assertExists()
        compose.onNodeWithContentDescription("GIMBAL / AUTO / PRE").performClick()
        compose.onNodeWithContentDescription("GIMBAL / AUTO / PRE").assertIsSelected()
        compose.onNodeWithContentDescription("LOCK").performClick()
        compose.runOnIdle { dark.value = true }
        save("dashboard-dark-damage")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("防御 · Defense"))
        save("dashboard-dark-defense")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("飞行 · Flight"))
        compose.onNodeWithContentDescription("HND").performScrollTo().performClick()
        compose.onNodeWithContentDescription("HND").assertIsSelected()
        compose.onNodeWithContentDescription("SPD").performClick()
        compose.onNodeWithContentDescription("SPD").assertIsSelected()
        compose.onNodeWithContentDescription("STD").performClick()
        compose.onNodeWithText("基础 › 额定加力").performScrollTo()
        save("dashboard-dark-flight")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("电力分配"))
        compose.onNodeWithContentDescription("停用护盾").performScrollTo().performClick()
        compose.onNodeWithContentDescription("启用护盾").assertIsEnabled().performClick()
        compose.onNodeWithContentDescription("停用护盾").assertIsEnabled()
        compose.onNodeWithContentDescription("NAV").performScrollTo().performClick()
        compose.onNodeWithContentDescription("NAV").assertIsSelected()
        compose.onNodeWithContentDescription("SCM").performClick()
        compose.onNodeWithContentDescription("SCM").assertIsSelected()
        save("loadout-power-native-dark")
    }

    private fun save(name: String) {
        compose.waitForIdle()
        evidenceFile("$name.txt").writeText(compose.onRoot(useUnmergedTree = true).printToString())
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        evidenceFile("$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}

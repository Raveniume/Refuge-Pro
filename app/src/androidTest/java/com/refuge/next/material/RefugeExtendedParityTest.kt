package com.refuge.next.material

import android.view.KeyEvent
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.refuge.next.R
import com.refuge.next.data.*
import com.refuge.next.design.RefugeColors
import com.refuge.next.reference.OfficialLiquidBottomTabsPort
import com.refuge.next.screens.HangarScreen
import com.refuge.next.screens.OwnedShipHero
import com.refuge.next.screens.ProfileScreen
import com.refuge.next.screens.TerminalScreen
import com.refuge.next.screens.StoreUpgradePurchaseScreen
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class RefugeExtendedParityTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun officialTabsSupportSingleTapAndDragWithGrowingLens() {
        val selection = mutableIntStateOf(0)
        val labels = listOf("机库", "商店", "终端")
        compose.setContent {
            RefugeScene(RefugeColors.dark) { backdrop ->
                Box(Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("official-tabs-host")) {
                    OfficialLiquidBottomTabsPort(
                        selectedIndex = selection.intValue,
                        onSelected = { selection.intValue = it },
                        backdrop = backdrop,
                        tabsCount = labels.size,
                        isDark = true,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                            .fillMaxWidth().testTag("official-tabs"),
                    ) { current, select ->
                        labels.forEachIndexed { index, label ->
                            Box(
                                Modifier.weight(1f).fillMaxHeight()
                                    .semantics {
                                        role = androidx.compose.ui.semantics.Role.Tab
                                        selected = current == index
                                        contentDescription = label
                                    }
                                    .clickable(interactionSource = null, indication = null, role = androidx.compose.ui.semantics.Role.Tab) {
                                        select(index)
                                    },
                                contentAlignment = androidx.compose.ui.Alignment.Center,
                            ) {
                                Text(label, color = if (current == index) Color.Cyan else Color.White)
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        val initialLens = compose.onNodeWithTag("mode-lens").fetchSemanticsNode().boundsInRoot
        save("official-tabs-resting")
        val tabs = compose.onNodeWithTag("official-tabs")
        compose.mainClock.autoAdvance = false
        try {
            tabs.performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(300)
            val pressedLens = compose.onNodeWithTag("mode-lens").fetchSemanticsNode().boundsInRoot
            assertTrue("Pressed lens grows beyond its resting size", pressedLens.width > initialLens.width)
            save("official-tabs-pressed")
            tabs.performTouchInput { up() }
            compose.mainClock.advanceTimeBy(500)
        } finally {
            compose.mainClock.autoAdvance = true
        }
        compose.waitForIdle()
        tabs.performTouchInput { click(center.copy(x = width * .5f)) }
        compose.runOnIdle { assertEquals("A single tap selects a tab", 1, selection.intValue) }
        save("official-tabs-tapped")

        val bounds = tabs.fetchSemanticsNode().boundsInRoot
        val from = bounds.center.copy(x = bounds.left + bounds.width * .18f)
        val to = bounds.center.copy(x = bounds.left + bounds.width * .84f)
        compose.onNodeWithTag("official-tabs").performTouchInput {
            down(from - bounds.topLeft)
            moveTo(to - bounds.topLeft, delayMillis = 420)
            up()
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals("A drag settles on its target tab", 2, selection.intValue) }
        save("official-tabs-dragged")
    }

    @Test fun modeSelectorPressDragSnapCollapseAndBothEdges() {
        val selection = mutableIntStateOf(2)
        val labels = StoreCategory.entries.map { it.label }
        compose.setContent {
            RefugeScene(RefugeColors.light) { backdrop ->
                Column(Modifier.padding(top = 100.dp, start = 12.dp, end = 12.dp)) {
                    RefugeLiquidModeSelector(backdrop, RefugeColors.light, labels, selection.intValue, { selection.intValue = it })
                }
            }
        }
        val selector = compose.onNodeWithTag("refuge-liquid-mode-selector")
        val before = compose.onNodeWithTag("mode-track").fetchSemanticsNode().boundsInRoot.width
        save("selector-idle")
        compose.mainClock.autoAdvance = false
        try {
            selector.performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(400)
            selector.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "expanded"))
            assertTrue(compose.onNodeWithTag("mode-track").fetchSemanticsNode().boundsInRoot.width > before)
            save("selector-pressed")
            val step = compose.onNodeWithTag("mode-lens").fetchSemanticsNode().boundsInRoot.width
            selector.performTouchInput { moveBy(androidx.compose.ui.geometry.Offset(-step, 0f), delayMillis = 500) }
            compose.mainClock.advanceTimeBy(32)
            save("selector-drag")
            selector.performTouchInput { up() }
        } finally { compose.mainClock.autoAdvance = true }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(3, selection.intValue) }
        selector.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "collapsed"))
        save("selector-released")
        for (index in listOf(0, labels.lastIndex)) {
            compose.runOnIdle { selection.intValue = index }
            compose.waitForIdle()
            val lens = compose.onNodeWithTag("mode-lens").fetchSemanticsNode().boundsInRoot
            val label = compose.onNodeWithContentDescription(labels[index]).fetchSemanticsNode().boundsInRoot
            val track = compose.onNodeWithTag("mode-track").fetchSemanticsNode().boundsInRoot
            assertEquals(label.center.x, lens.center.x, 2f)
            assertTrue(lens.left >= track.left && lens.right <= track.right)
            assertEquals("The lens stays centered even at either end", track.center.x, lens.center.x, 2f)
            save("selector-edge-$index")
        }
    }

    @Test fun modeSelectorTapAndVerticalCancelDoNotLeaveExpandedState() {
        val selection = mutableIntStateOf(2)
        val labels = StoreCategory.entries.map { it.label }
        compose.setContent {
            RefugeScene(RefugeColors.dark) { backdrop ->
                Box(Modifier.padding(top = 100.dp, start = 12.dp, end = 12.dp)) {
                    RefugeLiquidModeSelector(backdrop, RefugeColors.dark, labels,
                        selection.intValue, { selection.intValue = it })
                }
            }
        }
        val selector = compose.onNodeWithTag("refuge-liquid-mode-selector")
        val next = selection.intValue + 1
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithContentDescription(labels[next]).performClick()
            compose.mainClock.advanceTimeBy(400)
        } finally { compose.mainClock.autoAdvance = true }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(next, selection.intValue) }
        val retained = selection.intValue
        selector.performTouchInput { down(center); moveBy(androidx.compose.ui.geometry.Offset(0f, 120f), 200); up() }
        compose.waitForIdle()
        assertEquals(retained, selection.intValue)
        selector.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "collapsed"))
    }

    @Test fun ownedHeroUsesStableContentMaterial() {
        val color = mutableStateOf(Color.Red)
        compose.setContent {
            RefugeScene(RefugeColors.light) { backdrop ->
                PageGlassScope(backdrop, content = {
                    Box(Modifier.fillMaxSize().background(color.value))
                }, overlay = { page ->
                    OwnedShipHero(page, RefugeColors.light,
                        OwnedShip("M80", "Package", "$300", "$140", "LTI", R.drawable.m80_hero), {},
                        Modifier.fillMaxWidth().height(140.dp).testTag("hero"), imageSize = 104.dp)
                })
            }
        }
        fun cornerColor(): Color {
            val pixels = compose.onNodeWithTag("hero").captureToImage().toPixelMap()
            return pixels[pixels.width * 3 / 4, pixels.height - 16]
        }
        val before = cornerColor()
        compose.runOnIdle { color.value = Color.Blue }
        val after = cornerColor()
        assertEquals("Content hero must keep a stable opaque surface", before.red, after.red, .01f)
        assertEquals("Content hero must not refract page color", before.blue, after.blue, .01f)
        save("hero-stable-content-material")
    }

    @Test fun detailFieldsActionGeometryItemLogAndEligibility() {
        val items = listOf(
            HangarItem("Fixture Package", "$140", "2026-09-05", R.drawable.m80_hero,
                originalName = "Original package", insurance = "LTI", currentValue = "$300", savings = "$160",
                includedItems = listOf("Digital Star Citizen", "Fixture flair"), id = 101, status = "Attributed", page = 2),
            HangarItem("Fixture CCU", "$20", "2026-09-04", R.drawable.m80_hero,
                isUpgrade = true, upgradeFrom = "Aurora", upgradeTo = "Arrow", upgradeFromPrice = "$30", upgradeToPrice = "$75", id = 102),
            HangarItem("Fixture Gifted", "$20", "2026-09-03", R.drawable.m80_hero,
                isGiftable = false, status = "Gifted", id = 103),
            HangarItem("Fixture Locked", "$0", "2026-09-02", R.drawable.m80_hero,
                isGiftable = false, isReclaimable = false, id = 104),
        )
        val repository = object : HangarRepository {
            override fun cachedInventory() = items
            override suspend fun inventory() = items
            override suspend fun ownedShips() = emptyList<OwnedShip>()
        }
        val logs = object : HangarLogRepository {
            override fun cachedEntries() = listOf(
                HangarLogEntry("1", name = "Package-only log", target = "101"),
                HangarLogEntry("2", name = "Other-item log", target = "102"),
            )
            override suspend fun entries() = cachedEntries()
        }
        compose.setContent {
            RefugeScene(RefugeColors.light) { backdrop ->
                PageGlassScope(backdrop, content = {
                    HangarScreen(backdrop, RefugeColors.light, repository,
                        object : BuybackRepository { override suspend fun items() = emptyList<BuybackItem>() },
                        logs, ProductionCcuRepository(), false, 0, {}, {}, {}, true, UserPresence.ONLINE, null, {})
                }, overlay = {})
            }
        }
        // With no translation provider the preserved original name is shown.
        compose.onNodeWithContentDescription("Original package").performClick()
        fun field(value: String) = compose.onAllNodes(hasText(value) and hasAnyAncestor(isDialog())).onFirst()
        fun action(value: String) = compose.onNode(hasContentDescription(value) and hasAnyAncestor(isDialog()))
        for (text in listOf("Original package", "$140", "$300", "$160", "入库日期", "2026-09-05", "LTI", "101", "Digital Star Citizen")) {
            field(text).performScrollTo().assertIsDisplayed()
        }
        action("升级").assertIsNotEnabled()
        val actionBounds = listOf("日志", "礼物", "跳转", "升级", "回收").map {
            action(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        }
        val density = compose.activity.resources.displayMetrics.density
        actionBounds.forEach { assertTrue(it.height >= 44f * density && it.width >= 44f * density) }
        assertTrue(actionBounds.zipWithNext().all { (a, b) -> a.right <= b.left + 1f })
        save("detail-fields-toolbar")
        action("日志").performClick()
        field("Package-only log").assertIsDisplayed()
        compose.onNodeWithText("Other-item log").assertDoesNotExist()
        assertTrue(compose.onNodeWithTag("hangar-detail-sheet").fetchSemanticsNode().boundsInRoot.height >= 510f * density)
        save("detail-item-log")
        back()
        compose.onNodeWithText("Fixture CCU").performClick()
        action("升级").assertIsEnabled()
        for (text in listOf("Aurora", "Arrow", "$30", "$75")) field(text).performScrollTo().assertIsDisplayed()
        save("detail-ccu-fields")
        back()
        compose.onNodeWithText("Fixture Gifted").performClick()
        action("召回").assertIsEnabled().performClick()
        field("召回礼物").assertIsDisplayed()
        save("detail-recall-confirmation")
        back()
        compose.onNodeWithText("Fixture Locked").performScrollTo().performClick()
        action("礼物").assertIsNotEnabled()
        action("回收").assertIsNotEnabled()
        save("detail-disabled-actions")
        back()
        compose.onAllNodesWithContentDescription("召回").onFirst().performScrollTo().performClick()
        field("召回礼物").assertIsDisplayed()
        save("inventory-recall-route")
        back()
        compose.onAllNodesWithContentDescription("赠送").onFirst().performScrollTo().performClick()
        field("收件人邮箱").assertIsDisplayed()
        save("inventory-gift-route")
        field("核对赠送信息").performScrollTo().performClick()
        field("请输入有效的收件人邮箱").assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("qa@example.com")
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("Fixture Recipient")
        field("核对赠送信息").performScrollTo().performClick()
        field("请输入当前账户密码").assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction())[2].performTextReplacement("fixture-password")
        field("核对赠送信息").performScrollTo().performClick()
        field("确认赠送信息").performScrollTo().assertIsDisplayed()
        field("qa@example.com").performScrollTo().assertIsDisplayed()
        field("Fixture Recipient").assertIsDisplayed()
        field("101").assertIsDisplayed()
        field("确认赠送").assertIsEnabled()
        save("gift-recipient-confirmation")
        // Stop before final confirmation; use only synthetic form values.
        field("修改收件信息").performClick()
        compose.onAllNodes(hasSetTextAction()).onFirst().assertTextContains("qa@example.com")
        back()
    }

    @Test fun purchaseSelectorExcludesOwnedAndPreservesSkuAvailability() {
        assertEquals(6000, nativeStorePriceCents(JSONObject("""{"nativePrice":{"amount":6000},"price":{"amount":6212}}""")))
        val catalogue = JSONObject("""{"data":{"ships":[
          {"id":10,"name":"Owned","msrp":10000,"skus":[{"id":101,"price":10000}]},
          {"id":20,"name":"Target","msrp":20000,"skus":[
            {"id":201,"title":"Warbond Edition","price":18000,"available":true},
            {"id":202,"title":"Standard Edition","price":20000,"available":false}]}
        ]}}""")
        val purchase = object : CcuPurchaseRepository {
            override fun cachedCatalog() = catalogue
            override suspend fun catalog() = catalogue
            override fun cachedSourceIds(toId: Int) = setOf(10)
            override suspend fun sourceIds(toId: Int) = setOf(10)
        }
        val hangar = object : HangarRepository {
            override fun cachedOwnedShips() = listOf(OwnedShip("Different localized name", "", "", "", "", 0, shipId = 10))
            override suspend fun ownedShips() = cachedOwnedShips()
            override suspend fun inventory() = emptyList<HangarItem>()
        }
        val translations = object : TranslationRepository {
            override fun translate(value: String) = when(value) { "Owned" -> "已有舰船"; "Target" -> "目标测试舰"; else -> value }
            override fun translateGameItem(value: String) = translate(value)
        }
        val auth = RsiAuthDataSource(InstrumentationRegistry.getInstrumentation().context)
        compose.setContent {
            CompositionLocalProvider(com.refuge.next.design.LocalRefugeTranslation provides translations) {
                RefugeScene(RefugeColors.light) { canvas ->
                    StoreUpgradePurchaseScreen(
                        backdrop = canvas,
                        palette = RefugeColors.light,
                        isDark = false,
                        rootTab = 0,
                        onNavigate = {},
                        onClose = {},
                        auth = auth,
                        purchaseRepository = purchase,
                        hangarRepository = hangar,
                        translationRepository = translations,
                        presence = UserPresence.ONLINE,
                        avatarUrl = null,
                        onAvatarClick = {},
                    )
                }
            }
        }
        compose.waitUntil(10000) { compose.onAllNodesWithText("目标测试舰").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("已有舰船").assertDoesNotExist()
        save("upgrade-targets")
        compose.onNodeWithText("目标测试舰").performClick()
        compose.onNodeWithText("标准版").assertIsNotEnabled()
        compose.onNodeWithText("战争债券版").assertIsEnabled()
        save("upgrade-editions")
        compose.onNodeWithText("战争债券版").performClick()
        compose.onNodeWithText("已有舰船").assertIsDisplayed().performClick()
        compose.onNodeWithText("RSI 升级差价").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("待核验").assertIsDisplayed()
        save("upgrade-cost-preview")
    }

    @Test fun ownedUpgradeUsesServerEligibilityAndRetriesFailures() {
        val upgrade = HangarItem("Owned CCU", "$10", "", 0, id = 10, isUpgrade = true, upgradeFrom = "Aurora")
        val eligible = HangarItem("Eligible localized pledge", "$30", "", 0, id = 20, containedShip = "极光")
        val ineligible = HangarItem("Not approved by RSI", "$30", "", 0, id = 30, containedShip = "Aurora")
        var attempts = 0
        val repository = object : HangarRepository {
            override suspend fun ownedShips() = emptyList<OwnedShip>()
            override suspend fun inventory() = listOf(upgrade, eligible, ineligible)
            override suspend fun upgradeTargets(upgradeId: Long): List<HangarItem> {
                assertEquals(10L, upgradeId)
                if (attempts++ == 0) error("Eligibility offline")
                return listOf(eligible)
            }
        }
        compose.setContent {
            RefugeScene(RefugeColors.light) { canvas ->
                com.refuge.next.screens.HangarOwnedCcuApplyScreen(canvas, RefugeColors.light, false, 0, {},
                    repository, 10, UserPresence.ONLINE, null, {})
            }
        }
        compose.onNodeWithText("Eligibility offline").assertIsDisplayed()
        compose.onNodeWithText("重试").performClick()
        compose.onNodeWithText("Not approved by RSI").assertDoesNotExist()
        compose.onNodeWithText("Eligible localized pledge").assertIsDisplayed().performClick()
        compose.onNodeWithText("验证应用请求").assertIsDisplayed()
        save("owned-ccu-server-eligibility")
    }

    @Test fun profileToolsUseTwoColumnsBelowStatistics() {
        val profile = ProfileData(handle = "Fixture Pilot", isAuthenticated = true, totalSpent = "$1000",
            hangarValue = "$440", currentValue = "$900", credit = "$120")
        val repository = object : ProfileRepository {
            override fun cachedProfile() = profile
            override suspend fun profile() = profile
        }
        compose.setContent {
            RefugeScene(RefugeColors.light) { canvas ->
                ProfileScreen(canvas, RefugeColors.light, false, 4, {}, repository,
                    ProductionUtilityRepository(), {}, {}, true, UserPresence.ONLINE, profile, onToggleOnline = {})
            }
        }
        for (text in listOf("消费额", "机库价值", "信用点")) compose.onNodeWithText(text).assertIsDisplayed()
        save("profile-statistics")
        compose.onNodeWithContentDescription("玩家搜索").performScrollTo()
        val left = compose.onNodeWithContentDescription("众筹查询").fetchSemanticsNode().boundsInRoot
        val right = compose.onNodeWithContentDescription("玩家搜索").fetchSemanticsNode().boundsInRoot
        assertEquals(left.top, right.top, 1f)
        assertTrue(left.right < right.left)
        assertEquals(left.width, right.width, 2f)
        save("profile-tools-columns")
    }

    @Test fun terminalAllCategoriesRetainDetailedFieldsAndDescriptions() {
        val items = TerminalCategory.entries.map { category ->
            TerminalItem(category.name, "Fixture ${category.name}", "Fixture manufacturer", category,
                listOf("Grade A", "Size 2"), "12345 aUEC", "$100", "Fixture description for ${category.name}",
                "android.resource://com.refuge.next.compose/${R.drawable.m80_hero}",
                listOf("尺寸" to "2", "质量" to "123 kg", "功耗" to "987 W", "耐久" to "765 HP"))
        }
        val repository = object : TerminalRepository {
            override fun cachedItems() = items
            override suspend fun items() = items
        }
        compose.setContent {
            RefugeScene(RefugeColors.light) { canvas ->
                PageGlassScope(canvas, content = {
                    TerminalScreen(canvas, RefugeColors.light, false, 2, {}, repository, true, UserPresence.ONLINE, null, {})
                }, overlay = {})
            }
        }
        for (item in items) {
            compose.onNodeWithContentDescription(item.category.label).performScrollTo().performClick()
            compose.onNodeWithText(item.name).performClick()
            for (value in listOf("12345 aUEC", "123 kg", "987 W", "765 HP", item.description)) {
                compose.onNode(hasText(value) and hasAnyAncestor(isDialog())).performScrollTo().assertIsDisplayed()
            }
            save("terminal-${item.category.name}")
            compose.onNodeWithText("完成").performClick()
        }
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }
    private fun save(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(250)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val resolver = compose.activity.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "refuge-qa-$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Refuge QA")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        requireNotNull(resolver.openOutputStream(uri)).use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }, null, null)
        }
    }
}

package com.refuge.next.material

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.refuge.next.MainActivity
import com.refuge.next.data.RsiAuthDataSource
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import com.refuge.next.data.PreferencesSettingsRepository

/** Optional authenticated, read-only QA. Never replaces the user's saved session. */
class RefugeRootSessionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun rootNavigationRetainsStoreSearchAndCategory() {
        assumeTrue(RsiAuthDataSource(compose.activity).session() != null)
        compose.waitUntil(20000) { compose.onAllNodesWithContentDescription("商店").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithContentDescription("商店").onLast().performClick()
        compose.onNodeWithContentDescription("涂装").performClick()
        compose.onNodeWithContentDescription("搜索商品").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("M80")
        compose.onAllNodesWithContentDescription("终端").onLast().performClick()
        compose.onAllNodesWithContentDescription("商店").onLast().performClick()
        compose.onNode(hasSetTextAction()).assertTextEquals("M80")
        compose.onNodeWithContentDescription("涂装").assertIsSelected()
        save("root-search-restored")
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNodeWithContentDescription("搜索商品").performClick()
        compose.onNodeWithContentDescription("舰船").performClick()
        compose.onAllNodesWithContentDescription("机库").onLast().performClick()
    }

    @Test fun themeChangesRootPersistsAndCacheClearingCompletes() {
        assumeTrue(RsiAuthDataSource(compose.activity).session() != null)
        val settings = PreferencesSettingsRepository(compose.activity)
        fun openSettings() {
            compose.onAllNodesWithContentDescription("我的").onLast().performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("设置"))
            compose.onNodeWithContentDescription("设置").performClick()
        }
        compose.waitUntil(20000) { compose.onAllNodesWithContentDescription("我的").fetchSemanticsNodes().isNotEmpty() }
        openSettings()
        if (settings.load().darkTheme) compose.onNodeWithContentDescription("深色主题").performClick()
        compose.onNodeWithText("已关闭").assertIsDisplayed()
        save("root-theme-light")
        compose.onNodeWithContentDescription("深色主题").performClick()
        compose.onNodeWithText("已开启").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("机库").onLast().performClick()
        save("root-theme-dark-hangar")
        compose.activityRule.scenario.recreate()
        compose.waitUntil(20000) { compose.onAllNodesWithText("我的机库").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(PreferencesSettingsRepository(compose.activity).load().darkTheme)
        save("root-theme-dark-recreated")
        openSettings()
        compose.onNodeWithText("已开启").assertIsDisplayed()
        compose.onNodeWithContentDescription("深色主题").performClick()
        compose.onNodeWithText("已关闭").assertIsDisplayed()
        compose.onNodeWithText("清理缓存").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("图片缓存已清理").fetchSemanticsNodes().isNotEmpty() }
        save("root-cache-cleared")
        assertTrue(RsiAuthDataSource(compose.activity).session() != null)
        compose.onAllNodesWithContentDescription("机库").onLast().performClick()
        save("root-light-hangar-final")
    }

    @Test fun recordCompactSelectorStackAndBottomNavigation() {
        assumeTrue(RsiAuthDataSource(compose.activity).session() != null)
        compose.waitUntil(20000) { compose.onAllNodesWithContentDescription("回购").fetchSemanticsNodes().isNotEmpty() }
        compose.mainClock.advanceTimeBy(1500)
        compose.waitForIdle()
        // This optional authenticated smoke test depends on the user's live hangar.
        // Other session tests may clear the cache, and a valid session alone does
        // not guarantee that the account currently exposes an M80 entry.
        val hasM80 = compose.onAllNodesWithContentDescription("M80")
            .fetchSemanticsNodes().isNotEmpty()
        assumeTrue("Live hangar has no M80 fixture; skip optional smoke test", hasM80)
        save("compact-glass-ready")
        val hangar = compose.onAllNodesWithContentDescription("机库").onFirst().fetchSemanticsNode().boundsInRoot.center
        val buyback = compose.onNodeWithContentDescription("回购").fetchSemanticsNode().boundsInRoot.center
        val upgrade = compose.onNodeWithContentDescription("升级").fetchSemanticsNode().boundsInRoot.center
        touch(buyback, buyback)
        compose.waitForIdle()
        save("compact-tap")
        touch(buyback, upgrade)
        compose.waitForIdle()
        save("compact-drag-release")
        for (point in listOf(hangar, buyback, upgrade, hangar)) touch(point, point)
        compose.waitForIdle()
        save("compact-fast-taps")
        compose.onAllNodesWithContentDescription("机库").onFirst().performClick()
        val hero = compose.onNodeWithContentDescription("M80").fetchSemanticsNode().boundsInRoot
        touch(hero.center, hero.center.copy(y = hero.top + 5f))
        compose.waitForIdle()
        compose.onNode(isDialog()).assertDoesNotExist()
        org.junit.Assert.assertEquals(hero.width,
            compose.onNodeWithContentDescription("Perseus").fetchSemanticsNode().boundsInRoot.width, 3f)
        save("hero-stack-drag")
        for (label in listOf("商店", "终端", "我的", "机库")) {
            compose.onAllNodesWithContentDescription(label).onLast().performClick()
            compose.waitForIdle()
            save("nav-${when(label) { "商店" -> "store"; "终端" -> "terminal"; "我的" -> "profile"; else -> "hangar" }}")
        }
    }

    private fun touch(from: androidx.compose.ui.geometry.Offset, to: androidx.compose.ui.geometry.Offset) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val started = android.os.SystemClock.uptimeMillis()
        fun send(action: Int, point: androidx.compose.ui.geometry.Offset) {
            val event = android.view.MotionEvent.obtain(started, android.os.SystemClock.uptimeMillis(), action, point.x, point.y, 0)
            instrumentation.sendPointerSync(event)
            event.recycle()
        }
        send(android.view.MotionEvent.ACTION_DOWN, from)
        if (from == to) android.os.SystemClock.sleep(65) else {
            for (step in 1..16) {
                android.os.SystemClock.sleep(24)
                send(android.view.MotionEvent.ACTION_MOVE, from + (to - from) * (step / 16f))
            }
        }
        send(android.view.MotionEvent.ACTION_UP, to)
    }

    private fun save(name: String) {
        compose.waitForIdle()
        compose.onAllNodes(isRoot()).onLast().captureToImage()
        android.os.SystemClock.sleep(250)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

package com.refuge.next.material

import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.refuge.next.MainActivity
import com.refuge.next.data.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Opens production routes through MainActivity with the existing real session. */
class RefugeToolsRootTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun terminalToolsOpenRefreshAndReturnInBothThemes() {
        assertTrue("A real signed-in session is required", RsiAuthDataSource(compose.activity).session() != null)
        val settings = PreferencesSettingsRepository(compose.activity)
        for (dark in listOf(false, true)) {
            tap("我的", last = true)
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("设置"))
            tap("设置")
            compose.onAllNodesWithContentDescription("终端").assertCountEquals(0)
            if (settings.load().darkTheme != dark) tap("深色主题")
            if (settings.load().doubleLayerGlass != !dark) tap("双层液态玻璃")
            compose.runOnIdle { assertTrue(settings.load().doubleLayerGlass == !dark) }
            save("root-settings-${if (dark) "dark" else "light"}")
            tap("返回")
            tap("终端", last = true)
            compose.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(
                androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)).performScrollToIndex(0)
            tap("改船")
            compose.waitUntil(90_000) { compose.onAllNodesWithText("更换").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("配装定制").assertIsSelected()
            compose.onAllNodesWithContentDescription("我的").assertCountEquals(0)
            save("root-tool-${if (dark) "dark" else "light"}-customization")
            back()
            tap("制造蓝图")
            compose.waitUntil(90_000) { compose.onAllNodesWithTag("blueprint-database").fetchSemanticsNodes().isNotEmpty() }
            tap("更新蓝图")
            compose.waitUntil(90_000) { compose.onAllNodesWithText("蓝图数据已", substring = true).fetchSemanticsNodes().isNotEmpty() }
            tap("EN")
            compose.onNodeWithContentDescription("中文").assertIsDisplayed()
            save("root-tool-${if (dark) "dark" else "light"}-blueprints-en")
            tap("中文")
            back()
            tap("任务数据库")
            compose.waitUntil(90_000) { compose.onAllNodesWithTag("mission-database").fetchSemanticsNodes().isNotEmpty() }
            tap("更新任务")
            compose.waitUntil(90_000) { compose.onAllNodesWithText("任务数据已", substring = true).fetchSemanticsNodes().isNotEmpty() }
            save("root-tool-${if (dark) "dark" else "light"}-missions")
            back()
            compose.onAllNodesWithContentDescription("我的").onLast().assertIsDisplayed()
        }
    }

    private fun tap(label: String, last: Boolean = false) {
        val nodes = compose.onAllNodesWithContentDescription(label)
        val node = if (last) nodes.onLast() else nodes.onFirst()
        compose.waitUntil(30_000) { runCatching { node.isDisplayed() }.getOrDefault(false) }
        node.performTouchInput { click() }
        compose.waitForIdle()
    }
    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }
    private fun save(name: String) {
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        evidenceFile("$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

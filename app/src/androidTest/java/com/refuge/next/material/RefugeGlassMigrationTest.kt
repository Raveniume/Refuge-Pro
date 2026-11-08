package com.refuge.next.material

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.refuge.next.design.RefugeColors
import com.refuge.next.reference.ReferenceSearchField
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RefugeGlassMigrationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun liquidSwitchDoesNotToggleDuringVerticalScrollAndSupportsDragAndAccessibility() {
        val enabled = mutableStateOf(false)
        val dark = mutableStateOf(false)
        compose.setContent {
            val palette = if (dark.value) RefugeColors.dark else RefugeColors.light
            RefugeScene(palette) { backdrop ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp)) {
                    Spacer(Modifier.height(100.dp))
                    RefugeLiquidToggle(backdrop, palette, enabled.value, { enabled.value = !enabled.value }, "测试开关")
                    Spacer(Modifier.height(1400.dp))
                }
            }
        }
        val toggle = compose.onNodeWithContentDescription("测试开关")
        toggle.assertHeightIsAtLeast(44.dp).assertIsOff()
        toggle.performTouchInput { swipe(center, center + Offset(0f, 80f), 350) }
        toggle.assertIsOff()
        for (isDark in listOf(false, true)) {
            compose.runOnIdle { dark.value = isDark; enabled.value = false }
            compose.mainClock.autoAdvance = false
            toggle.performTouchInput {
                down(Offset(2f, centerY))
                moveTo(Offset(width * .6f, centerY), delayMillis = 150)
            }
            compose.mainClock.advanceTimeBy(160)
            save("liquid-switch-${if (isDark) "dark" else "light"}-dragged")
            toggle.performTouchInput { up() }
            compose.mainClock.autoAdvance = true
            compose.runOnIdle { enabled.value = false }
            toggle.performTouchInput { swipe(Offset(2f, centerY), Offset(width - 2f, centerY), 400) }
            toggle.assertIsOn()
            save("liquid-switch-${if (isDark) "dark" else "light"}")
            toggle.performClick()
            toggle.assertIsOff()
        }
    }

    @Test fun doubleLayerSearchEditsAndClearsInBothThemes() {
        val text = mutableStateOf("")
        val dark = mutableStateOf(false)
        compose.setContent {
            val palette = if (dark.value) RefugeColors.dark else RefugeColors.light
            CompositionLocalProvider(LocalDoubleLayerGlass provides true) {
                RefugeScene(palette) { backdrop ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 100.dp)) {
                        ReferenceSearchField(backdrop, dark.value, text.value, { text.value = it }, RefugeIcons.search)
                    }
                }
            }
        }
        for (isDark in listOf(false, true)) {
            compose.runOnIdle { dark.value = isDark }
            compose.onNode(hasSetTextAction()).performTextInput("Avenger")
            compose.runOnIdle { assertEquals("Avenger", text.value) }
            save("double-search-${if (isDark) "dark" else "light"}")
            compose.onNodeWithContentDescription("清空搜索").performClick()
            compose.runOnIdle { assertEquals("", text.value) }
        }
    }

    @Test fun elasticSheetReturnsAfterShortDragAndDismissesAfterLongDrag() {
        val shown = mutableStateOf(true)
        val dark = mutableStateOf(false)
        compose.setContent {
            val palette = if (dark.value) RefugeColors.dark else RefugeColors.light
            RefugeScene(palette) { backdrop ->
                if (shown.value) RefugeLiquidSheet(backdrop, palette, "面板", { shown.value = false }, sheetHeight = 500.dp) {
                    Text("拖动回弹与关闭", color = palette.text)
                }
            }
        }
        for (isDark in listOf(false, true)) {
            compose.runOnIdle { dark.value = isDark; shown.value = true }
            val handle = compose.onNodeWithTag("sheet-drag-handle")
            val initial = handle.fetchSemanticsNode().boundsInRoot.top
            handle.performTouchInput { swipe(center, center + Offset(0f, 65f), 600) }
            compose.waitForIdle()
            assertEquals(initial, handle.fetchSemanticsNode().boundsInRoot.top, 2f)
            save("elastic-sheet-${if (isDark) "dark" else "light"}")
            handle.performTouchInput { swipe(center, center + Offset(0f, 900f), 700) }
            compose.waitForIdle()
            compose.runOnIdle { assertFalse(shown.value) }
        }
    }

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        evidenceFile("$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}

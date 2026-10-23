package com.refuge.next.material

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.refuge.next.design.RefugeColors
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RefugeLiquidSliderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun officialThumbTapsDragsAndRangeUpdatesInBothThemes() {
        val value = mutableStateOf(500f)
        val minimum = mutableStateOf(0f)
        val dark = mutableStateOf(false)
        compose.setContent {
            val palette = if (dark.value) RefugeColors.dark else RefugeColors.light
            RefugeScene(palette) { backdrop ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 120.dp)) {
                    RefugeLiquidSlider({ value.value }, { value.value = it }, minimum.value..1000f,
                        1f, backdrop, palette, "材料品质")
                }
            }
        }
        for (isDark in listOf(false, true)) {
            compose.runOnIdle { dark.value = isDark; value.value = 500f }
            val slider = compose.onNodeWithTag("liquid-slider")
            slider.assertHeightIsAtLeast(48.dp)
            slider.performTouchInput { click(Offset(width * .8f, height * .2f)) }
            compose.runOnIdle { assertEquals(800f, value.value, 8f) }
            val thumb = compose.onNodeWithTag("liquid-slider-thumb")
            compose.mainClock.autoAdvance = false
            thumb.performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(200)
            save("slider-${if (isDark) "dark" else "light"}-pressed")
            thumb.performTouchInput { moveBy(Offset(-100f, 0f), delayMillis = 200) }
            compose.mainClock.advanceTimeBy(32)
            save("slider-${if (isDark) "dark" else "light"}-dragged")
            thumb.performTouchInput { up() }
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            compose.runOnIdle { assertTrue("Dragging must change quality", value.value < 790f) }
            save("slider-${if (isDark) "dark" else "light"}-resting")
        }
        compose.runOnIdle { minimum.value = 600f; value.value = 700f }
        compose.onNodeWithTag("liquid-slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        compose.runOnIdle { assertEquals(600f, value.value, .01f) }
        compose.onNodeWithTag("liquid-slider").performTouchInput { click(Offset(width * .5f, height * .2f)) }
        compose.runOnIdle { assertEquals(800f, value.value, 8f) }
        val width = compose.onNodeWithTag("liquid-slider").fetchSemanticsNode().size.width
        compose.onNodeWithTag("liquid-slider-thumb").performTouchInput {
            down(center); moveBy(Offset(-100f, 0f), delayMillis = 200); up()
        }
        compose.runOnIdle { assertEquals("Thumb must use the new range after switching material",
            800f - 400f * 100f / width, value.value, 4f) }
    }

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        evidenceFile("$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}

package com.refuge.next.material

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.refuge.next.R
import com.refuge.next.design.RefugeColors
import com.refuge.next.reference.OfficialLiquidButtonPort
import com.refuge.next.screens.RootBottomNav
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RefugeMaterialRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun floatingGlassSamplesContentWhilePanelsStayOpaque() {
        val artColor = mutableStateOf(Color(0xFFDD493B))
        val dark = mutableStateOf(false)
        val width = mutableStateOf(360.dp)
        val selected = mutableStateOf(0)
        compose.setContent {
            val palette = if (dark.value) RefugeColors.dark else RefugeColors.light
            Box(Modifier.requiredWidth(width.value).fillMaxSize().testTag("scene")) {
                RefugeScene(palette) { canvas ->
                    PageGlassScope(
                        backdrop = canvas,
                        content = {
                            Box(Modifier.fillMaxSize().background(artColor.value))
                            Column(Modifier.padding(24.dp)) {
                                RefugeContentSurface(
                                    palette,
                                    Modifier.fillMaxWidth().height(72.dp).testTag("panel"),
                                    padding = PaddingValues(16.dp),
                                ) { Text("M80", color = palette.text) }
                                Image(
                                    painterResource(R.drawable.m80_hero),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(180.dp),
                                )
                            }
                        },
                        overlay = { page ->
                            RefugeLiquidGlass(
                                page, palette,
                                Modifier.align(Alignment.Center).size(180.dp, 64.dp).testTag("glass"),
                                radius = 32.dp,
                            ) {}
                            RootBottomNav(page, dark.value, selected.value) { selected.value = it }
                        },
                    )
                }
            }
        }
        for (isDark in listOf(false, true)) {
            for (viewport in listOf(360.dp, 390.dp)) {
                compose.runOnIdle { dark.value = isDark; width.value = viewport; artColor.value = Color(0xFFDD493B) }
                compose.waitForIdle()
                val panelBefore = mean(compose.onNodeWithTag("panel").captureToImage())
                val glassBefore = mean(compose.onNodeWithTag("glass").captureToImage())
                compose.runOnIdle { artColor.value = Color(0xFF2872DC) }
                compose.waitForIdle()
                val panelAfter = mean(compose.onNodeWithTag("panel").captureToImage())
                val glassAfter = mean(compose.onNodeWithTag("glass").captureToImage())
                assertTrue("Floating glass must sample changing page content", glassBefore[0] - glassAfter[0] > .15f)
                assertTrue("Blue content must reach the glass", glassAfter[2] - glassBefore[2] > .15f)
                assertEquals("Content panel must not refract its background", panelBefore[0], panelAfter[0], .01f)
                save("materials-${if (isDark) "dark" else "light"}-${viewport.value.toInt()}")
            }
        }
        compose.onNodeWithText("\u5546\u5e97").performClick()
        compose.runOnIdle { assertEquals(1, selected.value) }
        val scene = compose.onNodeWithTag("scene")
        val origin = scene.fetchSemanticsNode().boundsInRoot.topLeft
        val start = compose.onNodeWithText("\u5546\u5e97").fetchSemanticsNode().boundsInRoot.center - origin
        val end = compose.onNodeWithText("\u7ec8\u7aef").fetchSemanticsNode().boundsInRoot.center - origin
        compose.mainClock.autoAdvance = false
        try {
            scene.performTouchInput { down(start); moveTo(end, delayMillis = 240) }
            compose.mainClock.advanceTimeBy(160)
            save("tabs-pressed")
            scene.performTouchInput { up() }
        } finally {
            compose.mainClock.autoAdvance = true
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals("Dragging the lens must select the destination", 2, selected.value) }
    }

    @Test fun whiteButtonShadowExtendsOutsideItsBounds() {
        compose.setContent {
            Box(Modifier.size(280.dp, 180.dp).background(Color.White).testTag("scene")) {
                RefugeScene(RefugeColors.light) { canvas ->
                    OfficialLiquidButtonPort(
                        onClick = {}, backdrop = canvas,
                        modifier = Modifier.align(Alignment.Center).size(96.dp, 48.dp),
                        surfaceColor = Color.White,
                    ) {}
                }
            }
        }
        val pixels = compose.onNodeWithTag("scene").captureToImage().toPixelMap()
        val y = ((180f / 2f + 48f / 2f + 3f) / 180f * pixels.height).toInt()
        val shadow = pixels[pixels.width / 2, y]
        val canvas = pixels[pixels.width / 2, pixels.height - 4]
        assertTrue("External shadow must survive clipping on a light canvas", shadow.red < canvas.red - .005f)
        save("white-button-shadow")
    }

    @Test fun modalGlassSamplesThePageAcrossDialogWindows() {
        val artColor = mutableStateOf(Color(0xFFDD493B))
        val showSheet = mutableStateOf(true)
        val dark = mutableStateOf(false)
        compose.setContent {
            val palette = if (dark.value) RefugeColors.dark else RefugeColors.light
            RefugeScene(palette) { canvas ->
                PageGlassScope(
                    backdrop = canvas,
                    content = {
                        Box(Modifier.fillMaxSize().background(artColor.value))
                        if (showSheet.value) {
                            RefugeLiquidSheet(
                                canvas, palette, "", onDismiss = {},
                                modifier = Modifier.testTag("sheet"), sheetHeight = 280.dp,
                                action = { page ->
                                    RefugeLiquidGlass(page, palette, Modifier.fillMaxWidth().height(56.dp)) {
                                        Text("Action", color = palette.text)
                                    }
                                },
                            ) { Box(Modifier.fillMaxWidth().height(80.dp)) }
                        } else {
                            RefugeDialog(canvas, palette, "M80", "Detail", "Done", {}, {})
                        }
                    },
                    overlay = {},
                )
            }
        }
        for (isDark in listOf(false, true)) {
            for (sheet in listOf(true, false)) {
                compose.runOnIdle { dark.value = isDark; showSheet.value = sheet; artColor.value = Color(0xFFDD493B) }
                compose.waitForIdle()
                val modal = if (sheet) compose.onNodeWithTag("sheet") else compose.onNode(isDialog())
                val before = mean(modal.captureToImage())
                compose.runOnIdle { artColor.value = Color(0xFF2872DC) }
                compose.waitForIdle()
                val rendered = modal.captureToImage()
                val after = mean(rendered)
                assertTrue("Modal must include its live page underlay", before[0] - after[0] > .02f)
                assertTrue("Modal must update when the page changes", after[2] - before[2] > .02f)
                save("${if (sheet) "sheet" else "dialog"}-${if (isDark) "dark" else "light"}", rendered)
            }
        }
    }

    private fun mean(image: ImageBitmap): FloatArray {
        val pixels = image.toPixelMap()
        val result = FloatArray(3)
        var count = 0
        for (y in pixels.height / 4 until pixels.height * 3 / 4 step 4) {
            for (x in pixels.width / 4 until pixels.width * 3 / 4 step 4) {
                val color = pixels[x, y]
                result[0] += color.red; result[1] += color.green; result[2] += color.blue
                count++
            }
        }
        return result.map { it / count }.toFloatArray()
    }

    private fun save(name: String, rendered: ImageBitmap = compose.onNodeWithTag("scene").captureToImage()) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "$name.png")
        val image = rendered.asAndroidBitmap()
        file.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}

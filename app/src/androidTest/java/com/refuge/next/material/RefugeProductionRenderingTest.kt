package com.refuge.next.material

import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.MotionEvent
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import com.refuge.next.R
import com.refuge.next.data.BuybackItem
import com.refuge.next.data.BuybackRepository
import com.refuge.next.data.HangarItem
import com.refuge.next.data.HangarRepository
import com.refuge.next.data.InMemoryCartRepository
import com.refuge.next.data.OwnedShip
import com.refuge.next.data.ProductionCcuRepository
import com.refuge.next.data.ProductionHangarLogRepository
import com.refuge.next.data.StoreCategory
import com.refuge.next.data.StoreProduct
import com.refuge.next.data.StoreRepository
import com.refuge.next.data.TerminalCategory
import com.refuge.next.data.TerminalItem
import com.refuge.next.data.TerminalRepository
import com.refuge.next.data.UserPresence
import com.refuge.next.design.RefugeColors
import com.refuge.next.screens.HangarScreen
import com.refuge.next.screens.RootBottomNav
import com.refuge.next.screens.StoreScreen
import com.refuge.next.screens.TerminalScreen
import java.io.File
import java.util.Collections
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RefugeProductionRenderingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun productionListsAndModalActionsRemainUsable() {
        val image = "android.resource://com.refuge.next.compose/${R.drawable.m80_hero}"
        val products = List(60) { index ->
            StoreProduct("ship-$index", "M80 ${index.toString().padStart(2, '0')}", StoreCategory.SHIPS,
                "Mirai", 8000 + index * 100, image, "M80", isWarbond = index % 3 == 0)
        }
        val store = object : StoreRepository {
            override fun cachedProducts() = products
            override suspend fun products() = products
        }
        val terminalItems = products.map {
            TerminalItem(it.id, it.title, "Mirai", TerminalCategory.VEHICLES,
                listOf("Racing"), "80", "$80", "M80", imageUrl = image)
        }
        val terminal = object : TerminalRepository {
            override fun cachedItems() = terminalItems
            override suspend fun items() = terminalItems
        }
        // Missing legacy IDs must not turn individually recycled rows into duplicate keys.
        val inventory = products.map { HangarItem(it.title, "$80", "2026-09-05", R.drawable.m80_hero) }
        val hangar = object : HangarRepository {
            override fun cachedInventory() = inventory
            override suspend fun inventory() = inventory
            override suspend fun ownedShips() = emptyList<OwnedShip>()
        }
        val buyback = object : BuybackRepository {
            override suspend fun items() = emptyList<BuybackItem>()
        }
        val logs = ProductionHangarLogRepository()
        val ccu = ProductionCcuRepository()
        val cart = InMemoryCartRepository()
        val route = mutableStateOf(1)
        val dark = mutableStateOf(false)
        val optics = mutableStateOf(true)
        val reusePage = mutableStateOf(true)
        compose.setContent {
            val palette = if (dark.value) RefugeColors.dark else RefugeColors.light
            CompositionLocalProvider(LocalOpticalGlassEnabled provides optics.value,
                LocalRasterizePageBackdrop provides reusePage.value) {
            Box(Modifier.fillMaxSize().testTag("scene")) {
                RefugeScene(palette) { canvas ->
                    PageGlassScope(canvas, content = {
                        when (route.value) {
                            0 -> HangarScreen(canvas, palette, hangar, buyback, logs, ccu,
                                dark.value, 0, { route.value = it }, {}, {}, true, UserPresence.ONLINE, null, {})
                            1 -> StoreScreen(canvas, palette, store, cart, dark.value, 1,
                                { route.value = it }, {}, true, UserPresence.ONLINE, null, {})
                            2 -> TerminalScreen(canvas, palette, dark.value, 2,
                                { route.value = it }, terminal, true, UserPresence.ONLINE, null, {})
                        }
                    }, overlay = { page ->
                        RootBottomNav(page, dark.value, route.value) { route.value = it }
                    })
                }
            }
            }
        }

        compose.onNodeWithText("M80 00").assertIsDisplayed()
        save("store-light")
        compose.onNodeWithText("M80 00").performClick()
        compose.onNodeWithText("\u52a0\u5165\u8d2d\u7269\u8f66").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, cart.lines().sumOf { it.quantity }) }
        save("store-detail-light", dialog = true)
        pressBack()
        compose.runOnIdle { dark.value = true }
        save("store-dark")
        compose.onNodeWithText("M80 00").performClick()
        compose.onNodeWithText("\u52a0\u5165\u8d2d\u7269\u8f66").assertIsDisplayed()
        save("store-detail-dark", dialog = true)
        pressBack()

        val list = compose.onNode(hasScrollToIndexAction())
        list.performTouchInput { swipeUp(durationMillis = 350) }
        list.performScrollToIndex(0)
        for ((run, reuse) in listOf(false, true, false, true).withIndex()) {
            compose.runOnIdle { reusePage.value = reuse }
            list.performScrollToIndex(0)
            nativeSwipe(up = true)
            list.performScrollToIndex(0)
            save("raster-${if (reuse) "cached" else "baseline"}-$run")
            recordScrollFrames(reuse, run) {
                repeat(4) { nativeSwipe(up = true) }
                repeat(4) { nativeSwipe(up = false) }
            }
        }
        compose.runOnIdle { reusePage.value = true }
        list.performScrollToIndex(0)
        compose.onNodeWithText("M80 00").assertIsDisplayed()
        compose.onNodeWithText("\u7ec8\u7aef").performClick()
        compose.onNodeWithText("M80 00").assertIsDisplayed()
        save("terminal-dark")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(30)
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNodeWithText("M80 00").performClick()
        compose.onNode(isDialog()).assertExists()
        save("terminal-detail-dark", dialog = true)
        pressBack()
        compose.onNodeWithText("\u673a\u5e93").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(5)
        save("hangar-dark")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(30)
        compose.runOnIdle { dark.value = false }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(5)
        save("hangar-light")
    }

    private fun recordScrollFrames(reuse: Boolean, run: Int, workload: () -> Unit) {
        val frames = Collections.synchronizedList(mutableListOf<LongArray>())
        val thread = HandlerThread("material-frame-metrics").apply { start() }
        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            frames.add(longArrayOf(metrics.getMetric(FrameMetrics.TOTAL_DURATION), metrics.getMetric(FrameMetrics.DEADLINE),
                metrics.getMetric(FrameMetrics.DRAW_DURATION), metrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION),
                metrics.getMetric(FrameMetrics.SYNC_DURATION), metrics.getMetric(FrameMetrics.GPU_DURATION)))
        }
        compose.runOnIdle { compose.activity.window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper)) }
        try {
            workload()
            compose.waitForIdle()
        } finally {
            compose.runOnIdle { compose.activity.window.removeOnFrameMetricsAvailableListener(listener) }
            thread.quitSafely()
            thread.join(3000)
        }
        val snapshot = synchronized(frames) { frames.toList() }
        assertTrue("Scrolling must produce measured frames", snapshot.isNotEmpty())
        val sorted = snapshot.map { it[0] / 1_000_000.0 }.sorted()
        val result = JSONObject()
            .put("workload", "Debug/API35 emulator; 60 local-image store rows, dark theme; 4 upward + 4 downward native touch swipes at 16ms intervals after warmup")
            .put("optics", true)
            .put("rasterizePageBackdrop", reuse)
            .put("run", run)
            .put("frames", sorted.size)
            .put("overDeadline", snapshot.count { it[1] > 0 && it[0] > it[1] })
            .put("medianMs", sorted[sorted.size / 2])
            .put("p95Ms", sorted[((sorted.size - 1) * .95).toInt()])
        for ((column, label) in listOf(2 to "draw", 3 to "layout", 4 to "sync", 5 to "gpu")) {
            val values = snapshot.map { it[column] / 1_000_000.0 }.sorted()
            result.put("${label}MedianMs", values[values.size / 2])
            result.put("${label}P95Ms", values[((values.size - 1) * .95).toInt()])
        }
        File(compose.activity.getExternalFilesDir(null), "store-raster-$run-${if (reuse) "cached" else "baseline"}.json").writeText(result.toString(2))
    }

    private fun nativeSwipe(up: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bounds = compose.onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot
        val startY = if (up) bounds.height * .72f else bounds.height * .3f
        val endY = if (up) bounds.height * .3f else bounds.height * .72f
        val started = SystemClock.uptimeMillis()
        fun send(action: Int, y: Float) {
            val event = MotionEvent.obtain(started, SystemClock.uptimeMillis(), action, bounds.center.x, bounds.top + y, 0)
            instrumentation.sendPointerSync(event)
            event.recycle()
        }
        send(MotionEvent.ACTION_DOWN, startY)
        for (step in 1..22) {
            SystemClock.sleep(16)
            send(MotionEvent.ACTION_MOVE, startY + (endY - startY) * step / 22f)
        }
        send(MotionEvent.ACTION_UP, endY)
        compose.waitForIdle()
    }

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun save(name: String, dialog: Boolean = false) {
        compose.waitForIdle()
        val image = if (dialog) InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            else compose.onNodeWithTag("scene").captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), "$name.png").outputStream().use {
            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

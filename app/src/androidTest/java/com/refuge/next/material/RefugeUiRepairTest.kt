package com.refuge.next.material

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.refuge.next.design.RefugeColors
import com.refuge.next.navigation.LocalRootScrollRegistry
import com.refuge.next.navigation.RootScrollRegistry
import com.refuge.next.navigation.rememberRootListState
import com.refuge.next.reference.OfficialLiquidButtonPort
import com.refuge.next.screens.RootBottomNav
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Isolated production controls; no network or account mutation is involved. */
class RefugeUiRepairTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun detailViewportReachesActionRailAndLastRowRemainsReachable() {
        compose.setContent {
            RefugeScene(RefugeColors.dark) { backdrop ->
                RefugeLiquidSheet(backdrop, RefugeColors.dark, "", {}, sheetHeight = 650.dp,
                    contentUnderHandle = true, surfaceRefraction = false, actionBottomPadding = 4.dp,
                    action = { Box(Modifier.fillMaxWidth().height(48.dp).testTag("test-action")) },
                ) {
                    repeat(24) { index -> Text("正文 $index", modifier = Modifier.fillMaxWidth().height(48.dp)) }
                }
            }
        }
        val viewport = compose.onNodeWithTag("sheet-scroll-viewport").fetchSemanticsNode().boundsInRoot
        val action = compose.onNodeWithTag("test-action").fetchSemanticsNode().boundsInRoot
        val gapDp = with(compose.density) { (action.top - viewport.bottom).toDp().value }
        assertTrue("Content must not stop 220dp before the controls: gap=$gapDp", gapDp in 0f..48f)
        compose.onNodeWithText("正文 23").performScrollTo().assertIsDisplayed()
    }

    @Test fun circularButtonPressDoesNotLightSquareCorners() {
        compose.setContent {
            RefugeScene(RefugeColors.dark) { backdrop ->
                Box(Modifier.padding(60.dp).size(96.dp).background(Color.Black).testTag("button-canvas")) {
                    OfficialLiquidButtonPort({}, backdrop,
                        Modifier.padding(16.dp).size(64.dp).testTag("round-button"),
                        shape = CircleShape, isInteractive = false, enablePressHighlight = true,
                    ) { Text("+") }
                }
            }
        }
        val canvas = compose.onNodeWithTag("button-canvas")
        val before = canvas.captureToImage().toPixelMap()
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithTag("round-button").performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(500)
            val after = canvas.captureToImage().toPixelMap()
            for (xFraction in listOf(.18f, .82f)) for (yFraction in listOf(.18f, .82f)) {
                val x = (before.width * xFraction).toInt()
                val y = (before.height * yFraction).toInt()
                assertEquals("Pressed light leaked beyond circle", before[x,y].red, after[x,y].red, .04f)
            }
            val x = before.width / 2
            val y = before.height * 2 / 5
            assertTrue("The glass still needs a visible press response", after[x,y].red > before[x,y].red + .04f)
            compose.onNodeWithTag("round-button").performTouchInput { up() }
        } finally { compose.mainClock.autoAdvance = true }
    }

    @Test fun reselectingEveryRootTabReturnsItsListToTheTop() {
        var activeList: androidx.compose.foundation.lazy.LazyListState? = null
        compose.setContent {
            val registry = remember { RootScrollRegistry() }
            val scope = rememberCoroutineScope()
            var route by remember { mutableIntStateOf(0) }
            CompositionLocalProvider(LocalRootScrollRegistry provides registry) {
                RefugeScene(RefugeColors.light) { backdrop ->
                    Box(Modifier.fillMaxSize()) {
                        key(route) {
                            val listState = rememberRootListState(route)
                            SideEffect { activeList = listState }
                            LazyColumn(Modifier.fillMaxSize().testTag("root-list"),
                                state = listState, contentPadding = PaddingValues(bottom = 160.dp)) {
                                items(50) { Text("$route:项目 $it", modifier = Modifier.height(60.dp)) }
                            }
                        }
                        RootBottomNav(backdrop, false, route) { next ->
                            if (next == route) scope.launch { registry.scrollToTop(next) } else route = next
                        }
                    }
                }
            }
        }
        for ((route, label) in listOf(0 to "机库", 1 to "商店", 2 to "终端", 4 to "我的")) {
            compose.onNodeWithContentDescription(label).performClick()
            compose.waitUntil(5000) { activeList?.let { !it.isScrollInProgress && it.firstVisibleItemIndex == 0 } == true }
            compose.onNodeWithTag("root-list").performScrollToIndex(30)
            compose.onNodeWithText("$route:项目 0").assertDoesNotExist()
            // Exercise the glass lens above the semantic tab, as a finger does.
            compose.onNodeWithContentDescription(label).performTouchInput { click(center) }
            compose.waitUntil(10000) { activeList?.let { !it.isScrollInProgress && it.firstVisibleItemIndex == 0 } == true }
            compose.onNodeWithText("$route:项目 0").assertIsDisplayed()
        }
    }
}

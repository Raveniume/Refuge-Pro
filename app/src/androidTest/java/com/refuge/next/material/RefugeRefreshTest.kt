package com.refuge.next.material

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RefugeRefreshTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun immediateCachedRefreshSettlesAndCanBeRepeated() {
        var calls = 0
        compose.setContent {
            var refreshing by remember { mutableStateOf(false) }
            val list = rememberLazyListState()
            RefugePullToRefresh(list, refreshing, {
                refreshing = true
                calls++
                refreshing = false // Cached version check can finish in one frame.
            }) {
                LazyColumn(Modifier.fillMaxSize().testTag("refresh-list"), state = list) {
                    items(30) { Text("Row $it", Modifier.fillMaxWidth().height(56.dp)) }
                }
            }
        }
        repeat(2) { index ->
            compose.onNodeWithTag("refresh-list").performTouchInput { swipeDown() }
            compose.waitUntil(5_000) { calls == index + 1 }
            compose.waitForIdle()
            compose.onNodeWithContentDescription("下拉刷新", substring = true).assertDoesNotExist()
            compose.onNodeWithContentDescription("正在刷新").assertDoesNotExist()
        }
        compose.runOnIdle { assertEquals(2, calls) }
    }
}

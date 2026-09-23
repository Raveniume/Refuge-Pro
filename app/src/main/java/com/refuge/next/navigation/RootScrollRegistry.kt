package com.refuge.next.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf

class RootScrollRegistry {
    private val lists = mutableMapOf<Int, LazyListState>()
    fun register(route: Int, state: LazyListState) { lists[route] = state }
    fun unregister(route: Int, state: LazyListState) {
        if (lists[route] === state) lists.remove(route)
    }
    suspend fun scrollToTop(route: Int) { lists[route]?.animateScrollToItem(0) }
}

val LocalRootScrollRegistry = staticCompositionLocalOf<RootScrollRegistry?> { null }

/** The list state remains saveable with its route; reselecting a root tab only scrolls it. */
@Composable
fun rememberRootListState(route: Int): LazyListState {
    val state = rememberLazyListState()
    val registry = LocalRootScrollRegistry.current
    DisposableEffect(route, state, registry) {
        registry?.register(route, state)
        onDispose { registry?.unregister(route, state) }
    }
    return state
}

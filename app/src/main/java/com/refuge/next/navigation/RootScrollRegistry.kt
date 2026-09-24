package com.refuge.next.navigation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf

class RootScrollRegistry {
    private val lists = mutableMapOf<Int, LazyListState>()
    private val refreshers = mutableMapOf<Int, () -> Unit>()
    fun register(route: Int, state: LazyListState) { lists[route] = state }
    fun unregister(route: Int, state: LazyListState) {
        if (lists[route] === state) lists.remove(route)
    }
    fun registerRefresh(route: Int, refresh: () -> Unit) { refreshers[route] = refresh }
    fun unregisterRefresh(route: Int, refresh: () -> Unit) {
        if (refreshers[route] === refresh) refreshers.remove(route)
    }
    fun triggerRefresh(route: Int) { refreshers[route]?.invoke() }
    suspend fun scrollToTop(route: Int) { lists[route]?.animateScrollToItem(0) }
    fun isAtTop(route: Int): Boolean = lists[route]?.let {
        it.firstVisibleItemIndex == 0 && it.firstVisibleItemScrollOffset == 0
    } ?: true
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

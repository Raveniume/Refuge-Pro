package com.refuge.next.material

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Refresh immediately on entry/resume, then poll only while visible. */
@Composable
fun RefreshWhileVisible(key: Any, refresh: suspend () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(refresh)
    LaunchedEffect(owner, key) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { latest(); delay(60_000) }
        }
    }
}

package com.refuge.next.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * Captures the complete page render, including wallpaper and content. Controls
 * that float above a page should use the backdrop supplied by this scope.
 */
@Composable
fun PageGlassScope(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.(Backdrop) -> Unit,
) {
    val contentBackdrop = rememberLayerBackdrop()
    val pageBackdrop = rememberCombinedBackdrop(backdrop, contentBackdrop)
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().layerBackdrop(contentBackdrop)) {
            content()
        }
        overlay(pageBackdrop)
    }
}

/**
 * Creates a new optical root for modal content. The supplied backdrop contains
 * only the modal base and its children, never the page below the scrim.
 */
@Composable
fun ModalGlassScope(
    modifier: Modifier = Modifier,
    base: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.(LayerBackdrop) -> Unit,
) {
    val modalBackdrop = rememberLayerBackdrop()
    Box(modifier) {
        Box(Modifier.matchParentSize().layerBackdrop(modalBackdrop)) {
            base()
        }
        content(modalBackdrop)
    }
}

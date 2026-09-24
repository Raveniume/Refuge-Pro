package com.refuge.next.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

private val LocalPageGlassScopeActive = staticCompositionLocalOf { false }
private val LocalPageContentBackdrop = staticCompositionLocalOf<Backdrop?> { null }

data class RootNavigationOverlay(
    val key: Any,
    val content: @Composable (Backdrop) -> Unit,
)

/** Root navigation is drawn after page content but before in-page modals. */
val LocalRootNavigationOverlay = staticCompositionLocalOf<RootNavigationOverlay?> { null }

// Only overlays outside the captured tree may consume this backdrop.
// In-flow controls keep sampling their explicitly supplied lower layer.
val LocalPageOverlayBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * Enables the shader-backed glass pass after the first cached frame has been
 * committed. The fallback keeps launch responsive while the GPU pipeline warms.
 */
val LocalOpticalGlassEnabled = staticCompositionLocalOf { true }

internal val LocalRasterizePageBackdrop = staticCompositionLocalOf { false }

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
    // Keep one page capture above route navigation so controls and the root
    // tab bar sample the same moving content while routes change.
    if (LocalPageGlassScopeActive.current) {
        Box(modifier.fillMaxSize()) {
            content()
            val pageBackdrop = LocalPageContentBackdrop.current ?: backdrop
            overlay(pageBackdrop)
            LocalRootNavigationOverlay.current?.let { navigation ->
                androidx.compose.runtime.key(navigation.key) { navigation.content(pageBackdrop) }
            }
        }
        return
    }
    // Keep content controls on the scene backdrop so they never sample
    // themselves. The floating root navbar gets a separate page capture so
    // the live list refracts and blurs beneath it like a real glass layer.
    val navigationBackdrop = if (LocalRootNavigationOverlay.current != null) {
        rememberLayerBackdrop()
    } else {
        null
    }
    Box(modifier.fillMaxSize()) {
        if (navigationBackdrop != null) {
            Box(Modifier.fillMaxSize().layerBackdrop(navigationBackdrop)) {
                CompositionLocalProvider(
                    LocalPageGlassScopeActive provides true,
                    LocalPageOverlayBackdrop provides backdrop,
                ) {
                    content()
                }
            }
        } else {
            CompositionLocalProvider(
                LocalPageGlassScopeActive provides true,
                LocalPageOverlayBackdrop provides backdrop,
            ) {
                content()
            }
        }
        overlay(backdrop)
        LocalRootNavigationOverlay.current?.let { navigation ->
            val glassBackdrop = navigationBackdrop ?: backdrop
            androidx.compose.runtime.key(navigation.key) { navigation.content(glassBackdrop) }
        }
    }
}

/**
 * Captures the modal base separately from its content. In-flow controls sample
 * the base; floating actions sample the page, modal base and modal content.
 */
@Composable
fun ModalGlassScope(
    modifier: Modifier = Modifier,
    underlay: Backdrop? = null,
    base: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.(LayerBackdrop) -> Unit,
    overlay: @Composable BoxScope.(Backdrop) -> Unit = {},
) {
    val modalBackdrop = rememberLayerBackdrop()
    val contentBackdrop = rememberLayerBackdrop()
    val combinedBackdrop = if (underlay == null) {
        rememberCombinedBackdrop(modalBackdrop, contentBackdrop)
    } else {
        rememberCombinedBackdrop(underlay, modalBackdrop, contentBackdrop)
    }
    Box(modifier.semantics { dialog() }) {
        Box(Modifier.matchParentSize().layerBackdrop(modalBackdrop)) {
            base()
        }
        Box(Modifier.layerBackdrop(contentBackdrop)) {
            CompositionLocalProvider(LocalPageOverlayBackdrop provides combinedBackdrop) {
                content(modalBackdrop)
            }
        }
        overlay(combinedBackdrop)
    }
}

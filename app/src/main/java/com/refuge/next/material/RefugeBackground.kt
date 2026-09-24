package com.refuge.next.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.refuge.next.design.RefugePalette
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.theme.darkColorScheme
import zone.ien.hig.theme.lightColorScheme
import com.refuge.next.material.LocalOpticalGlassEnabled

@Composable
fun RefugeScene(
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(backdrop: LayerBackdrop) -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        val backdrop = rememberLayerBackdrop()
        val canvasColor = if (palette.background.luminance() < .5f) Color.Black else palette.background
        // Light mode uses the system grouped background so white content groups
        // remain legible. Dark mode keeps the true black canvas requested by the
        // product while elevated surfaces supply the hierarchy.
        Box(
            Modifier
                .matchParentSize()
                .background(canvasColor)
                // Do not allocate the full-screen backdrop RenderNode during
                // cold start. The cached page is already readable without the
                // optical pass; enabling it after the first stable frames
                // keeps Android 15's emulator RenderThread responsive while
                // images and the list compiler are warming.
                .then(if (LocalOpticalGlassEnabled.current) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
        }
        CompositionLocalProvider(
            // Functional glass needs a readable surface even when the page has
            // no artwork behind it. The blur and lens still sample the page;
            // this semantic fill supplies the accessible light-mode boundary.
            LocalGlassControlSurface provides if (palette.background.luminance() < .5f) {
                Color.White.copy(alpha = .12f)
            } else {
                Color.White.copy(alpha = .66f)
            },
        ) {
            CupertinoTheme(colorScheme = if (palette.background.luminance() < .5f)
                darkColorScheme(accent = palette.accent) else lightColorScheme(accent = palette.accent)) {
                content(backdrop)
            }
        }
    }
}

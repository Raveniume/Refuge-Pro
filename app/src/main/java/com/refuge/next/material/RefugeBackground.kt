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

@Composable
fun RefugeScene(
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(backdrop: LayerBackdrop) -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        val backdrop = rememberLayerBackdrop()
        val canvasColor = if (palette.background.luminance() < .5f) Color.Black else Color.White
        // The product canvas is intentionally pure white/black. Liquid Glass
        // controls still sample page content, but the wallpaper must never
        // leak through the main background.
        Box(
            Modifier
                .matchParentSize()
                .background(canvasColor)
                .layerBackdrop(backdrop),
        ) {
        }
        CompositionLocalProvider(
            // On a pure white canvas a white fill has no visible material
            // boundary. A restrained semantic tint keeps the light theme
            // white while making the live refraction, edge and shadow read as
            // Liquid Glass. Dark mode uses the matching white lift.
            LocalGlassControlSurface provides if (palette.background.luminance() < .5f) {
                Color.White.copy(alpha = .12f)
            } else {
                Color.Black.copy(alpha = .045f)
            },
        ) {
            CupertinoTheme(colorScheme = if (palette.background.luminance() < .5f)
                darkColorScheme(accent = palette.accent) else lightColorScheme(accent = palette.accent)) {
                content(backdrop)
            }
        }
    }
}

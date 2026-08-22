package com.refuge.next.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography

/**
 * Shared modal composition: page -> scrim -> opaque-enough modal base -> local
 * backdrop -> controls. Content controls receive the local backdrop so they do
 * not refract the page behind the sheet.
 */
@Composable
fun RefugeLiquidSheet(
    backdrop: LayerBackdrop,
    palette: RefugePalette,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable (LayerBackdrop) -> Unit)? = null,
    content: @Composable ColumnScope.(LayerBackdrop) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(palette.scrim), contentAlignment = Alignment.BottomCenter) {
            val modalBackdrop = rememberLayerBackdrop()
            RefugeStandardGlassSurface(
                backdrop = backdrop,
                palette = palette,
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                radius = RefugeRadius.sheet,
                padding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            palette.contentSurfaceStrong.copy(
                                alpha = if (palette.background.luminance() < .5f) .82f else .72f,
                            ),
                        )
                        .layerBackdrop(modalBackdrop),
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm),
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(34.dp)
                            .height(4.dp)
                            .background(palette.outline.copy(alpha = .55f), RoundedCornerShape(2.dp)),
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material.Text(title, style = RefugeTypography.title(palette), modifier = Modifier.weight(1f))
                        action?.invoke(modalBackdrop)
                    }
                    content(modalBackdrop)
                }
            }
        }
    }
}

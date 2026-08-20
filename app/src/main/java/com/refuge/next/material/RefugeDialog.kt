package com.refuge.next.material

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeTypography

@Composable
fun RefugeDialog(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    palette: RefugePalette,
    title: String,
    body: String,
    primaryLabel: String,
    onDismiss: () -> Unit,
    onPrimary: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(palette.scrim), contentAlignment = Alignment.Center) {
            RefugeModalSurface(
                palette = palette,
                modifier = Modifier
                    .fillMaxWidth(.88f)
                    .padding(20.dp),
                radius = 24.dp,
                fill = palette.contentSurfaceStrong,
                padding = androidx.compose.foundation.layout.PaddingValues(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(title, style = RefugeTypography.title(palette))
                    Text(body, style = RefugeTypography.body(palette))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        RefugeGlassControl(
                            backdrop = backdrop,
                            palette = palette,
                            onClick = onPrimary,
                            contentDescription = primaryLabel,
                            padding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 18.dp,
                                vertical = 10.dp,
                            ),
                        ) {
                            Text(primaryLabel, style = RefugeTypography.body(palette))
                        }
                    }
                }
            }
        }
    }
}

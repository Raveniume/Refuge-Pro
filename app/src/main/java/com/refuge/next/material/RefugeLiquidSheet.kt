package com.refuge.next.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
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
    action: (@Composable (Backdrop) -> Unit)? = null,
    content: @Composable ColumnScope.(LayerBackdrop) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(palette.scrim), contentAlignment = Alignment.BottomCenter) {
            val maxSheetHeight = (maxHeight * .88f).coerceAtLeast(320.dp)
            var entered by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { entered = true }
            val translation by androidx.compose.animation.core.animateDpAsState(
                if (entered) 0.dp else 28.dp,
                label = "sheet-translation",
            )
            val opacity by androidx.compose.animation.core.animateFloatAsState(
                if (entered) 1f else 0f,
                label = "sheet-opacity",
            )
            ModalGlassScope(
                modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(max = maxSheetHeight)
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 12.dp)
                    .graphicsLayer {
                        translationY = translation.toPx()
                        alpha = opacity
                    },
                base = {
                    val isDark = palette.background.luminance() < .5f
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    if (isDark) {
                                        listOf(Color(0xFF182233), Color(0xFF101722))
                                    } else {
                                        listOf(Color(0xFFF5F6F8), Color(0xFFE9ECF1))
                                    },
                                ),
                                RoundedCornerShape(RefugeRadius.sheet),
                            ),
                    )
                },
                content = { modalBackdrop ->
                    val contentScroll = rememberScrollState()
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxSheetHeight - 24.dp)
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
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(contentScroll),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                                content(modalBackdrop)
                            }
                        }
                        if (action != null) {
                            Box(Modifier.fillMaxWidth().height(58.dp))
                        }
                    }
                },
                overlay = { combinedBackdrop ->
                    if (action != null) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                        ) {
                            action.invoke(combinedBackdrop)
                        }
                    }
                },
            )
        }
    }
}

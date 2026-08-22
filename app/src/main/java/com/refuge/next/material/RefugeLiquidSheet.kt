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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import kotlinx.coroutines.launch

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
    sheetHeight: androidx.compose.ui.unit.Dp? = null,
    actionBottomPadding: androidx.compose.ui.unit.Dp = 14.dp,
    actionOverContent: Boolean = false,
    action: (@Composable (Backdrop) -> Unit)? = null,
    content: @Composable ColumnScope.(LayerBackdrop) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            val maxSheetHeight = maxHeight.coerceAtLeast(240.dp)
            val density = androidx.compose.ui.platform.LocalDensity.current
            val scope = rememberCoroutineScope()
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val sheetOffset = remember { androidx.compose.animation.core.Animatable(maxHeightPx) }
            val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
            val dismissWithAnimation: () -> Unit = {
                scope.launch {
                    sheetOffset.animateTo(maxHeightPx, androidx.compose.animation.core.tween(220))
                    onDismiss()
                }
            }
            LaunchedEffect(maxHeightPx) {
                sheetOffset.snapTo(maxHeightPx)
                sheetOffset.animateTo(0f, androidx.compose.animation.core.spring(1f, 380f, .7f))
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(palette.scrim)
                    .clickable(interactionSource = null, indication = null, onClick = dismissWithAnimation),
            )
            ModalGlassScope(
                modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(max = maxSheetHeight)
                    .then(if (sheetHeight != null) Modifier.height(sheetHeight) else Modifier)
                    .graphicsLayer {
                        translationY = sheetOffset.value
                    },
                base = {
                    val isDark = palette.background.luminance() < .5f
                    Box(
                        Modifier
                            .matchParentSize()
                            .clip(shape)
                            .background(
                                Brush.verticalGradient(
                                    if (isDark) {
                                        listOf(Color(0xFF182233), Color(0xFF101722))
                                    } else {
                                        listOf(Color(0xFFF5F6F8), Color(0xFFE9ECF1))
                                    },
                                ),
                                shape,
                            ),
                    )
                },
                content = { modalBackdrop ->
                    val contentScroll = rememberScrollState()
                    val actionReservation = if (action != null && !actionOverContent) 152.dp else 104.dp
                    val viewportMax = (maxSheetHeight - actionReservation).coerceAtLeast(120.dp)
                    val contentMaxHeight = sheetHeight
                        ?.let {
                            (it - if (action != null && !actionOverContent) 180.dp else 120.dp)
                                .coerceAtLeast(120.dp)
                        }
                        ?: viewportMax
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm),
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(34.dp)
                                .height(44.dp)
                                .pointerInput(maxHeightPx) {
                                    val velocityTracker = VelocityTracker()
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consumePositionChange()
                                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                                            scope.launch {
                                                sheetOffset.snapTo((sheetOffset.value + dragAmount).coerceIn(0f, maxHeightPx))
                                            }
                                        },
                                        onDragStart = { offset ->
                                            velocityTracker.resetTracking()
                                            velocityTracker.addPosition(android.os.SystemClock.uptimeMillis(), offset)
                                        },
                                        onDragEnd = {
                                            val velocity = velocityTracker.calculateVelocity().y
                                            scope.launch {
                                                if (sheetOffset.value > maxHeightPx * .24f || velocity > 1100f) {
                                                    sheetOffset.animateTo(maxHeightPx, androidx.compose.animation.core.tween(200))
                                                    onDismiss()
                                                } else {
                                                    sheetOffset.animateTo(0f, androidx.compose.animation.core.spring(1f, 380f, .7f))
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            scope.launch { sheetOffset.animateTo(0f, androidx.compose.animation.core.spring(1f, 380f, .7f)) }
                                        },
                                    )
                                },
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Box(
                                Modifier
                                    .width(34.dp)
                                    .height(4.dp)
                                    .background(palette.outline.copy(alpha = .55f), RoundedCornerShape(2.dp)),
                            )
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material.Text(title, style = RefugeTypography.title(palette), modifier = Modifier.weight(1f))
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = contentMaxHeight)
                                .verticalScroll(contentScroll),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                                content(modalBackdrop)
                            }
                        }
                        if (action != null && !actionOverContent) {
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
                                .padding(
                                    start = 18.dp,
                                    end = 18.dp,
                                    top = if (actionOverContent) 0.dp else 14.dp,
                                    bottom = actionBottomPadding,
                                ),
                        ) {
                            action.invoke(combinedBackdrop)
                        }
                    }
                },
            )
        }
    }
}

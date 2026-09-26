package com.refuge.next.material

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.tween
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeSpacing
import com.refuge.next.design.RefugeTypography
import com.refuge.next.design.refugeContinuousShape
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    respectTopSafeArea: Boolean = false,
    actionBottomPadding: androidx.compose.ui.unit.Dp = 14.dp,
    actionOverContent: Boolean = false,
    contentUnderHandle: Boolean = false,
    transparentActionArea: Boolean = false,
    surfaceRefraction: Boolean = true,
    surfaceAlpha: Float = 1f,
    contentScrollable: Boolean = true,
    /** Increment to run the Cupertino dismissal animation from content. */
    dismissSignal: Int = 0,
    leadingAction: (@Composable (Backdrop) -> Unit)? = null,
    action: (@Composable (Backdrop) -> Unit)? = null,
    content: @Composable ColumnScope.(LayerBackdrop) -> Unit,
) {
    val pageBackdrop = LocalPageOverlayBackdrop.current ?: backdrop
    // Compose-hig style in-place overlay: keeping the sheet in the activity's
    // composition preserves the live page underneath during the entire slide
    // animation. A platform Dialog creates a second window and briefly turns
    // the page into a uniform dim rectangle while its content is entering.
    // Keep the root tab bar mounted at its original coordinates underneath the
    // sheet. The sheet itself must win the sibling z-order so its opaque body
    // and scrim cover the bar instead of letting the bar paint over the picker.
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .zIndex(1000f),
        contentAlignment = Alignment.BottomCenter,
    ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val statusBarInset = with(density) {
                WindowInsets.statusBars.getTop(this).toDp()
            }
            val navigationBarInset = with(density) {
                WindowInsets.navigationBars.getBottom(this).toDp()
            }
            val maxSheetHeight = (
                maxHeight - if (respectTopSafeArea) statusBarInset else 0.dp
                ).coerceAtLeast(240.dp)
            val targetSheetHeight = (sheetHeight ?: maxSheetHeight).coerceAtMost(maxSheetHeight)
            val scope = rememberCoroutineScope()
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val sheetOffset = remember { androidx.compose.animation.core.Animatable(maxHeightPx) }
            val shape = refugeContinuousShape(
                topStart = 28.dp,
                topEnd = 28.dp,
                bottomEnd = 0.dp,
                bottomStart = 0.dp,
            )
            var actionRailHeight by remember { mutableStateOf(76.dp + navigationBarInset) }
            // compose-hig's CupertinoActionSheet animates the scrim together
            // with the sheet: cupertinoTween on entry and a short ease-in on
            // exit. Keeping this in the activity window preserves the page
            // underneath while the glass surface moves over it.
            val scrimProgress = remember { androidx.compose.animation.core.Animatable(0f) }
            val dismissWithAnimation: () -> Unit = {
                scope.launch {
                    kotlinx.coroutines.coroutineScope {
                        val sheetJob = launch { sheetOffset.animateTo(maxHeightPx, tween(150, easing = EaseIn)) }
                        val scrimJob = launch { scrimProgress.animateTo(0f, tween(150, easing = EaseIn)) }
                        sheetJob.join()
                        scrimJob.join()
                    }
                    onDismiss()
                }
            }
            LaunchedEffect(dismissSignal) {
                if (dismissSignal > 0) dismissWithAnimation()
            }
            // System Back follows the same Cupertino dismissal path as the
            // scrim and drag handle, so the page remains visible throughout
            // the exit motion instead of disappearing in one frame.
            BackHandler(onBack = dismissWithAnimation)
            LaunchedEffect(maxHeightPx) {
                sheetOffset.snapTo(maxHeightPx)
                scrimProgress.snapTo(0f)
                kotlinx.coroutines.coroutineScope {
                    val sheetJob = launch { sheetOffset.animateTo(0f, zone.ien.hig.cupertinoTween()) }
                    val scrimJob = launch { scrimProgress.animateTo(1f, zone.ien.hig.cupertinoTween()) }
                    sheetJob.join()
                    scrimJob.join()
                }
            }
            val dragHandle: @Composable () -> Unit = {
                Box(
                    Modifier
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
                                            dismissWithAnimation()
                                        } else {
                                            sheetOffset.animateTo(0f, zone.ien.hig.cupertinoTween())
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { sheetOffset.animateTo(0f, zone.ien.hig.cupertinoTween()) }
                                },
                            )
                        },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        Modifier
                            .width(34.dp)
                            .height(4.dp)
                            .background(
                                palette.outline.copy(alpha = .55f),
                                Capsule(RoundedCornerStyle.Continuous),
                            ),
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(palette.scrim.copy(alpha = palette.scrim.alpha * scrimProgress.value))
                    .clickable(interactionSource = null, indication = null, onClick = dismissWithAnimation),
            )
            ModalGlassScope(
                modifier
                    .fillMaxWidth()
                    .height(targetSheetHeight)
                    .offset { IntOffset(0, sheetOffset.value.roundToInt()) },
                underlay = pageBackdrop,
                base = {
                    val isDark = palette.background.luminance() < .5f
                    Box(
                        Modifier
                            .matchParentSize()
                            .then(
                                if (surfaceRefraction) {
                                    Modifier.drawBackdrop(
                                        backdrop = pageBackdrop,
                                        shape = { shape },
                                        effects = {
                                            vibrancy()
                                            blur(12.dp.toPx())
                                            lens(18.dp.toPx(), 24.dp.toPx(), depthEffect = false)
                                        },
                                        highlight = { RefugeGlassStyle.barHighlight },
                                        shadow = { RefugeGlassStyle.barShadow },
                                        onDrawSurface = {
                                            if (transparentActionArea) {
                                                val fadeStart = (size.height - (112.dp + navigationBarInset).toPx())
                                                    .coerceAtLeast(0f)
                                                drawRect(
                                                    Brush.verticalGradient(
                                                        colors = listOf(palette.contentSurfaceStrong.copy(alpha = .96f), Color.Transparent),
                                                        startY = fadeStart,
                                                        endY = size.height,
                                                    ),
                                                )
                                            } else {
                                                // Keep the sampled page visible around the sheet during the
                                                // transition, while making the sheet body opaque enough that
                                                // the page's text does not ghost through its controls.
                                                // Keep enough live page color for the HIG in-place
                                                // presentation while the content rows themselves use
                                                // opaque surfaces for text legibility.
                                                drawRect(palette.contentSurfaceStrong.copy(alpha = surfaceAlpha.coerceIn(0f, 1f)))
                                            }
                                        },
                                    )
                                } else {
                                    Modifier
                                        .shadow(24.dp, shape, clip = false)
                                        .clip(shape)
                                        .background(palette.contentSurface)
                                },
                            )
                            .clip(shape)
                            // The sheet surface already has enough separation
                            // from the scrim; an outline becomes a dark rim.
                    )
                },
                content = { modalBackdrop ->
                    val contentScroll = rememberScrollState()
                    // The action rail is drawn in the sheet overlay. Keep a real
                    // viewport gap so the final content row can never sit below
                    // the rail or leave a dark rectangle behind it.
                    // Reserve only the measured controls and header. Fixed 220dp
                    // reservations clipped the body halfway down a detail sheet.
                    val actionReservation = if (action != null && !actionOverContent) actionRailHeight else 0.dp
                    val headerReservation = 24.dp +
                        (if (contentUnderHandle) 0.dp else 52.dp) +
                        (if (title.isBlank()) 0.dp else 36.dp) +
                        (if (action == null) navigationBarInset else 0.dp)
                    val contentMaxHeight = (targetSheetHeight -
                        actionReservation - headerReservation).coerceAtLeast(120.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 18.dp,
                                end = 18.dp,
                                top = 12.dp,
                                bottom = 12.dp + if (action == null) navigationBarInset else 0.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm),
                    ) {
                        if (!contentUnderHandle) {
                            Box(Modifier.align(Alignment.CenterHorizontally)) {
                                dragHandle()
                            }
                        }
                        if (title.isNotBlank()) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material.Text(title, style = RefugeTypography.title(palette), modifier = Modifier.weight(1f))
                            }
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                // Give nested LazyColumn content a real viewport. A
                                // wrap-content box measures loadout and selector rows
                                // as one child, removing fling velocity on a swipe.
                                .weight(1f, fill = true)
                                .heightIn(max = contentMaxHeight)
                                .testTag("sheet-scroll-viewport")
                                .then(if (contentScrollable) Modifier.verticalScroll(contentScroll) else Modifier),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(RefugeSpacing.sm)) {
                                content(modalBackdrop)
                            }
                        }
                        if (action != null && !actionOverContent) {
                            Box(Modifier.fillMaxWidth().height(actionReservation))
                        }
                    }
                },
                overlay = { combinedBackdrop ->
                    leadingAction?.let { action ->
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .zIndex(2f)
                                // The 28 dp continuous sheet corner and the
                                // circular 44 dp control share the same
                                // tangent point (6 dp optical inset).
                                .padding(start = 6.dp, top = 6.dp),
                        ) {
                            action.invoke(combinedBackdrop)
                        }
                    }
                    if (contentUnderHandle) {
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp),
                        ) {
                            dragHandle()
                        }
                    }
                    if (action != null) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .onSizeChanged { actionRailHeight = with(density) { it.height.toDp() } }
                                .padding(
                                    start = 18.dp,
                                    end = 18.dp,
                                    top = if (actionOverContent) 0.dp else 14.dp,
                                    // The action glass renders beyond the visible glyph bounds.
                                    // Reserve the real gesture/navigation inset for that optical
                                    // overscan instead of translating the entire sheet viewport.
                                    bottom = actionBottomPadding + navigationBarInset,
                                ),
                        ) {
                            action.invoke(combinedBackdrop)
                        }
                    }
                },
            )
    }
}

package com.refuge.next.material

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.VisualTransformation
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.refuge.next.design.RefugePalette
import com.refuge.next.design.RefugeRadius
import com.refuge.next.design.RefugeTypography
import com.refuge.next.design.refugeContinuousShape

/** Shared optical material used by every functional glass component and the optical test. */
@Composable
fun RefugeLiquidGlass(
    backdrop: Backdrop,
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    radius: Dp = RefugeRadius.control,
    padding: PaddingValues = PaddingValues(0.dp),
    refractionHeight: Dp = 14.dp,
    refractionAmount: Dp = 20.dp,
    blurRadius: Dp = 3.dp,
    surface: androidx.compose.ui.graphics.Color = palette.glass,
    surfaceAlpha: Float = .035f,
    interactionProgress: Float = 0f,
    chromaticAberration: Boolean = false,
    edgeAlpha: Float = 0f,
    edgeColor: androidx.compose.ui.graphics.Color = palette.text,
    highlightAlpha: Float = .26f,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = refugeContinuousShape(radius)
    val progress = interactionProgress.coerceIn(0f, 1f)
    if (!LocalOpticalGlassEnabled.current) {
        // Keep the cached first frame usable while the GPU pipeline warms.
        // This branch intentionally avoids drawBackdrop/lens allocation.
        Box(
            modifier
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = .18f + progress * .08f),
                            surface.copy(alpha = (surfaceAlpha + .18f).coerceAtMost(.55f)),
                            surface.copy(alpha = (surfaceAlpha + .12f).coerceAtMost(.48f)),
                        ),
                    ),
                )
                .padding(padding),
            contentAlignment = Alignment.Center,
            content = content,
        )
        return
    }
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(blurRadius.toPx())
                    lens(
                        refractionHeight.toPx() * (1f + progress * .25f),
                        refractionAmount.toPx() * (1f + progress * .30f),
                        depthEffect = true,
                        chromaticAberration = chromaticAberration && progress > .01f,
                    )
                },
                highlight = {
                    RefugeGlassStyle.controlHighlight.copy(alpha = highlightAlpha + progress * .12f)
                },
                shadow = {
                    RefugeGlassStyle.controlShadow
                },
                // Avoid a dark inner rim around every glass control. The
                // material is separated by blur, refraction and highlight.
                innerShadow = null,
                onDrawSurface = {
                    drawRect(surface.copy(alpha = surfaceAlpha + progress * .018f))
                },
            )
            .clip(shape)
            .padding(padding),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun RefugeLiquidGlassButton(
    backdrop: Backdrop,
    palette: RefugePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    radius: Dp = RefugeRadius.control,
    padding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    edgeAlpha: Float = .16f,
    highlightAlpha: Float = .20f,
    enabled: Boolean = true,
    surface: androidx.compose.ui.graphics.Color = palette.glassStrong,
    // Keep light-mode actions visibly separated from the grouped canvas so
    // labels remain readable even when the backdrop is plain white.
    surfaceAlpha: Float = if (palette.background.luminance() < .5f) .065f else .16f,
    refractionHeight: Dp = 12.dp,
    refractionAmount: Dp = 18.dp,
    blurRadius: Dp = 3.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(if (pressed) 1f else 0f, label = "liquid-glass-press")
    RefugeLiquidGlass(
        backdrop = backdrop,
        palette = palette,
        modifier = modifier
            // Keep every text action in the app at the same comfortable HIG
            // control height. Callers may still request a larger card.
            .heightIn(min = 48.dp)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            // Put semantics after clickable so accessibility services and
            // UIAutomator observe one actionable button node instead of a
            // non-clickable semantics wrapper around a clickable child.
            .semantics {
                role = Role.Button
                if (contentDescription != null) this.contentDescription = contentDescription
            },
        radius = radius,
        padding = padding,
        refractionHeight = refractionHeight,
        refractionAmount = refractionAmount,
        blurRadius = blurRadius,
        // Keep controls visibly glassy on both wallpaper and image-backed
        // pages. The previous near-zero fill made upgrade selectors and
        // modal actions read as flat transparent text.
        surface = surface,
        surfaceAlpha = surfaceAlpha,
        // Light glass should read as a soft material edge, not a black
        // one-pixel frame. Keep the stronger edge only for dark surfaces.
        edgeAlpha = 0f,
        highlightAlpha = highlightAlpha,
        interactionProgress = progress,
        content = content,
    )
}

/**
 * Reference-Lab field: the editable layer is deliberately BasicTextField so
 * no Material OutlinedTextField frame leaks into the Liquid Glass surface.
 */
@Composable
fun RefugeLiquidGlassField(
    value: String,
    onValueChange: (String) -> Unit,
    backdrop: Backdrop,
    palette: RefugePalette,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    leadingIconContentDescription: String? = null,
    onLeadingIconClick: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(if (focused) 1f else 0f, label = "liquid-glass-field-focus")
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        textStyle = RefugeTypography.body(palette).copy(color = palette.text),
        // Focus is communicated through a slightly stronger neutral glass
        // surface. A blue frame here reads like a web form selection state
        // and overwhelms the translucent control.
        cursorBrush = SolidColor(palette.text),
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Column(Modifier.fillMaxWidth()) {
                // Keep the field name outside the editable surface. This gives
                // the text a predictable start position and avoids a floating
                // label competing with the cursor inside a narrow glass field.
                androidx.compose.material.Text(
                    text = label,
                    style = RefugeTypography.caption(palette).copy(
                        color = palette.textSecondary,
                    ),
                )
                Spacer(Modifier.height(5.dp))
                RefugeLiquidGlass(
                    backdrop = backdrop,
                    palette = palette,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(
                            width = .5.dp,
                            color = palette.outline.copy(alpha = if (focused) .20f else .12f),
                            shape = refugeContinuousShape(18.dp),
                        ),
                    radius = 18.dp,
                    padding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    refractionHeight = 11.dp,
                    refractionAmount = 16.dp,
                    blurRadius = 3.dp,
                    surface = palette.glassStrong,
                    surfaceAlpha = if (focused) .12f else .085f,
                    edgeAlpha = 0f,
                    highlightAlpha = .08f,
                    interactionProgress = progress,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (leadingIcon != null && onLeadingIconClick != null) {
                            IconButton(
                                onClick = onLeadingIconClick,
                                modifier = Modifier
                                    .size(40.dp)
                                    .semantics {
                                        role = Role.Button
                                        if (leadingIconContentDescription != null) {
                                            contentDescription = leadingIconContentDescription
                                        }
                                    },
                            ) {
                                Icon(
                                    imageVector = leadingIcon,
                                    contentDescription = leadingIconContentDescription,
                                    tint = palette.textSecondary,
                                )
                            }
                        }
                        Box(
                            Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                        if (value.isEmpty() && placeholder != null) {
                            androidx.compose.material.Text(
                                placeholder,
                                style = RefugeTypography.body(palette).copy(color = palette.textMuted),
                            )
                        }
                        innerTextField()
                        }
                    }
                }
            }
        },
    )
}

/**
 * Compatibility entry for ordinary content rows, with no backdrop sampling.
 */
@Composable
fun RefugeQuietLiquidGlassSurface(
    backdrop: Backdrop,
    palette: RefugePalette,
    modifier: Modifier = Modifier,
    radius: Dp = RefugeRadius.panel,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = refugeContinuousShape(radius)
    val base = modifier
        .refugeContentMaterial(palette, shape)
        .then(
            if (onClick != null) Modifier
                .semantics {
                    role = Role.Button
                    if (contentDescription != null) this.contentDescription = contentDescription
                }
                .clickable(interactionSource = null, indication = null, onClick = onClick)
            else Modifier,
        )
        .padding(padding)
    Box(base, contentAlignment = Alignment.Center, content = content)
}

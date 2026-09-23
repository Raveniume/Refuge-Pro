package com.refuge.next.reference

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asComposeShader
import com.kyant.backdrop.isRuntimeShaderSupported
import com.refuge.next.motion.inspectDragGestures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Directly derived from the official catalog's pointer-tracked highlight. */
class ReferenceInteractiveHighlight(
    private val animationScope: CoroutineScope,
    private val position: (Size, Offset) -> Offset = { _, offset -> offset },
) {
    private val pressSpec = spring<Float>(0.5f, 300f, 0.001f)
    private val positionThreshold = Offset(0.01f, 0.01f)
    private val positionSpec = spring(0.5f, 300f, positionThreshold)
    private val press = Animatable(0f, 0.001f)
    private val pointer = Animatable(Offset.Zero, Offset.VectorConverter, positionThreshold)
    private var start = Offset.Zero

    val progress: Float get() = press.value
    val offset: Offset get() = pointer.value - start

    private val shader by lazy { if (isRuntimeShaderSupported()) {
        RuntimeShader(
            """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}
"""
        )
    } else null }

    val modifier: Modifier = Modifier.drawWithContent {
        val value = press.value
        if (value > 0f) {
            val shaderValue = shader
            if (shaderValue != null) {
                drawRect(Color.White.copy(alpha = 0.08f * value), blendMode = BlendMode.Plus)
                shaderValue.apply {
                    val point = position(size, pointer.value)
                    setFloatUniform("size", size.width, size.height)
                    setColorUniform("color", Color.White.copy(alpha = 0.15f * value))
                    setFloatUniform("radius", size.minDimension * 1.5f)
                    setFloatUniform(
                        "position",
                        point.x.fastCoerceIn(0f, size.width),
                        point.y.fastCoerceIn(0f, size.height),
                    )
                }
                drawRect(ShaderBrush(shaderValue.asComposeShader()), blendMode = BlendMode.Plus)
            } else {
                drawRect(Color.White.copy(alpha = 0.25f * value), blendMode = BlendMode.Plus)
            }
        }
        drawContent()
    }

    val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
        inspectDragGestures(
            onDragStart = { down ->
                start = down.position
                animationScope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                    pointer.snapTo(start)
                    press.animateTo(1f, pressSpec)
                }
            },
            onDragEnd = {
                animationScope.launch {
                    launch { press.animateTo(0f, pressSpec) }
                    launch { pointer.animateTo(start, positionSpec) }
                }
            },
            onDragCancel = {
                animationScope.launch {
                    launch { press.animateTo(0f, pressSpec) }
                    launch { pointer.animateTo(start, positionSpec) }
                }
            },
        ) { change, _ ->
            animationScope.launch { pointer.snapTo(change.position) }
        }
    }
}

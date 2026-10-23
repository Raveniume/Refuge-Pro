package com.refuge.next.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.refuge.next.design.RefugePalette

private val missionField = Regex("\\[[A-Za-z][A-Za-z0-9_]*\\]")

/** Game-assigned fields remain literal, readable and announced in context. */
@Composable
internal fun MissionRichText(text: String, palette: RefugePalette, style: TextStyle, modifier: Modifier = Modifier) {
    val fields = remember(text) { missionField.findAll(text).toList() }
    if (fields.isEmpty()) {
        Text(text, style = style, modifier = modifier)
        return
    }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val fieldStyle = style.copy(fontSize = style.fontSize * .86f, fontWeight = FontWeight.Medium, color = palette.accent)
    val inline = fields.map { it.value }.distinct().associateWith { field ->
        val measured = measurer.measure(field, fieldStyle, softWrap = false)
        InlineTextContent(Placeholder(
            width = with(density) { (measured.size.width + 10.dp.toPx()).toSp() },
            height = style.fontSize * 1.35f,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
        )) {
            Box(Modifier.fillMaxSize()
                .background(palette.accent.copy(alpha = .10f), RoundedCornerShape(5.dp))
                .clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
                Text(field, style = fieldStyle, maxLines = 1)
            }
        }
    }
    val annotated = remember(text) {
        buildAnnotatedString {
            var cursor = 0
            fields.forEach { match ->
                append(text.substring(cursor, match.range.first))
                appendInlineContent(match.value, match.value)
                cursor = match.range.last + 1
            }
            append(text.substring(cursor))
        }
    }
    Text(annotated, style = style, inlineContent = inline, modifier = modifier)
}

package com.refuge.next.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.UnevenRoundedRectangle
import com.refuge.next.R

@Immutable
data class RefugePalette(
    val background: Color,
    val backgroundEdge: Color,
    val backgroundLight: Color,
    val contentSurface: Color,
    val contentSurfaceStrong: Color,
    val glass: Color,
    val glassStrong: Color,
    val glassSelection: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSoft: Color,
    val positive: Color,
    val warning: Color,
    val error: Color,
    val divider: Color,
    val outline: Color,
    val scrim: Color,
)

object RefugeColors {
    val dark = RefugePalette(
        background = Color.Black,
        backgroundEdge = Color(0xFF101012),
        backgroundLight = Color(0xFF232326),
        contentSurface = Color(0xFF1C1C1E),
        contentSurfaceStrong = Color(0xFF2C2C2E),
        glass = Color.White.copy(alpha = .055f),
        glassStrong = Color.White.copy(alpha = .10f),
        glassSelection = Color.White.copy(alpha = .12f),
        text = Color(0xFFF5F5F7),
        textSecondary = Color(0xFFB8B8BE),
        textMuted = Color(0xFF8E8E93),
        accent = Color(0xFF64B5FF),
        accentSoft = Color(0xFF0A84FF).copy(alpha = .12f),
        positive = Color(0xFF72D6AA),
        warning = Color(0xFFFFD60A),
        error = Color(0xFFFF6961),
        divider = Color.White.copy(alpha = .12f),
        outline = Color.White.copy(alpha = .16f),
        scrim = Color.Black.copy(alpha = .56f),
    )

    val light = RefugePalette(
        // Match the iOS grouped-background relationship: the page is a
        // visibly tinted system group and content surfaces remain white.
        background = Color.White,
        backgroundEdge = Color(0xFFE5E5EA),
        backgroundLight = Color(0xFFFFFFFF),
        contentSurface = Color(0xFFF7F7F9),
        contentSurfaceStrong = Color(0xFFEFEFF4),
        glass = Color.White.copy(alpha = .54f),
        glassStrong = Color.White.copy(alpha = .70f),
        glassSelection = Color.Black.copy(alpha = .075f),
        text = Color(0xFF1C1C1E),
        textSecondary = Color(0xFF3C3C43),
        textMuted = Color(0xFF636366),
        accent = Color(0xFF007AFF),
        accentSoft = Color(0xFF007AFF).copy(alpha = .10f),
        positive = Color(0xFF087B51),
        warning = Color(0xFF9A6700),
        error = Color(0xFFFF3B30),
        divider = Color(0xFF3C3C43).copy(alpha = .24f),
        outline = Color(0xFF3C3C43).copy(alpha = .26f),
        scrim = Color.Black.copy(alpha = .34f),
    )
}

object RefugeSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val section = 32.dp
    val page = 24.dp
    val rootNavigation = 208.dp
}

object RefugeRadius {
    // Concentric geometry: controls sit inside grouped panels with a stable
    // inset instead of each layer choosing an unrelated pill radius.
    val image = 12.dp
    val control = 12.dp
    val panel = 16.dp
    val hero = 20.dp
    val floating = 24.dp
    val sheet = 28.dp
    val alert = 22.dp

    fun inner(outer: androidx.compose.ui.unit.Dp, inset: androidx.compose.ui.unit.Dp) =
        (outer - inset).coerceAtLeast(0.dp)
}

/**
 * Shared continuous-curvature shapes for every rounded functional/content
 * surface. AndroidLiquidGlass' shape implementation keeps the tangent
 * continuous through the straight-to-corner transition (rather than using a
 * circular arc), so nested surfaces retain the same optical silhouette.
 */
fun refugeContinuousShape(radius: androidx.compose.ui.unit.Dp): Shape =
    RoundedRectangle(radius, RoundedCornerStyle.Continuous)

fun refugeContinuousShape(
    topStart: androidx.compose.ui.unit.Dp,
    topEnd: androidx.compose.ui.unit.Dp,
    bottomEnd: androidx.compose.ui.unit.Dp,
    bottomStart: androidx.compose.ui.unit.Dp,
): Shape = UnevenRoundedRectangle(
    topStart,
    topEnd,
    bottomEnd,
    bottomStart,
    RoundedCornerStyle.Continuous,
)

object RefugeIconSize {
    val small = 18.dp
    val medium = 22.dp
    val large = 26.dp
}

object RefugeTypography {
    // The product uses only PingFang's two strongest supplied cuts. Body and
    // labels use the second-heaviest face; hierarchy uses the heavy face.
    private val pingFang = FontFamily(Font(R.font.pingfang_bold, FontWeight.SemiBold))
    private val pingFangHeavy = FontFamily(Font(R.font.pingfang_heavy, FontWeight.Bold))

    fun largeTitle(palette: RefugePalette) = TextStyle(
        color = palette.text,
        fontFamily = pingFangHeavy,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
    )

    fun title(palette: RefugePalette) = TextStyle(
        color = palette.text,
        fontFamily = pingFang,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold,
    )

    fun headline(palette: RefugePalette) = TextStyle(
        color = palette.text,
        fontFamily = pingFang,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    )

    fun body(palette: RefugePalette) = TextStyle(
        color = palette.textSecondary,
        fontFamily = pingFang,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    )

    fun secondary(palette: RefugePalette) = TextStyle(
        color = palette.textMuted,
        fontFamily = pingFang,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )

    fun caption(palette: RefugePalette) = TextStyle(
        color = palette.textMuted,
        fontFamily = pingFang,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold,
    )

    fun value(palette: RefugePalette) = TextStyle(
        color = palette.text,
        fontFamily = pingFangHeavy,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Bold,
    )

    fun detailTitle(palette: RefugePalette) = TextStyle(
        color = palette.text,
        fontFamily = pingFangHeavy,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.Bold,
    )

    fun detailSubtitle(palette: RefugePalette) = TextStyle(
        color = palette.textMuted,
        fontFamily = pingFang,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.SemiBold,
    )

    fun detailValue(palette: RefugePalette) = TextStyle(
        color = palette.text,
        fontFamily = pingFangHeavy,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.Bold,
    )

    fun detailBody(palette: RefugePalette) = TextStyle(
        color = palette.text,
        fontFamily = pingFang,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    )

    fun detailCaption(palette: RefugePalette) = TextStyle(
        color = palette.textMuted,
        fontFamily = pingFang,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )

    fun detailSection(palette: RefugePalette) = TextStyle(
        color = palette.text,
        fontFamily = pingFang,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

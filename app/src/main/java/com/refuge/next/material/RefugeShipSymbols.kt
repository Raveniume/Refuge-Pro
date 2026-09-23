package com.refuge.next.material

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp

/** Original 24-unit line glyphs, using one stroke weight throughout the outfitter. */
internal fun shipSymbol(name: String, data: String): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
    addPath(PathParser().parsePathString(data).toNodes(), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
}.build()

internal object ShipSymbols {
    val loadout = shipSymbol("ShipLoadout", "M12 3 L16 10 L21 15 L16 15 L15 20 L12 17 L9 20 L8 15 L3 15 L8 10 Z M12 7 L12 12 M18 3 L18 7 M16 5 L20 5")
    val weapons = shipSymbol("Weapons", "M4 19 L9 14 M3 14 L13 4 L16 7 L6 17 Z M9 20 L19 10 L21 12 L11 22 M15 3 L18 6")
    val engine = shipSymbol("Engine", "M4 5 L10 12 L4 19 M10 5 L16 12 L10 19 M16 5 L22 12 L16 19")
    val shield = shipSymbol("Shield", "M12 3 L20 6 L19 14 Q18 19 12 22 Q6 19 5 14 L4 6 Z M12 7 L12 17")
    val quantum = shipSymbol("Quantum", "M12 3 L21 12 L12 21 L3 12 Z M12 7 L17 12 L12 17 L7 12 Z")
    val radar = shipSymbol("Radar", "M4 20 L4 20 M4 14 Q10 14 10 20 M4 8 Q16 8 16 20 M4 2 Q22 2 22 20")
    val life = shipSymbol("LifeSupport", "M12 20 L3 11 Q0 4 7 4 Q10 4 12 7 Q14 4 17 4 Q24 4 21 11 Z M3 12 L8 12 L10 9 L13 15 L15 12 L21 12")
    val cooler = shipSymbol("Cooling", "M12 9 A3 3 0 1 0 12 15 A3 3 0 1 0 12 9 M12 9 Q6 1 4 7 Q2 11 9 12 M15 12 Q23 6 17 4 Q13 2 12 9 M12 15 Q18 23 20 17 Q22 13 15 12 M9 12 Q1 18 7 20 Q11 22 12 15")
    val power = shipSymbol("Power", "M14 2 L5 14 L11 14 L10 22 L20 9 L13 9 Z")
}

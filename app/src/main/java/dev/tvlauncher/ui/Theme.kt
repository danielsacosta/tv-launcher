package dev.tvlauncher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Colores del diseño (Especificación · glass). */
object Palette {
    val Ground = Color(0xFF0B0D10)
    val Glass = Color(0x8C15181D)          // #15181D al 55 %
    val GlassStrong = Color(0xF214171C)    // panel rápido (casi opaco: Android no desenfoca lo de atrás)
    val GlassEdge = Color(0x14FFFFFF)      // borde interior blanco al 8 %
    val Deep = Color(0xFF2A3550)
    val Text = Color(0xFFF2F4F7)
    val TextSoft = Color(0xFFE2E6EB)
    val Muted = Color(0xFFA9B1BB)
    val Icon = Color(0xFFC9D0D8)
    val Active = Color(0xEBF2F4F7)         // blanco al 92 %
    val Chip = Color(0x1AFFFFFF)
    val Accents = listOf(0xFF7AA2FF, 0xFFF2F4F7, 0xFF9BE3C4, 0xFFF5B971)
}

/** Íconos de línea (24×24, trazo 1.8) — los mismos del diseño. */
object Icons {
    private fun stroke(vararg paths: String, width: Float = 1.8f, fill: Boolean = false): ImageVector {
        val b = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        for (p in paths) {
            val nodes = PathParser().parsePathString(p).toNodes()
            if (fill) b.addPath(nodes, fill = SolidColor(Color.White))
            else b.addPath(
                nodes,
                stroke = SolidColor(Color.White),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r},$y h${w - 2 * r} a$r,$r 0 0 1 $r,$r v${h - 2 * r} a$r,$r 0 0 1 -$r,$r h${-(w - 2 * r)} a$r,$r 0 0 1 -$r,-$r v${-(h - 2 * r)} a$r,$r 0 0 1 $r,-$r z"

    val Search = stroke(circle(11f, 11f, 6.5f), "M20 20l-4.2-4.2")
    val Home = stroke("M4 10.5 12 4l8 6.5V19a1 1 0 0 1-1 1h-4.5v-5.5h-5V20H5a1 1 0 0 1-1-1z")
    val Grid = stroke(rect(4f, 4f, 6.5f, 6.5f, 2f), rect(13.5f, 4f, 6.5f, 6.5f, 2f), rect(4f, 13.5f, 6.5f, 6.5f, 2f), rect(13.5f, 13.5f, 6.5f, 6.5f, 2f))
    val Tv = stroke(rect(3f, 5f, 18f, 12f, 2.5f), "M9 21h6", "M12 17v4")
    val Sliders = stroke("M5 7h9", "M18 7h1", circle(16f, 7f, 2f), "M5 17h3", "M12 17h7", circle(10f, 17f, 2f), "M5 12h1", "M10 12h9", circle(8f, 12f, 2f))
    val Panel = stroke(rect(3.5f, 3.5f, 17f, 17f, 4f), "M14 3.5v17", "M17 8h.01", "M17 12h.01")
    val Moon = stroke("M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z")
    val Mic = stroke(rect(9f, 3f, 6f, 11f, 3f), "M5.5 11a6.5 6.5 0 0 0 13 0", "M12 17.5V21", width = 2f)
    val Sparkle = stroke("M12 3l1.8 4.7L18.5 9.5l-4.7 1.8L12 16l-1.8-4.7L5.5 9.5l4.7-1.8z", "M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z")
    val Back = stroke("m15 18-6-6 6-6", width = 2f)
    val Chevron = stroke("m9 6 6 6-6 6", width = 2f)
    val Expand = stroke("M4 8V4h4", "M20 8V4h-4", "M4 16v4h4", "M20 16v4h-4", width = 2f)
    val Shuffle = stroke("M3 7h3.5a4 4 0 0 1 3.3 1.7l4.4 6.6a4 4 0 0 0 3.3 1.7H21", "M3 17h3.5a4 4 0 0 0 3.3-1.7l.7-1", "M14.2 9.7l.6-1A4 4 0 0 1 18.1 7H21", "m18 4 3 3-3 3", "m18 14 3 3-3 3")
    val Repeat = stroke("m17 2 4 4-4 4", "M3 11V9a3 3 0 0 1 3-3h15", "m7 22-4-4 4-4", "M21 13v2a3 3 0 0 1-3 3H3")
    val Heart = stroke("M12 20s-7.5-4.6-7.5-10A4.5 4.5 0 0 1 12 7.2 4.5 4.5 0 0 1 19.5 10c0 5.4-7.5 10-7.5 10z")
    val Play = stroke("M7 4.5v15a1 1 0 0 0 1.5.9l12-7.5a1 1 0 0 0 0-1.7l-12-7.5A1 1 0 0 0 7 4.5z", fill = true)
    val Pause = stroke(rect(6f, 5f, 4f, 14f, 1.2f), rect(14f, 5f, 4f, 14f, 1.2f), fill = true)
    val Next = stroke("M6 5.5v13a1 1 0 0 0 1.5.9l9-6.5a1 1 0 0 0 0-1.6l-9-6.5A1 1 0 0 0 6 5.5z", rect(17f, 5f, 2.5f, 14f, 1f), fill = true)
    val Prev = stroke("M18 5.5v13a1 1 0 0 1-1.5.9l-9-6.5a1 1 0 0 1 0-1.6l9-6.5a1 1 0 0 1 1.5.9z", rect(4.5f, 5f, 2.5f, 14f, 1f), fill = true)
    val Wifi = stroke("M2 8.5a15 15 0 0 1 20 0", "M5.5 12a10 10 0 0 1 13 0", "M9 15.5a5 5 0 0 1 6 0", "M12 19h.01")
    val Bluetooth = stroke("m7 7 10 10-5 5V2l5 5L7 17")
    val Picture = stroke(rect(3f, 5f, 18f, 12f, 2f), "M8 21h8", "M12 17v4")
    val Sound = stroke("M11 5 6 9H3v6h3l5 4z", "M15.5 8.5a5 5 0 0 1 0 7", "M18.5 5.5a9 9 0 0 1 0 13")
    val Timer = stroke(circle(12f, 13f, 8f), "M12 9v4l2.5 2.5", "M9 2h6")
    val Hdmi = stroke("M4 8h16v5l-2 3H6l-2-3z", "M8 11h8")
    val Accessibility = stroke(circle(12f, 4f, 1.5f), "M5 8l7 1.5L19 8", "M12 9.5V14l-3 7", "M12 14l3 7")
    val Person = stroke(circle(12f, 8.5f, 3.5f), "M5 20a7 7 0 0 1 14 0")
    val Gear = stroke(circle(12f, 12f, 3f), "M12 2.5v2.2M12 19.3v2.2M4.6 4.6l1.6 1.6M17.8 17.8l1.6 1.6M2.5 12h2.2M19.3 12h2.2M4.6 19.4l1.6-1.6M17.8 6.2l1.6-1.6")
    val Power = stroke("M12 3v8", "M6.3 7.3a8 8 0 1 0 11.4 0")
    val Note = stroke("M9 18V6l11-2v12", circle(6.5f, 18f, 2.5f), circle(17.5f, 16f, 2.5f))
}

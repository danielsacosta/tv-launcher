package dev.tvlauncher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap

/**
 * Convierte el ícono de una app en un círculo que se llena por completo, sin caja alrededor.
 *
 * - Íconos adaptativos (fondo + logo): se dibujan las dos capas recortadas en círculo, como hace Android.
 * - Íconos planos: se recorta el margen transparente y, según su forma, se llenan el círculo
 *   o se centran sobre blanco (los que son solo un logo suelto, como Play Store).
 */
object IconRenderer {

    private const val SIZE = 256

    /** Ajustes finos para íconos planos conocidos (medidos sobre los íconos reales del TV). */
    private val overrides = mapOf(
        "com.netflix.ninja" to Tune(scale = 0.86f, background = Color.WHITE),
        "com.google.android.youtube.tv" to Tune(scale = 1.1f),
        "com.google.android.youtube.tvmusic" to Tune(scale = 1.6f, background = 0xFFFF0000.toInt()),
        "com.android.vending" to Tune(scale = 0.72f, background = Color.WHITE, forceGlyph = true),
    )

    private data class Tune(val scale: Float = 1f, val background: Int? = null, val forceGlyph: Boolean = false)

    fun render(packageName: String, drawable: Drawable): Bitmap {
        val out = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val circle = Path().apply { addCircle(SIZE / 2f, SIZE / 2f, SIZE / 2f, Path.Direction.CW) }
        canvas.clipPath(circle)

        if (drawable is AdaptiveIconDrawable && packageName !in overrides) {
            // Solo el 66 % central de un ícono adaptativo es visible: se agranda un 25 % por lado.
            val pad = SIZE / 4
            val bounds = Rect(-pad, -pad, SIZE + pad, SIZE + pad)
            drawable.background?.let { it.bounds = bounds; it.draw(canvas) }
            drawable.foreground?.let { it.bounds = bounds; it.draw(canvas) }
            return out
        }

        val src = drawable.toBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val tune = overrides[packageName] ?: Tune()
        val box = trimmedBounds(src)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        val cornersOpaque = opaqueAt(src, box, 0.15f, 0.15f) && opaqueAt(src, box, 0.85f, 0.85f)
        val edgesOpaque = opaqueAt(src, box, 0.03f, 0.5f) && opaqueAt(src, box, 0.5f, 0.03f)

        if (!tune.forceGlyph && (cornersOpaque || edgesOpaque)) {
            // Ícono que es un bloque de color (cuadrado o círculo): lo llena todo.
            val bg = tune.background ?: src.getPixel(
                (box.left + box.width() * 0.12f).toInt(),
                (box.top + box.height() * 0.5f).toInt(),
            )
            canvas.drawColor(bg or 0xFF000000.toInt())
            val side = maxOf(box.width(), box.height()).toFloat()
            val scale = SIZE / side * tune.scale
            val w = box.width() * scale
            val h = box.height() * scale
            val dst = RectF((SIZE - w) / 2, (SIZE - h) / 2, (SIZE + w) / 2, (SIZE + h) / 2)
            canvas.drawBitmap(src, box, dst, paint)
        } else {
            // Logo suelto con fondo transparente: va centrado sobre blanco.
            canvas.drawColor(tune.background ?: Color.WHITE)
            val side = maxOf(box.width(), box.height()).toFloat()
            val target = SIZE * if (tune.forceGlyph) tune.scale else 0.66f
            val scale = target / side
            val w = box.width() * scale
            val h = box.height() * scale
            val dst = RectF((SIZE - w) / 2, (SIZE - h) / 2, (SIZE + w) / 2, (SIZE + h) / 2)
            canvas.drawBitmap(src, box, dst, paint)
        }
        return out
    }

    /** Color dominante y saturado del ícono, para teñir el fondo del inicio. */
    fun dominantColor(bitmap: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
        var r = 0.0; var g = 0.0; var b = 0.0; var total = 0.0
        val hsv = FloatArray(3)
        for (y in 0 until 24) for (x in 0 until 24) {
            val c = small.getPixel(x, y)
            if (Color.alpha(c) < 200) continue
            Color.colorToHSV(c, hsv)
            val weight = (hsv[1] * hsv[2]).toDouble() + 0.02
            r += Color.red(c) * weight; g += Color.green(c) * weight; b += Color.blue(c) * weight
            total += weight
        }
        if (total == 0.0) return 0xFF2A3550.toInt()
        return Color.rgb((r / total).toInt(), (g / total).toInt(), (b / total).toInt())
    }

    private fun trimmedBounds(bmp: Bitmap): Rect {
        var left = bmp.width; var top = bmp.height; var right = -1; var bottom = -1
        for (y in 0 until bmp.height) for (x in 0 until bmp.width) {
            if (Color.alpha(bmp.getPixel(x, y)) > 16) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        return if (right < 0) Rect(0, 0, bmp.width, bmp.height) else Rect(left, top, right + 1, bottom + 1)
    }

    private fun opaqueAt(bmp: Bitmap, box: Rect, fx: Float, fy: Float): Boolean {
        val x = (box.left + box.width() * fx).toInt().coerceIn(0, bmp.width - 1)
        val y = (box.top + box.height() * fy).toInt().coerceIn(0, bmp.height - 1)
        return Color.alpha(bmp.getPixel(x, y)) > 200
    }
}

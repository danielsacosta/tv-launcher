package dev.tvlauncher.dream

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import dev.tvlauncher.media.MediaRepo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Protector de pantalla: reloj grande, fecha y lo que suena, sobre fondo oscuro. */
class ClockDreamService : DreamService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var time: TextView
    private lateinit var date: TextView
    private lateinit var song: TextView
    private val locale = Locale("es", "CO")

    private val tick = object : Runnable {
        override fun run() {
            val now = Date()
            time.text = SimpleDateFormat("HH:mm", locale).format(now)
            date.text = SimpleDateFormat("EEEE, d 'de' MMMM", locale).format(now)
                .replaceFirstChar { it.titlecase(locale) }
            val np = MediaRepo.nowPlaying.value
            song.text = np?.let { listOfNotNull(it.title, it.artist).joinToString(" · ") } ?: ""
            handler.postDelayed(this, 10_000)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        MediaRepo.attach(this)

        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(20, 26, 40), Color.rgb(11, 13, 16), Color.rgb(24, 30, 46)),
            )
        }
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun text(sizeSp: Float, color: Int) = TextView(this).apply {
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }
        time = text(120f, Color.rgb(242, 244, 247))
        date = text(20f, Color.rgb(169, 177, 187))
        song = text(16f, Color.rgb(201, 208, 216)).apply { setPadding(0, 24, 0, 0) }
        column.addView(time); column.addView(date); column.addView(song)
        val pad = (resources.displayMetrics.widthPixels * 0.08).toInt()
        root.addView(
            column,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.START)
                .apply { setMargins(pad, 0, 0, pad) },
        )
        setContentView(root)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        handler.post(tick)
    }

    override fun onDreamingStopped() {
        handler.removeCallbacks(tick)
        super.onDreamingStopped()
    }
}

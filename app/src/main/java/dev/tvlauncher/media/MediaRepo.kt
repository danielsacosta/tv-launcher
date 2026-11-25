package dev.tvlauncher.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.tvlauncher.data.IconRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lo que está sonando ahora, tal como lo reporta la app que reproduce. */
data class NowPlaying(
    val packageName: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val art: ImageBitmap?,
    /** Color dominante de la portada (ARGB) para teñir el fondo. */
    val tint: Int?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    /** Momento (elapsedRealtime) en que se midió [positionMs]. */
    val measuredAt: Long,
    val speed: Float,
) {
    fun currentPosition(now: Long = SystemClock.elapsedRealtime()): Long {
        if (!isPlaying) return positionMs
        val p = positionMs + ((now - measuredAt) * speed).toLong()
        return if (durationMs > 0) p.coerceAtMost(durationMs) else p
    }
}

/**
 * Sigue las sesiones de reproducción activas (Spotify, YouTube Music, etc.).
 * Requiere que el launcher tenga acceso a notificaciones; sin él, [hasAccess] queda en false.
 */
object MediaRepo {
    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _hasAccess = MutableStateFlow(false)
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private val main = Handler(Looper.getMainLooper())
    private var manager: MediaSessionManager? = null
    private var component: ComponentName? = null
    private var current: MediaController? = null
    private var lastArt: Bitmap? = null
    private var lastTint: Int? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        pick(list.orEmpty())
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            // Si esta sesión se pausa, puede que otra esté sonando.
            manager?.let { m -> runCatching { pick(m.getActiveSessions(component)) } } ?: publish()
        }
        override fun onSessionDestroyed() {
            manager?.let { m -> runCatching { pick(m.getActiveSessions(component)) } }
        }
    }

    fun attach(context: Context) {
        val app = context.applicationContext
        val m = app.getSystemService(MediaSessionManager::class.java) ?: return
        val c = ComponentName(app, MediaListenerService::class.java)
        try {
            val sessions = m.getActiveSessions(c)
            if (manager == null) m.addOnActiveSessionsChangedListener(sessionsListener, c, main)
            manager = m
            component = c
            _hasAccess.value = true
            pick(sessions)
        } catch (_: SecurityException) {
            _hasAccess.value = false
        }
    }

    private fun pick(sessions: List<MediaController>) {
        val best = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull { it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) != null }
        if (best?.sessionToken != current?.sessionToken) {
            current?.unregisterCallback(callback)
            current = best
            best?.registerCallback(callback, main)
        }
        publish()
    }

    private fun publish() {
        val c = current
        val md = c?.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        if (c == null || md == null || title.isNullOrBlank()) {
            _nowPlaying.value = null
            return
        }
        val art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (art !== lastArt) {
            lastArt = art
            lastTint = art?.let { IconRenderer.dominantColor(it.copy(Bitmap.Config.ARGB_8888, false)) }
        }
        val state = c.playbackState
        _nowPlaying.value = NowPlaying(
            packageName = c.packageName,
            title = title,
            artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),
            album = md.getString(MediaMetadata.METADATA_KEY_ALBUM),
            art = art?.asImageBitmap(),
            tint = lastTint,
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            positionMs = state?.position ?: 0L,
            durationMs = md.getLong(MediaMetadata.METADATA_KEY_DURATION),
            measuredAt = state?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
            speed = state?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
        )
    }

    fun playPause() {
        val c = current ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun next() = current?.transportControls?.skipToNext()
    fun previous() = current?.transportControls?.skipToPrevious()
}

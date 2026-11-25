package dev.tvlauncher.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import dev.tvlauncher.data.KnownApps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lo que está sonando (o lo último que sonó) en una app. */
data class NowPlaying(
    /** Paquete que publica la sesión (YouTube Music en este TV reproduce desde el de YouTube). */
    val packageName: String,
    /** App a la que el usuario le atribuye lo que suena: la que se ve en Favoritas. */
    val owner: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val art: ImageBitmap?,
    /** Color dominante de la portada (ARGB) para teñir el fondo. */
    val tint: Int?,
    val isPlaying: Boolean,
    /** false cuando la app ya cerró su sesión: queda como "lo último" que se vio o escuchó. */
    val live: Boolean,
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
 * Sigue las sesiones de reproducción (Spotify, YouTube, YouTube Music…).
 * Requiere que el launcher tenga acceso a notificaciones; sin él, [hasAccess] queda en false.
 *
 * Además de lo que suena ahora ([nowPlaying]) guarda lo último de cada app ([recent]), porque
 * YouTube cierra su sesión en cuanto se sale de la app y si no la miniatura desaparecería.
 */
object MediaRepo {
    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _recent = MutableStateFlow<Map<String, NowPlaying>>(emptyMap())
    /** Lo último de cada app, por [NowPlaying.owner]. */
    val recent: StateFlow<Map<String, NowPlaying>> = _recent.asStateFlow()

    private val _hasAccess = MutableStateFlow(false)
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    /** Última app abierta desde el launcher; sirve para saber si lo de YouTube viene de YouTube Music. */
    @Volatile var lastLaunched: String? = null

    private val main = Handler(Looper.getMainLooper())
    private var appContext: Context? = null
    private var manager: MediaSessionManager? = null
    private var component: ComponentName? = null
    private var current: MediaController? = null
    private var currentOwner: String? = null
    private var lastArt: Bitmap? = null
    private var lastTint: Int? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        pick(list.orEmpty())
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            // Si esta sesión se pausa, puede que otra esté sonando.
            refresh() ?: publish()
        }
        override fun onSessionDestroyed() {
            refresh() ?: pick(emptyList())
        }
    }

    private fun refresh(): Unit? = manager?.let { m -> runCatching { pick(m.getActiveSessions(component)) } }?.getOrNull()

    fun attach(context: Context) {
        val app = context.applicationContext
        appContext = app
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

    private fun ownerFor(pkg: String): String =
        if (pkg == KnownApps.YOUTUBE && lastLaunched == KnownApps.YT_MUSIC) KnownApps.YT_MUSIC else pkg

    private fun pick(sessions: List<MediaController>) {
        val withTitle = sessions.filter { it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) != null }
        val best = withTitle.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: withTitle.firstOrNull()
        if (best?.sessionToken != current?.sessionToken) {
            current?.unregisterCallback(callback)
            current = best
            currentOwner = best?.let { ownerFor(it.packageName) }
            best?.registerCallback(callback, main)
        }
        // Lo que ya no tiene sesión queda guardado como "lo último", sin reproducirse.
        val alive = withTitle.map { ownerFor(it.packageName) }.toSet() + listOfNotNull(currentOwner)
        _recent.value = _recent.value.mapValues { (owner, np) ->
            if (owner in alive) np else np.copy(isPlaying = false, live = false, positionMs = np.currentPosition())
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
        val owner = currentOwner ?: ownerFor(c.packageName)
        val np = NowPlaying(
            packageName = c.packageName,
            owner = owner,
            title = title,
            artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),
            album = md.getString(MediaMetadata.METADATA_KEY_ALBUM),
            art = art?.asImageBitmap(),
            tint = lastTint,
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            live = true,
            positionMs = state?.position ?: 0L,
            durationMs = md.getLong(MediaMetadata.METADATA_KEY_DURATION),
            measuredAt = state?.lastPositionUpdateTime?.takeIf { it > 0 } ?: SystemClock.elapsedRealtime(),
            speed = state?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
        )
        _nowPlaying.value = np
        _recent.value = _recent.value + (owner to np)
    }

    /**
     * Reproduce o pausa. Spotify "duerme" su reproductor tras una pausa larga e ignora la orden
     * de reanudar; si a los 2,5 s no suena, se abre su pantalla de reproducción para seguir ahí.
     */
    fun playPause() {
        val c = current ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) {
            c.transportControls.pause()
            return
        }
        c.transportControls.play()
        val token = c.sessionToken
        main.postDelayed({
            val now = current
            if (now?.sessionToken == token && now.playbackState?.state != PlaybackState.STATE_PLAYING) {
                openPlayer(now)
            }
        }, 2500)
    }

    fun next() = current?.transportControls?.skipToNext()
    fun previous() = current?.transportControls?.skipToPrevious()

    /** Abre la pantalla de reproducción de la app que suena (o la app, si no la ofrece). */
    private fun openPlayer(c: MediaController) {
        val ok = runCatching { c.sessionActivity?.send() != null }.getOrDefault(false)
        if (!ok) {
            val ctx = appContext ?: return
            val pm = ctx.packageManager
            val intent = pm.getLeanbackLaunchIntentForPackage(c.packageName)
                ?: pm.getLaunchIntentForPackage(c.packageName) ?: return
            runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}

package dev.tvlauncher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvlauncher.data.AppInfo
import dev.tvlauncher.data.KnownApps
import dev.tvlauncher.data.LauncherSettings
import dev.tvlauncher.media.MediaRepo
import dev.tvlauncher.media.NowPlaying
import kotlinx.coroutines.delay

/** Sombra suave para textos que van sobre imágenes. */
private val SoftShadow = Shadow(Color.Black.copy(alpha = 0.55f), blurRadius = 18f)

/** Lo que muestra el hero según la app enfocada. */
private data class Hero(
    val key: String,
    val eyebrow: String,
    val title: String,
    val line1: String?,
    val line2: String?,
    val tint: Color,
    val icon: ImageBitmap?,
    val art: ImageBitmap?,
    val music: NowPlaying?,
    val primary: String,
)

private val taglines = mapOf(
    "com.spotify.tv.android" to "Música y podcasts",
    "com.google.android.youtube.tv" to "Videos, música y en vivo",
    "com.netflix.ninja" to "Series y películas",
    "com.disney.disneyplus" to "Disney, Pixar, Marvel, Star Wars y National Geographic",
    "com.amazon.amazonvideo.livingroom" to "Películas, series y Amazon Originals",
    "com.wbd.stream" to "Series y películas de HBO y Warner",
    "com.google.android.youtube.tvmusic" to "Canciones, álbumes y videos musicales",
)

@Composable
fun HomeScreen(
    favorites: List<AppInfo>,
    allApps: List<AppInfo>,
    nowPlaying: NowPlaying?,
    recent: Map<String, NowPlaying>,
    settings: LauncherSettings,
    focusedPkg: String?,
    onFocusApp: (String) -> Unit,
    focusTrigger: Int,
    onOpenApp: (String) -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    val accent = Color(settings.accent)
    val now = rememberNow()

    val focusedApp = favorites.firstOrNull { it.packageName == focusedPkg } ?: favorites.firstOrNull()
    val hero = buildHero(focusedApp, nowPlaying, recent, allApps, settings.showNowPlaying)

    // Al volver al inicio, el foco va a la app que está sonando (si está en Favoritas) o a la primera.
    val requesters = remember(favorites.map { it.packageName }) {
        favorites.associate { it.packageName to FocusRequester() }
    }
    LaunchedEffect(focusTrigger, requesters) {
        if (requesters.isEmpty()) return@LaunchedEffect
        val target = nowPlaying?.takeIf { it.isPlaying }?.owner?.takeIf { it in requesters }
            ?: focusedPkg?.takeIf { it in requesters }
            ?: favorites.first().packageName
        // Se pide dos veces: al volver de otra app, Android puede devolver el foco al último
        // elemento enfocado (a veces el menú) justo después del primer intento.
        for (wait in listOf(80L, 320L)) {
            delay(wait)
            runCatching { requesters.getValue(target).requestFocus() }
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.Ground)) {
        // Cada hero (tinte + portada con sus difuminados) es una capa que se pinta UNA sola vez.
        // Al cambiar de app o de canción solo se funden dos capas ya pintadas: es barato incluso
        // en la GPU de gama baja de este TV, donde volver a pintar el fondo costaba ~200 ms por cuadro.
        AnimatedContent(
            targetState = hero,
            transitionSpec = { Motion.artEnter togetherWith Motion.artExit },
            contentKey = { it.key },
            label = "heroBackdrop",
        ) { h -> HeroBackdrop(h, settings.abstractWallpaper) }

        // Velos fijos para que el texto y el reloj siempre se lean; también en su propia capa.
        Box(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        listOf(Palette.Ground.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(420f, 410f),
                        radius = 1000f,
                    )
                )
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0f to Palette.Ground.copy(alpha = 0.7f),
                            0.45f to Palette.Ground.copy(alpha = 0.35f),
                            1f to Color.Transparent,
                            center = Offset(1640f, 70f),
                            radius = 560f,
                        )
                    )
            )
        }

        Rail(
            selected = Screen.Home,
            onSelect = onNavigate,
            modifier = Modifier.padding(start = Layout.RailStart, top = 54.dp, bottom = 54.dp),
        )

        Row(
            Modifier.align(Alignment.TopEnd).padding(top = 54.dp, end = 96.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(formatDate(now), color = Palette.TextSoft, fontSize = 22.sp, fontWeight = FontWeight.Medium, style = TextStyle(shadow = SoftShadow))
            Text(formatTime(now, settings.clock24h), color = Palette.Text, fontSize = 44.sp, letterSpacing = (-1).sp, style = TextStyle(shadow = SoftShadow))
        }

        HeroText(
            hero = hero,
            accent = accent,
            onPrimary = {
                if (hero.music?.live == true) onNavigate(Screen.NowPlaying)
                else focusedApp?.let { onOpenApp(it.packageName) }
            },
            modifier = Modifier.padding(start = Layout.ContentStart, top = 150.dp).width(900.dp),
        )

        Column(
            Modifier.align(Alignment.BottomStart).padding(start = Layout.ContentStart, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(34.dp),
        ) {
            Text(
                "FAVORITAS", color = Palette.Muted, fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                favorites.forEach { app ->
                    AppCircle(
                        app = app,
                        accent = accent,
                        focusRequester = requesters[app.packageName],
                        onFocused = { onFocusApp(app.packageName) },
                        onClick = { onOpenApp(app.packageName) },
                    )
                }
                AllAppsCircle(accent) { onNavigate(Screen.Apps) }
            }
        }
    }
}

private fun buildHero(
    app: AppInfo?,
    live: NowPlaying?,
    recent: Map<String, NowPlaying>,
    all: List<AppInfo>,
    showMedia: Boolean,
): Hero {
    if (app == null) {
        return Hero("empty", "Bienvenido", "Tu TV", null, null, Palette.Deep, null, null, null, "Ver apps")
    }
    // Cada app muestra solo lo suyo: lo que suena ahora en ella o, si ya cerró, lo último que se vio.
    val media = if (!showMedia) null
    else live?.takeIf { it.owner == app.packageName } ?: recent[app.packageName]
    if (media != null) {
        val label = app.label
        return Hero(
            key = "media:${media.owner}:${media.title}:${media.live}",
            eyebrow = when {
                media.live && media.isPlaying -> "Sonando en $label"
                media.live -> "En pausa · $label"
                else -> "Lo último en $label"
            },
            title = media.title,
            line1 = media.artist,
            line2 = media.album?.takeIf { it != media.artist },
            tint = media.tint?.let { Color(it) } ?: Color(app.tint),
            icon = app.icon,
            art = media.art,
            music = media,
            primary = if (media.live) "Abrir reproductor" else "Seguir en $label",
        )
    }
    return Hero(
        key = "app:${app.packageName}",
        eyebrow = app.label,
        title = taglines[app.packageName] ?: app.label,
        line1 = null,
        line2 = null,
        tint = Color(app.tint),
        icon = app.icon,
        art = null,
        music = null,
        primary = "Abrir ${app.label}",
    )
}

@Composable
private fun HeroBackdrop(h: Hero, abstract: Boolean) {
    Box(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        // Tinte del fondo con el color de la portada o del ícono.
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(h.tint.copy(alpha = 0.42f), Color.Transparent),
                    center = Offset(1350f, 380f),
                    radius = 1400f,
                )
            )
        )
        Box(Modifier.align(Alignment.TopEnd)) { HeroArt(h, abstract) }
    }
}

@Composable
private fun HeroArt(h: Hero, abstract: Boolean) {
    if (h.art != null) {
        // Portada: fondo hecho con la misma portada desenfocada + portada nítida, todo fundido.
        Box(
            Modifier
                .width(1180.dp)
                .height(1080.dp)
                .heroFade(
                    leftStops = listOf(0f to 0f, 0.22f to 0.35f, 0.42f to 0.85f, 0.58f to 1f),
                    bottomStops = listOf(0f to 1f, 0.4f to 1f, 0.56f to 0.9f, 0.78f to 0.4f, 1f to 0f),
                )
        ) {
            // La portada llena toda su zona: arriba y a la derecha toca el borde de la pantalla,
            // y hacia la izquierda y abajo se desvanece. Así nunca queda un borde visible.
            Image(
                h.art, h.title, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else {
        // Sin imagen: luces difuminadas con el color de la app.
        Box(
            Modifier
                .width(1600.dp)
                .height(1080.dp)
                .heroFade(
                    leftStops = listOf(0f to 0f, 0.18f to 0.3f, 0.36f to 0.8f, 0.52f to 1f),
                    bottomStops = listOf(0f to 1f, 0.42f to 1f, 0.58f to 0.85f, 0.8f to 0.35f, 1f to 0f),
                )
                .background(
                    Brush.radialGradient(
                        listOf(h.tint, Color.Transparent), center = Offset(1000f, 420f), radius = 650f,
                    )
                )
                .background(
                    Brush.radialGradient(
                        listOf(if (abstract) Palette.Deep else Color.Transparent, Color.Transparent),
                        center = Offset(1250f, 800f), radius = 700f,
                    )
                )
        )
    }
}

@Composable
private fun HeroTitles(hero: Hero) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            hero.icon?.let {
                Image(it, null, modifier = Modifier.size(34.dp).clip(CircleShape))
            }
            Text(
                hero.eyebrow.uppercase(SpanishCo), color = Palette.TextSoft, fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, maxLines = 1,
            )
        }
        Text(
            hero.title,
            color = Palette.Text,
            fontSize = if (hero.title.length > 18) 80.sp else 96.sp,
            lineHeight = if (hero.title.length > 18) 84.sp else 98.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-2).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        hero.line1?.let { Text(it, color = Palette.TextSoft, fontSize = 30.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        hero.line2?.let { Text(it, color = Palette.Muted, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun HeroText(hero: Hero, accent: Color, onPrimary: () -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        // Solo el texto cambia con transición: la barra y los botones se quedan quietos para no perder el foco.
        AnimatedContent(
            targetState = hero,
            contentKey = { it.key },
            transitionSpec = { (Motion.textEnter togetherWith Motion.textExit).using(SizeTransform(clip = false)) },
            label = "heroText",
        ) { h -> HeroTitles(h) }
        hero.music?.takeIf { it.live && it.durationMs > 0 }?.let { Progress(it) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PillButton(
                text = hero.primary,
                icon = if (hero.music?.live == true) Icons.Expand else Icons.Play,
                primary = true,
                accent = accent,
                onClick = onPrimary,
            )
            if (hero.music?.live == true) {
                RoundButton(if (hero.music.isPlaying) Icons.Pause else Icons.Play, "Pausar o reproducir", accent) { MediaRepo.playPause() }
                RoundButton(Icons.Next, "Siguiente", accent) { MediaRepo.next() }
            }
        }
    }
}

@Composable
fun Progress(np: NowPlaying, width: Int = 460, height: Int = 6) {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(np.isPlaying, np.measuredAt) {
        while (np.isPlaying) { delay(1000); tick++ }
    }
    @Suppress("UNUSED_EXPRESSION") tick
    val pos = np.currentPosition()
    val fraction = if (np.durationMs > 0) (pos.toFloat() / np.durationMs).coerceIn(0f, 1f) else 0f
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(formatDuration(pos), color = Palette.Muted, fontSize = 18.sp, maxLines = 1, softWrap = false)
        Box(Modifier.width(width.dp).height(height.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x29FFFFFF))) {
            Box(Modifier.fillMaxWidth(fraction).height(height.dp).clip(RoundedCornerShape(3.dp)).background(Palette.Text))
        }
        if (np.durationMs > 0) Text(formatDuration(np.durationMs), color = Palette.Muted, fontSize = 18.sp, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun AllAppsCircle(accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier
                .size(132.dp)
                .scale(if (focused) 1.1f else 1f)
                .focusRing(focused, accent, pill = false)
                .clip(CircleShape)
                .background(Color(0x14FFFFFF))
                .border(1.dp, Color(0x1FFFFFFF), CircleShape)
                .tvClickable(interaction, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Grid, "Ver todas las apps", tint = Palette.Icon, modifier = Modifier.size(40.dp))
        }
        Text("Todas", color = if (focused) Palette.Text else Palette.Muted, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(0.dp))
    }
}

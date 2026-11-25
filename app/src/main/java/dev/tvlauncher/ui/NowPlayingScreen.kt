package dev.tvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvlauncher.data.AppInfo
import dev.tvlauncher.media.MediaRepo
import dev.tvlauncher.media.NowPlaying

@Composable
fun NowPlayingScreen(np: NowPlaying?, app: AppInfo?, accent: Color, onBack: () -> Unit) {
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { playFocus.requestFocus() } }

    Box(Modifier.fillMaxSize().background(Palette.Ground)) {
        if (np == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Icon(Icons.Note, null, tint = Palette.Muted, modifier = Modifier.size(96.dp))
                Text("No está sonando nada", color = Palette.Text, fontSize = 40.sp)
                PillButton("Volver al inicio", primary = true, accent = accent, focusRequester = playFocus, onClick = onBack)
            }
            return@Box
        }

        // Fondo: la misma portada ampliada y muy desenfocada.
        np.art?.let {
            Image(
                it, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().scale(1.25f).blur(110.dp).alpha(0.6f),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Palette.Ground.copy(alpha = 0.6f), 0.3f to Color.Transparent,
                    0.6f to Color.Transparent, 1f to Palette.Ground.copy(alpha = 0.85f),
                )
            )
        )

        Row(
            Modifier.padding(start = 96.dp, end = 96.dp, top = 54.dp).align(Alignment.TopStart),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PillButton("Inicio", icon = Icons.Back, accent = accent, onClick = onBack)
        }
        if (app != null) {
            Row(
                Modifier.align(Alignment.TopEnd).padding(top = 54.dp, end = 96.dp)
                    .clip(RoundedCornerShape(28.dp)).background(Color(0x1AFFFFFF))
                    .padding(start = 12.dp, end = 22.dp, top = 11.dp, bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(app.icon, null, modifier = Modifier.size(34.dp).clip(CircleShape))
                Text("Desde ${app.label}", color = Palette.TextSoft, fontSize = 20.sp)
            }
        }

        Row(
            Modifier.align(Alignment.CenterStart).padding(start = 200.dp, end = 160.dp).offset(y = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(112.dp),
        ) {
            Box(Modifier.size(640.dp)) {
                np.art?.let {
                    Image(
                        it, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.offset(40.dp, 70.dp).size(560.dp).blur(60.dp).alpha(0.75f),
                    )
                    Image(
                        it, np.album ?: np.title, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(640.dp).shadow(40.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)),
                    )
                } ?: Box(
                    Modifier.size(640.dp).clip(RoundedCornerShape(28.dp)).background(Palette.Deep),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Note, null, tint = Palette.Muted, modifier = Modifier.size(120.dp)) }
            }

            Column(Modifier.width(860.dp), verticalArrangement = Arrangement.spacedBy(52.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        if (np.isPlaying) "SONANDO AHORA" else "EN PAUSA",
                        color = Palette.Icon, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                    )
                    Text(
                        np.title, color = Palette.Text,
                        fontSize = if (np.title.length > 18) 80.sp else 104.sp,
                        lineHeight = if (np.title.length > 18) 84.sp else 102.sp,
                        fontWeight = FontWeight.Medium, letterSpacing = (-3).sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    np.artist?.let { Text(it, color = Palette.TextSoft, fontSize = 36.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
                    np.album?.takeIf { it != np.artist }?.let { Text(it, color = Palette.Muted, fontSize = 24.sp, maxLines = 1) }
                }
                Progress(np, width = 620, height = 8)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    RoundButton(Icons.Prev, "Anterior", accent, size = 88.dp) { MediaRepo.previous() }
                    RoundButton(
                        if (np.isPlaying) Icons.Pause else Icons.Play, "Pausar o reproducir", accent,
                        size = 124.dp, filled = true, focusRequester = playFocus,
                    ) { MediaRepo.playPause() }
                    RoundButton(Icons.Next, "Siguiente", accent, size = 88.dp) { MediaRepo.next() }
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
    }
}

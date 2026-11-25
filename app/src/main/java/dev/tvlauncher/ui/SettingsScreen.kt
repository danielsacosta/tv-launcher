package dev.tvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvlauncher.data.LauncherSettings

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    hasMediaAccess: Boolean,
    onChange: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    val context = LocalContext.current
    val accent = Color(settings.accent)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    AmbientBackground(accent) {
        Rail(Screen.Settings, onNavigate, Modifier.padding(start = Layout.RailStart, top = 54.dp, bottom = 54.dp))
        Column(
            Modifier.fillMaxSize().padding(start = Layout.ContentStart, end = 240.dp, top = 54.dp),
            verticalArrangement = Arrangement.spacedBy(44.dp),
        ) {
            Text("Ajustes", color = Palette.Text, fontSize = 56.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1.5).sp)

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                SectionTitle("Color de acento")
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Palette.Accents.forEachIndexed { i, c ->
                        Swatch(Color(c), selected = settings.accent == c, focusRequester = if (i == 0) first else null) {
                            onChange { it.copy(accent = c) }
                        }
                    }
                }
            }

            Column(Modifier.width(1100.dp)) {
                ToggleRow("Mostrar lo que suena en el inicio", settings.showNowPlaying, accent) { onChange { it.copy(showNowPlaying = !it.showNowPlaying) } }
                ToggleRow("Fondo con luces difuminadas", settings.abstractWallpaper, accent) { onChange { it.copy(abstractWallpaper = !it.abstractWallpaper) } }
                ToggleRow("Reloj en formato 24 horas", settings.clock24h, accent) { onChange { it.copy(clock24h = !it.clock24h) } }
            }

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                SectionTitle("Lo que suena")
                Text(
                    if (hasMediaAccess) "Activado: el inicio muestra la portada y la canción de lo que suena."
                    else "Sin permiso. Actívalo una vez por ADB: cmd notification allow_listener dev.tvlauncher/dev.tvlauncher.media.MediaListenerService",
                    color = Palette.Muted, fontSize = 20.sp,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                SectionTitle("Ajustes del sistema")
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PillButton("Wi-Fi", accent = accent) { SystemActions.wifi(context) }
                    PillButton("Imagen", accent = accent) { SystemActions.picture(context) }
                    PillButton("Sonido", accent = accent) { SystemActions.sound(context) }
                    PillButton("Protector de pantalla", accent = accent) { SystemActions.dreamSettings(context) }
                    PillButton("Todos los ajustes", accent = accent) { SystemActions.settings(context) }
                }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, focusRequester: FocusRequester?, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        Modifier
            .size(72.dp)
            .scale(if (focused) 1.1f else 1f)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusRing(selected || focused, if (focused) color else Palette.Text, pill = false)
            .clip(CircleShape)
            .background(color)
            .tvClickable(interaction, onClick = onClick),
    )
}

@Composable
private fun ToggleRow(label: String, on: Boolean, accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (focused) Color(0x1AFFFFFF) else Color.Transparent)
            .tvClickable(interaction, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Palette.Text, fontSize = 24.sp)
        Box(
            Modifier.width(72.dp).height(40.dp).clip(RoundedCornerShape(20.dp))
                .background(if (on) accent else Color(0x29FFFFFF)).padding(4.dp),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(if (on) Palette.Ground else Palette.Icon))
        }
    }
}

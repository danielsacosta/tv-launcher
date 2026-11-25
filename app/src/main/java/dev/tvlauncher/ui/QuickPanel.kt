package dev.tvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvlauncher.data.LauncherSettings

private data class Tile(val label: String, val value: String, val icon: ImageVector, val on: Boolean = false, val action: () -> Unit)

/** Panel que se desliza desde la derecha, como el de Google TV. */
@Composable
fun QuickPanel(settings: LauncherSettings, onClose: () -> Unit) {
    val context = LocalContext.current
    val accent = Color(settings.accent)
    val now = rememberNow()
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    val tiles = listOf(
        Tile("Wi-Fi", "Red e internet", Icons.Wifi, on = true) { SystemActions.wifi(context) },
        Tile("Bluetooth", "Control y accesorios", Icons.Bluetooth) { SystemActions.bluetooth(context) },
        Tile("Imagen", "Modo de imagen", Icons.Picture) { SystemActions.picture(context) },
        Tile("Sonido", "Modo de sonido", Icons.Sound) { SystemActions.sound(context) },
        Tile("Entradas", "HDMI y TV en vivo", Icons.Hdmi) { SystemActions.inputPicker(context) },
        Tile("Accesibilidad", "Subtítulos y texto", Icons.Accessibility) { SystemActions.accessibility(context) },
        Tile("Protector", "Reloj y lo que suena", Icons.Moon) { SystemActions.dreamSettings(context) },
        Tile("Reposo", "Tiempo sin uso", Icons.Timer) { SystemActions.settings(context) },
    )

    Box(Modifier.fillMaxSize().background(Palette.Ground.copy(alpha = 0.72f))) {
        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .width(720.dp)
                .fillMaxHeight()
                .background(Palette.GlassStrong)
                .padding(horizontal = 56.dp, vertical = 54.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0x1FFFFFFF)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Person, null, tint = Palette.Icon, modifier = Modifier.size(30.dp))
                    }
                    Column {
                        Text("Perfil de Google", color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text("Cuenta y perfiles", color = Palette.Muted, fontSize = 18.sp)
                    }
                }
                Text(formatTime(now, settings.clock24h), color = Palette.Text, fontSize = 40.sp, letterSpacing = (-1).sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                tiles.chunked(2).forEachIndexed { row, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        pair.forEachIndexed { col, t ->
                            PanelTile(t, accent, Modifier.weight(1f), if (row == 0 && col == 0) first else null)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PanelRow("Cuenta y perfiles", Icons.Person, accent) { SystemActions.accounts(context) }
                PanelRow("Ajustes del dispositivo", Icons.Gear, accent) { SystemActions.settings(context) }
            }
        }
    }
}

@Composable
private fun PanelTile(t: Tile, accent: Color, modifier: Modifier, focusRequester: FocusRequester?) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(
        modifier
            .height(148.dp)
            .scale(if (focused) 1.03f else 1f)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(RoundedCornerShape(28.dp))
            .background(if (focused) Palette.Text else Color(0x14FFFFFF))
            .tvClickable(interaction, onClick = t.action)
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(
                when {
                    focused -> Color(0x14000000)
                    t.on -> accent
                    else -> Color(0x1AFFFFFF)
                }
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(t.icon, null, tint = if (focused || t.on) Palette.Ground else Palette.Icon, modifier = Modifier.size(24.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t.label, color = if (focused) Palette.Ground else Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.value, color = if (focused) Color(0xFF3A424D) else Palette.Muted, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PanelRow(text: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(if (focused) Palette.Text else Color(0x1AFFFFFF))
            .tvClickable(interaction, onClick = onClick)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = if (focused) Palette.Ground else Palette.Text, modifier = Modifier.size(26.dp))
        Text(text, color = if (focused) Palette.Ground else Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(Icons.Chevron, null, tint = if (focused) Palette.Ground else Palette.Muted, modifier = Modifier.size(22.dp))
    }
}

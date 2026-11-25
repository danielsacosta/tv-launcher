package dev.tvlauncher.ui

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Una entrada del TV (HDMI, antena…) tal como la reporta el sistema. */
data class TvInput(val id: String, val label: String, val isTuner: Boolean, val connected: Boolean?)

fun loadInputs(context: Context): List<TvInput> {
    val tim = context.getSystemService(TvInputManager::class.java) ?: return emptyList()
    return runCatching {
        tim.tvInputList.map { info ->
            val state = runCatching { tim.getInputState(info.id) }.getOrNull()
            TvInput(
                id = info.id,
                label = info.loadCustomLabel(context)?.toString()?.takeIf { it.isNotBlank() }
                    ?: info.loadLabel(context).toString(),
                isTuner = info.type == TvInputInfo.TYPE_TUNER,
                connected = when (state) {
                    TvInputManager.INPUT_STATE_CONNECTED -> true
                    TvInputManager.INPUT_STATE_DISCONNECTED -> false
                    else -> null
                },
            )
        }.sortedWith(compareByDescending<TvInput> { it.connected == true }.thenBy { it.label })
    }.getOrDefault(emptyList())
}

fun openInput(context: Context, input: TvInput) {
    val uri = if (input.isTuner) TvContract.buildChannelsUriForInput(input.id)
    else TvContract.buildChannelUriForPassthroughInput(input.id)
    val ok = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
    if (!ok) SystemActions.inputPicker(context)
}

@Composable
fun InputsScreen(accent: Color, onNavigate: (Screen) -> Unit) {
    val context = LocalContext.current
    val inputs = remember { loadInputs(context) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    AmbientBackground(accent) {
        Rail(Screen.Inputs, onNavigate, Modifier.padding(start = Layout.RailStart, top = 54.dp, bottom = 54.dp))
        Column(
            Modifier.fillMaxSize().padding(start = Layout.ContentStart, end = 96.dp, top = 54.dp),
            verticalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Entradas", color = Palette.Text, fontSize = 64.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1.5).sp)
                Text("Elige de dónde quieres ver.", color = Palette.Muted, fontSize = 22.sp)
            }
            if (inputs.isEmpty()) {
                PillButton("Abrir el selector de entradas", accent = accent, focusRequester = first) { SystemActions.inputPicker(context) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    inputs.take(5).forEachIndexed { i, input ->
                        InputCard(input, accent, if (i == 0) first else null) { openInput(context, input) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputCard(input: TvInput, accent: Color, focusRequester: FocusRequester?, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "inputScale")
    val shape = RoundedCornerShape(32.dp)
    Column(
        Modifier
            .width(300.dp)
            .scale(scale)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(shape)
            .background(if (focused) Palette.Text else Color(0x12FFFFFF))
            .tvClickable(interaction, onClick = onClick)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(64.dp),
    ) {
        Icon(
            if (input.isTuner) Icons.Tv else Icons.Hdmi, null,
            tint = if (focused) Palette.Ground else Palette.Icon, modifier = Modifier.size(48.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                input.label, color = if (focused) Palette.Ground else Palette.Text, fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(10.dp).clip(CircleShape)
                        .background(if (input.connected == true) Color(0xFF9BE3C4) else Color(0xFF5A626D))
                )
                Text(
                    when (input.connected) { true -> "Equipo conectado"; false -> "Sin señal"; null -> if (input.isTuner) "Antena" else "Entrada" },
                    color = if (focused) Color(0xFF3A424D) else Palette.Muted, fontSize = 20.sp,
                )
            }
        }
    }
}

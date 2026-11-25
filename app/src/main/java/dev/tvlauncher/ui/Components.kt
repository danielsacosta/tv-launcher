package dev.tvlauncher.ui

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvlauncher.data.AppInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val SpanishCo: Locale = Locale("es", "CO")

/** Hora y fecha que se actualizan solas. */
@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(10_000)
        }
    }
    return now
}

fun formatTime(ms: Long, h24: Boolean): String =
    SimpleDateFormat(if (h24) "HH:mm" else "h:mm", SpanishCo).format(Date(ms))

fun formatDate(ms: Long): String =
    SimpleDateFormat("EEEE, d 'de' MMMM", SpanishCo).format(Date(ms))
        .replaceFirstChar { it.titlecase(SpanishCo) }

fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/**
 * Hace un elemento enfocable con el control remoto: OK hace clic, mantener OK (o la tecla Menú)
 * hace clic largo. Devuelve si está enfocado para pintar el foco.
 */
fun Modifier.tvClickable(
    interaction: MutableInteractionSource,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = this
    .onPreviewKeyEvent { e ->
        val code = e.nativeKeyEvent.keyCode
        val isOk = code == NativeKeyEvent.KEYCODE_DPAD_CENTER || code == NativeKeyEvent.KEYCODE_ENTER
        when {
            onLongClick != null && code == NativeKeyEvent.KEYCODE_MENU && e.type == KeyEventType.KeyUp -> {
                onLongClick(); true
            }
            onLongClick != null && isOk && e.type == KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount == 1 -> {
                onLongClick(); true
            }
            // Tras un clic largo, se ignoran las repeticiones y el soltar del botón.
            isOk && e.nativeKeyEvent.repeatCount > 1 -> true
            else -> false
        }
    }
    .clickable(interactionSource = interaction, indication = null, onClick = onClick)

/** Desvanece el contenido hacia la izquierda y hacia abajo, sin líneas duras. */
fun Modifier.heroFade(leftStops: List<Pair<Float, Float>>, bottomStops: List<Pair<Float, Float>>): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(
            Brush.horizontalGradient(*leftStops.map { it.first to Color.Black.copy(alpha = it.second) }.toTypedArray()),
            blendMode = BlendMode.DstIn,
        )
        drawRect(
            Brush.verticalGradient(*bottomStops.map { it.first to Color.Black.copy(alpha = it.second) }.toTypedArray()),
            blendMode = BlendMode.DstIn,
        )
    }

/** Aro de foco de 3 px separado 5 px del elemento, dibujado por fuera para no encoger el contenido. */
fun Modifier.focusRing(focused: Boolean, accent: Color, pill: Boolean): Modifier =
    focusRing(if (focused) 1f else 0f, accent, pill)

/** Igual que el anterior, pero con [progress] de 0 a 1 para que el aro aparezca suavemente. */
fun Modifier.focusRing(progress: Float, accent: Color, pill: Boolean): Modifier = drawBehind {
    if (progress <= 0.01f) return@drawBehind
    val gap = 6.dp.toPx() * progress
    val w = 3.dp.toPx()
    val accent = accent.copy(alpha = accent.alpha * progress)
    val inset = gap + w / 2
    if (pill) {
        drawRoundRect(
            color = accent,
            topLeft = Offset(-inset, -inset),
            size = Size(size.width + inset * 2, size.height + inset * 2),
            cornerRadius = CornerRadius(size.height / 2 + inset),
            style = Stroke(w),
        )
    } else {
        drawCircle(accent, radius = size.minDimension / 2 + inset, style = Stroke(w))
    }
}

/** Superficie de vidrio: fondo translúcido con borde interior suave. */
fun Modifier.glass(shape: androidx.compose.ui.graphics.Shape, color: Color = Palette.Glass): Modifier = this
    .clip(shape)
    .background(color)
    .border(1.dp, Palette.GlassEdge, shape)

enum class Screen { Home, Apps, Search, Inputs, Settings, QuickPanel, NowPlaying }

private data class RailItem(val screen: Screen, val icon: ImageVector, val label: String)

/** Menú lateral de vidrio, igual en todas las vistas. */
@Composable
fun Rail(selected: Screen, onSelect: (Screen) -> Unit, modifier: Modifier = Modifier) {
    val top = listOf(
        RailItem(Screen.Search, Icons.Search, "Buscar"),
        RailItem(Screen.Home, Icons.Home, "Inicio"),
        RailItem(Screen.Apps, Icons.Grid, "Todas las apps"),
        RailItem(Screen.Inputs, Icons.Tv, "Entradas"),
        RailItem(Screen.Settings, Icons.Sliders, "Ajustes del launcher"),
    )
    val bottom = listOf(RailItem(Screen.QuickPanel, Icons.Panel, "Panel rápido"))
    Column(
        modifier
            .width(88.dp)
            .fillMaxHeight()
            .glass(RoundedCornerShape(44.dp))
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            top.forEach { RailButton(it, it.screen == selected) { onSelect(it.screen) } }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            bottom.forEach { RailButton(it, it.screen == selected) { onSelect(it.screen) } }
        }
    }
}

@Composable
private fun RailButton(item: RailItem, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val accent = LocalAccent.current
    // Solo el elemento enfocado lleva fondo blanco. La vista actual se marca con una barrita de acento.
    val f by animateFloatAsState(if (focused) 1f else 0f, Motion.fast, label = "railFocus")
    val sel by animateFloatAsState(if (selected && !focused) 1f else 0f, Motion.medium, label = "railSel")
    val fg = when {
        focused -> Palette.Ground
        selected -> Palette.Text
        else -> Palette.Icon
    }
    Box(contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier
                .offset(x = (-16).dp)
                .size(width = 4.dp, height = 24.dp)
                .graphicsLayer { alpha = sel; scaleY = 0.4f + 0.6f * sel }
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Box(
            Modifier
                .size(56.dp)
                .graphicsLayer { scaleX = 1f + 0.08f * f; scaleY = 1f + 0.08f * f }
                .clip(CircleShape)
                .background(Palette.Text.copy(alpha = f))
                .tvClickable(interaction, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, contentDescription = item.label, tint = fg, modifier = Modifier.size(26.dp))
        }
    }
}

/** Una app como círculo lleno con su ícono real; al enfocarse crece y gana un aro de acento. */
@Composable
fun AppCircle(
    app: AppInfo,
    accent: Color,
    size: Dp = 132.dp,
    showLabel: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val f by animateFloatAsState(if (focused) 1f else 0f, Motion.fast, label = "appFocus")
    val scale = 1f + 0.1f * f
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier
                .size(size)
                .scale(scale)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { if (it.isFocused) onFocused() }
                .focusRing(f, accent, pill = false)
                .clip(CircleShape)
                .tvClickable(interaction, onLongClick, onClick),
        ) {
            Image(app.icon, contentDescription = app.label, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        }
        if (showLabel) {
            Text(
                app.label,
                color = if (focused) Palette.Text else Palette.Muted,
                fontSize = if (focused) 20.sp else 18.sp,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(size + 24.dp),
            )
        }
    }
}

/** Botón en forma de píldora. [primary] = blanco con texto oscuro. */
@Composable
fun PillButton(
    text: String,
    icon: ImageVector? = null,
    primary: Boolean = false,
    accent: Color,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val f by animateFloatAsState(if (focused) 1f else 0f, Motion.fast, label = "pillFocus")
    val bg by animateColorAsState(if (primary || focused) Palette.Text else Color(0x24FFFFFF), Motion.colorFast, label = "pillBg")
    val fg = if (primary || focused) Palette.Ground else Palette.Text
    Row(
        modifier
            .graphicsLayer { scaleX = 1f + 0.05f * f; scaleY = 1f + 0.05f * f }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusRing(f, accent, pill = true)
            .clip(RoundedCornerShape(30.dp))
            .background(bg)
            .tvClickable(interaction, onClick = onClick)
            .padding(horizontal = 30.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
        Text(text, color = fg, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Botón redondo solo con ícono (pausa, siguiente…). */
@Composable
fun RoundButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    size: Dp = 60.dp,
    filled: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val f by animateFloatAsState(if (focused) 1f else 0f, Motion.fast, label = "roundFocus")
    val bg by animateColorAsState(
        when {
            filled -> Color.White
            focused -> Palette.Text
            else -> Color(0x24FFFFFF)
        }, Motion.colorFast, label = "roundBg",
    )
    val fg = if (filled || focused) Palette.Ground else Palette.Text
    Box(
        Modifier
            .size(size)
            .graphicsLayer { scaleX = 1f + 0.08f * f; scaleY = 1f + 0.08f * f }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusRing(f, accent, pill = false)
            .clip(CircleShape)
            .background(bg)
            .tvClickable(interaction, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(size * 0.36f))
    }
}

package dev.tvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvlauncher.data.AppInfo

/** Fondo común de las vistas con menú: oscuro con luces difuminadas. */
@Composable
fun AmbientBackground(accent: Color, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Ground)
            .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent), Offset(200f, 0f), 1000f))
            .background(Brush.radialGradient(listOf(Palette.Deep.copy(alpha = 0.8f), Color.Transparent), Offset(1800f, 1050f), 1000f))
    ) { content() }
}

@Composable
fun AppsScreen(
    apps: List<AppInfo>,
    hiddenApps: List<AppInfo>,
    favorites: List<String>,
    accent: Color,
    onOpen: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onHide: (String) -> Unit,
    onUnhide: (String) -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    val context = LocalContext.current
    var menuFor by remember { mutableStateOf<AppInfo?>(null) }
    var showHidden by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    val menuFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    LaunchedEffect(menuFor) { if (menuFor != null) runCatching { menuFocus.requestFocus() } }

    AmbientBackground(accent) {
        Rail(Screen.Apps, onNavigate, Modifier.padding(start = 96.dp, top = 54.dp, bottom = 54.dp))
        Column(
            Modifier.fillMaxSize().padding(start = 240.dp, end = 96.dp, top = 54.dp, bottom = 54.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Todas las apps", color = Palette.Text, fontSize = 56.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1.5).sp)
                Text(
                    "${apps.size} apps" + if (hiddenApps.isNotEmpty()) " · ${hiddenApps.size} ocultas" else "",
                    color = Palette.Muted, fontSize = 20.sp,
                )
            }
            val list = if (showHidden) hiddenApps else apps
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(36.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                items(list, key = { it.packageName }) { app ->
                    Box(contentAlignment = Alignment.Center) {
                        AppCircle(
                            app = app,
                            accent = accent,
                            focusRequester = if (app == list.firstOrNull()) first else null,
                            onLongClick = { menuFor = app },
                            onClick = { if (showHidden) onUnhide(app.packageName) else onOpen(app.packageName) },
                        )
                    }
                }
            }
            val target = menuFor
            Row(
                Modifier
                    .glass(RoundedCornerShape(36.dp), Color(0x0FFFFFFF))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (target == null) {
                    Text(
                        if (showHidden) "OK sobre una app oculta para volver a mostrarla" else "Mantén OK sobre una app para ver opciones",
                        color = Palette.Muted, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    if (hiddenApps.isNotEmpty() || showHidden) {
                        PillButton(if (showHidden) "Ver todas" else "Ver ocultas", accent = accent) { showHidden = !showHidden }
                    }
                } else {
                    val isFav = target.packageName in favorites
                    Text(target.label, color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp))
                    PillButton("Abrir", primary = true, accent = accent, focusRequester = menuFocus) { onOpen(target.packageName); menuFor = null }
                    PillButton(if (isFav) "Quitar de Favoritas" else "Agregar a Favoritas", accent = accent) { onToggleFavorite(target.packageName); menuFor = null }
                    if (isFav) {
                        PillButton("Mover ←", accent = accent) { onMove(target.packageName, -1) }
                        PillButton("Mover →", accent = accent) { onMove(target.packageName, 1) }
                    }
                    PillButton("Ocultar", accent = accent) { onHide(target.packageName); menuFor = null }
                    PillButton("Información", accent = accent) { SystemActions.appDetails(context, target.packageName); menuFor = null }
                    PillButton("Cerrar", accent = accent) { menuFor = null; runCatching { first.requestFocus() } }
                }
            }
        }
    }
}

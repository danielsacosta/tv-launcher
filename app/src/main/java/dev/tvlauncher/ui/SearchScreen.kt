package dev.tvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tvlauncher.data.AppInfo

@Composable
fun SearchScreen(
    apps: List<AppInfo>,
    accent: Color,
    onOpen: (String) -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var fieldFocused by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fieldFocus.requestFocus() } }

    val q = query.trim()
    val matches = if (q.isEmpty()) emptyList() else apps.filter { it.label.contains(q, ignoreCase = true) }

    AmbientBackground(accent) {
        Rail(Screen.Search, onNavigate, Modifier.padding(start = Layout.RailStart, top = 54.dp, bottom = 54.dp))
        Column(
            Modifier.fillMaxSize().padding(start = Layout.ContentStart, end = 240.dp, top = 120.dp),
            verticalArrangement = Arrangement.spacedBy(56.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                MicButton(accent) { SystemActions.assistant(context) }
                Row(
                    Modifier
                        .weight(1f)
                        .height(96.dp)
                        .clip(RoundedCornerShape(48.dp))
                        .background(Color(0x14FFFFFF))
                        .border(if (fieldFocused) 3.dp else 1.dp, if (fieldFocused) accent else Color(0x1FFFFFFF), RoundedCornerShape(48.dp))
                        .padding(horizontal = 36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Icon(Icons.Search, null, tint = Palette.Muted, modifier = Modifier.size(30.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text("Busca apps, películas o pregunta a Google", color = Palette.Muted, fontSize = 34.sp)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Palette.Text, fontSize = 40.sp),
                            cursorBrush = SolidColor(accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (q.isNotEmpty()) SystemActions.googleSearch(context, q)
                            }),
                            modifier = Modifier
                                .focusRequester(fieldFocus)
                                .onFocusChanged { fieldFocused = it.isFocused },
                        )
                    }
                }
            }

            if (q.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    SectionTitle(if (matches.isEmpty()) "Ninguna app coincide" else "Apps")
                    Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                        matches.take(8).forEach { app -> AppCircle(app, accent) { onOpen(app.packageName) } }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    SectionTitle("Buscar «$q» en")
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PillButton("Google", icon = Icons.Sparkle, accent = accent) { SystemActions.googleSearch(context, q) }
                        PillButton("YouTube", icon = Icons.Play, accent = accent) { SystemActions.youtubeSearch(context, q) }
                        PillButton("Play Store", icon = Icons.Grid, accent = accent) { SystemActions.playStoreSearch(context, q) }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text.uppercase(SpanishCo), color = Palette.Muted, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
}

@Composable
private fun MicButton(accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        Modifier
            .size(96.dp)
            .scale(if (focused) 1.08f else 1f)
            .focusRing(focused, accent.copy(alpha = 0.6f), pill = false)
            .clip(CircleShape)
            .background(accent)
            .tvClickable(interaction, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Mic, "Hablar con Google", tint = Palette.Ground, modifier = Modifier.size(38.dp))
    }
}

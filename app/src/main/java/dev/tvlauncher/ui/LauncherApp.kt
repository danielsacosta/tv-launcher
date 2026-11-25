package dev.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import dev.tvlauncher.data.AppRepository
import dev.tvlauncher.data.KnownApps
import dev.tvlauncher.data.Prefs
import dev.tvlauncher.media.MediaRepo
import kotlinx.coroutines.flow.StateFlow

/**
 * Raíz del launcher. Todo el diseño se hizo a 1920×1080: aquí se ajusta la densidad para que
 * 1 dp del código = 1 px del diseño, sin importar la resolución real del TV.
 */
@Composable
fun LauncherApp(repo: AppRepository, prefs: Prefs, homePresses: StateFlow<Int>) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val base = LocalDensity.current
        val scale = with(base) { constraints.maxWidth.toFloat() / 1920f }
        CompositionLocalProvider(LocalDensity provides Density(density = scale, fontScale = 1f)) {
            LauncherContent(repo, prefs, homePresses)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LauncherContent(repo: AppRepository, prefs: Prefs, homePresses: StateFlow<Int>) {
    val apps by repo.apps.collectAsState()
    val storedFavorites by prefs.favorites.collectAsState()
    val hidden by prefs.hidden.collectAsState()
    val settings by prefs.settings.collectAsState()
    val nowPlaying by MediaRepo.nowPlaying.collectAsState()
    val recent by MediaRepo.recent.collectAsState()
    val hasMediaAccess by MediaRepo.hasAccess.collectAsState()
    val home by homePresses.collectAsState()

    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var focusedPkg by rememberSaveable { mutableStateOf<String?>(null) }
    var focusTrigger by remember { mutableIntStateOf(0) }
    val accent = Color(settings.accent)

    val visible = apps.filter { it.packageName !in hidden }
    val favoriteIds = storedFavorites ?: KnownApps.defaultFavorites(apps.map { it.packageName })
    val favorites = favoriteIds.mapNotNull { id -> visible.firstOrNull { it.packageName == id } }.take(7)

    // El botón Home del control siempre vuelve al inicio.
    LaunchedEffect(home) { screen = Screen.Home }
    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    // Cada vez que se vuelve al inicio (Home, Atrás o al regresar de una app) se recoloca el foco.
    LaunchedEffect(screen, home, favorites.isNotEmpty()) {
        if (screen == Screen.Home) focusTrigger++
    }

    fun open(pkg: String) {
        MediaRepo.lastLaunched = pkg
        repo.launch(pkg)
    }

    fun editFavorites(transform: (MutableList<String>) -> Unit) {
        val list = favoriteIds.toMutableList()
        transform(list)
        prefs.setFavorites(list)
    }

    CompositionLocalProvider(LocalAccent provides accent) {
        // El panel rápido se dibuja encima del inicio; las demás vistas se reemplazan con un fundido suave.
        val base = if (screen == Screen.QuickPanel) Screen.Home else screen
        AnimatedContent(
            targetState = base,
            transitionSpec = {
                (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                    scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.985f)) togetherWith
                    fadeOut(tween(140))
            },
            label = "screens",
        ) { s ->
            when (s) {
                Screen.Home, Screen.QuickPanel -> HomeScreen(
                    favorites = favorites,
                    allApps = apps,
                    nowPlaying = nowPlaying,
                    recent = recent,
                    settings = settings,
                    focusedPkg = focusedPkg,
                    onFocusApp = { focusedPkg = it },
                    focusTrigger = focusTrigger,
                    onOpenApp = ::open,
                    onNavigate = { screen = it },
                )
                Screen.Apps -> AppsScreen(
                    apps = visible,
                    hiddenApps = apps.filter { it.packageName in hidden },
                    favorites = favoriteIds,
                    accent = accent,
                    onOpen = ::open,
                    onToggleFavorite = { pkg -> editFavorites { if (pkg in it) it.remove(pkg) else it.add(pkg) } },
                    onMove = { pkg, dir ->
                        editFavorites {
                            val i = it.indexOf(pkg)
                            val j = (i + dir).coerceIn(0, it.lastIndex)
                            if (i >= 0 && i != j) { it.removeAt(i); it.add(j, pkg) }
                        }
                    },
                    onHide = { pkg -> prefs.setHidden(hidden + pkg) },
                    onUnhide = { pkg -> prefs.setHidden(hidden - pkg) },
                    onNavigate = { screen = it },
                )
                Screen.Search -> SearchScreen(visible, accent, ::open) { screen = it }
                Screen.Inputs -> InputsScreen(accent) { screen = it }
                Screen.Settings -> SettingsScreen(settings, hasMediaAccess, prefs::updateSettings) { screen = it }
                Screen.NowPlaying -> NowPlayingScreen(
                    np = nowPlaying,
                    app = nowPlaying?.let { np -> apps.firstOrNull { it.packageName == np.owner } },
                    accent = accent,
                ) { screen = Screen.Home }
            }
        }
        AnimatedVisibility(
            visible = screen == Screen.QuickPanel,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160)),
        ) {
            QuickPanel(
                settings = settings,
                panelModifier = Modifier.animateEnterExit(
                    enter = slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 4 },
                    exit = slideOutHorizontally(tween(180)) { it / 4 },
                ),
            ) { screen = Screen.Home }
        }
    }
}

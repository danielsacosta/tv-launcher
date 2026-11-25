package dev.tvlauncher.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Ajustes del launcher que el usuario puede cambiar. */
data class LauncherSettings(
    val accent: Long = 0xFF7AA2FF,
    val showNowPlaying: Boolean = true,
    val abstractWallpaper: Boolean = true,
    val clock24h: Boolean = true,
)

/** Guarda favoritas, apps ocultas y ajustes en SharedPreferences. */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(readList(KEY_FAVORITES))
    val favorites: StateFlow<List<String>?> = _favorites.asStateFlow()

    private val _hidden = MutableStateFlow(readList(KEY_HIDDEN).orEmpty().toSet())
    val hidden: StateFlow<Set<String>> = _hidden.asStateFlow()

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    /** null = el usuario nunca eligió favoritas; se usan las predeterminadas. */
    private fun readList(key: String): List<String>? {
        if (!sp.contains(key)) return null
        return sp.getString(key, "").orEmpty().split(',').filter { it.isNotBlank() }
    }

    private fun readSettings() = LauncherSettings(
        accent = sp.getLong("accent", 0xFF7AA2FF),
        showNowPlaying = sp.getBoolean("showNowPlaying", true),
        abstractWallpaper = sp.getBoolean("abstractWallpaper", true),
        clock24h = sp.getBoolean("clock24h", true),
    )

    fun setFavorites(list: List<String>) {
        sp.edit().putString(KEY_FAVORITES, list.joinToString(",")).apply()
        _favorites.value = list
    }

    fun setHidden(set: Set<String>) {
        sp.edit().putString(KEY_HIDDEN, set.joinToString(",")).apply()
        _hidden.value = set
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        val s = transform(_settings.value)
        sp.edit()
            .putLong("accent", s.accent)
            .putBoolean("showNowPlaying", s.showNowPlaying)
            .putBoolean("abstractWallpaper", s.abstractWallpaper)
            .putBoolean("clock24h", s.clock24h)
            .apply()
        _settings.value = s
    }

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_HIDDEN = "hidden"
    }
}

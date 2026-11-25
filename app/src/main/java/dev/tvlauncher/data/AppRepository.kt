package dev.tvlauncher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Una app instalada, lista para mostrarse como círculo. */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
    /** Color dominante del ícono (ARGB), para teñir el hero. */
    val tint: Int,
)

/** Lee las apps de TV instaladas y se actualiza cuando se instala o desinstala algo. */
class AppRepository(private val context: Context, private val scope: CoroutineScope) {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = refresh()
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        refresh()
    }

    fun stop() = runCatching { context.unregisterReceiver(receiver) }

    fun refresh() {
        scope.launch {
            _apps.value = withContext(Dispatchers.Default) { load() }
        }
    }

    private fun load(): List<AppInfo> {
        val pm = context.packageManager
        val leanback = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val packages = pm.queryIntentActivities(leanback, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName }

        return packages.mapNotNull { pkg ->
            runCatching {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = KnownApps.labelFor(pkg) ?: pm.getApplicationLabel(appInfo).toString()
                val bitmap = IconRenderer.render(pkg, pm.getApplicationIcon(appInfo))
                AppInfo(pkg, label, bitmap.asImageBitmap(), IconRenderer.dominantColor(bitmap))
            }.getOrNull()
        }.sortedWith(compareBy({ KnownApps.order(it.packageName) }, { it.label.lowercase() }))
    }

    fun launch(packageName: String): Boolean {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}

/** Nombres y orden por defecto de las apps más comunes. */
object KnownApps {
    const val SPOTIFY = "com.spotify.tv.android"
    const val YT_MUSIC = "com.google.android.youtube.tvmusic"

    private val defaults = listOf(
        SPOTIFY,
        "com.google.android.youtube.tv",
        "com.netflix.ninja",
        "com.disney.disneyplus",
        "com.amazon.amazonvideo.livingroom",
        "com.wbd.stream",
        YT_MUSIC,
    )

    val musicApps = setOf(SPOTIFY, YT_MUSIC)

    private val labels = mapOf(
        "com.wbd.stream" to "HBO Max",
        "com.caracol.streaming" to "Ditu",
        "com.mediatek.tv.oneworld.tvcenter" to "TV en vivo",
        "com.gdch.wwtv.mediaplayer" to "Reproductor",
        "com.amazon.amazonvideo.livingroom" to "Prime Video",
    )

    fun labelFor(pkg: String) = labels[pkg]

    fun order(pkg: String): Int = defaults.indexOf(pkg).let { if (it < 0) Int.MAX_VALUE else it }

    fun defaultFavorites(installed: List<String>) = defaults.filter { it in installed }
}

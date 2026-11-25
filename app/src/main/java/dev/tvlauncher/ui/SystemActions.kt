package dev.tvlauncher.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Accesos a las pantallas del sistema que siguen disponibles sin Google TV.
 * Probados en el TV con `cmd package query-activities`: Asistente (katniss), Ajustes,
 * Wi-Fi, Imagen (MediaTek), Sonido, Accesibilidad, Cuentas y selector de entradas.
 */
object SystemActions {

    private fun Context.tryStart(vararg intents: Intent): Boolean {
        for (i in intents) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { startActivity(i) }.isSuccess) return true
        }
        return false
    }

    fun settings(c: Context) = c.tryStart(Intent(Settings.ACTION_SETTINGS))
    fun wifi(c: Context) = c.tryStart(Intent(Settings.ACTION_WIFI_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    fun bluetooth(c: Context) = c.tryStart(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    fun picture(c: Context) = c.tryStart(Intent(Settings.ACTION_DISPLAY_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    fun sound(c: Context) = c.tryStart(Intent(Settings.ACTION_SOUND_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    fun accessibility(c: Context) = c.tryStart(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    fun accounts(c: Context) = c.tryStart(Intent(Settings.ACTION_SYNC_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    fun dreamSettings(c: Context) = c.tryStart(Intent(Settings.ACTION_DREAM_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    fun appDetails(c: Context, pkg: String) =
        c.tryStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))

    /** Selector de entradas del sistema (HDMI, antena…). */
    fun inputPicker(c: Context) = c.tryStart(Intent("com.android.tv.action.VIEW_INPUTS"))

    /** Asistente de Google (voz). Es lo mismo que hace el botón del micrófono del control. */
    fun assistant(c: Context) = c.tryStart(Intent(Intent.ACTION_ASSIST), Intent(Intent.ACTION_SEARCH_LONG_PRESS))

    /** Búsqueda de Google con texto. */
    fun googleSearch(c: Context, query: String) = c.tryStart(
        Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query),
        Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH).putExtra(SearchManager.QUERY, query),
        Intent(Intent.ACTION_ASSIST),
    )

    fun youtubeSearch(c: Context, query: String) = c.tryStart(
        Intent(Intent.ACTION_SEARCH).setPackage("com.google.android.youtube.tv").putExtra(SearchManager.QUERY, query),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}"))
            .setPackage("com.google.android.youtube.tv"),
    )

    fun playStoreSearch(c: Context, query: String) = c.tryStart(
        Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(query)}&c=apps")),
    )

    /** Inicia el protector de pantalla elegido en el sistema. */
    fun startDream(c: Context) = c.tryStart(
        Intent(Intent.ACTION_MAIN).setClassName("com.android.systemui", "com.android.systemui.Somnambulator"),
        Intent(Settings.ACTION_DREAM_SETTINGS),
    )
}

package dev.tvlauncher.media

import android.service.notification.NotificationListenerService

/**
 * Servicio vacío: su único papel es darle al launcher permiso para leer las sesiones
 * de reproducción. Se activa una vez con:
 * adb shell cmd notification allow_listener dev.tvlauncher/dev.tvlauncher.media.MediaListenerService
 */
class MediaListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        MediaRepo.attach(this)
    }
}

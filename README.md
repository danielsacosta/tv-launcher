# tv-launcher

Launcher propio para Android TV / Google TV: oscuro, estilo glass, sin anuncios.
Hecho en Kotlin con Jetpack Compose, pensado para 1920×1080 y control remoto.

## Qué trae

- **Inicio:** hero sin caja que cambia según la app enfocada. Si algo suena, muestra la
  portada real, canción, artista y progreso, con el fondo teñido por la portada.
- **Favoritas** en círculos con el ícono real de cada app (los íconos adaptativos se recortan
  en círculo y los planos se ajustan para llenar el círculo).
- **Todas las apps:** mantener OK (o la tecla Menú) abre opciones: favoritas, mover, ocultar, información.
- **Buscar:** apps al instante, búsqueda en Google, YouTube y Play Store, y micrófono (Asistente de Google).
- **Entradas:** HDMI y TV en vivo según el sistema, con estado de señal.
- **Panel rápido:** Wi-Fi, Bluetooth, imagen, sonido, entradas, accesibilidad, protector, perfil y ajustes del dispositivo.
- **Reproductor** a pantalla completa con controles, y **protector de pantalla** propio (reloj + lo que suena).

## Compilar e instalar

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Permiso para leer lo que suena (una sola vez):

```bash
adb shell cmd notification allow_listener dev.tvlauncher/dev.tvlauncher.media.MediaListenerService
```

Abrirlo sin cambiar el inicio del TV:

```bash
adb shell am start -n dev.tvlauncher/.MainActivity
```

Usarlo como inicio (botón Home del control):

```bash
adb shell cmd package set-home-activity dev.tvlauncher/.MainActivity
```

En Google TV eso no basta: su inicio de sistema se impone al botón Home. Para que Home abra este
launcher hay que desactivar el inicio de Google (no se borra, solo se desactiva):

```bash
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
```

```bash
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

Volver al inicio de Google TV:

```bash
adb shell pm enable com.google.android.apps.tv.launcherx
```

```bash
adb shell pm enable com.google.android.tungsten.setupwraith
```

```bash
adb shell cmd package set-home-activity com.google.android.apps.tv.launcherx/.home.HomeActivity
```

No apagues la depuración inalámbrica: es la vía para revertirlo si algo falla.

## Límites conocidos

- Android no deja que un launcher de terceros lea "Seguir viendo" ni las recomendaciones de otras apps.
  Para destacados de Netflix, Disney+, etc. el plan es usar TMDB (requiere una clave gratuita).
- El fondo del Panel rápido es casi opaco: Android no permite desenfocar lo que hay detrás de una ventana de app.

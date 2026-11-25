package dev.tvlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import dev.tvlauncher.data.AppRepository
import dev.tvlauncher.data.Prefs
import dev.tvlauncher.media.MediaRepo
import dev.tvlauncher.ui.LauncherApp
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private lateinit var repo: AppRepository
    private lateinit var prefs: Prefs
    private val homePresses = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        repo = AppRepository(applicationContext, lifecycleScope)
        repo.start()
        setContent { LauncherApp(repo, prefs, homePresses) }
    }

    override fun onResume() {
        super.onResume()
        MediaRepo.attach(this)
        // Al regresar de otra app, el inicio vuelve a poner el foco donde corresponde.
        homePresses.value++
    }

    /** Llega cuando se pulsa Home estando ya en el launcher. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homePresses.value++
    }

    override fun onDestroy() {
        repo.stop()
        super.onDestroy()
    }
}

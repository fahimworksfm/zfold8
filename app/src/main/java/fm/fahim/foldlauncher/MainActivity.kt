package fm.fahim.foldlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import fm.fahim.foldlauncher.ui.LauncherScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repo: AppRepository
    private lateinit var usage: UsageModel

    private var apps by mutableStateOf<List<AppEntry>>(emptyList())
    private var fold by mutableStateOf<FoldingFeature?>(null)
    private var hidden by mutableStateOf<Set<String>>(emptySet())
    private var learnTick by mutableStateOf(0) // bumps after every launch so predictions refresh
    private var resetTick by mutableStateOf(0) // bumps when Home is pressed while already home
    private var stopWatching: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repo = AppRepository(this)
        usage = UsageModel(this)
        hidden = usage.hiddenKeys.toSet()

        reloadApps()
        stopWatching = repo.watch { reloadApps() }

        // Follow the hinge: closed, open flat, or half-open on a table.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { info -> fold = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull() }
            }
        }

        // A launcher never closes on Back; it just clears the search.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { resetTick++ }
        })

        setContent {
            LauncherScreen(
                apps = apps,
                hidden = hidden,
                fold = fold,
                resetTick = resetTick,
                scores = { posture -> learnTick.let { usage.scores(posture) } },
                counts = { learnTick.let { usage.counts() } },
                onLaunch = { app, posture ->
                    runCatching { repo.launch(app) }
                    usage.record(app.key, posture)
                    learnTick++
                },
                onAppInfo = { runCatching { repo.openAppInfo(it) } },
                onUninstall = { app ->
                    runCatching {
                        startActivity(Intent(Intent.ACTION_DELETE, "package:${app.component.packageName}".toUri()))
                    }
                },
                onSetHidden = { app, h ->
                    usage.setHidden(app.key, h)
                    hidden = usage.hiddenKeys.toSet()
                },
                onForget = { app ->
                    usage.forget(app.key)
                    learnTick++
                },
                isWork = repo::isWorkProfile,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        resetTick++
    }

    override fun onDestroy() {
        stopWatching?.invoke()
        super.onDestroy()
    }

    private fun reloadApps() {
        lifecycleScope.launch { apps = repo.load() }
    }

    private fun String.toUri() = android.net.Uri.parse(this)
}

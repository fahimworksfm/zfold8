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
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import fm.fahim.foldlauncher.ui.AppActions
import fm.fahim.foldlauncher.ui.GarageActions
import fm.fahim.foldlauncher.ui.LauncherInputs
import fm.fahim.foldlauncher.ui.LauncherScreen
import java.time.ZoneId
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
    private lateinit var projectStore: ProjectStore
    private var projects by mutableStateOf<List<Project>>(emptyList())
    private val prefs by lazy { getSharedPreferences("fold", MODE_PRIVATE) }
    private var zoneIndex by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repo = AppRepository(this)
        usage = UsageModel(this)
        hidden = usage.hiddenKeys.toSet()
        projectStore = ProjectStore(this)
        projects = projectStore.load()
        zoneIndex = prefs.getInt("zone", 0).coerceIn(0, ZONES.size - 1)

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
                LauncherInputs(
                    apps = apps,
                    hidden = hidden,
                    projects = projects,
                    fold = fold,
                    resetTick = resetTick,
                    secondZone = ZONES[zoneIndex]?.let(ZoneId::of),
                    onCycleZone = {
                        zoneIndex = (zoneIndex + 1) % ZONES.size
                        prefs.edit().putInt("zone", zoneIndex).apply()
                    },
                    scores = { posture -> learnTick.let { usage.scores(posture) } },
                    counts = { learnTick.let { usage.counts() } },
                    onLaunch = { app, posture ->
                        runCatching { repo.launch(app) }
                        usage.record(app.key, posture)
                        learnTick++
                    },
                    appActions = { launch ->
                        AppActions(
                            launch = launch,
                            info = { runCatching { repo.openAppInfo(it) } },
                            uninstall = { app ->
                                runCatching { startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.component.packageName}"))) }
                            },
                            setHidden = { app, h -> usage.setHidden(app.key, h); hidden = usage.hiddenKeys.toSet() },
                            forget = { app -> usage.forget(app.key); learnTick++ },
                            isHidden = { it.key in hidden },
                            isWork = repo::isWorkProfile,
                        )
                    },
                    garage = GarageActions(
                        openUrl = ::openUrl,
                        askClaude = ::askClaude,
                        save = { p ->
                            val i = projects.indexOfFirst { it.id == p.id }
                            projects = if (i >= 0) projects.toMutableList().also { it[i] = p } else projects + p
                            projectStore.save(projects)
                        },
                        delete = { p ->
                            projects = projects.filterNot { it.id == p.id }
                            projectStore.save(projects)
                        },
                    ),
                )
            )
        }
    }

    private fun openUrl(url: String) {
        val full = if (url.startsWith("http")) url else "https://$url"
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(full))) }
            .onFailure { Toast.makeText(this, "Couldn't open $full", Toast.LENGTH_SHORT).show() }
    }

    /** Hands the project to the Claude app, with its repo, so a session can start from it. */
    private fun askClaude(p: Project) {
        val text = buildString {
            append("Let's work on ${p.name}.")
            if (p.repo.isNotBlank()) append(" Repo: ${p.repo}.")
            if (p.live.isNotBlank()) append(" Live: ${p.live}.")
            if (p.host.isNotBlank()) append(" Hosted on ${p.host}.")
        }
        val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text).setPackage(CLAUDE)
        val opened = runCatching { startActivity(share) }.isSuccess ||
            runCatching { startActivity(packageManager.getLaunchIntentForPackage(CLAUDE)!!) }.isSuccess
        if (!opened) openUrl("https://claude.ai/code")
        else {
            getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("project", text))
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

    companion object {
        private const val CLAUDE = "com.anthropic.claude"

        /** Tap the small line under the clock to step through these. Dhaka first, then off. */
        private val ZONES = listOf("Asia/Dhaka", "Europe/London", "Asia/Singapore", "America/Los_Angeles", null)
    }
}

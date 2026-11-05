package fm.fahim.foldlauncher.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.layout.FoldingFeature
import fm.fahim.foldlauncher.AppEntry
import fm.fahim.foldlauncher.Posture
import fm.fahim.foldlauncher.Search
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFFF2F2F2)
private val Muted = Color(0xFFA8A8A8)
private val Accent = Color(0xFF9FD8FF)
private val Scrim = Color(0x8C000000)
private val Field = Color(0x26FFFFFF)

/** Everything an app cell can do, passed down once instead of as six lambdas. */
class AppActions(
    val launch: (AppEntry) -> Unit,
    val info: (AppEntry) -> Unit,
    val uninstall: (AppEntry) -> Unit,
    val setHidden: (AppEntry, Boolean) -> Unit,
    val forget: (AppEntry) -> Unit,
    val isHidden: (AppEntry) -> Boolean,
    val isWork: (AppEntry) -> Boolean,
)

@Composable
fun LauncherScreen(
    apps: List<AppEntry>,
    hidden: Set<String>,
    fold: FoldingFeature?,
    resetTick: Int,
    scores: (Posture) -> Map<String, Double>,
    counts: () -> Map<String, Int>,
    onLaunch: (AppEntry, Posture) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onUninstall: (AppEntry) -> Unit,
    onSetHidden: (AppEntry, Boolean) -> Unit,
    onForget: (AppEntry) -> Unit,
    isWork: (AppEntry) -> Boolean,
) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val posture = when {
        fold?.state == FoldingFeature.State.HALF_OPENED &&
            fold.orientation == FoldingFeature.Orientation.HORIZONTAL -> Posture.TABLETOP
        widthDp >= 600 -> Posture.OPEN
        else -> Posture.COVER
    }

    var query by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    fun reset() {
        query = ""; showAll = false
        focus.clearFocus(); keyboard?.hide()
    }
    LaunchedEffect(resetTick) { if (resetTick > 0) reset() }

    val visible = apps.filter { it.key !in hidden }
    val scoreMap = scores(posture)
    val predicted = visible
        .filter { (scoreMap[it.key] ?: 0.0) > 0.0 }
        .sortedByDescending { scoreMap[it.key] }
    // Search covers hidden apps too, so hiding never makes an app unreachable.
    val results = if (query.isBlank()) emptyList() else Search.rank(query, apps, counts())

    val actions = AppActions(
        launch = { app -> onLaunch(app, posture); reset() },
        info = onAppInfo,
        uninstall = onUninstall,
        setHidden = onSetHidden,
        forget = onForget,
        isHidden = { it.key in hidden },
        isWork = isWork,
    )
    val search: @Composable (Modifier) -> Unit = { m ->
        SearchField(query, { query = it }, onGo = { results.firstOrNull()?.let(actions.launch) }, modifier = m)
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Scrim)
                .safeDrawingPadding()
                .imePadding()
        ) {
            AnimatedContent(targetState = posture, label = "posture") { p ->
                when (p) {
                    Posture.COVER -> CoverLayout(query, results, predicted, visible, showAll, { showAll = it }, actions, search)
                    Posture.OPEN -> OpenLayout(query, results, predicted, visible, actions, search)
                    Posture.TABLETOP -> TabletopLayout(query, results, predicted, actions, search)
                }
            }
        }
    }
}

/** Folded: a narrow, one-handed screen. Clock, your next likely apps, search at thumb height. */
@Composable
private fun CoverLayout(
    query: String,
    results: List<AppEntry>,
    predicted: List<AppEntry>,
    all: List<AppEntry>,
    showAll: Boolean,
    setShowAll: (Boolean) -> Unit,
    actions: AppActions,
    search: @Composable (Modifier) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        when {
            query.isNotBlank() -> AppGrid(results, columns = 4, actions = actions, modifier = Modifier.weight(1f))
            showAll -> {
                Header("All apps", trailing = "Close") { setShowAll(false) }
                AppGrid(all, columns = 4, actions = actions, modifier = Modifier.weight(1f))
            }
            else -> {
                Spacer(Modifier.height(32.dp))
                Clock(big = true)
                Spacer(Modifier.weight(1f))
                Header("Right now", trailing = "All apps") { setShowAll(true) }
                if (predicted.isEmpty()) LearningHint()
                else AppGrid(predicted.take(8), columns = 4, actions = actions, scroll = false)
            }
        }
        search(Modifier.fillMaxWidth().padding(vertical = 12.dp))
    }
}

/** Unfolded: two panes. Left is "now" (time and predictions), right is every app plus search. */
@Composable
private fun OpenLayout(
    query: String,
    results: List<AppEntry>,
    predicted: List<AppEntry>,
    all: List<AppEntry>,
    actions: AppActions,
    search: @Composable (Modifier) -> Unit,
) {
    Row(Modifier.fillMaxSize().padding(16.dp)) {
        Column(Modifier.weight(0.42f).padding(end = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Clock(big = true)
            Spacer(Modifier.height(40.dp))
            Header("Right now")
            if (predicted.isEmpty()) LearningHint()
            else AppGrid(predicted.take(12), columns = 3, actions = actions, modifier = Modifier.weight(1f))
        }
        Column(Modifier.weight(0.58f)) {
            search(Modifier.fillMaxWidth().padding(bottom = 12.dp))
            if (query.isNotBlank()) AppGrid(results, columns = 5, actions = actions, modifier = Modifier.weight(1f))
            else AppGrid(all, columns = 5, actions = actions, modifier = Modifier.weight(1f))
        }
    }
}

/** Half-open on a table: the top half is a glanceable clock, the bottom half is for your thumbs. */
@Composable
private fun TabletopLayout(
    query: String,
    results: List<AppEntry>,
    predicted: List<AppEntry>,
    actions: AppActions,
    search: @Composable (Modifier) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Clock(big = true, center = true)
        }
        Column(Modifier.weight(1f).padding(top = 16.dp)) {
            search(Modifier.fillMaxWidth().padding(bottom = 8.dp))
            val list = if (query.isNotBlank()) results else predicted
            if (list.isEmpty() && query.isBlank()) LearningHint()
            else AppGrid(list, columns = 6, actions = actions, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Clock(big: Boolean, center: Boolean = false) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000) // wake exactly on the next minute
        }
    }
    val battery = remember(now) {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { i ->
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val charging = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
            if (level >= 0) "${level * 100 / scale}%" + if (charging) " charging" else "" else null
        }
    }
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val time = SimpleDateFormat(if (is24h) "HH:mm" else "h:mm", Locale.getDefault()).format(Date(now))
    val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now))
    val align = if (center) TextAlign.Center else TextAlign.Start

    Column(horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start) {
        Text(time, color = Ink, fontSize = if (big) 72.sp else 40.sp, fontWeight = FontWeight.Light, textAlign = align)
        Text(listOfNotNull(date, battery).joinToString("  ·  "), color = Muted, fontSize = 15.sp, textAlign = align)
    }
}

@Composable
private fun Header(title: String, trailing: String? = null, onTrailing: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), color = Muted, fontSize = 12.sp, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(
                trailing, color = Accent, fontSize = 14.sp,
                modifier = Modifier
                    .combinedClickable(onClick = onTrailing)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun LearningHint() {
    Text(
        "Open a few apps and this fills in with the ones you use at this time of day.",
        color = Muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, onGo: () -> Unit, modifier: Modifier) {
    val focusRequester = remember { FocusRequester() }
    BasicTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = Ink, fontSize = 17.sp),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        modifier = modifier.focusRequester(focusRequester),
        decorationBox = { inner ->
            Box(
                Modifier
                    .background(Field, RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                if (query.isEmpty()) Text("Type an app name, Go opens the top match", color = Muted, fontSize = 17.sp)
                inner()
            }
        }
    )
}

@Composable
private fun AppGrid(
    apps: List<AppEntry>,
    columns: Int,
    actions: AppActions,
    modifier: Modifier = Modifier,
    scroll: Boolean = true,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        userScrollEnabled = scroll,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(apps, key = { it.key }) { AppCell(it, actions) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCell(app: AppEntry, actions: AppActions) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { actions.launch(app) }, onLongClick = { menu = true })
                .padding(vertical = 8.dp)
        ) {
            Image(app.icon, contentDescription = null, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                app.label, color = Ink, fontSize = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.width(80.dp)
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            val hidden = actions.isHidden(app)
            DropdownMenuItem(text = { Text("App info") }, onClick = { menu = false; actions.info(app) })
            DropdownMenuItem(
                text = { Text(if (hidden) "Unhide" else "Hide from home") },
                onClick = { menu = false; actions.setHidden(app, !hidden) }
            )
            DropdownMenuItem(text = { Text("Forget my usage") }, onClick = { menu = false; actions.forget(app) })
            if (!actions.isWork(app)) {
                DropdownMenuItem(text = { Text("Uninstall") }, onClick = { menu = false; actions.uninstall(app) })
            }
        }
    }
}

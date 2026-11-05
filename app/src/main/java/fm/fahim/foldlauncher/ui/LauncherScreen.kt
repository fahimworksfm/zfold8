package fm.fahim.foldlauncher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.layout.FoldingFeature
import fm.fahim.foldlauncher.AppEntry
import fm.fahim.foldlauncher.Posture
import fm.fahim.foldlauncher.Project
import fm.fahim.foldlauncher.Search
import java.time.ZoneId

private val Ink = Cream
private val Muted = Dim
private val Accent = Gold
private val Field = Glass

/**
 * Apps for building and running the studio, in the order they appear in the Studio row.
 * Only the ones installed on the phone are shown.
 */
private val STUDIO_PACKAGES = listOf(
    "com.anthropic.claude",
    "com.github.android",
    "com.google.android.apps.bard",
    "com.whatsapp.w4b",
    "com.whatsapp",
    "org.telegram.messenger",
    "com.stripe.android.dashboard",
    "com.android.chrome",
    "com.termux",
)

/** All the state and callbacks the screen needs, gathered in one place. */
class LauncherInputs(
    val apps: List<AppEntry>,
    val hidden: Set<String>,
    val projects: List<Project>,
    val fold: FoldingFeature?,
    val resetTick: Int,
    val secondZone: ZoneId?,
    val onCycleZone: () -> Unit,
    val scores: (Posture) -> Map<String, Double>,
    val counts: () -> Map<String, Int>,
    val onLaunch: (AppEntry, Posture) -> Unit,
    val appActions: (launch: (AppEntry) -> Unit) -> AppActions,
    val garage: GarageActions,
)

private enum class Tab { GARAGE, APPS }

@Composable
fun LauncherScreen(inputs: LauncherInputs) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val fold = inputs.fold
    val posture = when {
        fold?.state == FoldingFeature.State.HALF_OPENED &&
            fold.orientation == FoldingFeature.Orientation.HORIZONTAL -> Posture.TABLETOP
        widthDp >= 600 -> Posture.OPEN
        else -> Posture.COVER
    }

    var query by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(Tab.GARAGE) }
    var sheet by remember { mutableStateOf<Project?>(null) }
    var editing by remember { mutableStateOf<Project?>(null) }
    var adding by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    fun reset() {
        query = ""; showAll = false
        focus.clearFocus(); keyboard?.hide()
    }
    LaunchedEffect(inputs.resetTick) { if (inputs.resetTick > 0) reset() }

    val visible = inputs.apps.filter { it.key !in inputs.hidden }
    val scoreMap = inputs.scores(posture)
    val predicted = visible.filter { (scoreMap[it.key] ?: 0.0) > 0.0 }.sortedByDescending { scoreMap[it.key] }
    val studio = STUDIO_PACKAGES.mapNotNull { pkg -> visible.firstOrNull { it.component.packageName == pkg } }
    // Search covers hidden apps too, so hiding never makes an app unreachable.
    val results = if (query.isBlank()) emptyList() else Search.rank(query, inputs.apps, inputs.counts())
    val projectHits = if (query.isBlank()) emptyList() else inputs.projects.filter {
        it.name.contains(query.trim(), ignoreCase = true) ||
            it.note.contains(query.trim(), ignoreCase = true) ||
            it.host.equals(query.trim(), ignoreCase = true)
    }

    val actions = inputs.appActions { app -> inputs.onLaunch(app, posture); reset() }
    val openBay: (Project) -> Unit = { sheet = it }
    val addBay: () -> Unit = { adding = true }
    val search: @Composable (Modifier) -> Unit = { m ->
        SearchField(
            query, { query = it },
            onGo = {
                results.firstOrNull()?.let(actions.launch) ?: projectHits.firstOrNull()?.let { sheet = it }
            },
            modifier = m,
        )
    }
    val now = rememberNow()
    val night = isNightShift(now)
    val clock: @Composable (Int) -> Unit = { size ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (night) NightShiftBadge() else Spacer(Modifier.height(16.dp))
            DialClock(now, size.dp, inputs.secondZone, inputs.onCycleZone)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, surface = Color(0xFF1A1816))) {
        Box(
            Modifier
                .fillMaxSize()
                .smokeBackground(night)
                .safeDrawingPadding()
                .imePadding()
        ) {
            AnimatedContent(targetState = posture, label = "posture") { p ->
                when (p) {
                    Posture.COVER -> Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        when {
                            query.isNotBlank() -> SearchResults(projectHits, results, 4, actions, openBay, Modifier.weight(1f))
                            showAll -> {
                                Header("All apps", trailing = "Close") { showAll = false }
                                AppGrid(visible, columns = 4, actions = actions, modifier = Modifier.weight(1f))
                            }
                            else -> {
                                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { clock(200) }
                                Header("Garage")
                                GarageRow(inputs.projects, openBay, addBay)
                                Header(if (predicted.isEmpty()) "Studio" else "Right now", trailing = "All apps") { showAll = true }
                                AppGrid((predicted.ifEmpty { studio }).take(4), columns = 4, actions = actions, scroll = false)
                            }
                        }
                        search(Modifier.fillMaxWidth().padding(vertical = 12.dp))
                    }

                    Posture.OPEN -> Row(Modifier.fillMaxSize().padding(16.dp)) {
                        Column(Modifier.weight(0.38f).padding(end = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            clock(250)
                            if (studio.isNotEmpty()) {
                                Header("Studio")
                                AppGrid(studio.take(6), columns = 3, actions = actions, scroll = false)
                            }
                            Header("Right now")
                            if (predicted.isEmpty()) LearningHint()
                            else AppGrid(predicted.take(9), columns = 3, actions = actions, modifier = Modifier.weight(1f))
                        }
                        Column(Modifier.weight(0.62f)) {
                            search(Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            if (query.isNotBlank()) {
                                SearchResults(projectHits, results, 5, actions, openBay, Modifier.weight(1f))
                            } else {
                                Row(Modifier.padding(vertical = 8.dp)) {
                                    TabLabel("GARAGE  ${inputs.projects.size}", tab == Tab.GARAGE) { tab = Tab.GARAGE }
                                    TabLabel("APPS  ${visible.size}", tab == Tab.APPS) { tab = Tab.APPS }
                                }
                                if (tab == Tab.GARAGE) GarageGrid(inputs.projects, 3, openBay, addBay, Modifier.weight(1f))
                                else AppGrid(visible, columns = 5, actions = actions, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Posture.TABLETOP -> Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { clock(220) }
                        Column(Modifier.weight(1f).padding(top = 12.dp)) {
                            search(Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            if (query.isNotBlank()) SearchResults(projectHits, results, 6, actions, openBay, Modifier.weight(1f))
                            else {
                                GarageRow(inputs.projects, openBay, addBay)
                                Spacer(Modifier.height(8.dp))
                                AppGrid(predicted.ifEmpty { studio }.take(6), columns = 6, actions = actions, scroll = false)
                            }
                        }
                    }
                }
            }

            sheet?.let { p -> ProjectSheet(p, inputs.garage, onEdit = { editing = p }, onDismiss = { sheet = null }) }
            editing?.let { p -> EditProjectDialog(p, inputs.garage, onDismiss = { editing = null }) }
            if (adding) EditProjectDialog(null, inputs.garage, onDismiss = { adding = false })
        }
    }
}

@Composable
private fun SearchResults(
    projects: List<Project>,
    apps: List<AppEntry>,
    columns: Int,
    actions: AppActions,
    onOpen: (Project) -> Unit,
    modifier: Modifier,
) {
    Column(modifier) {
        if (projects.isNotEmpty()) {
            Header("Garage")
            GarageRow(projects, onOpen, onAdd = {})
        }
        if (apps.isNotEmpty()) {
            Header("Apps")
            AppGrid(apps, columns = columns, actions = actions, modifier = Modifier.weight(1f))
        }
        if (projects.isEmpty() && apps.isEmpty()) {
            Text("Nothing by that name.", color = Muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}

@Composable
private fun TabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text, color = if (selected) Accent else Muted, fontSize = 12.sp, letterSpacing = 2.sp,
        fontFamily = FontFamily.Monospace, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clickable(onClick = onClick).padding(end = 20.dp, top = 6.dp, bottom = 6.dp)
    )
}

@Composable
private fun Header(title: String, trailing: String? = null, onTrailing: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title.uppercase(), color = Muted, fontSize = 11.sp, letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                trailing, color = Accent, fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onTrailing).padding(horizontal = 8.dp, vertical = 6.dp)
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

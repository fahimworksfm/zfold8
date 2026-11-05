package fm.fahim.foldlauncher.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import fm.fahim.foldlauncher.Kind
import fm.fahim.foldlauncher.Project
import fm.fahim.foldlauncher.Status
import fm.fahim.foldlauncher.dashboardFor
import fm.fahim.foldlauncher.daysAway
import fm.fahim.foldlauncher.dust
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** What the Garage can do with a project. */
class GarageActions(
    val openUrl: (String) -> Unit,
    val askClaude: (Project) -> Unit,
    val save: (Project) -> Unit,
    val delete: (Project) -> Unit,
    val visit: (Project) -> Unit,
    val pickArt: (Project) -> Unit,
    val clearArt: (Project) -> Unit,
)

/** Everything a bay needs to draw itself, shared by every bay on screen. */
class BayLook(
    val images: Map<String, ImageBitmap?>,
    val ticker: State<Long>,
    val lightsKey: Int,
)

private val BayShape = RoundedCornerShape(10.dp)
private val Amber = Color(0xFFFFB020)
private val Green = Color(0xFF6EE7A0)

/** What happens when you rev a bay all the way: the live site first, then the repo, then Claude. */
fun ignite(project: Project, actions: GarageActions) = when {
    project.live.isNotBlank() -> actions.openUrl(project.live)
    project.repo.isNotBlank() -> actions.openUrl(project.repo)
    else -> actions.askClaude(project)
}

/**
 * A parking bay. Its light strip flickers on like a garage tube when the door opens. Its status shows as
 * car lights. Dust settles on it the longer you stay away. Tap it to open it; hold it to rev the engine,
 * and at full revs it launches.
 */
@Composable
fun ProjectBay(
    index: Int,
    project: Project,
    look: BayLook,
    onClick: () -> Unit,
    onIgnite: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
) {
    val color = kindColor(project.kind)
    val view = LocalView.current
    val art = look.images["bay_${project.id}"]

    // The light strip flickers on when the door opens, each bay a beat after the one before.
    val strip = remember { Animatable(0f) }
    LaunchedEffect(look.lightsKey) {
        strip.snapTo(0f)
        delay(120L + index * 70L)
        strip.animateTo(1f, keyframes {
            durationMillis = 520
            0.9f at 40; 0.1f at 90; 0.8f at 150; 0.25f at 210; 1f at 300; 0.7f at 380; 1f at 520
        })
    }

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "press")
    val rev = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dust = project.dust()
    val specks = remember(project.id) {
        val r = Random(project.id.hashCode())
        List(140) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) }
    }

    Box(
        modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(BayShape)
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.16f), Glass, Color(0x14000000))))
            .border(1.dp, color.copy(alpha = 0.10f + 0.15f * strip.value), BayShape)
            .pointerInput(project.id) {
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        pressed = true
                        val started = System.currentTimeMillis()
                        val engine = launch {
                            delay(260)
                            // Revving: twelve steps, each with a tick you can feel.
                            for (step in 1..12) {
                                rev.animateTo(step / 12f, tween(70))
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            onIgnite()
                        }
                        val up = waitForUpOrCancellation()
                        pressed = false
                        val quick = System.currentTimeMillis() - started < 260
                        if (engine.isActive) engine.cancel()
                        if (up != null && quick) onClick()
                        scope.launch { rev.animateTo(0f, tween(350, easing = FastOutSlowInEasing)) }
                    }
                }
            }
    ) {
        if (art != null) {
            Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Spacer(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0xCC0B0A09)))))
        }

        // Dust: a grey film and specks that build up over three weeks away.
        if (dust > 0.05f) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFF8A8070).copy(alpha = dust * 0.22f))
                specks.forEach { (x, y, s) ->
                    if (s < dust) drawCircle(Color(0xFFCFC4B0).copy(alpha = 0.18f + 0.25f * s), (0.6f + s * 1.4f).dp.toPx(), Offset(x * size.width, y * size.height))
                }
            }
        }

        // The tube light along the top.
        val lightLevel = strip.value * if (project.status == Status.PAUSED) 0.25f else 1f
        Canvas(Modifier.fillMaxWidth().height(18.dp)) {
            drawRect(Brush.verticalGradient(listOf(color.copy(alpha = 0.35f * lightLevel), Color.Transparent)), size = Size(size.width, size.height))
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, color.copy(alpha = lightLevel), Color.Transparent)),
                size = Size(size.width, 3.dp.toPx())
            )
        }

        StatusLights(project, look.ticker, Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "BAY %02d".format(index + 1), color = color.copy(alpha = 0.85f),
                    fontSize = 10.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Text("ODO %05d".format(project.visits), color = Dim.copy(alpha = 0.8f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.weight(1f))
            Text(
                project.name, color = Cream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            val line = when {
                project.status == Status.WAITING && project.waitingOn.isNotBlank() -> "Waiting on ${project.waitingOn}"
                project.daysAway() >= 3 -> "${project.daysAway()} days untouched"
                else -> listOf(project.kind.label, project.host).filter { it.isNotBlank() }.joinToString(" · ")
            }
            Text(
                line, color = if (project.status == Status.WAITING) Amber else Dim,
                fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }

        if (rev.value > 0f) RevGauge(rev.value, color, Modifier.fillMaxSize())
    }
}

/** Status as car lights: a breathing green for live, amber hazards for waiting, a scanner for building. */
@Composable
private fun StatusLights(project: Project, ticker: State<Long>, modifier: Modifier) {
    if (project.status == Status.NONE) return
    Canvas(modifier) {
        val t = ticker.value / 1000.0
        when (project.status) {
            Status.LIVE -> {
                val a = (0.55 + 0.45 * sin(t * 2.2)).toFloat()
                val c = Offset(size.width - 14.dp.toPx(), 30.dp.toPx())
                drawCircle(Green.copy(alpha = 0.25f * a), 8.dp.toPx(), c)
                drawCircle(Green.copy(alpha = a), 3.dp.toPx(), c)
            }
            Status.WAITING -> {
                val on = (t * 1.4) % 1.0 < 0.5
                if (on) listOf(10.dp.toPx(), size.width - 10.dp.toPx()).forEach { x ->
                    val c = Offset(x, size.height - 10.dp.toPx())
                    drawCircle(Amber.copy(alpha = 0.3f), 9.dp.toPx(), c)
                    drawCircle(Amber, 3.5.dp.toPx(), c)
                }
            }
            Status.BUILDING -> {
                // A moving band of hazard stripes along the bottom edge.
                val h = 4.dp.toPx()
                val y = size.height - h
                val w = 10.dp.toPx()
                val shift = ((t * 30.0) % (2 * w)).toFloat()
                clipRect(0f, y, size.width, size.height) {
                    var x = -2 * w + shift
                    while (x < size.width + w) {
                        drawLine(Amber.copy(alpha = 0.85f), Offset(x, size.height), Offset(x + h, y), strokeWidth = w / 2)
                        x += w
                    }
                }
            }
            else -> {}
        }
    }
}

/** The rev counter that sweeps up while you hold a bay. */
@Composable
private fun RevGauge(progress: Float, color: Color, modifier: Modifier) {
    Canvas(modifier.background(Color(0x99000000))) {
        val r = size.minDimension * 0.36f
        val c = Offset(size.width / 2, size.height / 2 + r * 0.25f)
        val start = 150f
        val sweep = 240f
        drawArc(Cream.copy(alpha = 0.15f), start, sweep, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        drawArc(
            Brush.sweepGradient(listOf(color, Color(0xFFE0607E), color), c), start, sweep * progress, false,
            Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round)
        )
        val a = Math.toRadians((start + sweep * progress).toDouble())
        drawLine(Cream, c, Offset(c.x + r * 0.9f * cos(a).toFloat(), c.y + r * 0.9f * sin(a).toFloat()), 2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(Cream, 3.dp.toPx(), c)
    }
}

@Composable
private fun AddBay(onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 120.dp) {
    Box(
        modifier
            .height(height)
            .border(1.dp, Dim.copy(alpha = 0.35f), BayShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("+  NEW BAY", color = Dim, fontSize = 12.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
    }
}

/** A sideways strip of bays, for the cover screen and tabletop. */
@Composable
fun GarageRow(projects: List<Project>, look: BayLook, onOpen: (Project) -> Unit, onIgnite: (Project) -> Unit, onAdd: () -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(projects, key = { _, p -> p.id }) { i, p ->
            ProjectBay(i, p, look, onClick = { onOpen(p) }, onIgnite = { onIgnite(p) }, modifier = Modifier.width(156.dp))
        }
        item { AddBay(onAdd, Modifier.width(156.dp)) }
    }
}

/** The whole garage, for the open inner screen. */
@Composable
fun GarageGrid(
    projects: List<Project>,
    columns: Int,
    look: BayLook,
    onOpen: (Project) -> Unit,
    onIgnite: (Project) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(projects, key = { _, p -> p.id }) { i, p ->
            ProjectBay(i, p, look, onClick = { onOpen(p) }, onIgnite = { onIgnite(p) }, height = 132.dp)
        }
        item { AddBay(onAdd, height = 132.dp) }
    }
}

/**
 * The roll-up door in front of the garage. At 0 it's shut; at 1 it's rolled up out of sight.
 * As it lifts, gold light spills out from under it.
 */
@Composable
fun GarageDoor(progress: Float, modifier: Modifier = Modifier) {
    if (progress >= 0.999f) return
    BoxWithConstraints(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val lift = size.height * FastOutSlowInEasing.transform(progress)
            val bottom = size.height - lift

            // Light pouring out from the gap under the door.
            if (progress > 0.02f) {
                drawRect(
                    Brush.verticalGradient(listOf(Gold.copy(alpha = 0.35f * (1 - progress)), Color.Transparent), startY = bottom, endY = bottom + 220.dp.toPx()),
                    topLeft = Offset(0f, bottom), size = Size(size.width, 220.dp.toPx())
                )
            }

            // The slats.
            val slat = 34.dp.toPx()
            var y = bottom - slat
            while (y > -slat) {
                drawRect(
                    Brush.verticalGradient(listOf(Color(0xFF2E2B28), Color(0xFF1B1917), Color(0xFF26231F)), startY = y, endY = y + slat),
                    topLeft = Offset(0f, y), size = Size(size.width, slat)
                )
                drawLine(Color(0x33FFFFFF), Offset(0f, y + 1f), Offset(size.width, y + 1f), 1f)
                drawLine(Color(0x66000000), Offset(0f, y + slat - 1f), Offset(size.width, y + slat - 1f), 2f)
                y -= slat
            }
            // Bottom rail and handle.
            drawRect(Color(0xFF3A3632), Offset(0f, bottom - 10.dp.toPx()), Size(size.width, 10.dp.toPx()))
            drawRect(Gold.copy(alpha = 0.8f), Offset(size.width / 2 - 36.dp.toPx(), bottom - 26.dp.toPx()), Size(72.dp.toPx(), 5.dp.toPx()))
        }
        // Stencilled name on the door.
        Text(
            "UPLIFT  ·  GARAGE", color = Gold.copy(alpha = 0.55f), fontSize = 22.sp, letterSpacing = 6.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { translationY = -constraints.maxHeight * FastOutSlowInEasing.transform(progress) }
        )
    }
}

/** Tapping a bay opens this: every way into the project, one tap each, with a quick status switch. */
@Composable
fun ProjectSheet(project: Project, actions: GarageActions, hasArt: Boolean, onEdit: () -> Unit, onDismiss: () -> Unit) {
    val color = kindColor(project.kind)
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = appear.value.coerceIn(0f, 1f)
                    translationY = (1 - appear.value) * 80.dp.toPx()
                    rotationX = (1 - appear.value) * 12f
                }
                .background(Color(0xFF1A1816), RoundedCornerShape(16.dp))
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent), endY = 90.dp.toPx()))
                }
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(project.kind.label.uppercase(), color = color, fontSize = 11.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
            Text(project.name, color = Cream, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            if (project.note.isNotBlank()) Text(project.note, color = Dim, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            if (project.account.isNotBlank()) {
                Text("Lives under: ${project.account}", color = Cream, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            }
            StatusChips(project.status) { actions.save(project.copy(status = it)) }
            Spacer(Modifier.height(8.dp))

            fun go(block: () -> Unit) { onDismiss(); block() }
            if (project.live.isNotBlank()) SheetButton("Open live site", color) { go { actions.openUrl(project.live) } }
            if (project.repo.isNotBlank()) SheetButton("Open repo") { go { actions.openUrl(project.repo) } }
            dashboardFor(project.host)?.let { url -> SheetButton("Open ${project.host} dashboard") { go { actions.openUrl(url) } } }
            SheetButton("Work on it with Claude") { go { actions.askClaude(project) } }
            SheetButton(if (hasArt) "Change bay art" else "Add bay art from gallery") { go { actions.pickArt(project) } }
            if (hasArt) SheetButton("Remove bay art", Dim) { go { actions.clearArt(project) } }
            SheetButton("Edit bay", Dim) { go(onEdit) }
        }
    }
}

@Composable
private fun StatusChips(current: Status, onPick: (Status) -> Unit) {
    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Status.entries.filter { it != Status.NONE }.forEach { s ->
            val c = when (s) {
                Status.LIVE -> Green
                Status.WAITING, Status.BUILDING -> Amber
                else -> Dim
            }
            val on = s == current
            Text(
                s.label, fontSize = 12.sp, color = if (on) Charcoal else c,
                modifier = Modifier
                    .background(if (on) c else Color.Transparent, RoundedCornerShape(12.dp))
                    .border(1.dp, c.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .clickable { onPick(if (on) Status.NONE else s) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun SheetButton(text: String, color: Color = Cream, onClick: () -> Unit) {
    Text(
        text, color = color, fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp)
    )
}

/** Add or change a bay. The account field is for which login a project uses, never for passwords. */
@Composable
fun EditProjectDialog(initial: Project?, actions: GarageActions, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var kind by remember { mutableStateOf(initial?.kind ?: Kind.OWN) }
    var status by remember { mutableStateOf(initial?.status ?: Status.NONE) }
    var live by remember { mutableStateOf(initial?.live ?: "") }
    var repo by remember { mutableStateOf(initial?.repo ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var account by remember { mutableStateOf(initial?.account ?: "") }
    var waitingOn by remember { mutableStateOf(initial?.waitingOn ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1816), RoundedCornerShape(16.dp))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(if (initial == null) "New bay" else "Edit bay", color = Cream, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Kind.entries.forEach { k ->
                    val on = k == kind
                    Text(
                        k.label, color = if (on) Charcoal else kindColor(k), fontSize = 12.sp,
                        modifier = Modifier
                            .background(if (on) kindColor(k) else Color.Transparent, RoundedCornerShape(12.dp))
                            .border(1.dp, kindColor(k), RoundedCornerShape(12.dp))
                            .clickable { kind = k }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            StatusChips(status) { status = it }
            Field("Name", name) { name = it }
            if (status == Status.WAITING) Field("Waiting on (who or what)", waitingOn) { waitingOn = it }
            Field("Live site URL", live) { live = it }
            Field("Repo URL", repo) { repo = it }
            Field("Host (Netlify, Vercel, Render…)", host) { host = it }
            Field("Which account it's on (no passwords)", account) { account = it }
            Field("Note", note) { note = it }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (initial != null) {
                    Text("Delete", color = Color(0xFFE0607E), modifier = Modifier.clickable { actions.delete(initial); onDismiss() }.padding(8.dp))
                }
                Spacer(Modifier.weight(1f))
                Text("Cancel", color = Dim, modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
                Text(
                    "Save", color = Gold, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(enabled = name.isNotBlank()) {
                            val base = initial ?: Project(id = "p" + System.currentTimeMillis(), name = "", kind = kind, lastVisit = System.currentTimeMillis())
                            actions.save(
                                base.copy(
                                    name = name.trim(), kind = kind, status = status, live = live.trim(), repo = repo.trim(),
                                    host = host.trim(), account = account.trim(), waitingOn = waitingOn.trim(), note = note.trim(),
                                )
                            )
                            onDismiss()
                        }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true,
        label = { Text(label) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Cream, unfocusedTextColor = Cream,
            focusedBorderColor = Gold, unfocusedBorderColor = Dim.copy(alpha = 0.5f),
            focusedLabelColor = Gold, unfocusedLabelColor = Dim, cursorColor = Gold,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
}

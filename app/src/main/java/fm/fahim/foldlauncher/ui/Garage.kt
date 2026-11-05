package fm.fahim.foldlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import fm.fahim.foldlauncher.Kind
import fm.fahim.foldlauncher.Project
import fm.fahim.foldlauncher.dashboardFor

/** What the Garage can do with a project. */
class GarageActions(
    val openUrl: (String) -> Unit,
    val askClaude: (Project) -> Unit,
    val save: (Project) -> Unit,
    val delete: (Project) -> Unit,
)

private val BayShape = RoundedCornerShape(10.dp)

/** A parking bay: a light strip in the project's colour, a painted bay number, and its name. */
@Composable
fun ProjectBay(index: Int, project: Project, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 112.dp) {
    val color = kindColor(project.kind)
    Column(
        modifier
            .height(height)
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.16f), Glass, Color(0x14000000))), BayShape)
            .border(1.dp, color.copy(alpha = 0.22f), BayShape)
            .clickable(onClick = onClick)
    ) {
        // The light strip over the bay.
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, color, Color.Transparent)))
        )
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "BAY %02d".format(index + 1), color = color.copy(alpha = 0.85f),
                    fontSize = 10.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                if (project.live.isNotBlank()) Box(Modifier.size(6.dp).background(Color(0xFF6EE7A0), CircleShape))
            }
            Spacer(Modifier.weight(1f))
            Text(
                project.name, color = Cream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                listOf(project.kind.label, project.host).filter { it.isNotBlank() }.joinToString(" · "),
                color = Dim, fontSize = 11.sp, maxLines = 1
            )
        }
    }
}

@Composable
private fun AddBay(onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 112.dp) {
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
fun GarageRow(projects: List<Project>, onOpen: (Project) -> Unit, onAdd: () -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(projects, key = { _, p -> p.id }) { i, p ->
            ProjectBay(i, p, onClick = { onOpen(p) }, modifier = Modifier.width(150.dp))
        }
        item { AddBay(onAdd, Modifier.width(150.dp)) }
    }
}

/** The whole garage, for the open inner screen. */
@Composable
fun GarageGrid(projects: List<Project>, columns: Int, onOpen: (Project) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(projects, key = { _, p -> p.id }) { i, p -> ProjectBay(i, p, onClick = { onOpen(p) }) }
        item { AddBay(onAdd) }
    }
}

/** Tapping a bay opens this: every way into the project, with one tap each. */
@Composable
fun ProjectSheet(project: Project, actions: GarageActions, onEdit: () -> Unit, onDismiss: () -> Unit) {
    val color = kindColor(project.kind)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1816), RoundedCornerShape(16.dp))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(project.kind.label.uppercase(), color = color, fontSize = 11.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
            Text(project.name, color = Cream, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            if (project.note.isNotBlank()) Text(project.note, color = Dim, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            if (project.account.isNotBlank()) {
                Text("Lives under: ${project.account}", color = Cream, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            }
            Spacer(Modifier.height(16.dp))

            fun go(block: () -> Unit) { onDismiss(); block() }
            if (project.live.isNotBlank()) SheetButton("Open live site", color) { go { actions.openUrl(project.live) } }
            if (project.repo.isNotBlank()) SheetButton("Open repo") { go { actions.openUrl(project.repo) } }
            dashboardFor(project.host)?.let { url -> SheetButton("Open ${project.host} dashboard") { go { actions.openUrl(url) } } }
            SheetButton("Work on it with Claude") { go { actions.askClaude(project) } }
            SheetButton("Edit bay", Dim) { go(onEdit) }
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
            .padding(vertical = 12.dp)
    )
}

/** Add or change a bay. The account field is for which login a project uses, never for passwords. */
@Composable
fun EditProjectDialog(initial: Project?, actions: GarageActions, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var kind by remember { mutableStateOf(initial?.kind ?: Kind.OWN) }
    var live by remember { mutableStateOf(initial?.live ?: "") }
    var repo by remember { mutableStateOf(initial?.repo ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var account by remember { mutableStateOf(initial?.account ?: "") }
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
            Field("Name", name) { name = it }
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
                            actions.save(
                                Project(
                                    id = initial?.id ?: "p" + System.currentTimeMillis(),
                                    name = name.trim(), kind = kind, live = live.trim(), repo = repo.trim(),
                                    host = host.trim(), account = account.trim(), note = note.trim(),
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

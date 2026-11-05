package fm.fahim.foldlauncher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** What a project is. Each kind gets its own colour on the bay's light strip. */
enum class Kind(val label: String) { CLIENT("Client"), OWN("Own"), GAME("Game"), PERSONAL("Personal") }

/** Where a project stands. Each status shows on the bay as a different kind of light. */
enum class Status(val label: String) {
    NONE("No status"),
    LIVE("Live"),          // green running light
    BUILDING("Building"),  // a moving scanner strip
    WAITING("Waiting"),    // amber hazard lights
    PAUSED("Parked"),      // lights off
}

/**
 * One project, parked in its own bay in the Garage.
 * [account] says which login it lives under ("Netlify: work account"). Never store a password here.
 */
data class Project(
    val id: String,
    val name: String,
    val kind: Kind,
    val live: String = "",
    val repo: String = "",
    val host: String = "",
    val account: String = "",
    val note: String = "",
    val status: Status = Status.NONE,
    val waitingOn: String = "",
    val visits: Int = 0,
    val lastVisit: Long = 0,
)

/** How dusty a bay is: 0 if you touched it today, 1 after three weeks away. */
fun Project.dust(now: Long = System.currentTimeMillis()): Float =
    if (lastVisit == 0L) 0f else ((now - lastVisit) / (21f * 86_400_000f)).coerceIn(0f, 1f)

fun Project.daysAway(now: Long = System.currentTimeMillis()): Int =
    if (lastVisit == 0L) 0 else ((now - lastVisit) / 86_400_000L).toInt()

/** Saves your projects as a JSON file on the phone. The first run starts from the projects you've built with Claude. */
class ProjectStore(context: Context) {
    private val file = File(context.filesDir, "projects.json")

    fun load(): List<Project> {
        val now = System.currentTimeMillis()
        val list = if (!file.exists()) seed() else runCatching {
            val arr = JSONArray(file.readText())
            val seeded = seed().associateBy { it.id }
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val p = o.toProject()
                // Projects saved by an older version have no status yet: take the starting one.
                val s = seeded[p.id]
                if (!o.has("status") && s != null) p.copy(status = s.status, waitingOn = s.waitingOn) else p
            }
        }.getOrElse { seed() }
        // Dust starts building from the day you install this version, not from 1970.
        return list.map { if (it.lastVisit == 0L) it.copy(lastVisit = now) else it }.also(::save)
    }

    fun save(projects: List<Project>) {
        val arr = JSONArray()
        projects.forEach { arr.put(it.toJson()) }
        runCatching { file.writeText(arr.toString(2)) }
    }

    private fun JSONObject.toProject() = Project(
        id = optString("id"),
        name = optString("name"),
        kind = runCatching { Kind.valueOf(optString("kind")) }.getOrDefault(Kind.OWN),
        live = optString("live"),
        repo = optString("repo"),
        host = optString("host"),
        account = optString("account"),
        note = optString("note"),
        status = runCatching { Status.valueOf(optString("status")) }.getOrDefault(Status.NONE),
        waitingOn = optString("waitingOn"),
        visits = optInt("visits"),
        lastVisit = optLong("lastVisit"),
    )

    private fun Project.toJson() = JSONObject()
        .put("id", id).put("name", name).put("kind", kind.name)
        .put("live", live).put("repo", repo).put("host", host)
        .put("account", account).put("note", note)
        .put("status", status.name).put("waitingOn", waitingOn)
        .put("visits", visits).put("lastVisit", lastVisit)

    /**
     * The starting garage: projects from your Claude Code sessions. Only links that appeared in
     * those sessions are filled in. Edit any bay to add the rest.
     */
    private fun seed(): List<Project> = listOf(
        Project("garage", "Garage", Kind.OWN, repo = "https://github.com/fahimworksfm/garage", note = "Friends' cars, underground garage, Three.js", status = Status.WAITING, waitingOn = "Shawon's photos"),
        Project("uplift", "Uplift Digital Partners", Kind.OWN, live = "https://upliftdigitalpartners.com", note = "The studio", status = Status.LIVE),
        Project("anima", "Anima-Fy", Kind.OWN, host = "Vercel", note = "Team workspaces need Upstash Redis", status = Status.BUILDING),
        Project("neverforget", "NeverForget", Kind.OWN, live = "https://neverforget-timesheet.netlify.app", host = "Netlify", note = "Smoke bg, 3D dial", status = Status.LIVE),
        Project("near", "Near Coffee", Kind.CLIENT, live = "https://www.nearcoffee.space", note = "Small orbit, espresso & cream", status = Status.LIVE),
        Project("sharif", "Sharif Adornment", Kind.CLIENT, repo = "https://github.com/upliftdigitalpartners/Sharifadornment", host = "GitHub Pages", note = "Gold & charcoal, Eid collection"),
        Project("maria", "Maria's Closet", Kind.CLIENT, repo = "https://github.com/fahimworksfm/MariaCloset", host = "Vercel", status = Status.LIVE),
        Project("hololand", "Hololand", Kind.CLIENT, live = "https://hololandbd.com", status = Status.LIVE),
        Project("mozumder", "Mozumder", Kind.CLIENT, note = "3D map, Bangla version", status = Status.LIVE),
        Project("bandar", "BandarTrade", Kind.CLIENT, host = "Vercel", note = "WhatsApp + n8n pipeline"),
        Project("prime", "Primé", Kind.PERSONAL, host = "GitHub Pages", note = "Witty but subtle"),
        Project("tether", "Tether", Kind.PERSONAL, host = "Vercel", note = "Memory Jar", status = Status.LIVE),
        Project("curbside", "Curbside", Kind.GAME, host = "Unreal 5.7", note = "Street-food vendor game", status = Status.BUILDING),
        Project("nightwalker", "Night Walker NYC", Kind.GAME),
        Project("livegrid", "LiveGrid", Kind.OWN),
        Project("openmeteo", "Open-Meteo", Kind.OWN, repo = "https://github.com/fahimworksfm/open-meteo", host = "Netlify"),
        Project("livescanner", "Live Scanner", Kind.OWN, host = "Android"),
        Project("ghosted", "Ghosted", Kind.OWN, host = "Render", note = "The anti-ATS satire", status = Status.PAUSED),
    )
}

/** Where to manage a project's hosting. */
fun dashboardFor(host: String): String? = when (host.trim().lowercase()) {
    "netlify" -> "https://app.netlify.com"
    "vercel" -> "https://vercel.com/dashboard"
    "render" -> "https://dashboard.render.com"
    "supabase" -> "https://supabase.com/dashboard"
    "cloudflare" -> "https://dash.cloudflare.com"
    else -> null
}

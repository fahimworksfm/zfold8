package fm.fahim.foldlauncher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** What a project is. Each kind gets its own colour on the bay's light strip. */
enum class Kind(val label: String) { CLIENT("Client"), OWN("Own"), GAME("Game"), PERSONAL("Personal") }

/**
 * One project, shown as a parking bay in the Garage.
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
)

/** Keeps your projects in a JSON file on the phone. It starts from the projects you've built with Claude. */
class ProjectStore(context: Context) {
    private val file = File(context.filesDir, "projects.json")

    fun load(): List<Project> {
        if (!file.exists()) return seed().also(::save)
        return runCatching {
            val arr = JSONArray(file.readText())
            List(arr.length()) { i -> arr.getJSONObject(i).toProject() }
        }.getOrElse { seed() }
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
    )

    private fun Project.toJson() = JSONObject()
        .put("id", id).put("name", name).put("kind", kind.name)
        .put("live", live).put("repo", repo).put("host", host)
        .put("account", account).put("note", note)

    /**
     * The starting garage: projects from your Claude Code sessions. Only links that appeared in
     * those sessions are filled in. Edit any bay to add the rest.
     */
    private fun seed(): List<Project> = listOf(
        Project("garage", "Garage", Kind.OWN, repo = "https://github.com/fahimworksfm/garage", note = "Friends' cars, underground garage, Three.js"),
        Project("uplift", "Uplift Digital Partners", Kind.OWN, live = "https://upliftdigitalpartners.com", note = "The studio"),
        Project("anima", "Anima-Fy", Kind.OWN, host = "Vercel", note = "Team workspaces need Upstash Redis"),
        Project("neverforget", "NeverForget", Kind.OWN, live = "https://neverforget-timesheet.netlify.app", host = "Netlify", note = "Smoke bg, 3D dial"),
        Project("near", "Near Coffee", Kind.CLIENT, live = "https://www.nearcoffee.space", note = "Small orbit, espresso & cream"),
        Project("sharif", "Sharif Adornment", Kind.CLIENT, repo = "https://github.com/upliftdigitalpartners/Sharifadornment", host = "GitHub Pages", note = "Gold & charcoal, Eid collection"),
        Project("maria", "Maria's Closet", Kind.CLIENT, repo = "https://github.com/fahimworksfm/MariaCloset", host = "Vercel"),
        Project("hololand", "Hololand", Kind.CLIENT, live = "https://hololandbd.com"),
        Project("mozumder", "Mozumder", Kind.CLIENT, note = "3D map, Bangla version"),
        Project("bandar", "BandarTrade", Kind.CLIENT, host = "Vercel", note = "WhatsApp + n8n pipeline"),
        Project("prime", "Primé", Kind.PERSONAL, host = "GitHub Pages", note = "Witty but subtle"),
        Project("tether", "Tether", Kind.PERSONAL, host = "Vercel", note = "Memory Jar"),
        Project("curbside", "Curbside", Kind.GAME, host = "Unreal 5.7", note = "Street-food vendor game"),
        Project("nightwalker", "Night Walker NYC", Kind.GAME),
        Project("livegrid", "LiveGrid", Kind.OWN),
        Project("openmeteo", "Open-Meteo", Kind.OWN, repo = "https://github.com/fahimworksfm/open-meteo", host = "Netlify"),
        Project("livescanner", "Live Scanner", Kind.OWN, host = "Android"),
        Project("ghosted", "Ghosted", Kind.OWN, host = "Render", note = "The anti-ATS satire"),
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

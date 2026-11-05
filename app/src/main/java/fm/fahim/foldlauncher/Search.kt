package fm.fahim.foldlauncher

import java.text.Normalizer

/**
 * Matches what you type against app names, best match first:
 *   "you"  -> YouTube         (name starts with it)
 *   "gm"   -> Google Maps     (first letters of each word)
 *   "wa"   -> WhatsApp        (its capital letters)
 *   "tube" -> YouTube         (part of the name)
 *   "wtsp" -> WhatsApp        (letters in order)
 * Ties go to the app you open most.
 */
object Search {
    fun rank(query: String, apps: List<AppEntry>, counts: Map<String, Int>): List<AppEntry> {
        val q = normalize(query).replace(" ", "")
        if (q.isEmpty()) return apps
        return apps
            .mapNotNull { app -> score(q, app.label)?.let { app to it } }
            .sortedWith(
                compareByDescending<Pair<AppEntry, Int>> { it.second }
                    .thenByDescending { counts[it.first.key] ?: 0 }
                    .thenBy { it.first.label.length }
            )
            .map { it.first }
    }

    private fun score(q: String, label: String): Int? {
        val name = normalize(label)
        val compact = name.replace(" ", "")
        val words = name.split(' ', '-', '_', '.').filter { it.isNotEmpty() }
        val initials = words.joinToString("") { it.take(1) }
        val capitals = label.filter { it.isUpperCase() }.lowercase()

        return when {
            compact == q -> 1000
            compact.startsWith(q) -> 900 - compact.length
            initials.startsWith(q) -> 800
            capitals.length >= 2 && capitals.startsWith(q) -> 750
            words.any { it.startsWith(q) } -> 700
            compact.contains(q) -> 600
            isSubsequence(q, compact) -> 400 - (compact.length - q.length)
            else -> null
        }
    }

    private fun isSubsequence(q: String, s: String): Boolean {
        var i = 0
        for (c in s) if (i < q.length && c == q[i]) i++
        return i == q.length
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .trim()
}

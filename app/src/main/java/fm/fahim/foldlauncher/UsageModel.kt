package fm.fahim.foldlauncher

import android.content.Context
import java.io.File
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

/** How the phone is being held. The launcher learns separate habits for each posture. */
enum class Posture { COVER, OPEN, TABLETOP }

private data class Launch(val key: String, val at: Long, val posture: Posture)

/**
 * Learns which apps you open, when and how you hold the phone, using only data stored on
 * the phone: a small text file of recent launches. Nothing leaves the device.
 *
 * Each past launch adds to an app's score. A launch counts for more when
 *  - it happened recently (its weight halves every two weeks),
 *  - it happened at a similar time of day (within about an hour and a half),
 *  - it happened on the same kind of day (weekday or weekend),
 *  - the phone was in the same posture (cover screen, open, or tabletop).
 */
class UsageModel(context: Context) {
    private val file = File(context.filesDir, "launches.tsv")
    private val hiddenFile = File(context.filesDir, "hidden.txt")
    private val launches = ArrayDeque<Launch>()
    private val hidden = mutableSetOf<String>()

    init {
        runCatching {
            file.takeIf { it.exists() }?.forEachLine { line ->
                val p = line.split('\t')
                if (p.size == 3) {
                    val posture = runCatching { Posture.valueOf(p[2]) }.getOrNull()
                    val at = p[1].toLongOrNull()
                    if (posture != null && at != null) launches.addLast(Launch(p[0], at, posture))
                }
            }
        }
        runCatching { hiddenFile.takeIf { it.exists() }?.readLines()?.filter { it.isNotBlank() }?.let(hidden::addAll) }
    }

    val hiddenKeys: Set<String> get() = hidden

    fun record(key: String, posture: Posture, now: Long = System.currentTimeMillis()) {
        launches.addLast(Launch(key, now, posture))
        if (launches.size > MAX_EVENTS) {
            repeat(launches.size - MAX_EVENTS) { launches.removeFirst() }
            rewrite()
        } else {
            runCatching { file.appendText("$key\t$now\t${posture.name}\n") }
        }
    }

    fun setHidden(key: String, isHidden: Boolean) {
        if (isHidden) hidden.add(key) else hidden.remove(key)
        runCatching { hiddenFile.writeText(hidden.joinToString("\n")) }
    }

    fun forget(key: String) {
        launches.removeAll { it.key == key }
        rewrite()
    }

    /** Scores for every app that has been launched; higher means more likely right now. */
    fun scores(posture: Posture, now: Long = System.currentTimeMillis()): Map<String, Double> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        val nowHour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0
        val nowWeekend = cal.isWeekend()

        val out = HashMap<String, Double>()
        for (l in launches) {
            cal.timeInMillis = l.at
            val hour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0
            val diff = abs(hour - nowHour).let { min(it, 24 - it) }
            val ageDays = (now - l.at) / 86_400_000.0

            val recency = exp(-ageDays * LN2 / HALF_LIFE_DAYS)
            val timeOfDay = 0.25 + exp(-(diff * diff) / (2 * 1.5 * 1.5))
            val dayKind = if (cal.isWeekend() == nowWeekend) 1.0 else 0.6
            val pose = if (l.posture == posture) 1.0 else 0.5

            out.merge(l.key, recency * timeOfDay * dayKind * pose, Double::plus)
        }
        return out
    }

    /** Total launches per app, for sorting search results when two names match equally well. */
    fun counts(): Map<String, Int> = launches.groupingBy { it.key }.eachCount()

    private fun rewrite() {
        runCatching {
            file.writeText(launches.joinToString("") { "${it.key}\t${it.at}\t${it.posture.name}\n" })
        }
    }

    private fun Calendar.isWeekend(): Boolean {
        val d = get(Calendar.DAY_OF_WEEK)
        return d == Calendar.SATURDAY || d == Calendar.SUNDAY
    }

    companion object {
        private const val MAX_EVENTS = 4000
        private const val HALF_LIFE_DAYS = 14.0
        private const val LN2 = 0.6931471805599453
    }
}

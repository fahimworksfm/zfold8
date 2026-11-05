package fm.fahim.foldlauncher

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Your own images (for example ones made in Nano Banana), copied into the launcher's private
 * storage, so they keep working after you delete them from the gallery.
 * Slots: "bg_cover", "bg_open", "bg_tabletop", and "bay_<project id>" for each bay.
 */
class ImageStore(private val context: Context) {
    private val dir = File(context.filesDir, "images").apply { mkdirs() }
    private val cache = HashMap<String, ImageBitmap?>()

    private fun file(slot: String) = File(dir, "$slot.png")

    fun has(slot: String) = file(slot).exists()

    /** Copies a picked image into a slot, scaled so its longest side is at most [maxSide] pixels. */
    suspend fun put(slot: String, uri: Uri, maxSide: Int = 2200): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val longest = max(info.size.width, info.size.height)
                if (longest > maxSide) {
                    val scale = maxSide.toFloat() / longest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            file(slot).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            cache.remove(slot)
            true
        }.getOrDefault(false)
    }

    fun clear(slot: String) {
        file(slot).delete()
        cache.remove(slot)
    }

    suspend fun get(slot: String): ImageBitmap? = cache[slot] ?: withContext(Dispatchers.IO) {
        val f = file(slot)
        val img = if (f.exists()) runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(f)).asImageBitmap()
        }.getOrNull() else null
        cache[slot] = img
        img
    }
}

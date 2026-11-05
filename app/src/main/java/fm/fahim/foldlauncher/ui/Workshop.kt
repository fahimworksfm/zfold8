package fm.fahim.foldlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * The Workshop: where you put your own pictures into the garage and switch the effects on or off.
 * Pictures come from your gallery, so anything you make in Nano Banana and save works here.
 */
@Composable
fun Workshop(
    images: Map<String, ImageBitmap?>,
    prefs: Prefs,
    setPrefs: (Prefs) -> Unit,
    pick: (String) -> Unit,
    clear: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1816), RoundedCornerShape(16.dp))
                .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("WORKSHOP", color = Gold, fontSize = 12.sp, letterSpacing = 3.sp, fontFamily = FontFamily.Monospace)
            Text("Your garage, your pictures", color = Cream, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Pick a background for each screen. The haze and lamps are drawn on top. For a bay's own art, tap the bay.",
                color = Dim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Slot("Cover screen", "Tall, 9:21", images["bg_cover"], { pick("bg_cover") }, { clear("bg_cover") })
            Slot("Open screen", "Nearly square, 1:1", images["bg_open"], { pick("bg_open") }, { clear("bg_open") })
            Slot("Tabletop", "Wide, 16:9 (falls back to the open screen's)", images["bg_tabletop"], { pick("bg_tabletop") }, { clear("bg_tabletop") })

            Spacer(Modifier.height(12.dp))
            Toggle("Garage door", "Rolls up when you come back or unfold the phone", prefs.door) { setPrefs(prefs.copy(door = it)) }
            Toggle("Tilt depth", "The scene shifts as you tilt the phone", prefs.tilt) { setPrefs(prefs.copy(tilt = it)) }

            Text(
                "Done", color = Gold, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.End).clickable(onClick = onDismiss).padding(top = 12.dp, start = 8.dp, end = 8.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun Slot(title: String, hint: String, image: ImageBitmap?, onPick: () -> Unit, onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Glass)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center
        ) {
            if (image != null) Image(image, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Text("+", color = Dim, fontSize = 22.sp)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, color = Cream, fontSize = 15.sp)
            Text(hint, color = Dim, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (image == null) "Pick" else "Change", color = Gold, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onPick).padding(6.dp))
            if (image != null) Text("Clear", color = Dim, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onClear).padding(6.dp))
        }
    }
}

@Composable
private fun Toggle(title: String, hint: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Cream, fontSize = 15.sp)
            Text(hint, color = Dim, fontSize = 12.sp)
        }
        Switch(
            checked = on, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Charcoal, checkedTrackColor = Gold)
        )
    }
}

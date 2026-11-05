package fm.fahim.foldlauncher.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fm.fahim.foldlauncher.Kind
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Gold on charcoal, cream type.
val Charcoal = Color(0xFF121110)
val Gold = Color(0xFFE2B862)
val Cream = Color(0xFFF3EAD8)
val Dim = Color(0xFF8C857A)
val Glass = Color(0x1FFFFFFF)

fun kindColor(kind: Kind) = when (kind) {
    Kind.CLIENT -> Gold
    Kind.OWN -> Color(0xFF7FD1C7)
    Kind.GAME -> Color(0xFFE0607E)
    Kind.PERSONAL -> Color(0xFFF0B5A0)
}

/** Night shift runs 6 pm to 4 am, when most of your building happens. */
fun isNightShift(now: ZonedDateTime = ZonedDateTime.now()) = now.hour >= 18 || now.hour < 4

/** The current time, updated on each new minute. */
@Composable
fun rememberNow(): ZonedDateTime {
    var millis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            millis = System.currentTimeMillis()
            delay(60_000 - millis % 60_000)
        }
    }
    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
}

private const val SMOKE = """
uniform float2 res;
uniform float t;
uniform float density;

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
float noise(float2 p) {
    float2 i = floor(p); float2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1, 0)), f.x),
               mix(hash(i + float2(0, 1)), hash(i + float2(1, 1)), f.x), f.y);
}
float fbm(float2 p) {
    float v = 0.0; float a = 0.5;
    for (int i = 0; i < 5; i++) { v += a * noise(p); p *= 2.03; a *= 0.5; }
    return v;
}
half4 main(float2 fc) {
    float2 uv = fc / res.y;
    float2 q = float2(fbm(uv * 1.4 + float2(0.0, t * 0.035)), fbm(uv * 1.4 + float2(5.2, 1.3) - t * 0.025));
    float s = fbm(uv * 1.1 + q * 1.9 + float2(t * 0.012, -t * 0.02));
    half3 base = half3(0.050, 0.046, 0.043);
    half3 smoke = half3(0.22, 0.20, 0.18);
    half3 gold = half3(0.89, 0.72, 0.38);
    half3 c = mix(base, smoke, half(smoothstep(0.38, 0.9, s) * density));
    c += gold * half(pow(s, 7.0) * 0.55 * density);
    float2 v = fc / res - 0.5;
    c *= half(1.0 - dot(v, v) * 1.1);
    return half4(c, 1.0);
}
"""

/**
 * Slow drifting smoke behind everything, in the spirit of NeverForget's background.
 * It runs at 15 frames a second and stops whenever the launcher is out of sight.
 * Phones older than Android 13 get a plain charcoal gradient.
 */
@Composable
fun Modifier.smokeBackground(nightShift: Boolean): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) smokeShaderImpl(if (nightShift) 1f else 0.6f)
    else background(Brush.verticalGradient(listOf(Color(0xFF1A1816), Charcoal)))

@androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Modifier.smokeShaderImpl(density: Float): Modifier {
    val shader = remember { RuntimeShader(SMOKE) }
    val brush = remember(shader) { ShaderBrush(shader) }
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = System.nanoTime()
        while (true) {
            t = (System.nanoTime() - start) / 1e9f
            delay(66)
        }
    }
    return drawBehind {
        shader.setFloatUniform("res", size.width, size.height)
        shader.setFloatUniform("t", t)
        shader.setFloatUniform("density", density)
        drawRect(brush)
    }
}

/**
 * A dial clock tilted back a little for depth. The gold ticks fill as the hour goes by,
 * and a bright bead marks the hour. The digital time sits in the middle.
 */
@Composable
fun DialClock(now: ZonedDateTime, size: Dp, secondZone: ZoneId?, onCycleZone: () -> Unit) {
    val context = LocalContext.current
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val time = now.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm", Locale.getDefault()))
    val date = now.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .size(size)
                    .graphicsLayer { rotationX = 22f; cameraDistance = 14f * density }
            ) {
                val r = this.size.minDimension / 2
                val c = Offset(this.size.width / 2, this.size.height / 2)
                for (i in 0 until 60) {
                    val a = (i / 60.0) * 2 * PI - PI / 2
                    val major = i % 5 == 0
                    val inner = r * if (major) 0.80f else 0.87f
                    val lit = i <= now.minute
                    val color = when {
                        lit -> Gold
                        major -> Cream.copy(alpha = 0.45f)
                        else -> Cream.copy(alpha = 0.15f)
                    }
                    drawLine(
                        color,
                        Offset(c.x + inner * cos(a).toFloat(), c.y + inner * sin(a).toFloat()),
                        Offset(c.x + r * 0.96f * cos(a).toFloat(), c.y + r * 0.96f * sin(a).toFloat()),
                        strokeWidth = if (major) 3.dp.toPx() else 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                val ha = ((now.hour % 12 + now.minute / 60.0) / 12.0) * 2 * PI - PI / 2
                val hr = r * 0.70f
                drawCircle(Gold.copy(alpha = 0.25f), 9.dp.toPx(), Offset(c.x + hr * cos(ha).toFloat(), c.y + hr * sin(ha).toFloat()))
                drawCircle(Gold, 4.dp.toPx(), Offset(c.x + hr * cos(ha).toFloat(), c.y + hr * sin(ha).toFloat()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(time, color = Cream, fontSize = (size.value / 4.2f).sp, fontWeight = FontWeight.Light)
                Text(date.uppercase(), color = Dim, fontSize = 11.sp, letterSpacing = 2.sp)
            }
        }
        val zoneLabel = secondZone?.let {
            val there = now.withZoneSameInstant(it)
            val city = it.id.substringAfterLast('/').replace('_', ' ')
            "$city  " + there.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a", Locale.getDefault()))
        } ?: "+ add a second city"
        Text(
            zoneLabel, color = Dim, fontSize = 12.sp, letterSpacing = 1.sp,
            modifier = Modifier.clickable(onClick = onCycleZone).padding(8.dp)
        )
    }
}

@Composable
fun NightShiftBadge() {
    Text(
        "NIGHT SHIFT", color = Charcoal, fontSize = 10.sp, letterSpacing = 2.sp,
        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .background(Gold, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

package fm.fahim.foldlauncher.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
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

/**
 * One shared clock for every animation on the home screen: smoke, dial, light strips.
 * It ticks 20 times a second while the launcher is visible. Only drawing code reads it,
 * so the screen redraws without rebuilding its layout.
 */
@Composable
fun rememberTicker(): State<Long> {
    val millis = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            millis.longValue = System.currentTimeMillis()
            delay(50)
        }
    }
    return millis
}

private const val GARAGE = """
uniform float2 res;
uniform float t;
uniform float density;
uniform float overlay;
uniform float2 tilt;

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
    float2 p = fc / res;
    float2 uv = fc / res.y + tilt * 0.04;

    // Drifting haze.
    float2 q = float2(fbm(uv * 1.4 + float2(0.0, t * 0.035)), fbm(uv * 1.4 + float2(5.2, 1.3) - t * 0.025));
    float s = fbm(uv * 1.1 + q * 1.9 + float2(t * 0.012, -t * 0.02));
    float fog = smoothstep(0.35, 0.9, s) * density;

    // Three sodium lamps in the ceiling, throwing cones of light down through the haze.
    // Now and then one of them stutters, like an old garage tube.
    float light = 0.0;
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float x = 0.18 + 0.32 * fi - tilt.x * 0.03;
        float width = 0.05 + 0.32 * p.y;
        float cone = smoothstep(width, 0.0, abs(p.x - x)) * (1.0 - p.y * 0.85);
        float stutter = fract(t * 0.07 + fi * 0.31) > 0.985 ? 0.35 : 1.0;
        float hot = exp(-pow(length((p - float2(x, 0.0)) * float2(1.0, 3.0)) * 9.0, 2.0));
        light += (cone * (0.55 + fog * 1.2) + hot * 1.5) * stutter;
    }

    // Wet concrete: the bottom of the screen picks up streaks of reflected lamp light.
    float floorMask = smoothstep(0.62, 1.0, p.y);
    float streak = 0.0;
    for (int i = 0; i < 3; i++) {
        float x = 0.18 + 0.32 * float(i) + tilt.x * 0.05;
        streak += exp(-pow((p.x - x) * 22.0, 2.0)) * (0.6 + 0.4 * noise(float2(p.x * 40.0, p.y * 6.0 - t * 0.2)));
    }
    streak *= floorMask * 0.5;

    half3 lamp = half3(1.0, 0.76, 0.42);
    half3 haze = half3(0.24, 0.22, 0.20);
    half3 base = half3(0.045, 0.042, 0.040);
    half3 c = base + haze * half(fog * 0.8) + lamp * half(light * 0.22 + streak * 0.5);
    float2 v = p - 0.5;
    float vignette = 1.0 - dot(v, v) * 1.2;

    if (overlay > 0.5) {
        // Over your own picture: only the haze, lamps and reflections, and a darker edge.
        half3 add = haze * half(fog * 0.8) + lamp * half(light * 0.22 + streak * 0.5);
        float a = clamp(fog * 0.45 + light * 0.18 + (1.0 - vignette) * 0.9, 0.0, 0.9);
        return half4(add, half(a));
    }
    return half4(c * half(vignette), 1.0);
}
"""

/**
 * The underground garage drawn behind everything: drifting haze, three ceiling lamps that now and then
 * flicker, and reflections on a wet floor. With your own picture set, it draws the picture with the haze
 * and lamps on top. Tilting the phone moves the light slightly. Night shift thickens the haze.
 */
@Composable
fun GarageBackdrop(nightShift: Boolean, ticker: State<Long>, tilt: State<Offset>, image: ImageBitmap?, modifier: Modifier = Modifier) {
    val start = remember { System.currentTimeMillis() }
    Box(modifier.fillMaxSize().background(Charcoal)) {
        if (image != null) {
            Image(
                image, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // A little bigger than the screen so tilting can reveal the edges.
                        scaleX = 1.08f; scaleY = 1.08f
                        translationX = -tilt.value.x * 18.dp.toPx()
                        translationY = -tilt.value.y * 18.dp.toPx()
                    }
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shader = remember { RuntimeShader(GARAGE) }
            val brush = remember(shader) { ShaderBrush(shader) }
            Spacer(
                Modifier.fillMaxSize().drawBehind {
                    shader.setFloatUniform("res", size.width, size.height)
                    shader.setFloatUniform("t", (ticker.value - start) / 1000f)
                    shader.setFloatUniform("density", if (nightShift) 1f else 0.6f)
                    shader.setFloatUniform("overlay", if (image != null) 1f else 0f)
                    shader.setFloatUniform("tilt", tilt.value.x, tilt.value.y)
                    drawRect(brush)
                }
            )
        } else if (image == null) {
            Spacer(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A1816), Charcoal))))
        }
    }
}

/**
 * A dial clock, tilted back for depth. The gold ticks fill as the hour passes, a bead marks the hour,
 * and a comet with a fading tail sweeps the ring once a minute. The digital time sits in the middle.
 */
@Composable
fun DialClock(now: ZonedDateTime, ticker: State<Long>, size: Dp, secondZone: ZoneId?, onCycleZone: () -> Unit, tilt: State<Offset>) {
    val context = LocalContext.current
    val is24h = android.text.format.DateFormat.is24HourFormat(context)
    val time = now.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm", Locale.getDefault()))
    val date = now.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .size(size)
                    .graphicsLayer {
                        rotationX = 22f + tilt.value.y * 8f
                        rotationY = tilt.value.x * 10f
                        cameraDistance = 14f * density
                    }
            ) {
                val r = this.size.minDimension / 2
                val c = Offset(this.size.width / 2, this.size.height / 2)
                fun at(angle: Double, radius: Float) = Offset(c.x + radius * cos(angle).toFloat(), c.y + radius * sin(angle).toFloat())

                // A faint glow under the whole dial.
                drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = 0.10f), Color.Transparent), c, r * 1.1f), r * 1.1f, c)

                for (i in 0 until 60) {
                    val a = (i / 60.0) * 2 * PI - PI / 2
                    val major = i % 5 == 0
                    val lit = i <= now.minute
                    val color = when {
                        lit -> Gold
                        major -> Cream.copy(alpha = 0.45f)
                        else -> Cream.copy(alpha = 0.15f)
                    }
                    drawLine(
                        color, at(a, r * if (major) 0.80f else 0.87f), at(a, r * 0.96f),
                        strokeWidth = if (major) 3.dp.toPx() else 1.5.dp.toPx(), cap = StrokeCap.Round,
                    )
                }

                // The seconds comet.
                val ms = ticker.value % 60_000
                val head = (ms / 60_000.0) * 2 * PI - PI / 2
                for (k in 0 until 18) {
                    val a = head - k * 0.035
                    drawCircle(Gold.copy(alpha = (1f - k / 18f) * 0.55f), (3.2f - k * 0.15f).dp.toPx(), at(a, r * 1.03f))
                }
                drawCircle(Cream, 2.6.dp.toPx(), at(head, r * 1.03f))

                val ha = ((now.hour % 12 + now.minute / 60.0) / 12.0) * 2 * PI - PI / 2
                drawCircle(Gold.copy(alpha = 0.25f), 9.dp.toPx(), at(ha, r * 0.70f))
                drawCircle(Gold, 4.dp.toPx(), at(ha, r * 0.70f))
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

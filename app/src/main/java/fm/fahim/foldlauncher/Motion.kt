package fm.fahim.foldlauncher

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * The phone's physical movement: how far the hinge is open, and how the phone is tilted.
 * Listens only while the launcher is on screen, and stops when it isn't.
 */
class Motion(context: Context, private val onHinge: (Float) -> Unit, private val onTilt: (Float, Float) -> Unit) :
    SensorEventListener {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val hinge: Sensor? = sensors.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
    private val rotation: Sensor? = sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val matrix = FloatArray(9)
    private val angles = FloatArray(3)
    private var baseline: FloatArray? = null
    private var tx = 0f
    private var ty = 0f

    val hasHinge get() = hinge != null

    fun start(tilt: Boolean) {
        hinge?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        if (tilt) rotation?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        baseline = null
    }

    fun stop() = sensors.unregisterListener(this)

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_HINGE_ANGLE -> onHinge(e.values[0])
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(matrix, e.values)
                SensorManager.getOrientation(matrix, angles)
                // Tilt is measured from how you were holding the phone when the launcher appeared,
                // and the neutral point slowly follows you, so the scene always settles back to centre.
                val base = baseline ?: angles.copyOf().also { baseline = it }
                base[1] += (angles[1] - base[1]) * 0.01f
                base[2] += (angles[2] - base[2]) * 0.01f
                val pitch = (angles[1] - base[1]).coerceIn(-0.35f, 0.35f) / 0.35f
                val roll = (angles[2] - base[2]).coerceIn(-0.35f, 0.35f) / 0.35f
                tx += (roll - tx) * 0.15f
                ty += (pitch - ty) * 0.15f
                onTilt(tx, ty)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

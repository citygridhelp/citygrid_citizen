package com.example.potholereport.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs

/**
 * Live device compass / rotation heading (degrees, 0 = north, clockwise).
 * Used so the trip vehicle cursor turns when the phone turns, not only from GPS course.
 */
class TripDeviceHeadingTracker(
    context: Context,
    private val onHeadingDeg: (Float) -> Unit,
) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var lastEmittedDeg = Float.NaN
    private var started = false

    fun start() {
        if (started) return
        val sensor = rotationSensor ?: return
        started = sensorManager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_UI,
        )
    }

    fun stop() {
        if (!started) return
        sensorManager.unregisterListener(this)
        started = false
        lastEmittedDeg = Float.NaN
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return
        if (values.size < 3) return
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        val displayRotation = currentDisplayRotation()
        val (axisX, axisY) = when (displayRotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        if (!SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)) {
            return
        }
        SensorManager.getOrientation(remappedMatrix, orientation)
        var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
        if (azimuthDeg < 0f) azimuthDeg += 360f
        azimuthDeg %= 360f
        if (lastEmittedDeg.isFinite() &&
            abs(normalizeHeadingDelta(azimuthDeg - lastEmittedDeg)) < MIN_EMIT_DELTA_DEG
        ) {
            return
        }
        lastEmittedDeg = azimuthDeg
        onHeadingDeg(azimuthDeg)
    }

    @Suppress("DEPRECATION")
    private fun currentDisplayRotation(): Int {
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return wm.defaultDisplay.rotation
    }

    companion object {
        /** Ignore sub-degree jitter so Compose/map updates stay light. */
        private const val MIN_EMIT_DELTA_DEG = 2f
    }
}

/** Prefer GPS course when moving; otherwise device compass (phone turn). */
fun fuseNavHeadingDeg(
    deviceHeadingDeg: Float?,
    gpsBearingDeg: Float? = null,
    gpsSpeedMps: Float? = null,
): Float? {
    val compass = deviceHeadingDeg?.takeIf { it.isFinite() }
    val movingFastEnough =
        gpsSpeedMps != null &&
            gpsSpeedMps.isFinite() &&
            gpsSpeedMps >= GPS_COURSE_MIN_SPEED_M_S
    val gpsCourse = gpsBearingDeg
        ?.takeIf { it.isFinite() && movingFastEnough }
    // While moving, trust GPS course only — magnetometer swings flip the map.
    if (movingFastEnough) return gpsCourse ?: compass
    return compass ?: gpsBearingDeg?.takeIf { it.isFinite() }
}

fun fuseNavHeadingDeg(
    deviceHeadingDeg: Float?,
    gps: Location?,
): Float? = fuseNavHeadingDeg(
    deviceHeadingDeg = deviceHeadingDeg,
    gpsBearingDeg = gps?.takeIf { it.hasBearing() }?.bearing,
    gpsSpeedMps = gps?.takeIf { it.hasSpeed() }?.speed,
)

/** Copy [gps] with fused bearing for map follow / icon rotation. */
fun Location.withFusedNavHeading(deviceHeadingDeg: Float?): Location {
    val heading = fuseNavHeadingDeg(deviceHeadingDeg, this) ?: return this
    return Location(this).apply { bearing = heading }
}

fun normalizeHeadingDelta(deltaDeg: Float): Float {
    var d = deltaDeg % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return d
}

/** Above this, GPS course-over-ground is usually steadier than the magnetometer. ~7 km/h. */
private const val GPS_COURSE_MIN_SPEED_M_S = 2.0f

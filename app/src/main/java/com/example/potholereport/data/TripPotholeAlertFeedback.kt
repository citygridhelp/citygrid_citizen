package com.example.potholereport.data

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Distance at which a pothole first triggers sound/vibration once. */
const val TRIP_POTHOLE_NOTIFY_WITHIN_M = 150.0

/** Human-readable side for the trip banner. */
fun formatTripPotholeSide(sideLabel: String?): String? {
    val raw = sideLabel?.trim().orEmpty()
    if (raw.isEmpty()) return null
    return when (raw.lowercase()) {
        "left", "l" -> "on your left"
        "right", "r" -> "on your right"
        "middle", "m", "center", "centre" -> "in the middle of the road"
        "full width", "fullwidth", "full road", "full road pothole", "f",
        "across", "across the road", "whole road", "crater",
        -> "across the road"
        else -> raw
    }
}

/** Full trip-banner line for the next pothole alert. */
fun formatTripPotholeAlertLine(alert: TripPotholeAlert): String {
    val severity = alert.report.severity.title
    val distance = alert.distanceMeters.toInt().coerceAtLeast(0)
    val side = formatTripPotholeSide(alert.sideLabel)?.let { " · $it" }.orEmpty()
    return "Next $severity pothole in $distance m$side"
}

/** Banner / capsule accent by severity — light red → full red. */
fun tripPotholeAlertAccentArgb(severity: PotholeSeverity): Int =
    when (severity) {
        PotholeSeverity.MINOR -> 0xFFFECACA.toInt()      // pale rose
        PotholeSeverity.MODERATE -> 0xFFF87171.toInt()   // light red
        PotholeSeverity.SEVERE -> 0xFFEF4444.toInt()     // red
        PotholeSeverity.CRITICAL -> 0xFFDC2626.toInt()   // strong red
    }

/**
 * Plays a short vibration + tone once. Safe to call from the main thread;
 * failures are ignored (no hardware / muted device).
 */
fun notifyTripPotholeAlert(context: Context, severity: PotholeSeverity) {
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            val pattern = when (severity) {
                PotholeSeverity.CRITICAL, PotholeSeverity.SEVERE ->
                    longArrayOf(0, 120, 60, 120)
                else ->
                    longArrayOf(0, 80)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }
    runCatching {
        val tone = when (severity) {
            PotholeSeverity.CRITICAL, PotholeSeverity.SEVERE -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
            else -> ToneGenerator.TONE_PROP_BEEP
        }
        val durationMs = when (severity) {
            PotholeSeverity.CRITICAL, PotholeSeverity.SEVERE -> 220
            else -> 140
        }
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
        tg.startTone(tone, durationMs)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            runCatching { tg.release() }
        }, (durationMs + 80).toLong())
    }
}

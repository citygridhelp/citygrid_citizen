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

/**
 * Compact lane code for capsule, map pins, and markers:
 * L / M / R / LM / MR / LMR.
 */
fun formatTripPotholeSideLetter(sideLabel: String?): String? =
    when (TripNavigationMatcher.normalizeSideKey(sideLabel)) {
        "left" -> "L"
        "right" -> "R"
        "middle" -> "M"
        "lm" -> "LM"
        "mr" -> "MR"
        "full" -> "LMR"
        else -> null
    }

/** Spoken / banner side phrase — same keys as [formatTripPotholeSideLetter]. */
fun formatTripPotholeSide(sideLabel: String?): String? {
    val key = TripNavigationMatcher.normalizeSideKey(sideLabel)
    if (key != null) {
        return when (key) {
            "left" -> "on your left"
            "right" -> "on your right"
            "middle" -> "in the middle of the road"
            "lm" -> "between the left and middle of the road"
            "mr" -> "between the middle and right of the road"
            "full" -> "across the road"
            else -> null
        }
    }
    val raw = sideLabel?.trim().orEmpty()
    return raw.takeIf { it.isNotEmpty() }
}

/** Full trip-banner / voice line for the next pothole alert. */
fun formatTripPotholeAlertLine(alert: TripPotholeAlert): String {
    val phrase = alert.report.severity.driverPhrase
    val distance = alert.distanceMeters.toInt().coerceAtLeast(0)
    val side = formatTripPotholeSide(alert.sideLabel)?.let { " · $it" }.orEmpty()
    return "Next $phrase in $distance m$side"
}

/** Marker / blink fill by severity — light red → full red. */
fun tripPotholeAlertAccentArgb(severity: PotholeSeverity): Int =
    when (severity) {
        PotholeSeverity.MINOR -> 0xFFFECACA.toInt() // pale rose
        PotholeSeverity.MODERATE -> 0xFFF87171.toInt() // light red
        PotholeSeverity.SEVERE -> 0xFFEF4444.toInt() // red
        PotholeSeverity.CRITICAL -> 0xFFDC2626.toInt() // strong red
    }

/**
 * Readable ink on white for capsule / banner severity text.
 * Same severity ladder as [tripPotholeAlertAccentArgb], darker for contrast.
 */
fun tripPotholeAlertInkArgb(severity: PotholeSeverity): Int =
    when (severity) {
        PotholeSeverity.MINOR -> 0xFFB45309.toInt() // amber caution
        PotholeSeverity.MODERATE -> 0xFFDC2626.toInt()
        PotholeSeverity.SEVERE -> 0xFFB91C1C.toInt() // same as Critical for outdoor contrast
        PotholeSeverity.CRITICAL -> 0xFFB91C1C.toInt()
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

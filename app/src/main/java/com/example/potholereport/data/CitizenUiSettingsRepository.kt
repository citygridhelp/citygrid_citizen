package com.example.potholereport.data

import android.content.Context
import android.graphics.Color as AndroidColor

/** Map navigation cursor shape. */
enum class MapCursorStyle(val storageKey: String, val label: String) {
    ARROW("arrow", "Arrow"),
    CAR("car", "Car"),
    BIKE("bike", "Bike"),
    ;

    companion object {
        fun fromStorage(key: String?): MapCursorStyle = when (key) {
            // Legacy "vehicle" → rear-view car preset.
            "vehicle", CAR.storageKey -> CAR
            BIKE.storageKey -> BIKE
            ARROW.storageKey -> ARROW
            else -> CAR
        }
    }
}

/** Accent color for the trip map cursor. */
enum class MapCursorColor(val storageKey: String, val label: String, val argb: Int) {
    BLUE("blue", "Blue", AndroidColor.parseColor("#2563EB")),
    RED("red", "Red", AndroidColor.parseColor("#DC2626")),
    YELLOW("yellow", "Yellow", AndroidColor.parseColor("#EAB308")),
    GREEN("green", "Green", AndroidColor.parseColor("#16A34A")),
    ORANGE("orange", "Orange", AndroidColor.parseColor("#EA580C")),
    PURPLE("purple", "Purple", AndroidColor.parseColor("#7C3AED")),
    ;

    companion object {
        fun fromStorage(key: String?): MapCursorColor =
            entries.firstOrNull { it.storageKey == key } ?: BLUE
    }
}

/**
 * Local UI preferences (profile → Settings). Device-scoped; not synced to cloud yet.
 */
object CitizenUiSettingsRepository {

    private const val PREFS = "citizen_ui_settings"
    private const val KEY_CURSOR_STYLE = "map_cursor_style"
    private const val KEY_CURSOR_COLOR = "map_cursor_color"
    private const val KEY_CAPTURE_DISPLAY = "trip_capture_display"
    private const val KEY_CAPTURE_LAYOUT = "trip_capture_camera_map_layout"
    private const val KEY_CAPTURE_ROAD = "trip_capture_road_source"
    private const val KEY_CAPTURE_CABIN = "trip_capture_cabin_source"
    private const val KEY_DEVICE_ROLE = "trip_device_role"
    private const val KEY_COMPANION_ROLE = "trip_companion_role"
    private const val KEY_COMPANION_CODE = "trip_companion_pairing_code"
    private const val KEY_IMU_BUMP_ENABLED = "imu_bump_detection_enabled"
    private const val KEY_SWERVE_ENABLED = "imu_swerve_detection_enabled"
    private const val KEY_ROAD_VIDEO_AUTO_DETECT = "road_video_auto_detect_enabled"
    private const val KEY_WEATHER_ALERTS_ENABLED = "weather_alerts_enabled"
    private const val KEY_WEATHER_ALERT_CADENCE = "weather_alert_cadence"
    private const val KEY_WEATHER_MAP_CADENCE = "weather_map_cadence"
    private const val KEY_EMAIL_NOTIFICATIONS = "email_notifications_enabled"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun mapCursorStyle(context: Context): MapCursorStyle =
        MapCursorStyle.fromStorage(
            prefs(context).getString(KEY_CURSOR_STYLE, MapCursorStyle.CAR.storageKey),
        )

    fun mapCursorColor(context: Context): MapCursorColor =
        MapCursorColor.fromStorage(
            prefs(context).getString(KEY_CURSOR_COLOR, MapCursorColor.BLUE.storageKey),
        )

    fun setMapCursorStyle(context: Context, style: MapCursorStyle) {
        prefs(context).edit().putString(KEY_CURSOR_STYLE, style.storageKey).apply()
    }

    fun setMapCursorColor(context: Context, color: MapCursorColor) {
        prefs(context).edit().putString(KEY_CURSOR_COLOR, color.storageKey).apply()
    }

    // --- Trip capture (#32 Phase 0 shell; Phase 1+ honors these) ---

    fun tripCaptureDisplayMode(context: Context): TripCaptureDisplayMode =
        TripCaptureDisplayMode.fromStorage(
            prefs(context).getString(KEY_CAPTURE_DISPLAY, TripCaptureDisplayMode.MAP_ONLY.storageKey),
        )

    fun tripCaptureCameraMapLayout(context: Context): TripCaptureCameraMapLayout =
        TripCaptureCameraMapLayout.fromStorage(
            prefs(context).getString(KEY_CAPTURE_LAYOUT, TripCaptureCameraMapLayout.SWITCH.storageKey),
        )

    fun tripCaptureRoadSource(context: Context): TripCaptureSource {
        val stored = TripCaptureSource.fromStorage(
            prefs(context).getString(KEY_CAPTURE_ROAD, TripCaptureSource.PHONE_BACK.storageKey),
        )
        if (TripFeatureGates.isRoadSourceEnabled(stored)) return stored
        // WIP gates: fall back to phone back and clear stale preference.
        prefs(context).edit()
            .putString(KEY_CAPTURE_ROAD, TripCaptureSource.PHONE_BACK.storageKey)
            .apply()
        return TripCaptureSource.PHONE_BACK
    }

    fun tripCaptureCabinSource(context: Context): TripCaptureCabinSource {
        val stored = TripCaptureCabinSource.fromStorage(
            prefs(context).getString(KEY_CAPTURE_CABIN, TripCaptureCabinSource.OFF.storageKey),
        )
        if (TripFeatureGates.isCabinSourceEnabled(stored)) return stored
        prefs(context).edit()
            .putString(KEY_CAPTURE_CABIN, TripCaptureCabinSource.OFF.storageKey)
            .apply()
        return TripCaptureCabinSource.OFF
    }

    fun setTripCaptureDisplayMode(context: Context, mode: TripCaptureDisplayMode) {
        prefs(context).edit().putString(KEY_CAPTURE_DISPLAY, mode.storageKey).apply()
    }

    fun setTripCaptureCameraMapLayout(context: Context, layout: TripCaptureCameraMapLayout) {
        if (!layout.selectable) return
        prefs(context).edit().putString(KEY_CAPTURE_LAYOUT, layout.storageKey).apply()
    }

    fun setTripCaptureRoadSource(context: Context, source: TripCaptureSource) {
        if (!TripFeatureGates.isRoadSourceEnabled(source)) return
        val editor = prefs(context).edit()
            .putString(KEY_CAPTURE_ROAD, source.storageKey)
        val needsLiveCamera =
            source == TripCaptureSource.USB_1 ||
                source == TripCaptureSource.USB_2 ||
                source == TripCaptureSource.COMPANION
        if (needsLiveCamera &&
            tripCaptureDisplayMode(context) == TripCaptureDisplayMode.MAP_ONLY
        ) {
            editor.putString(KEY_CAPTURE_DISPLAY, TripCaptureDisplayMode.CAMERA.storageKey)
        }
        if (source == TripCaptureSource.COMPANION) {
            editor.putString(KEY_DEVICE_ROLE, TripDeviceRole.NAV_AND_REPORT.storageKey)
            editor.putString(KEY_COMPANION_ROLE, TripCompanionRole.NAVIGATOR.storageKey)
        }
        editor.apply()
    }

    /** Select USB camera 1 only when a real UVC webcam is present — never a second phone. */
    fun activateUsbCamera1ForTripCapture(context: Context) {
        if (!TripFeatureGates.USB_CAMERA_ENABLED) return
        if (tripDeviceRole(context).usesCompanionPairing) return
        val stored = TripCaptureSource.fromStorage(
            prefs(context).getString(KEY_CAPTURE_ROAD, TripCaptureSource.PHONE_BACK.storageKey),
        )
        if (stored == TripCaptureSource.COMPANION) return
        val hasUvc = TripUsbCameraInventory.listVideoCandidates(context).any { it.likelyUvc }
        if (!hasUvc) return
        setTripCaptureRoadSource(context, TripCaptureSource.USB_1)
    }

    fun setTripCaptureCabinSource(context: Context, source: TripCaptureCabinSource) {
        if (!TripFeatureGates.isCabinSourceEnabled(source)) return
        prefs(context).edit().putString(KEY_CAPTURE_CABIN, source.storageKey).apply()
    }

    fun tripCompanionRole(context: Context): TripCompanionRole =
        TripCompanionRole.fromStorage(
            prefs(context).getString(KEY_COMPANION_ROLE, TripCompanionRole.OFF.storageKey),
        )

    /**
     * Phase B product role. Prefers [KEY_DEVICE_ROLE]; otherwise migrates from
     * the older companion-role pref.
     */
    fun tripDeviceRole(context: Context): TripDeviceRole {
        val stored = prefs(context).getString(KEY_DEVICE_ROLE, null)
        val role = if (stored != null) {
            TripDeviceRole.fromStorage(stored)
        } else {
            TripDeviceRole.fromCompanionRole(tripCompanionRole(context))
        }
        if (TripFeatureGates.isDeviceRoleEnabled(role)) return role
        // Companion roles WIP — stay on single-phone Both.
        applyTripDeviceRole(context, TripDeviceRole.BOTH)
        return TripDeviceRole.BOTH
    }

    /**
     * Applies a device role: syncs companion role and sensible road defaults.
     * Does not change display/layout modes.
     */
    fun applyTripDeviceRole(context: Context, role: TripDeviceRole) {
        val effective =
            if (TripFeatureGates.isDeviceRoleEnabled(role)) role else TripDeviceRole.BOTH
        val editor = prefs(context).edit()
            .putString(KEY_DEVICE_ROLE, effective.storageKey)
            .putString(KEY_COMPANION_ROLE, effective.companionRole.storageKey)
        when (effective) {
            TripDeviceRole.NAV_AND_REPORT ->
                if (TripFeatureGates.COMPANION_CAMERA_ENABLED) {
                    editor.putString(KEY_CAPTURE_ROAD, TripCaptureSource.COMPANION.storageKey)
                } else {
                    editor.putString(KEY_CAPTURE_ROAD, TripCaptureSource.PHONE_BACK.storageKey)
                }
            TripDeviceRole.BOTH -> {
                val road = TripCaptureSource.fromStorage(
                    prefs(context).getString(KEY_CAPTURE_ROAD, TripCaptureSource.PHONE_BACK.storageKey),
                )
                if (!TripFeatureGates.isRoadSourceEnabled(road) ||
                    road == TripCaptureSource.COMPANION
                ) {
                    editor.putString(KEY_CAPTURE_ROAD, TripCaptureSource.PHONE_BACK.storageKey)
                }
            }
            TripDeviceRole.CAMERA_ONLY -> Unit
        }
        editor.apply()
    }

    fun tripCompanionPairingCode(context: Context): String =
        TripCompanionPairing.normalize(
            prefs(context).getString(KEY_COMPANION_CODE, null),
        ).ifBlank {
            TripCompanionPairing.generateCode().also { setTripCompanionPairingCode(context, it) }
        }

    fun setTripCompanionRole(context: Context, role: TripCompanionRole) {
        if (!TripFeatureGates.COMPANION_CAMERA_ENABLED) {
            applyTripDeviceRole(context, TripDeviceRole.BOTH)
            return
        }
        prefs(context).edit()
            .putString(KEY_COMPANION_ROLE, role.storageKey)
            .putString(KEY_DEVICE_ROLE, TripDeviceRole.fromCompanionRole(role).storageKey)
            .apply()
        if (role == TripCompanionRole.NAVIGATOR) {
            setTripCaptureRoadSource(context, TripCaptureSource.COMPANION)
        }
    }

    fun setTripCompanionPairingCode(context: Context, code: String) {
        prefs(context).edit()
            .putString(KEY_COMPANION_CODE, TripCompanionPairing.normalize(code))
            .apply()
    }

    /** #33 Phase C — IMU bump sensing while Navigate is on (gated while WIP). */
    fun imuBumpDetectionEnabled(context: Context): Boolean {
        if (!TripFeatureGates.IMU_BUMP_ENABLED) return false
        return prefs(context).getBoolean(KEY_IMU_BUMP_ENABLED, true)
    }

    fun setImuBumpDetectionEnabled(context: Context, enabled: Boolean) {
        if (!TripFeatureGates.IMU_BUMP_ENABLED) {
            prefs(context).edit().putBoolean(KEY_IMU_BUMP_ENABLED, false).apply()
            return
        }
        prefs(context).edit().putBoolean(KEY_IMU_BUMP_ENABLED, enabled).apply()
    }

    /** #33 Phase G — avoid/swerve heading crook (default on). */
    fun swerveDetectionEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SWERVE_ENABLED, true)

    fun setSwerveDetectionEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SWERVE_ENABLED, enabled).apply()
    }

    /**
     * While Navigate + road video are on, periodically YOLO-score keyframes and
     * auto-capture potholes (High → report, Medium → Unconfirmed). Default on.
     */
    fun roadVideoAutoDetectEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ROAD_VIDEO_AUTO_DETECT, true)

    fun setRoadVideoAutoDetectEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ROAD_VIDEO_AUTO_DETECT, enabled).apply()
    }

    /** In-app weather bulletins on the Alerts page. Default on. */
    fun weatherAlertsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WEATHER_ALERTS_ENABLED, true)

    fun setWeatherAlertsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_WEATHER_ALERTS_ENABLED, enabled).apply()
    }

    fun weatherAlertCadence(context: Context): WeatherAlertCadence =
        WeatherAlertCadence.fromAlertStorage(
            prefs(context).getString(KEY_WEATHER_ALERT_CADENCE, WeatherAlertCadence.EVERY_3H.storageKey),
        )

    fun setWeatherAlertCadence(context: Context, cadence: WeatherAlertCadence) {
        val stored = if (cadence.isCurrent) WeatherAlertCadence.EVERY_3H else cadence
        prefs(context).edit().putString(KEY_WEATHER_ALERT_CADENCE, stored.storageKey).apply()
    }

    /** Map overlay + ticker window only. Independent of Alerts / bell cadence. */
    fun weatherMapCadence(context: Context): WeatherAlertCadence =
        WeatherAlertCadence.fromMapStorage(
            prefs(context).getString(KEY_WEATHER_MAP_CADENCE, WeatherAlertCadence.NOW.storageKey),
        )

    fun setWeatherMapCadence(context: Context, cadence: WeatherAlertCadence) {
        prefs(context).edit().putString(KEY_WEATHER_MAP_CADENCE, cadence.storageKey).apply()
    }

    /** Local preference for report-confirmation email. Default on for signed-in users. */
    fun emailNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_EMAIL_NOTIFICATIONS, true)

    fun setEmailNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_EMAIL_NOTIFICATIONS, enabled).apply()
    }
}

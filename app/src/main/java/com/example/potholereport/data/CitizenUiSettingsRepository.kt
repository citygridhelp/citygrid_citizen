package com.example.potholereport.data

import android.content.Context
import android.graphics.Color as AndroidColor

/** Map navigation cursor shape. */
enum class MapCursorStyle(val storageKey: String, val label: String) {
    VEHICLE("vehicle", "Vehicle icon"),
    ARROW("arrow", "Arrow"),
    ;

    companion object {
        fun fromStorage(key: String?): MapCursorStyle =
            entries.firstOrNull { it.storageKey == key } ?: VEHICLE
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

    fun mapCursorStyle(context: Context): MapCursorStyle =
        MapCursorStyle.fromStorage(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_CURSOR_STYLE, MapCursorStyle.VEHICLE.storageKey),
        )

    fun mapCursorColor(context: Context): MapCursorColor =
        MapCursorColor.fromStorage(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_CURSOR_COLOR, MapCursorColor.BLUE.storageKey),
        )

    fun setMapCursorStyle(context: Context, style: MapCursorStyle) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CURSOR_STYLE, style.storageKey)
            .apply()
    }

    fun setMapCursorColor(context: Context, color: MapCursorColor) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CURSOR_COLOR, color.storageKey)
            .apply()
    }
}

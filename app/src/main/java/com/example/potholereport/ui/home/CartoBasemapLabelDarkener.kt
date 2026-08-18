package com.example.potholereport.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Darkens CARTO Voyager / Positron **label** pixels in each tile bitmap without a
 * whole-tile ColorMatrix.
 *
 * Labels sit in a mid-grey band; pale road fills stay above that band and are left alone.
 * Avoids contrast filters that fade / thin residential streets (especially at high zoom).
 *
 * #25: stronger ink so place / street names stay readable when street-follow overzooms.
 * #25.2 adds Overpass street/place name overlays at high zoom (see StreetNameOverlayClient).
 */
object CartoBasemapLabelDarkener {

    /** Mark bitmaps already remapped so we only pay the cost once per cached tile. */
    private val processed: MutableSet<Bitmap> =
        Collections.newSetFromMap(WeakHashMap())

    fun darkenLabelsInPlace(bitmap: Bitmap) {
        if (!bitmap.isMutable) return
        synchronized(processed) {
            if (bitmap in processed) return
        }

        val w = bitmap.width
        val h = bitmap.height
        val row = IntArray(w)
        for (y in 0 until h) {
            bitmap.getPixels(row, 0, w, 0, y, w, 1)
            var changed = false
            for (x in 0 until w) {
                val c = row[x]
                val next = remapLabelGrey(c)
                if (next != c) {
                    row[x] = next
                    changed = true
                }
            }
            if (changed) {
                bitmap.setPixels(row, 0, w, 0, y, w, 1)
            }
        }
        synchronized(processed) {
            processed.add(bitmap)
        }
    }

    /**
     * Pure remap for one ARGB pixel. Pale roads / background / tinted fills pass through;
     * desaturated mid-grey label ink is scaled toward charcoal (no contrast matrix).
     */
    fun remapLabelGrey(color: Int): Int {
        val a = Color.alpha(color)
        if (a == 0) return color
        return remapLabelChannels(
            a = a,
            r = Color.red(color),
            g = Color.green(color),
            b = Color.blue(color),
        )
    }

    /**
     * Channel-level remap for unit tests (no Android Color stubs).
     * Returns packed ARGB.
     */
    fun remapLabelChannels(a: Int, r: Int, g: Int, b: Int): Int {
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val chroma = maxC - minC
        val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

        // Background + pale road fills / casings (CARTO streets sit very light).
        if (lum >= 198) return packArgb(a, r, g, b)

        // Tinted water / park / land fills.
        if (isTintedFill(r, g, b, chroma, lum)) return packArgb(a, r, g, b)

        // Label ink: low-chroma mid greys (Voyager + Positron). Slightly wider band
        // than v1.0.6 so lighter street names at high zoom still get pulled darker.
        if (chroma > 42 || lum !in 68..188) return packArgb(a, r, g, b)

        // #25 — stronger darken for readable names when tiles are overzoomed (#25.1 + #25.2 contrast).
        val scale = when {
            lum >= 145 -> 0.24f
            lum >= 120 -> 0.30f
            lum >= 95 -> 0.36f
            else -> 0.42f
        }
        val nr = (r * scale).toInt().coerceIn(0, 255)
        val ng = (g * scale).toInt().coerceIn(0, 255)
        val nb = (b * scale).toInt().coerceIn(0, 255)
        return packArgb(a, nr, ng, nb)
    }

    private fun packArgb(a: Int, r: Int, g: Int, b: Int): Int =
        (a and 0xFF shl 24) or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)

    private fun isTintedFill(r: Int, g: Int, b: Int, chroma: Int, lum: Int): Boolean {
        if (chroma < 16) return false
        if (b > r + 16 && b > g + 6 && lum < 210) return true
        if (g > r + 10 && g > b + 5 && lum in 130..230) return true
        if (abs(r - g) < 14 && r > b + 10 && lum in 140..220) return true
        return false
    }
}

/**
 * Remaps label greys on each tile bitmap once, with **no** ColorFilter
 * (ColorFilters also reshape pale road pixels).
 */
class CartoLabelDarkeningTilesOverlay(
    tileProvider: MapTileProviderBase,
    context: Context,
    horizontalWrapEnabled: Boolean,
    verticalWrapEnabled: Boolean,
) : TilesOverlay(tileProvider, context, horizontalWrapEnabled, verticalWrapEnabled) {

    override fun onTileReadyToDraw(c: Canvas, currentMapTile: Drawable, tileRect: Rect) {
        if (currentMapTile is BitmapDrawable) {
            // Bilinear when overzooming past native tile levels — softer road edges
            // and less blocky label strokes at street-follow zoom.
            currentMapTile.isFilterBitmap = true
            currentMapTile.bitmap?.let { bmp ->
                if (bmp.isMutable) {
                    CartoBasemapLabelDarkener.darkenLabelsInPlace(bmp)
                }
            }
        }
        currentMapTile.colorFilter = null
        super.onTileReadyToDraw(c, currentMapTile, tileRect)
    }
}

/** Swap in label-darkening tiles overlay; keeps the same tile provider. */
fun MapView.installCartoLabelDarkeningTilesOverlay() {
    val provider = tileProvider ?: return
    val next = CartoLabelDarkeningTilesOverlay(
        provider,
        context,
        horizontalWrapEnabled = false,
        verticalWrapEnabled = false,
    )
    next.setLoadingBackgroundColor(Color.WHITE)
    next.setLoadingLineColor(Color.TRANSPARENT)
    next.setColorFilter(null)
    overlayManager.tilesOverlay = next
}

/**
 * Prefer native street detail over overscaled tiles on the home / city map.
 * Soft-zooming past this range makes residential streets vanish — keep camera here.
 */
const val CartoBasemapUsefulMaxZoom = 18.0

/**
 * Highest zoom CARTO Voyager raster tiles are served at.
 * Trip street-follow may zoom the camera past this; native tiles up to here
 * keep road boundaries sharper than stretching z18 alone.
 */
const val CartoBasemapNativeMaxZoom = 20.0

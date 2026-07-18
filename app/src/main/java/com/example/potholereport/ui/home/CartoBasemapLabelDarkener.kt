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
 * Darkens CARTO Positron **label** pixels in each tile bitmap without a whole-tile ColorMatrix.
 *
 * Labels sit in a mid-grey band; pale road fills stay above that band and are left alone.
 * Avoids contrast filters that fade / thin residential streets (especially at high zoom).
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

        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val chroma = maxC - minC
        val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

        // Background + pale road fills / casings (CARTO Positron streets sit very light).
        if (lum >= 200) return color

        // Tinted water / park / land fills.
        if (isTintedFill(r, g, b, chroma, lum)) return color

        // Label ink: low-chroma mid greys only — darken by scaling (not contrast stretch).
        if (chroma > 36 || lum !in 78..175) return color

        val scale = when {
            lum >= 140 -> 0.42f
            lum >= 110 -> 0.48f
            else -> 0.55f
        }
        val nr = (r * scale).toInt().coerceIn(0, 255)
        val ng = (g * scale).toInt().coerceIn(0, 255)
        val nb = (b * scale).toInt().coerceIn(0, 255)
        return Color.argb(a, nr, ng, nb)
    }

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
 * Prefer native street detail over overscaled tiles.
 * Soft-zooming past the provider’s sharp range is what makes residential streets vanish.
 */
const val CartoBasemapUsefulMaxZoom = 18.0

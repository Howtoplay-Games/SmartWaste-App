package sg.smartwaste.app.data.places

import sg.smartwaste.app.data.model.Place
import sg.smartwaste.app.util.Geo
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor

/** Live places closer than this to a known place of the same type are treated as duplicates. */
internal const val DUPLICATE_RADIUS_M = 40.0

/** Grid cell edge. Must stay well above the duplicate radius (see [mergePlaces]). */
private const val CELL_M = 100.0
private const val METERS_PER_DEGREE = 111_195.0 // matches the sphere used by Geo.distanceMeters
/** Up to here a 100 m cell is still ≥ 50 m wide east–west, so the 3×3 search can't miss a 40 m neighbour. */
private const val MAX_GRID_LAT = 60.0
private val REF_LAT = Geo.SINGAPORE.lat
private val REF_LNG = Geo.SINGAPORE.lng
private val LNG_SCALE = cos(Math.toRadians(REF_LAT))

/**
 * Returns [base] followed by every [live] place that isn't within [radiusM] of an
 * already-kept place of the same type (live places are also checked against each other,
 * in order).
 *
 * Places are bucketed into a 100 m grid around Singapore, so each live place is only
 * compared with places in its own and the 8 neighbouring cells instead of the whole list.
 * The grid only narrows the candidates; the duplicate test is still the exact
 * [Geo.distanceMeters] check, so the result is the same as comparing against every place.
 * Cells shrink east–west away from the equator, so above ±60° latitude (never the case in
 * Singapore) it falls back to the plain scan to stay exact.
 */
internal fun mergePlaces(base: List<Place>, live: List<Place>, radiusM: Double = DUPLICATE_RADIUS_M): List<Place> {
    require(radiusM <= CELL_M / 2) { "radiusM must be at most ${CELL_M / 2} m" }
    if (base.any { abs(it.point.lat) > MAX_GRID_LAT } || live.any { abs(it.point.lat) > MAX_GRID_LAT }) {
        return linearMerge(base, live, radiusM)
    }
    val result = ArrayList<Place>(base.size + live.size)
    val grid = HashMap<Long, MutableList<Place>>()

    fun keep(p: Place) {
        result.add(p)
        grid.getOrPut(cellKey(cellX(p), cellY(p))) { ArrayList(2) }.add(p)
    }

    base.forEach { keep(it) }
    for (p in live) {
        val cx = cellX(p)
        val cy = cellY(p)
        var duplicate = false
        search@ for (dx in -1..1) {
            for (dy in -1..1) {
                val bucket = grid[cellKey(cx + dx, cy + dy)] ?: continue
                if (bucket.any { it.type == p.type && Geo.distanceMeters(it.point, p.point) < radiusM }) {
                    duplicate = true
                    break@search
                }
            }
        }
        if (!duplicate) keep(p)
    }
    return result
}

/** The original O(n²) scan: compares each live place with every place kept so far. */
private fun linearMerge(base: List<Place>, live: List<Place>, radiusM: Double): List<Place> {
    val result = ArrayList<Place>(base)
    for (p in live) {
        if (result.none { it.type == p.type && Geo.distanceMeters(it.point, p.point) < radiusM }) result.add(p)
    }
    return result
}

private fun cellX(p: Place): Int = floor((p.point.lng - REF_LNG) * LNG_SCALE * METERS_PER_DEGREE / CELL_M).toInt()
private fun cellY(p: Place): Int = floor((p.point.lat - REF_LAT) * METERS_PER_DEGREE / CELL_M).toInt()
private fun cellKey(x: Int, y: Int): Long = (x.toLong() shl 32) or (y.toLong() and 0xffffffffL)

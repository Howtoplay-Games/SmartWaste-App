package sg.smartwaste.app.util

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val lat: Double, val lng: Double)

object Geo {
    /** Geographic centre of Singapore, used when location is unavailable. */
    val SINGAPORE = GeoPoint(1.3521, 103.8198)

    private const val EARTH_RADIUS_M = 6_371_000.0

    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
    }

    /** Initial bearing from [a] to [b] in degrees (0..360). */
    fun bearing(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    /** Rough check that a point is inside Singapore's bounding box. */
    fun isInSingapore(p: GeoPoint): Boolean = p.lat in 1.15..1.48 && p.lng in 103.59..104.10

    fun formatDistance(meters: Double): String =
        if (meters < 1000) "${meters.roundToInt()} m"
        else String.format(Locale.US, "%.1f km", meters / 1000.0)

    fun formatDuration(seconds: Double): String {
        val mins = (seconds / 60.0).roundToInt().coerceAtLeast(1)
        return if (mins < 60) "$mins min" else "${mins / 60} h ${mins % 60} min"
    }
}

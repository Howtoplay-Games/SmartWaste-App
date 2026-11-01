package sg.smartwaste.app.util

/**
 * SmartWaste only serves Singapore. A location fix outside it (for example the Android
 * emulator's default fix in Mountain View, California) must not be used as a starting point:
 * distances would read 13,600 km, "Nearest" would rank bins by distance from another
 * continent, and walking routes across an ocean fail.
 */
object ServiceArea {

    /** Message shown when the device reports a location outside Singapore. */
    const val OUTSIDE_MESSAGE = "Your location is outside Singapore, so distances and routes start from the city centre."

    fun contains(point: GeoPoint): Boolean = Geo.isInSingapore(point)

    /** Where distances, "Nearest" and routes start: the user's fix if it's in Singapore, otherwise the city centre. */
    fun origin(user: GeoPoint?): GeoPoint = user?.takeIf(::contains) ?: Geo.SINGAPORE
}

package sg.smartwaste.app.data.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import sg.smartwaste.app.util.Geo
import sg.smartwaste.app.util.GeoPoint
import java.io.IOException
import java.util.Locale

enum class TravelMode(val label: String, val osrmService: String, val fallbackSpeedMps: Double, val googleMode: String) {
    WALK("Walk", "routed-foot", 1.3, "w"),
    CYCLE("Cycle", "routed-bike", 4.2, "b"),
    DRIVE("Drive", "routed-car", 8.5, "d"),
}

data class RouteStep(
    val instruction: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val location: GeoPoint,
    val maneuverType: String,
    val modifier: String?,
)

data class Route(
    val points: List<GeoPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val steps: List<RouteStep>,
    val mode: TravelMode,
    /** True when routing failed and this is a straight-line estimate. */
    val isEstimate: Boolean = false,
)

/**
 * Turn-by-turn routing using the public FOSSGIS OSRM servers (OpenStreetMap data) with
 * separate foot, bike and car profiles. Falls back to a straight-line estimate offline.
 */
class RoutingRepository(private val http: OkHttpClient, private val json: Json) {

    suspend fun route(from: GeoPoint, to: GeoPoint, mode: TravelMode): Route = withContext(Dispatchers.IO) {
        runCatching { fetch(from, to, mode) }.getOrElse { straightLine(from, to, mode) }
    }

    private fun fetch(from: GeoPoint, to: GeoPoint, mode: TravelMode): Route {
        val coords = String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f", from.lng, from.lat, to.lng, to.lat)
        val url = "https://routing.openstreetmap.de/${mode.osrmService}/route/v1/driving/$coords" +
            "?overview=full&geometries=geojson&steps=true"
        val request = Request.Builder().url(url).header("User-Agent", "SmartWasteSG/1.0 (Android)").build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Routing HTTP ${resp.code}")
            val root = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            if (root["code"]?.jsonPrimitive?.contentOrNull != "Ok") throw IOException("Routing failed")
            val route = root["routes"]!!.jsonArray.first().jsonObject
            val geometry = route["geometry"]!!.jsonObject["coordinates"]!!.jsonArray.map {
                val pair = it.jsonArray
                GeoPoint(pair[1].jsonPrimitive.doubleOrNull ?: 0.0, pair[0].jsonPrimitive.doubleOrNull ?: 0.0)
            }
            val steps = route["legs"]?.jsonArray?.flatMap { leg ->
                leg.jsonObject["steps"]?.jsonArray?.map { parseStep(it.jsonObject) } ?: emptyList()
            } ?: emptyList()
            return Route(
                points = geometry,
                distanceMeters = route["distance"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                durationSeconds = route["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                steps = steps,
                mode = mode,
            )
        }
    }

    private fun parseStep(step: JsonObject): RouteStep {
        val maneuver = step["maneuver"]?.jsonObject ?: JsonObject(emptyMap())
        val type = maneuver["type"]?.jsonPrimitive?.contentOrNull ?: "continue"
        val modifier = maneuver["modifier"]?.jsonPrimitive?.contentOrNull
        val exit = maneuver["exit"]?.jsonPrimitive?.intOrNull
        val loc = maneuver["location"]?.jsonArray
        val name = step["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        return RouteStep(
            instruction = instructionFor(type, modifier, name, exit),
            distanceMeters = step["distance"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            durationSeconds = step["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            location = GeoPoint(
                loc?.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: 0.0,
                loc?.getOrNull(0)?.jsonPrimitive?.doubleOrNull ?: 0.0,
            ),
            maneuverType = type,
            modifier = modifier,
        )
    }

    private fun instructionFor(type: String, modifier: String?, name: String, exit: Int?): String {
        val onto = if (name.isNotBlank()) " onto $name" else ""
        val dir = modifier?.replaceFirstChar { it.uppercase() } ?: ""
        return when (type) {
            "depart" -> if (name.isNotBlank()) "Start on $name" else "Start your trip"
            "arrive" -> "You have arrived"
            "turn" -> when (modifier) {
                "uturn" -> "Make a U-turn$onto"
                "straight" -> "Continue straight$onto"
                null -> "Turn$onto"
                else -> "Turn $modifier$onto"
            }
            "new name", "continue" -> if (modifier == null || modifier == "straight") "Continue$onto" else "Keep $modifier$onto"
            "merge" -> "Merge${if (modifier != null) " $modifier" else ""}$onto"
            "on ramp" -> "Take the ramp${if (modifier != null) " on the $modifier" else ""}$onto"
            "off ramp" -> "Take the exit${if (modifier != null) " on the $modifier" else ""}$onto"
            "fork" -> "Keep ${modifier ?: "straight"} at the fork$onto"
            "end of road" -> "At the end of the road, turn ${modifier ?: ""}$onto".replace("  ", " ")
            "roundabout", "rotary" -> "Enter the roundabout" + (exit?.let { " and take exit $it" } ?: "") + onto
            "exit roundabout", "exit rotary" -> "Exit the roundabout$onto"
            else -> "${dir.ifBlank { "Continue" }}$onto"
        }
    }

    private fun straightLine(from: GeoPoint, to: GeoPoint, mode: TravelMode): Route {
        val d = Geo.distanceMeters(from, to)
        // Typical urban detour factor of ~1.3 for the estimate.
        val est = d * 1.3
        val dur = est / mode.fallbackSpeedMps
        return Route(
            points = listOf(from, to),
            distanceMeters = est,
            durationSeconds = dur,
            steps = listOf(
                RouteStep("Head towards the destination (approximate)", est, dur, from, "depart", null),
                RouteStep("You have arrived", 0.0, 0.0, to, "arrive", null),
            ),
            mode = mode,
            isEstimate = true,
        )
    }
}

package sg.smartwaste.app.data.places

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import sg.smartwaste.app.BuildConfig
import sg.smartwaste.app.data.model.BuyerInfo
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.OnlineBuyer
import sg.smartwaste.app.data.model.Place
import sg.smartwaste.app.data.model.PlaceSource
import sg.smartwaste.app.data.model.PlaceType
import sg.smartwaste.app.data.model.PlaceWithDistance
import sg.smartwaste.app.util.Geo
import sg.smartwaste.app.util.GeoPoint
import java.io.IOException

data class PlacesStatus(
    val loading: Boolean = false,
    val osmCount: Int = 0,
    val neaCount: Int = 0,
    val geocoded: Boolean = false,
    val lastError: String? = null,
)

/**
 * Aggregates e-waste drop-off points and buyers from every available source, regardless
 * of which company runs them:
 *  - SmartWaste IoT smart bins (with fill level)
 *  - the bundled, researched Singapore directory (refined via OneMap geocoding)
 *  - OpenStreetMap (Overpass API): any recycling point tagged for electronics/batteries/bulbs,
 *    plus second-hand electronics shops that buy devices
 *  - NEA "E-waste Recycling" GeoJSON from data.gov.sg (when a dataset id is configured)
 */
class PlacesRepository(
    context: Context,
    private val http: OkHttpClient,
    private val json: Json,
) {
    private val cachePrefs = context.getSharedPreferences("geocode_cache", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    private var curated: List<Place> = CuratedPlaces.bins + CuratedPlaces.buyers
    private var osm: List<Place> = emptyList()
    private var nea: List<Place> = emptyList()
    private var lastOsmCenter: GeoPoint? = null
    private var lastOsmFetch = 0L

    private val _places = MutableStateFlow(merge())
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    private val _status = MutableStateFlow(PlacesStatus())
    val status: StateFlow<PlacesStatus> = _status.asStateFlow()

    val onlineBuyers: List<OnlineBuyer> = CuratedPlaces.onlineBuyers

    init {
        curated = curated.map { applyCachedGeocode(it) }
        _places.value = merge()
    }

    fun byId(id: String): Place? = _places.value.firstOrNull { it.id == id }

    /**
     * Nearest places to [from]. Bins reporting >= 90% full are skipped for bins so users
     * never make a wasted trip (as described in the SmartWaste overview).
     */
    fun nearest(
        from: GeoPoint,
        type: PlaceType? = null,
        category: EWasteCategory? = null,
        limit: Int = 10,
        skipFull: Boolean = true,
    ): List<PlaceWithDistance> = _places.value.asSequence()
        .filter { type == null || it.type == type }
        .filter { category == null || category in it.accepts }
        .filter { !(skipFull && it.type == PlaceType.BIN && it.isFull) }
        .map { PlaceWithDistance(it, Geo.distanceMeters(from, it.point)) }
        .sortedBy { it.distanceMeters }
        .take(limit)
        .toList()

    /** Refreshes live sources around [center]. Safe to call often; results are throttled. */
    suspend fun refresh(center: GeoPoint, force: Boolean = false) = mutex.withLock {
        withContext(Dispatchers.IO) {
            _status.update { it.copy(loading = true, lastError = null) }
            var error: String? = null

            // Smart bin telemetry refresh (demo values change through the day).
            _places.value = merge()

            if (!_status.value.geocoded) {
                runCatching { geocodeCurated() }
                    .onSuccess { _status.update { it.copy(geocoded = true) } }
                    .onFailure { Log.w(TAG, "OneMap geocoding failed", it) }
            }

            val moved = lastOsmCenter?.let { Geo.distanceMeters(it, center) > 2_500 } ?: true
            val stale = System.currentTimeMillis() - lastOsmFetch > 15 * 60_000
            if (force || moved || stale) {
                runCatching { fetchOverpass(center, radiusM = 7_000) }
                    .onSuccess {
                        osm = it; lastOsmCenter = center; lastOsmFetch = System.currentTimeMillis()
                    }
                    .onFailure { error = "OpenStreetMap: ${it.message}"; Log.w(TAG, "Overpass failed", it) }
            }

            if (nea.isEmpty() && BuildConfig.DATAGOV_EWASTE_DATASET_ID.isNotBlank()) {
                runCatching { fetchNea(BuildConfig.DATAGOV_EWASTE_DATASET_ID) }
                    .onSuccess { nea = it }
                    .onFailure { Log.w(TAG, "NEA dataset failed", it) }
            }

            _places.value = merge()
            _status.update {
                it.copy(loading = false, osmCount = osm.size, neaCount = nea.size, lastError = error)
            }
        }
    }

    // ---------------------------------------------------------------- merging

    private fun merge(): List<Place> {
        val result = ArrayList<Place>(CuratedPlaces.smartBins())
        result.addAll(curated)
        // Live sources are added only if they're not duplicates of a known place.
        for (p in nea + osm) {
            val dup = result.any { it.type == p.type && Geo.distanceMeters(it.point, p.point) < 40 }
            if (!dup) result.add(p)
        }
        return result
    }

    // --------------------------------------------------------------- OneMap

    private fun applyCachedGeocode(place: Place): Place {
        val postal = place.postalCode ?: return place
        val cached = cachePrefs.getString(postal, null) ?: return place
        val parts = cached.split(",")
        val lat = parts.getOrNull(0)?.toDoubleOrNull() ?: return place
        val lng = parts.getOrNull(1)?.toDoubleOrNull() ?: return place
        return place.copy(point = GeoPoint(lat, lng))
    }

    /** Refines bundled coordinates using OneMap's public search API (no token needed). */
    private fun geocodeCurated() {
        val editor = cachePrefs.edit()
        var changed = false
        curated = curated.map { place ->
            val postal = place.postalCode
            if (postal == null || cachePrefs.contains(postal)) return@map applyCachedGeocode(place)
            val point = runCatching { oneMapSearch(postal) }.getOrNull()
            if (point != null && Geo.isInSingapore(point) && Geo.distanceMeters(point, place.point) < 3_000) {
                editor.putString(postal, "${point.lat},${point.lng}")
                changed = true
                place.copy(point = point)
            } else place
        }
        if (changed) editor.apply()
    }

    private fun oneMapSearch(query: String): GeoPoint? {
        val url = "https://www.onemap.gov.sg/api/common/elastic/search".toHttpUrl().newBuilder()
            .addQueryParameter("searchVal", query)
            .addQueryParameter("returnGeom", "Y")
            .addQueryParameter("getAddrDetails", "Y")
            .addQueryParameter("pageNum", "1")
            .build()
        http.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val root = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            val first = (root["results"] as? JsonArray)?.firstOrNull()?.jsonObject ?: return null
            val lat = first["LATITUDE"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: return null
            val lng = first["LONGITUDE"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: return null
            return GeoPoint(lat, lng)
        }
    }

    // ------------------------------------------------------------- Overpass

    private fun fetchOverpass(center: GeoPoint, radiusM: Int): List<Place> {
        val around = "(around:$radiusM,${center.lat},${center.lng})"
        val recyclingTags = listOf(
            "electrical_appliances", "small_appliances", "batteries", "mobile_phones",
            "computers", "electronics", "light_bulbs", "fluorescent_tubes",
        )
        val query = buildString {
            append("[out:json][timeout:25];(")
            recyclingTags.forEach { append("nwr[\"amenity\"=\"recycling\"][\"recycling:$it\"=\"yes\"]$around;") }
            append("nwr[\"shop\"=\"mobile_phone\"][\"second_hand\"~\"yes|only\"]$around;")
            append("nwr[\"shop\"=\"computer\"][\"second_hand\"~\"yes|only\"]$around;")
            append("nwr[\"shop\"=\"electronics\"][\"second_hand\"~\"yes|only\"]$around;")
            append(");out center tags;")
        }
        val request = Request.Builder()
            .url("https://overpass-api.de/api/interpreter")
            .header("User-Agent", "SmartWasteSG/1.0 (Android)")
            .post(FormBody.Builder().add("data", query).build())
            .build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val root = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            val elements = root["elements"]?.jsonArray ?: return emptyList()
            return elements.mapNotNull { parseOsmElement(it.jsonObject) }
        }
    }

    private fun parseOsmElement(el: JsonObject): Place? {
        val center = el["center"] as? JsonObject
        val lat = el["lat"]?.jsonPrimitive?.doubleOrNull ?: center?.get("lat")?.jsonPrimitive?.doubleOrNull ?: return null
        val lng = el["lon"]?.jsonPrimitive?.doubleOrNull ?: center?.get("lon")?.jsonPrimitive?.doubleOrNull ?: return null
        val tags = (el["tags"] as? JsonObject)?.mapValues { it.value.jsonPrimitive.contentOrNull.orEmpty() } ?: emptyMap()
        val id = "osm-${el["type"]?.jsonPrimitive?.contentOrNull}-${el["id"]?.jsonPrimitive?.contentOrNull}"
        val operator = tags["operator"] ?: tags["brand"]
        val address = listOfNotNull(
            listOfNotNull(tags["addr:housenumber"], tags["addr:street"]).joinToString(" ").ifBlank { null },
            tags["addr:postcode"]?.let { "Singapore $it" },
        ).joinToString(", ").ifBlank { "See map location" }

        val isShop = tags.containsKey("shop")
        return if (isShop) {
            Place(
                id = id,
                name = tags["name"] ?: "Second-hand electronics shop",
                type = PlaceType.BUYER,
                point = GeoPoint(lat, lng),
                address = address,
                postalCode = tags["addr:postcode"],
                operator = operator ?: "Independent shop",
                accepts = setOf(EWasteCategory.DEVICE_WITH_BATTERY, EWasteCategory.COMPONENT),
                source = PlaceSource.OSM,
                openingHours = tags["opening_hours"],
                phone = tags["phone"] ?: tags["contact:phone"],
                website = tags["website"] ?: tags["contact:website"],
                buyer = BuyerInfo("Cash (in-store quote)", "Used phones, computers & electronics"),
            )
        } else {
            val accepts = buildSet {
                if (tags["recycling:batteries"] == "yes") add(EWasteCategory.SMALL_BATTERY)
                if (tags["recycling:light_bulbs"] == "yes" || tags["recycling:fluorescent_tubes"] == "yes") add(EWasteCategory.LAMP)
                if (listOf("mobile_phones", "computers", "electronics", "small_appliances", "electrical_appliances")
                        .any { tags["recycling:$it"] == "yes" }
                ) {
                    add(EWasteCategory.DEVICE_WITH_BATTERY); add(EWasteCategory.APPLIANCE_NO_BATTERY); add(EWasteCategory.COMPONENT)
                }
            }.ifEmpty { setOf(EWasteCategory.DEVICE_WITH_BATTERY) }
            Place(
                id = id,
                name = tags["name"] ?: operator?.let { "E-waste point · $it" } ?: "E-waste recycling point",
                type = PlaceType.BIN,
                point = GeoPoint(lat, lng),
                address = address,
                postalCode = tags["addr:postcode"],
                operator = operator ?: "Community-mapped",
                accepts = accepts,
                source = PlaceSource.OSM,
                openingHours = tags["opening_hours"],
            )
        }
    }

    // ------------------------------------------------------------ NEA data

    private fun fetchNea(datasetId: String): List<Place> {
        val poll = Request.Builder()
            .url("https://api-open.data.gov.sg/v1/public/api/datasets/$datasetId/poll-download")
            .build()
        val downloadUrl = http.newCall(poll).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("poll HTTP ${resp.code}")
            val root = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            root["data"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull ?: throw IOException("No download url")
        }
        http.newCall(Request.Builder().url(downloadUrl).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("download HTTP ${resp.code}")
            val root = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            val features = root["features"]?.jsonArray ?: return emptyList()
            return features.mapIndexedNotNull { index, f -> parseNeaFeature(index, f.jsonObject) }
        }
    }

    private fun parseNeaFeature(index: Int, feature: JsonObject): Place? {
        val coords = feature["geometry"]?.jsonObject?.get("coordinates")?.jsonArray ?: return null
        val lng = coords.getOrNull(0)?.jsonPrimitive?.doubleOrNull ?: return null
        val lat = coords.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: return null
        val props = feature["properties"]?.jsonObject ?: JsonObject(emptyMap())

        // Newer data.gov.sg GeoJSON has flat properties; older exports embed an HTML table.
        val fields = HashMap<String, String>()
        props.forEach { (k, v) -> (v as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.let { fields[k.uppercase()] = it } }
        fields["DESCRIPTION"]?.let { html ->
            Regex("<th>(.*?)</th>\\s*<td>(.*?)</td>", RegexOption.IGNORE_CASE).findAll(html).forEach {
                fields[it.groupValues[1].trim().uppercase()] = it.groupValues[2].trim()
            }
        }
        val name = fields["NAME"]?.takeIf { it.isNotBlank() } ?: fields["ADDRESSBUILDINGNAME"] ?: "E-waste collection point"
        val postal = fields["ADDRESSPOSTALCODE"]?.takeIf { it.isNotBlank() }
        val address = listOfNotNull(
            listOfNotNull(fields["ADDRESSBLOCKHOUSENUMBER"], fields["ADDRESSSTREETNAME"])
                .filter { it.isNotBlank() }.joinToString(" ").ifBlank { null },
            postal?.let { "Singapore $it" },
        ).joinToString(", ").ifBlank { fields["ADDRESSBUILDINGNAME"] ?: "Singapore" }
        val desc = (fields["DESCRIPTION"] ?: "").lowercase()
        val accepts = buildSet {
            add(EWasteCategory.DEVICE_WITH_BATTERY); add(EWasteCategory.APPLIANCE_NO_BATTERY); add(EWasteCategory.COMPONENT)
            if ("batter" in desc || desc.isBlank()) add(EWasteCategory.SMALL_BATTERY)
            if ("bulb" in desc || "lamp" in desc || desc.isBlank()) add(EWasteCategory.LAMP)
        }
        return Place(
            id = "nea-${fields["OBJECTID"]?.toIntOrNull() ?: index}",
            name = name,
            type = PlaceType.BIN,
            point = GeoPoint(lat, lng),
            address = address,
            postalCode = postal,
            operator = "NEA-listed e-waste collection point",
            accepts = accepts,
            source = PlaceSource.NEA,
        )
    }

    private companion object {
        const val TAG = "PlacesRepository"
    }
}

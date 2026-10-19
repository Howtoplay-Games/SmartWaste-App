package sg.smartwaste.app.data.model

import sg.smartwaste.app.util.GeoPoint
import java.util.UUID

/**
 * E-waste categories. The first four map 1:1 to the SmartWaste smart-bin compartments
 * described in the system overview; LAMP and LARGE_APPLIANCE cover the remaining NEA
 * regulated streams so the app can guide users for everything they might scan.
 */
enum class EWasteCategory(
    val title: String,
    val shortTitle: String,
    val compartment: String,
    val examples: String,
    val basePoints: Int,
    /** Average mass per item in kg, used for impact estimates. */
    val avgKg: Double,
    /** Whether the item fits in a public e-waste bin. */
    val binFriendly: Boolean,
) {
    DEVICE_WITH_BATTERY(
        "Device with battery", "Battery devices", "Upper-left compartment",
        "Phones, AirPods, laptops, tablets, e-readers", 50, 0.8, true
    ),
    APPLIANCE_NO_BATTERY(
        "Appliance without battery", "Appliances", "Lower-left compartment",
        "Monitors, printers, routers, set-top boxes, kettles", 40, 4.0, true
    ),
    SMALL_BATTERY(
        "Small batteries", "Batteries", "Upper-right battery chute",
        "AA, AAA, 9V, 18650 lithium cells, power banks", 10, 0.03, true
    ),
    COMPONENT(
        "Device components", "Components", "Lower-right compartment",
        "RAM, CPUs, motherboards, hard drives, circuit boards", 30, 0.3, true
    ),
    LAMP(
        "Lamps & bulbs", "Lighting", "Battery & bulb bin",
        "LED bulbs, fluorescent tubes, CFLs", 15, 0.15, true
    ),
    LARGE_APPLIANCE(
        "Large appliance", "Large items", "Take-back or collection",
        "Fridges, washing machines, TVs, air-cons, PMDs", 120, 40.0, false
    );

    companion object {
        fun fromKey(key: String?): EWasteCategory? =
            entries.firstOrNull { it.name.equals(key?.trim(), ignoreCase = true) }

        /** The four compartments of a SmartWaste smart bin. */
        val smartBinCompartments = listOf(DEVICE_WITH_BATTERY, APPLIANCE_NO_BATTERY, SMALL_BATTERY, COMPONENT)
    }
}

enum class HazardLevel { LOW, MEDIUM, HIGH }

enum class ResalePotential { NONE, LOW, MEDIUM, HIGH }

enum class DetectionEngine(val label: String) {
    CLOUD("Cloud AI"),
    ON_DEVICE("On-device backup AI"),
    MANUAL("Selected manually"),
}

data class DetectionResult(
    val id: String = UUID.randomUUID().toString(),
    val itemName: String,
    val brand: String? = null,
    val category: EWasteCategory?,
    val isEWaste: Boolean = true,
    val confidence: Float,
    val hazardLevel: HazardLevel = HazardLevel.LOW,
    val containsBattery: Boolean = false,
    val description: String = "",
    val preparationSteps: List<String> = emptyList(),
    val safetyWarnings: List<String> = emptyList(),
    val resalePotential: ResalePotential = ResalePotential.NONE,
    val estimatedValue: String? = null,
    val materials: List<String> = emptyList(),
    val neaRegulated: Boolean = false,
    val engine: DetectionEngine,
    val modelName: String,
    val photoPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class PlaceType { BIN, BUYER }

enum class PlaceSource(val label: String) {
    SMARTWASTE("SmartWaste smart bin"),
    CURATED("SmartWaste directory"),
    NEA("NEA open data"),
    OSM("OpenStreetMap"),
}

data class BuyerInfo(
    val pays: String,
    val buys: String,
    val pickup: Boolean = false,
    val notes: String? = null,
)

data class Place(
    val id: String,
    val name: String,
    val type: PlaceType,
    val point: GeoPoint,
    val address: String,
    val postalCode: String? = null,
    val operator: String,
    val accepts: Set<EWasteCategory>,
    val source: PlaceSource,
    /** 0..100 fill level. Only SmartWaste smart bins report capacity. */
    val fillLevel: Int? = null,
    val openingHours: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val buyer: BuyerInfo? = null,
) {
    val isFull: Boolean get() = (fillLevel ?: 0) >= 90
    val isSmartBin: Boolean get() = source == PlaceSource.SMARTWASTE
}

data class PlaceWithDistance(val place: Place, val distanceMeters: Double)

/** Buyers that operate online / via doorstep pickup and so have no map pin. */
data class OnlineBuyer(
    val name: String,
    val description: String,
    val buys: String,
    val pays: String,
    val website: String,
)

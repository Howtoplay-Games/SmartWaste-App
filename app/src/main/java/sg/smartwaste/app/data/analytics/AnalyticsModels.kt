package sg.smartwaste.app.data.analytics

import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.Place
import sg.smartwaste.app.util.GeoPoint

/*
 * Data model for the data-analytics demo (Profile → Settings → Data analytics). It follows the
 * four stages of the presentation: raw data → clean data → insights → action.
 * Pure Kotlin with no Android types, so the whole pipeline can be run and checked on the JVM.
 */

/** A compartment at or above this level counts as full (the map treats ≥ 90% as full too). */
const val FULL_PCT = 90.0

/** Fill level right after a pickup. */
const val EMPTY_PCT = 2.0

const val HOUR_MS = 3_600_000L
const val DAY_MS = 24 * HOUR_MS

// ================================================================== 1. raw data

/** A fill-level reading from one smart-bin compartment sensor, sent every 30 minutes. */
data class SensorRead(
    val id: String,
    val binId: String,
    val compartment: EWasteCategory,
    val time: Long,
    val fillPct: Double,
)

/** A QR session: the user scans the bin's QR code, drops items in, and the session closes. */
data class QrSession(
    val id: String,
    val binId: String,
    val userId: String,
    val start: Long,
    val end: Long,
    val items: Int,
)

enum class ItemEventType { SCAN, DEPOSIT }

/**
 * An item with its category, location and time. A SCAN is recorded where the item is
 * (usually at home); a DEPOSIT is recorded at the bin it went into.
 */
data class ItemEvent(
    val id: String,
    val type: ItemEventType,
    val userId: String,
    val category: EWasteCategory,
    val time: Long,
    val location: GeoPoint,
    val binId: String? = null,
    val sessionId: String? = null,
)

/** A collection visit that emptied a compartment (from the operations log). */
data class Pickup(val binId: String, val compartment: EWasteCategory, val time: Long)

/** How many errors the generator injected, so the cleaning step can be checked against it. */
data class InjectedNoise(
    val duplicateReads: Int = 0,
    val duplicateSessions: Int = 0,
    val duplicateEvents: Int = 0,
    val outOfRangeReads: Int = 0,
    val spikeReads: Int = 0,
    val outsideEvents: Int = 0,
    val futureEvents: Int = 0,
)

data class RawData(
    val generatedAt: Long,
    /** Local midnight at the start of the window. */
    val windowStart: Long,
    val days: Int,
    val smartBins: List<Place>,
    val allBins: List<Place>,
    val reads: List<SensorRead>,
    val sessions: List<QrSession>,
    val events: List<ItemEvent>,
    val pickups: List<Pickup>,
    /** Deposits from this device's real history that were added to the demo data. */
    val realEvents: Int,
    val noise: InjectedNoise,
) {
    val totalRows: Int get() = reads.size + sessions.size + events.size

    /** "SmartWaste Bin · Waterway Point" → "Waterway Point". */
    fun binName(id: String): String = allBins.firstOrNull { it.id == id }?.name?.substringAfter("· ") ?: id
}

// ================================================================ 2. clean data

enum class FlagRule(val label: String) {
    OUT_OF_RANGE("Out of range"),
    IMPOSSIBLE_JUMP("Impossible jump"),
    OUTSIDE_SINGAPORE("Outside Singapore"),
    FUTURE_TIME("Time in the future"),
}

data class Flagged<T>(val row: T, val rule: FlagRule, val detail: String)

data class CleanData(
    val duplicateReads: Int,
    val duplicateSessions: Int,
    val duplicateEvents: Int,
    val flaggedReads: List<Flagged<SensorRead>>,
    val flaggedEvents: List<Flagged<ItemEvent>>,
    /** Devices, appliances and components compartments. */
    val generalReads: List<SensorRead>,
    /** The battery chute, kept as its own stream (fire risk, separate licensed collector). */
    val batteryReads: List<SensorRead>,
    /** Non-battery item events, with anonymous user IDs. */
    val generalEvents: List<ItemEvent>,
    /** Battery item events, with anonymous user IDs. */
    val batteryEvents: List<ItemEvent>,
    /** De-duplicated QR sessions, with anonymous user IDs. */
    val sessions: List<QrSession>,
    val users: Int,
    /** A few raw ID → anonymous ID examples for display. */
    val sampleIds: List<Pair<String, String>>,
) {
    val reads: List<SensorRead> get() = generalReads + batteryReads
    val events: List<ItemEvent> get() = generalEvents + batteryEvents
    val totalRows: Int get() = generalReads.size + batteryReads.size + sessions.size + generalEvents.size + batteryEvents.size
}

// =================================================================== 3. insights

data class CompartmentSeries(
    /** Fill level at the end of each hour of the window. */
    val hourly: List<Float>,
    val current: Double,
    val ratePerDay: Double,
    val pickups: Int,
)

data class FullnessTrend(val binId: String, val binName: String, val series: Map<EWasteCategory, CompartmentSeries>)

/** A ~1.3 km grid cell where items were scanned. */
data class Hotspot(
    val center: GeoPoint,
    val scans: Int,
    val area: String,
    val nearestBinName: String?,
    val nearestBinM: Double,
)

enum class Priority(val label: String) { NOW("Collect now"), TODAY("Today"), TOMORROW("Tomorrow"), OK("OK") }

data class CompartmentStatus(
    val binId: String,
    val binName: String,
    val compartment: EWasteCategory,
    val current: Double,
    val ratePerHour: Double,
    /** Hours until it reaches [FULL_PCT]; null when it isn't filling. */
    val hoursToFull: Double?,
    val priority: Priority,
)

data class Behaviour(
    val depositsByHour: List<Int>,
    val peakHour: Int,
    val deposits: Int,
    val scans: Int,
    val weekendShare: Double,
    val repeatUserShare: Double,
    /** Share of scans at least 3 days old that were followed by a deposit of the same kind. */
    val scanToDeposit: Double,
    val conversionByCategory: Map<EWasteCategory, Double>,
    val matureScansByCategory: Map<EWasteCategory, Int>,
    val depositsByCategory: List<Pair<EWasteCategory, Int>>,
    val depositsByBin: Map<String, Int>,
)

data class Insights(
    val trends: List<FullnessTrend>,
    /** Every scan cell, busiest first. */
    val hotspots: List<Hotspot>,
    /** Every smart-bin compartment, most urgent first. */
    val priorities: List<CompartmentStatus>,
    val behaviour: Behaviour,
)

// ===================================================================== 4. action

data class Depot(val name: String, val point: GeoPoint)

data class PickupStop(
    val binId: String,
    val binName: String,
    val point: GeoPoint,
    val compartments: List<EWasteCategory>,
    val maxFill: Double,
)

data class TruckRoute(val stops: List<PickupStop>, val km: Double)

data class PickupPlan(val depot: Depot, val general: TruckRoute, val battery: TruckRoute)

data class MoveSuggestion(val binId: String, val binName: String, val dropOffsThisWeek: Int, val to: Hotspot)

data class ReminderPlan(val users: Int, val items: Int, val sampleTitle: String?, val sampleMessage: String?)

data class Campaign(
    val title: String,
    val detail: String,
    val category: EWasteCategory,
    val binNames: List<String>,
    val window: String,
    val conversion: Double,
    val goal: Double,
)

data class Actions(
    val pickups: PickupPlan,
    val moves: List<MoveSuggestion>,
    val reminders: ReminderPlan,
    val campaign: Campaign?,
)

/** Bins emptied by "Simulate pickups": main compartments and battery chutes separately. */
data class Collected(val general: Set<String> = emptySet(), val battery: Set<String> = emptySet()) {
    val count: Int get() = general.size + battery.size
}

data class AnalyticsResult(
    val raw: RawData,
    val clean: CleanData,
    val insights: Insights,
    val actions: Actions,
    val collected: Collected,
)

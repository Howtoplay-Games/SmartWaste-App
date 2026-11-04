package sg.smartwaste.app.data.analytics

import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.EWasteCategory.APPLIANCE_NO_BATTERY
import sg.smartwaste.app.data.model.EWasteCategory.COMPONENT
import sg.smartwaste.app.data.model.EWasteCategory.DEVICE_WITH_BATTERY
import sg.smartwaste.app.data.model.EWasteCategory.LAMP
import sg.smartwaste.app.data.model.EWasteCategory.LARGE_APPLIANCE
import sg.smartwaste.app.data.model.EWasteCategory.SMALL_BATTERY
import sg.smartwaste.app.data.model.PlaceType
import sg.smartwaste.app.util.Geo
import sg.smartwaste.app.util.GeoPoint
import sg.smartwaste.app.util.Hashing
import sg.smartwaste.app.util.ServiceArea
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Raw data → clean data → insights → action, as described in the "Data analytics design" slide.
 * Every number on the demo screen comes from here.
 */
object AnalyticsPipeline {

    /** A rise bigger than this between two readings less than [SPIKE_WINDOW_MS] apart is physically impossible. */
    const val SPIKE_POINTS = 35.0
    const val SPIKE_WINDOW_MS = 2 * HOUR_MS
    const val FUTURE_TOLERANCE_MS = 5 * 60_000L

    /** A drop bigger than this is a pickup. */
    const val PICKUP_DROP = 20.0

    /** Demo salt. In production this is a server-side secret, so IDs can't be rebuilt from an email list. */
    const val ANON_SALT = "smartwaste-demo-2026"

    /** Users get a reminder once a scanned item has waited this long without a drop-off. */
    const val REMINDER_AFTER_MS = 2 * DAY_MS

    /** Scans younger than this may still be dropped off, so they don't count for conversion. */
    const val MATURE_SCAN_MS = 3 * DAY_MS

    /** A hotspot with no e-waste bin this close is uncovered. */
    const val UNCOVERED_M = 1_500.0
    const val MIN_HOTSPOT_SCANS = 8
    const val MIN_CAMPAIGN_SCANS = 15

    /** Grid cell for the hotspot map: 0.012° ≈ 1.3 km. */
    const val HOTSPOT_CELL_DEG = 0.012

    /** Roads are ~30% longer than the straight line in Singapore. */
    const val ROAD_FACTOR = 1.3

    val DEPOT = Depot("SmartWaste depot · Kaki Bukit", GeoPoint(1.3355, 103.9064))

    /** "alex.tan17@example.com" → "U-7F3A9C21": salted SHA-256, so it's stable but can't be reversed. */
    fun anonymise(userId: String, salt: String = ANON_SALT): String =
        "U-" + Hashing.sha256("$salt:${userId.trim().lowercase()}").take(8).uppercase(Locale.US)

    fun run(raw: RawData, collected: Collected = Collected(), zone: TimeZone = TimeZone.getDefault()): AnalyticsResult =
        analyse(raw, clean(raw), collected, zone)

    // ================================================================= clean data

    fun clean(raw: RawData, salt: String = ANON_SALT): CleanData {
        // 1. Remove duplicates: the same reading (bin, compartment, time) or the same event/session ID.
        val readKeys = HashSet<String>()
        val reads = raw.reads.filter { readKeys.add("${it.binId}|${it.compartment}|${it.time}") }
        val sessionIds = HashSet<String>()
        val sessions = raw.sessions.filter { sessionIds.add(it.id) }
        val eventIds = HashSet<String>()
        val events = raw.events.filter { eventIds.add(it.id) }

        // 2. Flag impossible readings. Each reading is compared with the last one that was kept.
        val flaggedReads = ArrayList<Flagged<SensorRead>>()
        val validReads = ArrayList<SensorRead>(reads.size)
        for (series in reads.groupBy { it.binId to it.compartment }.values) {
            var last: SensorRead? = null
            for (r in series.sortedBy { it.time }) {
                val prev = last
                val flag = when {
                    r.fillPct < 0.0 || r.fillPct > 100.0 ->
                        Flagged(r, FlagRule.OUT_OF_RANGE, "${pct1(r.fillPct)} is not a possible fill level")
                    prev != null && r.time - prev.time <= SPIKE_WINDOW_MS && r.fillPct - prev.fillPct > SPIKE_POINTS ->
                        Flagged(r, FlagRule.IMPOSSIBLE_JUMP, "+${(r.fillPct - prev.fillPct).roundToInt()} pts in ${(r.time - prev.time) / 60_000} min")
                    else -> null
                }
                if (flag != null) {
                    flaggedReads += flag
                } else {
                    validReads += r
                    last = r
                }
            }
        }
        val flaggedEvents = ArrayList<Flagged<ItemEvent>>()
        val validEvents = events.filter { e ->
            val flag = when {
                !ServiceArea.contains(e.location) -> Flagged(
                    e, FlagRule.OUTSIDE_SINGAPORE,
                    String.format(Locale.US, "GPS %.4f, %.4f", e.location.lat, e.location.lng),
                )
                e.time > raw.generatedAt + FUTURE_TOLERANCE_MS -> Flagged(
                    e, FlagRule.FUTURE_TIME,
                    "${(e.time - raw.generatedAt) / DAY_MS} days ahead",
                )
                else -> null
            }
            if (flag != null) flaggedEvents += flag
            flag == null
        }

        // 3. Separate the battery stream.
        val (batteryReads, generalReads) = validReads.partition { it.compartment == SMALL_BATTERY }

        // 4. Anonymise user IDs before anything is analysed.
        val ids = LinkedHashMap<String, String>()
        fun anon(userId: String) = ids.getOrPut(userId) { anonymise(userId, salt) }
        val anonEvents = validEvents.map { it.copy(userId = anon(it.userId)) }
        val anonSessions = sessions.map { it.copy(userId = anon(it.userId)) }
        val (batteryEvents, generalEvents) = anonEvents.partition { it.category == SMALL_BATTERY }

        val realUser = raw.events.firstOrNull { it.id.startsWith(DemoDataGenerator.REAL_PREFIX) }?.userId
        val samples = buildList {
            realUser?.let { u -> ids[u]?.let { add(u to it) } }
            ids.entries.asSequence().filter { it.key != realUser }.take(3).forEach { add(it.key to it.value) }
        }

        return CleanData(
            duplicateReads = raw.reads.size - reads.size,
            duplicateSessions = raw.sessions.size - sessions.size,
            duplicateEvents = raw.events.size - events.size,
            flaggedReads = flaggedReads.sortedByDescending { it.row.time },
            flaggedEvents = flaggedEvents,
            generalReads = generalReads.sortedBy { it.time },
            batteryReads = batteryReads.sortedBy { it.time },
            generalEvents = generalEvents,
            batteryEvents = batteryEvents,
            sessions = anonSessions,
            users = ids.size,
            sampleIds = samples,
        )
    }

    // =================================================================== insights

    fun analyse(
        raw: RawData,
        clean: CleanData,
        collected: Collected = Collected(),
        zone: TimeZone = TimeZone.getDefault(),
    ): AnalyticsResult {
        val cal = Calendar.getInstance(zone)
        val readsByKey = clean.reads.groupBy { it.binId to it.compartment }.mapValues { (_, v) -> v.sortedBy { it.time } }
        val trends = trends(raw, readsByKey, collected)
        val priorities = priorities(raw, readsByKey, collected)
        val match = matchScans(clean.events)
        val hotspots = hotspots(raw, clean)
        val behaviour = behaviour(raw, clean, match, cal)
        val actions = Actions(
            pickups = pickupPlan(raw, priorities),
            moves = moves(raw, hotspots, behaviour, priorities),
            reminders = reminders(raw, match, priorities),
            campaign = campaign(raw, behaviour),
        )
        return AnalyticsResult(raw, clean, Insights(trends, hotspots, priorities, behaviour), actions, collected)
    }

    private fun emptied(binId: String, c: EWasteCategory, collected: Collected) =
        if (c == SMALL_BATTERY) binId in collected.battery else binId in collected.general

    private fun trends(raw: RawData, readsByKey: Map<Pair<String, EWasteCategory>, List<SensorRead>>, collected: Collected): List<FullnessTrend> {
        val hours = ((raw.generatedAt - raw.windowStart) / HOUR_MS).toInt() + 1
        return raw.smartBins.map { bin ->
            val series = EWasteCategory.smartBinCompartments.associateWith { c ->
                val reads = readsByKey[bin.id to c].orEmpty()
                val emptied = emptied(bin.id, c, collected)
                val hourly = hourlyValues(reads, raw.windowStart, hours).toMutableList()
                if (emptied && hourly.isNotEmpty()) hourly[hourly.lastIndex] = EMPTY_PCT.toFloat()
                CompartmentSeries(
                    hourly = hourly,
                    current = if (emptied) EMPTY_PCT else reads.lastOrNull()?.fillPct ?: 0.0,
                    ratePerDay = fillRatePerHour(reads, raw.generatedAt) * 24,
                    pickups = raw.pickups.count { it.binId == bin.id && it.compartment == c && it.time >= raw.windowStart } + if (emptied) 1 else 0,
                )
            }
            FullnessTrend(bin.id, raw.binName(bin.id), series)
        }
    }

    /** The last reading in each hour of the window, carried forward through hours without one. */
    private fun hourlyValues(reads: List<SensorRead>, start: Long, hours: Int): List<Float> {
        if (reads.isEmpty()) return emptyList()
        val out = FloatArray(hours)
        var idx = 0
        var value = reads.first().fillPct
        for (h in 0 until hours) {
            val end = start + (h + 1) * HOUR_MS
            while (idx < reads.size && reads[idx].time < end) {
                value = reads[idx].fillPct
                idx++
            }
            out[h] = value.toFloat()
        }
        return out.toList()
    }

    /**
     * Fill rate in points per hour over the last [lookbackMs]. Uses the net rise of each stretch
     * between pickups rather than every small step, so sensor jitter doesn't inflate it.
     */
    internal fun fillRatePerHour(reads: List<SensorRead>, now: Long, lookbackMs: Long = 3 * DAY_MS): Double {
        val recent = reads.filter { it.time >= now - lookbackMs }
        if (recent.size < 2) return 0.0
        var rise = 0.0
        var hours = 0.0
        var segStart = recent.first()
        for (i in 1 until recent.size) {
            val prev = recent[i - 1]
            val cur = recent[i]
            if (prev.fillPct - cur.fillPct > PICKUP_DROP) {
                rise += prev.fillPct - segStart.fillPct
                hours += (prev.time - segStart.time) / HOUR_MS.toDouble()
                segStart = cur
            }
        }
        val end = recent.last()
        rise += end.fillPct - segStart.fillPct
        hours += (end.time - segStart.time) / HOUR_MS.toDouble()
        return if (hours >= 1.0) (rise / hours).coerceAtLeast(0.0) else 0.0
    }

    fun priorityFor(hoursToFull: Double?): Priority = when {
        hoursToFull == null -> Priority.OK
        hoursToFull <= 6 -> Priority.NOW
        hoursToFull <= 24 -> Priority.TODAY
        hoursToFull <= 48 -> Priority.TOMORROW
        else -> Priority.OK
    }

    private fun priorities(raw: RawData, readsByKey: Map<Pair<String, EWasteCategory>, List<SensorRead>>, collected: Collected): List<CompartmentStatus> =
        raw.smartBins.flatMap { bin ->
            EWasteCategory.smartBinCompartments.mapNotNull { c ->
                val reads = readsByKey[bin.id to c]
                if (reads.isNullOrEmpty()) return@mapNotNull null
                val current = if (emptied(bin.id, c, collected)) EMPTY_PCT else reads.last().fillPct
                val rate = fillRatePerHour(reads, raw.generatedAt)
                val hoursToFull = when {
                    current >= FULL_PCT -> 0.0
                    rate > 0.05 -> (FULL_PCT - current) / rate
                    else -> null
                }
                CompartmentStatus(bin.id, raw.binName(bin.id), c, current, rate, hoursToFull, priorityFor(hoursToFull))
            }
        }.sortedWith(compareBy<CompartmentStatus> { it.hoursToFull ?: Double.MAX_VALUE }.thenByDescending { it.current })

    private class ScanMatch(val matched: List<ItemEvent>, val unmatched: List<ItemEvent>)

    /** Pairs each scan with the same user's next deposit of the same kind. */
    private fun matchScans(events: List<ItemEvent>): ScanMatch {
        val pending = events.filter { it.type == ItemEventType.DEPOSIT }
            .groupBy { it.userId to it.category }
            .mapValues { (_, v) -> v.sortedBy { it.time }.toMutableList() }
        val matched = ArrayList<ItemEvent>()
        val unmatched = ArrayList<ItemEvent>()
        for (scan in events.filter { it.type == ItemEventType.SCAN }.sortedBy { it.time }) {
            val list = pending[scan.userId to scan.category]
            val i = list?.indexOfFirst { it.time >= scan.time } ?: -1
            if (list != null && i >= 0) {
                list.removeAt(i)
                matched += scan
            } else {
                unmatched += scan
            }
        }
        return ScanMatch(matched, unmatched)
    }

    private fun hotspots(raw: RawData, clean: CleanData): List<Hotspot> {
        val eWasteBins = raw.allBins.filter { it.type == PlaceType.BIN && DEVICE_WITH_BATTERY in it.accepts }
        return clean.events.asSequence()
            .filter { it.type == ItemEventType.SCAN }
            .groupBy { floor(it.location.lat / HOTSPOT_CELL_DEG).toInt() to floor(it.location.lng / HOTSPOT_CELL_DEG).toInt() }
            .map { (_, scans) ->
                val center = GeoPoint(scans.map { it.location.lat }.average(), scans.map { it.location.lng }.average())
                val nearest = eWasteBins.minByOrNull { Geo.distanceMeters(it.point, center) }
                Hotspot(
                    center = center,
                    scans = scans.size,
                    area = DemoTowns.nearest(center)?.name ?: String.format(Locale.US, "%.3f, %.3f", center.lat, center.lng),
                    nearestBinName = nearest?.let { raw.binName(it.id) },
                    nearestBinM = nearest?.let { Geo.distanceMeters(it.point, center) } ?: Double.MAX_VALUE,
                )
            }
            .sortedByDescending { it.scans }
    }

    private fun behaviour(raw: RawData, clean: CleanData, match: ScanMatch, cal: Calendar): Behaviour {
        val deposits = clean.events.filter { it.type == ItemEventType.DEPOSIT }
        val scans = clean.events.filter { it.type == ItemEventType.SCAN }
        val byHour = IntArray(24)
        var weekend = 0
        for (d in deposits) {
            cal.timeInMillis = d.time
            byHour[cal.get(Calendar.HOUR_OF_DAY)]++
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) weekend++
        }
        val perUser = deposits.groupingBy { it.userId }.eachCount()
        val matchedIds = match.matched.mapTo(HashSet()) { it.id }
        val mature = scans.filter { it.time <= raw.generatedAt - MATURE_SCAN_MS }
        val matureByCat = mature.groupBy { it.category }
        return Behaviour(
            depositsByHour = byHour.toList(),
            peakHour = byHour.indices.maxByOrNull { byHour[it] } ?: 0,
            deposits = deposits.size,
            scans = scans.size,
            weekendShare = ratio(weekend, deposits.size),
            repeatUserShare = ratio(perUser.count { it.value >= 2 }, perUser.size),
            scanToDeposit = ratio(mature.count { it.id in matchedIds }, mature.size),
            conversionByCategory = matureByCat.mapValues { (_, list) -> ratio(list.count { it.id in matchedIds }, list.size) },
            matureScansByCategory = matureByCat.mapValues { it.value.size },
            depositsByCategory = deposits.groupingBy { it.category }.eachCount().toList().sortedByDescending { it.second },
            depositsByBin = deposits.mapNotNull { it.binId }.groupingBy { it }.eachCount(),
        )
    }

    // ===================================================================== action

    private fun pickupPlan(raw: RawData, priorities: List<CompartmentStatus>): PickupPlan {
        val due = setOf(Priority.NOW, Priority.TODAY)
        fun stops(battery: Boolean): List<PickupStop> = priorities
            .filter { it.priority in due && (it.compartment == SMALL_BATTERY) == battery }
            .groupBy { it.binId }
            .mapNotNull { (binId, list) ->
                val bin = raw.smartBins.firstOrNull { it.id == binId } ?: return@mapNotNull null
                PickupStop(binId, raw.binName(binId), bin.point, list.map { it.compartment }, list.maxOf { it.current })
            }
        return PickupPlan(DEPOT, route(stops(battery = false)), route(stops(battery = true)))
    }

    /** Nearest-neighbour route from the depot and back. */
    private fun route(stops: List<PickupStop>): TruckRoute {
        val left = stops.toMutableList()
        val ordered = ArrayList<PickupStop>(stops.size)
        var at = DEPOT.point
        var meters = 0.0
        while (left.isNotEmpty()) {
            val next = left.minBy { Geo.distanceMeters(at, it.point) }
            meters += Geo.distanceMeters(at, next.point)
            at = next.point
            ordered += next
            left.remove(next)
        }
        if (ordered.isNotEmpty()) meters += Geo.distanceMeters(at, DEPOT.point)
        return TruckRoute(ordered, meters * ROAD_FACTOR / 1000.0)
    }

    /**
     * Pairs the least-used SmartWaste bins with the busiest hotspots that have no e-waste bin nearby.
     * Bins with a pickup due are filling up, so they aren't candidates.
     */
    private fun moves(raw: RawData, hotspots: List<Hotspot>, behaviour: Behaviour, priorities: List<CompartmentStatus>): List<MoveSuggestion> {
        val use = raw.smartBins.associate { it.id to (behaviour.depositsByBin[it.id] ?: 0) }
        val busy = priorities.filter { it.priority == Priority.NOW || it.priority == Priority.TODAY }.map { it.binId }.toSet()
        val lowUse = raw.smartBins.filter { it.id !in busy }.sortedBy { use.getValue(it.id) }
        val targets = hotspots
            .filter { it.nearestBinM > UNCOVERED_M && it.scans >= MIN_HOTSPOT_SCANS }
            .distinctBy { it.area }
            .take(2)
        return targets.mapIndexedNotNull { i, h ->
            lowUse.getOrNull(i)?.let { MoveSuggestion(it.id, raw.binName(it.id), use.getValue(it.id), h) }
        }
    }

    private fun reminders(raw: RawData, match: ScanMatch, priorities: List<CompartmentStatus>): ReminderPlan {
        val pending = match.unmatched.filter { it.time <= raw.generatedAt - REMINDER_AFTER_MS && it.category.binFriendly }
        val sample = pending.maxByOrNull { it.time }
            ?: return ReminderPlan(0, 0, null, null)
        val full = priorities.filter { it.current >= FULL_PCT || it.priority == Priority.NOW }.map { it.binId to it.compartment }.toSet()
        val bin = raw.allBins
            .filter { it.type == PlaceType.BIN && sample.category in it.accepts && (it.id to sample.category) !in full }
            .minByOrNull { Geo.distanceMeters(sample.location, it.point) }
        val days = ((raw.generatedAt - sample.time) / DAY_MS).coerceAtLeast(2)
        val noun = itemNoun(sample.category)
        val them = if (sample.category == SMALL_BATTERY) "them" else "it"
        val where = bin?.let {
            "${raw.binName(it.id)} is ${Geo.formatDistance(Geo.distanceMeters(sample.location, it.point))} away and has space."
        } ?: "There's a bin with space near you."
        return ReminderPlan(
            users = pending.map { it.userId }.distinct().size,
            items = pending.size,
            sampleTitle = "Still have ${noun} at home?",
            sampleMessage = "You scanned $noun $days days ago. $where Drop $them off to earn up to ${sample.category.basePoints * 2} pts.",
        )
    }

    /** Targets the item type people most often scan but don't drop off, at the quietest hours and least-used bins. */
    private fun campaign(raw: RawData, b: Behaviour): Campaign? {
        val category = EWasteCategory.smartBinCompartments
            .filter { (b.matureScansByCategory[it] ?: 0) >= MIN_CAMPAIGN_SCANS }
            .minByOrNull { b.conversionByCategory[it] ?: 1.0 }
            ?: return null
        val bins = raw.smartBins.filter { category in it.accepts }
            .sortedBy { b.depositsByBin[it.id] ?: 0 }
            .take(2)
            .map { raw.binName(it.id) }
        if (bins.isEmpty()) return null
        val start = (10..18).minByOrNull { h -> b.depositsByHour[h] + b.depositsByHour[h + 1] + b.depositsByHour[h + 2] } ?: 14
        val window = "${hourLabel(start)}–${hourLabel(start + 3)}"
        val conversion = b.conversionByCategory[category] ?: 0.0
        return Campaign(
            title = "${campaignName(category)}: 2× points",
            detail = "Double points for ${itemNoun(category)} dropped at ${bins.joinToString(" or ")} between " +
                "${hourLabel(start)} and ${hourLabel(start + 3)}: the quietest hours, at the least-used SmartWaste bins.",
            category = category,
            binNames = bins,
            window = window,
            conversion = conversion,
            goal = min(0.9, conversion + 0.15),
        )
    }

    // ==================================================================== helpers

    fun hourLabel(hour: Int): String {
        val h = ((hour % 24) + 24) % 24
        return when {
            h == 0 -> "12am"
            h < 12 -> "${h}am"
            h == 12 -> "12pm"
            else -> "${h - 12}pm"
        }
    }

    fun itemNoun(c: EWasteCategory): String = when (c) {
        DEVICE_WITH_BATTERY -> "a device with a battery"
        APPLIANCE_NO_BATTERY -> "an appliance"
        SMALL_BATTERY -> "batteries"
        COMPONENT -> "a computer part"
        LAMP -> "a lamp"
        LARGE_APPLIANCE -> "a large appliance"
    }

    private fun campaignName(c: EWasteCategory): String = when (c) {
        SMALL_BATTERY -> "Battery Week"
        DEVICE_WITH_BATTERY -> "Gadget Week"
        APPLIANCE_NO_BATTERY -> "Appliance Week"
        COMPONENT -> "Parts Week"
        LAMP -> "Lamp Week"
        LARGE_APPLIANCE -> "Big Item Week"
    }

    private fun ratio(a: Int, b: Int) = if (b == 0) 0.0 else a.toDouble() / b

    private fun pct1(v: Double) = String.format(Locale.US, "%.1f%%", v)
}

package sg.smartwaste.app.data.analytics

import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.EWasteCategory.APPLIANCE_NO_BATTERY
import sg.smartwaste.app.data.model.EWasteCategory.COMPONENT
import sg.smartwaste.app.data.model.EWasteCategory.DEVICE_WITH_BATTERY
import sg.smartwaste.app.data.model.EWasteCategory.LAMP
import sg.smartwaste.app.data.model.EWasteCategory.LARGE_APPLIANCE
import sg.smartwaste.app.data.model.EWasteCategory.SMALL_BATTERY
import sg.smartwaste.app.data.model.Place
import sg.smartwaste.app.data.model.PlaceType
import sg.smartwaste.app.util.Geo
import sg.smartwaste.app.util.GeoPoint
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt
import kotlin.random.Random

data class Town(val name: String, val point: GeoPoint, val weight: Double)

/** Singapore towns used to place demo users and to name hotspots. Weights are rough relative sizes. */
object DemoTowns {
    val all = listOf(
        Town("Tampines", GeoPoint(1.3540, 103.9451), 1.0),
        Town("Bedok", GeoPoint(1.3240, 103.9300), 0.9),
        Town("Jurong West", GeoPoint(1.3404, 103.7090), 0.9),
        Town("Woodlands", GeoPoint(1.4382, 103.7890), 0.8),
        Town("Punggol", GeoPoint(1.4043, 103.9020), 0.8),
        Town("Sengkang", GeoPoint(1.3868, 103.8914), 0.8),
        Town("Pasir Ris", GeoPoint(1.3721, 103.9494), 0.7),
        Town("Yishun", GeoPoint(1.4304, 103.8354), 0.7),
        Town("Ang Mo Kio", GeoPoint(1.3691, 103.8454), 0.7),
        Town("Sembawang", GeoPoint(1.4491, 103.8185), 0.6),
        Town("Hougang", GeoPoint(1.3712, 103.8863), 0.6),
        Town("Toa Payoh", GeoPoint(1.3343, 103.8563), 0.6),
        Town("Bukit Batok", GeoPoint(1.3490, 103.7496), 0.6),
        Town("Choa Chu Kang", GeoPoint(1.3840, 103.7470), 0.5),
        Town("Jurong East", GeoPoint(1.3329, 103.7436), 0.5),
        Town("Clementi", GeoPoint(1.3150, 103.7650), 0.5),
        Town("Queenstown", GeoPoint(1.2942, 103.7861), 0.5),
        Town("Geylang", GeoPoint(1.3201, 103.8918), 0.5),
        Town("Bishan", GeoPoint(1.3510, 103.8480), 0.4),
        Town("City", GeoPoint(1.2966, 103.8520), 0.4),
    )

    fun nearest(p: GeoPoint, maxM: Double = 3_000.0): Town? {
        val town = all.minByOrNull { Geo.distanceMeters(it.point, p) } ?: return null
        return town.takeIf { Geo.distanceMeters(it.point, p) <= maxM }
    }
}

/** Short compartment names used throughout the demo. */
fun compartmentLabel(c: EWasteCategory): String = when (c) {
    DEVICE_WITH_BATTERY -> "Devices"
    APPLIANCE_NO_BATTERY -> "Appliances"
    SMALL_BATTERY -> "Battery chute"
    COMPONENT -> "Components"
    LAMP -> "Lamps"
    LARGE_APPLIANCE -> "Large items"
}

/**
 * Builds a realistic week of raw data for the demo: compartment sensor reads for every
 * SmartWaste smart bin, app scans and deposits from 600 made-up users across Singapore,
 * QR sessions and the pickup log. It then injects the errors a real pipeline sees
 * (gateway retries, re-sent events, sensor glitches, GPS and clock faults) so the
 * cleaning step has something to remove. The same seed always gives the same data.
 *
 * The devices compartment of each smart bin ends exactly at the fill level the map shows.
 */
object DemoDataGenerator {
    const val DAYS = 7
    const val USERS = 600
    const val READ_INTERVAL_MS = 30 * 60_000L

    /** IDs of rows that come from this device's real deposit history. */
    const val REAL_PREFIX = "REAL-"

    /** The Android emulator's default GPS fix, one of the injected location faults. */
    private val EMULATOR_FIX = GeoPoint(37.421998, -122.084)

    /** Relative activity by hour of day: people recycle at lunch and in the evening. */
    internal val hourWeights = doubleArrayOf(
        0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.4, 0.7, 0.8, 0.8, 0.9, 1.0,
        1.4, 1.3, 0.9, 0.9, 1.0, 1.2, 1.8, 2.0, 1.9, 1.5, 0.8, 0.4,
    )
    private val meanHourWeight = hourWeights.average()

    private val categoryWeights = listOf(
        DEVICE_WITH_BATTERY to 0.37, SMALL_BATTERY to 0.25, APPLIANCE_NO_BATTERY to 0.18,
        COMPONENT to 0.11, LAMP to 0.06, LARGE_APPLIANCE to 0.03,
    )

    /** Chance that a scanned item is later dropped off. Batteries are the most often forgotten. */
    private val depositChance = mapOf(
        DEVICE_WITH_BATTERY to 0.78, SMALL_BATTERY to 0.5, APPLIANCE_NO_BATTERY to 0.64,
        COMPONENT to 0.72, LAMP to 0.55, LARGE_APPLIANCE to 0.0,
    )

    private val firstNames = listOf(
        "alex", "wei", "siti", "raj", "mei", "jun", "aisha", "ken", "priya", "hui",
        "daniel", "nur", "arjun", "grace", "farah", "marcus", "li", "zara", "ethan", "yuki",
    )
    private val lastNames = listOf("tan", "lim", "ng", "lee", "kumar", "wong", "goh", "ong", "chua", "rahman", "koh", "teo", "singh", "yeo", "chan")

    fun generate(
        smartBins: List<Place>,
        allBins: List<Place>,
        now: Long,
        seed: Int = 1,
        zone: TimeZone = TimeZone.getDefault(),
        realEvents: List<ItemEvent> = emptyList(),
        realSessions: List<QrSession> = emptyList(),
    ): RawData {
        val rnd = Random(seed)
        val cal = Calendar.getInstance(zone)
        val windowStart = startOfDay(now, cal) - (DAYS - 1) * DAY_MS
        val lastRead = now - now % READ_INTERVAL_MS
        val pickups = ArrayList<Pickup>()

        val cleanReads = sensorReads(smartBins, windowStart, lastRead, rnd, cal, pickups)
        val (cleanEvents, cleanSessions) = itemActivity(smartBins, allBins, windowStart, now, rnd, cal)

        // ------------------------------------------------ inject the errors to clean up
        val reads = cleanReads.toMutableList()
        val corrupted = HashSet<Int>()
        val candidates = reads.indices.filter { reads[it].time < lastRead - 2 * READ_INTERVAL_MS }

        // Sensor faults: 255 (error code), -1 (no echo) or > 100% (item stuck over the sensor).
        val outOfRange = if (candidates.isEmpty()) 0 else (reads.size * 0.006).toInt().coerceAtLeast(3)
        var injected = 0
        var guard = 0
        while (injected < outOfRange && guard++ < 50_000) {
            val i = candidates.random(rnd)
            if (!corrupted.add(i)) continue
            val bad = when (rnd.nextInt(3)) {
                0 -> 255.0
                1 -> -1.0
                else -> round1(rnd.nextDouble(104.0, 140.0))
            }
            reads[i] = reads[i].copy(fillPct = bad)
            injected++
        }
        // Glitches: a hand or bag in front of the sensor reads +40–55 points for one interval.
        val spikeTarget = if (candidates.isEmpty()) 0 else (reads.size * 0.004).toInt().coerceAtLeast(2)
        var spikes = 0
        guard = 0
        while (spikes < spikeTarget && guard++ < 50_000) {
            val i = candidates.random(rnd)
            if (i == 0 || i in corrupted || (i - 1) in corrupted) continue
            val prev = reads[i - 1]
            val cur = reads[i]
            if (prev.binId != cur.binId || prev.compartment != cur.compartment || prev.fillPct > 44.0) continue
            corrupted += i
            reads[i] = cur.copy(fillPct = round1(minOf(99.0, prev.fillPct + rnd.nextDouble(40.0, 55.0))))
            spikes++
        }
        // Gateway retries: the same reading arrives twice.
        val duplicateReads = (cleanReads.size * 0.015).toInt()
        repeat(duplicateReads) { reads += reads[rnd.nextInt(cleanReads.size)].copy() }
        val finalReads = reads.sortedBy { it.time }.mapIndexed { i, r -> r.copy(id = "R${300_000 + i}") }

        val events = cleanEvents.toMutableList()
        val eventUsed = HashSet<Int>()
        // GPS faults: no fix yet (0, 0) or the emulator's default location.
        val scanIdx = events.indices.filter { events[it].type == ItemEventType.SCAN }
        val outsideTarget = if (scanIdx.isEmpty()) 0 else (scanIdx.size * 0.006).toInt().coerceAtLeast(2)
        var outside = 0
        guard = 0
        while (outside < outsideTarget && guard++ < 50_000) {
            val i = scanIdx.random(rnd)
            if (!eventUsed.add(i)) continue
            events[i] = events[i].copy(location = if (outside % 2 == 0) GeoPoint(0.0, 0.0) else EMULATOR_FIX)
            outside++
        }
        // Clock faults: a phone with the wrong date sends events from the future.
        val futureTarget = if (events.isEmpty()) 0 else (events.size * 0.003).toInt().coerceAtLeast(2)
        var future = 0
        guard = 0
        while (future < futureTarget && guard++ < 50_000) {
            val i = rnd.nextInt(events.size)
            if (!eventUsed.add(i)) continue
            events[i] = events[i].copy(time = now + rnd.nextLong(2 * DAY_MS, 30 * DAY_MS))
            future++
        }
        // Offline sync: the app re-sends events (and sessions) it already uploaded.
        val duplicateEvents = (cleanEvents.size * 0.03).toInt()
        repeat(duplicateEvents) { events += events[rnd.nextInt(cleanEvents.size)].copy() }
        val sessions = cleanSessions.toMutableList()
        val duplicateSessions = (cleanSessions.size * 0.03).toInt()
        repeat(duplicateSessions) { sessions += sessions[rnd.nextInt(cleanSessions.size)].copy() }

        return RawData(
            generatedAt = now,
            windowStart = windowStart,
            days = DAYS,
            smartBins = smartBins,
            allBins = allBins,
            reads = finalReads,
            sessions = (sessions + realSessions).sortedBy { it.start },
            events = (events + realEvents).sortedBy { it.time },
            pickups = pickups.sortedBy { it.time },
            realEvents = realEvents.size,
            noise = InjectedNoise(
                duplicateReads = duplicateReads,
                duplicateSessions = duplicateSessions,
                duplicateEvents = duplicateEvents,
                outOfRangeReads = injected,
                spikeReads = spikes,
                outsideEvents = outside,
                futureEvents = future,
            ),
        )
    }

    // ------------------------------------------------------------------ sensors

    private class Plan(val bin: Place, val compartment: EWasteCategory, var target: Double, var ratePerHour: Double) {
        /** Reaches [FULL_PCT] within 20 hours at this rate. */
        fun isDue() = target >= FULL_PCT || (FULL_PCT - target) / ratePerHour <= 20.0

        fun makeDue(rnd: Random) {
            target = rnd.nextDouble(84.0, 89.0)
            ratePerHour = rnd.nextDouble(0.55, 0.9)
        }
    }

    private fun sensorReads(
        bins: List<Place>,
        windowStart: Long,
        lastRead: Long,
        rnd: Random,
        cal: Calendar,
        pickups: MutableList<Pickup>,
    ): List<SensorRead> {
        val plans = bins.flatMap { bin ->
            val main = (bin.fillLevel ?: rnd.nextInt(20, 70)).toDouble()
            EWasteCategory.smartBinCompartments.map { c ->
                when (c) {
                    DEVICE_WITH_BATTERY -> Plan(bin, c, main, rnd.nextDouble(0.45, 1.0))
                    APPLIANCE_NO_BATTERY -> Plan(bin, c, main * rnd.nextDouble(0.45, 0.95), rnd.nextDouble(0.3, 0.7))
                    COMPONENT -> Plan(bin, c, main * rnd.nextDouble(0.3, 0.8), rnd.nextDouble(0.2, 0.5))
                    else -> Plan(bin, c, rnd.nextDouble(15.0, 80.0), rnd.nextDouble(0.35, 0.9))
                }
            }
        }
        // Keep the demo meaningful: one main-compartment and one battery-chute pickup are always due.
        // The devices compartment is never changed, so the map's fill level stays the same.
        val general = plans.filter { it.compartment != SMALL_BATTERY }
        if (general.none { it.isDue() }) general.filter { it.compartment != DEVICE_WITH_BATTERY }.randomOrNull(rnd)?.makeDue(rnd)
        val battery = plans.filter { it.compartment == SMALL_BATTERY }
        if (battery.none { it.isDue() }) battery.randomOrNull(rnd)?.makeDue(rnd)

        val out = ArrayList<SensorRead>()
        for (p in plans) {
            // Built backwards from the current level: going back in time the level falls, and
            // where it would go below empty a pickup happened, so before it the bin was nearly full.
            val series = ArrayList<SensorRead>()
            var t = lastRead
            var fill = p.target
            while (t >= windowStart) {
                val shown = if (t == lastRead) fill else fill + rnd.nextDouble(-0.6, 0.6)
                series += SensorRead("", p.bin.id, p.compartment, t, round1(shown.coerceIn(0.0, 100.0)))
                val weight = hourWeights[hourOf(t - READ_INTERVAL_MS, cal)] / meanHourWeight
                fill -= p.ratePerHour * 0.5 * weight * rnd.nextDouble(0.6, 1.4)
                if (fill <= EMPTY_PCT) {
                    pickups += Pickup(p.bin.id, p.compartment, t - READ_INTERVAL_MS / 2)
                    fill = rnd.nextDouble(84.0, 95.0)
                }
                t -= READ_INTERVAL_MS
            }
            series.reverse()
            out += series
        }
        return out
    }

    // ------------------------------------------------------------ users & items

    private class DemoUser(val email: String, val home: GeoPoint, val scansPerWeek: Double)

    private fun users(rnd: Random): List<DemoUser> {
        val townWeights = DemoTowns.all.map { it.weight }
        return List(USERS) { i ->
            val email = "${firstNames[rnd.nextInt(firstNames.size)]}.${lastNames[rnd.nextInt(lastNames.size)]}${10 + i}@example.com"
            val town = DemoTowns.all[weightedIndex(townWeights, rnd)]
            DemoUser(email, jitter(town.point, 900.0, rnd), 0.4 + rnd.nextDouble().pow(2) * 3.6)
        }
    }

    private fun itemActivity(
        smartBins: List<Place>,
        allBins: List<Place>,
        windowStart: Long,
        now: Long,
        rnd: Random,
        cal: Calendar,
    ): Pair<List<ItemEvent>, List<QrSession>> {
        val bins = allBins.filter { it.type == PlaceType.BIN }
        val events = ArrayList<ItemEvent>()
        val sessions = ArrayList<QrSession>()
        val catWeights = categoryWeights.map { it.second }
        var sessionNo = 0
        for (u in users(rnd)) {
            val expected = u.scansPerWeek * DAYS / 7.0
            val count = expected.toInt() + if (rnd.nextDouble() < expected - expected.toInt()) 1 else 0
            repeat(count) {
                val scanTime = randomTime(rnd, windowStart, now, cal) ?: return@repeat
                val category = categoryWeights[weightedIndex(catWeights, rnd)].first
                events += ItemEvent("", ItemEventType.SCAN, u.email, category, scanTime, jitter(u.home, 80.0, rnd))
                if (rnd.nextDouble() >= depositChance.getValue(category)) return@repeat

                val depositTime = depositTimeAfter(scanTime, rnd, cal)
                if (depositTime > now) return@repeat
                val bin = chooseBin(u.home, category, bins, smartBins, rnd) ?: return@repeat

                var sessionId: String? = null
                if (bin.isSmartBin && rnd.nextDouble() < 0.8) {
                    sessionNo++
                    val id = "S${100_000 + sessionNo}"
                    sessionId = id
                    val extra = rnd.nextDouble() < 0.2
                    sessions += QrSession(
                        id, bin.id, u.email,
                        start = depositTime - rnd.nextLong(60_000, 180_000),
                        end = depositTime + rnd.nextLong(90_000, 240_000),
                        items = if (extra) 2 else 1,
                    )
                    if (extra) {
                        // A second item dropped in the same session without scanning it first.
                        val second = EWasteCategory.smartBinCompartments.random(rnd)
                        events += ItemEvent("", ItemEventType.DEPOSIT, u.email, second, depositTime + 60_000, bin.point, bin.id, id)
                    }
                }
                events += ItemEvent("", ItemEventType.DEPOSIT, u.email, category, depositTime, bin.point, bin.id, sessionId)
            }
        }
        val withIds = events.sortedBy { it.time }.mapIndexed { i, e -> e.copy(id = "E${200_000 + i}") }
        return withIds to sessions
    }

    /**
     * When a scanned item gets dropped off: mostly within a day, sometimes up to three days later,
     * and at the same busy hours people usually go out (lunch, evening).
     */
    private fun depositTimeAfter(scanTime: Long, rnd: Random, cal: Calendar): Long {
        val delay = if (rnd.nextDouble() < 0.7) rnd.nextLong(2 * HOUR_MS, 30 * HOUR_MS) else rnd.nextLong(30 * HOUR_MS, 72 * HOUR_MS)
        val day = startOfDay(scanTime + delay, cal)
        var t = day + weightedIndex(hourWeights.toList(), rnd) * HOUR_MS + rnd.nextLong(HOUR_MS)
        if (t < scanTime + HOUR_MS) t += DAY_MS
        return t
    }

    /** Usually the nearest bin that accepts the item; people near a SmartWaste bin often pick it. */
    private fun chooseBin(home: GeoPoint, category: EWasteCategory, bins: List<Place>, smartBins: List<Place>, rnd: Random): Place? {
        val nearestSmart = smartBins.filter { category in it.accepts }.minByOrNull { Geo.distanceMeters(home, it.point) }
        if (nearestSmart != null && Geo.distanceMeters(home, nearestSmart.point) < 5_000 && rnd.nextDouble() < 0.55) return nearestSmart
        return bins.filter { category in it.accepts }.minByOrNull { Geo.distanceMeters(home, it.point) }
    }

    /** A time in the window, weighted by hour of day and with busier weekends; null if none fits before [now]. */
    private fun randomTime(rnd: Random, windowStart: Long, now: Long, cal: Calendar): Long? {
        val dayWeights = (0 until DAYS).map { d ->
            cal.timeInMillis = windowStart + d * DAY_MS + 12 * HOUR_MS
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) 1.35 else 1.0
        }
        repeat(6) {
            val day = weightedIndex(dayWeights, rnd)
            val hour = weightedIndex(hourWeights.toList(), rnd)
            val t = windowStart + day * DAY_MS + hour * HOUR_MS + rnd.nextLong(HOUR_MS)
            if (t <= now) return t
        }
        return null
    }

    // ------------------------------------------------------------------ helpers

    private fun weightedIndex(weights: List<Double>, rnd: Random): Int {
        var x = rnd.nextDouble() * weights.sum()
        weights.forEachIndexed { i, w ->
            x -= w
            if (x < 0) return i
        }
        return weights.lastIndex
    }

    private fun gaussian(rnd: Random): Double {
        val u = rnd.nextDouble().coerceAtLeast(1e-12)
        return sqrt(-2 * ln(u)) * cos(2 * PI * rnd.nextDouble())
    }

    private fun jitter(p: GeoPoint, sigmaM: Double, rnd: Random) = GeoPoint(
        p.lat + gaussian(rnd) * sigmaM / 111_195.0,
        p.lng + gaussian(rnd) * sigmaM / (111_195.0 * cos(Math.toRadians(p.lat))),
    )

    private fun startOfDay(t: Long, cal: Calendar): Long {
        cal.timeInMillis = t
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun hourOf(t: Long, cal: Calendar): Int {
        cal.timeInMillis = t
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    private fun round1(v: Double) = round(v * 10) / 10
}

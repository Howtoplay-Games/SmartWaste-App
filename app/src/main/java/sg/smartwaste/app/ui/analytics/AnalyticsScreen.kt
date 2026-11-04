package sg.smartwaste.app.ui.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sg.smartwaste.app.data.analytics.AnalyticsPipeline
import sg.smartwaste.app.data.analytics.AnalyticsResult
import sg.smartwaste.app.data.analytics.CompartmentStatus
import sg.smartwaste.app.data.analytics.Depot
import sg.smartwaste.app.data.analytics.DemoDataGenerator
import sg.smartwaste.app.data.analytics.FULL_PCT
import sg.smartwaste.app.data.analytics.FlagRule
import sg.smartwaste.app.data.analytics.Hotspot
import sg.smartwaste.app.data.analytics.ItemEventType
import sg.smartwaste.app.data.analytics.MoveSuggestion
import sg.smartwaste.app.data.analytics.Priority
import sg.smartwaste.app.data.analytics.RawData
import sg.smartwaste.app.data.analytics.SensorRead
import sg.smartwaste.app.data.analytics.TruckRoute
import sg.smartwaste.app.data.analytics.compartmentLabel
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.Place
import sg.smartwaste.app.data.model.PlaceType
import sg.smartwaste.app.ui.components.BrandMark
import sg.smartwaste.app.ui.components.CircleIconButton
import sg.smartwaste.app.ui.components.PrimaryButton
import sg.smartwaste.app.ui.components.ProgressBar
import sg.smartwaste.app.ui.components.ScreenHeader
import sg.smartwaste.app.ui.components.StatsStrip
import sg.smartwaste.app.ui.components.TabChips
import sg.smartwaste.app.ui.components.TintTag
import sg.smartwaste.app.ui.components.TrendChart
import sg.smartwaste.app.ui.components.appViewModel
import sg.smartwaste.app.ui.components.tint
import sg.smartwaste.app.ui.theme.SW
import sg.smartwaste.app.util.Geo
import sg.smartwaste.app.util.GeoPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** The four stages and their points, worded and coloured like the "Data analytics design" slide. */
private data class Stage(val title: String, val points: List<String>, val color: Color)

private val Stages = listOf(
    Stage("Raw Data", listOf("Sensor reads", "QR session", "Item category", "Location / time"), Color(0xFF1B4F83)),
    Stage("Clean Data", listOf("Remove duplicates", "Flag impossible readings", "Separate battery stream", "Anonymise user ID"), Color(0xFF4A5C85)),
    Stage("Insights", listOf("Fullness trend", "Hotspot map", "Collection priority", "Behaviour pattern"), Color(0xFF9475B4)),
    Stage("Action", listOf("Plan pickups", "Move bins", "Send reminders", "Reward campaigns"), Color(0xFFA68AAE)),
)

@Composable
fun AnalyticsScreen(onBack: () -> Unit) {
    val vm = appViewModel { AnalyticsViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val list = rememberLazyListState()
    LaunchedEffect(s.stage) { list.animateScrollToItem(0) }
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp

    Column(Modifier.fillMaxSize().background(SW.Bg)) {
        ScreenHeader(
            title = "Data analytics",
            subtitle = "Demo · simulated data",
            onBack = onBack,
            trailing = { CircleIconButton(Icons.Outlined.Shuffle, "Generate new demo data", vm::shuffle) },
        )
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "pipeline") { PipelineHeader(s.stage, s.playing, vm::selectStage, vm::togglePlay) }
            val r = s.result
            if (s.loading || r == null) {
                item(key = "loading") { LoadingCard() }
            } else {
                item(key = "stage-${s.stage}") {
                    when (s.stage) {
                        0 -> RawStage(r)
                        1 -> CleanStage(r)
                        2 -> InsightsStage(r, s, vm::selectBin, vm::selectCompartment)
                        else -> ActionStage(r, s, vm::simulatePickups, vm::sendReminders, vm::launchCampaign)
                    }
                }
                item(key = "note") { DemoNote(r.raw) }
            }
        }
    }
}

// ============================================================== pipeline header

@Composable
private fun PipelineHeader(stage: Int, playing: Boolean, onSelect: (Int) -> Unit, onPlay: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(SW.Surface).padding(16.dp)) {
        Text("Data analytics design", style = MaterialTheme.typography.titleLarge, color = SW.Text)
        Text(
            "What we collect, how we clean it, and how decisions are made.",
            style = MaterialTheme.typography.bodySmall, color = SW.Muted,
        )
        Spacer(Modifier.height(14.dp))
        // Four overlapping circles, like the slide. Tap one to open that stage.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val d = maxWidth / 3.25f
            val step = d * 0.75f
            Box(Modifier.fillMaxWidth().height(d)) {
                Stages.forEachIndexed { i, st ->
                    val selected = i == stage
                    val scale by animateFloatAsState(if (selected) 1f else 0.88f, label = "stageScale")
                    Box(
                        Modifier
                            .offset(x = step * i)
                            .zIndex(if (selected) 10f else i.toFloat())
                            .size(d)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .background(if (selected) st.color else st.color.copy(alpha = 0.78f))
                            .then(if (selected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                            .clickable { onSelect(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp)) {
                            Text("${i + 1}", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text(
                                st.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center, maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        val st = Stages[stage]
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(st.color))
            Spacer(Modifier.width(8.dp))
            Text("${stage + 1}. ${st.title}", style = MaterialTheme.typography.titleMedium, color = SW.Text, modifier = Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(SW.Ink).clickable(onClick = onPlay).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = SW.OnInk, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (playing) "Pause" else "Play", color = SW.OnInk, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(10.dp))
        st.points.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                row.forEach { point ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = st.color, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(point, style = MaterialTheme.typography.bodySmall, color = SW.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ================================================================== 1. raw data

@Composable
private fun RawStage(r: AnalyticsResult) {
    val raw = r.raw
    val accent = Stages[0].color
    val reads = remember(raw) { sampleReads(raw) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatsStrip(
            listOf(
                Triple("Sensor reads", n(raw.reads.size), "${raw.smartBins.size} bins × 4"),
                Triple("QR sessions", n(raw.sessions.size), "${raw.days} days"),
                Triple("Item events", n(raw.events.size), "scans + drop-offs"),
            ),
            color = SW.Surface,
        )
        PointCard(1, "Sensor reads", Icons.Outlined.Sensors, accent) {
            Body(
                "Every SmartWaste bin reports the fill level of its 4 compartments every 30 minutes. " +
                    "These rows are shown exactly as they arrived, before cleaning.",
            )
            Spacer(Modifier.height(10.dp))
            MiniTable(
                listOf("Time", "Bin", "Compartment", "Fill"),
                reads.map { listOf(time(it.time), raw.binName(it.binId), compartmentLabel(it.compartment), String.format(Locale.US, "%.1f%%", it.fillPct)) },
                listOf(1.1f, 1.6f, 1.3f, 0.8f),
            )
        }
        PointCard(2, "QR session", Icons.Outlined.QrCodeScanner, accent) {
            val minutes = raw.sessions.map { (it.end - it.start) / 60_000.0 }.average().takeIf { !it.isNaN() } ?: 0.0
            val items = raw.sessions.map { it.items }.average().takeIf { !it.isNaN() } ?: 0.0
            Body(
                "Scanning a bin's QR code opens a session, which closes after the drop-off. " +
                    String.format(Locale.US, "%s sessions, %.1f min and %.1f items on average.", n(raw.sessions.size), minutes, items),
            )
            Spacer(Modifier.height(10.dp))
            MiniTable(
                listOf("Session", "Bin", "User", "Items"),
                raw.sessions.filter { it.start <= raw.generatedAt }.takeLast(3).reversed()
                    .map { listOf(it.id.removePrefix(DemoDataGenerator.REAL_PREFIX), raw.binName(it.binId), displayUser(it.userId), "${it.items}") },
                listOf(0.9f, 1.4f, 2.0f, 0.5f),
            )
        }
        PointCard(3, "Item category", Icons.Outlined.Category, accent) {
            Body("Each scan and drop-off records what kind of item it is.")
            Spacer(Modifier.height(8.dp))
            val counts = remember(raw) { raw.events.groupingBy { it.category }.eachCount().toList().sortedByDescending { it.second } }
            val max = counts.maxOfOrNull { it.second } ?: 1
            counts.forEach { (category, count) -> CategoryBar(category.shortTitle, count, max, category.tint) }
        }
        PointCard(4, "Location / time", Icons.Outlined.Place, accent) {
            Body("Every event carries GPS coordinates and a timestamp. Scans are recorded where the item is; drop-offs at the bin.")
            Spacer(Modifier.height(10.dp))
            MiniTable(
                listOf("Time", "Event", "Location", "User"),
                raw.events.filter { it.time <= raw.generatedAt }.takeLast(3).reversed().map {
                    listOf(
                        time(it.time),
                        "${eventLabel(it.type)} · ${it.category.shortTitle}",
                        String.format(Locale.US, "%.4f, %.4f", it.location.lat, it.location.lng),
                        displayUser(it.userId),
                    )
                },
                listOf(1f, 1.5f, 1.5f, 1.7f),
            )
            if (raw.realEvents > 0) {
                Spacer(Modifier.height(10.dp))
                TintTag("Includes ${raw.realEvents} real drop-off(s) from this device", accent)
            }
        }
    }
}

/** A few raw rows, including a duplicated reading and an impossible one, so the problems are visible. */
private fun sampleReads(raw: RawData): List<SensorRead> {
    fun key(r: SensorRead) = Triple(r.binId, r.compartment, r.time)
    val counts = raw.reads.groupingBy { key(it) }.eachCount()
    val dupKey = raw.reads.lastOrNull { (counts[key(it)] ?: 0) > 1 }?.let { key(it) }
    val dupRows = if (dupKey == null) emptyList() else raw.reads.filter { key(it) == dupKey }.take(2)
    val bad = raw.reads.lastOrNull { it.fillPct < 0.0 || it.fillPct > 100.0 }
    return (raw.reads.takeLast(2) + dupRows + listOfNotNull(bad)).distinctBy { it.id }.sortedByDescending { it.time }
}

// ================================================================ 2. clean data

@Composable
private fun CleanStage(r: AnalyticsResult) {
    val raw = r.raw
    val c = r.clean
    val accent = Stages[1].color
    val flaggedRows = remember(c) {
        c.flaggedReads.groupBy { it.rule }.values.flatMap { it.take(2) }.map { f ->
            listOf("${raw.binName(f.row.binId)} · ${compartmentLabel(f.row.compartment)}", String.format(Locale.US, "%.1f%%", f.row.fillPct), f.detail)
        } + c.flaggedEvents.groupBy { it.rule }.values.flatMap { it.take(1) }.map { f ->
            listOf("${eventLabel(f.row.type)} ${f.row.id}", f.rule.label, f.detail)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SW.Surface).padding(16.dp)) {
            Text("Rows in → rows out", style = MaterialTheme.typography.titleMedium, color = SW.Text)
            Spacer(Modifier.height(4.dp))
            Body("${n(raw.totalRows)} raw rows → ${n(c.totalRows)} clean rows. ${n(raw.totalRows - c.totalRows)} were duplicates or impossible.")
            Spacer(Modifier.height(10.dp))
            ProgressBar(c.totalRows.toFloat() / raw.totalRows.coerceAtLeast(1), accent, height = 10.dp)
        }
        PointCard(1, "Remove duplicates", Icons.Outlined.ContentCopy, accent) {
            Body(
                "Gateways retry and phones re-send when they get back online, so the same row can arrive twice. " +
                    "Rows with the same bin, compartment and time, or the same event or session ID, are kept once.",
            )
            Spacer(Modifier.height(8.dp))
            Fact("Sensor reads received twice", "−${n(c.duplicateReads)}")
            Fact("Events re-sent after offline sync", "−${n(c.duplicateEvents)}")
            Fact("QR sessions uploaded twice", "−${n(c.duplicateSessions)}")
        }
        PointCard(2, "Flag impossible readings", Icons.Outlined.ReportProblem, accent) {
            Body("Rows that can't be true are set aside instead of being analysed.")
            Spacer(Modifier.height(8.dp))
            FlagRule.entries.forEach { rule ->
                val count = c.flaggedReads.count { it.rule == rule } + c.flaggedEvents.count { it.rule == rule }
                Fact(ruleDescription(rule), n(count), SW.Danger)
            }
            Spacer(Modifier.height(10.dp))
            MiniTable(listOf("Row", "Value", "Why"), flaggedRows, listOf(1.6f, 1f, 1.6f), highlight = { true })
        }
        PointCard(3, "Separate battery stream", Icons.Outlined.BatteryChargingFull, accent) {
            Body(
                "Batteries are a fire risk and go to a licensed battery collector, so the battery chute and " +
                    "battery items get their own stream, their own insights and their own pickups.",
            )
            Spacer(Modifier.height(10.dp))
            StreamRow("Battery stream", c.batteryReads.size, c.batteryEvents.size, SW.Warn)
            StreamRow("Main stream", c.generalReads.size, c.generalEvents.size, accent)
        }
        PointCard(4, "Anonymise user ID", Icons.Outlined.VisibilityOff, accent) {
            Body(
                "Before analysis, every email is replaced with a salted SHA-256 ID. The same person always gets " +
                    "the same ID, but the ID can't be turned back into an email.",
            )
            Spacer(Modifier.height(10.dp))
            MiniTable(listOf("Raw user", "Anonymous ID"), c.sampleIds.map { (user, id) -> listOf(displayUser(user), id) }, listOf(1.7f, 1f))
            Spacer(Modifier.height(6.dp))
            Fact("Users anonymised", n(c.users))
        }
    }
}

private fun ruleDescription(rule: FlagRule) = when (rule) {
    FlagRule.OUT_OF_RANGE -> "Fill below 0% or above 100%"
    FlagRule.IMPOSSIBLE_JUMP -> "Rise of ${AnalyticsPipeline.SPIKE_POINTS.roundToInt()}+ pts within 2 h"
    FlagRule.OUTSIDE_SINGAPORE -> "GPS outside Singapore"
    FlagRule.FUTURE_TIME -> "Timestamp in the future"
}

@Composable
private fun StreamRow(label: String, reads: Int, items: Int, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = SW.Text, modifier = Modifier.weight(1f))
        Text("${n(reads)} reads · ${n(items)} items", style = MaterialTheme.typography.bodySmall, color = SW.Subtle)
    }
}

// =================================================================== 3. insights

@Composable
private fun InsightsStage(
    r: AnalyticsResult,
    s: AnalyticsUiState,
    onBin: (String) -> Unit,
    onCompartment: (EWasteCategory) -> Unit,
) {
    val ins = r.insights
    val accent = Stages[2].color
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PointCard(1, "Fullness trend", Icons.AutoMirrored.Outlined.ShowChart, accent) {
            val binId = s.selectedBin ?: r.raw.smartBins.firstOrNull()?.id.orEmpty()
            TabChips(r.raw.smartBins.map { it.id }, binId, { r.raw.binName(it) }, onBin)
            Spacer(Modifier.height(8.dp))
            TabChips(EWasteCategory.smartBinCompartments, s.selectedCompartment, { compartmentLabel(it) }, onCompartment)
            val series = ins.trends.firstOrNull { it.binId == binId }?.series?.get(s.selectedCompartment)
            if (series != null && series.hourly.size >= 2) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${series.current.roundToInt()}%", color = SW.Text, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "now · fills ~${series.ratePerDay.roundToInt()}%/day · emptied ${series.pickups}× this week",
                        style = MaterialTheme.typography.bodySmall, color = SW.Muted, modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                TrendChart(series.hourly, Modifier.fillMaxWidth().height(130.dp), color = accent)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (0 until r.raw.days).forEach { d ->
                        Text(
                            SimpleDateFormat("EEE", Locale.US).format(Date(r.raw.windowStart + d * 86_400_000L)),
                            style = MaterialTheme.typography.labelSmall, color = SW.Muted,
                        )
                    }
                }
            }
        }
        PointCard(2, "Hotspot map", Icons.Outlined.Map, accent) {
            Body("Where people scan e-waste (${n(ins.behaviour.scans)} scans in ~1.3 km cells). Dots are bins; ringed dots are SmartWaste bins.")
            Spacer(Modifier.height(10.dp))
            HotspotMap(ins.hotspots, r.raw.allBins, accent)
            Spacer(Modifier.height(8.dp))
            ins.hotspots.take(3).forEachIndexed { i, h ->
                Fact("${i + 1}. ${h.area}", "${h.scans} scans · nearest bin ${Geo.formatDistance(h.nearestBinM)}")
            }
        }
        PointCard(3, "Collection priority", Icons.Outlined.PriorityHigh, accent) {
            Body("Each compartment's recent fill rate predicts when it reaches ${FULL_PCT.roundToInt()}%.")
            Spacer(Modifier.height(4.dp))
            ins.priorities.take(6).forEach { PriorityRow(it) }
        }
        PointCard(4, "Behaviour pattern", Icons.Outlined.Insights, accent) {
            val b = ins.behaviour
            Body("Drop-offs by hour of day (${n(b.deposits)} this week)")
            Spacer(Modifier.height(10.dp))
            HourBars(b.depositsByHour, b.peakHour, accent)
            Spacer(Modifier.height(12.dp))
            StatGrid(
                listOf(
                    "Busiest hour" to AnalyticsPipeline.hourLabel(b.peakHour),
                    "On weekends" to pct(b.weekendShare),
                    "Repeat users" to pct(b.repeatUserShare),
                    "Scan → drop-off" to pct(b.scanToDeposit),
                ),
            )
            Spacer(Modifier.height(10.dp))
            Body("Most dropped off: " + b.depositsByCategory.take(3).joinToString { (c, count) -> "${c.shortTitle} (${n(count)})" })
        }
    }
}

@Composable
private fun PriorityRow(p: CompartmentStatus) {
    val tint = priorityColor(p.priority)
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(p.binName, style = MaterialTheme.typography.titleSmall, color = SW.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${compartmentLabel(p.compartment)} · ${p.current.roundToInt()}% · ${eta(p.hoursToFull)}",
                style = MaterialTheme.typography.bodySmall, color = SW.Muted,
            )
            Spacer(Modifier.height(4.dp))
            ProgressBar((p.current / 100.0).toFloat(), tint)
        }
        Spacer(Modifier.width(10.dp))
        TintTag(p.priority.label, tint)
    }
}

private fun priorityColor(p: Priority) = when (p) {
    Priority.NOW -> SW.Danger
    Priority.TODAY -> SW.Warn
    Priority.TOMORROW -> SW.Info
    Priority.OK -> SW.Success
}

private fun eta(hoursToFull: Double?): String = when {
    hoursToFull == null -> "not filling"
    hoursToFull <= 0.0 -> "full now"
    hoursToFull < 1.0 -> "full within 1 h"
    hoursToFull <= 48.0 -> "full in ~${hoursToFull.roundToInt()} h"
    else -> "full in ~${(hoursToFull / 24).roundToInt()} days"
}

@Composable
private fun HourBars(byHour: List<Int>, peak: Int, accent: Color) {
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val max = (byHour.maxOrNull() ?: 0).coerceAtLeast(1)
        val slot = size.width / 24f
        val barW = slot * 0.62f
        byHour.forEachIndexed { h, v ->
            val barH = (size.height * v / max).coerceAtLeast(2f)
            drawRoundRect(
                color = if (h == peak) accent else accent.copy(alpha = 0.3f),
                topLeft = Offset(h * slot + (slot - barW) / 2, size.height - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2, barW / 2),
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("12am", "6am", "12pm", "6pm", "11pm").forEach {
            Text(it, style = MaterialTheme.typography.labelSmall, color = SW.Muted)
        }
    }
}

// Projection box for the schematic map of Singapore.
private const val MIN_LAT = 1.19
private const val MAX_LAT = 1.48
private const val MIN_LNG = 103.59
private const val MAX_LNG = 104.06
private const val MAP_ASPECT = 1.62f

/** A simplified Singapore coastline (lat, lng), clockwise from Tuas. Schematic only. */
private val SINGAPORE_OUTLINE = listOf(
    1.305 to 103.610, 1.333 to 103.630, 1.352 to 103.660, 1.378 to 103.676, 1.414 to 103.686,
    1.440 to 103.705, 1.446 to 103.745, 1.452 to 103.772, 1.460 to 103.798, 1.465 to 103.822,
    1.452 to 103.845, 1.430 to 103.870, 1.420 to 103.893, 1.418 to 103.912, 1.398 to 103.940,
    1.388 to 103.958, 1.393 to 103.985, 1.380 to 104.010, 1.355 to 104.030, 1.330 to 104.000,
    1.310 to 103.965, 1.299 to 103.930, 1.290 to 103.890, 1.275 to 103.866, 1.258 to 103.845,
    1.258 to 103.818, 1.280 to 103.790, 1.290 to 103.760, 1.285 to 103.735, 1.300 to 103.700,
    1.290 to 103.660, 1.300 to 103.630,
)

@Composable
private fun HotspotMap(hotspots: List<Hotspot>, bins: List<Place>, accent: Color) {
    val measurer = rememberTextMeasurer()
    val badgeStyle = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(MAP_ASPECT).clip(RoundedCornerShape(18.dp)).background(Color(0xFFE8F1F5)),
    ) {
        fun project(lat: Double, lng: Double) = Offset(
            ((lng - MIN_LNG) / (MAX_LNG - MIN_LNG) * size.width).toFloat(),
            ((MAX_LAT - lat) / (MAX_LAT - MIN_LAT) * size.height).toFloat(),
        )
        fun projectPoint(p: GeoPoint) = project(p.lat, p.lng)

        val island = Path().apply {
            SINGAPORE_OUTLINE.forEachIndexed { i, (lat, lng) ->
                val o = project(lat, lng)
                if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
            }
            close()
        }
        drawPath(island, SW.Surface)
        drawPath(island, SW.Line, style = Stroke(1.5.dp.toPx()))

        val max = (hotspots.maxOfOrNull { it.scans } ?: 0).coerceAtLeast(1)
        hotspots.forEach { h ->
            val c = projectPoint(h.center)
            val radius = (8f + 22f * sqrt(h.scans / max.toFloat())).dp.toPx()
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0f)), c, radius), radius, c)
        }
        bins.filter { it.type == PlaceType.BIN }.forEach { b ->
            val c = projectPoint(b.point)
            if (b.isSmartBin) {
                drawCircle(Color.White, 5.5.dp.toPx(), c)
                drawCircle(SW.Ink, 4.dp.toPx(), c)
            } else {
                drawCircle(SW.Subtle, 2.dp.toPx(), c)
            }
        }
        hotspots.take(3).forEachIndexed { i, h ->
            val c = projectPoint(h.center)
            drawCircle(SW.Ink, 9.dp.toPx(), c)
            val label = measurer.measure("${i + 1}", badgeStyle)
            drawText(label, topLeft = Offset(c.x - label.size.width / 2f, c.y - label.size.height / 2f))
        }
    }
}

// ===================================================================== 4. action

@Composable
private fun ActionStage(
    r: AnalyticsResult,
    s: AnalyticsUiState,
    onPickups: () -> Unit,
    onRemind: () -> Unit,
    onCampaign: () -> Unit,
) {
    val a = r.actions
    val accent = Stages[3].color
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PointCard(1, "Plan pickups", Icons.Outlined.LocalShipping, accent) {
            Body("Compartments due within 24 h go on the next route. Batteries travel on a separate, fire-safe truck.")
            Spacer(Modifier.height(10.dp))
            RouteBlock("Main truck", a.pickups.depot, a.pickups.general)
            Spacer(Modifier.height(8.dp))
            RouteBlock("Battery truck", a.pickups.depot, a.pickups.battery)
            Spacer(Modifier.height(12.dp))
            if (r.collected.count > 0) {
                TintTag("Emptied ${r.collected.count} compartment group(s); priorities updated", SW.Success, icon = Icons.Rounded.CheckCircle)
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton(
                "Simulate pickups", onPickups, Modifier.fillMaxWidth(),
                enabled = a.pickups.general.stops.isNotEmpty() || a.pickups.battery.stops.isNotEmpty(),
                leading = Icons.Outlined.LocalShipping,
            )
        }
        PointCard(2, "Move bins", Icons.Outlined.OpenWith, accent) {
            if (a.moves.isEmpty()) {
                Body("Every busy area already has an e-waste bin within 1.5 km.")
            } else {
                Body("Hotspots with no e-waste bin within 1.5 km, paired with the least-used SmartWaste bins.")
                a.moves.forEach { MoveRow(it) }
            }
        }
        PointCard(3, "Send reminders", Icons.Outlined.NotificationsActive, accent) {
            val rem = a.reminders
            Body("${n(rem.users)} users scanned ${n(rem.items)} items 2+ days ago and haven't dropped them off yet.")
            if (rem.sampleTitle != null && rem.sampleMessage != null) {
                Spacer(Modifier.height(10.dp))
                NotificationPreview(rem.sampleTitle, rem.sampleMessage)
            }
            Spacer(Modifier.height(12.dp))
            if (s.remindersSent) {
                TintTag("Sent to ${n(rem.users)} anonymous IDs (demo, nothing was sent)", SW.Success, icon = Icons.Rounded.CheckCircle)
            } else {
                PrimaryButton("Send reminders (demo)", onRemind, Modifier.fillMaxWidth(), enabled = rem.users > 0, leading = Icons.Outlined.NotificationsActive)
            }
        }
        PointCard(4, "Reward campaigns", Icons.Outlined.Campaign, accent) {
            val c = a.campaign
            if (c == null) {
                Body("Not enough data for a campaign yet.")
            } else {
                Text(c.title, style = MaterialTheme.typography.titleLarge, color = SW.Text)
                Spacer(Modifier.height(4.dp))
                Body(c.detail)
                Spacer(Modifier.height(8.dp))
                Fact("${c.category.shortTitle}: scanned → dropped off", pct(c.conversion))
                Fact("Campaign goal", pct(c.goal), accent)
                Spacer(Modifier.height(6.dp))
                ProgressBar((c.conversion / c.goal).toFloat().coerceIn(0f, 1f), accent, height = 8.dp)
                Spacer(Modifier.height(12.dp))
                if (s.campaignLaunched) {
                    TintTag("Live in the demo (real points aren't changed)", SW.Success, icon = Icons.Rounded.CheckCircle)
                } else {
                    PrimaryButton("Launch campaign (demo)", onCampaign, Modifier.fillMaxWidth(), leading = Icons.Outlined.Campaign)
                }
            }
        }
    }
}

@Composable
private fun RouteBlock(title: String, depot: Depot, route: TruckRoute) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SW.SurfaceAlt).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = SW.Text, modifier = Modifier.weight(1f))
            Text(
                if (route.stops.isEmpty()) "No stops" else String.format(Locale.US, "%d stop(s) · %.1f km", route.stops.size, route.km),
                style = MaterialTheme.typography.labelMedium, color = SW.Muted,
            )
        }
        if (route.stops.isEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Nothing due in the next 24 h.", style = MaterialTheme.typography.bodySmall, color = SW.Muted)
        } else {
            StopLine("D", depot.name.substringAfter("· "), "Start")
            route.stops.forEachIndexed { i, stop ->
                StopLine("${i + 1}", stop.binName, "${stop.compartments.joinToString { compartmentLabel(it) }} · ${stop.maxFill.roundToInt()}%")
            }
            StopLine("D", depot.name.substringAfter("· "), "Back to the depot")
        }
    }
}

@Composable
private fun StopLine(marker: String, title: String, detail: String) {
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(SW.Ink), contentAlignment = Alignment.Center) {
            Text(marker, color = SW.OnInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = SW.Text)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = SW.Muted)
        }
    }
}

@Composable
private fun MoveRow(m: MoveSuggestion) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(16.dp)).background(SW.SurfaceAlt).padding(12.dp)) {
        Text("Move ${m.binName} → ${m.to.area}", style = MaterialTheme.typography.titleSmall, color = SW.Text)
        Spacer(Modifier.height(2.dp))
        Text(
            "${m.binName}: ${n(m.dropOffsThisWeek)} drop-offs this week. ${m.to.area}: ${n(m.to.scans)} scans, " +
                "nearest e-waste bin ${Geo.formatDistance(m.to.nearestBinM)} away${m.to.nearestBinName?.let { " ($it)" }.orEmpty()}.",
            style = MaterialTheme.typography.bodySmall, color = SW.Subtle,
        )
    }
}

@Composable
private fun NotificationPreview(title: String, message: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SW.SurfaceAlt).padding(12.dp)) {
        BrandMark(size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Row {
                Text("SmartWaste", style = MaterialTheme.typography.labelMedium, color = SW.Text)
                Text(" · now", style = MaterialTheme.typography.labelMedium, color = SW.Muted)
            }
            Text(title, style = MaterialTheme.typography.titleSmall, color = SW.Text)
            Text(message, style = MaterialTheme.typography.bodySmall, color = SW.Subtle)
        }
    }
}

// =================================================================== shared bits

@Composable
private fun PointCard(number: Int, title: String, icon: ImageVector, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SW.Surface).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Point $number", style = MaterialTheme.typography.bodySmall, color = SW.Muted)
                Text(title, style = MaterialTheme.typography.titleMedium, color = SW.Text)
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = SW.Subtle)
}

@Composable
private fun Fact(label: String, value: String, valueColor: Color = SW.Text) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SW.Subtle, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, color = valueColor)
    }
}

@Composable
private fun CategoryBar(label: String, count: Int, max: Int, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SW.Subtle, modifier = Modifier.width(110.dp), maxLines = 1)
        ProgressBar(count.toFloat() / max, color, Modifier.weight(1f), height = 8.dp)
        Spacer(Modifier.width(10.dp))
        Text(n(count), style = MaterialTheme.typography.labelMedium, color = SW.Text, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun StatGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(SW.SurfaceAlt).padding(12.dp)) {
                        Text(value, style = MaterialTheme.typography.titleLarge, color = SW.Text)
                        Text(label, style = MaterialTheme.typography.bodySmall, color = SW.Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniTable(headers: List<String>, rows: List<List<String>>, weights: List<Float>, highlight: (Int) -> Boolean = { false }) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SW.SurfaceAlt).padding(horizontal = 10.dp, vertical = 4.dp)) {
        TableRow(headers, weights, SW.Muted, FontWeight.Medium)
        rows.forEachIndexed { i, row ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(SW.Line))
            TableRow(row, weights, if (highlight(i)) SW.Danger else SW.Text, FontWeight.Normal)
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, weights: List<Float>, color: Color, fontWeight: FontWeight) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        cells.forEachIndexed { i, cell ->
            Text(
                cell, color = color, fontSize = 11.sp, fontWeight = fontWeight, fontFamily = FontFamily.Monospace,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(weights.getOrElse(i) { 1f }).padding(end = 6.dp),
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SW.Surface).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = SW.Ink)
        Spacer(Modifier.width(12.dp))
        Text("Building a week of demo data…", style = MaterialTheme.typography.bodyMedium, color = SW.Subtle)
    }
}

@Composable
private fun DemoNote(raw: RawData) {
    Text(
        "Demo data: ${raw.days} days of simulated sensor readings for the ${raw.smartBins.size} SmartWaste pilot bins and " +
            "${n(DemoDataGenerator.USERS)} made-up users (example.com addresses), with realistic errors added on purpose. " +
            (if (raw.realEvents > 0) "Your ${raw.realEvents} real drop-off(s) from this week are included and anonymised. " else "") +
            "Nothing is sent anywhere and your points don't change. Tap the shuffle button for new data.",
        style = MaterialTheme.typography.labelSmall, color = SW.Muted,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

// ===================================================================== formatting

private fun n(v: Int): String = NumberFormat.getIntegerInstance(Locale.US).format(v)

private fun pct(v: Double): String = "${(v * 100).roundToInt()}%"

private fun time(t: Long): String = SimpleDateFormat("EEE HH:mm", Locale.US).format(Date(t))

private fun eventLabel(type: ItemEventType) = if (type == ItemEventType.SCAN) "Scan" else "Drop-off"

/** Made-up demo addresses are shown as is; a real email (the device's user) is masked for screen recordings. */
private fun displayUser(id: String): String {
    if (id.endsWith("@example.com")) return id
    val at = id.indexOf('@')
    return if (at <= 0) "•••" else id.first() + "•••" + id.substring(at)
}

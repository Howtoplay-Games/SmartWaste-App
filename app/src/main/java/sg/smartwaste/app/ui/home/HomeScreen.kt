package sg.smartwaste.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import sg.smartwaste.app.data.KnowledgeBase
import sg.smartwaste.app.data.db.DepositEntity
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.PlaceType
import sg.smartwaste.app.data.model.PlaceWithDistance
import sg.smartwaste.app.ui.components.Avatar
import sg.smartwaste.app.ui.components.CircleIconButton
import sg.smartwaste.app.ui.components.CoverArt
import sg.smartwaste.app.ui.components.DayCard
import sg.smartwaste.app.ui.components.FrostTag
import sg.smartwaste.app.ui.components.FrostedCircleButton
import sg.smartwaste.app.ui.components.LOCATION_PERMISSIONS
import sg.smartwaste.app.ui.components.ListingCard
import sg.smartwaste.app.ui.components.PillChip
import sg.smartwaste.app.ui.components.ProgressBar
import sg.smartwaste.app.ui.components.RatingChip
import sg.smartwaste.app.ui.components.SearchPill
import sg.smartwaste.app.ui.components.SectionHeader
import sg.smartwaste.app.ui.components.SeeMorePill
import sg.smartwaste.app.ui.components.StatsStrip
import sg.smartwaste.app.ui.components.Tones
import sg.smartwaste.app.ui.components.TrendChart
import sg.smartwaste.app.ui.components.appViewModel
import sg.smartwaste.app.ui.components.icon
import sg.smartwaste.app.ui.components.placeIcon
import sg.smartwaste.app.ui.components.placeTones
import sg.smartwaste.app.ui.components.rememberPermissionState
import sg.smartwaste.app.ui.components.tint
import sg.smartwaste.app.ui.components.tones
import sg.smartwaste.app.ui.map.openUrl
import sg.smartwaste.app.ui.theme.SW
import sg.smartwaste.app.util.Geo
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

/** Monthly item goal per category used for the category progress cards. */
private const val MONTHLY_GOAL = 5

@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onOpenMap: (mode: String?, category: String?, placeId: String?) -> Unit,
    onRewards: () -> Unit,
    onProfile: () -> Unit,
    onDeposit: (placeId: String?) -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val selectedCategory by vm.category.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = rememberPermissionState(*LOCATION_PERMISSIONS) { vm.refreshLocation() }
    var showUpdates by remember { mutableStateOf(false) }
    val fmt = NumberFormat.getIntegerInstance(Locale.US)
    val month = SimpleDateFormat("MMMM", Locale.US).format(Date())

    LaunchedEffect(Unit) {
        if (permission.granted) vm.refreshLocation() else permission.request()
    }

    LazyColumn(
        Modifier.fillMaxSize().background(SW.Bg),
        contentPadding = PaddingValues(bottom = 130.dp),
    ) {
        // ------------------------------------------------ greeting + search
        item {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Hello, ${s.name.substringBefore(' ').ifBlank { "there" }}",
                        style = MaterialTheme.typography.headlineSmall, color = SW.Text,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text("Welcome to SmartWaste", style = MaterialTheme.typography.bodySmall, color = SW.Muted)
                }
                Spacer(Modifier.width(12.dp))
                Avatar(s.name, size = 50.dp, onClick = onProfile)
            }
            Spacer(Modifier.height(18.dp))
            SearchPill(
                "Search bins, buyers, malls",
                onClick = { onOpenMap(null, null, null) },
                onFilter = { onOpenMap("bins", selectedCategory?.name, null) },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(26.dp))
        }

        // --------------------------------------------- category chips + deck
        item {
            Text(
                "What are you recycling?", style = MaterialTheme.typography.titleLarge, color = SW.Text,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { PillChip("All", selected = selectedCategory == null, onClick = { vm.selectCategory(null) }) }
                items(EWasteCategory.entries) { c ->
                    PillChip(c.shortTitle, selected = selectedCategory == c, onClick = { vm.selectCategory(c) })
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        item {
            BinDeck(
                bins = s.nearbyBins,
                locationKnown = s.locationKnown,
                category = selectedCategory,
                onOpen = { id -> onOpenMap("bins", null, id) },
                onVerify = { id -> onDeposit(id) },
                onOpenMap = { mode -> onOpenMap(mode, selectedCategory?.name, null) },
            )
        }

        // ---------------------------------------------------------- points
        item {
            Spacer(Modifier.height(24.dp))
            PointsCard(s, fmt, onRewards = onRewards, onUpdates = { showUpdates = true })
        }
        item {
            Spacer(Modifier.height(16.dp))
            ScanCard(selectedCategory, onScan, onDeposit = { onDeposit(null) })
        }

        // ------------------------------------------- per-category progress
        item {
            Spacer(Modifier.height(28.dp))
            SectionHeader("Recycled by category", action = "See bins", onAction = { onOpenMap("bins", selectedCategory?.name, null) })
            Spacer(Modifier.height(14.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(EWasteCategory.entries) { c ->
                    CategoryCard(c, s.impact.byCategory[c] ?: 0, selected = selectedCategory == c) { vm.selectCategory(c) }
                }
            }
        }

        // ------------------------------------------------------------ sell
        item {
            Spacer(Modifier.height(28.dp))
            SectionHeader("Sell your e-waste", action = "See all", onAction = { onOpenMap("buyers", selectedCategory?.name, null) })
            Spacer(Modifier.height(14.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(s.nearbyBuyers, key = { it.place.id }) { item ->
                    val p = item.place
                    val phone = p.phone
                    ListingCard(
                        title = p.name,
                        meta = "${Geo.formatDistance(item.distanceMeters)} • ${p.buyer?.buys ?: "Used electronics"}",
                        chip = p.buyer?.pays ?: "Get a quote",
                        chipNote = if (p.buyer?.pickup == true) "Pickup" else null,
                        chipIcon = Icons.Outlined.Payments,
                        onClick = { onOpenMap("buyers", null, p.id) },
                        coverAction = {
                            if (phone != null) FrostedCircleButton(Icons.Outlined.Call, "Call ${p.name}", { openUrl(context, "tel:$phone") })
                        },
                        cover = {
                            CoverArt(placeTones(p), Modifier.matchParentSize(), seed = p.id.hashCode(), icon = Icons.Rounded.Store, iconSize = 44.dp)
                        },
                    )
                }
                items(s.onlineBuyers, key = { "online-" + it.name }) { b ->
                    ListingCard(
                        title = b.name,
                        meta = "Online • ${b.buys}",
                        chip = b.pays,
                        chipNote = null,
                        chipIcon = Icons.Outlined.Payments,
                        onClick = { openUrl(context, b.website) },
                        coverAction = { FrostTag("Online", icon = Icons.Rounded.Language) },
                        cover = {
                            CoverArt(Tones.forKey(b.name), Modifier.matchParentSize(), seed = b.name.hashCode(), icon = Icons.Rounded.Language, iconSize = 44.dp)
                        },
                    )
                }
            }
        }

        // ---------------------------------------------------------- latest
        item {
            Spacer(Modifier.height(28.dp))
            SectionHeader("Recent deposits", action = if (s.latest.isNotEmpty()) "See all" else null, onAction = onProfile)
            Spacer(Modifier.height(12.dp))
            if (s.latest.isEmpty()) {
                Text(
                    "No deposits yet. Scan an item and drop it in a bin to earn your first points.",
                    color = SW.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        items(s.latest, key = { "latest-${it.id}" }) { d -> LatestRow(d) }

        item {
            Spacer(Modifier.height(20.dp))
            TrendCard(s.trend, month)
        }
        item {
            Spacer(Modifier.height(16.dp))
            SafetyTipCard()
        }
    }

    if (showUpdates) {
        AlertDialog(
            onDismissRequest = { showUpdates = false },
            containerColor = SW.Surface,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Updates", color = SW.Text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• ${s.redeemableCount} reward(s) you can redeem now", color = SW.Subtle)
                    Text("• ${s.nearlyFullBins} SmartWaste bin(s) are over 75% full. The map skips full bins for you.", color = SW.Subtle)
                    Text("• Tip: ${KnowledgeBase.safetyTips.first()}", color = SW.Subtle)
                }
            },
            confirmButton = {
                TextButton(onClick = { showUpdates = false; onRewards() }) { Text("Open rewards", color = SW.Ink, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { showUpdates = false }) { Text("Close", color = SW.Muted) } },
        )
    }
}

/** Stacked carousel of the nearest bins: the centre card sits in front of its neighbours. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BinDeck(
    bins: List<PlaceWithDistance>,
    locationKnown: Boolean,
    category: EWasteCategory?,
    onOpen: (String) -> Unit,
    onVerify: (String) -> Unit,
    onOpenMap: (mode: String) -> Unit,
) {
    if (bins.isEmpty()) {
        EmptyDeckCard(category, onOpenMap)
        return
    }
    val pager = rememberPagerState(pageCount = { bins.size })
    HorizontalPager(
        state = pager,
        contentPadding = PaddingValues(horizontal = 40.dp),
        modifier = Modifier.fillMaxWidth().height(390.dp),
    ) { page ->
        val item = bins[page]
        BinHeroCard(
            item = item,
            locationKnown = locationKnown,
            onOpen = { onOpen(item.place.id) },
            onVerify = { onVerify(item.place.id) },
            modifier = Modifier
                .zIndex(if (page == pager.currentPage) 1f else 0f)
                .graphicsLayer {
                    val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
                    val dist = offset.absoluteValue.coerceIn(0f, 1f)
                    val scale = 1f - 0.14f * dist
                    scaleX = scale
                    scaleY = scale
                    // Tuck the side cards behind the centre one.
                    translationX = offset.coerceIn(-1f, 1f) * size.width * 0.16f
                },
        )
    }
}

@Composable
private fun BinHeroCard(
    item: PlaceWithDistance,
    locationKnown: Boolean,
    onOpen: () -> Unit,
    onVerify: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = item.place
    // Without a location fix, distances are measured from the city centre.
    val distance = Geo.formatDistance(item.distanceMeters) + if (locationKnown) "" else " from city centre"
    val types = p.accepts.size
    val typesLabel = "Accepts $types item type${if (types == 1) "" else "s"}"
    Box(modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).clickable(onClick = onOpen)) {
        CoverArt(
            placeTones(p), Modifier.matchParentSize(), seed = p.id.hashCode(), icon = placeIcon(p),
            iconSize = 84.dp, iconAlignment = BiasAlignment(0f, -0.45f), scrim = true,
        )
        if (p.type == PlaceType.BIN) {
            FrostedCircleButton(
                Icons.Outlined.TaskAlt, "I'm at this bin: verify and earn points", onVerify,
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
            )
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(horizontal = 8.dp)) {
                Text(
                    "${if (p.isSmartBin) "Smart bin" else "E-waste bin"} · $distance",
                    color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall, maxLines = 1,
                )
                Text(
                    p.name, color = Color.White, style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val fill = p.fillLevel
                    RatingChip(if (fill != null) "$fill% full" else "$types types", icon = Icons.Outlined.Delete, onImage = true)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (fill != null) typesLabel else p.operator,
                        color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            SeeMorePill("View on map", onOpen)
        }
    }
}

/** Shown when no bin matches: items that don't fit in bins get a take-back prompt instead. */
@Composable
private fun EmptyDeckCard(category: EWasteCategory?, onOpenMap: (mode: String) -> Unit) {
    val takeBack = category != null && !category.binFriendly
    val mode = if (takeBack) "buyers" else "bins"
    Box(
        Modifier.padding(horizontal = 40.dp).fillMaxWidth().height(390.dp).clip(RoundedCornerShape(32.dp))
            .clickable { onOpenMap(mode) },
    ) {
        CoverArt(
            category?.tones ?: Tones.Alpine, Modifier.matchParentSize(), seed = 3, icon = category?.icon ?: Icons.Rounded.Map,
            iconSize = 84.dp, iconAlignment = BiasAlignment(0f, -0.45f), scrim = true,
        )
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(horizontal = 8.dp)) {
                Text("Singapore", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                Text(
                    if (takeBack) "Too big for a bin" else "No bins to show yet",
                    color = Color.White, style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (takeBack) "Use retailer take-back, a licensed collector, or sell it if it still works."
                    else "Open the map to browse every e-waste bin and buyer.",
                    color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(14.dp))
            SeeMorePill(if (takeBack) "Find take-back" else "Open map", { onOpenMap(mode) })
        }
    }
}

@Composable
private fun PointsCard(s: HomeState, fmt: NumberFormat, onRewards: () -> Unit, onUpdates: () -> Unit) {
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(SW.Surface)
            .clickable(onClick = onRewards).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Points balance", style = MaterialTheme.typography.bodySmall, color = SW.Muted)
                Text(
                    buildAnnotatedString {
                        append(fmt.format(s.balance))
                        withStyle(SpanStyle(color = SW.Muted, fontSize = 18.sp, fontWeight = FontWeight.Medium)) { append(" pts") }
                    },
                    color = SW.Text, fontSize = 34.sp, fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingChip(s.tier.name, icon = Icons.Outlined.EmojiEvents)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        s.nextTier?.let { "${fmt.format(it.minLifetimePoints - s.lifetime)} pts to ${it.name}" } ?: "Top tier reached",
                        style = MaterialTheme.typography.bodySmall, color = SW.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            CircleIconButton(Icons.Outlined.Notifications, "Updates", onUpdates, container = SW.SurfaceAlt)
        }
        Spacer(Modifier.height(16.dp))
        val diff = s.monthItems - s.lastMonthItems
        StatsStrip(
            listOf(
                Triple("Items this month", "${s.monthItems}", if (diff >= 0) "+$diff vs last month" else "$diff vs last month"),
                Triple("Deposits", "${s.monthDeposits}", "${s.impact.verifiedDeposits} verified total"),
                Triple("CO₂e avoided", String.format(Locale.US, "%.1f kg", s.monthCo2), "this month"),
            ),
        )
    }
}

/** Wide cover card that opens the AI scanner, with a shortcut to verify a deposit at any bin. */
@Composable
private fun ScanCard(category: EWasteCategory?, onScan: () -> Unit, onDeposit: () -> Unit) {
    Box(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(220.dp).clip(RoundedCornerShape(30.dp)).clickable(onClick = onScan),
    ) {
        CoverArt(
            category?.tones ?: Tones.Lagoon, Modifier.matchParentSize(), seed = 11, icon = category?.icon ?: Icons.Rounded.Eco,
            iconSize = 60.dp, iconAlignment = BiasAlignment(0.2f, -0.55f), scrim = true,
        )
        FrostTag("AI powered", Modifier.align(Alignment.TopStart).padding(16.dp), icon = Icons.Rounded.AutoAwesome)
        FrostedCircleButton(
            Icons.Outlined.TaskAlt, "Earn at a bin: verify a deposit", onDeposit,
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp)) {
            Column(Modifier.padding(horizontal = 8.dp)) {
                Text(
                    if (category != null) "Scan your ${category.shortTitle.lowercase()}" else "Identify e-waste with one photo",
                    color = Color.White, style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "Right bin, safety steps and resale value",
                    color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(12.dp))
            SeeMorePill("Start scanning", onScan)
        }
    }
}

/** Items recycled in a category against the monthly goal. Selected cards turn charcoal. */
@Composable
private fun CategoryCard(c: EWasteCategory, count: Int, selected: Boolean, onClick: () -> Unit) {
    val pct = (count * 100 / MONTHLY_GOAL).coerceAtMost(999)
    val fg = if (selected) SW.OnInk else SW.Text
    val sub = if (selected) SW.OnInk.copy(alpha = 0.7f) else SW.Muted
    Column(
        Modifier.width(168.dp).clip(RoundedCornerShape(26.dp)).background(if (selected) SW.Ink else SW.Surface)
            .clickable(onClick = onClick).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(c.tint), contentAlignment = Alignment.Center) {
                Icon(c.icon, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(c.shortTitle, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(12.dp))
        Text("$count", color = fg, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text("items recycled", color = sub, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
        ProgressBar(
            count / MONTHLY_GOAL.toFloat(), if (selected) SW.OnInk else SW.Ink,
            track = if (selected) SW.OnInk.copy(alpha = 0.2f) else SW.Track,
        )
        Spacer(Modifier.height(6.dp))
        Row {
            Text("$pct%", color = fg, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("goal $MONTHLY_GOAL", color = sub, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LatestRow(d: DepositEntity) {
    val cat = EWasteCategory.fromKey(d.category)
    DayCard(
        overline = "${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(d.timestamp))} · ${d.placeName}",
        title = d.itemName,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
        trailing = { Text("+${d.points}", color = SW.Text, style = MaterialTheme.typography.titleMedium) },
        thumbnail = {
            CoverArt(
                cat?.tones ?: Tones.Forest, Modifier.matchParentSize(), seed = d.id.hashCode(),
                icon = cat?.icon ?: Icons.Rounded.Eco, iconSize = 24.dp,
            )
            d.photoPath?.let { path ->
                AsyncImage(File(path), null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            }
        },
    )
}

@Composable
private fun TrendCard(values: List<Float>, monthName: String) {
    val total = values.sum().toInt()
    val max = (values.maxOrNull() ?: 0f).toInt()
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(SW.Surface).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Recycling trend", color = SW.Text, style = MaterialTheme.typography.titleMedium)
                Text("$total pts earned in $monthName", color = SW.Muted, style = MaterialTheme.typography.bodySmall)
            }
            RatingChip("Max $max / day", icon = Icons.Rounded.Eco)
        }
        Spacer(Modifier.height(14.dp))
        val hasData = values.size >= 2 && values.any { it > 0f }
        val data = if (hasData) values else List(maxOf(values.size, 7)) { 0.2f + (it % 3) * 0.05f }
        TrendChart(data, Modifier.fillMaxWidth().height(90.dp), color = if (hasData) SW.Ink else SW.Line)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val days = values.size.coerceAtLeast(1)
            listOf(1, (days * 0.25).toInt(), (days * 0.5).toInt(), (days * 0.75).toInt()).distinct().filter { it >= 1 }.forEach {
                Text("${monthName.take(3)} $it", color = SW.Muted, style = MaterialTheme.typography.labelSmall)
            }
            Text("Now", color = SW.Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SafetyTipCard() {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { delay(6_000); index = (index + 1) % KnowledgeBase.safetyTips.size }
    }
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SW.Surface).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(SW.Ink), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Lightbulb, null, tint = SW.OnInk, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Safety tip", style = MaterialTheme.typography.bodySmall, color = SW.Muted)
            Text(KnowledgeBase.safetyTips[index], style = MaterialTheme.typography.bodyMedium, color = SW.Text)
        }
    }
}

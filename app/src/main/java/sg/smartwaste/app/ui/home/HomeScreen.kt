package sg.smartwaste.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import sg.smartwaste.app.data.KnowledgeBase
import sg.smartwaste.app.data.db.DepositEntity
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.PlaceWithDistance
import sg.smartwaste.app.ui.components.ArrowCircle
import sg.smartwaste.app.ui.components.Avatar
import sg.smartwaste.app.ui.components.CircleIconButton
import sg.smartwaste.app.ui.components.FillLevelBar
import sg.smartwaste.app.ui.components.HeroHeader
import sg.smartwaste.app.ui.components.IconBadge
import sg.smartwaste.app.ui.components.LOCATION_PERMISSIONS
import sg.smartwaste.app.ui.components.ProgressBar
import sg.smartwaste.app.ui.components.SearchPill
import sg.smartwaste.app.ui.components.SectionHeader
import sg.smartwaste.app.ui.components.StatsStrip
import sg.smartwaste.app.ui.components.Tag
import sg.smartwaste.app.ui.components.TintTag
import sg.smartwaste.app.ui.components.TrendChart
import sg.smartwaste.app.ui.components.appViewModel
import sg.smartwaste.app.ui.components.icon
import sg.smartwaste.app.ui.components.rememberPermissionState
import sg.smartwaste.app.ui.components.tint
import sg.smartwaste.app.ui.map.openUrl
import sg.smartwaste.app.ui.theme.SW
import sg.smartwaste.app.util.Geo
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Monthly item goal per category used for the category progress cards. */
private const val MONTHLY_GOAL = 5

@Composable
fun HomeScreen(
    onScan: () -> Unit,
    onOpenMap: (mode: String?, category: String?, placeId: String?) -> Unit,
    onRewards: () -> Unit,
    onProfile: () -> Unit,
    onDeposit: () -> Unit,
) {
    val vm = appViewModel { HomeViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val selectedCategory by vm.category.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = rememberPermissionState(*LOCATION_PERMISSIONS) { vm.refreshLocation() }
    var showUpdates by remember { mutableStateOf(false) }
    val fmt = NumberFormat.getIntegerInstance(Locale.US)
    val month = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())

    LaunchedEffect(Unit) {
        if (permission.granted) vm.refreshLocation() else permission.request()
    }

    LazyColumn(
        Modifier.fillMaxSize().background(SW.Sheet),
        contentPadding = PaddingValues(bottom = 130.dp),
    ) {
        // ------------------------------------------------------------- hero
        item {
            HeroHeader {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(s.name, onClick = onProfile)
                    Spacer(Modifier.weight(1f))
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(month, color = SW.Text, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = SW.Subtle, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    CircleIconButton(Icons.Rounded.Notifications, "Updates", { showUpdates = true }, container = Color.White.copy(alpha = 0.08f))
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Hi ${s.name.substringBefore(' ').ifBlank { "there" }} · Points balance",
                    color = SW.Subtle, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = SW.Text)) { append(fmt.format(s.balance)) }
                        withStyle(SpanStyle(color = SW.Text.copy(alpha = 0.45f), fontSize = 28.sp)) { append(" pts") }
                    },
                    fontSize = 46.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                Text(
                    buildString {
                        append(s.tier.name)
                        s.nextTier?.let { append(" · ${fmt.format(it.minLifetimePoints - s.lifetime)} pts to ${it.name}") }
                        append(" · ${s.redeemableCount} rewards ready")
                    },
                    color = SW.Muted, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                val diff = s.monthItems - s.lastMonthItems
                StatsStrip(
                    listOf(
                        Triple("Items this month", "${s.monthItems}", if (diff >= 0) "+$diff vs last month" else "$diff vs last month"),
                        Triple("Deposits", "${s.monthDeposits}", "${s.impact.verifiedDeposits} verified total"),
                        Triple("CO₂e avoided", String.format(Locale.US, "%.1f kg", s.monthCo2), "this month"),
                    ),
                    Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        // ------------------------------------------------------------- sheet
        item {
            SearchPill(
                "Search bins, buyers, items…",
                onClick = { onOpenMap(null, null, null) },
                onFilter = { onOpenMap("bins", selectedCategory?.name, null) },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
        item { TrendCard(s.trend, month.substringBefore(' ')) }
        item {
            Spacer(Modifier.height(16.dp))
            ScanHero(selectedCategory, onScan, onDeposit)
        }
        item {
            Spacer(Modifier.height(24.dp))
            SectionHeader("Categories", action = "View All", onAction = { onOpenMap("bins", selectedCategory?.name, null) })
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(EWasteCategory.entries) { c ->
                    CategoryCard(c, s.impact.byCategory[c] ?: 0, selected = selectedCategory == c) { vm.selectCategory(c) }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            SectionHeader(
                if (s.locationKnown) "Nearest e-waste bins" else "E-waste bins in Singapore",
                action = "View All", onAction = { onOpenMap("bins", selectedCategory?.name, null) },
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(s.nearbyBins, key = { it.place.id }) { item -> BinCard(item) { onOpenMap("bins", null, item.place.id) } }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            SectionHeader("Sell your e-waste", action = "View All", onAction = { onOpenMap("buyers", selectedCategory?.name, null) })
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(s.nearbyBuyers, key = { it.place.id }) { item -> BuyerCard(item) { onOpenMap("buyers", null, item.place.id) } }
                items(s.onlineBuyers, key = { "online-" + it.name }) { b ->
                    OnlineBuyerCard(b.name, b.buys, b.pays) { openUrl(context, b.website) }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            SectionHeader("Latest", action = if (s.latest.isNotEmpty()) "View All" else null, onAction = onProfile)
            Spacer(Modifier.height(10.dp))
            if (s.latest.isEmpty()) {
                Text(
                    "No deposits yet. Scan an item and drop it in a bin to earn your first points.",
                    color = SW.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        items(s.latest, key = { "latest-${it.id}" }) { d -> LatestRow(d) }
        item {
            Spacer(Modifier.height(16.dp))
            SafetyTipCard()
        }
    }

    if (showUpdates) {
        AlertDialog(
            onDismissRequest = { showUpdates = false },
            containerColor = SW.Card,
            shape = RoundedCornerShape(26.dp),
            title = { Text("Updates", color = SW.Text) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• ${s.redeemableCount} reward(s) you can redeem now", color = SW.Subtle)
                    Text("• ${s.nearlyFullBins} SmartWaste bin(s) are over 75% full. The map skips full bins for you.", color = SW.Subtle)
                    Text("• Tip: ${KnowledgeBase.safetyTips.first()}", color = SW.Subtle)
                }
            },
            confirmButton = { TextButton(onClick = { showUpdates = false; onRewards() }) { Text("Open rewards", color = SW.Lime) } },
            dismissButton = { TextButton(onClick = { showUpdates = false }) { Text("Close", color = SW.Muted) } },
        )
    }
}

@Composable
private fun TrendCard(values: List<Float>, monthName: String) {
    val total = values.sum().toInt()
    val max = (values.maxOrNull() ?: 0f).toInt()
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SW.Card)
            .border(1.dp, SW.Line, RoundedCornerShape(24.dp)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recycling trend · $monthName", color = SW.Text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text("0 — $max pts daily", color = SW.Lime, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(4.dp))
        Text("$total pts earned this month", color = SW.Muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(12.dp))
        val data = if (values.size >= 2 && values.any { it > 0f }) values else List(maxOf(values.size, 7)) { 0.2f + (it % 3) * 0.05f }
        TrendChart(data, Modifier.fillMaxWidth().height(90.dp), color = if (values.any { it > 0f }) SW.Lime else SW.Line)
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

/** Gradient call-to-action card for the AI scanner. */
@Composable
private fun ScanHero(category: EWasteCategory?, onScan: () -> Unit, onDeposit: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(SW.Olive, SW.OliveDark)))
            .border(1.dp, SW.Lime.copy(alpha = 0.25f), RoundedCornerShape(28.dp))
            .clickable(onClick = onScan)
            .padding(20.dp),
    ) {
        Icon(
            category?.icon ?: Icons.Rounded.Eco, null, tint = SW.Lime.copy(alpha = 0.12f),
            modifier = Modifier.align(Alignment.TopEnd).size(120.dp),
        )
        Column {
            TintTag("AI powered", SW.Lime)
            Spacer(Modifier.height(10.dp))
            Text(
                if (category != null) "Scan your ${category.shortTitle.lowercase()}" else "Identify e-waste\nwith one photo",
                color = SW.Text, style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text("Right bin, safety steps & resale value instantly", color = SW.Subtle, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onScan).padding(start = 18.dp, end = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Start scanning", color = SW.Text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    ArrowCircle(size = 42.dp)
                }
                Spacer(Modifier.width(10.dp))
                CircleIconButton(Icons.Rounded.VerifiedUser, "Earn at a bin", onDeposit, container = SW.Card2, content = SW.Lime, size = 52.dp)
            }
        }
    }
}

/** "Food €682 · 88% of €780" style card, here: items recycled vs monthly goal. */
@Composable
private fun CategoryCard(c: EWasteCategory, count: Int, selected: Boolean, onClick: () -> Unit) {
    val pct = (count * 100 / MONTHLY_GOAL).coerceAtMost(999)
    Column(
        Modifier.width(168.dp).clip(RoundedCornerShape(22.dp))
            .background(if (selected) c.tint.copy(alpha = 0.12f) else SW.Card)
            .border(1.dp, if (selected) c.tint else SW.Line, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(c.icon, null, tint = c.tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(c.shortTitle, color = SW.Subtle, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
        Spacer(Modifier.height(10.dp))
        Text("$count", color = SW.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("items recycled", color = SW.Muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(10.dp))
        ProgressBar(count / MONTHLY_GOAL.toFloat(), c.tint)
        Spacer(Modifier.height(6.dp))
        Row {
            Text("$pct%", color = c.tint, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("goal $MONTHLY_GOAL", color = SW.Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun BinCard(item: PlaceWithDistance, onClick: () -> Unit) {
    val p = item.place
    val accent = if (p.isSmartBin) SW.Mint else SW.Lime
    Column(
        Modifier.width(230.dp).clip(RoundedCornerShape(24.dp)).background(SW.Card).border(1.dp, SW.Line, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier.fillMaxWidth().height(104.dp).padding(8.dp).clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.32f), SW.OliveDark))),
        ) {
            Icon(
                (p.accepts.firstOrNull() ?: EWasteCategory.DEVICE_WITH_BATTERY).icon, null,
                tint = accent.copy(alpha = 0.45f), modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).size(64.dp),
            )
            Tag(
                Geo.formatDistance(item.distanceMeters), SW.Bg.copy(alpha = 0.7f), SW.Text, icon = Icons.Rounded.NearMe,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            )
            if (p.isSmartBin) Tag("Smart bin", accent, SW.OnLime, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
        }
        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 4.dp)) {
            Text(p.name, style = MaterialTheme.typography.titleSmall, color = SW.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = SW.Muted, modifier = Modifier.size(12.dp))
                Text(p.address, style = MaterialTheme.typography.labelSmall, color = SW.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FillLevelBar(p.fillLevel, Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                ArrowCircle(size = 34.dp)
            }
        }
    }
}

@Composable
private fun BuyerCard(item: PlaceWithDistance, onClick: () -> Unit) {
    val p = item.place
    Column(
        Modifier.width(210.dp).clip(RoundedCornerShape(24.dp)).background(SW.Card).border(1.dp, SW.Line, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Store, SW.Amber, size = 40.dp)
            Spacer(Modifier.weight(1f))
            Tag(Geo.formatDistance(item.distanceMeters), SW.Card2, SW.Text)
        }
        Spacer(Modifier.height(10.dp))
        Text(p.name, style = MaterialTheme.typography.titleSmall, color = SW.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(p.buyer?.buys.orEmpty(), style = MaterialTheme.typography.labelSmall, color = SW.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text(p.buyer?.pays ?: "Get a quote", style = MaterialTheme.typography.labelMedium, color = SW.Amber, maxLines = 1)
    }
}

@Composable
private fun OnlineBuyerCard(name: String, buys: String, pays: String, onClick: () -> Unit) {
    Column(
        Modifier.width(210.dp).clip(RoundedCornerShape(24.dp)).background(SW.OliveDark).border(1.dp, SW.Lime.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Language, SW.Lime, size = 40.dp)
            Spacer(Modifier.weight(1f))
            Tag("Online", SW.Lime, SW.OnLime)
        }
        Spacer(Modifier.height(10.dp))
        Text(name, style = MaterialTheme.typography.titleSmall, color = SW.Text, maxLines = 1)
        Text(buys, style = MaterialTheme.typography.labelSmall, color = SW.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text(pays, style = MaterialTheme.typography.labelMedium, color = SW.Lime, maxLines = 1)
    }
}

@Composable
private fun LatestRow(d: DepositEntity) {
    val cat = EWasteCategory.fromKey(d.category)
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SW.Card)
            .border(1.dp, SW.Line, RoundedCornerShape(20.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(cat?.icon ?: Icons.Rounded.Eco, cat?.tint ?: SW.Lime, size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(d.itemName, color = SW.Text, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${d.placeName} · ${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(d.timestamp))}",
                color = SW.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Text("+${d.points}", color = SW.Lime, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SafetyTipCard() {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { delay(6_000); index = (index + 1) % KnowledgeBase.safetyTips.size }
    }
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(SW.LimeSoft).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(SW.Lime), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Lightbulb, null, tint = SW.OnLime, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Safety tip", style = MaterialTheme.typography.labelMedium, color = SW.Lime)
            Text(KnowledgeBase.safetyTips[index], style = MaterialTheme.typography.bodyMedium, color = SW.Text)
        }
    }
}

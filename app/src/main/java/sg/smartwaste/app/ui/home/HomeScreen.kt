package sg.smartwaste.app.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import sg.smartwaste.app.R
import sg.smartwaste.app.data.KnowledgeBase
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.PlaceWithDistance
import sg.smartwaste.app.ui.components.ArrowCircle
import sg.smartwaste.app.ui.components.Avatar
import sg.smartwaste.app.ui.components.FillLevelBar
import sg.smartwaste.app.ui.components.IconBadge
import sg.smartwaste.app.ui.components.LOCATION_PERMISSIONS
import sg.smartwaste.app.ui.components.PillChip
import sg.smartwaste.app.ui.components.SearchPill
import sg.smartwaste.app.ui.components.SectionHeader
import sg.smartwaste.app.ui.components.StatTile
import sg.smartwaste.app.ui.components.Tag
import sg.smartwaste.app.ui.components.appViewModel
import sg.smartwaste.app.ui.components.icon
import sg.smartwaste.app.ui.components.rememberPermissionState
import sg.smartwaste.app.ui.components.tint
import sg.smartwaste.app.ui.map.openUrl
import sg.smartwaste.app.ui.theme.SW
import sg.smartwaste.app.util.Geo
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val permission = rememberPermissionState(*LOCATION_PERMISSIONS) { vm.refreshLocation() }

    LaunchedEffect(Unit) {
        if (permission.granted) vm.refreshLocation() else permission.request()
    }

    LazyColumn(
        Modifier.fillMaxSize().background(SW.Bg),
        contentPadding = PaddingValues(bottom = 150.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${greeting()}, ${s.name.ifBlank { "Recycler" }}",
                        style = MaterialTheme.typography.headlineSmall, color = SW.Ink,
                    )
                    Text("Welcome to SmartWaste", style = MaterialTheme.typography.bodyMedium, color = SW.Muted)
                }
                Avatar(s.name, onClick = onProfile)
            }
        }
        item {
            SearchPill(
                "Search bins, buyers, items…",
                onClick = { onOpenMap(null, null, null) },
                onFilter = { onOpenMap("bins", selectedCategory?.name, null) },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(18.dp))
        }
        item { BalanceCard(s.balance, s.tier.name, s.redeemableCount, onRewards, onDeposit) }
        item {
            Spacer(Modifier.height(22.dp))
            SectionHeader("What are you recycling?")
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { PillChip("All", selected = selectedCategory == null, onClick = { vm.selectCategory(null) }) }
                items(EWasteCategory.entries) { c ->
                    PillChip(c.shortTitle, selected = selectedCategory == c, onClick = { vm.selectCategory(c) }, leading = c.icon)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        item { ScanHero(selectedCategory, onScan) }
        item {
            Spacer(Modifier.height(24.dp))
            SectionHeader(
                if (s.locationKnown) "Nearest e-waste bins" else "E-waste bins in Singapore",
                action = "See all", onAction = { onOpenMap("bins", selectedCategory?.name, null) },
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(s.nearbyBins, key = { it.place.id }) { item ->
                    BinCard(item) { onOpenMap("bins", null, item.place.id) }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            SectionHeader("Sell your e-waste", action = "See all", onAction = { onOpenMap("buyers", selectedCategory?.name, null) })
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(s.nearbyBuyers, key = { it.place.id }) { item ->
                    BuyerCard(item) { onOpenMap("buyers", null, item.place.id) }
                }
                items(s.onlineBuyers, key = { "online-" + it.name }) { b ->
                    OnlineBuyerCard(b.name, b.buys, b.pays) { openUrl(context, b.website) }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            SectionHeader("Your impact")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.Rounded.Devices, "${s.impact.items}", "Items recycled", Modifier.weight(1f), tint = SW.Electric)
                StatTile(Icons.Rounded.Eco, String.format(Locale.US, "%.1f kg", s.impact.kg), "E-waste diverted", Modifier.weight(1f))
                StatTile(Icons.Rounded.Cloud, String.format(Locale.US, "%.1f kg", s.impact.co2Kg), "CO₂e avoided", Modifier.weight(1f), tint = SW.Amber)
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            SafetyTipCard()
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun BalanceCard(balance: Int, tier: String, redeemable: Int, onRewards: () -> Unit, onDeposit: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(SW.Ink)
            .padding(20.dp),
    ) {
        Image(
            painterResource(R.drawable.ic_recycle), null,
            colorFilter = ColorFilter.tint(SW.Emerald.copy(alpha = 0.25f)),
            modifier = Modifier.align(Alignment.CenterEnd).size(130.dp).offset(x = 30.dp).rotate(-12f),
        )
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your points", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                Tag(tier, SW.Lime, SW.Ink)
            }
            Spacer(Modifier.height(4.dp))
            Text(NumberFormat.getIntegerInstance(Locale.US).format(balance), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Text(
                if (redeemable > 0) "$redeemable reward${if (redeemable > 1) "s" else ""} ready to redeem"
                else "Recycle & verify items to earn rewards",
                color = SW.Lime, style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White).clickable(onClick = onRewards).padding(horizontal = 18.dp, vertical = 11.dp),
                ) { Text("Redeem", color = SW.Ink, style = MaterialTheme.typography.labelLarge) }
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f)).clickable(onClick = onDeposit)
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.VerifiedUser, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Earn at a bin", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Large gradient hero card with a frosted "See more"-style action bar (reference design 1). */
@Composable
private fun ScanHero(category: EWasteCategory?, onScan: () -> Unit) {
    val accent = category?.tint ?: SW.Emerald
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(250.dp)
            .shadow(18.dp, RoundedCornerShape(32.dp), spotColor = accent)
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(accent, SW.Ink)))
            .clickable(onClick = onScan),
    ) {
        Icon(
            category?.icon ?: Icons.Rounded.Eco, null, tint = Color.White.copy(alpha = 0.16f),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp).size(170.dp),
        )
        Column(Modifier.align(Alignment.TopStart).padding(22.dp)) {
            Tag("AI powered", Color.White.copy(alpha = 0.18f), Color.White)
            Spacer(Modifier.height(12.dp))
            Text(
                if (category != null) "Scan your ${category.shortTitle.lowercase()}" else "Identify e-waste\nwith one photo",
                color = Color.White, style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Get the right bin, safety steps & resale value instantly",
                color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp)
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .padding(start = 22.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Start scanning", color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            ArrowCircle(size = 48.dp, container = Color.White, content = SW.Ink)
        }
    }
}

@Composable
private fun BinCard(item: PlaceWithDistance, onClick: () -> Unit) {
    val p = item.place
    val accent = if (p.isSmartBin) SW.Electric else SW.Emerald
    Column(
        Modifier.width(230.dp).clip(RoundedCornerShape(28.dp)).background(Color.White).clickable(onClick = onClick),
    ) {
        Box(
            Modifier.fillMaxWidth().height(110.dp).padding(8.dp).clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.45f)))),
        ) {
            Icon(
                (p.accepts.firstOrNull() ?: EWasteCategory.DEVICE_WITH_BATTERY).icon, null,
                tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).size(70.dp),
            )
            Tag(
                Geo.formatDistance(item.distanceMeters), Color.White, SW.Ink, icon = Icons.Rounded.NearMe,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            )
            if (p.isSmartBin) {
                Tag("Smart bin", SW.Lime, SW.Ink, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
            }
        }
        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 4.dp)) {
            Text(p.name, style = MaterialTheme.typography.titleSmall, color = SW.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = SW.Muted, modifier = Modifier.size(12.dp))
                Text(p.address, style = MaterialTheme.typography.labelSmall, color = SW.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FillLevelBar(p.fillLevel, Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                ArrowCircle(size = 36.dp)
            }
        }
    }
}

@Composable
private fun BuyerCard(item: PlaceWithDistance, onClick: () -> Unit) {
    val p = item.place
    Column(
        Modifier.width(210.dp).clip(RoundedCornerShape(26.dp)).background(Color.White).clickable(onClick = onClick).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Store, SW.Amber, size = 40.dp)
            Spacer(Modifier.weight(1f))
            Tag(Geo.formatDistance(item.distanceMeters), SW.Field, SW.Ink)
        }
        Spacer(Modifier.height(10.dp))
        Text(p.name, style = MaterialTheme.typography.titleSmall, color = SW.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(p.buyer?.buys.orEmpty(), style = MaterialTheme.typography.labelSmall, color = SW.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text(p.buyer?.pays ?: "Get a quote", style = MaterialTheme.typography.labelMedium, color = SW.Amber, maxLines = 1)
    }
}

@Composable
private fun OnlineBuyerCard(name: String, buys: String, pays: String, onClick: () -> Unit) {
    Column(
        Modifier.width(210.dp).clip(RoundedCornerShape(26.dp)).background(SW.Ink).clickable(onClick = onClick).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Language, SW.Lime, size = 40.dp, container = Color.White.copy(alpha = 0.1f))
            Spacer(Modifier.weight(1f))
            Tag("Online", SW.Lime, SW.Ink)
        }
        Spacer(Modifier.height(10.dp))
        Text(name, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)
        Text(buys, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Text(pays, style = MaterialTheme.typography.labelMedium, color = SW.Lime, maxLines = 1)
    }
}

@Composable
private fun SafetyTipCard() {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { delay(6_000); index = (index + 1) % KnowledgeBase.safetyTips.size }
    }
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SW.Mint).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(SW.Emerald), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Lightbulb, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Safety tip", style = MaterialTheme.typography.labelMedium, color = SW.EmeraldDeep)
            Text(KnowledgeBase.safetyTips[index], style = MaterialTheme.typography.bodyMedium, color = SW.Ink)
        }
    }
}

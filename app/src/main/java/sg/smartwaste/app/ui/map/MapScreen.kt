package sg.smartwaste.app.ui.map

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.OnlineBuyer
import sg.smartwaste.app.data.model.Place
import sg.smartwaste.app.data.model.PlaceType
import sg.smartwaste.app.data.model.PlaceWithDistance
import sg.smartwaste.app.data.routing.Route
import sg.smartwaste.app.data.routing.RouteStep
import sg.smartwaste.app.data.routing.TravelMode
import sg.smartwaste.app.ui.components.ArrowCircle
import sg.smartwaste.app.ui.components.CircleIconButton
import sg.smartwaste.app.ui.components.FillLevelBar
import sg.smartwaste.app.ui.components.IconBadge
import sg.smartwaste.app.ui.components.LOCATION_PERMISSIONS
import sg.smartwaste.app.ui.components.PillChip
import sg.smartwaste.app.ui.components.PrimaryButton
import sg.smartwaste.app.ui.components.SecondaryButton
import sg.smartwaste.app.ui.components.Tag
import sg.smartwaste.app.ui.components.appViewModel
import sg.smartwaste.app.ui.components.icon
import sg.smartwaste.app.ui.components.rememberPermissionState
import sg.smartwaste.app.ui.components.tint
import sg.smartwaste.app.ui.theme.SW
import sg.smartwaste.app.util.Geo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MapScreen(
    mode: String?,
    category: String?,
    auto: Boolean,
    placeId: String?,
    query: String?,
    onVerifyDeposit: (placeId: String) -> Unit,
) {
    val vm = appViewModel { MapViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val camera by vm.camera.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val density = LocalDensity.current

    val permission = rememberPermissionState(*LOCATION_PERMISSIONS) { granted -> if (granted) vm.startTracking() }
    LaunchedEffect(Unit) {
        vm.applyArgs(mode, category, auto, placeId, query)
        if (!permission.granted) permission.request()
    }
    DisposableEffect(permission.granted) {
        if (permission.granted) vm.startTracking()
        onDispose { vm.stopTracking() }
    }
    LaunchedEffect(s.message) {
        if (s.message != null) { delay(3500); vm.consumeMessage() }
    }

    val bottomPadPx = with(density) { 330.dp.roundToPx() }

    Box(Modifier.fillMaxSize().background(SW.Bg)) {
        SmartMap(
            places = s.visible.map { it.place },
            selectedId = s.selected?.place?.id,
            user = s.user?.point,
            route = s.route?.points,
            dark = s.darkMap,
            camera = camera,
            bottomPaddingPx = bottomPadPx,
            onPlaceClick = vm::select,
            onMapClick = { if (s.route == null) vm.clearSelection() },
            onUserGesture = vm::onUserGesture,
            modifier = Modifier.fillMaxSize(),
        )

        // ---------------------------------------------------------- top area
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp),
        ) {
            AnimatedContent(targetState = s.nav.active, label = "top") { navigating ->
                if (navigating) {
                    NavBanner(s.nav.nextStep, s.nav.distanceToNextM, s.nav.rerouting, s.nav.arrived)
                } else {
                    Column {
                        MapSearchBar(s.query, vm::setQuery, s.darkMap, vm::toggleDark)
                        Spacer(Modifier.height(10.dp))
                        FilterRow(
                            filter = s.filter,
                            category = s.category,
                            onFilter = vm::setFilter,
                            onClearCategory = { vm.setCategory(null) },
                            onNearest = { vm.selectNearest(routeToo = true) },
                        )
                    }
                }
            }
            AnimatedVisibility(visible = s.message != null, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    Text(
                        s.message.orEmpty(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(SW.Ink.copy(alpha = 0.92f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }

        // -------------------------------------------------- floating controls
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircleIconButton(
                Icons.Rounded.MyLocation, "My location", vm::recenter,
                modifier = Modifier.shadow(8.dp, CircleShape),
                container = if (s.following) SW.Electric else Color.White,
                content = if (s.following) Color.White else SW.Ink,
                size = 48.dp,
            )
            CircleIconButton(
                Icons.Rounded.Refresh, "Refresh places", vm::refresh,
                modifier = Modifier.shadow(8.dp, CircleShape), size = 48.dp,
            )
            if (s.loading) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = SW.Emerald)
                }
            }
        }

        // -------------------------------------------------------- bottom area
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 94.dp),
        ) {
            val panel = when {
                s.nav.active -> "nav"
                s.route != null || s.routeLoading -> "route"
                s.selected != null -> "detail"
                else -> "list"
            }
            AnimatedContent(
                targetState = panel,
                transitionSpec = {
                    (slideInVertically { it / 3 } + fadeIn()) togetherWith (slideOutVertically { it / 3 } + fadeOut())
                },
                label = "panel",
            ) { which ->
                when (which) {
                    "list" -> PlaceCarousel(
                        places = s.visible.take(25),
                        showOnline = s.filter == MapFilter.BUYERS,
                        online = s.onlineBuyers,
                        onClick = { vm.select(it.place.id) },
                        onOpenWeb = { openUrl(context, it) },
                    )
                    "detail" -> s.selected?.let { sel ->
                        PlaceDetailCard(
                            item = sel,
                            onDirections = { vm.requestRoute() },
                            onClose = vm::clearSelection,
                            onCall = { sel.place.phone?.let { openUrl(context, "tel:$it") } },
                            onWeb = { sel.place.website?.let { openUrl(context, it) } },
                            onVerify = { onVerifyDeposit(sel.place.id) },
                        )
                    }
                    "route" -> RouteCard(
                        place = s.selected?.place,
                        route = s.route,
                        loading = s.routeLoading,
                        mode = s.mode,
                        onMode = vm::setMode,
                        onStart = vm::startNavigation,
                        onExternal = { s.selected?.place?.let { openExternalNavigation(context, it, s.mode) } },
                        onClose = vm::clearSelection,
                    )
                    else -> NavBottomCard(
                        place = s.selected?.place,
                        progress = s.nav,
                        onEnd = vm::stopNavigation,
                        onExternal = { s.selected?.place?.let { openExternalNavigation(context, it, s.mode) } },
                        onVerify = { s.selected?.place?.let { onVerifyDeposit(it.id) } },
                    )
                }
            }
        }
    }
}

// ============================================================== top widgets

@Composable
private fun MapSearchBar(query: String, onQuery: (String) -> Unit, dark: Boolean, onToggleDark: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .height(54.dp)
                .shadow(10.dp, RoundedCornerShape(27.dp))
                .clip(RoundedCornerShape(27.dp))
                .background(Color.White)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = SW.Ink, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Search bins, buyers, malls…", color = SW.Muted, style = MaterialTheme.typography.bodyLarge)
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = SW.Ink),
                    cursorBrush = SolidColor(SW.Emerald),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(Icons.Rounded.Close, "Clear", tint = SW.Muted, modifier = Modifier.size(20.dp).clickable { onQuery("") })
            }
        }
        Spacer(Modifier.width(10.dp))
        CircleIconButton(
            Icons.Rounded.Layers, if (dark) "Light map" else "Dark map", onToggleDark,
            modifier = Modifier.shadow(10.dp, CircleShape),
            container = if (dark) Color.White else SW.Ink, content = if (dark) SW.Ink else Color.White, size = 54.dp,
        )
    }
}

@Composable
private fun FilterRow(
    filter: MapFilter,
    category: EWasteCategory?,
    onFilter: (MapFilter) -> Unit,
    onClearCategory: () -> Unit,
    onNearest: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MapFilter.entries.forEach { f ->
            PillChip(f.label, selected = filter == f, onClick = { onFilter(f) }, modifier = Modifier.shadow(4.dp, RoundedCornerShape(50)))
        }
        PillChip(
            "Nearest", selected = false, onClick = onNearest, leading = Icons.Rounded.NearMe,
            modifier = Modifier.shadow(4.dp, RoundedCornerShape(50)),
        )
        if (category != null) {
            PillChip(
                "${category.shortTitle}  ✕", selected = true, onClick = onClearCategory, leading = category.icon,
                selectedColor = SW.Emerald, modifier = Modifier.shadow(4.dp, RoundedCornerShape(50)),
            )
        }
    }
}

@Composable
private fun NavBanner(step: RouteStep?, distanceM: Double, rerouting: Boolean, arrived: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(16.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(SW.Navy)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(SW.Electric), contentAlignment = Alignment.Center) {
            if (arrived || step?.maneuverType == "arrive") {
                Icon(Icons.Rounded.Flag, null, tint = Color.White, modifier = Modifier.size(28.dp))
            } else {
                Icon(
                    Icons.Rounded.ArrowUpward, null, tint = Color.White,
                    modifier = Modifier.size(30.dp).rotate(maneuverAngle(step?.maneuverType, step?.modifier)),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    arrived -> "Arrived"
                    rerouting -> "Re-routing…"
                    else -> Geo.formatDistance(distanceM)
                },
                color = SW.Lime, style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (arrived) "You've reached your destination" else step?.instruction ?: "Follow the route",
                color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun maneuverAngle(type: String?, modifier: String?): Float = when {
    type == "roundabout" || type == "rotary" -> 45f
    modifier == "uturn" -> 180f
    modifier == "sharp right" -> 135f
    modifier == "right" -> 90f
    modifier == "slight right" -> 45f
    modifier == "slight left" -> -45f
    modifier == "left" -> -90f
    modifier == "sharp left" -> -135f
    else -> 0f
}

// ============================================================ bottom panels

@Composable
private fun PlaceCarousel(
    places: List<PlaceWithDistance>,
    showOnline: Boolean,
    online: List<OnlineBuyer>,
    onClick: (PlaceWithDistance) -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showOnline) {
            items(online, key = { "online-${it.name}" }) { buyer ->
                Column(
                    Modifier
                        .width(250.dp)
                        .shadow(12.dp, RoundedCornerShape(26.dp))
                        .clip(RoundedCornerShape(26.dp))
                        .background(SW.Ink)
                        .clickable { onOpenWeb(buyer.website) }
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Language, SW.Lime, size = 38.dp, container = Color.White.copy(alpha = 0.1f))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(buyer.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text("Online · ${buyer.pays}", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(buyer.description, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    Spacer(Modifier.height(8.dp))
                    Text("Buys: ${buyer.buys}", color = SW.Lime, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
        items(places, key = { it.place.id }) { item ->
            PlaceMiniCard(item, onClick = { onClick(item) })
        }
        if (places.isEmpty() && !showOnline) {
            item {
                Box(
                    Modifier.width(300.dp).clip(RoundedCornerShape(26.dp)).background(Color.White).padding(20.dp),
                ) { Text("No places match your filters.", color = SW.Subtle) }
            }
        }
    }
}

@Composable
private fun PlaceMiniCard(item: PlaceWithDistance, onClick: () -> Unit) {
    val p = item.place
    val accent = placeAccent(p)
    Column(
        Modifier
            .width(250.dp)
            .shadow(12.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(placeIcon(p), accent, size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleSmall, color = SW.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(p.operator, style = MaterialTheme.typography.labelSmall, color = SW.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (p.type == PlaceType.BIN) {
            FillLevelBar(p.fillLevel)
        } else {
            Text(p.buyer?.pays ?: "Get a quote", style = MaterialTheme.typography.labelSmall, color = SW.Amber, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(p.buyer?.buys.orEmpty(), style = MaterialTheme.typography.labelSmall, color = SW.Subtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Tag(Geo.formatDistance(item.distanceMeters), SW.Field, SW.Ink, icon = Icons.Rounded.NearMe)
            Spacer(Modifier.width(6.dp))
            Tag(p.source.label, accent.copy(alpha = 0.12f), accent)
            Spacer(Modifier.weight(1f))
            ArrowCircle(size = 34.dp)
        }
    }
}

@Composable
private fun PlaceDetailCard(
    item: PlaceWithDistance,
    onDirections: () -> Unit,
    onClose: () -> Unit,
    onCall: () -> Unit,
    onWeb: () -> Unit,
    onVerify: () -> Unit,
) {
    val p = item.place
    val accent = placeAccent(p)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .shadow(18.dp, RoundedCornerShape(30.dp))
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(placeIcon(p), accent, size = 50.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleLarge, color = SW.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(p.address, style = MaterialTheme.typography.bodySmall, color = SW.Subtle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            CircleIconButton(Icons.Rounded.Close, "Close", onClose, container = SW.Field, size = 36.dp, iconSize = 18.dp)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Tag(Geo.formatDistance(item.distanceMeters), SW.Field, SW.Ink, icon = Icons.Rounded.NearMe)
            p.openingHours?.let { Tag(it, SW.Field, SW.Ink, icon = Icons.Rounded.Schedule) }
            Tag(p.operator, accent.copy(alpha = 0.12f), accent)
        }
        Spacer(Modifier.height(14.dp))
        if (p.type == PlaceType.BIN) {
            FillLevelBar(p.fillLevel)
            if (p.isSmartBin) {
                Spacer(Modifier.height(4.dp))
                Text("Live capacity from SmartWaste sensors (demo telemetry)", style = MaterialTheme.typography.labelSmall, color = SW.Muted)
            }
        } else p.buyer?.let { b ->
            Text("Pays: ${b.pays}", style = MaterialTheme.typography.titleSmall, color = SW.Ink)
            Text("Buys: ${b.buys}", style = MaterialTheme.typography.bodySmall, color = SW.Subtle)
            b.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = SW.Subtle) }
            if (b.pickup) {
                Spacer(Modifier.height(6.dp))
                Tag("Pickup available", SW.Mint, SW.EmeraldDeep)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Accepts", style = MaterialTheme.typography.labelMedium, color = SW.Muted)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            p.accepts.forEach { c -> Tag(c.shortTitle, c.tint.copy(alpha = 0.12f), c.tint, icon = c.icon) }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton("Directions", onDirections, Modifier.weight(1f), leading = Icons.Rounded.Directions)
            if (p.phone != null) {
                Spacer(Modifier.width(10.dp))
                CircleIconButton(Icons.Rounded.Call, "Call", onCall, container = SW.Field, size = 58.dp)
            }
            if (p.website != null) {
                Spacer(Modifier.width(10.dp))
                CircleIconButton(Icons.Rounded.Language, "Website", onWeb, container = SW.Field, size = 58.dp)
            }
        }
        if (p.type == PlaceType.BIN) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton("I'm here: verify & earn points", onVerify, Modifier.fillMaxWidth(), leading = Icons.Rounded.VerifiedUser)
        }
    }
}

@Composable
private fun RouteCard(
    place: Place?,
    route: Route?,
    loading: Boolean,
    mode: TravelMode,
    onMode: (TravelMode) -> Unit,
    onStart: () -> Unit,
    onExternal: () -> Unit,
    onClose: () -> Unit,
) {
    var showSteps by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .shadow(18.dp, RoundedCornerShape(30.dp))
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (loading || route == null) {
                    Text("Finding the best route…", style = MaterialTheme.typography.titleLarge, color = SW.Ink)
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(Geo.formatDuration(route.durationSeconds), style = MaterialTheme.typography.headlineMedium, color = SW.EmeraldDeep)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "· ${Geo.formatDistance(route.distanceMeters)}",
                            style = MaterialTheme.typography.titleMedium, color = SW.Subtle,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                }
                Text("to ${place?.name ?: "destination"}", style = MaterialTheme.typography.bodySmall, color = SW.Subtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            CircleIconButton(Icons.Rounded.Close, "Close", onClose, container = SW.Field, size = 36.dp, iconSize = 18.dp)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TravelMode.entries.forEach { m ->
                PillChip(m.label, selected = m == mode, onClick = { onMode(m) }, leading = modeIcon(m), selectedColor = SW.Electric)
            }
        }
        if (route != null && !loading) {
            Spacer(Modifier.height(12.dp))
            Text(
                if (showSteps) "Hide steps" else "${route.steps.size} steps · tap to preview",
                style = MaterialTheme.typography.labelMedium, color = SW.Electric,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showSteps = !showSteps }.padding(vertical = 4.dp),
            )
            AnimatedVisibility(showSteps) {
                LazyColumn(Modifier.heightIn(max = 200.dp).padding(top = 6.dp)) {
                    itemsIndexed(route.steps) { i, step ->
                        StepRow(step)
                        if (i < route.steps.lastIndex) HorizontalDivider(color = SW.Line)
                    }
                }
            }
            if (route.isEstimate) {
                Text("Approximate straight-line estimate (routing offline)", style = MaterialTheme.typography.labelSmall, color = SW.Amber)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row {
            PrimaryButton("Start", onStart, Modifier.weight(1f), enabled = route != null && !loading, leading = Icons.Rounded.Navigation, container = SW.Electric)
            Spacer(Modifier.width(10.dp))
            SecondaryButton("Google Maps", onExternal, Modifier.weight(1f), leading = Icons.Rounded.Map)
        }
    }
}

@Composable
private fun StepRow(step: RouteStep) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(SW.Sky), contentAlignment = Alignment.Center) {
            if (step.maneuverType == "arrive") Icon(Icons.Rounded.Flag, null, tint = SW.Electric, modifier = Modifier.size(16.dp))
            else Icon(Icons.Rounded.ArrowUpward, null, tint = SW.Electric, modifier = Modifier.size(16.dp).rotate(maneuverAngle(step.maneuverType, step.modifier)))
        }
        Spacer(Modifier.width(10.dp))
        Text(step.instruction, style = MaterialTheme.typography.bodyMedium, color = SW.Ink, modifier = Modifier.weight(1f))
        if (step.distanceMeters > 0) Text(Geo.formatDistance(step.distanceMeters), style = MaterialTheme.typography.labelSmall, color = SW.Muted)
    }
}

@Composable
private fun NavBottomCard(
    place: Place?,
    progress: NavProgress,
    onEnd: () -> Unit,
    onExternal: () -> Unit,
    onVerify: () -> Unit,
) {
    val eta = remember(progress.remainingS) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(System.currentTimeMillis() + (progress.remainingS * 1000).toLong()))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .shadow(18.dp, RoundedCornerShape(30.dp))
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White)
            .padding(20.dp),
    ) {
        if (progress.arrived) {
            Text("You've arrived 🎉", style = MaterialTheme.typography.headlineSmall, color = SW.Ink)
            Text(place?.name.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = SW.Subtle)
            Spacer(Modifier.height(14.dp))
            if (place?.type == PlaceType.BIN) {
                PrimaryButton("Verify deposit & earn points", onVerify, Modifier.fillMaxWidth(), container = SW.Emerald, leading = Icons.Rounded.VerifiedUser)
                Spacer(Modifier.height(10.dp))
            }
            SecondaryButton("End navigation", onEnd, Modifier.fillMaxWidth())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(Geo.formatDuration(progress.remainingS), style = MaterialTheme.typography.headlineMedium, color = SW.EmeraldDeep)
                    Text("${Geo.formatDistance(progress.remainingM)} · arrive $eta", style = MaterialTheme.typography.bodyMedium, color = SW.Subtle)
                }
                CircleIconButton(Icons.Rounded.Map, "Open in Google Maps", onExternal, container = SW.Field, size = 50.dp)
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.height(50.dp).clip(RoundedCornerShape(25.dp)).background(SW.Coral).clickable(onClick = onEnd).padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("End", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ================================================================= helpers

private fun placeAccent(p: Place): Color = when {
    p.isSmartBin -> SW.Electric
    p.type == PlaceType.BUYER -> SW.Amber
    else -> SW.Emerald
}

private fun placeIcon(p: Place): ImageVector = when {
    p.type == PlaceType.BUYER -> Icons.Rounded.Store
    p.isSmartBin -> Icons.Rounded.VerifiedUser
    else -> (p.accepts.firstOrNull() ?: EWasteCategory.DEVICE_WITH_BATTERY).icon
}

private fun modeIcon(m: TravelMode): ImageVector = when (m) {
    TravelMode.WALK -> Icons.AutoMirrored.Rounded.DirectionsWalk
    TravelMode.CYCLE -> Icons.AutoMirrored.Rounded.DirectionsBike
    TravelMode.DRIVE -> Icons.Rounded.DirectionsCar
}

fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Hands off to Google Maps turn-by-turn voice navigation (or the web directions fallback). */
fun openExternalNavigation(context: Context, place: Place, mode: TravelMode) {
    val lat = place.point.lat
    val lng = place.point.lng
    val nav = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng&mode=${mode.googleMode}"))
        .setPackage("com.google.android.apps.maps")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val ok = runCatching { context.startActivity(nav) }.isSuccess
    if (!ok) {
        val travel = when (mode) {
            TravelMode.WALK -> "walking"
            TravelMode.CYCLE -> "bicycling"
            TravelMode.DRIVE -> "driving"
        }
        openUrl(context, "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=$travel")
    }
}

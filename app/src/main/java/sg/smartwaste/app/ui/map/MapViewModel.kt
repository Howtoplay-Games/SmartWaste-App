package sg.smartwaste.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sg.smartwaste.app.AppContainer
import sg.smartwaste.app.data.location.LocationFix
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.OnlineBuyer
import sg.smartwaste.app.data.model.Place
import sg.smartwaste.app.data.model.PlaceType
import sg.smartwaste.app.data.model.PlaceWithDistance
import sg.smartwaste.app.data.routing.Route
import sg.smartwaste.app.data.routing.RouteStep
import sg.smartwaste.app.data.routing.TravelMode
import sg.smartwaste.app.util.Geo
import sg.smartwaste.app.util.GeoPoint

enum class MapFilter(val label: String) { ALL("All"), BINS("Bins"), BUYERS("Buyers") }

data class NavProgress(
    val active: Boolean = false,
    val nextStep: RouteStep? = null,
    val distanceToNextM: Double = 0.0,
    val remainingM: Double = 0.0,
    val remainingS: Double = 0.0,
    val arrived: Boolean = false,
    val rerouting: Boolean = false,
)

data class MapUiState(
    val visible: List<PlaceWithDistance> = emptyList(),
    val filter: MapFilter = MapFilter.ALL,
    val category: EWasteCategory? = null,
    val query: String = "",
    val user: LocationFix? = null,
    val selected: PlaceWithDistance? = null,
    val route: Route? = null,
    val routeLoading: Boolean = false,
    val mode: TravelMode = TravelMode.WALK,
    val nav: NavProgress = NavProgress(),
    val darkMap: Boolean = false,
    val loading: Boolean = false,
    val skippedFull: Int = 0,
    val following: Boolean = false,
    val message: String? = null,
    val onlineBuyers: List<OnlineBuyer> = emptyList(),
)

class MapViewModel(private val c: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(MapUiState(onlineBuyers = c.places.onlineBuyers))
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private val _camera = MutableStateFlow<CameraRequest?>(null)
    val camera: StateFlow<CameraRequest?> = _camera.asStateFlow()

    private var trackingJob: Job? = null
    private var autoNearestPending = false
    private var pendingPlaceId: String? = null
    private var argsApplied = false
    private var firstFix = true
    private var lastReroute = 0L

    // Route geometry helpers for progress tracking.
    private var cumulative: DoubleArray = DoubleArray(0)
    private var stepVertex: IntArray = IntArray(0)

    init {
        viewModelScope.launch {
            combine(c.places.places, c.places.status, c.prefs.darkMap) { places, status, dark -> Triple(places, status, dark) }
                .collect { (places, status, dark) ->
                    _state.update { it.copy(darkMap = dark, loading = status.loading) }
                    recompute(places)
                }
        }
        c.location.last.value?.let { fix -> _state.update { it.copy(user = fix) } }
    }

    fun applyArgs(mode: String?, category: String?, auto: Boolean, placeId: String?, query: String?) {
        if (argsApplied) return
        argsApplied = true
        val filter = when (mode) {
            "bins" -> MapFilter.BINS
            "buyers" -> MapFilter.BUYERS
            else -> MapFilter.ALL
        }
        _state.update {
            it.copy(filter = filter, category = EWasteCategory.fromKey(category), query = query.orEmpty())
        }
        autoNearestPending = auto
        pendingPlaceId = placeId?.takeIf { it.isNotBlank() }
        recompute(c.places.places.value)
        pendingPlaceId?.let { id ->
            select(id); pendingPlaceId = null
        }
        // If we already know where the user is, act immediately.
        if (autoNearestPending && _state.value.user != null) {
            autoNearestPending = false
            selectNearest(routeToo = true)
        }
    }

    // ------------------------------------------------------------ tracking

    fun startTracking() {
        if (trackingJob?.isActive == true || !c.location.hasPermission()) return
        trackingJob = viewModelScope.launch {
            c.location.updates().collect { fix -> onFix(fix) }
        }
        viewModelScope.launch {
            if (_state.value.user == null) c.location.current()?.let { onFix(it) }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
    }

    private fun onFix(fix: LocationFix) {
        _state.update { it.copy(user = fix) }
        if (firstFix) {
            firstFix = false
            viewModelScope.launch { c.places.refresh(fix.point) }
            if (_state.value.selected == null && !autoNearestPending) {
                _camera.value = CameraRequest.Focus(fix.point, 14.5)
            }
        }
        if (autoNearestPending) {
            autoNearestPending = false
            selectNearest(routeToo = true)
        }
        recompute(c.places.places.value)
        updateNavigation(fix)
    }

    // ----------------------------------------------------------- filtering

    private fun origin(): GeoPoint = _state.value.user?.point ?: Geo.SINGAPORE

    private fun recompute(all: List<Place>) {
        val s = _state.value
        val from = origin()
        val q = s.query.trim().lowercase()
        val typeFilter = when (s.filter) {
            MapFilter.ALL -> null
            MapFilter.BINS -> PlaceType.BIN
            MapFilter.BUYERS -> PlaceType.BUYER
        }
        val matching = all.filter { p ->
            (typeFilter == null || p.type == typeFilter) &&
                (s.category == null || s.category in p.accepts) &&
                (q.isEmpty() || p.name.lowercase().contains(q) || p.address.lowercase().contains(q) || p.operator.lowercase().contains(q))
        }
        val visible = matching.map { PlaceWithDistance(it, Geo.distanceMeters(from, it.point)) }.sortedBy { it.distanceMeters }
        val selected = s.selected?.let { sel -> visible.firstOrNull { it.place.id == sel.place.id } ?: PlaceWithDistance(sel.place, Geo.distanceMeters(from, sel.place.point)) }
        _state.update { it.copy(visible = visible, selected = selected) }
    }

    fun setFilter(filter: MapFilter) {
        _state.update { it.copy(filter = filter) }
        recompute(c.places.places.value)
    }

    fun setCategory(category: EWasteCategory?) {
        _state.update { it.copy(category = category) }
        recompute(c.places.places.value)
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        recompute(c.places.places.value)
    }

    fun toggleDark() {
        viewModelScope.launch { c.prefs.setDarkMap(!_state.value.darkMap) }
    }

    fun refresh() {
        viewModelScope.launch { c.places.refresh(origin(), force = true) }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    // ----------------------------------------------------------- selection

    fun select(id: String) {
        val place = c.places.byId(id) ?: return
        stopNavigation()
        _state.update {
            it.copy(selected = PlaceWithDistance(place, Geo.distanceMeters(origin(), place.point)), route = null)
        }
        _camera.value = CameraRequest.Focus(place.point, 15.5)
    }

    fun clearSelection() {
        stopNavigation()
        _state.update { it.copy(selected = null, route = null) }
    }

    /** Finds the nearest suitable place (skipping full bins) and optionally routes to it. */
    fun selectNearest(routeToo: Boolean) {
        val s = _state.value
        val type = when (s.filter) {
            MapFilter.BUYERS -> PlaceType.BUYER
            MapFilter.BINS -> PlaceType.BIN
            MapFilter.ALL -> PlaceType.BIN
        }
        val from = origin()
        val all = c.places.nearest(from, type, s.category, limit = 50, skipFull = false)
        val usable = all.filterNot { it.place.type == PlaceType.BIN && it.place.isFull }
        val skipped = all.takeWhile { it.place.type == PlaceType.BIN && it.place.isFull }.size
        val best = usable.firstOrNull()
        if (best == null) {
            _state.update {
                it.copy(message = if (s.category == EWasteCategory.LARGE_APPLIANCE && type == PlaceType.BIN)
                    "Large appliances don't fit in bins. Showing buyers and recyclers instead."
                else "No matching places found nearby.")
            }
            if (s.category == EWasteCategory.LARGE_APPLIANCE && type == PlaceType.BIN) {
                setFilter(MapFilter.BUYERS); selectNearest(routeToo)
            }
            return
        }
        _state.update {
            it.copy(
                skippedFull = skipped,
                message = if (skipped > 0) "Skipped $skipped nearby bin(s) that are full." else it.message,
            )
        }
        select(best.place.id)
        if (routeToo) requestRoute()
    }

    // -------------------------------------------------------------- routing

    fun setMode(mode: TravelMode) {
        _state.update { it.copy(mode = mode) }
        if (_state.value.route != null) requestRoute(keepNavigating = _state.value.nav.active)
    }

    fun requestRoute(keepNavigating: Boolean = false) {
        val target = _state.value.selected?.place ?: return
        val from = _state.value.user?.point
        if (from == null) {
            _state.update { it.copy(message = "Turn on location for routes from where you are. Showing from Singapore centre.") }
        }
        val start = from ?: Geo.SINGAPORE
        _state.update { it.copy(routeLoading = true) }
        viewModelScope.launch {
            val route = c.routing.route(start, target.point, _state.value.mode)
            prepareProgress(route)
            _state.update {
                it.copy(
                    route = route,
                    routeLoading = false,
                    nav = if (keepNavigating) it.nav.copy(rerouting = false) else NavProgress(
                        remainingM = route.distanceMeters, remainingS = route.durationSeconds,
                        nextStep = route.steps.getOrNull(1) ?: route.steps.firstOrNull(),
                    ),
                    message = if (route.isEstimate) "Live routing unavailable. Showing an estimate." else it.message,
                )
            }
            if (!keepNavigating) _camera.value = CameraRequest.Fit(route.points.ifEmpty { listOf(start, target.point) })
        }
    }

    fun startNavigation() {
        val route = _state.value.route ?: return
        _state.update {
            it.copy(
                following = true,
                nav = it.nav.copy(active = true, arrived = false, remainingM = route.distanceMeters, remainingS = route.durationSeconds),
            )
        }
        _state.value.user?.let { updateNavigation(it) } ?: run {
            _camera.value = CameraRequest.Follow(route.points.first(), 0.0)
        }
    }

    fun stopNavigation() {
        _state.update { it.copy(nav = it.nav.copy(active = false, rerouting = false), following = false) }
    }

    fun recenter() {
        val user = _state.value.user
        if (_state.value.nav.active) {
            _state.update { it.copy(following = true) }
            user?.let { updateNavigation(it) }
        } else if (user != null) {
            _camera.value = CameraRequest.Focus(user.point, 15.5)
        } else {
            _state.update { it.copy(message = "Location unavailable. Enable location permission.") }
        }
    }

    fun onUserGesture() {
        if (_state.value.following) _state.update { it.copy(following = false) }
    }

    private fun prepareProgress(route: Route) {
        val pts = route.points
        cumulative = DoubleArray(pts.size)
        for (i in 1 until pts.size) cumulative[i] = cumulative[i - 1] + Geo.distanceMeters(pts[i - 1], pts[i])
        stepVertex = IntArray(route.steps.size) { idx -> closestIndex(pts, route.steps[idx].location) }
    }

    private fun closestIndex(pts: List<GeoPoint>, p: GeoPoint): Int {
        var best = 0
        var bestD = Double.MAX_VALUE
        pts.forEachIndexed { i, q ->
            val d = Geo.distanceMeters(p, q)
            if (d < bestD) { bestD = d; best = i }
        }
        return best
    }

    private fun updateNavigation(fix: LocationFix) {
        val s = _state.value
        val route = s.route ?: return
        if (!s.nav.active || route.points.isEmpty()) return
        val target = s.selected?.place ?: return

        val toDest = Geo.distanceMeters(fix.point, target.point)
        if (toDest < 35) {
            _state.update { it.copy(nav = it.nav.copy(arrived = true, remainingM = 0.0, remainingS = 0.0, distanceToNextM = 0.0)) }
            return
        }

        val idx = closestIndex(route.points, fix.point)
        val offBy = Geo.distanceMeters(fix.point, route.points[idx])
        val total = cumulative.lastOrNull() ?: route.distanceMeters
        val remaining = (total - cumulative.getOrElse(idx) { 0.0 } + offBy).coerceAtLeast(0.0)
        val speed = if (route.durationSeconds > 0) route.distanceMeters / route.durationSeconds else route.mode.fallbackSpeedMps
        val nextIdx = stepVertex.indices.firstOrNull { stepVertex[it] > idx } ?: (route.steps.size - 1)
        val next = route.steps.getOrNull(nextIdx)
        val toNext = if (next != null) (cumulative.getOrElse(stepVertex[nextIdx]) { total } - cumulative.getOrElse(idx) { 0.0 }).coerceAtLeast(0.0) else 0.0

        _state.update {
            it.copy(nav = it.nav.copy(nextStep = next, distanceToNextM = toNext, remainingM = remaining, remainingS = remaining / speed, arrived = false))
        }

        if (s.following) {
            val bearing = fix.bearing?.toDouble()
                ?: route.points.getOrNull(idx + 1)?.let { Geo.bearing(route.points[idx], it) }
                ?: 0.0
            _camera.value = CameraRequest.Follow(fix.point, bearing)
        }

        // Off-route: re-route at most every 15 s.
        val now = System.currentTimeMillis()
        if (!route.isEstimate && offBy > 55 + fix.accuracyMeters && now - lastReroute > 15_000) {
            lastReroute = now
            _state.update { it.copy(nav = it.nav.copy(rerouting = true)) }
            requestRoute(keepNavigating = true)
        }
    }

    override fun onCleared() {
        stopTracking()
    }
}

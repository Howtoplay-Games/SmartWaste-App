package sg.smartwaste.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sg.smartwaste.app.AppContainer
import sg.smartwaste.app.data.analytics.AnalyticsPipeline
import sg.smartwaste.app.data.analytics.AnalyticsResult
import sg.smartwaste.app.data.analytics.CleanData
import sg.smartwaste.app.data.analytics.Collected
import sg.smartwaste.app.data.analytics.DAY_MS
import sg.smartwaste.app.data.analytics.DemoDataGenerator
import sg.smartwaste.app.data.analytics.ItemEvent
import sg.smartwaste.app.data.analytics.ItemEventType
import sg.smartwaste.app.data.analytics.QrSession
import sg.smartwaste.app.data.analytics.RawData
import sg.smartwaste.app.data.db.DepositEntity
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.PlaceType
import sg.smartwaste.app.data.places.CuratedPlaces
import sg.smartwaste.app.util.GeoPoint

data class AnalyticsUiState(
    val loading: Boolean = true,
    val seed: Int = 1,
    /** 0 raw data, 1 clean data, 2 insights, 3 action. */
    val stage: Int = 0,
    val playing: Boolean = false,
    val result: AnalyticsResult? = null,
    val selectedBin: String? = null,
    val selectedCompartment: EWasteCategory = EWasteCategory.DEVICE_WITH_BATTERY,
    val remindersSent: Boolean = false,
    val campaignLaunched: Boolean = false,
)

/**
 * Runs the demo analytics pipeline. Everything is computed in memory on a background thread;
 * nothing is stored, sent, or added to the user's points.
 */
class AnalyticsViewModel(private val c: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsUiState())
    val state: StateFlow<AnalyticsUiState> = _state.asStateFlow()

    private var raw: RawData? = null
    private var clean: CleanData? = null
    private var loadJob: Job? = null
    private var playJob: Job? = null

    init {
        load(seed = 1)
    }

    /** New demo data with a different seed (same bins, new users and readings). */
    fun shuffle() = load(_state.value.seed + 1)

    private fun load(seed: Int) {
        loadJob?.cancel()
        _state.update { it.copy(loading = true, seed = seed, remindersSent = false, campaignLaunched = false) }
        loadJob = viewModelScope.launch {
            val email = c.auth.currentEmailNow()
            val deposits = c.history.deposits.first()
            val places = c.places.places.value
            val (r, cl, result) = withContext(Dispatchers.Default) {
                val now = System.currentTimeMillis()
                val smart = places.filter { it.isSmartBin }.ifEmpty { CuratedPlaces.smartBins(now) }
                val bins = places.filter { it.type == PlaceType.BIN }.ifEmpty { smart + CuratedPlaces.bins }
                val (events, sessions) = realActivity(deposits, email, now)
                val generated = DemoDataGenerator.generate(smart, bins, now, seed, realEvents = events, realSessions = sessions)
                val cleaned = AnalyticsPipeline.clean(generated)
                Triple(generated, cleaned, AnalyticsPipeline.analyse(generated, cleaned))
            }
            raw = r
            clean = cl
            _state.update { s ->
                s.copy(
                    loading = false,
                    result = result,
                    selectedBin = s.selectedBin?.takeIf { id -> r.smartBins.any { it.id == id } } ?: r.smartBins.firstOrNull()?.id,
                )
            }
        }
    }

    fun selectStage(stage: Int) {
        playJob?.cancel()
        _state.update { it.copy(stage = stage.coerceIn(0, STAGES - 1), playing = false) }
    }

    /** Steps through the four stages on its own, for recording the presentation video. */
    fun togglePlay() {
        if (playJob?.isActive == true) {
            playJob?.cancel()
            return
        }
        playJob = viewModelScope.launch {
            _state.update { it.copy(playing = true, stage = if (it.stage >= STAGES - 1) 0 else it.stage) }
            try {
                while (_state.value.stage < STAGES - 1) {
                    delay(STAGE_MS)
                    _state.update { it.copy(stage = it.stage + 1) }
                }
            } finally {
                _state.update { it.copy(playing = false) }
            }
        }
    }

    fun selectBin(id: String) = _state.update { it.copy(selectedBin = id) }

    fun selectCompartment(c: EWasteCategory) = _state.update { it.copy(selectedCompartment = c) }

    /** Empties the bins on today's routes and re-runs insights and actions, closing the loop. */
    fun simulatePickups() {
        val r = raw ?: return
        val cl = clean ?: return
        val current = _state.value.result ?: return
        val plan = current.actions.pickups
        val collected = Collected(
            general = current.collected.general + plan.general.stops.map { it.binId },
            battery = current.collected.battery + plan.battery.stops.map { it.binId },
        )
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { AnalyticsPipeline.analyse(r, cl, collected) }
            _state.update { it.copy(result = result) }
        }
    }

    fun sendReminders() = _state.update { it.copy(remindersSent = true) }

    fun launchCampaign() = _state.update { it.copy(campaignLaunched = true) }

    /** The device's real drop-offs from the last week, added to the demo data so they show up in it. */
    private fun realActivity(deposits: List<DepositEntity>, email: String, now: Long): Pair<List<ItemEvent>, List<QrSession>> {
        val user = email.ifBlank { "you@this-device" }
        val prefix = DemoDataGenerator.REAL_PREFIX
        val recent = deposits.filter { it.timestamp in (now - DemoDataGenerator.DAYS * DAY_MS)..now }
        val events = recent.mapNotNull { d ->
            val category = EWasteCategory.fromKey(d.category) ?: return@mapNotNull null
            ItemEvent(
                id = "${prefix}D${d.id}",
                type = ItemEventType.DEPOSIT,
                userId = user,
                category = category,
                time = d.timestamp,
                location = GeoPoint(d.lat, d.lng),
                binId = d.placeId,
                sessionId = if (d.binProof == "QR") "${prefix}S${d.id}" else null,
            )
        }
        val sessions = recent.filter { it.binProof == "QR" }.map { d ->
            QrSession("${prefix}S${d.id}", d.placeId, user, start = d.timestamp - 60_000, end = d.timestamp, items = d.quantity)
        }
        return events to sessions
    }

    companion object {
        const val STAGES = 4
        const val STAGE_MS = 7_000L
    }
}

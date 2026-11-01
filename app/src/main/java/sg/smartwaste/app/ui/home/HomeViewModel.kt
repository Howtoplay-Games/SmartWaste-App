package sg.smartwaste.app.ui.home

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sg.smartwaste.app.AppContainer
import sg.smartwaste.app.data.Impact
import sg.smartwaste.app.data.auth.SessionState
import sg.smartwaste.app.data.db.DepositEntity
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.OnlineBuyer
import sg.smartwaste.app.data.model.PlaceType
import sg.smartwaste.app.data.model.PlaceWithDistance
import sg.smartwaste.app.data.rewards.RewardsCatalog
import sg.smartwaste.app.data.rewards.Tier
import sg.smartwaste.app.util.Geo
import sg.smartwaste.app.util.RefreshGate
import java.util.Calendar

/** Minimum time between automatic Home refreshes (location + places). */
private const val REFRESH_INTERVAL_MS = 60_000L

data class HomeState(
    val name: String = "",
    val balance: Int = 0,
    val lifetime: Int = 0,
    val tier: Tier = RewardsCatalog.tiers.first(),
    val nextTier: Tier? = RewardsCatalog.tiers.getOrNull(1),
    val impact: Impact = Impact(),
    val monthItems: Int = 0,
    val lastMonthItems: Int = 0,
    val monthDeposits: Int = 0,
    val monthCo2: Double = 0.0,
    /** Points earned per day for the current month, day 1..today. */
    val trend: List<Float> = emptyList(),
    val latest: List<DepositEntity> = emptyList(),
    val nearbyBins: List<PlaceWithDistance> = emptyList(),
    val nearbyBuyers: List<PlaceWithDistance> = emptyList(),
    val onlineBuyers: List<OnlineBuyer> = emptyList(),
    val locationKnown: Boolean = false,
    val redeemableCount: Int = 0,
    val nearlyFullBins: Int = 0,
)

class HomeViewModel(private val c: AppContainer) : ViewModel() {

    /**
     * Latest fix from anywhere in the app (Home refreshes, Map tracking, deposit checks), or null
     * when there is none or it's outside Singapore. Null makes Home measure from the city centre.
     */
    private val origin = c.location.last.map { fix -> fix?.takeIf { it.inServiceArea }?.point }.distinctUntilChanged()
    val category = MutableStateFlow<EWasteCategory?>(null)

    private val name = c.auth.session.map { (it as? SessionState.SignedIn)?.user?.name.orEmpty() }
    private val points = combine(c.history.balance, c.history.lifetimePoints) { b, l -> b to l }
    private val history = combine(c.history.impact, c.history.deposits) { i, d -> i to d }

    val state: StateFlow<HomeState> = combine(
        name, points, history, c.places.places, combine(origin, category) { o, cat -> o to cat },
    ) { name, (balance, lifetime), (impact, deposits), places, (o, cat) ->
        val from = o ?: Geo.SINGAPORE
        val now = Calendar.getInstance()
        val monthStart = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val lastMonthStart = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val thisMonth = deposits.filter { it.timestamp >= monthStart.timeInMillis }
        val lastMonth = deposits.filter { it.timestamp in lastMonthStart.timeInMillis until monthStart.timeInMillis }
        val today = now.get(Calendar.DAY_OF_MONTH)
        val perDay = FloatArray(today)
        val cal = Calendar.getInstance()
        thisMonth.forEach { d ->
            cal.timeInMillis = d.timestamp
            val idx = cal.get(Calendar.DAY_OF_MONTH) - 1
            if (idx in perDay.indices) perDay[idx] += d.points.toFloat()
        }
        HomeState(
            name = name,
            balance = balance,
            lifetime = lifetime,
            tier = RewardsCatalog.tierFor(lifetime),
            nextTier = RewardsCatalog.nextTier(lifetime),
            impact = impact,
            monthItems = thisMonth.sumOf { it.quantity },
            lastMonthItems = lastMonth.sumOf { it.quantity },
            monthDeposits = thisMonth.size,
            monthCo2 = thisMonth.sumOf { it.co2Estimate },
            trend = perDay.toList(),
            latest = deposits.take(3),
            nearbyBins = c.places.nearest(from, PlaceType.BIN, cat, limit = 8),
            nearbyBuyers = c.places.nearest(from, PlaceType.BUYER, cat, limit = 8),
            onlineBuyers = c.places.onlineBuyers,
            locationKnown = o != null,
            redeemableCount = RewardsCatalog.rewards.count { it.cost <= balance },
            nearlyFullBins = places.count { it.isSmartBin && (it.fillLevel ?: 0) >= 75 },
        )
    }
        // Sorting every place by distance on each update is list work, not UI work.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    /**
     * Home re-enters composition on every tab switch. Without the gate, each visit started a
     * new high-accuracy GPS request (up to 12 s) and a places refresh.
     */
    private val refreshGate = RefreshGate(minIntervalMs = REFRESH_INTERVAL_MS, clock = SystemClock::elapsedRealtime)
    private var hadPermission = c.location.hasPermission()
    /** A forced refresh arrived while another one was running; run it when that one ends. */
    private var forcedPending = false

    /** True once the location prompt has been shown, so returning to Home doesn't ask again. */
    var locationPromptShown = false
        private set

    fun onLocationPromptShown() {
        locationPromptShown = true
    }

    /**
     * Gets a fresh fix and refreshes nearby places. [force] bypasses the time limit; permission
     * granted since the last refresh (here or on another screen) forces one too.
     */
    fun refreshLocation(force: Boolean = false) {
        val granted = c.location.hasPermission()
        val forced = force || (granted && !hadPermission)
        if (!refreshGate.tryStart(forced)) {
            if (forced) forcedPending = true
            return
        }
        hadPermission = granted
        viewModelScope.launch {
            try {
                // current() also publishes the fix to location.last, which feeds `origin`.
                val fix = c.location.current()
                c.places.refresh(fix?.point ?: Geo.SINGAPORE)
            } finally {
                refreshGate.finish()
                if (forcedPending) {
                    forcedPending = false
                    refreshLocation(force = true)
                }
            }
        }
    }

    fun selectCategory(cat: EWasteCategory?) {
        category.value = if (category.value == cat) null else cat
    }
}

package sg.smartwaste.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sg.smartwaste.app.AppContainer
import sg.smartwaste.app.data.Impact
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.OnlineBuyer
import sg.smartwaste.app.data.model.PlaceType
import sg.smartwaste.app.data.model.PlaceWithDistance
import sg.smartwaste.app.data.rewards.RewardsCatalog
import sg.smartwaste.app.data.rewards.Tier
import sg.smartwaste.app.util.Geo
import sg.smartwaste.app.util.GeoPoint

data class HomeState(
    val name: String = "",
    val balance: Int = 0,
    val lifetime: Int = 0,
    val tier: Tier = RewardsCatalog.tiers.first(),
    val impact: Impact = Impact(),
    val nearbyBins: List<PlaceWithDistance> = emptyList(),
    val nearbyBuyers: List<PlaceWithDistance> = emptyList(),
    val onlineBuyers: List<OnlineBuyer> = emptyList(),
    val locationKnown: Boolean = false,
    val redeemableCount: Int = 0,
)

class HomeViewModel(private val c: AppContainer) : ViewModel() {

    private val origin = MutableStateFlow<GeoPoint?>(c.location.last.value?.point)
    val category = MutableStateFlow<EWasteCategory?>(null)

    private val points = combine(c.history.balance, c.history.lifetimePoints) { b, l -> b to l }

    val state: StateFlow<HomeState> = combine(
        c.prefs.userName, points, c.history.impact, c.places.places, combine(origin, category) { o, cat -> o to cat },
    ) { name, (balance, lifetime), impact, _, (o, cat) ->
        val from = o ?: Geo.SINGAPORE
        HomeState(
            name = name,
            balance = balance,
            lifetime = lifetime,
            tier = RewardsCatalog.tierFor(lifetime),
            impact = impact,
            nearbyBins = c.places.nearest(from, PlaceType.BIN, cat, limit = 8),
            nearbyBuyers = c.places.nearest(from, PlaceType.BUYER, cat, limit = 8),
            onlineBuyers = c.places.onlineBuyers,
            locationKnown = o != null,
            redeemableCount = RewardsCatalog.rewards.count { it.cost <= balance },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun refreshLocation() {
        viewModelScope.launch {
            val fix = c.location.current()
            if (fix != null) origin.value = fix.point
            c.places.refresh(fix?.point ?: Geo.SINGAPORE)
        }
    }

    fun selectCategory(cat: EWasteCategory?) {
        category.value = if (category.value == cat) null else cat
    }
}

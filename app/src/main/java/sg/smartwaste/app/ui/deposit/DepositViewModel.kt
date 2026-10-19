package sg.smartwaste.app.ui.deposit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sg.smartwaste.app.AppContainer
import sg.smartwaste.app.data.ai.BinPhotoCheck
import sg.smartwaste.app.data.db.DepositEntity
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.PlaceType
import sg.smartwaste.app.data.model.PlaceWithDistance
import sg.smartwaste.app.data.rewards.BinProof
import sg.smartwaste.app.data.rewards.PointsEngine
import sg.smartwaste.app.data.verify.VerificationLevel
import sg.smartwaste.app.data.verify.VerificationOutcome
import sg.smartwaste.app.util.Geo
import sg.smartwaste.app.util.Hashing
import java.io.File

enum class DepositStep(val label: String) { LOCATION("At the bin"), PHOTO("Photo proof"), VERIFY("Verify item"), DONE("Done") }

data class DepositUiState(
    val step: DepositStep = DepositStep.LOCATION,
    val category: EWasteCategory? = null,
    val itemName: String = "",
    val quantity: Int = 1,
    val target: PlaceWithDistance? = null,
    val proof: BinProof? = null,
    val checkingLocation: Boolean = false,
    val sessionStart: Long? = null,
    val photoPath: String? = null,
    val checkingPhoto: Boolean = false,
    val photoCheck: BinPhotoCheck? = null,
    val verifying: Boolean = false,
    val verification: VerificationOutcome? = null,
    val saving: Boolean = false,
    val result: PointsEngine.Breakdown? = null,
    val error: String? = null,
)

class DepositViewModel(private val c: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(DepositUiState())
    val state: StateFlow<DepositUiState> = _state.asStateFlow()

    private var initialised = false

    fun init(placeId: String?, qr: String?) {
        if (initialised) return
        initialised = true
        val detection = c.session.detection.value
        val category = detection?.category?.takeIf { it.binFriendly }
        val place = placeId?.takeIf { it.isNotBlank() }?.let { c.places.byId(it) }
        val from = c.location.last.value?.point ?: Geo.SINGAPORE
        _state.update {
            it.copy(
                category = category,
                itemName = detection?.itemName ?: category?.title.orEmpty(),
                target = place?.let { p -> PlaceWithDistance(p, Geo.distanceMeters(from, p.point)) },
            )
        }
        if (!qr.isNullOrBlank()) onQr(qr)
    }

    fun setCategory(category: EWasteCategory) = _state.update {
        it.copy(
            category = category,
            quantity = it.quantity.coerceAtMost(PointsEngine.maxQuantity(category)),
            itemName = if (it.itemName.isBlank() || EWasteCategory.entries.any { c -> c.title == it.itemName }) category.title else it.itemName,
        )
    }

    fun setItemName(name: String) = _state.update { it.copy(itemName = name) }

    fun changeQuantity(delta: Int) = _state.update {
        val max = it.category?.let { c -> PointsEngine.maxQuantity(c) } ?: 5
        it.copy(quantity = (it.quantity + delta).coerceIn(1, max))
    }

    fun clearError() = _state.update { it.copy(error = null) }

    // ------------------------------------------------------------ step 1

    fun checkLocation() {
        val category = _state.value.category
        if (category == null) {
            _state.update { it.copy(error = "Choose what you're recycling first.") }
            return
        }
        _state.update { it.copy(checkingLocation = true, error = null) }
        viewModelScope.launch {
            val fix = c.location.current()
            if (fix == null) {
                _state.update { it.copy(checkingLocation = false, error = "Couldn't get your location. Enable location, or scan the bin's QR code.") }
                return@launch
            }
            if (fix.isMock) {
                _state.update { it.copy(checkingLocation = false, error = "Mock locations aren't allowed for earning points.") }
                return@launch
            }
            runCatching { c.places.refresh(fix.point) }
            val chosen = _state.value.target?.place
            val candidates = if (chosen != null) listOf(PlaceWithDistance(chosen, Geo.distanceMeters(fix.point, chosen.point)))
            else c.places.nearest(fix.point, PlaceType.BIN, category, limit = 5, skipFull = false)
            val best = candidates.minByOrNull { it.distanceMeters }
            val tolerance = PointsEngine.MAX_BIN_DISTANCE_M + fix.accuracyMeters.coerceAtMost(50f)
            when {
                best == null -> _state.update { it.copy(checkingLocation = false, error = "No e-waste bin for ${category.shortTitle.lowercase()} found nearby.") }
                category !in best.place.accepts -> _state.update {
                    it.copy(checkingLocation = false, target = best, error = "${best.place.name} doesn't accept ${category.shortTitle.lowercase()}. Try another bin.")
                }
                best.distanceMeters > tolerance -> _state.update {
                    it.copy(
                        checkingLocation = false, target = best,
                        error = "You're ${Geo.formatDistance(best.distanceMeters)} from ${best.place.name}. Get within ${PointsEngine.MAX_BIN_DISTANCE_M.toInt()} m, or scan the bin's QR code.",
                    )
                }
                else -> _state.update {
                    it.copy(checkingLocation = false, target = best, proof = BinProof.GPS, step = DepositStep.PHOTO)
                }
            }
        }
    }

    /** Handles a scanned SmartWaste bin QR (payload `SMARTWASTE:BIN:<id>` or a URL ending in the id). */
    fun onQr(payload: String) {
        val id = Regex("SMARTWASTE:BIN:([A-Za-z0-9-]+)", RegexOption.IGNORE_CASE).find(payload)?.groupValues?.get(1)
            ?: payload.trimEnd('/').substringAfterLast('/').takeIf { payload.contains("smartwaste", true) }
        val place = id?.let { c.places.byId(it.uppercase()) ?: c.places.byId(it) }
        if (place == null) {
            _state.update { it.copy(error = "Unknown SmartWaste bin QR code.") }
            return
        }
        val from = c.location.last.value?.point
        // Sanity check: a QR scanned far away from its bin (e.g. a photo of the QR) is rejected.
        if (from != null && Geo.distanceMeters(from, place.point) > 2_000) {
            _state.update { it.copy(error = "This QR belongs to ${place.name}, which is far from your location.") }
            return
        }
        val category = _state.value.category
        if (category != null && category !in place.accepts) {
            _state.update { it.copy(error = "${place.name} doesn't accept ${category.shortTitle.lowercase()}.") }
            return
        }
        _state.update {
            it.copy(
                target = PlaceWithDistance(place, from?.let { f -> Geo.distanceMeters(f, place.point) } ?: 0.0),
                proof = BinProof.QR,
                sessionStart = System.currentTimeMillis(),
                step = if (it.category != null) DepositStep.PHOTO else it.step,
                error = if (it.category == null) "Bin linked. Now choose what you're recycling." else null,
            )
        }
    }

    fun continueAfterQrCategory() {
        val s = _state.value
        if (s.proof != null && s.category != null) _state.update { it.copy(step = DepositStep.PHOTO, error = null) }
    }

    // ------------------------------------------------------------ step 2

    fun onPhoto(file: File) {
        val s = _state.value
        _state.update { it.copy(photoPath = file.path, checkingPhoto = true, photoCheck = null) }
        viewModelScope.launch {
            val check = c.ai.checkBinPhoto(file, s.category, s.itemName)
            _state.update { it.copy(checkingPhoto = false, photoCheck = check) }
        }
    }

    fun retakePhoto() = _state.update { it.copy(photoPath = null, photoCheck = null) }

    fun continueToVerify() = _state.update { it.copy(step = DepositStep.VERIFY, error = null) }

    // ------------------------------------------------------------ step 3

    fun onCode(raw: String) {
        if (_state.value.verifying || raw.isBlank()) return
        _state.update { it.copy(verifying = true, error = null) }
        viewModelScope.launch {
            val outcome = runCatching { c.verifier.verify(raw, c.session.detection.value) }.getOrNull()
            _state.update {
                it.copy(verifying = false, verification = outcome, error = if (outcome == null) "Verification failed. Try again." else null)
            }
        }
    }

    fun clearVerification() = _state.update { it.copy(verification = null) }

    fun preview(level: VerificationLevel): Int? {
        val s = _state.value
        val category = s.category ?: return null
        val proof = s.proof ?: return null
        return PointsEngine.compute(category, s.quantity, proof, s.photoCheck?.confirmed == true, level).total
    }

    /** Claims points. Pass skip = true to claim without item verification (fewer points). */
    fun claim(skip: Boolean) {
        val s = _state.value
        val category = s.category ?: return
        val proof = s.proof ?: return
        val target = s.target ?: return
        if (s.saving) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            if (c.history.depositsToday() >= PointsEngine.DAILY_DEPOSIT_LIMIT) {
                _state.update { it.copy(saving = false, error = "Daily limit of ${PointsEngine.DAILY_DEPOSIT_LIMIT} deposits reached. Come back tomorrow!") }
                return@launch
            }
            val verification = if (skip) null else s.verification
            val level = verification?.level ?: VerificationLevel.UNVERIFIED
            val breakdown = PointsEngine.compute(category, s.quantity, proof, s.photoCheck?.confirmed == true, level)
            val usable = verification?.takeIf { it.level != VerificationLevel.DUPLICATE }
            c.history.addDeposit(
                DepositEntity(
                    userEmail = "", // filled in by HistoryRepository
                    timestamp = System.currentTimeMillis(),
                    itemName = s.itemName.ifBlank { category.title },
                    category = category.name,
                    quantity = s.quantity,
                    placeId = target.place.id,
                    placeName = target.place.name,
                    lat = target.place.point.lat,
                    lng = target.place.point.lng,
                    binProof = proof.name,
                    photoPath = s.photoPath,
                    photoConfirmed = s.photoCheck?.confirmed == true,
                    verification = level.name,
                    codeType = verification?.codeType?.name,
                    // Duplicates aren't stored again so the original deposit keeps the claim.
                    codeHash = usable?.codeHash,
                    codeMasked = verification?.code?.let { Hashing.mask(it) },
                    productTitle = verification?.productTitle,
                    points = breakdown.total,
                    kgEstimate = PointsEngine.kgFor(category, s.quantity),
                    co2Estimate = PointsEngine.co2For(category, s.quantity),
                )
            )
            c.session.setDetection(null)
            _state.update { it.copy(saving = false, result = breakdown, step = DepositStep.DONE) }
        }
    }
}

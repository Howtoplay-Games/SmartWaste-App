package sg.smartwaste.app.data.rewards

import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.verify.VerificationLevel
import kotlin.math.roundToInt

enum class BinProof(val label: String, val factor: Double) {
    QR("SmartWaste bin QR scanned", 1.2),
    GPS("GPS confirmed at bin", 1.0),
}

/**
 * Transparent points calculation (shown to the user, per the SmartWaste data-transparency principle).
 *
 *   base         = category points × quantity (batteries per piece, max 20; others max 5)
 *   location     = ×1.2 if the SmartWaste bin QR was scanned, ×1.0 for GPS confirmation
 *   photo        = ×1.0 if AI confirms the bin photo, ×0.6 otherwise
 *   verification = + base × bonus (online-verified 100%, checksum 80%, plausible serial 50%, none 0%)
 */
object PointsEngine {

    const val DAILY_DEPOSIT_LIMIT = 10
    const val MAX_BIN_DISTANCE_M = 150.0

    data class Line(val label: String, val points: Int)
    data class Breakdown(val lines: List<Line>, val total: Int)

    fun maxQuantity(category: EWasteCategory) = if (category == EWasteCategory.SMALL_BATTERY) 20 else 5

    fun compute(
        category: EWasteCategory,
        quantity: Int,
        proof: BinProof,
        photoConfirmed: Boolean,
        verification: VerificationLevel,
    ): Breakdown {
        val qty = quantity.coerceIn(1, maxQuantity(category))
        val base = category.basePoints * qty
        val lines = mutableListOf(Line("${category.shortTitle} × $qty", base))

        val afterProof = (base * proof.factor).roundToInt()
        if (afterProof != base) lines += Line(proof.label, afterProof - base)

        val afterPhoto = if (photoConfirmed) afterProof else (afterProof * 0.6).roundToInt()
        if (afterPhoto != afterProof) lines += Line("Photo not confirmed by AI", afterPhoto - afterProof)

        val bonus = (base * verification.bonusFactor).roundToInt()
        lines += Line("Item verification: ${verification.label}", bonus)

        return Breakdown(lines, (afterPhoto + bonus).coerceAtLeast(1))
    }

    fun kgFor(category: EWasteCategory, quantity: Int) = category.avgKg * quantity

    /** Rough avoided-emissions factor for recycling e-waste vs. landfill/incineration (kg CO2e per kg). */
    fun co2For(category: EWasteCategory, quantity: Int) = kgFor(category, quantity) * 1.8
}

package sg.smartwaste.app.data.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DetectionDto(
    @SerialName("is_ewaste") val isEWaste: Boolean = true,
    @SerialName("item_name") val itemName: String = "Electronic item",
    val brand: String? = null,
    val category: String? = null,
    val confidence: Double = 0.5,
    @SerialName("hazard_level") val hazardLevel: String = "LOW",
    @SerialName("contains_battery") val containsBattery: Boolean = false,
    val description: String = "",
    @SerialName("preparation_steps") val preparationSteps: List<String> = emptyList(),
    @SerialName("safety_warnings") val safetyWarnings: List<String> = emptyList(),
    @SerialName("resale_potential") val resalePotential: String = "NONE",
    @SerialName("estimated_value_sgd") val estimatedValue: String? = null,
    val materials: List<String> = emptyList(),
    @SerialName("nea_regulated") val neaRegulated: Boolean = false,
)

@Serializable
data class BinCheckDto(
    @SerialName("is_collection_point") val isCollectionPoint: Boolean = false,
    @SerialName("item_visible") val itemVisible: Boolean = false,
    @SerialName("matches_expected_item") val matchesExpectedItem: Boolean = false,
    val confidence: Double = 0.0,
    val reason: String = "",
)

@Serializable
data class SerialCheckDto(
    val plausible: Boolean = false,
    @SerialName("code_kind") val codeKind: String = "",
    val reason: String = "",
)

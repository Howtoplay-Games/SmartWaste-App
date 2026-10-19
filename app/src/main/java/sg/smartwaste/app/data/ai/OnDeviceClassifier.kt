package sg.smartwaste.app.data.ai

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await
import sg.smartwaste.app.data.KnowledgeBase
import sg.smartwaste.app.data.model.DetectionEngine
import sg.smartwaste.app.data.model.DetectionResult
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.HazardLevel
import sg.smartwaste.app.data.model.ResalePotential
import sg.smartwaste.app.util.ImageUtils
import java.io.File

/**
 * Backup AI that runs fully on the phone (Google ML Kit image labelling), used when the
 * cloud models are unreachable. Less specific than the cloud model, so the result screen
 * lets the user correct the category.
 */
class OnDeviceClassifier {

    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.45f).build())
    }

    data class Label(val text: String, val confidence: Float)

    suspend fun labels(file: File): List<Label> {
        val bitmap = ImageUtils.loadUprightBitmap(file, 1024) ?: return emptyList()
        return labeler.process(InputImage.fromBitmap(bitmap, 0)).await()
            .map { Label(it.text, it.confidence) }
            .sortedByDescending { it.confidence }
    }

    private val rules: List<Triple<List<String>, String, EWasteCategory>> = listOf(
        Triple(listOf("mobile phone", "cell phone", "smartphone", "phone"), "Mobile phone", EWasteCategory.DEVICE_WITH_BATTERY),
        Triple(listOf("laptop", "notebook", "netbook"), "Laptop", EWasteCategory.DEVICE_WITH_BATTERY),
        Triple(listOf("tablet"), "Tablet", EWasteCategory.DEVICE_WITH_BATTERY),
        Triple(listOf("headphones", "earphone", "headset", "earbud"), "Headphones / earbuds", EWasteCategory.DEVICE_WITH_BATTERY),
        Triple(listOf("watch", "wristwatch"), "Smartwatch", EWasteCategory.DEVICE_WITH_BATTERY),
        Triple(listOf("camera", "camcorder"), "Camera", EWasteCategory.DEVICE_WITH_BATTERY),
        Triple(listOf("remote control", "joystick", "gamepad", "game controller"), "Controller / remote", EWasteCategory.DEVICE_WITH_BATTERY),
        Triple(listOf("battery", "batteries"), "Battery", EWasteCategory.SMALL_BATTERY),
        Triple(listOf("light bulb", "lamp", "lightbulb", "fluorescent"), "Lamp / bulb", EWasteCategory.LAMP),
        Triple(listOf("refrigerator", "fridge", "washing machine", "washer", "air conditioner", "television", "scooter"), "Large appliance", EWasteCategory.LARGE_APPLIANCE),
        Triple(listOf("monitor", "screen", "display", "printer", "router", "modem", "keyboard", "computer", "desktop", "microwave", "kettle", "speaker", "toaster", "appliance"), "Electrical appliance", EWasteCategory.APPLIANCE_NO_BATTERY),
        Triple(listOf("circuit", "electronics", "computer hardware", "chip", "cable", "wire", "electrical supply"), "Electronic component", EWasteCategory.COMPONENT),
    )

    suspend fun classify(file: File, reason: String): DetectionResult {
        val labels = runCatching { labels(file) }.getOrDefault(emptyList())
        var match: Triple<List<String>, String, EWasteCategory>? = null
        var confidence = 0f
        loop@ for (label in labels) {
            val text = label.text.lowercase()
            for (rule in rules) {
                if (rule.first.any { it in text }) {
                    match = rule; confidence = label.confidence; break@loop
                }
            }
        }
        val category = match?.third
        val guidance = category?.let { KnowledgeBase.guidance(it) }
        val topLabels = labels.take(3).joinToString { it.text }
        return DetectionResult(
            itemName = match?.second ?: labels.firstOrNull()?.text ?: "Unrecognised item",
            category = category,
            isEWaste = category != null,
            confidence = if (category != null) confidence * 0.8f else 0.2f,
            hazardLevel = when (category) {
                EWasteCategory.SMALL_BATTERY, EWasteCategory.DEVICE_WITH_BATTERY -> HazardLevel.MEDIUM
                EWasteCategory.LAMP, EWasteCategory.LARGE_APPLIANCE -> HazardLevel.MEDIUM
                else -> HazardLevel.LOW
            },
            containsBattery = category == EWasteCategory.DEVICE_WITH_BATTERY || category == EWasteCategory.SMALL_BATTERY,
            description = buildString {
                append("Identified offline by the on-device backup AI")
                if (topLabels.isNotBlank()) append(" (saw: $topLabels)")
                append(". Cloud AI was unavailable: $reason. Please confirm the category below.")
            },
            preparationSteps = guidance?.preparation ?: emptyList(),
            safetyWarnings = guidance?.safety ?: emptyList(),
            resalePotential = if (category == EWasteCategory.DEVICE_WITH_BATTERY) ResalePotential.MEDIUM else ResalePotential.LOW,
            neaRegulated = category != null && category != EWasteCategory.COMPONENT,
            engine = DetectionEngine.ON_DEVICE,
            modelName = "ML Kit Image Labeling (on-device)",
            photoPath = file.path,
        )
    }
}

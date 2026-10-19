package sg.smartwaste.app.data.ai

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import sg.smartwaste.app.BuildConfig
import sg.smartwaste.app.data.KnowledgeBase
import sg.smartwaste.app.data.model.DetectionEngine
import sg.smartwaste.app.data.model.DetectionResult
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.data.model.HazardLevel
import sg.smartwaste.app.data.model.ResalePotential
import sg.smartwaste.app.util.ImageUtils
import java.io.File

data class BinPhotoCheck(
    val confirmed: Boolean,
    val itemVisible: Boolean,
    val confidence: Float,
    val reason: String,
    /** False when no AI (cloud or on-device) could evaluate the photo. */
    val evaluated: Boolean,
)

data class SerialPlausibility(val plausible: Boolean, val kind: String, val reason: String)

data class ChatTurn(val fromUser: Boolean, val text: String)

class AiRepository(
    private val client: OpenRouterClient,
    private val onDevice: OnDeviceClassifier,
    private val json: Json,
) {
    private val visionModels = BuildConfig.AI_VISION_MODELS.split(",")
    private val chatModels = BuildConfig.AI_CHAT_MODELS.split(",")

    /** Identifies e-waste in a photo: cloud vision models first, on-device ML Kit as backup. */
    suspend fun detect(photo: File): DetectionResult {
        val base64 = ImageUtils.toBase64Jpeg(photo, maxDim = 1024)
            ?: return onDevice.classify(photo, "the photo could not be read")
        return try {
            val completion = client.complete(
                models = visionModels,
                messages = listOf(
                    OpenRouterClient.Msg.system(Prompts.DETECTION_SYSTEM),
                    OpenRouterClient.Msg.userWithImage(Prompts.DETECTION_USER, base64),
                ),
                temperature = 0.1,
            )
            val raw = OpenRouterClient.extractJsonObject(completion.text) ?: error("AI reply had no JSON")
            json.decodeFromString(DetectionDto.serializer(), raw).toResult(completion.model, photo)
        } catch (e: Exception) {
            Log.w(TAG, "Cloud detection failed, using on-device backup", e)
            onDevice.classify(photo, e.message?.take(80) ?: "network error")
        }
    }

    /** Checks the deposit proof photo shows a collection bin (and ideally the item). */
    suspend fun checkBinPhoto(photo: File, expected: EWasteCategory?, itemName: String?): BinPhotoCheck {
        val base64 = ImageUtils.toBase64Jpeg(photo, maxDim = 896)
        if (base64 != null) {
            try {
                val completion = client.complete(
                    models = visionModels,
                    messages = listOf(
                        OpenRouterClient.Msg.system("You verify recycling proof photos. Reply with JSON only."),
                        OpenRouterClient.Msg.userWithImage(Prompts.binCheckUser(expected, itemName), base64),
                    ),
                    temperature = 0.0,
                    maxTokens = 600,
                )
                val raw = OpenRouterClient.extractJsonObject(completion.text) ?: error("no JSON")
                val dto = json.decodeFromString(BinCheckDto.serializer(), raw)
                return BinPhotoCheck(
                    confirmed = dto.isCollectionPoint && dto.confidence >= 0.5,
                    itemVisible = dto.itemVisible,
                    confidence = dto.confidence.toFloat().coerceIn(0f, 1f),
                    reason = dto.reason.ifBlank { if (dto.isCollectionPoint) "Collection bin recognised" else "No bin recognised" },
                    evaluated = true,
                )
            } catch (e: Exception) {
                Log.w(TAG, "Cloud bin check failed, using on-device backup", e)
            }
        }
        // On-device backup: look for container / bin style labels.
        val labels = runCatching { onDevice.labels(photo) }.getOrDefault(emptyList())
        if (labels.isEmpty()) return BinPhotoCheck(false, false, 0f, "AI unavailable. Photo saved for review", evaluated = false)
        val binWords = listOf("bin", "container", "waste", "box", "machine", "kiosk", "metal")
        val hit = labels.firstOrNull { l -> binWords.any { it in l.text.lowercase() } }
        return BinPhotoCheck(
            confirmed = hit != null && hit.confidence >= 0.6f,
            itemVisible = labels.any { it.text.contains("phone", true) || it.text.contains("electronic", true) },
            confidence = hit?.confidence ?: 0f,
            reason = if (hit != null) "On-device AI saw: ${hit.text}" else "On-device AI couldn't recognise a bin",
            evaluated = true,
        )
    }

    /** Asks the model whether a serial number looks genuine for the item/brand. Null if AI unavailable. */
    suspend fun serialPlausibility(code: String, itemName: String?, brand: String?): SerialPlausibility? = try {
        val completion = client.complete(
            models = chatModels,
            messages = listOf(
                OpenRouterClient.Msg.system("You are a hardware identification expert. Reply with JSON only."),
                OpenRouterClient.Msg.user(Prompts.serialCheckUser(code, itemName, brand)),
            ),
            temperature = 0.0,
            maxTokens = 500,
        )
        val raw = OpenRouterClient.extractJsonObject(completion.text) ?: error("no JSON")
        val dto = json.decodeFromString(SerialCheckDto.serializer(), raw)
        SerialPlausibility(dto.plausible, dto.codeKind, dto.reason)
    } catch (e: Exception) {
        Log.w(TAG, "Serial plausibility check failed", e)
        null
    }

    /** AI support chat. Falls back to the offline knowledge base. */
    suspend fun chat(history: List<ChatTurn>): ChatTurn {
        val messages = buildList<JsonObject> {
            add(OpenRouterClient.Msg.system(Prompts.ASSISTANT_SYSTEM))
            history.takeLast(16).forEach {
                add(if (it.fromUser) OpenRouterClient.Msg.user(it.text) else OpenRouterClient.Msg.assistant(it.text))
            }
        }
        return try {
            val completion = client.complete(chatModels, messages, temperature = 0.4, maxTokens = 1200)
            ChatTurn(fromUser = false, text = completion.text)
        } catch (e: Exception) {
            Log.w(TAG, "Chat failed, answering offline", e)
            val question = history.lastOrNull { it.fromUser }?.text.orEmpty()
            ChatTurn(fromUser = false, text = KnowledgeBase.offlineAnswer(question) + "\n\n(Offline answer: AI is unreachable right now.)")
        }
    }

    private fun DetectionDto.toResult(model: String, photo: File): DetectionResult {
        val cat = EWasteCategory.fromKey(category)
        val guidance = cat?.let { KnowledgeBase.guidance(it) }
        return DetectionResult(
            itemName = itemName.ifBlank { "Electronic item" },
            brand = brand?.takeIf { it.isNotBlank() && !it.equals("null", true) },
            category = if (isEWaste) cat else null,
            isEWaste = isEWaste && cat != null,
            confidence = confidence.toFloat().coerceIn(0f, 1f),
            hazardLevel = runCatching { HazardLevel.valueOf(hazardLevel.uppercase()) }.getOrDefault(HazardLevel.LOW),
            containsBattery = containsBattery,
            description = description,
            preparationSteps = preparationSteps.ifEmpty { guidance?.preparation ?: emptyList() },
            safetyWarnings = safetyWarnings.ifEmpty { guidance?.safety ?: emptyList() },
            resalePotential = runCatching { ResalePotential.valueOf(resalePotential.uppercase()) }.getOrDefault(ResalePotential.NONE),
            estimatedValue = estimatedValue?.takeIf { it.isNotBlank() && !it.equals("null", true) },
            materials = materials,
            neaRegulated = neaRegulated,
            engine = DetectionEngine.CLOUD,
            modelName = model,
            photoPath = photo.path,
        )
    }

    private companion object {
        const val TAG = "AiRepository"
    }
}

package sg.smartwaste.app.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class AiUnavailableException(message: String) : IOException(message)
class AiAuthException(message: String) : IOException(message)

/**
 * Minimal OpenRouter chat-completions client (OpenAI-compatible API).
 *
 * Reliability strategy:
 *  1. Send the whole model chain via OpenRouter's `models` array so OpenRouter fails over
 *     server-side (rate limits, outages, moderation, context errors).
 *  2. If the request itself fails, retry each model individually client-side.
 *  3. Callers fall back to on-device AI if everything fails.
 */
class OpenRouterClient(
    private val http: OkHttpClient,
    private val json: Json,
    private val apiKey: suspend () -> String,
) {
    data class Completion(val text: String, val model: String)

    object Msg {
        fun system(text: String) = buildJsonObject { put("role", "system"); put("content", text) }
        fun user(text: String) = buildJsonObject { put("role", "user"); put("content", text) }
        fun assistant(text: String) = buildJsonObject { put("role", "assistant"); put("content", text) }

        /** User message with an image (base64 JPEG) and text, for vision models. */
        fun userWithImage(text: String, base64Jpeg: String) = buildJsonObject {
            put("role", "user")
            putJsonArray("content") {
                add(buildJsonObject { put("type", "text"); put("text", text) })
                add(buildJsonObject {
                    put("type", "image_url")
                    putJsonObject("image_url") { put("url", "data:image/jpeg;base64,$base64Jpeg") }
                })
            }
        }
    }

    suspend fun complete(
        models: List<String>,
        messages: List<JsonObject>,
        temperature: Double = 0.2,
        maxTokens: Int = 1800,
    ): Completion = withContext(Dispatchers.IO) {
        val key = apiKey().trim()
        if (key.isBlank()) throw AiUnavailableException("No OpenRouter API key configured")
        val chain = models.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (chain.isEmpty()) throw AiUnavailableException("No AI models configured")

        val attempts = buildList {
            add(chain.take(3))
            chain.forEach { add(listOf(it)) }
        }.distinct()

        var lastError: Exception? = null
        for (attempt in attempts) {
            try {
                return@withContext call(key, attempt, messages, temperature, maxTokens)
            } catch (e: AiAuthException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("AI request failed")
    }

    private fun call(
        key: String,
        chain: List<String>,
        messages: List<JsonObject>,
        temperature: Double,
        maxTokens: Int,
    ): Completion {
        val body = buildJsonObject {
            put("model", chain.first())
            if (chain.size > 1) putJsonArray("models") { chain.forEach { add(it) } }
            put("messages", JsonArray(messages))
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            // Keep reasoning short and hidden: we only want the final answer.
            putJsonObject("reasoning") { put("effort", "low"); put("exclude", true) }
        }
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $key")
            .header("HTTP-Referer", "https://github.com/Howtoplay-Games/SmartWaste-App")
            .header("X-Title", "SmartWaste SG")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(request).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (resp.code == 401) throw AiAuthException("OpenRouter rejected the API key (401)")
            if (!resp.isSuccessful) throw IOException("OpenRouter HTTP ${resp.code}: ${text.take(200)}")
            val root = json.parseToJsonElement(text).jsonObject
            (root["error"] as? JsonObject)?.let {
                throw IOException("OpenRouter error: ${it["message"]?.jsonPrimitive?.contentOrNull}")
            }
            val message = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
                ?: throw IOException("Empty AI response")
            val output = when (val content = message["content"]) {
                is JsonPrimitive -> content.contentOrNull.orEmpty()
                is JsonArray -> content.joinToString("") {
                    (it as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()
                }
                else -> ""
            }
            if (output.isBlank()) throw IOException("Empty AI response")
            val model = root["model"]?.jsonPrimitive?.contentOrNull ?: chain.first()
            return Completion(output.trim(), model)
        }
    }

    companion object {
        /** Extracts the first JSON object from a model reply (handles ```json fences and prose). */
        fun extractJsonObject(text: String): String? {
            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            return if (start >= 0 && end > start) text.substring(start, end + 1) else null
        }
    }
}

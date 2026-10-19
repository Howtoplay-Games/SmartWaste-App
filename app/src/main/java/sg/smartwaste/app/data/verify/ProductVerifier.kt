package sg.smartwaste.app.data.verify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import sg.smartwaste.app.data.ai.AiRepository
import sg.smartwaste.app.data.db.DepositDao
import sg.smartwaste.app.data.model.DetectionResult
import sg.smartwaste.app.data.model.EWasteCategory
import sg.smartwaste.app.util.Hashing

enum class CodeType(val label: String) {
    GTIN("Product barcode (EAN/UPC)"),
    IMEI("IMEI"),
    SERIAL("Serial number"),
    OTHER("Code"),
}

enum class VerificationLevel(val label: String, val bonusFactor: Double) {
    ONLINE_VERIFIED("Verified online", 1.0),
    CHECKSUM_VALID("Valid checksum", 0.8),
    PLAUSIBLE("Plausible serial", 0.5),
    UNVERIFIED("Not verified", 0.0),
    DUPLICATE("Already used", 0.0),
}

data class VerificationOutcome(
    val level: VerificationLevel,
    val codeType: CodeType,
    val code: String,
    val codeHash: String,
    val productTitle: String? = null,
    val brand: String? = null,
    val source: String,
    val details: String,
    /** True if the looked-up product matches the AI-detected category, null if unknown. */
    val categoryMatch: Boolean? = null,
)

/**
 * Verifies the barcode / serial number / IMEI scanned from an item:
 *  - GTIN barcodes (EAN-8/UPC-A/EAN-13/GTIN-14): GS1 check digit, then online lookup in
 *    UPCitemdb (free trial endpoint) and Open Products Facts.
 *  - IMEI (15 digits): Luhn checksum (the GSMA standard check digit).
 *  - Other serials: format checks plus an AI plausibility check for the detected brand.
 *  - Any code already used in a previous deposit is rejected for bonus points (anti-fraud).
 */
class ProductVerifier(
    private val http: OkHttpClient,
    private val json: Json,
    private val ai: AiRepository,
    private val deposits: DepositDao,
) {

    suspend fun verify(rawCode: String, detection: DetectionResult?): VerificationOutcome = withContext(Dispatchers.IO) {
        val code = normalise(rawCode)
        val hash = Hashing.sha256(code)
        val type = classify(code)

        if (deposits.countByCodeHash(hash) > 0) {
            return@withContext VerificationOutcome(
                VerificationLevel.DUPLICATE, type, code, hash, source = "SmartWaste",
                details = "This code was already used for a previous deposit, so no verification bonus applies.",
            )
        }

        when (type) {
            CodeType.GTIN -> verifyGtin(code, hash, detection)
            CodeType.IMEI -> VerificationOutcome(
                VerificationLevel.CHECKSUM_VALID, type, code, hash, source = "GSMA Luhn check",
                details = "IMEI check digit is valid (TAC ${code.take(8)}).",
                categoryMatch = detection?.category?.let { it == EWasteCategory.DEVICE_WITH_BATTERY },
            )
            CodeType.SERIAL, CodeType.OTHER -> verifySerial(code, hash, type, detection)
        }
    }

    // --------------------------------------------------------------- GTIN

    private fun verifyGtin(code: String, hash: String, detection: DetectionResult?): VerificationOutcome {
        val lookup = runCatching { upcItemDb(code) }.getOrNull() ?: runCatching { openProductsFacts(code) }.getOrNull()
        return if (lookup != null) {
            VerificationOutcome(
                VerificationLevel.ONLINE_VERIFIED, CodeType.GTIN, code, hash,
                productTitle = lookup.title, brand = lookup.brand, source = lookup.source,
                details = "Product found in ${lookup.source}.",
                categoryMatch = detection?.category?.let { categoryMatches(it, "${lookup.title} ${lookup.category}") },
            )
        } else {
            VerificationOutcome(
                VerificationLevel.CHECKSUM_VALID, CodeType.GTIN, code, hash, source = "GS1 check digit",
                details = "Valid GS1 barcode, but this product isn't listed in the public databases.",
            )
        }
    }

    private data class Lookup(val title: String, val brand: String?, val category: String, val source: String)

    private fun upcItemDb(code: String): Lookup? {
        val req = Request.Builder()
            .url("https://api.upcitemdb.com/prod/trial/lookup?upc=$code")
            .header("Accept", "application/json")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val root = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            if ((root["total"]?.jsonPrimitive?.intOrNull ?: 0) <= 0) return null
            val item = (root["items"] as? JsonArray)?.firstOrNull()?.jsonObject ?: return null
            val title = item["title"]?.jsonPrimitive?.contentOrNull ?: return null
            return Lookup(
                title = title,
                brand = item["brand"]?.jsonPrimitive?.contentOrNull,
                category = item["category"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                source = "UPCitemdb",
            )
        }
    }

    private fun openProductsFacts(code: String): Lookup? {
        val req = Request.Builder()
            .url("https://world.openproductsfacts.org/api/v2/product/$code.json")
            .header("User-Agent", "SmartWasteSG/1.0 (Android)")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val root = json.parseToJsonElement(resp.body?.string().orEmpty()).jsonObject
            if (root["status"]?.jsonPrimitive?.intOrNull != 1) return null
            val product = root["product"] as? JsonObject ?: return null
            val title = product["product_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
            return Lookup(
                title = title,
                brand = product["brands"]?.jsonPrimitive?.contentOrNull,
                category = product["categories"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                source = "Open Products Facts",
            )
        }
    }

    // ------------------------------------------------------------- Serial

    private suspend fun verifySerial(code: String, hash: String, type: CodeType, detection: DetectionResult?): VerificationOutcome {
        val formatOk = code.length in 6..24 && code.any { it.isDigit() } && code.all { it.isLetterOrDigit() || it == '-' }
        if (!formatOk) {
            return VerificationOutcome(
                VerificationLevel.UNVERIFIED, type, code, hash, source = "Format check",
                details = "This doesn't look like a manufacturer serial number.",
            )
        }
        val check = ai.serialPlausibility(code, detection?.itemName, detection?.brand)
        return when {
            check == null -> VerificationOutcome(
                VerificationLevel.PLAUSIBLE, CodeType.SERIAL, code, hash, source = "Format check",
                details = "Serial format looks valid (AI cross-check unavailable).",
            )
            check.plausible -> VerificationOutcome(
                VerificationLevel.PLAUSIBLE, CodeType.SERIAL, code, hash, source = "AI cross-check",
                details = check.reason.ifBlank { "Serial format matches this kind of device." },
            )
            else -> VerificationOutcome(
                VerificationLevel.UNVERIFIED, CodeType.SERIAL, code, hash, source = "AI cross-check",
                details = check.reason.ifBlank { "Serial doesn't match the detected device." },
            )
        }
    }

    // ------------------------------------------------------------ helpers

    companion object {
        fun normalise(raw: String): String =
            raw.trim().uppercase().replace(Regex("^(S/?N|SERIAL( NO\\.?| NUMBER)?|IMEI ?\\d?)[:#. ]*"), "").replace(" ", "")

        fun classify(code: String): CodeType = when {
            code.all { it.isDigit() } && code.length == 15 && luhnValid(code) -> CodeType.IMEI
            code.all { it.isDigit() } && code.length in setOf(8, 12, 13, 14) && gs1Valid(code) -> CodeType.GTIN
            code.any { it.isLetter() } && code.any { it.isDigit() } -> CodeType.SERIAL
            else -> CodeType.OTHER
        }

        /** GS1 mod-10 check digit for EAN-8, UPC-A, EAN-13 and GTIN-14. */
        fun gs1Valid(code: String): Boolean {
            if (code.length < 8 || !code.all { it.isDigit() }) return false
            val digits = code.map { it - '0' }
            val body = digits.dropLast(1).reversed()
            val sum = body.mapIndexed { i, d -> if (i % 2 == 0) d * 3 else d }.sum()
            val check = (10 - sum % 10) % 10
            return check == digits.last()
        }

        /** Luhn checksum used by IMEI numbers. */
        fun luhnValid(code: String): Boolean {
            if (!code.all { it.isDigit() }) return false
            var sum = 0
            code.reversed().forEachIndexed { i, c ->
                var d = c - '0'
                if (i % 2 == 1) { d *= 2; if (d > 9) d -= 9 }
                sum += d
            }
            return sum % 10 == 0
        }

        private val categoryKeywords = mapOf(
            EWasteCategory.DEVICE_WITH_BATTERY to listOf("phone", "laptop", "tablet", "ipad", "watch", "headphone", "earbud", "airpods", "kindle", "camera", "notebook", "macbook", "galaxy", "iphone", "pixel", "speaker"),
            EWasteCategory.APPLIANCE_NO_BATTERY to listOf("monitor", "printer", "router", "modem", "kettle", "microwave", "keyboard", "desktop", "set-top", "toaster", "appliance"),
            EWasteCategory.SMALL_BATTERY to listOf("battery", "batteries", "alkaline", "lithium", "power bank", "aa ", "aaa"),
            EWasteCategory.COMPONENT to listOf("ram", "memory", "ssd", "hard drive", "hdd", "cpu", "processor", "motherboard", "graphics", "cable", "charger", "adapter"),
            EWasteCategory.LAMP to listOf("bulb", "lamp", "led", "fluorescent", "light"),
            EWasteCategory.LARGE_APPLIANCE to listOf("refrigerator", "fridge", "washer", "washing", "dryer", "television", "tv", "air conditioner", "scooter"),
        )

        /** Returns true/false when the product text clearly does/doesn't match, null if undetermined. */
        fun categoryMatches(category: EWasteCategory, productText: String): Boolean? {
            val text = productText.lowercase()
            val own = categoryKeywords[category].orEmpty().any { it in text }
            if (own) return true
            val other = categoryKeywords.filterKeys { it != category }.values.flatten().any { it in text }
            return if (other) false else null
        }
    }
}

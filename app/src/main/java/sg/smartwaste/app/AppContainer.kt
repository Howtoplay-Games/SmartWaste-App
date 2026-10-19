package sg.smartwaste.app

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import sg.smartwaste.app.data.HistoryRepository
import sg.smartwaste.app.data.SessionStore
import sg.smartwaste.app.data.ai.AiRepository
import sg.smartwaste.app.data.ai.OnDeviceClassifier
import sg.smartwaste.app.data.ai.OpenRouterClient
import sg.smartwaste.app.data.db.AppDatabase
import sg.smartwaste.app.data.location.LocationClient
import sg.smartwaste.app.data.places.PlacesRepository
import sg.smartwaste.app.data.prefs.UserPrefs
import sg.smartwaste.app.data.routing.RoutingRepository
import sg.smartwaste.app.data.verify.ProductVerifier
import java.util.concurrent.TimeUnit

/** Manual dependency container (keeps the project free of DI frameworks). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(75, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(100, TimeUnit.SECONDS)
        .build()

    val prefs = UserPrefs(appContext)
    private val db = AppDatabase.build(appContext)
    val history = HistoryRepository(db)
    val location = LocationClient(appContext)
    val places = PlacesRepository(appContext, http, json)
    val routing = RoutingRepository(http, json)
    val session = SessionStore()

    private val openRouter = OpenRouterClient(http, json) {
        prefs.apiKeyOverride.first().ifBlank { BuildConfig.OPENROUTER_API_KEY }
    }
    val ai = AiRepository(openRouter, OnDeviceClassifier(), json)
    val verifier = ProductVerifier(http, json, ai, db.depositDao())
}

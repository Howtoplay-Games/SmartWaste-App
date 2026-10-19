package sg.smartwaste.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "smartwaste_prefs")

class UserPrefs(private val context: Context) {

    private object Keys {
        val SESSION_EMAIL = stringPreferencesKey("session_email")
        val LAST_EMAIL = stringPreferencesKey("last_email")
        val API_KEY = stringPreferencesKey("openrouter_key_override")
        val DARK_MAP = booleanPreferencesKey("dark_map_v2")
    }

    /** Persisted session (only written when "Remember me" is ticked). */
    val sessionEmail: Flow<String?> = context.dataStore.data.map { it[Keys.SESSION_EMAIL]?.takeIf { e -> e.isNotBlank() } }
    val lastEmail: Flow<String> = context.dataStore.data.map { it[Keys.LAST_EMAIL].orEmpty() }

    /** Optional key entered in Settings. Overrides the build-time key from local.properties. */
    val apiKeyOverride: Flow<String> = context.dataStore.data.map { it[Keys.API_KEY].orEmpty() }

    /** Dark basemap by default so the map matches the app's dark theme. */
    val darkMap: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MAP] ?: true }

    suspend fun setSession(email: String?, remember: Boolean) {
        context.dataStore.edit {
            if (email != null && remember) it[Keys.SESSION_EMAIL] = email else it.remove(Keys.SESSION_EMAIL)
            if (email != null) it[Keys.LAST_EMAIL] = email
        }
    }

    suspend fun setApiKey(key: String) = context.dataStore.edit { it[Keys.API_KEY] = key.trim() }
    suspend fun setDarkMap(dark: Boolean) = context.dataStore.edit { it[Keys.DARK_MAP] = dark }
}

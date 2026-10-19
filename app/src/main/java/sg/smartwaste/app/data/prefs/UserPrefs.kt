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
        val NAME = stringPreferencesKey("name")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val API_KEY = stringPreferencesKey("openrouter_key_override")
        val DARK_MAP = booleanPreferencesKey("dark_map")
    }

    val userName: Flow<String> = context.dataStore.data.map { it[Keys.NAME].orEmpty() }
    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDED] ?: false }

    /** Optional key entered in Settings. Overrides the build-time key from local.properties. */
    val apiKeyOverride: Flow<String> = context.dataStore.data.map { it[Keys.API_KEY].orEmpty() }
    val darkMap: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MAP] ?: false }

    suspend fun completeOnboarding(name: String) {
        context.dataStore.edit {
            it[Keys.NAME] = name.trim()
            it[Keys.ONBOARDED] = true
        }
    }

    suspend fun setName(name: String) = context.dataStore.edit { it[Keys.NAME] = name.trim() }
    suspend fun setApiKey(key: String) = context.dataStore.edit { it[Keys.API_KEY] = key.trim() }
    suspend fun setDarkMap(dark: Boolean) = context.dataStore.edit { it[Keys.DARK_MAP] = dark }
}

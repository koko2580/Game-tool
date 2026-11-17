package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.GameProfile
import com.example.domain.model.GameSessionRecord
import com.example.domain.model.TurboPerformanceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.nexoraDataStore: DataStore<Preferences> by preferencesDataStore(name = "nexora_x_prefs")

enum class AppThemeMode(val label: String) {
    DARK_OBSIDIAN("Obsidian Dark (Default)"),
    AMOLED_BLACK("AMOLED True Black"),
    LIGHT_CLEAN("Precision Light")
}

enum class AppLanguageOption(val tag: String, val displayName: String) {
    SYSTEM("system", "System Default"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    GERMAN("de", "Deutsch"),
    FRENCH("fr", "Français")
}

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.DARK_OBSIDIAN,
    val language: AppLanguageOption = AppLanguageOption.SYSTEM,
    val hapticsEnabled: Boolean = true,
    val reducedMotionOverride: Boolean = false,
    val favoriteGamePackages: Set<String> = emptySet(),
    val recentLaunches: Map<String, Long> = emptyMap(),
    val gameProfiles: Map<String, GameProfile> = emptyMap(),
    val turboProfile: TurboPerformanceProfile = TurboPerformanceProfile.BALANCED,
    val activeSession: GameSessionRecord? = null,
    val sessionHistory: List<GameSessionRecord> = emptyList()
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion_override")
        val FAVORITE_GAMES = stringSetPreferencesKey("favorite_game_packages")
        val RECENT_LAUNCHES_JSON = stringPreferencesKey("recent_launches_json")
        val GAME_PROFILES_JSON = stringPreferencesKey("game_profiles_json")
        val TURBO_PROFILE = stringPreferencesKey("turbo_performance_profile")
        val ACTIVE_SESSION_JSON = stringPreferencesKey("active_turbo_session_json")
        val SESSION_HISTORY_JSON = stringPreferencesKey("turbo_session_history_json")
    }

    val preferencesFlow: Flow<UserPreferences> = context.nexoraDataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME_MODE]?.let { raw ->
            AppThemeMode.entries.firstOrNull { it.name == raw }
        } ?: AppThemeMode.DARK_OBSIDIAN

        val lang = prefs[Keys.LANGUAGE_TAG]?.let { tag ->
            AppLanguageOption.entries.firstOrNull { it.tag == tag }
        } ?: AppLanguageOption.SYSTEM

        val turbo = prefs[Keys.TURBO_PROFILE]?.let { raw ->
            TurboPerformanceProfile.entries.firstOrNull { it.name == raw }
        } ?: TurboPerformanceProfile.BALANCED

        UserPreferences(
            themeMode = theme,
            language = lang,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            reducedMotionOverride = prefs[Keys.REDUCED_MOTION] ?: false,
            favoriteGamePackages = prefs[Keys.FAVORITE_GAMES] ?: emptySet(),
            recentLaunches = decodeRecentLaunches(prefs[Keys.RECENT_LAUNCHES_JSON]),
            gameProfiles = decodeGameProfiles(prefs[Keys.GAME_PROFILES_JSON]),
            turboProfile = turbo,
            activeSession = decodeSession(prefs[Keys.ACTIVE_SESSION_JSON]),
            sessionHistory = decodeSessionHistory(prefs[Keys.SESSION_HISTORY_JSON])
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.nexoraDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguageOption) {
        context.nexoraDataStore.edit { it[Keys.LANGUAGE_TAG] = language.tag }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.nexoraDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setReducedMotionOverride(enabled: Boolean) {
        context.nexoraDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setTurboProfile(profile: TurboPerformanceProfile) {
        context.nexoraDataStore.edit { it[Keys.TURBO_PROFILE] = profile.name }
    }

    suspend fun startSession(session: GameSessionRecord) {
        context.nexoraDataStore.edit { prefs ->
            // Close out any prior active session into history before starting the new one
            val previous = decodeSession(prefs[Keys.ACTIVE_SESSION_JSON])
            if (previous != null) {
                val closed = previous.copy(endedAtMillis = System.currentTimeMillis())
                val updatedHistory = (listOf(closed) + decodeSessionHistory(prefs[Keys.SESSION_HISTORY_JSON])).take(20)
                prefs[Keys.SESSION_HISTORY_JSON] = encodeSessionHistory(updatedHistory)
            }
            prefs[Keys.ACTIVE_SESSION_JSON] = encodeSession(session).toString()
        }
    }

    suspend fun endActiveSession(endedAtMillis: Long = System.currentTimeMillis()) {
        context.nexoraDataStore.edit { prefs ->
            val current = decodeSession(prefs[Keys.ACTIVE_SESSION_JSON]) ?: return@edit
            val completed = current.copy(endedAtMillis = endedAtMillis.coerceAtLeast(current.startedAtMillis))
            val updatedHistory = (listOf(completed) + decodeSessionHistory(prefs[Keys.SESSION_HISTORY_JSON])).take(20)
            prefs[Keys.SESSION_HISTORY_JSON] = encodeSessionHistory(updatedHistory)
            prefs.remove(Keys.ACTIVE_SESSION_JSON)
        }
    }

    suspend fun clearSessionHistory() {
        context.nexoraDataStore.edit { prefs ->
            prefs.remove(Keys.SESSION_HISTORY_JSON)
        }
    }

    suspend fun toggleFavoriteGame(packageName: String) {
        context.nexoraDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_GAMES]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(packageName)) {
                current.remove(packageName)
            }
            prefs[Keys.FAVORITE_GAMES] = current
        }
    }

    suspend fun recordGameLaunch(packageName: String, timestampMillis: Long = System.currentTimeMillis()) {
        context.nexoraDataStore.edit { prefs ->
            val existing = decodeRecentLaunches(prefs[Keys.RECENT_LAUNCHES_JSON]).toMutableMap()
            existing[packageName] = timestampMillis
            val trimmed = existing.entries
                .sortedByDescending { it.value }
                .take(12)
                .associate { it.key to it.value }
            prefs[Keys.RECENT_LAUNCHES_JSON] = encodeRecentLaunches(trimmed)
        }
    }

    suspend fun saveGameProfile(profile: GameProfile) {
        context.nexoraDataStore.edit { prefs ->
            val current = decodeGameProfiles(prefs[Keys.GAME_PROFILES_JSON]).toMutableMap()
            current[profile.packageName] = profile
            prefs[Keys.GAME_PROFILES_JSON] = encodeGameProfiles(current)
        }
    }

    private fun decodeRecentLaunches(raw: String?): Map<String, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { key ->
                    put(key, obj.optLong(key, 0L))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun encodeRecentLaunches(map: Map<String, Long>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    private fun decodeGameProfiles(raw: String?): Map<String, GameProfile> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val arr = JSONArray(raw)
            buildMap {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val pkg = item.optString("packageName")
                    if (pkg.isNotBlank()) {
                        val turboName = item.optString("preferredTurboProfile", "")
                        val turbo = TurboPerformanceProfile.entries.firstOrNull { it.name == turboName }
                        put(
                            pkg,
                            GameProfile(
                                packageName = pkg,
                                targetRefreshNote = item.optString("targetRefreshNote", "Auto"),
                                enableDndReminder = item.optBoolean("enableDndReminder", false),
                                brightnessChecklistNote = item.optString("brightnessChecklistNote", ""),
                                preferredTurboProfile = turbo
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun encodeGameProfiles(map: Map<String, GameProfile>): String {
        val arr = JSONArray()
        map.values.forEach { profile ->
            arr.put(
                JSONObject().apply {
                    put("packageName", profile.packageName)
                    put("targetRefreshNote", profile.targetRefreshNote)
                    put("enableDndReminder", profile.enableDndReminder)
                    put("brightnessChecklistNote", profile.brightnessChecklistNote)
                    put("preferredTurboProfile", profile.preferredTurboProfile?.name ?: "")
                }
            )
        }
        return arr.toString()
    }

    private fun encodeSession(session: GameSessionRecord): JSONObject {
        return JSONObject().apply {
            put("id", session.id)
            put("packageName", session.packageName)
            put("appLabel", session.appLabel)
            put("startedAtMillis", session.startedAtMillis)
            if (session.endedAtMillis != null) {
                put("endedAtMillis", session.endedAtMillis)
            }
            put("profileUsed", session.profileUsed.name)
        }
    }

    private fun decodeSession(raw: String?): GameSessionRecord? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val obj = JSONObject(raw)
            val id = obj.optString("id")
            val pkg = obj.optString("packageName")
            val label = obj.optString("appLabel")
            if (id.isBlank() || pkg.isBlank()) return null
            val profile = TurboPerformanceProfile.entries.firstOrNull {
                it.name == obj.optString("profileUsed")
            } ?: TurboPerformanceProfile.BALANCED
            GameSessionRecord(
                id = id,
                packageName = pkg,
                appLabel = label.ifBlank { pkg },
                startedAtMillis = obj.optLong("startedAtMillis", 0L),
                endedAtMillis = if (obj.has("endedAtMillis")) obj.optLong("endedAtMillis") else null,
                profileUsed = profile
            )
        }.getOrNull()
    }

    private fun encodeSessionHistory(list: List<GameSessionRecord>): String {
        val arr = JSONArray()
        list.forEach { arr.put(encodeSession(it)) }
        return arr.toString()
    }

    private fun decodeSessionHistory(raw: String?): List<GameSessionRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    decodeSession(arr.getJSONObject(i).toString())?.let { add(it) }
                }
            }
        }.getOrDefault(emptyList())
    }
}

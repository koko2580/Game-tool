package com.example.data.repository

import com.example.data.preferences.UserPreferencesRepository
import com.example.data.system.InstalledGamesManager
import com.example.domain.model.GameProfile
import com.example.domain.model.InstalledAppItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class GamesRepository(
    private val installedGamesManager: InstalledGamesManager,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val rawApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    private val isLoaded = MutableStateFlow(false)

    val appsWithPrefsFlow: Flow<Pair<Boolean, List<InstalledAppItem>>> = combine(
        isLoaded,
        rawApps,
        userPreferencesRepository.preferencesFlow
    ) { loaded, apps, prefs ->
        val merged = apps.map { app ->
            app.copy(
                isFavorite = app.packageName in prefs.favoriteGamePackages,
                lastLaunchedMillis = prefs.recentLaunches[app.packageName],
                profile = prefs.gameProfiles[app.packageName]
            )
        }
        loaded to merged
    }

    suspend fun reloadInstalledApps() {
        val queried = installedGamesManager.queryLaunchableApps()
        rawApps.value = queried
        isLoaded.value = true
    }

    suspend fun launchApp(packageName: String): Boolean {
        val launched = installedGamesManager.launchApp(packageName)
        if (launched) {
            userPreferencesRepository.recordGameLaunch(packageName)
        }
        return launched
    }

    suspend fun toggleFavorite(packageName: String) {
        userPreferencesRepository.toggleFavoriteGame(packageName)
    }

    suspend fun saveProfile(profile: GameProfile) {
        userPreferencesRepository.saveGameProfile(profile)
    }
}

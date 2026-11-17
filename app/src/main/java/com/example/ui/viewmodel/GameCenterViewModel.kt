package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.GamesRepository
import com.example.domain.model.GameCenterState
import com.example.domain.model.GameFilter
import com.example.domain.model.GameProfile
import com.example.domain.model.InstalledAppItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameCenterViewModel(
    private val gamesRepository: GamesRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(GameFilter.DETECTED_GAMES)
    private val statusMessage = MutableStateFlow<String?>(null)

    init {
        refreshInstalledApps()
    }

    val uiState: StateFlow<GameCenterState> = combine(
        gamesRepository.appsWithPrefsFlow,
        searchQuery,
        selectedFilter,
        statusMessage
    ) { (isLoaded, allApps), query, filter, message ->
        val filtered = filterAndSearchApps(allApps, query, filter)
        val recents = allApps
            .filter { it.lastLaunchedMillis != null }
            .sortedByDescending { it.lastLaunchedMillis }
            .take(5)

        GameCenterState(
            apps = filtered,
            recentApps = recents,
            searchQuery = query,
            selectedFilter = filter,
            isLoading = !isLoaded,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = GameCenterState()
    )

    fun refreshInstalledApps() {
        viewModelScope.launch {
            gamesRepository.reloadInstalledApps()
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onFilterSelected(filter: GameFilter) {
        selectedFilter.value = filter
    }

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            gamesRepository.toggleFavorite(packageName)
        }
    }

    fun launchGame(item: InstalledAppItem) {
        viewModelScope.launch {
            val ok = gamesRepository.launchApp(item.packageName)
            statusMessage.value = if (ok) {
                val reminder = if (item.profile?.enableDndReminder == true) {
                    " (Reminder: enable Do Not Disturb)"
                } else ""
                "Launching ${item.label}$reminder"
            } else {
                "Unable to launch ${item.label} on this device."
            }
        }
    }

    fun saveGameProfile(profile: GameProfile) {
        viewModelScope.launch {
            gamesRepository.saveProfile(profile)
            statusMessage.value = "Saved pre-launch profile."
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    companion object {
        fun filterAndSearchApps(
            allApps: List<InstalledAppItem>,
            query: String,
            filter: GameFilter
        ): List<InstalledAppItem> {
            val normalizedQuery = query.trim().lowercase()
            return allApps
                .asSequence()
                .filter { app ->
                    when (filter) {
                        GameFilter.DETECTED_GAMES -> app.isAndroidGameCategory || app.isFavorite
                        GameFilter.FAVORITES -> app.isFavorite
                        GameFilter.ALL_APPS -> true
                    }
                }
                .filter { app ->
                    normalizedQuery.isBlank() ||
                        app.label.lowercase().contains(normalizedQuery) ||
                        app.packageName.lowercase().contains(normalizedQuery)
                }
                .sortedWith(
                    compareByDescending<InstalledAppItem> { it.isFavorite }
                        .thenByDescending { it.isAndroidGameCategory }
                        .thenBy { it.label.lowercase() }
                )
                .toList()
        }
    }

    class Factory(private val repository: GamesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameCenterViewModel(repository) as T
        }
    }
}

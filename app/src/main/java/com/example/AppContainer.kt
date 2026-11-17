package com.example

import android.content.Context
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.GamesRepository
import com.example.data.repository.TelemetryRepository
import com.example.data.system.DeviceTelemetryManager
import com.example.data.system.DndController
import com.example.data.system.InstalledGamesManager
import com.example.data.system.NetworkTelemetryManager
import com.example.data.system.SystemSettingsNavigator
import com.example.turbo.MediaCaptureController

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    val deviceTelemetryManager: DeviceTelemetryManager by lazy {
        DeviceTelemetryManager(appContext)
    }

    val networkTelemetryManager: NetworkTelemetryManager by lazy {
        NetworkTelemetryManager(appContext)
    }

    val installedGamesManager: InstalledGamesManager by lazy {
        InstalledGamesManager(appContext)
    }

    val systemSettingsNavigator: SystemSettingsNavigator by lazy {
        SystemSettingsNavigator(appContext)
    }

    val dndController: DndController by lazy {
        DndController(appContext)
    }

    val mediaCaptureController: MediaCaptureController by lazy {
        MediaCaptureController(appContext)
    }

    val telemetryRepository: TelemetryRepository by lazy {
        TelemetryRepository(
            deviceTelemetryManager = deviceTelemetryManager,
            networkTelemetryManager = networkTelemetryManager,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    val gamesRepository: GamesRepository by lazy {
        GamesRepository(
            installedGamesManager = installedGamesManager,
            userPreferencesRepository = userPreferencesRepository
        )
    }
}

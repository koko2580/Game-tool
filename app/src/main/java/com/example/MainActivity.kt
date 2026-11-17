package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.AppLanguageOption
import com.example.ui.components.ProvideMotionAndHaptics
import com.example.ui.navigation.NexoraRootApp
import com.example.ui.theme.NexoraTheme
import com.example.ui.viewmodel.MainViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as NexoraApplication).container

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(appContainer.userPreferencesRepository)
            )
            val prefs by mainViewModel.userPreferences.collectAsStateWithLifecycle()

            val baseContext = LocalContext.current
            val baseConfig = LocalConfiguration.current
            val localizedContext = remember(prefs.language, baseContext, baseConfig) {
                if (prefs.language == AppLanguageOption.SYSTEM) {
                    baseContext
                } else {
                    val locale = Locale.forLanguageTag(prefs.language.tag)
                    val config = Configuration(baseConfig).apply {
                        setLocale(locale)
                    }
                    baseContext.createConfigurationContext(config)
                }
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                NexoraTheme(themeMode = prefs.themeMode) {
                    ProvideMotionAndHaptics(
                        userReducedMotionOverride = prefs.reducedMotionOverride || prefs.turboProfile.reduceVisualEffects,
                        userHapticsEnabled = prefs.hapticsEnabled
                    ) {
                        NexoraRootApp(appContainer = appContainer)
                    }
                }
            }
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.preferences.BrowserPreferences
import com.example.ui.screens.MainScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.ZallBackground
import com.example.ui.theme.ZallBrowserTheme

enum class AppScreen {
    SPLASH,
    MAIN,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var preferences: BrowserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferences = BrowserPreferences(this)

        setContent {
            ZallBrowserTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ZallBackground
                ) {
                    AppNavigation(preferences)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(preferences: BrowserPreferences) {
    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            AppScreen.SPLASH -> {
                SplashScreen(
                    onSplashFinished = {
                        currentScreen = AppScreen.MAIN
                    }
                )
            }

            AppScreen.MAIN -> {
                MainScreen(
                    preferences = preferences,
                    onNavigateToSettings = {
                        currentScreen = AppScreen.SETTINGS
                    }
                )
            }

            AppScreen.SETTINGS -> {
                SettingsScreen(
                    preferences = preferences,
                    onNavigateBack = {
                        currentScreen = AppScreen.MAIN
                    }
                )
            }
        }
    }
}

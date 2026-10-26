package com.aliayali.distortion

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aliayali.designsystem.theme.DistortionTheme
import com.aliayali.distortion.ui.DistortionApp
import com.aliayali.distortion.ui.rememberDistortionAppState

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install before super.onCreate so the system splash is shown without a blank frame.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Landscape, edge-to-edge, immersive presentation for the horror-game experience.
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        setContent {
            DistortionTheme {
                val appState = rememberDistortionAppState()
                DistortionApp(appState = appState)
            }
        }
    }
}

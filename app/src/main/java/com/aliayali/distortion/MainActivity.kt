package com.aliayali.distortion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aliayali.designsystem.theme.DistortionTheme
import com.aliayali.distortion.ui.DistortionApp
import com.aliayali.distortion.ui.rememberDistortionAppState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DistortionTheme {
                val appState = rememberDistortionAppState()
                DistortionApp(appState = appState)
            }
        }
    }
}
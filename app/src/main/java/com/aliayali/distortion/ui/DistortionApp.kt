package com.aliayali.distortion.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.aliayali.game.navigation.gameEntry
import com.aliayali.home.navigation.homeEntry
import com.aliayali.navigation.Navigator
import com.aliayali.navigation.toEntries

@Composable
fun DistortionApp(
    appState: DistortionAppState,
    modifier: Modifier = Modifier,
) {
    val navigator = remember(appState.navigationState) {
        Navigator(appState.navigationState)
    }

    val appEntryProvider = entryProvider {
        homeEntry(navigator)
        gameEntry(navigator)
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            NavDisplay(
                entries = appState.navigationState.toEntries(appEntryProvider),
                onBack = {
                    navigator.goBack()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
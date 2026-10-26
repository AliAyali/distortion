package com.aliayali.distortion.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.aliayali.distortion.navigation.TOP_LEVEL_NAV_ITEMS
import com.aliayali.home.navigation.HomeNavKey
import com.aliayali.navigation.NavigationState
import com.aliayali.navigation.rememberNavigationState

class DistortionAppState(
    val navigationState: NavigationState,
)

@Composable
fun rememberDistortionAppState(): DistortionAppState {

    val navigationState = rememberNavigationState(
        startKey = HomeNavKey,
        topLevelKeys = TOP_LEVEL_NAV_ITEMS.keys,
    )

    return remember {
        DistortionAppState(
            navigationState = navigationState,
        )
    }
}
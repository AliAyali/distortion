package com.aliayali.game.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.aliayali.game.GameRoute
import com.aliayali.navigation.Navigator

fun EntryProviderScope<NavKey>.gameEntry(
    navigator: Navigator
) {
    entry<GameNavKey> {
        GameRoute(
            onBackToHome = { navigator.goBack() },
        )
    }
}
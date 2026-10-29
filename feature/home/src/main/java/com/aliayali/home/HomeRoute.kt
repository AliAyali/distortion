package com.aliayali.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aliayali.game.persistence.GameSaveStore

@Composable
fun HomeRoute(
    onNewGame: () -> Unit,
    onContinueGame: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val saveStore = remember(context) { GameSaveStore(context.applicationContext) }
    var hasSavedGame by remember { mutableStateOf(saveStore.hasSave()) }
    val uiState = homeViewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner, saveStore) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasSavedGame = saveStore.hasSave()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.value.isStartingGame) {
        if (uiState.value.isStartingGame) {
            // Starting a fresh run deliberately replaces the existing save slot.
            saveStore.clear()
            homeViewModel.onGameNavigationHandled()
            onNewGame()
        }
    }

    HomeScreen(
        onNewGameClick = homeViewModel::onNewGameClick,
        onContinueClick = onContinueGame,
        hasSavedGame = hasSavedGame,
    )
}

package com.aliayali.game

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
import com.aliayali.game.settings.GameSettingsStore
import kotlinx.coroutines.delay

@Composable
fun GameRoute(
    onBackToHome: () -> Unit = {},
    gameViewModel: GameViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val saveStore = remember(context) { GameSaveStore(context.applicationContext) }
    val settingsStore = remember(context) { GameSettingsStore(context.applicationContext) }
    var gameSettings by remember(settingsStore) { mutableStateOf(settingsStore.load()) }

    DisposableEffect(lifecycleOwner, settingsStore) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) gameSettings = settingsStore.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val uiState = gameViewModel.uiState.collectAsStateWithLifecycle()
    val onAction: (com.aliayali.game.model.GameAction) -> Unit = remember(gameViewModel) { gameViewModel::onAction }
    val onMoveStick: (Float, Float) -> Unit = remember(gameViewModel) { { x, y -> gameViewModel.moveByStick(x, y) } }
    val onLookDelta: (Float, Float) -> Unit = remember(gameViewModel, gameSettings.lookSensitivity) {
        { x, y -> gameViewModel.lookBy(x, y, gameSettings.lookSensitivity) }
    }
    var restorationFinished by remember { mutableStateOf(false) }

    LaunchedEffect(gameViewModel, saveStore) {
        gameViewModel.restoreState(saveStore.load())
        restorationFinished = true
    }

    DisposableEffect(gameViewModel, saveStore) {
        onDispose { saveStore.save(gameViewModel.uiState.value) }
    }

    // Debounce movement saves so the analog stick doesn't write to disk on every tiny step.
    LaunchedEffect(uiState.value, restorationFinished) {
        if (restorationFinished) {
            delay(420)
            saveStore.save(uiState.value)
        }
    }

    GameScreen(
        uiState = uiState.value,
        onAction = onAction,
        onMoveStick = onMoveStick,
        onLookDelta = onLookDelta,
        onBackToHome = onBackToHome,
    )
}

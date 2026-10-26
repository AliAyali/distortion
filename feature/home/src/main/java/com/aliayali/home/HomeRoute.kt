package com.aliayali.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeRoute(
    onNewGame: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
) {
    val uiState = homeViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.value.isStartingGame) {
        if (uiState.value.isStartingGame) {
            homeViewModel.onGameNavigationHandled()
            onNewGame()
        }
    }

    HomeScreen(
        onNewGameClick = homeViewModel::onNewGameClick,
    )
}
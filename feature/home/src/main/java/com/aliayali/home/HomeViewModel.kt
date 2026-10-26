package com.aliayali.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    fun onNewGameClick() {
        _uiState.update {
            it.copy(isStartingGame = true)
        }
    }

    fun onGameNavigationHandled() {
        _uiState.update {
            it.copy(isStartingGame = false)
        }
    }
}
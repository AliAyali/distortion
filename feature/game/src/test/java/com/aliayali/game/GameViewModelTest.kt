package com.aliayali.game

import com.aliayali.game.model.GameAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewModelTest {

    @Test
    fun `movement updates player position after intro is dismissed`() {
        val viewModel = GameViewModel()
        viewModel.onAction(GameAction.ContinueDialogue)
        val oldY = viewModel.uiState.value.playerY

        viewModel.onAction(GameAction.MoveNorth)

        assertTrue(viewModel.uiState.value.playerY < oldY)
    }

    @Test
    fun `legacy scene tap moves a short step without teleporting`() {
        val viewModel = GameViewModel()
        viewModel.onAction(GameAction.ContinueDialogue)
        val before = viewModel.uiState.value

        viewModel.onSceneTap(36f, 56f)

        val after = viewModel.uiState.value
        assertTrue(before.playerY != after.playerY)
        assertTrue(kotlin.math.abs(after.playerY - before.playerY) <= 7f)
    }

    @Test
    fun `movement follows the first person camera direction`() {
        val viewModel = GameViewModel()
        viewModel.onAction(GameAction.ContinueDialogue)
        viewModel.lookBy(281.25f, 0f) // 90 degrees at 0.32 degrees per pixel.
        val before = viewModel.uiState.value.playerX

        viewModel.moveByStick(0f, -1f, 2f)

        assertTrue(viewModel.uiState.value.playerX > before)
    }

    @Test
    fun `sound can be toggled independently from flashlight`() {
        val viewModel = GameViewModel()
        val flashlightBefore = viewModel.uiState.value.isFlashlightOn
        viewModel.onAction(GameAction.ToggleSound)

        assertEquals(false, viewModel.uiState.value.isSoundOn)
        assertEquals(flashlightBefore, viewModel.uiState.value.isFlashlightOn)
    }

    @Test
    fun `movement stops at the living room coffee table`() {
        val viewModel = GameViewModel()
        viewModel.onAction(GameAction.ContinueDialogue)

        repeat(20) { viewModel.moveByStick(0f, -1f, 1f) }

        // The table occupies the lane ahead of the spawn point; the player must go around it.
        assertTrue(viewModel.uiState.value.playerY > 40f)
    }

    @Test
    fun `seven nights have a designated anomaly`() {
        assertEquals(7, NIGHT_ANOMALIES.size)
        assertEquals("mirror", anomalyForNight(1).id)
        assertEquals("closet", anomalyForNight(7).id)
    }
}

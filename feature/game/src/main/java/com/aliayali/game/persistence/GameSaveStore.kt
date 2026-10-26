package com.aliayali.game.persistence

import android.content.Context
import com.aliayali.game.GameUiState
import com.aliayali.game.model.AudioCue

/** Small local save slot. Uses SharedPreferences to avoid adding a database just for one save. */
class GameSaveStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("the_distortion_save_v1", Context.MODE_PRIVATE)

    fun hasSave(): Boolean = preferences.getBoolean(KEY_HAS_SAVE, false)

    fun clear() {
        preferences.edit().clear().apply()
    }

    fun save(state: GameUiState) {
        preferences.edit()
            .putBoolean(KEY_HAS_SAVE, true)
            .putInt("night", state.night)
            .putInt("stress", state.stress)
            .putFloat("player_x", state.playerX)
            .putFloat("player_y", state.playerY)
            .putFloat("camera_yaw", state.cameraYaw)
            .putFloat("camera_pitch", state.cameraPitch)
            .putBoolean("anomaly_detected", state.anomalyDetected)
            .putBoolean("flashlight_on", state.isFlashlightOn)
            .putBoolean("sound_on", state.isSoundOn)
            .putBoolean("game_over", state.gameOver)
            .putString("ending_title", state.endingTitle)
            .putString("dialogue_title", state.dialogueTitle)
            .putString("dialogue", state.dialogue)
            .putString("selected_object", state.selectedObject)
            .putString("nearby_object", state.nearbyObjectId)
            .putStringSet("inspected_objects", state.inspectedObjectIds.toSet())
            .putStringSet("inspected_this_night", state.inspectedThisNight.toSet())
            .putStringSet("clues_found", state.cluesFound.toSet())
            .apply()
    }

    fun load(): GameUiState? {
        if (!hasSave()) return null
        val defaults = GameUiState()
        return defaults.copy(
            night = preferences.getInt("night", defaults.night).coerceIn(1, 7),
            stress = preferences.getInt("stress", defaults.stress).coerceIn(0, 100),
            playerX = preferences.getFloat("player_x", defaults.playerX),
            playerY = preferences.getFloat("player_y", defaults.playerY),
            cameraYaw = preferences.getFloat("camera_yaw", defaults.cameraYaw),
            cameraPitch = preferences.getFloat("camera_pitch", defaults.cameraPitch).coerceIn(-8f, 8f),
            anomalyDetected = preferences.getBoolean("anomaly_detected", false),
            isFlashlightOn = preferences.getBoolean("flashlight_on", true),
            isSoundOn = preferences.getBoolean("sound_on", true),
            gameOver = preferences.getBoolean("game_over", false),
            endingTitle = preferences.getString("ending_title", "").orEmpty(),
            dialogueTitle = preferences.getString("dialogue_title", defaults.dialogueTitle).orEmpty(),
            dialogue = preferences.getString("dialogue", defaults.dialogue).orEmpty(),
            selectedObject = preferences.getString("selected_object", null),
            nearbyObjectId = preferences.getString("nearby_object", null),
            inspectedObjectIds = preferences.getStringSet("inspected_objects", emptySet())?.toSet() ?: emptySet(),
            inspectedThisNight = preferences.getStringSet("inspected_this_night", emptySet())?.toSet() ?: emptySet(),
            cluesFound = preferences.getStringSet("clues_found", emptySet())?.toSet() ?: emptySet(),
            showDialogue = false,
            journalOpen = false,
            audioCue = AudioCue.None,
            audioCueId = 0,
        )
    }

    private companion object {
        const val KEY_HAS_SAVE = "has_save"
    }
}

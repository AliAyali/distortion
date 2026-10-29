package com.aliayali.game.settings

import android.content.Context

/** Persistent player-facing settings shared by the title screen and gameplay. */
data class GameSettings(
    val musicEnabled: Boolean = true,
    val effectsEnabled: Boolean = true,
    val musicVolume: Float = 0.48f,
    val effectsVolume: Float = 0.82f,
    val lookSensitivity: Float = 0.32f,
    val reduceFlashes: Boolean = false,
)

class GameSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): GameSettings = GameSettings(
        musicEnabled = preferences.getBoolean(KEY_MUSIC_ENABLED, true),
        effectsEnabled = preferences.getBoolean(KEY_EFFECTS_ENABLED, true),
        musicVolume = preferences.getFloat(KEY_MUSIC_VOLUME, 0.48f).coerceIn(0f, 1f),
        effectsVolume = preferences.getFloat(KEY_EFFECTS_VOLUME, 0.82f).coerceIn(0f, 1f),
        lookSensitivity = preferences.getFloat(KEY_LOOK_SENSITIVITY, 0.32f).coerceIn(0.14f, 0.62f),
        reduceFlashes = preferences.getBoolean(KEY_REDUCE_FLASHES, false),
    )

    fun save(settings: GameSettings) {
        preferences.edit()
            .putBoolean(KEY_MUSIC_ENABLED, settings.musicEnabled)
            .putBoolean(KEY_EFFECTS_ENABLED, settings.effectsEnabled)
            .putFloat(KEY_MUSIC_VOLUME, settings.musicVolume.coerceIn(0f, 1f))
            .putFloat(KEY_EFFECTS_VOLUME, settings.effectsVolume.coerceIn(0f, 1f))
            .putFloat(KEY_LOOK_SENSITIVITY, settings.lookSensitivity.coerceIn(0.14f, 0.62f))
            .putBoolean(KEY_REDUCE_FLASHES, settings.reduceFlashes)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "the_distortion_player_settings"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_EFFECTS_ENABLED = "effects_enabled"
        private const val KEY_MUSIC_VOLUME = "music_volume"
        private const val KEY_EFFECTS_VOLUME = "effects_volume"
        private const val KEY_LOOK_SENSITIVITY = "look_sensitivity"
        private const val KEY_REDUCE_FLASHES = "reduce_flashes"
    }
}

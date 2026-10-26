package com.aliayali.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.aliayali.game.R
import com.aliayali.game.model.AudioCue
import com.aliayali.game.settings.GameSettings

/** Procedural, original audio assets live in res/raw and can later be replaced by recorded sounds. */
class HorrorAudioManager(context: Context, initialSettings: GameSettings = GameSettings()) : AutoCloseable {
    private val appContext = context.applicationContext
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundIds = mutableMapOf<AudioCue, Int>()
    private val loadedSounds = mutableSetOf<Int>()
    private var enabled = true
    private var settings: GameSettings = initialSettings
    private var ambient: MediaPlayer? = MediaPlayer.create(appContext, R.raw.ambient_drone)?.apply {
        isLooping = true
        setVolume(0.42f * initialSettings.musicVolume, 0.42f * initialSettings.musicVolume)
        setOnErrorListener { _, _, _ -> true }
    }

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedSounds += sampleId
        }
        soundIds[AudioCue.Footstep] = soundPool.load(appContext, R.raw.footstep, 1)
        soundIds[AudioCue.Interaction] = soundPool.load(appContext, R.raw.interaction_click, 1)
        soundIds[AudioCue.FlashlightClick] = soundPool.load(appContext, R.raw.flashlight_click, 1)
        soundIds[AudioCue.Paper] = soundPool.load(appContext, R.raw.paper_rustle, 1)
        soundIds[AudioCue.MirrorSting] = soundPool.load(appContext, R.raw.mirror_sting, 1)
        soundIds[AudioCue.DoorCreak] = soundPool.load(appContext, R.raw.door_creak, 1)
        soundIds[AudioCue.LampHum] = soundPool.load(appContext, R.raw.lamp_hum, 1)
        soundIds[AudioCue.WindowWind] = soundPool.load(appContext, R.raw.window_wind, 1)
        soundIds[AudioCue.RadioStatic] = soundPool.load(appContext, R.raw.radio_static, 1)
        soundIds[AudioCue.ClockTick] = soundPool.load(appContext, R.raw.clock_tick, 1)
        soundIds[AudioCue.PhotoWhisper] = soundPool.load(appContext, R.raw.photo_whisper, 1)
        soundIds[AudioCue.PhoneVibration] = soundPool.load(appContext, R.raw.phone_vibration, 1)
        soundIds[AudioCue.ClosetCreak] = soundPool.load(appContext, R.raw.closet_creak, 1)
        soundIds[AudioCue.BedCreak] = soundPool.load(appContext, R.raw.bed_creak, 1)
        soundIds[AudioCue.AnomalyGlitch] = soundPool.load(appContext, R.raw.anomaly_glitch, 1)
        soundIds[AudioCue.NightTransition] = soundPool.load(appContext, R.raw.night_transition, 1)
        applyMusicState()
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        applyMusicState()
    }

    fun updateSettings(value: GameSettings) {
        settings = value
        ambient?.setVolume(0.42f * settings.musicVolume, 0.42f * settings.musicVolume)
        applyMusicState()
    }

    fun play(cue: AudioCue) {
        if (!enabled || !settings.effectsEnabled || settings.effectsVolume <= 0f || cue == AudioCue.None) return
        val id = soundIds[cue] ?: return
        if (id !in loadedSounds) return
        val volume = when (cue) {
            AudioCue.Footstep -> 0.30f
            AudioCue.Interaction, AudioCue.FlashlightClick -> 0.50f
            AudioCue.Paper -> 0.35f
            AudioCue.MirrorSting -> 0.85f
            AudioCue.DoorCreak, AudioCue.ClosetCreak, AudioCue.BedCreak -> 0.68f
            AudioCue.LampHum -> 0.42f
            AudioCue.WindowWind, AudioCue.PhotoWhisper -> 0.62f
            AudioCue.RadioStatic -> 0.72f
            AudioCue.ClockTick, AudioCue.PhoneVibration -> 0.55f
            AudioCue.AnomalyGlitch -> 0.78f
            AudioCue.NightTransition -> 0.62f
            AudioCue.None -> 0f
        }
        val gain = (volume * settings.effectsVolume).coerceIn(0f, 1f)
        soundPool.play(id, gain, gain, 1, 0, 1f)
    }

    private fun applyMusicState() {
        if (enabled && settings.musicEnabled && settings.musicVolume > 0.001f) {
            ambient?.setVolume(0.42f * settings.musicVolume, 0.42f * settings.musicVolume)
            startAmbient()
        } else {
            stopAmbient()
        }
    }

    private fun startAmbient() {
        try {
            ambient?.let { if (!it.isPlaying) it.start() }
        } catch (_: IllegalStateException) {
            // A device may have reclaimed the audio session; SFX can still run.
        }
    }

    private fun stopAmbient() {
        try { if (ambient?.isPlaying == true) ambient?.pause() } catch (_: IllegalStateException) { }
    }

    override fun close() {
        try { ambient?.stop() } catch (_: Throwable) { }
        try { ambient?.release() } catch (_: Throwable) { }
        ambient = null
        soundPool.release()
    }
}

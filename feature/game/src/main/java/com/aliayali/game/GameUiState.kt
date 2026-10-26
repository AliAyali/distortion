package com.aliayali.game

import com.aliayali.game.model.ApartmentObject
import com.aliayali.game.model.AudioCue

data class GameUiState(
    val night: Int = 1,
    val stress: Int = 8,
    val dialogueTitle: String = "شب اول — ۰۰:۱۳",
    val dialogue: String = "ساعت از نیمه‌شب گذشته. برق راهرو چند بار لرزیده و حالا سکوت، آپارتمان را بلعیده است. چیزی در این خانه سر جایش نیست. با جوی‌استیک حرکت کن، با کشیدن انگشت به اطراف نگاه کن و اشیای مشکوک را بررسی کن.",
    val interactionHint: String = "جوی‌استیک پایین چپ: حرکت • کشیدن سمت راست: نگاه • تعامل: بررسی شیء روبه‌رو",
    val selectedObject: String? = null,
    val nearbyObjectId: String? = null,
    val playerX: Float = 36f,
    val playerY: Float = 47f,
    /** Degrees; zero looks toward the top/north edge of the apartment map. */
    val cameraYaw: Float = 0f,
    /** Small vertical look offset, clamped in the ViewModel. */
    val cameraPitch: Float = 0f,
    val showDialogue: Boolean = true,
    val anomalyDetected: Boolean = false,
    val isFlashlightOn: Boolean = true,
    val isSoundOn: Boolean = true,
    val journalOpen: Boolean = false,
    val inspectedObjectIds: Set<String> = emptySet(),
    val inspectedThisNight: Set<String> = emptySet(),
    val cluesFound: Set<String> = emptySet(),
    val gameOver: Boolean = false,
    val endingTitle: String = "",
    val audioCue: AudioCue = AudioCue.None,
    val audioCueId: Int = 0,
)

val NIGHT_ANOMALIES: List<ApartmentObject> = listOf(
    ApartmentObject.Mirror,
    ApartmentObject.FamilyPhoto,
    ApartmentObject.Radio,
    ApartmentObject.FrontDoor,
    ApartmentObject.Phone,
    ApartmentObject.Window,
    ApartmentObject.Closet,
)

fun anomalyForNight(night: Int): ApartmentObject =
    NIGHT_ANOMALIES[(night - 1).coerceIn(0, NIGHT_ANOMALIES.lastIndex)]

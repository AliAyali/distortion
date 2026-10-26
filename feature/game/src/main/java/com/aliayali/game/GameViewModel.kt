package com.aliayali.game

import androidx.lifecycle.ViewModel
import com.aliayali.game.model.ApartmentObject
import com.aliayali.game.model.AudioCue
import com.aliayali.game.model.GameAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()
    private var movementTicks = 0

    fun onAction(action: GameAction) {
        when (action) {
            GameAction.MoveNorth -> moveByStick(0f, -1f, 2.1f)
            GameAction.MoveSouth -> moveByStick(0f, 1f, 2.1f)
            GameAction.MoveWest -> moveByStick(-1f, 0f, 2.1f)
            GameAction.MoveEast -> moveByStick(1f, 0f, 2.1f)
            GameAction.Interact -> interactWithNearbyObject()
            GameAction.ToggleFlashlight -> toggleFlashlight()
            GameAction.ToggleSound -> toggleSound()
            GameAction.ToggleJournal -> toggleJournal()
            GameAction.EndNight -> endNight()
            GameAction.ContinueDialogue -> continueDialogue()
            GameAction.IgnoreAnomaly -> ignoreAnomaly()
            GameAction.RestartGame -> restartGame()
        }
    }

    /** Analog virtual stick. stickY is negative when the thumb is pushed forward. */
    fun moveByStick(stickX: Float, stickY: Float, speed: Float = 0.92f) {
        val state = _uiState.value
        if (state.gameOver || state.showDialogue || state.journalOpen) return
        val length = hypot(stickX, stickY).coerceAtMost(1f)
        if (length < 0.08f) return
        val yaw = Math.toRadians(state.cameraYaw.toDouble()).toFloat()
        val strafe = stickX.coerceIn(-1f, 1f) * speed
        val forward = -stickY.coerceIn(-1f, 1f) * speed
        val dx = (sin(yaw) * forward + cos(yaw) * strafe) * length
        val dy = (-cos(yaw) * forward + sin(yaw) * strafe) * length
        movePlayer(dx, dy)
    }

    /** Right-side swipe camera control. Horizontal movement rotates; vertical movement tilts. */
    fun lookBy(deltaX: Float, deltaY: Float, sensitivity: Float = 0.32f) {
        _uiState.update { state ->
            if (state.gameOver || state.showDialogue || state.journalOpen) return@update state
            val look = sensitivity.coerceIn(0.14f, 0.62f)
            val yaw = ((state.cameraYaw + deltaX * look) % 360f + 360f) % 360f
            val pitch = (state.cameraPitch + deltaY * (0.045f * look / 0.32f)).coerceIn(-8f, 8f)
            val target = findLookTarget(state.playerX, state.playerY, yaw)
            state.copy(
                cameraYaw = yaw,
                cameraPitch = pitch,
                nearbyObjectId = target?.id,
                interactionHint = target?.let { "${it.title} روبه‌روست؛ برای بررسی دکمه‌ی تعامل را بزن." }
                    ?: "به اطراف نگاه کن و با جوی‌استیک در آپارتمان حرکت کن.",
            )
        }
    }

    /** Kept as a compatibility entry point for older scene-tap callers. */
    fun onSceneTap(x: Float, y: Float) {
        val state = _uiState.value
        if (state.gameOver || state.journalOpen || state.showDialogue) return
        val target = ApartmentObject.entries.minByOrNull { distance(x, y, it.x, it.y) }
        if (target != null && distance(x, y, target.x, target.y) <= 9f) {
            val dx = target.x - state.playerX
            val dy = target.y - state.playerY
            lookBy(dx * 0.25f, dy * 0.1f)
            if (distance(state.playerX, state.playerY, target.x, target.y) < 19f) inspectObject(target)
        } else {
            moveByStick(0f, -1f, 1.0f)
        }
    }

    /** Restores a saved session. The route calls this before enabling autosave. */
    fun restoreState(saved: GameUiState?) {
        if (saved == null) {
            _uiState.value = GameUiState()
            movementTicks = 0
            return
        }
        val restored = saved.copy(
            showDialogue = true,
            journalOpen = false,
            dialogueTitle = if (saved.gameOver) saved.endingTitle else "بازی بازیابی شد",
            dialogue = if (saved.gameOver) saved.dialogue else "به آپارتمان برگشتی. خانه همه‌چیز را به خاطر دارد. برای ادامه، آماده شو.",
            interactionHint = "پیشرفت قبلی بازیابی شد.",
            nearbyObjectId = findLookTarget(saved.playerX, saved.playerY, saved.cameraYaw)?.id,
        )
        _uiState.value = restored
    }

    private fun movePlayer(dx: Float, dy: Float) {
        val state = _uiState.value
        val nextX = (state.playerX + dx).coerceIn(5f, 96f)
        val nextY = (state.playerY + dy).coerceIn(5f, 96f)
        val next = findWalkableStep(state.playerX, state.playerY, nextX, nextY)
        if (next == null) {
            _uiState.update { it.copy(interactionHint = "جلوت دیوار است. مسیر دیگری را امتحان کن.") }
            return
        }
        applyMovement(next.first, next.second)
        movementTicks++
        if (movementTicks % 6 == 0) emitCue(AudioCue.Footstep)
    }

    private fun applyMovement(x: Float, y: Float, customHint: String = "") {
        _uiState.update { state ->
            val target = findLookTarget(x, y, state.cameraYaw)
            state.copy(
                playerX = x,
                playerY = y,
                nearbyObjectId = target?.id,
                interactionHint = customHint.ifBlank {
                    target?.let { "${it.title} روبه‌روست؛ برای بررسی دکمه‌ی تعامل را بزن." }
                        ?: "جوی‌استیک را حرکت بده؛ برای نگاه‌کردن صفحه را به چپ یا راست بکش."
                },
            )
        }
    }

    private fun findWalkableStep(fromX: Float, fromY: Float, toX: Float, toY: Float): Pair<Float, Float>? {
        if (isWalkable(toX, toY) && !blockedByFurniture(toX, toY)) return toX to toY
        // Axis-separated collision allows the player to slide along a sofa or cabinet edge.
        if (isWalkable(toX, fromY) && !blockedByFurniture(toX, fromY)) return toX to fromY
        if (isWalkable(fromX, toY) && !blockedByFurniture(fromX, toY)) return fromX to toY
        return null
    }

    /** Coarse furniture collision matches the fixed props rendered into the room. */
    private fun blockedByFurniture(x: Float, y: Float): Boolean =
        (x in 16f..28.5f && y in 37.5f..44.2f) || // sofa
            (x in 30f..40.5f && y in 34.8f..40f) || // coffee table
            (x in 9f..14.2f && y in 23f..39f) || // bookcase against left wall
            (x in 43.5f..52.5f && y in 24.5f..30.7f) || // TV stand
            (x in 53f..59.5f && y in 47.5f..53.5f) || // hall sideboard
            (x in 56.5f..59.8f && y in 40.5f..45.5f) || // standing mirror in the passage
            (x in 77.5f..88.5f && y in 31f..37.8f) || // dining table
            (x in 81.5f..91f && y in 19f..24.5f) || // kitchen counter
            (x in 90f..94f && y in 24f..34.5f) || // refrigerator
            (x in 23.5f..29.5f && y in 73.5f..78.5f) || // bedside table
            (x in 13f..24.5f && y in 76f..86f) || // bed
            (x in 39.5f..48.5f && y in 85f..91f) // closet

    private fun interactWithNearbyObject() {
        val state = _uiState.value
        val target = ApartmentObject.fromId(state.nearbyObjectId)
        if (target == null) {
            _uiState.update { it.copy(interactionHint = "اول روبه‌روی یک شیء بایست و کمی نزدیک‌تر شو.") }
            return
        }
        val distance = distance(state.playerX, state.playerY, target.x, target.y)
        if (distance > 26f || !hasLineOfSight(state.playerX, state.playerY, target.x, target.y)) {
            _uiState.update { it.copy(interactionHint = "هنوز به ${target.title} نزدیک نیستی یا چیزی جلوی دیدت را گرفته است.") }
            return
        }
        inspectObject(target)
    }

    private fun inspectObject(target: ApartmentObject) {
        val current = _uiState.value
        val isAnomaly = target == anomalyForNight(current.night)
        val newlyFound = isAnomaly && !current.anomalyDetected
        val wasInspected = target.id in current.inspectedThisNight
        val clueAdded = target.clue != null && target.id !in current.cluesFound
        _uiState.update { state ->
            if (state.gameOver) return@update state
            val text = when {
                wasInspected && isAnomaly -> target.anomalyDescription
                wasInspected -> "قبلاً اینجا را بررسی کرده‌ای. این بار فقط سکوت جواب می‌دهد."
                isAnomaly -> target.anomalyDescription
                else -> target.description
            }
            val extra = if (clueAdded) "\n\nسرنخ تازه: ${target.clue}" else ""
            state.copy(
                selectedObject = target.id,
                nearbyObjectId = target.id,
                dialogueTitle = target.title,
                dialogue = text + extra,
                interactionHint = when {
                    newlyFound -> "ناهنجاری پیدا شد. صداها انگار نزدیک‌تر شده‌اند."
                    clueAdded -> "یک سرنخ به دفترچه اضافه شد."
                    else -> "به جزئیات دقت کن؛ همه‌چیز آن‌طور که به نظر می‌رسد نیست."
                },
                stress = (state.stress + when {
                    newlyFound -> target.stressCost + 2
                    wasInspected -> 1
                    else -> target.stressCost
                }).coerceAtMost(100),
                anomalyDetected = state.anomalyDetected || isAnomaly,
                showDialogue = true,
                journalOpen = false,
                inspectedObjectIds = state.inspectedObjectIds + target.id,
                inspectedThisNight = state.inspectedThisNight + target.id,
                cluesFound = if (clueAdded) state.cluesFound + target.id else state.cluesFound,
                audioCue = when (target) {
                    ApartmentObject.Mirror -> AudioCue.MirrorSting
                    ApartmentObject.FrontDoor -> AudioCue.DoorCreak
                    ApartmentObject.Lamp -> AudioCue.LampHum
                    ApartmentObject.Window -> AudioCue.WindowWind
                    ApartmentObject.Radio -> AudioCue.RadioStatic
                    ApartmentObject.Clock -> AudioCue.ClockTick
                    ApartmentObject.FamilyPhoto -> AudioCue.PhotoWhisper
                    ApartmentObject.Phone -> AudioCue.PhoneVibration
                    ApartmentObject.Closet -> AudioCue.ClosetCreak
                    ApartmentObject.Bed -> AudioCue.BedCreak
                },
                audioCueId = state.audioCueId + 1,
            )
        }
    }

    private fun toggleFlashlight() {
        _uiState.update { state ->
            val on = !state.isFlashlightOn
            state.copy(
                isFlashlightOn = on,
                interactionHint = if (on) "چراغ‌قوه روشن شد؛ گوشه‌های تاریک را بگرد." else "چراغ‌قوه خاموش است. تاریکی، جزئیات را می‌بلعد.",
                audioCue = AudioCue.FlashlightClick,
                audioCueId = state.audioCueId + 1,
            )
        }
    }

    private fun toggleSound() {
        _uiState.update { state ->
            state.copy(
                isSoundOn = !state.isSoundOn,
                interactionHint = if (!state.isSoundOn) "صدای محیط و افکت‌ها روشن شد." else "صدا خاموش شد؛ می‌توانی هر زمان دوباره روشنش کنی.",
            )
        }
        if (_uiState.value.isSoundOn) emitCue(AudioCue.Interaction)
    }

    private fun toggleJournal() {
        _uiState.update { state ->
            if (state.journalOpen) {
                state.copy(journalOpen = false, showDialogue = false, audioCue = AudioCue.Paper, audioCueId = state.audioCueId + 1)
            } else {
                val clues = state.cluesFound.mapNotNull { id ->
                    ApartmentObject.fromId(id)?.let { it.title to it.clue }
                }.sortedBy { it.first }
                val body = if (clues.isEmpty()) {
                    "هنوز سرنخی پیدا نکرده‌ای. آینه، عکس، رادیو و پنجره را بررسی کن."
                } else {
                    clues.joinToString("\n\n") { (title, clue) -> "• $title\n${clue.orEmpty()}" } + "\n\nسرنخ‌های جمع‌شده: ${clues.size}"
                }
                state.copy(
                    journalOpen = true,
                    showDialogue = true,
                    dialogueTitle = "دفترچه‌ی سرنخ‌ها",
                    dialogue = body,
                    audioCue = AudioCue.Paper,
                    audioCueId = state.audioCueId + 1,
                )
            }
        }
    }

    private fun continueDialogue() {
        _uiState.update { state ->
            state.copy(
                showDialogue = false,
                journalOpen = false,
                nearbyObjectId = findLookTarget(state.playerX, state.playerY, state.cameraYaw)?.id,
                interactionHint = if (state.gameOver) "پایان بازی" else "جوی‌استیک پایین صفحه حرکتت می‌دهد؛ سمت راست را بکش تا نگاه کنی.",
            )
        }
    }

    private fun ignoreAnomaly() {
        _uiState.update { state ->
            state.copy(
                stress = (state.stress + 3).coerceAtMost(100),
                showDialogue = false,
                journalOpen = false,
                interactionHint = "سعی می‌کنی به اتفاقی که دیدی فکر نکنی؛ اما صدا هنوز آنجاست.",
                audioCue = AudioCue.AnomalyGlitch,
                audioCueId = state.audioCueId + 1,
            )
        }
    }

    private fun endNight() {
        _uiState.update { state ->
            if (state.gameOver) return@update state
            if (state.inspectedThisNight.size < 3) {
                return@update state.copy(
                    showDialogue = true,
                    dialogueTitle = "هنوز زود است",
                    dialogue = "هنوز چیزهای کمی بررسی کرده‌ای. دست‌کم سه شیء را بگرد و بعد گزارش شب را ثبت کن.",
                )
            }
            val finalStress = (state.stress + if (state.anomalyDetected) -11 else 18).coerceIn(0, 100)
            if (state.night >= 7 || finalStress >= 100) {
                val ending = when {
                    finalStress < 35 && state.anomalyDetected -> "پایان: سکوتِ امن"
                    finalStress < 70 -> "پایان: حقیقتِ شکسته"
                    else -> "پایان: چیزی جای تو را گرفت"
                }
                val endingText = when (ending) {
                    "پایان: سکوتِ امن" -> "صبح از پنجره وارد می‌شود. خانه دوباره عادی به نظر می‌رسد؛ فقط آینه با پارچه پوشانده شده. تو زنده مانده‌ای، اما دیگر به بازتابت نگاه نمی‌کنی."
                    "پایان: حقیقتِ شکسته" -> "با طلوع آفتاب، ردپاها ناپدید می‌شوند. سرنخ‌ها نشان می‌دهند اتفاقی واقعی بوده، اما هیچ توضیحی کامل نیست. وقتی از خانه بیرون می‌روی، رادیو پشت سرت روشن می‌شود."
                    else -> "صبح نمی‌شود. صدای قدم‌ها از داخل سرت می‌آید و تصویر آینه لبخند می‌زند. خانه آرام می‌گیرد؛ حالا دیگر تنها نیستی، چون تو بخشی از آن شده‌ای."
                }
                state.copy(
                    stress = finalStress,
                    gameOver = true,
                    endingTitle = ending,
                    dialogueTitle = ending,
                    dialogue = endingText,
                    showDialogue = true,
                    journalOpen = false,
                    interactionHint = "ماجرا تمام شد. می‌توانی دوباره از شب اول شروع کنی.",
                    audioCue = AudioCue.AnomalyGlitch,
                    audioCueId = state.audioCueId + 1,
                )
            } else {
                val nextNight = state.night + 1
                state.copy(
                    night = nextNight,
                    stress = finalStress,
                    anomalyDetected = false,
                    inspectedThisNight = emptySet(),
                    selectedObject = null,
                    nearbyObjectId = null,
                    playerX = 36f,
                    playerY = 47f,
                    cameraYaw = 0f,
                    cameraPitch = 0f,
                    showDialogue = true,
                    journalOpen = false,
                    dialogueTitle = "شب ${persianNumber(nextNight)} — ۰۰:۱۳",
                    dialogue = nightIntro(nextNight),
                    interactionHint = "خانه تغییر کرده است. این بار با دقت بیشتری بگرد.",
                    audioCue = AudioCue.NightTransition,
                    audioCueId = state.audioCueId + 1,
                )
            }
        }
    }

    private fun restartGame() {
        val previous = _uiState.value
        _uiState.value = GameUiState(
            isSoundOn = previous.isSoundOn,
            audioCue = AudioCue.NightTransition,
            audioCueId = previous.audioCueId + 1,
        )
        movementTicks = 0
    }

    private fun findLookTarget(x: Float, y: Float, yawDegrees: Float): ApartmentObject? {
        val yaw = Math.toRadians(yawDegrees.toDouble())
        return ApartmentObject.entries.mapNotNull { obj ->
            val dx = (obj.x - x) / 5f
            val dy = (obj.y - y) / 5f
            val forward = dx * sin(yaw).toFloat() - dy * cos(yaw).toFloat()
            val side = dx * cos(yaw).toFloat() + dy * sin(yaw).toFloat()
            val depth = hypot(dx, dy)
            val angle = Math.toDegrees(atan2(side.toDouble(), forward.toDouble())).toFloat()
            if (forward > 0.15f && abs(angle) < 24f && depth < 5.1f && hasLineOfSight(x, y, obj.x, obj.y)) {
                obj to depth
            } else null
        }.minByOrNull { it.second }?.first
    }

    private fun hasLineOfSight(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = hypot(dx, dy)
        val steps = (length / 1.5f).toInt().coerceAtLeast(1)
        for (i in 1 until steps) {
            val t = i.toFloat() / steps
            if (!isWalkable(x1 + dx * t, y1 + dy * t)) return false
        }
        return true
    }

    private fun isWalkable(x: Float, y: Float): Boolean {
        val inLivingRoom = x in 9f..61f && y in 13f..62f
        val inKitchen = x in 71f..94f && y in 13f..51f
        val inHallway = x in 49f..78f && y in 38f..68f
        val inBedroom = x in 10f..51f && y in 59f..91f
        val inBathroom = x in 57f..80f && y in 63f..91f
        return inLivingRoom || inKitchen || inHallway || inBedroom || inBathroom
    }

    private fun emitCue(cue: AudioCue) {
        _uiState.update { it.copy(audioCue = cue, audioCueId = it.audioCueId + 1) }
    }

    private fun nightIntro(night: Int): String = when (night) {
        2 -> "ساعت ۰۰:۱۳. عکس خانوادگی روی دیوار کمی کج شده؛ مطمئنی دیشب صاف بود. از اتاق خواب صدای کشیده‌شدن چیزی روی زمین می‌آید."
        3 -> "این بار برق قطع نمی‌شود، اما همه‌ی ساعت‌ها ایستاده‌اند. از آشپزخانه صدای رادیویی می‌آید که اصلاً به برق وصل نیست."
        4 -> "سه ضربه به در می‌خورد. وقتی به چشمی نگاه می‌کنی، راهرو خالی است. روی کف، ردپاهایی تازه به سمت تو می‌آیند."
        5 -> "گوشی روشن می‌شود و چند تماس ازدست‌رفته از شماره‌ی خودت نشان می‌دهد؛ همه در زمان‌هایی که خواب بوده‌ای."
        6 -> "در پنجره انعکاس اتاق را می‌بینی، اما جای مبلمان در بازتاب فرق دارد. پشت سرت را که نگاه می‌کنی، چیزی تغییر نکرده."
        else -> "شب هفتم است. خانه دیگر شبیه خانه نیست؛ راهرو طولانی‌تر شده و صدای قدم‌ها درست با قدم‌های تو هماهنگ است. این آخرین فرصت توست."
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float = hypot(x1 - x2, y1 - y2)

    private fun persianNumber(number: Int): String = number.toString().map { char ->
        if (char in '0'..'9') ('۰'.code + (char - '0')).toChar() else char
    }.joinToString("")
}

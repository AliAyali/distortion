package com.aliayali.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliayali.game.audio.HorrorAudioManager
import com.aliayali.game.components.DialoguePanel
import com.aliayali.game.components.InteractionHint
import com.aliayali.game.components.SceneView
import com.aliayali.game.components.StressMeter
import com.aliayali.game.components.VirtualJoystick
import com.aliayali.game.model.ApartmentObject
import com.aliayali.game.model.AudioCue
import com.aliayali.game.model.GameAction
import com.aliayali.game.settings.GameSettingsStore

@Composable
fun GameScreen(
    uiState: GameUiState,
    onAction: (GameAction) -> Unit,
    onMoveStick: (Float, Float) -> Unit,
    onLookDelta: (Float, Float) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsStore = remember(context) { GameSettingsStore(context.applicationContext) }
    var settings by remember(settingsStore) { mutableStateOf(settingsStore.load()) }
    var appInForeground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner, settingsStore) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> appInForeground = true
                Lifecycle.Event.ON_STOP -> appInForeground = false
                Lifecycle.Event.ON_RESUME -> settings = settingsStore.load()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val audio = remember(context) { HorrorAudioManager(context.applicationContext, settings) }
    DisposableEffect(audio) { onDispose { audio.close() } }

    LaunchedEffect(settings, uiState.isSoundOn, appInForeground) {
        audio.updateSettings(settings)
        audio.setEnabled(uiState.isSoundOn && appInForeground)
    }
    LaunchedEffect(uiState.audioCueId) {
        if (uiState.audioCueId > 0) audio.play(uiState.audioCue)
    }
    LaunchedEffect(uiState.anomalyDetected) {
        if (uiState.anomalyDetected) audio.play(AudioCue.AnomalyGlitch)
    }

    // Controls are positioned in LTR coordinates (joystick left, interaction buttons right),
    // while Persian copy is explicitly right-aligned where appropriate.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF050608)),
        ) {
            SceneView(
                playerX = uiState.playerX,
                playerY = uiState.playerY,
                cameraYaw = uiState.cameraYaw,
                cameraPitch = uiState.cameraPitch,
                night = uiState.night,
                nearbyObjectId = uiState.nearbyObjectId,
                flashlightOn = uiState.isFlashlightOn,
                anomalyDetected = uiState.anomalyDetected,
                reduceFlashes = settings.reduceFlashes,
                onLookDelta = onLookDelta,
                modifier = Modifier.fillMaxSize(),
            )

            // Small, translucent cinematic HUD keeps most of the room visible.
            Column(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختلال", color = Color(0xFFE8E1DF), fontSize = 19.sp, fontWeight = FontWeight.Light)
                    Text("/", color = Color(0xFF7E424B), fontSize = 15.sp)
                    Text("شب ${persianNumber(uiState.night)} از ۷", color = Color(0xFFD8BCB5), fontSize = 12.sp)
                }
                Surface(
                    color = Color(0xB90A0B10),
                    shape = RoundedCornerShape(9.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x334C4348)),
                ) {
                    StressMeter(
                        stress = uiState.stress,
                        modifier = Modifier.width(205.dp).padding(horizontal = 9.dp, vertical = 7.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 14.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HudChip(
                    text = if (uiState.isSoundOn) "صدا روشن" else "صدا خاموش",
                    onClick = { onAction(GameAction.ToggleSound) },
                )
                HudChip(text = "بازگشت", onClick = onBackToHome)
            }

            // Center reticle is quiet until an interactable object is in reach.
            Canvas(modifier = Modifier.align(Alignment.Center).size(24.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val color = if (uiState.nearbyObjectId != null) Color(0xFFE5C4C5) else Color(0xBBD9D4CF)
                drawCircle(color.copy(alpha = .72f), radius = 2.dp.toPx(), center = center)
                val arm = 8.dp.toPx()
                val gap = 4.dp.toPx()
                drawLine(color, Offset(center.x - arm, center.y), Offset(center.x - gap, center.y), 1.dp.toPx())
                drawLine(color, Offset(center.x + gap, center.y), Offset(center.x + arm, center.y), 1.dp.toPx())
                drawLine(color, Offset(center.x, center.y - arm), Offset(center.x, center.y - gap), 1.dp.toPx())
                drawLine(color, Offset(center.x, center.y + gap), Offset(center.x, center.y + arm), 1.dp.toPx())
            }

            if (!uiState.showDialogue && !uiState.gameOver) {
                // The analog stick is fixed to the lower-left corner in landscape mode.
                VirtualJoystick(
                    enabled = !uiState.journalOpen,
                    onMove = onMoveStick,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 14.dp),
                )

                Column(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val target = ApartmentObject.fromId(uiState.nearbyObjectId)
                    ControlButton(
                        text = target?.let { "بررسی: ${it.title}" } ?: "تعامل",
                        onClick = { onAction(GameAction.Interact) },
                        enabled = target != null && !uiState.journalOpen,
                        primary = true,
                        modifier = Modifier.widthIn(min = 142.dp, max = 178.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ControlButton(
                            text = if (uiState.isFlashlightOn) "خاموشی نور" else "چراغ‌قوه",
                            onClick = { onAction(GameAction.ToggleFlashlight) },
                            modifier = Modifier.width(102.dp),
                        )
                        ControlButton(
                            text = "دفترچه (${persianNumber(uiState.cluesFound.size)})",
                            onClick = { onAction(GameAction.ToggleJournal) },
                            modifier = Modifier.width(102.dp),
                        )
                    }
                    ControlButton(
                        text = "ثبت گزارش شب",
                        onClick = { onAction(GameAction.EndNight) },
                        modifier = Modifier.width(211.dp),
                    )
                }

                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 19.dp).widthIn(max = 280.dp),
                    color = Color(0xA8080A0F),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x263E3940)),
                ) {
                    InteractionHint(
                        text = uiState.interactionHint,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                    )
                }
            }

            if (uiState.showDialogue || uiState.gameOver) {
                DialoguePanel(
                    title = uiState.dialogueTitle,
                    dialogue = uiState.dialogue,
                    buttonText = if (uiState.gameOver) "شروع دوباره" else if (uiState.journalOpen) "بستن دفترچه" else "ادامه",
                    onContinue = {
                        when {
                            uiState.gameOver -> onAction(GameAction.RestartGame)
                            uiState.journalOpen -> onAction(GameAction.ToggleJournal)
                            else -> onAction(GameAction.ContinueDialogue)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.66f)
                        .padding(bottom = 12.dp),
                )
            }

            if (uiState.nearbyObjectId != null && !uiState.showDialogue) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                    color = Color(0xB90A0B10),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "هدف: ${ApartmentObject.fromId(uiState.nearbyObjectId)?.title.orEmpty()}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = Color(0xFFE2C2C2),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun HudChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xB90A0B10),
        contentColor = Color(0xFFE0D7D4),
        shape = RoundedCornerShape(9.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x443B3438)),
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 11.sp)
    }
}

@Composable
private fun ControlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightInGameButton(),
        shape = RoundedCornerShape(10.dp),
        color = when {
            !enabled -> Color(0xB8101116)
            primary -> Color(0xD52B1119)
            else -> Color(0xC80B0D12)
        },
        contentColor = when {
            !enabled -> Color(0xFF6C666B)
            primary -> Color(0xFFEAD3D4)
            else -> Color(0xFFD2C8C7)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                !enabled -> Color(0x35252227)
                primary -> Color(0xB66A303B)
                else -> Color(0x554B4147)
            },
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

private fun Modifier.heightInGameButton(): Modifier = this.heightIn(min = 42.dp)

private fun persianNumber(number: Int): String = number.toString().map { char ->
    if (char in '0'..'9') ('۰'.code + (char - '0')).toChar() else char
}.joinToString("")

package com.aliayali.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aliayali.game.audio.HorrorAudioManager
import com.aliayali.game.settings.GameSettingsStore
import com.aliayali.home.components.AudioSettingsDialog
import com.aliayali.home.components.GameLogo
import com.aliayali.home.components.HomeBackground
import com.aliayali.home.components.HomeMenuButton
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onNewGameClick: () -> Unit,
    onContinueClick: () -> Unit,
    hasSavedGame: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsStore = remember(context) { GameSettingsStore(context.applicationContext) }
    var settings by remember(settingsStore) { mutableStateOf(settingsStore.load()) }
    var appInForeground by remember { mutableStateOf(true) }
    val menuAudio = remember(context) { HorrorAudioManager(context.applicationContext, settings) }
    DisposableEffect(menuAudio) {
        onDispose { menuAudio.close() }
    }
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
    LaunchedEffect(settings, appInForeground) {
        menuAudio.updateSettings(settings)
        menuAudio.setEnabled(appInForeground)
    }
    var contentVisible by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(180)
        contentVisible = true
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        HomeBackground(modifier = modifier) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 38.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Landscape composition: title/art occupies the left half, actions the right.
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AnimatedVisibility(
                        visible = contentVisible,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 10 }),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            GameLogo()
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = "هر ناهنجاری، چیزی از تو کم می‌کند.",
                                color = Color(0xFF777078),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "نسخه‌ی آزمایشی ۰.۳.۰  •  تجربه‌ی اول‌شخص",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .widthIn(max = 410.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "آپارتمان شماره‌ی سیزده",
                        color = Color(0xFFE2D9D8),
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "بعضی شب‌ها، خانه تو را به یاد می‌آورد.",
                        color = Color(0xFF777078),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(22.dp))
                    AnimatedVisibility(visible = contentVisible, enter = fadeIn()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            HomeMenuButton(
                                text = "شروع بازی",
                                onClick = onNewGameClick,
                                primary = true,
                            )
                            Spacer(Modifier.height(10.dp))
                            HomeMenuButton(
                                text = if (hasSavedGame) "ادامه‌ی بازی" else "ادامه‌ی بازی · ذخیره‌ای نیست",
                                onClick = onContinueClick,
                                enabled = hasSavedGame,
                            )
                            Spacer(Modifier.height(10.dp))
                            HomeMenuButton(
                                text = "تنظیمات صدا و تصویر",
                                onClick = { showSettings = true },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        AudioSettingsDialog(
            store = settingsStore,
            onDismiss = { showSettings = false },
            onSettingsChanged = { settings = it },
        )
    }
}

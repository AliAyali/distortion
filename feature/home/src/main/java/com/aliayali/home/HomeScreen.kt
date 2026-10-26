package com.aliayali.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliayali.home.components.GameLogo
import com.aliayali.home.components.HomeBackground
import com.aliayali.home.components.HomeMenuButton
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onNewGameClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var contentVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        delay(250)
        contentVisible = true
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        HomeBackground(
            modifier = modifier,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 28.dp,
                        vertical = 24.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(
                        modifier = Modifier.weight(0.8f),
                    )

                    AnimatedVisibility(
                        visible = contentVisible,
                        enter = fadeIn() + slideInVertically(
                            initialOffsetY = { it / 8 },
                        ),
                    ) {
                        GameLogo()
                    }

                    Spacer(
                        modifier = Modifier.height(64.dp),
                    )

                    AnimatedVisibility(
                        visible = contentVisible,
                        enter = fadeIn(),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            HomeMenuButton(
                                text = "شروع بازی",
                                onClick = onNewGameClick,
                                primary = true,
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp),
                            )

                            HomeMenuButton(
                                text = "ادامه‌ی بازی",
                                onClick = {},
                                enabled = false,
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp),
                            )

                            HomeMenuButton(
                                text = "تنظیمات",
                                onClick = {},
                                enabled = false,
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text = "اینجا تنها نیستی...",
                        color = Color(0xFF777078),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp),
                    )

                    Text(
                        text = "نسخه‌ی آزمایشی ۰.۱.۰",
                        color = MaterialTheme.colorScheme
                            .onSurfaceVariant
                            .copy(alpha = 0.45f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
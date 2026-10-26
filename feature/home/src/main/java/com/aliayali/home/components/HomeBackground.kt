package com.aliayali.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun HomeBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "home_background")

    val glowAlpha by transition.animateFloat(
        initialValue = 0.025f,
        targetValue = 0.085f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3200,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "red_glow",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050608))
            .drawWithCache {
                val center = Offset(
                    x = size.width * 0.5f,
                    y = size.height * 0.36f,
                )

                val radius = size.maxDimension * 0.72f

                val glowBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7D1725).copy(alpha = glowAlpha),
                        Color(0xFF260B12).copy(alpha = glowAlpha * 0.45f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                )

                onDrawBehind {
                    drawRect(glowBrush)
                }
            },
    ) {
        content()
    }
}
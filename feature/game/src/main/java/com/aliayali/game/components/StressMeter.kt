package com.aliayali.game.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StressMeter(
    stress: Int,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = stress.coerceIn(0, 100) / 100f,
        label = "stress_progress",
    )
    val barColor by animateColorAsState(
        targetValue = when {
            stress >= 70 -> Color(0xFFE34B58)
            stress >= 35 -> Color(0xFFC58B58)
            else -> Color(0xFF75C9C0)
        },
        label = "stress_color",
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "وضعیت روانی",
                    color = Color(0xFFE1DFDC),
                    fontSize = 11.sp,
                    textAlign = TextAlign.End,
                )
                Text(
                    text = "${persianNumber(stress)}٪",
                    color = barColor,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Start,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF282A30)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(barColor),
                )
            }
        }
    }
}

private fun persianNumber(number: Int): String = number.toString().map { char ->
    if (char in '0'..'9') ('۰'.code + (char - '0')).toChar() else char
}.joinToString("")

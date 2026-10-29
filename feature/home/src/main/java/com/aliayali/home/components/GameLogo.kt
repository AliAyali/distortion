package com.aliayali.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameLogo(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "یک تجربه‌ی ترس روان‌شناختی",
            color = Color(0xFF9B9AA0),
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "اختلال",
            color = Color(0xFFE8E5E0),
            fontSize = 52.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.24f)
                .height(1.dp),
        ) {
            drawLine(
                color = Color(0xFF9B303C).copy(alpha = 0.8f),
                start = Offset(
                    0f,
                    size.height / 2,
                ),
                end = Offset(
                    size.width,
                    size.height / 2,
                ),
                strokeWidth = size.height,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "بعضی چیزها نباید دیده شوند",
            color = Color(0xFF85858B),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}
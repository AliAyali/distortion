package com.aliayali.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeMenuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    val contentColor = when {
        !enabled -> Color(0xFF555961)
        primary -> Color(0xFFE8E5E0)
        else -> Color(0xFFAAAEB4)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.small,
        color = if (primary) {
            Color(0xFF241116)
        } else {
            Color.Transparent
        },
        contentColor = contentColor,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                !enabled -> Color(0xFF202329)
                primary -> Color(0xFF79303A)
                else -> Color(0xFF282C33)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = if (primary) {
                    FontWeight.Medium
                } else {
                    FontWeight.Normal
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}
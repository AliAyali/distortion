package com.aliayali.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

/** Fixed mobile thumbstick. While held, it continuously emits a normalised move vector. */
@Composable
fun VirtualJoystick(
    enabled: Boolean,
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var thumb by remember { mutableStateOf(Offset.Zero) }
    var axisX by remember { mutableFloatStateOf(0f) }
    var axisY by remember { mutableFloatStateOf(0f) }
    var movementJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun updateThumb(position: Offset, width: Float, height: Float) {
        val radius = minOf(width, height) * 0.34f
        val dx = position.x - width / 2f
        val dy = position.y - height / 2f
        val length = hypot(dx, dy)
        val scale = if (length > radius) radius / length else 1f
        thumb = Offset(dx * scale, dy * scale)
        axisX = (thumb.x / radius).coerceIn(-1f, 1f)
        axisY = (thumb.y / radius).coerceIn(-1f, 1f)
    }

    Canvas(
        modifier = modifier
            .size(116.dp)
            .pointerInput(enabled) {
                detectDragGestures(
                    onDragStart = { position ->
                        if (enabled) {
                            updateThumb(position, size.width.toFloat(), size.height.toFloat())
                            movementJob?.cancel()
                            movementJob = scope.launch {
                                while (true) {
                                    onMove(axisX, axisY)
                                    delay(85)
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        movementJob?.cancel()
                        movementJob = null
                        thumb = Offset.Zero
                        axisX = 0f
                        axisY = 0f
                    },
                    onDragCancel = {
                        movementJob?.cancel()
                        movementJob = null
                        thumb = Offset.Zero
                        axisX = 0f
                        axisY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (enabled) {
                            val position = Offset(
                                (size.width / 2f + thumb.x + dragAmount.x).coerceIn(0f, size.width.toFloat()),
                                (size.height / 2f + thumb.y + dragAmount.y).coerceIn(0f, size.height.toFloat()),
                            )
                            updateThumb(position, size.width.toFloat(), size.height.toFloat())
                        }
                    },
                )
            },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) * 0.43f
        drawCircle(Color(0x99080A10), radius, center)
        drawCircle(Color(0x99C8B9B2), radius, center, style = Stroke(width = 1.5.dp.toPx()))
        drawCircle(Color(0x334B9C91), radius * 0.68f, center, style = Stroke(width = 1.dp.toPx()))
        drawLine(Color(0x335F8F89), Offset(center.x - radius * .55f, center.y), Offset(center.x + radius * .55f, center.y), 1.dp.toPx())
        drawLine(Color(0x335F8F89), Offset(center.x, center.y - radius * .55f), Offset(center.x, center.y + radius * .55f), 1.dp.toPx())
        val knobRadius = radius * 0.39f
        val knobCenter = Offset(center.x + thumb.x, center.y + thumb.y)
        drawCircle(Color(0xC82B3339), knobRadius, knobCenter)
        drawCircle(
            color = if (enabled) Color(0xFFE2C9C5) else Color(0xFF6B6568),
            radius = knobRadius,
            center = knobCenter,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawCircle(Color(0xFF7D343F), knobRadius * 0.20f, knobCenter)
    }
}

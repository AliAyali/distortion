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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Lightweight illustrated set for the title screen. It is drawn as an architectural backdrop
 * (window, city, wall, doorway, furniture and floor) rather than a floating-object collage.
 * The foreground UI remains legible because a dark cinematic grade is applied last.
 */
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
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "red_glow",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050608))
            .drawWithCache {
                val w = size.width
                val h = size.height
                val wallBottom = h * 0.70f
                val wallBrush = Brush.verticalGradient(
                    listOf(Color(0xFF282225), Color(0xFF16171C), Color(0xFF0D1016)),
                    startY = 0f,
                    endY = wallBottom,
                )
                val floorBrush = Brush.verticalGradient(
                    listOf(Color(0xFF28201F), Color(0xFF100F13), Color(0xFF050608)),
                    startY = wallBottom,
                    endY = h,
                )
                val windowX = w * 0.405f
                val windowY = h * 0.13f
                val windowW = w * 0.235f
                val windowH = h * 0.37f
                val cityBrush = Brush.verticalGradient(
                    listOf(Color(0xFF1E3245), Color(0xFF0A111C)),
                    startY = windowY,
                    endY = windowY + windowH,
                )
                val lampGlow = Brush.radialGradient(
                    colors = listOf(Color(0x5ED5A16A), Color(0x20C57D4E), Color.Transparent),
                    center = Offset(w * 0.22f, h * 0.50f),
                    radius = h * 0.43f,
                )
                val redGlow = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7D1725).copy(alpha = glowAlpha),
                        Color(0xFF260B12).copy(alpha = glowAlpha * 0.45f),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.52f, h * 0.38f),
                    radius = size.maxDimension * 0.72f,
                )
                val rugPath = Path().apply {
                    moveTo(w * 0.20f, h * 0.74f)
                    lineTo(w * 0.70f, h * 0.74f)
                    lineTo(w * 0.92f, h)
                    lineTo(w * 0.04f, h)
                    close()
                }
                val doorwayPath = Path().apply {
                    moveTo(w * 0.75f, wallBottom)
                    lineTo(w * 0.75f, h * 0.20f)
                    quadraticTo(w * 0.75f, h * 0.16f, w * 0.78f, h * 0.16f)
                    lineTo(w * 0.91f, h * 0.16f)
                    quadraticTo(w * 0.94f, h * 0.16f, w * 0.94f, h * 0.20f)
                    lineTo(w * 0.94f, wallBottom)
                    close()
                }
                val vignette = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0x50000000), Color(0xD9000000)),
                    center = Offset(w * 0.50f, h * 0.48f),
                    radius = size.maxDimension * 0.82f,
                )

                onDrawBehind {
                    // Room envelope and floor plane.
                    drawRect(wallBrush, size = Size(w, wallBottom))
                    drawRect(floorBrush, topLeft = Offset(0f, wallBottom), size = Size(w, h - wallBottom))
                    drawRect(Color(0xFF08090D).copy(alpha = 0.42f), size = Size(w, h * 0.08f))

                    // Window is set into the back wall and frames a city at night.
                    drawRoundRect(
                        color = Color(0xFF111116),
                        topLeft = Offset(windowX - w * 0.012f, windowY - h * 0.012f),
                        size = Size(windowW + w * 0.024f, windowH + h * 0.024f),
                        cornerRadius = CornerRadius(w * 0.006f),
                    )
                    drawRect(Color(0xFF795B46), Offset(windowX - w * 0.006f, windowY - h * 0.004f), Size(windowW + w * 0.012f, h * 0.012f))
                    drawRect(cityBrush, Offset(windowX, windowY), Size(windowW, windowH))
                    // Moon and overlapping apartment silhouettes.
                    drawCircle(Color(0xFFD8E0E8).copy(alpha = 0.84f), h * 0.012f, Offset(windowX + windowW * 0.72f, windowY + windowH * 0.20f))
                    val buildings = listOf(
                        floatArrayOf(0.00f, 0.22f, 0.62f),
                        floatArrayOf(0.20f, 0.24f, 0.44f),
                        floatArrayOf(0.42f, 0.18f, 0.73f),
                        floatArrayOf(0.60f, 0.22f, 0.52f),
                        floatArrayOf(0.80f, 0.20f, 0.64f),
                    )
                    buildings.forEachIndexed { buildingIndex, building ->
                        val bx = windowX + windowW * building[0]
                        val bw = windowW * building[1]
                        val bh = windowH * building[2]
                        val by = windowY + windowH - bh
                        drawRect(Color(0xFF0A111B), Offset(bx, by), Size(bw, bh))
                        for (r in 0..4) for (c in 0..2) {
                            if ((r + c + buildingIndex) % 4 != 1) {
                                val wc = if ((r + buildingIndex) % 3 == 0) Color(0xFFB99770) else Color(0xFF6F91A7)
                                drawRect(
                                    wc.copy(alpha = if ((r * 2 + c + buildingIndex) % 3 == 0) 0.48f else 0.24f),
                                    Offset(bx + bw * (0.15f + c * 0.27f), by + bh * (0.12f + r * 0.15f)),
                                    Size(max(1f, bw * 0.075f), max(1f, bh * 0.038f)),
                                )
                            }
                        }
                    }
                    // Heavy curtains and deep sill keep the window visually anchored to the wall.
                    drawRect(Color(0xFF241B20), Offset(windowX - w * 0.025f, windowY - h * 0.025f), Size(w * 0.035f, windowH + h * 0.065f))
                    drawRect(Color(0xFF241B20), Offset(windowX + windowW - w * 0.01f, windowY - h * 0.025f), Size(w * 0.035f, windowH + h * 0.065f))
                    drawRect(Color(0xFF805D46), Offset(windowX - w * 0.025f, windowY + windowH), Size(windowW + w * 0.05f, h * 0.025f))

                    // Doorway into the darker bedroom/hallway, with a little warm light behind it.
                    drawPath(doorwayPath, Color(0xFF08090D))
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF1A1314), Color(0xFF60402C), Color(0xFF120F14))),
                        topLeft = Offset(w * 0.775f, h * 0.205f),
                        size = Size(w * 0.145f, wallBottom - h * 0.205f),
                    )
                    drawRect(Color(0xFF382820), Offset(w * 0.77f, h * 0.19f), Size(w * 0.18f, h * 0.022f))
                    drawRect(Color(0xFF39271F), Offset(w * 0.76f, h * 0.20f), Size(w * 0.012f, wallBottom - h * 0.20f))
                    drawRect(Color(0xFF39271F), Offset(w * 0.93f, h * 0.20f), Size(w * 0.012f, wallBottom - h * 0.20f))
                    drawCircle(Color(0xFFD0A46F), h * 0.006f, Offset(w * 0.905f, h * 0.48f))

                    // Framed pictures and a shelf break up the blank wall without adding visual noise.
                    listOf(0.075f to 0.30f, 0.16f to 0.24f, 0.69f to 0.31f).forEachIndexed { index, pos ->
                        val fx = w * pos.first
                        val fy = h * pos.second
                        val fw = w * if (index == 1) 0.055f else 0.064f
                        val fh = h * if (index == 1) 0.12f else 0.15f
                        drawRect(Color(0xFF171214), Offset(fx - 3.dp.toPx(), fy - 3.dp.toPx()), Size(fw + 6.dp.toPx(), fh + 6.dp.toPx()))
                        drawRect(Color(0xFF735440), Offset(fx, fy), Size(fw, fh))
                        drawRect(Color(0xFF24232A), Offset(fx + fw * 0.09f, fy + fh * 0.08f), Size(fw * 0.82f, fh * 0.84f))
                        drawOval(Color(0xFF65504A).copy(alpha = 0.58f), Offset(fx + fw * 0.25f, fy + fh * 0.17f), Size(fw * 0.50f, fh * 0.60f))
                    }
                    val shelfX = w * 0.055f
                    val shelfY = h * 0.43f
                    drawRect(Color(0xFF201714), Offset(shelfX, shelfY), Size(w * 0.115f, h * 0.24f))
                    drawRect(Color(0xFF5E3C2D), Offset(shelfX - w * 0.008f, shelfY + h * 0.06f), Size(w * 0.13f, h * 0.015f))
                    drawRect(Color(0xFF5E3C2D), Offset(shelfX - w * 0.008f, shelfY + h * 0.13f), Size(w * 0.13f, h * 0.015f))
                    drawRect(Color(0xFF5E3C2D), Offset(shelfX - w * 0.008f, shelfY + h * 0.20f), Size(w * 0.13f, h * 0.015f))
                    for (i in 0..8) {
                        val bx = shelfX + w * (0.008f + i * 0.012f)
                        val bh = h * (0.035f + (i % 3) * 0.012f)
                        val bookColor = if (i % 2 == 0) Color(0xFF43312A) else Color(0xFF48352F)
                        drawRect(bookColor, Offset(bx, shelfY + h * 0.055f - bh), Size(w * 0.009f, bh))
                    }

                    // Old sofa with seat, arms and cushions sitting on the floor plane.
                    val sofaX = w * 0.20f
                    val sofaY = h * 0.67f
                    drawRoundRect(Color(0xB9000000), Offset(sofaX - w * 0.012f, sofaY + h * 0.14f), Size(w * 0.25f, h * 0.16f), CornerRadius(h * 0.02f))
                    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF443239), Color(0xFF21191F)), sofaY, sofaY + h * 0.25f), Offset(sofaX, sofaY + h * 0.055f), Size(w * 0.23f, h * 0.20f), CornerRadius(h * 0.025f))
                    drawRoundRect(Color(0xFF30232A), Offset(sofaX + w * 0.015f, sofaY), Size(w * 0.20f, h * 0.08f), CornerRadius(h * 0.02f))
                    drawRoundRect(Color(0xFF503B40), Offset(sofaX + w * 0.028f, sofaY + h * 0.035f), Size(w * 0.075f, h * 0.070f), CornerRadius(h * 0.012f))
                    drawRoundRect(Color(0xFF392B31), Offset(sofaX + w * 0.115f, sofaY + h * 0.030f), Size(w * 0.075f, h * 0.075f), CornerRadius(h * 0.012f))

                    // Coffee table and believable small clutter, kept low contrast behind the menu.
                    drawRoundRect(Color(0x88000000), Offset(w * 0.40f, h * 0.79f), Size(w * 0.20f, h * 0.045f), CornerRadius(h * 0.008f))
                    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF70503A), Color(0xFF2B201C)), h * 0.76f, h * 0.82f), Offset(w * 0.39f, h * 0.76f), Size(w * 0.21f, h * 0.036f), CornerRadius(h * 0.006f))
                    drawRect(Color(0xFF171519), Offset(w * 0.415f, h * 0.795f), Size(w * 0.055f, h * 0.008f))
                    drawRect(Color(0xFF9B846F).copy(alpha = 0.54f), Offset(w * 0.50f, h * 0.785f), Size(w * 0.024f, h * 0.012f))
                    drawRoundRect(Color(0xFF19181E), Offset(w * 0.54f, h * 0.754f), Size(w * 0.022f, h * 0.023f), CornerRadius(h * 0.004f))

                    // Perspective rug with very subdued weave (no high-contrast grid artefacts).
                    drawPath(rugPath, Brush.verticalGradient(listOf(Color(0xFF302027), Color(0xFF17151B)), h * 0.73f, h))
                    drawPath(rugPath, Color(0xFF78534B).copy(alpha = 0.19f), style = Stroke(width = max(1f, h * 0.0012f)))

                    // Floor-standing lamp and its amber pool; the base aligns with the floor.
                    drawRect(Color(0xFF211A18), Offset(w * 0.165f, h * 0.56f), Size(w * 0.010f, h * 0.17f))
                    drawRoundRect(Color(0xFF9D7148), Offset(w * 0.142f, h * 0.50f), Size(w * 0.056f, h * 0.064f), CornerRadius(h * 0.008f))
                    drawRoundRect(Color(0xFFFFD49A).copy(alpha = 0.72f), Offset(w * 0.15f, h * 0.51f), Size(w * 0.040f, h * 0.044f), CornerRadius(h * 0.006f))
                    drawRect(Color(0xFF211917), Offset(w * 0.15f, h * 0.73f), Size(w * 0.04f, h * 0.012f))
                    drawRect(lampGlow)

                    // Wall/floor joint and final dark grade make the backdrop sit behind the menu.
                    drawRect(Color(0xFF0A090D), Offset(0f, wallBottom - h * 0.008f), Size(w, h * 0.02f))
                    drawRect(redGlow)
                    drawRect(
                        brush = Brush.verticalGradient(listOf(Color(0x14000000), Color(0x6A000000), Color(0xA8000000)), 0f, h),
                        size = Size(w, h),
                    )
                    drawRect(vignette, size = Size(w, h))
                }
            },
    ) {
        content()
    }
}

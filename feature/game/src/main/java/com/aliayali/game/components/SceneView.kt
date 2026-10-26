package com.aliayali.game.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.aliayali.game.anomalyForNight
import com.aliayali.game.model.ApartmentObject
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

private const val CAMERA_HEIGHT = 0.72f
private const val OBJECT_ASSET_ROOT = "scenes/apartment/objects/"

/**
 * Perspective-first apartment renderer. All visible props are projected from fixed room
 * coordinates, so doors/windows stay against their walls and furniture sits on the floor.
 * Optional transparent WebP sprites can replace any procedural fallback without code changes.
 */
@Composable
fun SceneView(
    playerX: Float,
    playerY: Float,
    cameraYaw: Float,
    cameraPitch: Float,
    night: Int,
    nearbyObjectId: String?,
    flashlightOn: Boolean,
    anomalyDetected: Boolean,
    reduceFlashes: Boolean = false,
    onLookDelta: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sprites = rememberSceneSprites()
    val anomalyPulse by animateFloatAsState(
        targetValue = if (anomalyDetected) 1f else 0f,
        animationSpec = tween(durationMillis = if (reduceFlashes) 900 else 420),
        label = "anomaly_pulse",
    )
    val infinite = rememberInfiniteTransition(label = "apartment_ambience")
    val lightFlicker by infinite.animateFloat(
        initialValue = 0.965f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (reduceFlashes) 2400 else 1350),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lamp_flicker",
    )

    Canvas(
        modifier = modifier.pointerInput(onLookDelta) {
            var looking = false
            detectDragGestures(
                onDragStart = { start -> looking = start.x > size.width * 0.42f },
                onDragEnd = { looking = false },
                onDragCancel = { looking = false },
                onDrag = { change, drag ->
                    if (looking) {
                        change.consume()
                        onLookDelta(drag.x, drag.y)
                    }
                },
            )
        },
    ) {
        val w = size.width
        val h = size.height
        val yaw = Math.toRadians(cameraYaw.toDouble()).toFloat()
        val fov = Math.toRadians(70.0).toFloat()
        val focal = w / (2f * tan(fov / 2f))
        val horizon = (h * (0.405f + cameraPitch / 100f)).coerceIn(h * 0.32f, h * 0.49f)
        val rayStep = 0.045f
        val rayCount = (w / 2.8f).toInt().coerceIn(220, 440)
        val sliceWidth = w / rayCount
        val wallDistances = FloatArray(rayCount)

        // Ceiling and floor are continuous gradients; the old artificial floor grid is gone.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF080A10), Color(0xFF17171B), Color(0xFF211B1A)),
                startY = 0f,
                endY = horizon + h * 0.06f,
            ),
            size = Size(w, horizon + h * 0.06f),
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF302626), Color(0xFF211B1C), Color(0xFF0C0D12)),
                startY = horizon,
                endY = h,
            ),
            topLeft = Offset(0f, horizon),
            size = Size(w, h - horizon),
        )

        // A rug is a true floor-plane polygon, not a floating vertical sprite.
        drawLivingRoomRug(
            playerX = playerX,
            playerY = playerY,
            yaw = yaw,
            focal = focal,
            horizon = horizon,
            screenWidth = w,
            screenHeight = h,
        )

        // Ray cast room walls with sub-pixel overlap. No per-ray stripes or floor guide lines.
        for (ray in 0 until rayCount) {
            val screenX = (ray + 0.5f) * sliceWidth
            val cameraX = (screenX - w * 0.5f) / focal
            val angle = yaw + atan(cameraX)
            var distance = rayStep
            var previousDistance = 0f
            var didHit = false
            while (distance < 18f) {
                val wx = playerX + sin(angle) * distance * 5f
                val wy = playerY - cos(angle) * distance * 5f
                if (!insideApartment(wx, wy)) {
                    didHit = true
                    break
                }
                previousDistance = distance
                distance += rayStep
            }
            if (didHit) {
                var low = previousDistance
                var high = distance
                repeat(5) {
                    val mid = (low + high) * 0.5f
                    val mx = playerX + sin(angle) * mid * 5f
                    val my = playerY - cos(angle) * mid * 5f
                    if (insideApartment(mx, my)) low = mid else high = mid
                }
                distance = (low + high) * 0.5f
            }

            val correctedDistance = max(0.20f, distance * cos(angle - yaw))
            wallDistances[ray] = correctedDistance
            val wallHeight = focal * (2.72f - CAMERA_HEIGHT) / correctedDistance
            val top = horizon - wallHeight
            val bottom = horizon + focal * CAMERA_HEIGHT / correctedDistance
            val hitX = playerX + sin(angle) * distance * 5f
            val hitY = playerY - cos(angle) * distance * 5f
            val distanceLight = (1f - correctedDistance / 17f).coerceIn(0.16f, 0.93f)
            val warmWall = hitY < 37f && hitX < 63f
            val kitchenWall = hitX > 70f && hitY < 52f
            val base = when {
                kitchenWall -> intArrayOf(53, 49, 47)
                hitY > 59f && hitX < 53f -> intArrayOf(54, 39, 42)
                warmWall -> intArrayOf(69, 49, 42)
                else -> intArrayOf(54, 47, 49)
            }
            val warmGlow = if (warmWall) 0.10f else 0f
            val light = (0.19f + distanceLight * 0.69f + warmGlow) * lightFlicker
            val wallColor = Color(
                red = (base[0] * light).roundToInt().coerceIn(0, 255),
                green = (base[1] * light).roundToInt().coerceIn(0, 255),
                blue = (base[2] * light).roundToInt().coerceIn(0, 255),
            )
            drawRect(
                color = wallColor,
                topLeft = Offset(ray * sliceWidth, top),
                size = Size(ceil(sliceWidth) + 1.25f, bottom - top),
            )
            // Low, broad baseboard band grounds the wall against the floor, not a raster seam.
            val baseboardHeight = (focal * 0.105f / correctedDistance).coerceIn(0.8f, h * 0.12f)
            drawRect(
                color = Color(0xFF231B1B).copy(alpha = (0.42f * distanceLight).coerceIn(0.05f, 0.42f)),
                topLeft = Offset(ray * sliceWidth, bottom - baseboardHeight),
                size = Size(ceil(sliceWidth) + 1.25f, baseboardHeight),
            )
        }

        // Render in depth order, far objects first. The depth buffer stops props appearing
        // through walls and gives furniture a stable floor/wall anchor.
        val objectItems = ApartmentObject.entries.mapNotNull { obj ->
            val projection = projectObject(
                x = obj.x,
                y = obj.y,
                worldWidth = objectDimensions(obj).first,
                worldHeight = objectDimensions(obj).second,
                centerZ = objectCenterHeight(obj),
                playerX = playerX,
                playerY = playerY,
                yaw = yaw,
                focal = focal,
                horizon = horizon,
                screenWidth = w,
            ) ?: return@mapNotNull null
            RenderItem(
                depth = projection.depth,
                screenX = projection.screenX,
                top = projection.top,
                width = projection.width,
                height = projection.height,
                objectValue = obj,
                propValue = null,
            )
        }
        val propItems = ROOM_PROPS.mapNotNull { prop ->
            val projection = projectObject(
                x = prop.x,
                y = prop.y,
                worldWidth = prop.width,
                worldHeight = prop.height,
                centerZ = prop.centerZ,
                playerX = playerX,
                playerY = playerY,
                yaw = yaw,
                focal = focal,
                horizon = horizon,
                screenWidth = w,
            ) ?: return@mapNotNull null
            RenderItem(
                depth = projection.depth,
                screenX = projection.screenX,
                top = projection.top,
                width = projection.width,
                height = projection.height,
                objectValue = null,
                propValue = prop,
            )
        }

        val anomalyObject = anomalyForNight(night)
        (objectItems + propItems).sortedByDescending { it.depth }.forEach { item ->
            if (item.screenX < -w * 0.45f || item.screenX > w * 1.45f) return@forEach
            val rayIndex = ((item.screenX / w) * rayCount).toInt().coerceIn(0, rayCount - 1)
            if (item.depth > wallDistances[rayIndex] + 0.08f) return@forEach
            val left = item.screenX - item.width / 2f
            val bottom = item.top + item.height
            if (item.objectValue != null) {
                val obj = item.objectValue
                drawObjectBillboard(
                    obj = obj,
                    centerX = item.screenX,
                    top = item.top,
                    width = item.width,
                    height = item.height,
                    isTarget = obj.id == nearbyObjectId,
                    isAnomaly = anomalyDetected && obj == anomalyObject,
                    flicker = if (reduceFlashes) 0.98f else lightFlicker,
                    sprite = sprites[obj.id],
                )
            } else {
                val prop = item.propValue ?: return@forEach
                drawRoomProp(
                    prop = prop,
                    left = left,
                    top = item.top,
                    width = item.width,
                    height = item.height,
                    sprite = sprites[prop.id],
                )
            }
            // Small, local anomaly feedback: no full-screen tearing by default.
            if (item.objectValue != null && item.objectValue == anomalyObject && anomalyDetected) {
                val flashAlpha = if (reduceFlashes) 0.08f else 0.11f + anomalyPulse * 0.12f
                drawRoundRect(
                    color = Color(0xFF9B2639).copy(alpha = flashAlpha),
                    topLeft = Offset(left - item.width * 0.08f, item.top - item.height * 0.06f),
                    size = Size(item.width * 1.16f, item.height * 1.12f),
                    cornerRadius = CornerRadius(max(3f, item.width * 0.035f)),
                )
                if (!reduceFlashes) {
                    val tearY = item.top + item.height * (0.33f + anomalyPulse * 0.12f)
                    drawRect(
                        color = Color(0xFFCE5261).copy(alpha = 0.12f),
                        topLeft = Offset(left - item.width * 0.04f, tearY),
                        size = Size(item.width * 1.08f, max(1f, item.height * 0.012f)),
                    )
                }
            }
        }

        // Flashlight falloff: clear centre, rich shadows at the edge. It never draws grid lines.
        if (flashlightOn) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x18020408),
                        Color(0x7103070D),
                        Color(0xC9000207),
                    ),
                    center = Offset(w * 0.50f, horizon + h * 0.15f),
                    radius = max(w, h) * (if (anomalyDetected) 0.59f else 0.78f) * lightFlicker,
                ),
                size = size,
            )
        } else {
            drawRect(Color(0xB6080A0F), size = size)
        }

        // Cinematic vignette and a few drifting dust motes; reduced-flash mode removes motion.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x5E000000), Color(0xB9000000)),
                center = Offset(w * 0.50f, h * 0.47f),
                radius = max(w, h) * 0.82f,
            ),
            size = size,
        )
        if (!reduceFlashes) {
            repeat(9) { i ->
                val x = (w * ((i * 0.173f + lightFlicker * 0.025f) % 1f))
                val y = h * (0.22f + ((i * 0.137f + lightFlicker * 0.018f) % 0.62f))
                drawCircle(Color(0x45D8C8B6), radius = if (i % 3 == 0) 1.3.dp.toPx() else 0.8.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}

private data class Projection(
    val depth: Float,
    val screenX: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

private data class RenderItem(
    val depth: Float,
    val screenX: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val objectValue: ApartmentObject?,
    val propValue: RoomProp?,
)

private fun projectObject(
    x: Float,
    y: Float,
    worldWidth: Float,
    worldHeight: Float,
    centerZ: Float,
    playerX: Float,
    playerY: Float,
    yaw: Float,
    focal: Float,
    horizon: Float,
    screenWidth: Float,
): Projection? {
    val dx = (x - playerX) / 5f
    val dy = (y - playerY) / 5f
    val forward = dx * sin(yaw) - dy * cos(yaw)
    if (forward <= 0.16f) return null
    val side = dx * cos(yaw) + dy * sin(yaw)
    val depth = max(0.16f, forward)
    val screenX = screenWidth * 0.5f + (side / depth) * focal
    val height = min(worldHeight * focal / depth, 1.9f * focal)
    val width = min(worldWidth * focal / depth, screenWidth * 1.25f)
    val centerY = horizon + focal * (CAMERA_HEIGHT - centerZ) / depth
    return Projection(depth, screenX, centerY - height / 2f, width, height)
}

private fun objectDimensions(obj: ApartmentObject): Pair<Float, Float> = when (obj) {
    ApartmentObject.FrontDoor -> 1.18f to 2.18f
    ApartmentObject.Lamp -> 0.46f to 0.48f
    ApartmentObject.Window -> 2.28f to 1.28f
    ApartmentObject.Mirror -> 0.82f to 1.62f
    ApartmentObject.Radio -> 0.72f to 0.48f
    ApartmentObject.Clock -> 0.47f to 0.47f
    ApartmentObject.FamilyPhoto -> 0.58f to 0.68f
    ApartmentObject.Phone -> 0.30f to 0.20f
    ApartmentObject.Closet -> 1.16f to 2.12f
    ApartmentObject.Bed -> 1.86f to 0.76f
}

private fun objectCenterHeight(obj: ApartmentObject): Float = when (obj) {
    ApartmentObject.FrontDoor -> 1.09f
    ApartmentObject.Lamp -> 1.72f
    ApartmentObject.Window -> 1.42f
    ApartmentObject.Mirror -> 0.81f
    ApartmentObject.Radio -> 0.93f
    ApartmentObject.Clock -> 1.72f
    ApartmentObject.FamilyPhoto -> 1.50f
    ApartmentObject.Phone -> 0.78f
    ApartmentObject.Closet -> 1.06f
    ApartmentObject.Bed -> 0.39f
}

@Composable
private fun rememberSceneSprites(): Map<String, ImageBitmap> {
    val context = LocalContext.current
    return remember(context) {
        SPRITE_NAMES.mapNotNull { name ->
            val bitmap = runCatching {
                context.assets.open("$OBJECT_ASSET_ROOT$name.webp").use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
            if (bitmap == null) null else name to bitmap
        }.toMap()
    }
}

private val SPRITE_NAMES = listOf(
    "front_door", "lamp", "window", "mirror", "radio", "clock", "family_photo", "phone", "closet", "bed",
    "sofa", "coffee_table", "bookshelf", "tv_stand", "television", "plant", "sideboard", "dining_table",
    "chair", "kitchen_counter", "refrigerator", "wall_art", "radiator", "bedside_table", "ceiling_light",
)

private fun DrawScope.drawLivingRoomRug(
    playerX: Float,
    playerY: Float,
    yaw: Float,
    focal: Float,
    horizon: Float,
    screenWidth: Float,
    screenHeight: Float,
) {
    val corners = listOf(19f to 31f, 53f to 31f, 53f to 44f, 19f to 44f)
    val points = corners.mapNotNull { (x, y) ->
        val dx = (x - playerX) / 5f
        val dy = (y - playerY) / 5f
        val forward = dx * sin(yaw) - dy * cos(yaw)
        val side = dx * cos(yaw) + dy * sin(yaw)
        if (forward <= 0.16f) null else Offset(
            x = screenWidth * 0.5f + (side / forward) * focal,
            y = horizon + focal * CAMERA_HEIGHT / forward,
        )
    }
    if (points.size != 4) return
    val rugPath = Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(rugPath, Brush.linearGradient(listOf(Color(0xFF3B2028), Color(0xFF241920), Color(0xFF332129))))
    drawPath(rugPath, Color(0xFF8B5C50).copy(alpha = 0.27f), style = Stroke(width = 1.5.dp.toPx()))
    // No floor grid or repetitive rug weave is drawn; this avoids fine-line shimmer while moving.
}

private enum class PropStyle {
    Sofa, CoffeeTable, Bookshelf, TvStand, Television, Plant, Sideboard, DiningTable, Chair,
    KitchenCounter, Refrigerator, WallArt, Radiator, BedsideTable, CeilingLight,
}

private data class RoomProp(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val centerZ: Float,
    val style: PropStyle,
)

private val ROOM_PROPS = listOf(
    RoomProp("bookshelf", 11.5f, 30f, 1.30f, 2.03f, 1.015f, PropStyle.Bookshelf),
    RoomProp("sofa", 22.5f, 41f, 2.52f, 1.08f, 0.54f, PropStyle.Sofa),
    RoomProp("coffee_table", 35.2f, 38.0f, 1.78f, 0.53f, 0.265f, PropStyle.CoffeeTable),
    RoomProp("tv_stand", 48.0f, 29.2f, 1.92f, 0.70f, 0.35f, PropStyle.TvStand),
    RoomProp("television", 48.0f, 28.4f, 1.42f, 0.89f, 1.13f, PropStyle.Television),
    RoomProp("plant", 57.0f, 31.0f, 0.70f, 1.25f, 0.625f, PropStyle.Plant),
    RoomProp("wall_art", 19.0f, 14.5f, 0.62f, 0.74f, 1.52f, PropStyle.WallArt),
    RoomProp("wall_art", 27.0f, 14.5f, 0.58f, 0.70f, 1.53f, PropStyle.WallArt),
    RoomProp("radiator", 47.0f, 20.5f, 1.70f, 0.46f, 0.30f, PropStyle.Radiator),
    RoomProp("sideboard", 56.0f, 51.0f, 1.44f, 0.82f, 0.41f, PropStyle.Sideboard),
    RoomProp("dining_table", 83.0f, 34.2f, 2.08f, 0.76f, 0.38f, PropStyle.DiningTable),
    RoomProp("chair", 78.0f, 36.5f, 0.66f, 0.86f, 0.43f, PropStyle.Chair),
    RoomProp("chair", 88.0f, 36.0f, 0.66f, 0.86f, 0.43f, PropStyle.Chair),
    RoomProp("kitchen_counter", 86.0f, 22.5f, 2.10f, 0.98f, 0.49f, PropStyle.KitchenCounter),
    RoomProp("refrigerator", 92.0f, 29.0f, 0.84f, 2.10f, 1.05f, PropStyle.Refrigerator),
    RoomProp("bedside_table", 27.0f, 77.5f, 0.72f, 0.74f, 0.37f, PropStyle.BedsideTable),
    RoomProp("wall_art", 11.4f, 70.0f, 0.70f, 0.74f, 1.51f, PropStyle.WallArt),
    RoomProp("ceiling_light", 36.0f, 27.0f, 0.74f, 0.33f, 2.44f, PropStyle.CeilingLight),
)

private fun DrawScope.drawRoomProp(
    prop: RoomProp,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    sprite: ImageBitmap?,
) {
    if (width < 1f || height < 1f) return
    val base = top + height
    if (sprite != null) {
        drawImage(
            image = sprite,
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1)),
        )
        return
    }
    when (prop.style) {
        PropStyle.Sofa -> {
            drawRoundRect(Color(0xFF171316).copy(alpha = 0.65f), Offset(left - width * 0.06f, base - height * 0.04f), Size(width * 1.12f, height * 0.12f), CornerRadius(width * .08f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF49353A), Color(0xFF241A20), Color(0xFF161317)), top, base), Offset(left, top + height * .20f), Size(width, height * .73f), CornerRadius(width * .075f))
            drawRoundRect(Color(0xFF574047), Offset(left + width * .06f, top + height * .02f), Size(width * .88f, height * .44f), CornerRadius(width * .07f))
            drawRoundRect(Color(0xFF3A2A30), Offset(left + width * .09f, top + height * .35f), Size(width * .38f, height * .33f), CornerRadius(width * .04f))
            drawRoundRect(Color(0xFF30242A), Offset(left + width * .52f, top + height * .35f), Size(width * .37f, height * .33f), CornerRadius(width * .04f))
            drawRoundRect(Color(0xFF1C171B), Offset(left, top + height * .48f), Size(width * .12f, height * .39f), CornerRadius(width * .04f))
            drawRoundRect(Color(0xFF1C171B), Offset(left + width * .88f, top + height * .48f), Size(width * .12f, height * .39f), CornerRadius(width * .04f))
        }
        PropStyle.CoffeeTable -> {
            drawRoundRect(Color(0xFF171213), Offset(left + width * .12f, top + height * .42f), Size(width * .76f, height * .52f), CornerRadius(width * .025f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF684737), Color(0xFF3D2A24)), top, top + height * .32f), Offset(left, top + height * .14f), Size(width, height * .28f), CornerRadius(width * .04f))
            drawRect(Color(0xFF8A624B).copy(alpha = .55f), Offset(left + width * .13f, top + height * .22f), Size(width * .34f, height * .018f))
            drawRoundRect(Color(0xFF6D5C4C), Offset(left + width * .23f, top + height * .02f), Size(width * .26f, height * .11f), CornerRadius(width * .02f))
            drawRoundRect(Color(0xFF202124), Offset(left + width * .70f, top - height * .015f), Size(width * .09f, height * .13f), CornerRadius(width * .025f))
            drawRoundRect(Color(0xFFB7A18B), Offset(left + width * .52f, top + height * .065f), Size(width * .15f, height * .07f), CornerRadius(width * .018f))
        }
        PropStyle.Bookshelf -> {
            drawRect(Color(0xFF1B1312), Offset(left - width * .04f, top + height * .025f), Size(width * 1.08f, height * .96f))
            drawRect(Brush.horizontalGradient(listOf(Color(0xFF704A35), Color(0xFF38251F), Color(0xFF201716)), left, left + width), Offset(left, top), Size(width, height))
            drawRect(Color(0xFF181315), Offset(left + width * .08f, top + height * .06f), Size(width * .84f, height * .88f))
            for (row in 0..3) {
                val y = top + height * (0.20f + row * 0.19f)
                drawRect(Color(0xFF674635), Offset(left + width * .08f, y), Size(width * .84f, height * .035f))
                val bookColors = listOf(Color(0xFF6C3D39), Color(0xFF9A7956), Color(0xFF344246), Color(0xFF5A4B3E), Color(0xFF7B5A45))
                for (book in 0..4) {
                    val bx = left + width * (0.13f + book * 0.145f)
                    val bh = height * (0.10f + ((book + row) % 3) * 0.018f)
                    drawRect(bookColors[(book + row) % bookColors.size], Offset(bx, y - bh), Size(width * .105f, bh))
                    drawRect(Color(0xFF0F1013).copy(alpha = .25f), Offset(bx + width * .085f, y - bh), Size(width * .02f, bh))
                }
            }
            drawRect(Color(0xFF8B6245), Offset(left, top), Size(width, height * .035f))
            drawRect(Color(0xFF8B6245), Offset(left, base - height * .035f), Size(width, height * .035f))
        }
        PropStyle.TvStand -> {
            drawRect(Color(0xFF171314), Offset(left + width * .06f, top + height * .55f), Size(width * .88f, height * .42f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF5B3D2E), Color(0xFF291D19)), top, base), Offset(left, top + height * .30f), Size(width, height * .32f), CornerRadius(width * .025f))
            drawRect(Color(0xFF76503A), Offset(left + width * .05f, top + height * .34f), Size(width * .9f, height * .035f))
            drawLine(Color(0xFF120F10), Offset(left + width * .5f, top + height * .39f), Offset(left + width * .5f, top + height * .60f), max(1f, width * .008f))
            drawCircle(Color(0xFFB39A6D), width * .018f, Offset(left + width * .25f, top + height * .47f))
            drawCircle(Color(0xFFB39A6D), width * .018f, Offset(left + width * .73f, top + height * .47f))
        }
        PropStyle.Television -> {
            drawRoundRect(Color(0xFF090B0F), Offset(left, top), Size(width, height * .88f), CornerRadius(width * .04f))
            drawRoundRect(Brush.linearGradient(listOf(Color(0xFF26343B), Color(0xFF101820), Color(0xFF080B11)), Offset(left, top), Offset(left + width, top + height)), Offset(left + width * .045f, top + height * .05f), Size(width * .91f, height * .78f), CornerRadius(width * .025f))
            drawRect(Color(0xFF78909A).copy(alpha = .14f), Offset(left + width * .11f, top + height * .14f), Size(width * .34f, height * .025f))
            drawLine(Color(0xFF4E6470).copy(alpha = .3f), Offset(left + width * .62f, top + height * .08f), Offset(left + width * .40f, top + height * .72f), max(1f, width * .009f))
            drawRect(Color(0xFF090A0D), Offset(left + width * .43f, top + height * .87f), Size(width * .14f, height * .08f))
            drawRect(Color(0xFF27272B), Offset(left + width * .28f, base - height * .025f), Size(width * .44f, height * .025f))
        }
        PropStyle.Plant -> {
            drawRoundRect(Color(0xFF32201B), Offset(left + width * .22f, top + height * .67f), Size(width * .56f, height * .28f), CornerRadius(width * .05f))
            drawOval(Color(0xFF1D342A), Offset(left + width * .08f, top + height * .13f), Size(width * .55f, height * .50f))
            drawOval(Color(0xFF274437), Offset(left + width * .38f, top + height * .04f), Size(width * .50f, height * .55f))
            drawOval(Color(0xFF355641), Offset(left + width * .35f, top + height * .32f), Size(width * .42f, height * .45f))
            drawOval(Color(0xFF152A22), Offset(left + width * .03f, top + height * .37f), Size(width * .48f, height * .42f))
            drawLine(Color(0xFF65775A).copy(alpha = .6f), Offset(left + width * .50f, top + height * .56f), Offset(left + width * .58f, top + height * .18f), max(1f, width * .018f))
        }
        PropStyle.Sideboard -> {
            drawRect(Color(0xFF211716), Offset(left + width * .04f, top + height * .20f), Size(width * .92f, height * .73f))
            drawRect(Brush.verticalGradient(listOf(Color(0xFF624332), Color(0xFF34231F)), top, top + height * .32f), Offset(left, top + height * .18f), Size(width, height * .18f))
            for (i in 0..2) {
                val x = left + width * (0.08f + i * .29f)
                drawRect(Color(0xFF181315), Offset(x, top + height * .42f), Size(width * .25f, height * .39f))
                drawCircle(Color(0xFFB49B70), width * .015f, Offset(x + width * .21f, top + height * .61f))
            }
        }
        PropStyle.DiningTable -> {
            drawRect(Color(0xFF241915), Offset(left + width * .10f, top + height * .50f), Size(width * .80f, height * .44f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF74513A), Color(0xFF3A2922)), top, base), Offset(left, top + height * .26f), Size(width, height * .30f), CornerRadius(width * .035f))
            drawRect(Color(0xFF9A6A49).copy(alpha = .42f), Offset(left + width * .10f, top + height * .34f), Size(width * .80f, height * .022f))
            drawRoundRect(Color(0xFF242329), Offset(left + width * .23f, top + height * .12f), Size(width * .16f, height * .12f), CornerRadius(width * .03f))
            drawRoundRect(Color(0xFF81715C), Offset(left + width * .61f, top + height * .16f), Size(width * .12f, height * .08f), CornerRadius(width * .03f))
        }
        PropStyle.Chair -> {
            drawRect(Color(0xFF32221D), Offset(left + width * .13f, top + height * .16f), Size(width * .74f, height * .52f))
            drawRoundRect(Color(0xFF6E4A37), Offset(left + width * .05f, top + height * .44f), Size(width * .90f, height * .17f), CornerRadius(width * .03f))
            drawRect(Color(0xFF241A18), Offset(left + width * .16f, top + height * .58f), Size(width * .12f, height * .39f))
            drawRect(Color(0xFF241A18), Offset(left + width * .72f, top + height * .58f), Size(width * .12f, height * .39f))
        }
        PropStyle.KitchenCounter -> {
            drawRect(Color(0xFF211918), Offset(left + width * .04f, top + height * .18f), Size(width * .92f, height * .80f))
            drawRect(Brush.horizontalGradient(listOf(Color(0xFF69564A), Color(0xFFB3A18A), Color(0xFF5A4940)), left, left + width), Offset(left, top + height * .12f), Size(width, height * .17f))
            drawRect(Color(0xFF3B3431), Offset(left + width * .12f, top + height * .42f), Size(width * .34f, height * .42f))
            drawRect(Color(0xFF3B3431), Offset(left + width * .53f, top + height * .42f), Size(width * .34f, height * .42f))
            drawCircle(Color(0xFFB7A28C), width * .012f, Offset(left + width * .42f, top + height * .62f))
            drawCircle(Color(0xFFB7A28C), width * .012f, Offset(left + width * .82f, top + height * .62f))
        }
        PropStyle.Refrigerator -> {
            drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF7F8180), Color(0xFF383D41), Color(0xFF23262B)), left, left + width), Offset(left, top), Size(width, height), CornerRadius(width * .04f))
            drawLine(Color(0xFF181A1D), Offset(left + width * .05f, top + height * .42f), Offset(left + width * .95f, top + height * .42f), max(1f, width * .012f))
            drawLine(Color(0xFFB3B5B0).copy(alpha = .48f), Offset(left + width * .80f, top + height * .10f), Offset(left + width * .80f, top + height * .34f), max(1f, width * .025f))
            drawLine(Color(0xFFB3B5B0).copy(alpha = .42f), Offset(left + width * .80f, top + height * .52f), Offset(left + width * .80f, top + height * .84f), max(1f, width * .025f))
        }
        PropStyle.WallArt -> {
            drawRect(Color(0xFF1D1716), Offset(left - width * .06f, top - height * .06f), Size(width * 1.12f, height * 1.12f))
            drawRect(Color(0xFF80674E), Offset(left, top), Size(width, height))
            drawRect(Color(0xFF27272D), Offset(left + width * .09f, top + height * .09f), Size(width * .82f, height * .82f))
            drawCircle(Color(0xFF9C8274).copy(alpha = .7f), width * .14f, Offset(left + width * .54f, top + height * .34f))
            drawOval(Color(0xFF5A4444), Offset(left + width * .28f, top + height * .45f), Size(width * .47f, height * .36f))
        }
        PropStyle.Radiator -> {
            drawRoundRect(Color(0xFF292D31), Offset(left, top), Size(width, height), CornerRadius(width * .035f))
            for (i in 0..7) {
                val x = left + width * (0.06f + i * .12f)
                drawRoundRect(Color(0xFF6D6C67).copy(alpha = .65f), Offset(x, top + height * .08f), Size(width * .055f, height * .82f), CornerRadius(width * .015f))
            }
        }
        PropStyle.BedsideTable -> {
            drawRect(Color(0xFF211716), Offset(left + width * .08f, top + height * .26f), Size(width * .84f, height * .65f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF76523A), Color(0xFF39271F)), top, top + height * .35f), Offset(left, top + height * .18f), Size(width, height * .18f), CornerRadius(width * .03f))
            drawRect(Color(0xFF171214), Offset(left + width * .12f, top + height * .48f), Size(width * .76f, height * .27f))
            drawCircle(Color(0xFFBEA06B), width * .025f, Offset(left + width * .77f, top + height * .60f))
        }
        PropStyle.CeilingLight -> {
            drawCircle(Color(0x20F5D49B), width * .75f, Offset(left + width / 2f, top + height * .55f))
            drawRoundRect(Color(0xFF372A24), Offset(left + width * .28f, top + height * .36f), Size(width * .44f, height * .31f), CornerRadius(width * .12f))
            drawRoundRect(Color(0xFFBCA47B).copy(alpha = .82f), Offset(left + width * .36f, top + height * .41f), Size(width * .28f, height * .20f), CornerRadius(width * .10f))
        }
    }
}

private fun DrawScope.drawObjectBillboard(
    obj: ApartmentObject,
    centerX: Float,
    top: Float,
    width: Float,
    height: Float,
    isTarget: Boolean,
    isAnomaly: Boolean,
    flicker: Float,
    sprite: ImageBitmap?,
) {
    if (width < 1f || height < 1f) return
    val left = centerX - width / 2f
    val base = top + height
    val outline = if (isAnomaly) Color(0xFFAF3449) else Color(0xFF9B8A7E)
    if (sprite != null) {
        drawImage(
            image = sprite,
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1)),
        )
    } else {
        when (obj) {
            ApartmentObject.FrontDoor -> {
                drawRoundRect(Color(0xFF1A1211), Offset(left - width * .045f, top - height * .015f), Size(width * 1.09f, height * 1.03f), CornerRadius(width * .025f))
                drawRect(Brush.horizontalGradient(listOf(Color(0xFF65412F), Color(0xFF38251F), Color(0xFF231817)), left, left + width), Offset(left, top), Size(width, height))
                drawRect(Color(0xFF8A6246), Offset(left + width * .09f, top + height * .06f), Size(width * .82f, height * .025f))
                drawRoundRect(Color(0xFF241A17), Offset(left + width * .16f, top + height * .14f), Size(width * .68f, height * .30f), CornerRadius(width * .025f), style = Stroke(max(1f, width * .025f)))
                drawRoundRect(Color(0xFF241A17), Offset(left + width * .16f, top + height * .51f), Size(width * .68f, height * .29f), CornerRadius(width * .025f), style = Stroke(max(1f, width * .025f)))
                drawCircle(Color(0xFFC3A16B), width * .045f, Offset(left + width * .79f, top + height * .57f))
                drawCircle(Color(0xFF0D0C0E), width * .026f, Offset(left + width * .50f, top + height * .13f))
                drawRect(Color(0xFF221917), Offset(left + width * .035f, top), Size(width * .055f, height))
            }
            ApartmentObject.Mirror -> {
                drawRoundRect(Color(0xFF231716), Offset(left - width * .09f, top - height * .025f), Size(width * 1.18f, height * 1.05f), CornerRadius(width * .12f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF6A7778), Color(0xFF26353A), Color(0xFF10161C)), top, base), Offset(left, top), Size(width, height), CornerRadius(width * .09f))
                drawRoundRect(Color(0xFF9B7A5C), Offset(left + width * .075f, top + height * .035f), Size(width * .85f, height * .93f), CornerRadius(width * .07f), style = Stroke(max(1f, width * .055f)))
                drawOval(Color(0xFF12151A).copy(alpha = if (isAnomaly) .92f else .28f), Offset(left + width * .27f, top + height * .22f), Size(width * .46f, height * .63f))
                drawCircle(Color(0xFF080A0D).copy(alpha = if (isAnomaly) .95f else .18f), width * .20f, Offset(centerX, top + height * .30f))
                if (isAnomaly) {
                    val eyeY = top + height * .34f + (1f - flicker) * height * .15f
                    drawCircle(Color(0xFFB54454).copy(alpha = .35f + flicker * .45f), max(1f, width * .035f), Offset(left + width * .42f, eyeY))
                    drawCircle(Color(0xFFB54454).copy(alpha = .35f + flicker * .45f), max(1f, width * .035f), Offset(left + width * .59f, eyeY))
                    drawLine(Color(0xFFB94655).copy(alpha = .42f), Offset(left + width * .19f, top + height * .62f), Offset(left + width * .74f, top + height * .72f), max(1f, width * .018f))
                }
                drawLine(Color(0xFFD9E4E2).copy(alpha = .18f), Offset(left + width * .19f, top + height * .12f), Offset(left + width * .42f, top + height * .32f), max(1f, width * .035f))
            }
            ApartmentObject.Window -> drawWindowWithCity(left, top, width, height, flicker, isAnomaly)
            ApartmentObject.Lamp -> {
                drawRoundRect(Color(0xFF372823), Offset(left + width * .30f, top + height * .18f), Size(width * .40f, height * .28f), CornerRadius(width * .07f))
                drawRect(Color(0xFF8F6C4E), Offset(left + width * .44f, top + height * .40f), Size(width * .12f, height * .20f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFF0D9A2), Color(0xFFBE8E52)), top + height * .35f, base), Offset(left, top + height * .40f), Size(width, height * .35f), CornerRadius(width * .14f))
                drawCircle(Color(0x38F5D79C).copy(alpha = flicker), width * .72f, Offset(centerX, top + height * .58f))
                drawRoundRect(Color(0xFF3C2922), Offset(left + width * .12f, top + height * .72f), Size(width * .76f, height * .14f), CornerRadius(width * .08f))
            }
            ApartmentObject.Radio -> {
                drawRoundRect(Color(0xFF211A18), Offset(left, top + height * .10f), Size(width, height * .84f), CornerRadius(width * .08f))
                drawRoundRect(Color(0xFF6C4632), Offset(left + width * .06f, top + height * .17f), Size(width * .88f, height * .68f), CornerRadius(width * .055f))
                drawRoundRect(Color(0xFF15171B), Offset(left + width * .14f, top + height * .24f), Size(width * .39f, height * .48f), CornerRadius(width * .04f))
                drawCircle(Color(0xFF1C2024), width * .12f, Offset(left + width * .70f, top + height * .45f))
                drawCircle(Color(0xFF82715D), width * .055f, Offset(left + width * .70f, top + height * .45f), style = Stroke(max(1f, width * .02f)))
                drawRect(Color(0xFFAE4D50).copy(alpha = if (isAnomaly) flicker else .55f), Offset(left + width * .18f, top + height * .29f), Size(width * .28f, height * .025f))
            }
            ApartmentObject.Clock -> {
                drawCircle(Color(0xFF241A17), width * .49f, Offset(centerX, top + height * .5f))
                drawCircle(Color(0xFFC8B99D), width * .405f, Offset(centerX, top + height * .5f))
                drawCircle(Color(0xFF39312A), width * .36f, Offset(centerX, top + height * .5f), style = Stroke(max(1f, width * .018f)))
                for (mark in 0..11) {
                    val angle = mark * (Math.PI / 6.0)
                    val rx = cos(angle).toFloat()
                    val ry = sin(angle).toFloat()
                    drawCircle(Color(0xFF43352C), max(0.7f, width * .012f), Offset(centerX + rx * width * .29f, top + height * .5f + ry * height * .29f))
                }
                drawLine(Color(0xFF29221D), Offset(centerX, top + height * .5f), Offset(centerX, top + height * .27f), max(1f, width * .025f))
                drawLine(Color(0xFF29221D), Offset(centerX, top + height * .5f), Offset(centerX + width * .20f, top + height * .59f), max(1f, width * .023f))
                drawCircle(Color(0xFF251E1B), width * .035f, Offset(centerX, top + height * .5f))
            }
            ApartmentObject.FamilyPhoto -> {
                drawRect(Color(0xFF241B18), Offset(left - width * .04f, top - height * .03f), Size(width * 1.08f, height * 1.06f))
                drawRect(Color(0xFF8E6C4B), Offset(left, top), Size(width, height))
                drawRect(Color(0xFF21191A), Offset(left + width * .11f, top + height * .10f), Size(width * .78f, height * .80f))
                drawRect(Brush.verticalGradient(listOf(Color(0xFF3D4445), Color(0xFF202428)), top, base), Offset(left + width * .17f, top + height * .16f), Size(width * .66f, height * .67f))
                drawCircle(Color(0xFF9B8A7D), width * .12f, Offset(left + width * .40f, top + height * .35f))
                drawCircle(Color(0xFF87756B), width * .10f, Offset(left + width * .61f, top + height * .37f))
                drawOval(Color(0xFF5D5551), Offset(left + width * .30f, top + height * .45f), Size(width * .23f, height * .25f))
                drawOval(Color(0xFF40363A), Offset(left + width * .52f, top + height * .47f), Size(width * .22f, height * .24f))
                if (isAnomaly) drawCircle(Color(0xFFBD3449).copy(alpha = .58f * flicker), width * .045f, Offset(left + width * .68f, top + height * .34f))
            }
            ApartmentObject.Phone -> {
                drawRoundRect(Color(0xFF0A0B0E), Offset(left, top), Size(width, height), CornerRadius(width * .12f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF32434C), Color(0xFF111820)), top, base), Offset(left + width * .08f, top + height * .08f), Size(width * .84f, height * .82f), CornerRadius(width * .08f))
                drawRect(Color(0xFFB0C7CA).copy(alpha = if (isAnomaly || isTarget) .68f else .22f), Offset(left + width * .18f, top + height * .22f), Size(width * .64f, height * .04f))
                drawCircle(Color(0xFFE4DBD4).copy(alpha = .65f), width * .035f, Offset(centerX, base - height * .07f))
            }
            ApartmentObject.Closet -> {
                drawRect(Color(0xFF211817), Offset(left - width * .03f, top - height * .01f), Size(width * 1.06f, height * 1.02f))
                drawRect(Brush.horizontalGradient(listOf(Color(0xFF654431), Color(0xFF38241E), Color(0xFF4A3025)), left, left + width), Offset(left, top), Size(width, height))
                drawRect(Color(0xFF211818), Offset(left + width * .48f, top + height * .03f), Size(width * .045f, height * .94f))
                drawRoundRect(Color(0xFF120F11), Offset(left + width * .50f, top + height * .13f), Size(width * .43f, height * .74f), CornerRadius(width * .025f))
                drawRoundRect(Color(0xFF4E382E), Offset(left + width * .53f, top + height * .15f), Size(width * .37f, height * .70f), CornerRadius(width * .02f))
                for (i in 0..3) {
                    val x = left + width * (.57f + i * .075f)
                    drawLine(Color(0xFF8E6A4D).copy(alpha = .55f), Offset(x, top + height * .20f), Offset(x, top + height * .70f), max(1f, width * .018f))
                }
                drawCircle(Color(0xFFC2A070), width * .025f, Offset(left + width * .43f, top + height * .57f))
            }
            ApartmentObject.Bed -> {
                drawRoundRect(Color(0xFF1A1518), Offset(left, top + height * .19f), Size(width, height * .75f), CornerRadius(width * .055f))
                drawRoundRect(Color(0xFF5B3F45), Offset(left, top + height * .10f), Size(width, height * .28f), CornerRadius(width * .06f))
                drawRoundRect(Color(0xFFB4A49A), Offset(left + width * .06f, top + height * .20f), Size(width * .38f, height * .23f), CornerRadius(width * .06f))
                drawRoundRect(Color(0xFF9E8F87), Offset(left + width * .48f, top + height * .20f), Size(width * .36f, height * .23f), CornerRadius(width * .06f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF4D343D), Color(0xFF2D2028)), top + height * .39f, base), Offset(left + width * .04f, top + height * .39f), Size(width * .92f, height * .48f), CornerRadius(width * .04f))
                if (isAnomaly) drawOval(Color(0xFF090A0D).copy(alpha = .45f + flicker * .20f), Offset(left + width * .46f, top + height * .44f), Size(width * .34f, height * .25f))
            }
        }
    }
    if (isTarget || isAnomaly) {
        drawRoundRect(
            color = outline.copy(alpha = if (isAnomaly) 0.72f else 0.52f),
            topLeft = Offset(left - width * .055f, top - height * .045f),
            size = Size(width * 1.11f, height * 1.09f),
            cornerRadius = CornerRadius(max(2f, width * .025f)),
            style = Stroke(width = if (isTarget) 1.25.dp.toPx() else 1.8.dp.toPx()),
        )
    }
}

private fun DrawScope.drawWindowWithCity(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    flicker: Float,
    anomaly: Boolean,
) {
    // Thick wooden frame and sill physically attach the window to the apartment wall.
    drawRoundRect(Color(0xFF1B1413), Offset(left - width * .075f, top - height * .08f), Size(width * 1.15f, height * 1.18f), CornerRadius(width * .035f))
    drawRect(Brush.verticalGradient(listOf(Color(0xFF101D30), Color(0xFF263B50), Color(0xFF0A111C)), top, top + height), Offset(left, top), Size(width, height))
    // Moon and distant skyline visible beyond the glass.
    drawCircle(Color(0xFFD7E0E5).copy(alpha = if (anomaly) .5f else .88f), width * .055f, Offset(left + width * .73f, top + height * .19f))
    val buildings = listOf(
        Triple(.03f, .27f, .58f), Triple(.21f, .18f, .43f), Triple(.40f, .26f, .70f),
        Triple(.68f, .16f, .52f), Triple(.82f, .17f, .63f),
    )
    buildings.forEachIndexed { index, building ->
        val bx = left + width * building.first
        val bw = width * building.second
        val bh = height * building.third
        val by = top + height - bh
        drawRect(Color(0xFF111923), Offset(bx, by), Size(bw, bh))
        for (row in 0..4) {
            for (col in 0..2) {
                val lit = (row * 3 + col + index) % 4 != 0
                if (lit) {
                    drawRect(
                        color = if ((row + col + index) % 3 == 0) Color(0xFFB99A70).copy(alpha = .52f) else Color(0xFF7290A2).copy(alpha = .37f),
                        topLeft = Offset(bx + bw * (.15f + col * .27f), by + bh * (.12f + row * .15f)),
                        size = Size(max(1f, bw * .095f), max(1f, bh * .055f)),
                    )
                }
            }
        }
    }
    if (anomaly) {
        drawOval(Color(0xFF080A0E).copy(alpha = .65f), Offset(left + width * .40f, top + height * .34f), Size(width * .18f, height * .50f))
        drawCircle(Color(0xFFB53C4D).copy(alpha = .55f * flicker), width * .013f, Offset(left + width * .45f, top + height * .48f))
        drawCircle(Color(0xFFB53C4D).copy(alpha = .55f * flicker), width * .013f, Offset(left + width * .52f, top + height * .48f))
    }
    // Mullions and heavy curtains are structural parts of the window, not rendering artefacts.
    drawRect(Color(0xFF78523B), Offset(left - width * .025f, top - height * .02f), Size(width * 1.05f, height * .055f))
    drawRect(Color(0xFF78523B), Offset(left - width * .025f, top + height * .95f), Size(width * 1.05f, height * .07f))
    drawRect(Color(0xFF4C302D), Offset(left + width * .46f, top), Size(width * .065f, height))
    drawRect(Color(0xFF392027), Offset(left - width * .08f, top - height * .04f), Size(width * .12f, height * 1.10f))
    drawRect(Color(0xFF392027), Offset(left + width * .96f, top - height * .04f), Size(width * .12f, height * 1.10f))
    drawRect(Color(0xFF855B42), Offset(left - width * .08f, top + height * .99f), Size(width * 1.16f, height * .10f))
}

private fun insideApartment(x: Float, y: Float): Boolean {
    val inLivingRoom = x in 9f..61f && y in 13f..62f
    val inKitchen = x in 71f..94f && y in 13f..51f
    val inHallway = x in 49f..78f && y in 38f..68f
    val inBedroom = x in 10f..51f && y in 59f..91f
    val inBathroom = x in 57f..80f && y in 63f..91f
    return inLivingRoom || inKitchen || inHallway || inBedroom || inBathroom
}

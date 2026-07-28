package com.example.dinocompanionapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt



@Composable
fun ColorPicker(
    currentColor: Color,
    onColorChanged: (Color) -> Unit,
    onColorStream: (Color) -> Unit,
    sendColorFinal: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    enableGammaCorrection: Boolean = true
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }
    var isUserInteracting by remember { mutableStateOf(false) }

    // 🔥 NUEVO: guardamos el último color que enviamos nosotros
    var internalColor by remember { mutableStateOf(Color.White) }

    // 🔥 Solo sincronizar si el color externo es diferente al interno
    LaunchedEffect(currentColor) {
        if (!isUserInteracting && currentColor != internalColor) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(currentColor.toArgb(), hsv)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
            internalColor = currentColor
        }
    }
    val wheelColors = remember {
        listOf(
            Color.Red, Color.Yellow, Color.Green, Color.Cyan,
            Color.Blue, Color.Magenta, Color.Red
        )
    }

    fun computeColor(h: Float, s: Float, v: Float) = Color.hsv(h, s, v)

    fun applyGamma(channel: Float): Int {
        return if (enableGammaCorrection) {
            (channel.toDouble().pow(2.2) * 255).toInt().coerceIn(0, 255)
        } else {
            (channel * 255).toInt().coerceIn(0, 255)
        }
    }

    fun notifyUpdate(h: Float, s: Float, v: Float, isFinal: Boolean) {
        val color = computeColor(h, s, v)

        // 🔥 Actualizar UI local siempre
        internalColor = color
        onColorChanged(color)

        // 🔥 Enviar al ESP32 SOLO mediante el canal (con throttling)
        if (isFinal) {
            // En lugar de sendColorFinal, usamos onColorStream (que ya está throttled)
            // Pero necesitamos que el ViewModel sepa que es "final" para persistir
            onColorStream(color)
            // Y también notificamos que es final (para guardar en SharedPreferences)
            // Podemos usar sendColorFinal solo para eso, sin enviar Bluetooth
            sendColorFinal(
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
        } else {
            onColorStream(color)
        }
    }

    val selectedColor = computeColor(hue, saturation, value)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.50f),
                        Color.White.copy(alpha = 0.20f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // --- 1. RUEDA CROMÁTICA ---
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val distanceFromCenter = sqrt(dx * dx + dy * dy)

                            val maxRadius = minOf(centerX, centerY) - 16f
                            val ringThickness = 48f
                            val wheelRadius = maxRadius - (ringThickness / 2f)

                            val minAllowedRadius = wheelRadius - (ringThickness / 2f) - 16f
                            val maxAllowedRadius = wheelRadius + (ringThickness / 2f) + 16f

                            if (distanceFromCenter in minAllowedRadius..maxAllowedRadius) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f

                                hue = angle
                                notifyUpdate(hue, saturation, value, isFinal = true)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        var isDraggingRing = false

                        detectDragGestures(
                            onDragStart = { offset ->
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val dx = offset.x - centerX
                                val dy = offset.y - centerY
                                val distanceFromCenter = sqrt(dx * dx + dy * dy)

                                val maxRadius = minOf(centerX, centerY) - 16f
                                val ringThickness = 48f
                                val wheelRadius = maxRadius - (ringThickness / 2f)

                                val minAllowedRadius = wheelRadius - (ringThickness / 2f) - 16f
                                val maxAllowedRadius = wheelRadius + (ringThickness / 2f) + 16f

                                isDraggingRing = distanceFromCenter in minAllowedRadius..maxAllowedRadius

                                if (isDraggingRing) {
                                    isUserInteracting = true
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f

                                    hue = angle
                                    notifyUpdate(hue, saturation, value, isFinal = false)
                                }
                            },
                            onDragEnd = {
                                if (isDraggingRing) {
                                    notifyUpdate(hue, saturation, value, isFinal = true)
                                    isDraggingRing = false
                                }
                                isUserInteracting = false
                            },
                            onDragCancel = {
                                isDraggingRing = false
                                isUserInteracting = false
                            },
                            onDrag = { change, _ ->
                                if (isDraggingRing) {
                                    change.consume()
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val dx = change.position.x - centerX
                                    val dy = change.position.y - centerY

                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f

                                    hue = angle
                                    notifyUpdate(hue, saturation, value, isFinal = false)
                                }
                            }
                        )
                    }
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val maxRadius = minOf(centerX, centerY) - 16f
                val ringThickness = 48f
                val wheelRadius = maxRadius - (ringThickness / 2f)

                drawCircle(
                    brush = Brush.sweepGradient(wheelColors, center = Offset(centerX, centerY)),
                    radius = wheelRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(ringThickness)
                )

                val innerRadius = wheelRadius - (ringThickness / 2f) - 12f
                if (innerRadius > 0) {
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.20f),
                        radius = innerRadius + 2f,
                        center = Offset(centerX, centerY)
                    )
                    drawCircle(
                        color = selectedColor,
                        radius = innerRadius,
                        center = Offset(centerX, centerY)
                    )
                }

                val angleRad = Math.toRadians(hue.toDouble())
                val cursorX = centerX + wheelRadius * cos(angleRad).toFloat()
                val cursorY = centerY + wheelRadius * sin(angleRad).toFloat()

                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = ringThickness / 2f + 2f,
                    center = Offset(cursorX + 1, cursorY + 1)
                )
                drawCircle(
                    color = Color.White,
                    radius = ringThickness / 2f,
                    center = Offset(cursorX, cursorY)
                )
                drawCircle(
                    color = computeColor(hue, 1f, 1f),
                    radius = (ringThickness / 2f) - 4f,
                    center = Offset(cursorX, cursorY)
                )
            }

            // --- 2. BARRA DE SATURACIÓN ---
            ColorSeekBar(
                value = saturation,
                onValueChange = { newSat ->
                    saturation = newSat
                    notifyUpdate(hue, saturation, value, isFinal = false)
                },
                onValueFinal = { newSat ->
                    saturation = newSat
                    notifyUpdate(hue, saturation, value, isFinal = true)
                },
                onInteractionStateChange = { active -> isUserInteracting = active },
                gradientBrush = Brush.horizontalGradient(
                    listOf(
                        computeColor(hue, 0f, value),
                        computeColor(hue, 1f, value)
                    )
                )
            )

            // --- 3. BARRA DE BRILLO ---
            ColorSeekBar(
                value = value,
                onValueChange = { newVal ->
                    value = newVal
                    notifyUpdate(hue, saturation, value, isFinal = false)
                },
                onValueFinal = { newVal ->
                    value = newVal
                    notifyUpdate(hue, saturation, value, isFinal = true)
                },
                onInteractionStateChange = { active -> isUserInteracting = active },
                gradientBrush = Brush.horizontalGradient(
                    listOf(
                        Color.Black,
                        computeColor(hue, saturation, 1f)
                    )
                )
            )
        }
    }
}

@Composable
fun ColorSeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueFinal: (Float) -> Unit,
    onInteractionStateChange: (Boolean) -> Unit,
    gradientBrush: Brush,
    modifier: Modifier = Modifier
) {
    val currentValue by rememberUpdatedState(value)

    fun xToValue(x: Float, width: Float): Float {
        val rawFraction = (x / width).coerceIn(0f, 1f)
        return if (rawFraction >= 0.97f) 1.0f else if (rawFraction <= 0.03f) 0.0f else rawFraction
    }

    val fraction = value.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onInteractionStateChange(true)
                        onValueChange(xToValue(offset.x, size.width.toFloat()))
                        // NO llamar a onValueFinal aquí
                    },
                    onDragEnd = {
                        onValueFinal(currentValue) // 🔥 SOLO aquí
                        onInteractionStateChange(false)
                    },
                    onDragCancel = {
                        onInteractionStateChange(false)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onValueChange(xToValue(change.position.x, size.width.toFloat()))
                        // NO llamar a onValueFinal aquí
                    }
                )
            }
    ) {
        val cornerRadius = CornerRadius(18.dp.toPx())

        drawRoundRect(
            brush = gradientBrush,
            cornerRadius = cornerRadius
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.25f),
            style = Stroke(1.5.dp.toPx()),
            cornerRadius = cornerRadius
        )

        val lineLineWidth = 6.dp.toPx()
        val lineHeight = size.height - 10.dp.toPx()

        val lineX = (fraction * size.width).coerceIn(12.dp.toPx(), size.width - 12.dp.toPx()) - (lineLineWidth / 2f)
        val lineY = (size.height - lineHeight) / 2f

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(lineX + 1.5f, lineY + 1.5f),
            size = Size(lineLineWidth, lineHeight),
            cornerRadius = CornerRadius(lineLineWidth / 2f)
        )

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(lineX, lineY),
            size = Size(lineLineWidth, lineHeight),
            cornerRadius = CornerRadius(lineLineWidth / 2f)
        )
    }
}
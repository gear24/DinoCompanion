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
    //enableGammaCorrection: Boolean = true
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }
    var isUserInteracting by remember { mutableStateOf(false) }

    // 0.45 es un buen equilibrio: 50% del slider = ~70% saturación real
    //  return raw.pow(0.35f).coerceIn(0f, 1f)  // Más agresivo
    //  return raw.pow(0.6f).coerceIn(0f, 1f)   // Más suave
    val SATURATION_CURVE = 0.35f


    // 🔥 NUEVO: guardamos el último color que enviamos nosotros
    var internalColor by remember { mutableStateOf(Color.White) }


    val wheelColors = remember {
        listOf(
            Color.Red, Color.Yellow, Color.Green, Color.Cyan,
            Color.Blue, Color.Magenta, Color.Red
        )
    }


    fun computeColor(h: Float, s: Float, v: Float) = Color.hsv(h, s, v)
/*
    fun applyGamma(channel: Float): Int {
        return if (enableGammaCorrection) {
            (channel.toDouble().pow(2.2) * 255).toInt().coerceIn(0, 255)
        } else {
            (channel * 255).toInt().coerceIn(0, 255)
        }
    }
*/
     // 🔥 Función para mapear la saturación de forma no lineal
    // Exponente más BAJO = más saturación al principio (menos blanco)
   // Exponente más ALTO = menos saturación al principio (más blanco)

    fun mapSaturation(raw: Float): Float = raw.pow(SATURATION_CURVE).coerceIn(0f, 1f)
    fun unmapSaturation(mapped: Float): Float = mapped.pow(1f / SATURATION_CURVE).coerceIn(0f, 1f)

    // 🔥 Solo sincronizar si el color externo es diferente al interno
// 🔥 Sincronizar SIEMPRE que cambie el currentColor desde afuera (al cambiar de caja o efecto)
    LaunchedEffect(currentColor) {
        if (!isUserInteracting) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(currentColor.toArgb(), hsv)
            hue = hsv[0]
            saturation = unmapSaturation(hsv[1]) // Sincronizar también la barra de saturación
            value = hsv[2]
            internalColor = currentColor
        }
    }


    fun notifyUpdate(h: Float, s: Float, v: Float, isFinal: Boolean) {
        val color = computeColor(h, s, v)
        internalColor = color
        onColorChanged(color)  // Actualiza UI

        // 🔥 SOLO UNA LLAMADA: onColorStream ya maneja el throttling
        // Y si es final, el ViewModel decide si persistir
        onColorStream(color)

        // 🔥 Si es final, notificar al ViewModel para que persista
        // (pero NO envía al ESP32 directamente, solo guarda en SharedPreferences)
        if (isFinal) {
            sendColorFinal(
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
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
                value = unmapSaturation(saturation),  // 🔥 El slider se mueve en espacio mapeado
                onValueChange = { mappedValue ->
                    saturation = mapSaturation(mappedValue)  // 🔥 Guardamos el valor real
                    notifyUpdate(hue, saturation, value, isFinal = false)
                },
                onValueFinal = { mappedValue ->
                    saturation = mapSaturation(mappedValue)
                    notifyUpdate(hue, saturation, value, isFinal = true)
                },
                onInteractionStateChange = { active -> isUserInteracting = active },
                gradientBrush = Brush.horizontalGradient(
                    listOf(
                        computeColor(hue, 0f, value),      // 0% saturación (blanco)
                        computeColor(hue, 1f, value)       // 100% saturación (color puro)
                        // 🔥 Nota: El gradiente sigue siendo lineal VISUALMENTE,
                        // pero el slider ahora se mueve en el espacio mapeado.
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
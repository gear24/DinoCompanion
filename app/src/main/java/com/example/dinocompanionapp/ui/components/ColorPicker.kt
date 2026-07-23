package com.example.dinocompanionapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay




@Composable
fun ColorPicker(
    currentColor: Color,
    onColorChanged: (Color) -> Unit,
    onColorStream: (Color) -> Unit, // 👈 Nuevo callback para el arrastre en vivo
    sendColorFinal: (Int, Int, Int) -> Unit // Para tap / dragEnd
) {
    var touchX by remember { mutableFloatStateOf(0f) }
    var touchY by remember { mutableFloatStateOf(0f) }

    var drawX by remember { mutableFloatStateOf(0f) }
    var drawY by remember { mutableFloatStateOf(0f) }
    val ease = 0.25f // Aumentado ligeramente para que la bolita siga el dedo con menos retraso

    LaunchedEffect(Unit) {
        while (true) {
            drawX += (touchX - drawX) * ease
            drawY += (touchY - drawY) * ease
            delay(16) // ~60 FPS para el renderizado del cursor
        }
    }

    LaunchedEffect(currentColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(currentColor.toArgb(), hsv)
        touchX = hsv[0] / 360f
        touchY = 1f - hsv[2]
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(2.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val x = offset.x.coerceIn(0f, size.width.toFloat())
                    val y = offset.y.coerceIn(0f, size.height.toFloat())

                    touchX = x / size.width
                    touchY = y / size.height

                    val hue = touchX * 360f
                    val value = (1f - touchY).coerceIn(0.2f, 1f)
                    val color = Color.hsv(hue, 1f, value)

                    onColorChanged(color)
                    sendColorFinal(
                        (color.red * 255).toInt(),
                        (color.green * 255).toInt(),
                        (color.blue * 255).toInt()
                    )
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // Forzamos la actualización guardando en SharedPreferences al soltar
                        val hue = touchX * 360f
                        val value = (1f - touchY).coerceIn(0.2f, 1f)
                        val color = Color.hsv(hue, 1f, value)

                        onColorChanged(color)
                        sendColorFinal(
                            (color.red * 255).toInt(),
                            (color.green * 255).toInt(),
                            (color.blue * 255).toInt()
                        )
                    },
                    onDrag = { change, _ ->
                        change.consume()

                        val x = change.position.x.coerceIn(0f, size.width.toFloat())
                        val y = change.position.y.coerceIn(0f, size.height.toFloat())

                        touchX = x / size.width
                        touchY = y / size.height

                        val hue = touchX * 360f
                        val value = (1f - touchY).coerceIn(0.2f, 1f)
                        val color = Color.hsv(hue, 1f, value)

                        onColorChanged(color)
                        onColorStream(color) // 👈 Stream amortiguado al ViewModel
                    }
                )
            }
    ) {
        // (Tus drawRect y drawCircle se quedan exactamente igual)
        drawRect(brush = Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = 16f, center = Offset(drawX * size.width + 2, drawY * size.height + 2))
        drawCircle(color = Color.White, radius = 12f, center = Offset(drawX * size.width, drawY * size.height))
        drawCircle(color = Color.Black, radius = 10f, style = Stroke(2f), center = Offset(drawX * size.width, drawY * size.height))
    }
}
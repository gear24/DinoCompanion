package com.example.dinocompanionapp.managers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.dinocompanionapp.data.DinoProtocol
import com.example.dinocompanionapp.bluetooth.BluetoothManager
import com.example.dinocompanionapp.repository.DinoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DinoLightingManager(
    private val bluetoothManager: BluetoothManager,
    private val repository: DinoRepository,
    private val scope: CoroutineScope
) {

    fun updateBrilloColor(brillo: Float) {
        val valor = brillo.coerceIn(0f, 100f)

        repository.saveBrilloColor(valor)
        sendBrightness(valor.toInt())
    }

    fun sendBrightness(value: Int) {
        val brillo = value.coerceIn(0, 100)

        scope.launch {
            bluetoothManager.send(
                "${DinoProtocol.BRIGHTNESS}|$brillo"
            )
        }
    }

    fun sendColor(color: Color) {
        val hsv = FloatArray(3)

        android.graphics.Color.colorToHSV(
            color.toArgb(),
            hsv
        )

        android.util.Log.d(
            "DINO_COLOR_DEBUG",
            "📤 ENVIANDO AL ESP32 - H: ${hsv[0].toInt()}°, S: ${(hsv[1] * 100).toInt()}%, V: ${(hsv[2] * 100).toInt()}%"
        )

        android.util.Log.d(
            "DINO_COLOR_DEBUG",
            "📤 Color: ${color.toArgb().toString(16)}"
        )

        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()

        scope.launch {
            bluetoothManager.send("$r,$g,$b")
        }
    }
}
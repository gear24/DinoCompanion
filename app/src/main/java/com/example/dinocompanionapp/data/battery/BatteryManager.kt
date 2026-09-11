package com.example.dinocompanionapp.data.battery

import com.example.dinocompanionapp.bluetooth.BluetoothManager
import com.example.dinocompanionapp.data.DinoProtocol
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

class BatteryManager(
    private val bluetoothManager: BluetoothManager
) {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    var onBatteryChanged: ((BatteryState) -> Unit)? = null

    fun start() {
        scope.launch {
            while (isActive) {
                if (bluetoothManager.isConnected()) {
                    bluetoothManager.send(DinoProtocol.BATTERY)
                }

                delay(60.seconds)
            }
        }
    }

    fun processMessage(mensaje: String) {
        when {
            mensaje.startsWith(DinoProtocol.BATTERY_RESPONSE) -> {
                val partes = mensaje.split("|")
                val porcentaje = partes.getOrNull(1)?.toIntOrNull()
                    ?: return

                val raw = partes.getOrNull(2)?.toIntOrNull()
                val voltaje = partes.getOrNull(3)?.toFloatOrNull()

                val estado = when {
                    porcentaje <= 10 -> "¡Carga a Dino!"
                    porcentaje <= 20 -> "Batería baja"
                    else -> "Normal"
                }

                onBatteryChanged?.invoke(
                    BatteryState(
                        porcentaje = porcentaje,
                        estado = estado,
                        raw = raw,
                        voltaje = voltaje
                    )
                )
            }

            mensaje.startsWith("BATINFO|") -> {
                val partes = mensaje.split("|")
                val raw = partes.getOrNull(1)?.toIntOrNull()
                val voltaje = partes.getOrNull(2)?.toFloatOrNull()

                if (raw != null && voltaje != null) {
                    onBatteryChanged?.invoke(
                        BatteryState(
                            raw = raw,
                            voltaje = voltaje
                        )
                    )
                }
            }
        }
    }
}
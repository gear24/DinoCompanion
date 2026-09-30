package com.example.dinocompanionapp.managers


import android.util.Log
import com.example.dinocompanionapp.bluetooth.BluetoothManager
import com.example.dinocompanionapp.data.DinoProtocol
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds
import com.example.dinocompanionapp.data.BatteryState

class DinoBatteryManager(
    private val bluetoothManager: BluetoothManager

) {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )
    private var ultimoPorcentaje = -1
    private var ultimoEstado = "Normal"

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
        Log.d("DINO_BATTERY", "📥 Mensaje recibido: $mensaje")

        when {
            mensaje.startsWith(DinoProtocol.BATTERY_RESPONSE) -> {
                val porcentaje = mensaje
                    .split("|")
                    .getOrNull(1)
                    ?.toIntOrNull()
                    ?: return

                ultimoPorcentaje = porcentaje

                ultimoEstado = when {
                    porcentaje <= 10 -> "¡Carga a Dino!"
                    porcentaje <= 20 -> "Batería baja"
                    else -> "Normal"
                }

                onBatteryChanged?.invoke(
                    BatteryState(
                        porcentaje = ultimoPorcentaje,
                        estado = ultimoEstado,
                        disponible = true

                    )
                )

                Log.d(
                    "DINO_BATTERY",
                    "🔋 $ultimoPorcentaje%"
                )
            }

            mensaje.startsWith("BATINFO|") -> {
                val partes = mensaje.split("|")
                val raw = partes.getOrNull(1)?.toIntOrNull()
                val voltaje = partes.getOrNull(2)?.toFloatOrNull()

                if (raw != null && voltaje != null) {
                    onBatteryChanged?.invoke(
                        BatteryState(
                            porcentaje = ultimoPorcentaje,
                            estado = ultimoEstado,
                            raw = raw,
                            voltaje = voltaje,
                            disponible = true

                        )
                    )

                    Log.d(
                        "DINO_BATTERY",
                        "🔋 $ultimoPorcentaje% | raw=$raw | voltaje=$voltaje"
                    )
                }
            }
        }
    }

    fun clearBattery() {
        ultimoPorcentaje = -1
        ultimoEstado = "Sin conexión"

        onBatteryChanged?.invoke(
            BatteryState(
                porcentaje = -1,
                estado = "Sin conexión",
                disponible = false
            )
        )
    }
}
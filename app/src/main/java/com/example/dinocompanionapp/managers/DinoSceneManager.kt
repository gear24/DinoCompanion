package com.example.dinocompanionapp.managers

import androidx.compose.ui.graphics.Color
import com.example.dinocompanionapp.bluetooth.BluetoothManager
import com.example.dinocompanionapp.data.DinoProtocol
import com.example.dinocompanionapp.data.Escena
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class DinoSceneManager(
    private val bluetoothManager: BluetoothManager,
    private val scope: CoroutineScope,
    private val onDinoEncendidoChanged: (Boolean) -> Unit,
    private val onModoActualChanged: (Int) -> Unit,
    private val onUltimaEscenaChanged: (Long) -> Unit
) {
    private var estadoPrevioModo: Int = 0
    private var escenaPrevia: Escena? = null
    fun previewEscena(escena: Escena) {
        onDinoEncendidoChanged(true)

        scope.launch {
            bluetoothManager.send(
                "${DinoProtocol.BRIGHTNESS}|${escena.brillo}"
            )
            delay(20.milliseconds)
            bluetoothManager.send(
                construirProtocoloEscena(escena)
            )
        }
    }

    fun previewBrillo(brillo: Int) {
        scope.launch {
            bluetoothManager.send(
                "${DinoProtocol.BRIGHTNESS}|$brillo"
            )
        }
    }

    fun aplicarEscena(escena: Escena) {
        onDinoEncendidoChanged(true)
        onModoActualChanged(99)
        onUltimaEscenaChanged(escena.id)

        scope.launch {
            bluetoothManager.send(
                "${DinoProtocol.BRIGHTNESS}|${escena.brillo}"
            )
            bluetoothManager.send(
                construirProtocoloEscena(escena)
            )
        }
    }

    private fun construirProtocoloEscena(escena: Escena): String {
        val stringEscena = StringBuilder(
            "${DinoProtocol.SCENE}|${escena.efecto.codigo}|${escena.velocidad}|${escena.colores.size}"
        )

        escena.colores.forEach { colorArgb ->
            val color = Color(colorArgb)

            val r = (color.red * 255).toInt()
            val g = (color.green * 255).toInt()
            val b = (color.blue * 255).toInt()

            stringEscena.append("|$r|$g|$b")
        }

        return stringEscena.toString()
    }

    fun iniciarLiveScene(
        modoActual: Int,
        escenaEnEdicion: Escena?,
        ultimaEscena: Escena?
    ) {
        estadoPrevioModo = modoActual

        escenaPrevia = escenaEnEdicion?.copy()
            ?: ultimaEscena?.copy()
    }

    fun obtenerEscenaPrevia(): Escena? = escenaPrevia

    fun obtenerModoPrevio(): Int = estadoPrevioModo

    fun limpiarEstadoEdicion() {
        escenaPrevia = null
    }

    fun reactivarUltimaEscena(
        ultimaEscenaId: Long,
        ultimaEscena: Escena?
    ) {
        if (ultimaEscenaId != -1L && ultimaEscena != null) {
            aplicarEscena(ultimaEscena)
        }
    }
}
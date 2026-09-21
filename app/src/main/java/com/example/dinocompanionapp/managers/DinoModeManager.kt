package com.example.dinocompanionapp.managers

import com.example.dinocompanionapp.bluetooth.BluetoothManager
import com.example.dinocompanionapp.repository.DinoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


class DinoModeManager(
    private val bluetoothManager: BluetoothManager,
    private val repository: DinoRepository,
    private val lightingManager: DinoLightingManager,
    private val scope: CoroutineScope,
    private val onModoActualChanged: (Int) -> Unit,
    private val onUltimoModoChanged: (Int) -> Unit,
    private val onDinoEncendidoChanged: (Boolean) -> Unit,
    private val onAnimStateChanged: (Boolean) -> Unit
) {

    fun ejecutarModo(idModo: Int) {
        val brilloModo = repository.getBrilloModo(idModo)
            .toInt()
            .coerceIn(0, 100)

        onModoActualChanged(idModo)
        onUltimoModoChanged(idModo)
        onDinoEncendidoChanged(true)
        onAnimStateChanged(true)

        repository.saveModoActual(idModo)
        repository.saveUltimoModoId(idModo)
        repository.saveDinoEncendido(true)

        scope.launch {
            bluetoothManager.send(idModo.toString())

            delay(30.milliseconds)

            lightingManager.sendBrightness(brilloModo)
        }
    }

    fun reactivarUltimoModo(ultimoModoId: Int) {
        when (ultimoModoId) {
            1 -> ejecutarModo(1)
            2 -> ejecutarModo(2)
            3 -> ejecutarModo(3)
            4 -> ejecutarModo(4)
            5 -> ejecutarModo(5)
            6 -> ejecutarModo(6)
            10 -> ejecutarModo(10)
            11 -> ejecutarModo(11)
            12 -> ejecutarModo(12)
            else -> ejecutarModo(3)
        }
    }
}
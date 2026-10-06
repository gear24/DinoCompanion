package com.example.dinocompanionapp.viewmodel

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.example.dinocompanionapp.managers.DinoModeManager
import com.example.dinocompanionapp.repository.DinoRepository
import android.app.Application
import androidx.lifecycle.ViewModelProvider

class DinoModesViewModel(
    private val repository: DinoRepository,
    private val modeManager: DinoModeManager,
    private val getModoActual: () -> Int,
    private val onBrilloChanged: (Float) -> Unit
) : ViewModel() {

    private val brillosModo = mutableStateMapOf<Int, Float>()

    fun brilloModo(idModo: Int): Float {
        return brillosModo.getOrPut(idModo) {
            repository.getBrilloModo(idModo)
        }
    }

    fun updateBrilloModo(
        idModo: Int,
        nuevoBrillo: Float
    ) {
        val brillo = nuevoBrillo.coerceIn(0f, 100f)

        brillosModo[idModo] = brillo
        repository.saveBrilloModo(idModo, brillo)

        if (getModoActual() == idModo) {
            onBrilloChanged(brillo)
        }
    }

    fun startLava() = modeManager.ejecutarModo(3)
    fun startArcoiris() = modeManager.ejecutarModo(4)
    fun startRespirar() = modeManager.ejecutarModo(1)
    fun startOcean() = modeManager.ejecutarModo(2)
    fun startForest() = modeManager.ejecutarModo(5)
    fun startParty() = modeManager.ejecutarModo(6)
    fun modo10() = modeManager.ejecutarModo(10)
    fun modo11() = modeManager.ejecutarModo(11)
    fun modo12() = modeManager.ejecutarModo(12)

    fun reactivarUltimoModo(ultimoModoId: Int) {
        modeManager.reactivarUltimoModo(ultimoModoId)
    }
}


class DinoModesViewModelFactory(
    private val application: Application,
    private val modeManager: DinoModeManager,
    private val getModoActual: () -> Int,
    private val onBrilloChanged: (Float) -> Unit
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(DinoModesViewModel::class.java)) {
            return DinoModesViewModel(
                repository = DinoRepository(
                    application.applicationContext
                ),
                modeManager = modeManager,
                getModoActual = getModoActual,
                onBrilloChanged = onBrilloChanged
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}
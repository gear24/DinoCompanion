package com.example.dinocompanionapp.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.dinocompanionapp.data.Escena
import com.example.dinocompanionapp.managers.DinoSceneManager
import com.example.dinocompanionapp.repository.DinoRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue



class DinoScenesViewModel(
    private val repository: DinoRepository,
    private val sceneManager: DinoSceneManager,
    private val getModoActual: () -> Int,
    private val getUltimaEscenaId: () -> Long,
    private val onRestoreColor: () -> Unit,
    private val onRestoreMode: (Int) -> Unit
) : ViewModel() {


    val listaEscenas = mutableStateListOf<Escena>()

    init {
        cargarEscenasLocales()
    }

    fun cargarEscenasLocales() {
        listaEscenas.clear()
        listaEscenas.addAll(
            repository.cargarTodasLasEscenas()
        )
    }

    fun iniciarLiveScene(
        escenaEnEdicion: Escena? = null
    ) {
        val ultimaEscenaIdActual = getUltimaEscenaId()

        val ultimaEscena = if (ultimaEscenaIdActual != -1L) {
            listaEscenas.find { it.id == ultimaEscenaIdActual }
        } else {
            null
        }

        sceneManager.iniciarLiveScene(
            modoActual = getModoActual(),
            escenaEnEdicion = escenaEnEdicion,
            ultimaEscena = ultimaEscena
        )
    }


    fun guardarNuevaEscena(
        escena: Escena,
        esEdicion: Boolean
    ) {
        if (!esEdicion) {
            repository.agregarEscena(escena)
        } else {
            repository.actualizarEscena(escena)
        }

        cargarEscenasLocales()
    }

    fun borrarEscena(id: Long) {
        repository.eliminarEscena(id)
        cargarEscenasLocales()
    }

    fun aplicarEscena(escena: Escena) {
        sceneManager.aplicarEscena(escena)
    }

    fun previewEscenaEnVivo(escenaTemporal: Escena) {
        sceneManager.previewEscena(escenaTemporal)
    }

    fun previewBrilloEscena(brillo: Int) {
        sceneManager.previewBrillo(brillo)
    }

    fun obtenerEscenaPrevia(): Escena? {
        return sceneManager.obtenerEscenaPrevia()
    }

    fun obtenerModoPrevio(): Int {
        return sceneManager.obtenerModoPrevio()
    }

    fun limpiarEstadoEdicion() {
        sceneManager.limpiarEstadoEdicion()
    }

    fun cancelarEdicionEscena(
        onAplicarEscenaPrevia: (Escena) -> Unit
    ) {
        val previa = obtenerEscenaPrevia()

        if (previa != null) {
            onAplicarEscenaPrevia(previa)
        } else {
            val modoPrevio = obtenerModoPrevio()

            if (modoPrevio == 0) {
                onRestoreColor()
            } else {
                onRestoreMode(modoPrevio)
            }
        }

        limpiarEstadoEdicion()
    }


    fun reactivarUltimaEscena() {
        val ultimaEscenaIdActual = getUltimaEscenaId()
        Log.d(
            "DINO_SCENE_DEBUG",
            "Reactivar | ultimaEscenaId=$ultimaEscenaIdActual | " +
                    "escenas=${listaEscenas.map { "${it.id}:${it.nombre}" }}"
        )
        val escena = if (ultimaEscenaIdActual != -1L) {
            listaEscenas.find { it.id == ultimaEscenaIdActual }
        } else {
            null
        }

        sceneManager.reactivarUltimaEscena(
            ultimaEscenaId = ultimaEscenaIdActual,
            ultimaEscena = escena
        )
    }


}
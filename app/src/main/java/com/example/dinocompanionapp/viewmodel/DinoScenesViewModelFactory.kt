package com.example.dinocompanionapp.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dinocompanionapp.managers.DinoSceneManager
import com.example.dinocompanionapp.repository.DinoRepository

class DinoScenesViewModelFactory(
    private val application: Application,
    private val sceneManager: DinoSceneManager,
    private val getModoActual: () -> Int,
    private val onRestoreColor: () -> Unit,
    private val onRestoreMode: (Int) -> Unit,
    private val getUltimaEscenaId: () -> Long

) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(DinoScenesViewModel::class.java)) {
            return DinoScenesViewModel(
                repository = DinoRepository(application.applicationContext),
                sceneManager = sceneManager,
                getModoActual = getModoActual,
                getUltimaEscenaId = getUltimaEscenaId,
                onRestoreColor = onRestoreColor,
                onRestoreMode = onRestoreMode,


            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}
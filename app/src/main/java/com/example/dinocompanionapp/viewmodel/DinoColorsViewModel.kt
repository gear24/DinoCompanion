package com.example.dinocompanionapp.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.example.dinocompanionapp.managers.DinoLightingManager
import com.example.dinocompanionapp.repository.DinoRepository
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import com.example.dinocompanionapp.bluetooth.BluetoothManager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.sample

class DinoColorsViewModel(
    application: Application,
    bluetoothManager: BluetoothManager,
    private val onDinoEncendidoChanged: (Boolean) -> Unit,
    private val onModoActualChanged: (Int) -> Unit
) : AndroidViewModel(application) {

    private val repository = DinoRepository(
        application.applicationContext
    )

    private val lightingManager = DinoLightingManager(
        bluetoothManager = bluetoothManager,
        repository = repository,
        scope = viewModelScope
    )

    private val colorStreamChannel =
        Channel<Color>(Channel.CONFLATED)

    var brilloColor by mutableFloatStateOf(
        repository.getBrilloColor()
    )
        private set

    var currentColor by mutableStateOf(
        Color(repository.getCurrentColor(Color.Red.toArgb()))
    )
        private set

    data class Favorito(
        val color: Color?,
        val brillo: Float
    )
    var favoritoActivo by mutableStateOf(
        repository.getFavoritoActivo()
    )
        private set

    var favoritoModificado by mutableStateOf(
        repository.getFavoritoModificado()
    )
        private set
    val favoritos = mutableStateListOf<Favorito>().apply {
        addAll(
            List(5) { index ->
                if (repository.hasFavorito(index)) {
                    Favorito(
                        color = Color(
                            repository.getFavoritoColor(
                                index,
                                Color.Transparent.toArgb()
                            )
                        ),
                        brillo = repository.getFavoritoBrillo(index)
                    )
                } else {
                    Favorito(
                        color = null,
                        brillo = 80f
                    )
                }
            }
        )
    }

    init {
        iniciarProcesadorDeColores()
    }

    @OptIn(FlowPreview::class)
    private fun iniciarProcesadorDeColores() {
        viewModelScope.launch {
            colorStreamChannel
                .consumeAsFlow()
                .sample(60.milliseconds)
                .collect { color ->
                    lightingManager.sendColor(color)
                }
        }
    }


    fun updateCurrentColor(nuevoColor: Color) {
        currentColor = nuevoColor

        favoritoActivo = null
        favoritoModificado = true

        repository.saveFavoritoActivo(null)
        repository.saveFavoritoModificado(true)

        repository.saveCurrentColor(nuevoColor.toArgb())
    }


    fun updateBrilloColor(nuevoBrillo: Float) {
        val brillo = nuevoBrillo.coerceIn(0f, 100f)

        favoritoActivo = null
        favoritoModificado = true

        repository.saveFavoritoActivo(null)
        repository.saveFavoritoModificado(true)

        brilloColor = brillo
        lightingManager.updateBrilloColor(brillo)
    }

    fun sendCurrentColor() {
        onDinoEncendidoChanged(true)
        onModoActualChanged(0)

        repository.saveDinoEncendido(true)
        repository.saveModoActual(0)

        sendColor(currentColor, persistir = true)
        lightingManager.sendBrightness(brilloColor.toInt())
    }

    fun streamColorLive(color: Color) {
        colorStreamChannel.trySend(color)
    }

    fun sendColorFinal(
        red: Int,
        green: Int,
        blue: Int
    ) {
        sendColor(
            Color(red, green, blue),
            persistir = true
        )
    }

    private fun sendColor(
        color: Color,
        persistir: Boolean
    ) {
        colorStreamChannel.trySend(color)

        if (persistir) {
            currentColor = color

            onDinoEncendidoChanged(true)
            onModoActualChanged(0)

            repository.saveCurrentColor(color.toArgb())
            repository.saveDinoEncendido(true)
            repository.saveModoActual(0)
        }
    }

    fun saveOrClearFavorite(
        index: Int,
        color: Color,
        brillo: Float
    ) {
        if (index !in favoritos.indices) return

        favoritos[index] = Favorito(
            color = color,
            brillo = brillo
        )

        repository.saveFavorito(
            index = index,
            color = color.toArgb(),
            brillo = brillo
        )

        favoritoActivo = index
        favoritoModificado = false

        repository.saveFavoritoActivo(index)
        repository.saveFavoritoModificado(false)
    }
    fun activarFavorito(index: Int) {
        if (index !in favoritos.indices) return

        val favorito = favoritos[index]

        favorito.color?.let { color ->
            favoritoActivo = index
            favoritoModificado = false

            repository.saveFavoritoActivo(index)
            repository.saveFavoritoModificado(false)

            sendColorFinal(
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )

            brilloColor = favorito.brillo
            lightingManager.updateBrilloColor(favorito.brillo)
        }
    }


}


class DinoColorsViewModelFactory(
    private val application: Application,
    private val bluetoothManager: BluetoothManager,
    private val onDinoEncendidoChanged: (Boolean) -> Unit,
    private val onModoActualChanged: (Int) -> Unit
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(DinoColorsViewModel::class.java)) {
            return DinoColorsViewModel(
                application = application,
                bluetoothManager = bluetoothManager,
                onDinoEncendidoChanged = onDinoEncendidoChanged,
                onModoActualChanged = onModoActualChanged
            ) as T
        }

        throw IllegalArgumentException(
            "ViewModel desconocido: ${modelClass.name}"
        )
    }


}
package com.example.dinocompanionapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.AndroidViewModel
import com.example.dinocompanionapp.bluetooth.BluetoothManager
import com.example.dinocompanionapp.data.BtState
import com.example.dinocompanionapp.data.DinoInfo
import com.example.dinocompanionapp.data.DinoProtocol
import com.example.dinocompanionapp.data.audio.MediaSessionManager
import com.example.dinocompanionapp.data.audio.MediaState
import com.example.dinocompanionapp.data.audio.MusicManager
import com.example.dinocompanionapp.data.audio.VolumeManager
import com.example.dinocompanionapp.managers.DinoBatteryManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import com.example.dinocompanionapp.managers.DinoLightingManager
import com.example.dinocompanionapp.managers.DinoModeManager
import com.example.dinocompanionapp.managers.DinoSceneManager
//
import com.example.dinocompanionapp.repository.DinoRepository
import kotlinx.coroutines.flow.distinctUntilChanged
/**
 *
 * 📌 MÉTODOS EN DinoColorsViewModel (Módulo de UI/Colors):
 *
 * 1. streamColorLive(color)
 *    - Uso: ColorPicker / Sliders (mientras el usuario ARRASTRA el dedo).
 *    - Qué hace: Envía el color a 'colorStreamChannel' (con throttle/sample de 60ms).
 *    - Persistencia: NO guarda en SharedPreferences ni altera el estado permanente.
 *
 * 2. sendColorFinal(r, g, b) [o sendColor(color, persistir = true)]
 *    - Uso: Al SOLTAR el slider, seleccionar un Preset o activar un Favorito.
 *    - Qué hace: Pasa el color al canal, guarda el color en 'repository', activa
 *      dinoEncendido = true y fuerza el modoActual = 0 (Estático).
 *    - Persistencia: SÍ guarda permanentemente en SharedPreferences.
 *
 * 3. sendCurrentColor()
 *    - Uso: Reaplicar el color guardado manualmente activando estados de encendido.
 *    - Qué hace: Sincroniza 'dinoEncendido' y 'modoActual' en repositorios, y manda
 *      el color actual + brillo por el stream.
 *
 * -----------------------------------------------------------------------------
 * 📌 MÉTODOS EN DinoLightingManager (Módulo Directo/Hardware):
 *
 * 4. lightingManager.sendColor(color)
 *    - Uso: Usado internamente por el 'colorStreamChannel' y respuestas a eventos
 *      del firmware (ej: reactivarColorActual).
 *    - Qué hace: Transforma a RGB "$r,$g,$b" y lo envía INMEDIATAMENTE por Bluetooth.
 *    - Persistencia: NO altera SharedPreferences ni pasa por colas de retardo.
 */

class DinoViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = DinoRepository(appContext)

    var ultimoModoId by mutableIntStateOf(
        repository.getUltimoModoId()
    )
        private set

    val bluetoothManager = BluetoothManager(appContext)
    val mediaSessionManager = MediaSessionManager(appContext)

    private val lightingManager = DinoLightingManager(
        bluetoothManager = bluetoothManager,
        repository = repository,
        scope = viewModelScope
    )
     val modeManager = DinoModeManager(
        bluetoothManager = bluetoothManager,
        repository = repository,
        lightingManager = lightingManager,
        scope = viewModelScope,
        onModoActualChanged = { modoActual = it },
        onUltimoModoChanged = { ultimoModoId = it },
        onDinoEncendidoChanged = { dinoEncendido = it },
        onAnimStateChanged = { animState = it }
    )
     val sceneManager = DinoSceneManager(
        bluetoothManager = bluetoothManager,
        scope = viewModelScope,
        onDinoEncendidoChanged = { dinoEncendido = it },
        onModoActualChanged = { modoActual = it },
        onUltimaEscenaChanged = {
            ultimaEscenaId = it
            repository.saveUltimaEscenaId(it)
        }
    )

    // Guardar el ID de la última escena seleccionada
    var ultimaEscenaId by mutableLongStateOf(
        repository.getUltimaEscenaId()
    )
        private set

    // --Guardar nombre del dino
    var dinoName by mutableStateOf(
        repository.getDinoName()
    )
        private set

    // -- estados de la bateria
    var ultimoRaw by mutableStateOf<Int?>(null)
    var ultimoVoltaje by mutableStateOf<Float?>(null)

    private val batteryManager = DinoBatteryManager(bluetoothManager)

    // --- ESTADOS DE HOME / GLOBAL ---
    var bateria by mutableIntStateOf(-1)
        private set


    var estadoBateria by mutableStateOf("Normal")
        private set

    var modoActual by mutableIntStateOf(
        repository.getModoActual()
    )
        private set

    var dinoEncendido by mutableStateOf(
        repository.isDinoEncendido()
    )
        private set

    // --- ESTADOS DE MODESSCREEN ---

    var animState by mutableStateOf(modoActual != 0)
        private set

    // --Estados para el manejo del audio
    var mediaState by mutableStateOf(MediaState())
        private set

    var dinoInfo by mutableStateOf(DinoInfo())
        private set

    val musicManager = MusicManager()
    private val volumeManager = VolumeManager(appContext)

    init {
        bluetoothManager.updateDeviceName(dinoName)
        configurarBluetooth()
        configurarBateria()
        configurarMediaSession()
        intentarAutoConexion()
        configurarVolumen()
        observarEstadoBluetooth()
    }

    /**
     * Llama a esto mientras ARRASTRAS el dedo en el ColorPicker.
     * Es ultra ligero y no satura el Bluetooth ni crea corrutinas descontroladas.
     */
    fun intentarAutoConexion() {
        viewModelScope.launch {
            if (!bluetoothManager.isConnected() &&
                bluetoothManager.state == BtState.DISCONNECTED
            ) {
                bluetoothManager.connect()
            }
        }
    }


    private fun configurarBluetooth() {
        bluetoothManager.onMessageReceived = { mensaje ->
            procesarMensajeESP32(mensaje)
        }

        bluetoothManager.onConnectionLost = {
            animState = false
            batteryManager.clearBattery()

        }
    }


    private fun configurarBateria() {
        batteryManager.onBatteryChanged = { battery ->
            bateria = battery.porcentaje
            estadoBateria = battery.estado
            ultimoRaw = battery.raw
            ultimoVoltaje = battery.voltaje
        }

        batteryManager.start()
    }

    private fun observarEstadoBluetooth() {
        viewModelScope.launch {
            snapshotFlow { bluetoothManager.state }
                .distinctUntilChanged()
                .collect { state ->
                    if (state != BtState.CONNECTED) {
                        batteryManager.clearBattery()
                    }
                }
        }
    }

    private fun procesarMensaje(mensaje: String) {

        if (
            mensaje.startsWith(DinoProtocol.BATTERY_RESPONSE) ||
            mensaje.startsWith("BATINFO|")
        ) {
            batteryManager.processMessage(mensaje)
            return
        }

        when {
            mensaje.contains("HELLO") ||
                    mensaje.startsWith(DinoProtocol.HELLO_RESPONSE) -> {

                Log.d("DINO_ESP32", "Firmware iniciado: $mensaje")

                viewModelScope.launch {
                    bluetoothManager.send(DinoProtocol.BATTERY)

                    if (dinoEncendido) {
                        when (modoActual) {
                            0 -> reactivarColorActual()
                            99 -> reactivarUltimaEscena()
                            else -> modeManager.ejecutarModo(modoActual)
                        }
                    }
                }
            }

            mensaje.startsWith(DinoProtocol.ACK) -> {
                Log.d("DINO_ESP32", mensaje)
            }

            mensaje.startsWith(DinoProtocol.INFO) -> {
                Log.d("DINO_ESP32", mensaje)
            }

            mensaje.startsWith(DinoProtocol.ERROR) -> {
                Log.e("DINO_ESP32", mensaje)
            }

            else -> {
                Log.d("DINO_ESP32", mensaje)
            }
        }
    }

    private fun procesarMensajeESP32(mensaje: String) {
        procesarMensaje(mensaje)
    }

    // --- ACCIONES DE ENERGÍA Y CONEXIÓN ---

    fun turnOffDino() {
        dinoEncendido = false
        animState = false
        modoActual = 0
        repository.saveDinoEncendido(false)
        repository.saveModoActual(0)
        viewModelScope.launch {
            bluetoothManager.send("0")
        }
    }


    fun hasBtPermission(): Boolean {
        return bluetoothManager.hasBtPermission()
    }


    // --- ACCIONES DE COLORES ---
    fun actualizarDinoEncendido(encendido: Boolean) {
        dinoEncendido = encendido
        repository.saveDinoEncendido(encendido)
    }

    fun actualizarModoActual(modo: Int) {
        modoActual = modo
        repository.saveModoActual(modo)
    }

    fun reactivarColorActual() {
        val color = Color(
            repository.getCurrentColor(Color.Red.toArgb())
        )
        val brillo = repository.getBrilloColor()
        dinoEncendido = true
        modoActual = 0
        repository.saveDinoEncendido(true)
        repository.saveModoActual(0)
        lightingManager.sendColor(color)
        lightingManager.sendBrightness(brillo.toInt())
    }
    
    fun sendBrightness(value: Int) {
        lightingManager.sendBrightness(value)
    }
    
    // --- ACCIONES DE MODOS ---
    fun stopAnimation() {
        animState = false
        turnOffDino()
    }


    // --- ACCIONES DE ESCENAS ---

    // Función para reactivar la última escena si entramos estando apagados
    fun reactivarUltimaEscena() {
        val escena = if (ultimaEscenaId != -1L) {
            repository.cargarTodasLasEscenas()
                .find { it.id == ultimaEscenaId }
        } else {
            null
        }

        sceneManager.reactivarUltimaEscena(
            ultimaEscenaId = ultimaEscenaId,
            ultimaEscena = escena
        )
    }


    // -- Personalizacion
    suspend fun changeDinoName(name: String) {
        bluetoothManager.send(
            DinoProtocol.SET_NAME + name
        )
    }

    suspend fun restartDino() {
        bluetoothManager.send(
            DinoProtocol.RESTART
        )
    }

    fun cambiarNombreDesdeUI(nombre: String) {
        viewModelScope.launch {
            changeDinoName(nombre)
            delay(500.milliseconds)
            restartDino()
            // Actualiza inmediatamente la app
            dinoName = nombre
            repository.saveDinoName(nombre)
        }
    }

    // --- CONFIGURACIÓN de sonido---
    fun Long.formatAsTime(): String {
        val totalSegundos = this / 1000
        val minutos = totalSegundos / 60
        val segundos = totalSegundos % 60
        return String.format("%02d:%02d", minutos, segundos)
    }

    private fun configurarMediaSession() {
        musicManager.onSongChanged = { media ->
            viewModelScope.launch {
                val duracionReloj = media.duration.formatAsTime()
                val posicionReloj = media.position.formatAsTime()
                val fuenteApp = media.packageName

                bluetoothManager.send(
                    DinoProtocol.MUSIC_SONG + "${media.title}|${media.artist}|$posicionReloj|$duracionReloj|$fuenteApp")
            }
        }

        musicManager.onPlaybackChanged = { playing ->
            viewModelScope.launch {
                if (playing) {
                    bluetoothManager.send(DinoProtocol.MUSIC_PLAY)
                } else {
                    bluetoothManager.send(DinoProtocol.MUSIC_PAUSE)
                }
            }
        }

        mediaSessionManager.onMediaChanged = { media ->
            mediaState = media
            musicManager.update(media)
        }

        mediaSessionManager.start()
    }

    fun hasAudioPermission(): Boolean {
        return mediaSessionManager.hasNotificationAccess()
    }

    fun requestAudioPermission() {
        mediaSessionManager.requestNotificationAccess()
    }

    private fun configurarVolumen() {
        volumeManager.onVolumeChanged = { volume ->
            Log.d("DINO_VOLUME_VM", "$volume%")
            viewModelScope.launch {
                bluetoothManager.send(
                    DinoProtocol.VOLUME + volume)
            }
        }
        volumeManager.start()
    }
}
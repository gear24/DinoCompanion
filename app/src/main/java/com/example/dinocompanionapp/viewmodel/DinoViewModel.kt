package com.example.dinocompanionapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.dinocompanionapp.data.Escena
import com.example.dinocompanionapp.data.audio.MediaSessionManager
import com.example.dinocompanionapp.data.audio.MediaState
import com.example.dinocompanionapp.data.audio.MusicManager
import com.example.dinocompanionapp.data.audio.VolumeManager
import com.example.dinocompanionapp.managers.DinoBatteryManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import com.example.dinocompanionapp.managers.DinoLightingManager
import com.example.dinocompanionapp.managers.DinoModeManager
import com.example.dinocompanionapp.managers.DinoSceneManager
//
import com.example.dinocompanionapp.repository.DinoRepository
import kotlinx.coroutines.flow.distinctUntilChanged


class DinoViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = DinoRepository(appContext)


    // 1. Canal con estrategia CONFLATED para los colores en movimiento
    private val colorStreamChannel = Channel<Color>(Channel.CONFLATED)



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
    private val modeManager = DinoModeManager(
        bluetoothManager = bluetoothManager,
        repository = repository,
        lightingManager = lightingManager,
        scope = viewModelScope,
        onModoActualChanged = { modoActual = it },
        onUltimoModoChanged = { ultimoModoId = it },
        onDinoEncendidoChanged = { dinoEncendido = it },
        onAnimStateChanged = { animState = it }
    )
    private val sceneManager = DinoSceneManager(
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

    // --- ESTADOS DE COLORSSCREEN ---

    var brilloColor by mutableFloatStateOf(
        repository.getBrilloColor()
    )
        private set

    private val brillosModo = mutableStateMapOf<Int, Float>()

    var currentColor by mutableStateOf(
        Color(repository.getCurrentColor(Color.Red.toArgb()))
    )
        private set

    data class Favorito(
        val color: Color?,
        val brillo: Float
    )

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

    // --- ESTADOS DE MODESSCREEN ---

    var animState by mutableStateOf(modoActual != 0)
        private set

    // --- ESTADOS DE ESCENAS CREACIÓN / LISTA ---

    val listaEscenas = mutableStateListOf<Escena>()

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
        cargarEscenasLocales()
        iniciarProcesadorDeColores()
        intentarAutoConexion()
        configurarVolumen()
        observarEstadoBluetooth()

    }


    @OptIn(FlowPreview::class)
    private fun iniciarProcesadorDeColores() {
        viewModelScope.launch {
            colorStreamChannel
                .consumeAsFlow()
                .sample(60.milliseconds)
                .collect { color ->
                    val r = (color.red * 255).toInt()
                    val g = (color.green * 255).toInt()
                    val b = (color.blue * 255).toInt()

                    bluetoothManager.send("$r,$g,$b")
                }
        }
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


    fun cargarEscenasLocales() {
        listaEscenas.clear()
        listaEscenas.addAll(
            repository.cargarTodasLasEscenas()
        )
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
                            0 -> sendCurrentColor()
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

    fun updateCurrentColor(nuevoColor: Color) {
        currentColor = nuevoColor
        repository.saveCurrentColor(nuevoColor.toArgb())
    }



    fun updateBrilloModo(
        idModo: Int,
        nuevoBrillo: Float
    ) {
        val brillo = nuevoBrillo.coerceIn(0f, 100f)
        brillosModo[idModo] = brillo
        repository.saveBrilloModo(idModo, brillo)
        if (modoActual == idModo) {
            sendBrightness(brillo.toInt())
        }
    }

    fun actualizarDinoEncendido(encendido: Boolean) {
        dinoEncendido = encendido
        repository.saveDinoEncendido(encendido)
    }

    fun actualizarModoActual(modo: Int) {
        modoActual = modo
        repository.saveModoActual(modo)
    }

    fun updateBrilloColor(nuevoBrillo: Float) {
        val brillo = nuevoBrillo.coerceIn(0f, 100f)
        brilloColor = brillo
        lightingManager.updateBrilloColor(brillo)
    }


    fun brilloModo(idModo: Int): Float {
        return brillosModo.getOrPut(idModo) {
            repository.getBrilloModo(idModo)
        }
    }


    fun sendCurrentColor() {
        dinoEncendido = true
        modoActual = 0
        repository.saveDinoEncendido(true)
        repository.saveModoActual(0)
        enviarColorAlESP32(currentColor, persistir = true)
        sendBrightness(brilloColor.toInt())
    }

    fun saveOrClearFavorite(index: Int, color: Color, brillo: Float)
    {
        if (index !in favoritos.indices) return
        favoritos[index] = Favorito(color = color, brillo = brillo)
        repository.saveFavorito(index = index, color = color.toArgb(), brillo = brillo)
    }

    fun sendBrightness(value: Int) {
        lightingManager.sendBrightness(value)
    }

    private fun enviarColorAlESP32(
        color: Color,
        persistir: Boolean = false
    ) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)

        Log.d("DINO_COLOR_DEBUG","📤 ENVIANDO AL ESP32 - H: ${hsv[0].toInt()}°, S: ${(hsv[1] * 100).toInt()}%, V: ${(hsv[2] * 100).toInt()}%")
        Log.d("DINO_COLOR_DEBUG","📤 Color: ${color.toArgb().toString(16)}")

        colorStreamChannel.trySend(color)

        if (persistir) {
            currentColor = color
            dinoEncendido = true
            modoActual = 0

            repository.saveCurrentColor(color.toArgb())
            repository.saveDinoEncendido(true)
            repository.saveModoActual(0)

            Log.d("DINO_COLOR_DEBUG", "💾 Color persistido en SharedPreferences")
        }
    }


    // 🔥 Para el arrastre (streaming)
    fun streamColorLive(color: Color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)

        Log.d(
            "DINO_COLOR_DEBUG",
            "🔄 ARRASTRE - H: ${hsv[0].toInt()}°, S: ${(hsv[1] * 100).toInt()}%, V: ${(hsv[2] * 100).toInt()}%"
        )

        enviarColorAlESP32(
            color,
            persistir = false
        )
    }
    fun sendColorFinal(
        red: Int,
        green: Int,
        blue: Int
    ) {
        val color = Color(red, green, blue)
        val hsv = FloatArray(3)

        android.graphics.Color.colorToHSV(
            color.toArgb(),
            hsv
        )
        Log.d(
            "DINO_COLOR_DEBUG",
            "✅ FINAL - H: ${hsv[0].toInt()}°, S: ${(hsv[1] * 100).toInt()}%, V: ${(hsv[2] * 100).toInt()}%"
        )

        enviarColorAlESP32(
            color,
            persistir = true
        )
    }

    // --- ACCIONES DE MODOS ---
    fun startLava() = modeManager.ejecutarModo(3)
    fun startArcoiris() = modeManager.ejecutarModo(4)
    fun startRespirar() = modeManager.ejecutarModo(1)
    fun startOcean() = modeManager.ejecutarModo(2)
    fun startForest() = modeManager.ejecutarModo(5)
    fun startParty() = modeManager.ejecutarModo(6)
    fun modo10() = modeManager.ejecutarModo(10)
    fun modo11() = modeManager.ejecutarModo(11)
    fun modo12() = modeManager.ejecutarModo(12)
    fun reactivarUltimoModo() {
        modeManager.reactivarUltimoModo(ultimoModoId)
    }
    fun stopAnimation() {
        animState = false
        turnOffDino()
    }


    // --- ACCIONES DE ESCENAS ---


    fun iniciarLiveScene(escenaEnEdicion: Escena? = null) {
        val ultimaEscena = if (ultimaEscenaId != -1L) {
            listaEscenas.find { it.id == ultimaEscenaId }
        } else {
            null
        }

        sceneManager.iniciarLiveScene(
            modoActual = modoActual,
            escenaEnEdicion = escenaEnEdicion,
            ultimaEscena = ultimaEscena
        )
    }

    fun previewEscenaEnVivo(escenaTemporal: Escena) {
        sceneManager.previewEscena(escenaTemporal)
    }

    fun previewBrilloEscena(brillo: Int) {
        sceneManager.previewBrillo(brillo)
    }

    fun cancelarEdicionEscena() {
        viewModelScope.launch {
            val previa = sceneManager.obtenerEscenaPrevia()

            if (previa != null) {
                aplicarEscena(previa)
            } else {
                val modoPrevio = sceneManager.obtenerModoPrevio()

                if (modoPrevio == 0) {
                    sendCurrentColor()
                } else {
                    modeManager.ejecutarModo(modoPrevio)
                }
            }

            sceneManager.limpiarEstadoEdicion()
        }
    }

    fun aplicarEscena(escena: Escena) {
        sceneManager.aplicarEscena(escena)
    }

    // Función para reactivar la última escena si entramos estando apagados
    fun reactivarUltimaEscena() {
        val escena = if (ultimaEscenaId != -1L) {
            listaEscenas.find { it.id == ultimaEscenaId }
        } else {
            null
        }
        sceneManager.reactivarUltimaEscena(
            ultimaEscenaId = ultimaEscenaId,
            ultimaEscena = escena
        )
    }

    fun guardarNuevaEscena(
        escena: Escena,
        esEdicion: Boolean
    ) {
        if (!esEdicion) {
            repository.agregarEscena(escena)
        } else {
            repository.actualizarEscena( escena)
        }
        cargarEscenasLocales()
    }

    fun borrarEscena(id: Long) {
        repository.eliminarEscena( id)
        cargarEscenasLocales()
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
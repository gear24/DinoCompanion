package com.example.dinocompanionapp.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinocompanionapp.bluetooth.BluetoothManager
import com.example.dinocompanionapp.data.BtState
import com.example.dinocompanionapp.data.DinoProtocol
import com.example.dinocompanionapp.data.Escena
import com.example.dinocompanionapp.data.SceneManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.FlowPreview
import androidx.compose.runtime.mutableLongStateOf
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.content.edit
import com.example.dinocompanionapp.data.audio.MediaSessionManager
import com.example.dinocompanionapp.data.audio.MusicManager
import kotlinx.coroutines.delay
import com.example.dinocompanionapp.data.DinoInfo
import com.example.dinocompanionapp.data.audio.MediaState
import com.example.dinocompanionapp.data.audio.VolumeManager


class DinoViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    // 1. Canal con estrategia CONFLATED para los colores en movimiento
    private val colorStreamChannel = Channel<Color>(Channel.CONFLATED)

    // SharedPreferences compartidas del Dino
    private val prefs = appContext.getSharedPreferences(
        "dino_settings",
        Context.MODE_PRIVATE
    )

    var ultimoModoId by mutableIntStateOf(
        prefs.getInt("ultimo_modo_id", 3)
    )
        private set


    val bluetoothManager = BluetoothManager(appContext)
    val mediaSessionManager = MediaSessionManager(appContext)

    // Guardar el ID de la última escena seleccionada
    var ultimaEscenaId by mutableLongStateOf(
        prefs.getLong("ultima_escena_id", -1L)
    )
        private set

    // --Guardar nombre del dino
    var dinoName by mutableStateOf(
        prefs.getString("dino_name", "Dino") ?: "Dino"
    )
        private set




    // --- ESTADOS DE HOME / GLOBAL ---

    var bateria by mutableIntStateOf(-1)
        private set

    var estadoBateria by mutableStateOf("Normal")
        private set


    var modoActual by mutableIntStateOf(
        prefs.getInt("modo_actual", 0)
    )
        private set


    var dinoEncendido by mutableStateOf(
        prefs.getBoolean("dino_encendido", false)
    )
        private set

    // --- ESTADOS DE COLORSSCREEN ---

    var brillo by mutableFloatStateOf(
        prefs.getFloat("brillo", 80f)
    )
        private set


    var currentColor by mutableStateOf(
        Color(prefs.getInt("current_color", Color.Red.toArgb()))
    )
        private set


    val favoritos = mutableStateListOf<Color?>().apply {

        addAll(List(5) { index ->

            if (prefs.contains("favorito_$index")) {

                Color(
                    prefs.getInt(
                        "favorito_$index",
                        Color.Transparent.toArgb()
                    )
                )

            } else {
                null
            }
        })
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
        configurarMediaSession()
        cargarEscenasLocales()
        iniciarProcesadorDeColores()
        intentarAutoConexion()
        configurarVolumen()
        iniciarActualizacionBateria()
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
        }
    }

    private fun iniciarActualizacionBateria() {
        viewModelScope.launch {
            while (true) {
                delay(60_000)

                if (bluetoothManager.isConnected()) {
                    bluetoothManager.send(DinoProtocol.BATTERY)
                }
            }
        }
    }


    fun cargarEscenasLocales() {

        listaEscenas.clear()

        listaEscenas.addAll(
            SceneManager.cargarTodasLasEscenas(appContext)
        )
    }


    private fun procesarMensaje(mensaje: String) {

        when {

            mensaje.startsWith(DinoProtocol.ACK) ->
                Log.d("DINO_ESP32", mensaje)


            mensaje.startsWith(DinoProtocol.INFO) ->
                Log.d("DINO_ESP32", mensaje)


            mensaje.startsWith(DinoProtocol.ERROR) ->
                Log.e("DINO_ESP32", mensaje)


            mensaje.startsWith(DinoProtocol.HELLO_RESPONSE) -> {

                Log.d("DINO_ESP32", "Firmware iniciado")

                viewModelScope.launch {

                    bluetoothManager.send(DinoProtocol.BATTERY)

                    if (dinoEncendido) {

                        sendCurrentColor()

                        bluetoothManager.send(
                            "${DinoProtocol.BRIGHTNESS}|${brillo.toInt()}"
                        )
                    }
                }
            }


            mensaje.startsWith(DinoProtocol.BATTERY_RESPONSE) -> {

                val partes = mensaje.split("|")

                val porcentaje = partes.getOrNull(1)?.toIntOrNull()

                if (porcentaje != null) {
                    bateria = porcentaje

                    estadoBateria = when {
                        porcentaje <= 10 -> "¡Carga a Dino!"
                        porcentaje <= 20 -> "Batería baja"
                        else -> "Normal"
                    }

                    val raw = partes.getOrNull(2)?.toIntOrNull()
                    val voltaje = partes.getOrNull(3)?.toFloatOrNull()

                    Log.d(
                        "DINO_BATTERY",
                        "Batería recibida: $porcentaje% | RAW: $raw | BAT: ${voltaje}V | Estado: $estadoBateria"
                    )
                }
            }

            mensaje.startsWith("BATINFO|") -> {

                val partes = mensaje.split("|")

                val raw = partes.getOrNull(1)
                val voltaje = partes.getOrNull(2)

                Log.d(
                    "DINO_BATTERY",
                    "RAW: $raw | BAT: ${voltaje}V"
                )
            }


            else -> Log.d("DINO_ESP32", mensaje)
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

        prefs.edit {

            putBoolean("dino_encendido", false)
            putInt("modo_actual", 0)
        }


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

        prefs.edit {

            putInt(
                "current_color",
                nuevoColor.toArgb()
            )
        }
    }


    fun updateBrillo(nuevoBrillo: Float) {

        brillo = nuevoBrillo

        prefs.edit {

            putFloat(
                "brillo",
                nuevoBrillo
            )
        }

        sendBrightness(nuevoBrillo.toInt())
    }


    fun sendCurrentColor() {

        dinoEncendido = true
        modoActual = 0

        prefs.edit {

            putBoolean("dino_encendido", true)
            putInt("modo_actual", 0)
        }


        enviarColorAlESP32(
            currentColor,
            persistir = true
        )
    }


    fun saveOrClearFavorite(
        index: Int,
        colorAsociado: Color
    ) {

        if (index in favoritos.indices) {

            favoritos[index] = colorAsociado

            prefs.edit {

                putInt(
                    "favorito_$index",
                    colorAsociado.toArgb()
                )
            }
        }
    }


    fun sendBrightness(value: Int) {

        viewModelScope.launch {

            bluetoothManager.send(
                "${DinoProtocol.BRIGHTNESS}|$value"
            )
        }
    }


    private fun enviarColorAlESP32(
        color: Color,
        persistir: Boolean = false
    ) {

        val hsv = FloatArray(3)

        android.graphics.Color.colorToHSV(
            color.toArgb(),
            hsv
        )


        Log.d(
            "DINO_COLOR_DEBUG",
            "📤 ENVIANDO AL ESP32 - H: ${hsv[0].toInt()}°, S: ${(hsv[1] * 100).toInt()}%, V: ${(hsv[2] * 100).toInt()}%"
        )

        Log.d(
            "DINO_COLOR_DEBUG",
            "📤 Color: ${color.toArgb().toString(16)}"
        )


        colorStreamChannel.trySend(color)


        if (persistir) {

            currentColor = color
            dinoEncendido = true
            modoActual = 0


            prefs.edit {

                putInt(
                    "current_color",
                    color.toArgb()
                )

                putBoolean(
                    "dino_encendido",
                    true
                )

                putInt(
                    "modo_actual",
                    0
                )
            }


            Log.d(
                "DINO_COLOR_DEBUG",
                "💾 Color persistido en SharedPreferences"
            )
        }
    }

    // 🔥 Para el arrastre (streaming)
    fun streamColorLive(color: Color) {

        val hsv = FloatArray(3)

        android.graphics.Color.colorToHSV(
            color.toArgb(),
            hsv
        )

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

    fun startLava() {
        ejecutarModo(3)
    }

    fun startArcoiris() {
        ejecutarModo(4)
    }

    fun startRespirar() {
        ejecutarModo(1)
    }

    fun startOcean() {
        ejecutarModo(2)
    }

    fun startForest() {
        ejecutarModo(5)
    }

    fun startParty() {
        ejecutarModo(6)
    }


    private fun ejecutarModo(idModo: Int) {

        modoActual = idModo
        ultimoModoId = idModo
        dinoEncendido = true
        animState = true


        val brilloInt = brillo
            .toInt()
            .coerceIn(0, 100)


        prefs.edit {

            putInt(
                "modo_actual",
                idModo
            )

            putInt(
                "ultimo_modo_id",
                idModo
            )

            putBoolean(
                "dino_encendido",
                true
            )
        }


        viewModelScope.launch {

            bluetoothManager.send(
                idModo.toString()
            )

            delay(30.milliseconds)

            bluetoothManager.send(
                "${DinoProtocol.BRIGHTNESS}|$brilloInt"
            )
        }
    }


    fun reactivarUltimoModo() {

        when (ultimoModoId) {

            1 -> startRespirar()
            2 -> startOcean()
            3 -> startLava()
            4 -> startArcoiris()
            5 -> startForest()
            6 -> startParty()

            else -> startLava()
        }
    }


    fun stopAnimation() {

        animState = false

        turnOffDino()
    }

    // --- ACCIONES DE ESCENAS ---

    private var estadoPrevioModo: Int = 0
    private var escenaPrevia: Escena? = null


    fun iniciarLiveScene(escenaEnEdicion: Escena? = null) {

        estadoPrevioModo = modoActual


        escenaPrevia = escenaEnEdicion?.copy()
            ?: if (ultimaEscenaId != -1L) {

                listaEscenas
                    .find { it.id == ultimaEscenaId }
                    ?.copy()

            } else {

                null
            }
    }


    fun previewEscenaEnVivo(escenaTemporal: Escena) {

        dinoEncendido = true


        val cantColores = escenaTemporal.colores.size

        val stringEscena = StringBuilder(
            "${DinoProtocol.SCENE}|${escenaTemporal.efecto.codigo}|${escenaTemporal.velocidad}|$cantColores"
        )


        escenaTemporal.colores.forEach { colorArgb ->

            val color = Color(colorArgb)

            val r = (color.red * 255).toInt()
            val g = (color.green * 255).toInt()
            val b = (color.blue * 255).toInt()

            stringEscena.append("|$r|$g|$b")
        }


        viewModelScope.launch {

            bluetoothManager.send(
                "${DinoProtocol.BRIGHTNESS}|${escenaTemporal.brillo}"
            )

            delay(20.milliseconds)

            bluetoothManager.send(
                stringEscena.toString()
            )
        }
    }


    fun previewBrilloEscena(brillo: Int) {

        viewModelScope.launch {

            bluetoothManager.send(
                "${DinoProtocol.BRIGHTNESS}|$brillo"
            )
        }
    }


    fun cancelarEdicionEscena() {

        viewModelScope.launch {

            val previa = escenaPrevia


            if (previa != null) {

                aplicarEscena(previa)

            } else {

                if (estadoPrevioModo == 0) {

                    sendCurrentColor()

                } else {

                    ejecutarModo(estadoPrevioModo)
                }
            }


            escenaPrevia = null
        }
    }


    fun aplicarEscena(escena: Escena) {

        dinoEncendido = true
        modoActual = 99

        ultimaEscenaId = escena.id


        prefs.edit {

            putBoolean(
                "dino_encendido",
                true
            )

            putInt(
                "modo_actual",
                99
            )

            putLong(
                "ultima_escena_id",
                escena.id
            )
        }


        val cantColores = escena.colores.size


        val stringEscena = StringBuilder(
            "${DinoProtocol.SCENE}|${escena.efecto.codigo}|${escena.velocidad}|$cantColores"
        )


        escena.colores.forEach { colorArgb ->

            val color = Color(colorArgb)

            val r = (color.red * 255).toInt()
            val g = (color.green * 255).toInt()
            val b = (color.blue * 255).toInt()

            stringEscena.append("|$r|$g|$b")
        }


        viewModelScope.launch {

            bluetoothManager.send(
                "${DinoProtocol.BRIGHTNESS}|${escena.brillo}"
            )

            bluetoothManager.send(
                stringEscena.toString()
            )
        }
    }

    // Función para reactivar la última escena si entramos estando apagados
    fun reactivarUltimaEscena() {

        if (ultimaEscenaId != -1L) {

            val escenaEncontrada =
                listaEscenas.find {
                    it.id == ultimaEscenaId
                }


            escenaEncontrada?.let {
                aplicarEscena(it)
            }
        }
    }


    fun guardarNuevaEscena(
        escena: Escena,
        esEdicion: Boolean
    ) {

        if (!esEdicion) {

            SceneManager.agregarEscena(
                appContext,
                escena
            )

        } else {

            SceneManager.actualizarEscena(
                appContext,
                escena
            )
        }


        cargarEscenasLocales()
    }


    fun borrarEscena(id: Long) {

        SceneManager.eliminarEscena(
            appContext,
            id
        )

        cargarEscenasLocales()
    }

    // -- Administracion de audio

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

            prefs.edit()
                .putString("dino_name", nombre)
                .apply()



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

        mediaSessionManager.onMediaChanged = { media ->

            mediaState = media
            musicManager.update(media)

            viewModelScope.launch {
                val duracionReloj = media.duration.formatAsTime()
                val posicionReloj = media.position.formatAsTime()
                bluetoothManager.send(
                    DinoProtocol.MUSIC_SONG +
                            "${media.title}|${media.artist}|$posicionReloj| $duracionReloj"
                )
            }


        }



        mediaSessionManager.onPlaybackChanged = { playing ->

            viewModelScope.launch {

                if (playing) {

                    bluetoothManager.send(
                        DinoProtocol.MUSIC_PLAY
                    )

                } else {

                    bluetoothManager.send(
                        DinoProtocol.MUSIC_PAUSE
                    )
                }
            }
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
                    DinoProtocol.VOLUME + volume
                )
            }
        }

        volumeManager.start()
    }




}
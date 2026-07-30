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




class DinoViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // 1. Canal con estrategia CONFLATED para los colores en movimiento
    private val colorStreamChannel = Channel<Color>(Channel.CONFLATED)

    // SharedPreferences compartidas del Dino
    private val prefs = context.getSharedPreferences("dino_settings", Context.MODE_PRIVATE)
    var ultimoModoId by mutableIntStateOf(prefs.getInt("ultimo_modo_id", 3)) // 3 (Lava) por defecto
        private set

    val bluetoothManager = BluetoothManager(context)
    // Guardar el ID de la última escena seleccionada
    var ultimaEscenaId by mutableStateOf(prefs.getLong("ultima_escena_id", -1L))
        private set


    // --- ESTADOS DE HOME / GLOBAL ---
    var bateria by mutableIntStateOf(-1)
        private set

    var modoActual by mutableIntStateOf(prefs.getInt("modo_actual", 0))
        private set

    var dinoEncendido by mutableStateOf(prefs.getBoolean("dino_encendido", false))
        private set

    // --- ESTADOS DE COLORSSCREEN ---
    var brillo by mutableFloatStateOf(prefs.getFloat("brillo", 80f))
        private set

    var currentColor by mutableStateOf(
        Color(prefs.getInt("current_color", Color.Red.toArgb()))
    )
        private set

    val favoritos = mutableStateListOf<Color?>().apply {
        addAll(List(5) { index ->
            if (prefs.contains("favorito_$index")) {
                Color(prefs.getInt("favorito_$index", Color.Transparent.toArgb()))
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

    init {
        // Configuración inicial
        configurarBluetooth()
        cargarEscenasLocales()

        // Escuchador con Throttle para el ColorPicker
        iniciarProcesadorDeColores()

        // 🟢 Intentar conectar automáticamente al iniciar la app
        intentarAutoConexion()
    }

    @OptIn(FlowPreview::class)
    private fun iniciarProcesadorDeColores() {
        viewModelScope.launch {
            colorStreamChannel.consumeAsFlow()
                .sample(60L) // Emitir como máximo cada 60ms al Bluetooth (~16 coms/sec)
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
            if (!bluetoothManager.isConnected() && bluetoothManager.state == BtState.DISCONNECTED) {
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

    fun cargarEscenasLocales() {
        listaEscenas.clear()
        listaEscenas.addAll(SceneManager.cargarTodasLasEscenas(context))
    }

    private fun procesarMensaje(mensaje: String) {
        when {
            mensaje.startsWith(DinoProtocol.ACK) -> Log.d("ESP32", mensaje)
            mensaje.startsWith(DinoProtocol.INFO) -> Log.d("ESP32", mensaje)
            mensaje.startsWith(DinoProtocol.ERROR) -> Log.e("ESP32", mensaje)

            mensaje.startsWith(DinoProtocol.HELLO_RESPONSE) -> {
                Log.d("ESP32", "Firmware iniciado")
                viewModelScope.launch {
                    bluetoothManager.send(DinoProtocol.BATTERY)

                    // Sincronización al reconectar
                    if (dinoEncendido) {
                        sendCurrentColor()
                        bluetoothManager.send("${DinoProtocol.BRIGHTNESS}|${brillo.toInt()}")

                    }
                }
            }

            mensaje.startsWith(DinoProtocol.BATTERY_RESPONSE) -> {
                mensaje.substringAfter("|").toIntOrNull()?.let { porcentaje ->
                    bateria = porcentaje
                }
            }

            else -> Log.d("ESP32", mensaje)
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
        prefs.edit().putBoolean("dino_encendido", false).putInt("modo_actual", 0).apply()

        viewModelScope.launch {
            bluetoothManager.send("0")
        }
    }



// --- ACCIONES DE COLORES ---

    fun updateCurrentColor(nuevoColor: Color) {
        currentColor = nuevoColor
        prefs.edit().putInt("current_color", nuevoColor.toArgb()).apply()
    }
    // 🔥 Función auxiliar para debug
    private fun logColorInfo(tag: String, h: Float, s: Float, v: Float) {
        Log.d("COLOR_DEBUG", "$tag - H: ${h.toInt()}°, S: ${(s * 100).toInt()}%, V: ${(v * 100).toInt()}%")
        Log.d("COLOR_DEBUG", "$tag - Color: ${Color.hsv(h, s, v)}")
    }

    fun updateBrillo(nuevoBrillo: Float) {
        brillo = nuevoBrillo
        prefs.edit().putFloat("brillo", nuevoBrillo).apply()
        sendBrightness(nuevoBrillo.toInt())
    }

    /**
     * Reenvía el color que está actualmente guardado en memoria.
     * Útil al salir de estado Apagado o al hacer click en el favorito activo.
     */
    fun sendCurrentColor() {
        dinoEncendido = true
        modoActual = 0
        prefs.edit().putBoolean("dino_encendido", true).putInt("modo_actual", 0).apply()
        // 🔥 Usar el método unificado en lugar de sendColor
        enviarColorAlESP32(currentColor, persistir = true)
    }

// 🔥 Este método ya no es necesario (reemplazado por enviarColorAlESP32)
// fun sendColor(color: Color) { ... }  // ELIMINAR

// 🔥 Este método ya no es necesario (reemplazado por enviarColorAlESP32)
// fun sendColorRGB(r: Int, g: Int, b: Int) { ... }  // ELIMINAR

    fun saveOrClearFavorite(index: Int, colorAsociado: Color) {
        if (index in favoritos.indices) {
            favoritos[index] = colorAsociado
            prefs.edit().putInt("favorito_$index", colorAsociado.toArgb()).apply()
        }
    }

    fun sendBrightness(value: Int) {
        viewModelScope.launch {
            bluetoothManager.send("${DinoProtocol.BRIGHTNESS}|$value")
        }
    }

    // 🔥 NUEVO: Método unificado para enviar colores (el ÚNICO que envía al ESP32)
    private fun enviarColorAlESP32(color: Color, persistir: Boolean = false) {

        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)

        // 🔥 LOG: Mostrar valores HSV que se envían al ESP32
        Log.d("COLOR_DEBUG", "📤 ENVIANDO AL ESP32 - H: ${hsv[0].toInt()}°, S: ${(hsv[1] * 100).toInt()}%, V: ${(hsv[2] * 100).toInt()}%")
        Log.d("COLOR_DEBUG", "📤 Color: ${color.toArgb().toString(16)}")


        // 1. Siempre enviar por el canal (throttled)
        colorStreamChannel.trySend(color)

        // 2. Si es final, persistir en SharedPreferences
        if (persistir) {
            currentColor = color
            dinoEncendido = true
            modoActual = 0
            prefs.edit()
                .putInt("current_color", color.toArgb())
                .putBoolean("dino_encendido", true)
                .putInt("modo_actual", 0)
                .apply()
            Log.d("COLOR_DEBUG", "💾 Color persistido en SharedPreferences")

        }
    }

    // 🔥 Para el arrastre (streaming)
    fun streamColorLive(color: Color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        Log.d("COLOR_DEBUG", "🔄 ARRASTRE - H: ${hsv[0].toInt()}°, S: ${(hsv[1] * 100).toInt()}%, V: ${(hsv[2] * 100).toInt()}%")
        enviarColorAlESP32(color, persistir = false)
    }

    // 🔥 Para el final del arrastre (persistir)
    fun sendColorFinal(red: Int, green: Int, blue: Int) {
        val color = Color(red, green, blue)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        Log.d("COLOR_DEBUG", "✅ FINAL - H: ${hsv[0].toInt()}°, S: ${(hsv[1] * 100).toInt()}%, V: ${(hsv[2] * 100).toInt()}%")
        enviarColorAlESP32(color, persistir = true)
    }




    // --- ACCIONES DE MODOS (MODESSCREEN) ---

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

        val brilloInt = brillo.toInt().coerceIn(0, 100)

        prefs.edit()
            .putInt("modo_actual", idModo)
            .putInt("ultimo_modo_id", idModo)
            .putBoolean("dino_encendido", true)
            .apply()

        viewModelScope.launch {
            bluetoothManager.send(idModo.toString())
            kotlinx.coroutines.delay(30)
            bluetoothManager.send("${DinoProtocol.BRIGHTNESS}|$brilloInt")
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
            else -> startLava() // Modo por defecto si no hay ninguno
        }
    }

    fun stopAnimation() {
        animState = false
        turnOffDino()
    }

    // --- ACCIONES DE ESCENAS ---
    private var estadoPrevioModo: Int = 0
    private var escenaPrevia: Escena? = null

    /**
     * Llama a esto justo cuando abras el Creador/Editor de Escenas
     */
    fun iniciarLiveScene(escenaEnEdicion: Escena? = null) {
        estadoPrevioModo = modoActual

        if (escenaEnEdicion != null) {
            // Si estamos EDITANDO una escena existente
            escenaPrevia = escenaEnEdicion.copy()
        } else if (ultimaEscenaId != -1L) {
            // Si estamos CREANDO una nueva pero ya había una escena reproduciéndose antes
            escenaPrevia = listaEscenas.find { it.id == ultimaEscenaId }?.copy()
        } else {
            escenaPrevia = null
        }
    }

    /**
     * Llama a esto en CADA cambio de la UI dentro del Creador de Escenas
     * (cuando agreguen un color, muevan el slider de velocidad, etc.)
     */
    /**
     * Llama a esto en CADA cambio de la UI dentro del Creador de Escenas
     */
    fun previewEscenaEnVivo(escenaTemporal: Escena) {
        dinoEncendido = true

        val cantColores = escenaTemporal.colores.size
        val stringEscena = StringBuilder("${DinoProtocol.SCENE}|${escenaTemporal.efecto.codigo}|${escenaTemporal.velocidad}|$cantColores")

        escenaTemporal.colores.forEach { colorArgb ->
            val color = Color(colorArgb)
            val r = (color.red * 255).toInt()
            val g = (color.green * 255).toInt()
            val b = (color.blue * 255).toInt()
            stringEscena.append("|$r|$g|$b")
        }

        viewModelScope.launch {
            // 🟢 Sincronizar brillo y enviar la escena de inmediato
            bluetoothManager.send("${DinoProtocol.BRIGHTNESS}|${escenaTemporal.brillo}")
            kotlinx.coroutines.delay(20) // Pequeño margen entre tramas seriales
            bluetoothManager.send(stringEscena.toString())
        }
    }

    // 🟢 Manten una función separada para cuando cambien el brillo en el slider de la escena
    fun previewBrilloEscena(brillo: Int) {
        viewModelScope.launch {
            bluetoothManager.send("${DinoProtocol.BRIGHTNESS}|$brillo")
        }
    }

    /**
     * Llama a esto si el usuario presiona "Cancelar" o regresa sin guardar
     */
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
        modoActual = 99 // 👈 IMPORTANTE: Marcamos que el modo actual ahora es Escena (99)
        ultimaEscenaId = escena.id
        prefs.edit()
            .putBoolean("dino_encendido", true)
            .putInt("modo_actual", 99)
            .putLong("ultima_escena_id", escena.id)
            .apply()

        val cantColores = escena.colores.size
        val stringEscena = StringBuilder("${DinoProtocol.SCENE}|${escena.efecto.codigo}|${escena.velocidad}|$cantColores")
        escena.colores.forEach { colorArgb ->
            val color = Color(colorArgb)
            val r = (color.red * 255).toInt()
            val g = (color.green * 255).toInt()
            val b = (color.blue * 255).toInt()
            stringEscena.append("|$r|$g|$b")
        }

        viewModelScope.launch {
            bluetoothManager.send("${DinoProtocol.BRIGHTNESS}|${escena.brillo}")
            bluetoothManager.send(stringEscena.toString())
        }
    }

    // Función para reactivar la última escena si entramos estando apagados
    fun reactivarUltimaEscena() {
        if (ultimaEscenaId != -1L) {
            val escenaEncontrada = listaEscenas.find { it.id == ultimaEscenaId }
            escenaEncontrada?.let { aplicarEscena(it) }
        }
    }

    fun guardarNuevaEscena(escena: Escena, esEdicion: Boolean) {
        if (!esEdicion) {
            SceneManager.agregarEscena(context, escena)
        } else {
            SceneManager.actualizarEscena(context, escena)
        }
        cargarEscenasLocales()
    }

    fun borrarEscena(id: Long) {
        SceneManager.eliminarEscena(context, id)
        cargarEscenasLocales()
    }
}
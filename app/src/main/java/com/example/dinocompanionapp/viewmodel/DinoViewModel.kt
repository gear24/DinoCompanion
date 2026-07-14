package com.example.dinocompanionapp.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dinocompanionapp.data.*
import kotlinx.coroutines.launch

class DinoViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val bluetoothManager = BluetoothManager(context)

    // Estados de navegación y UI General
    var pantalla by mutableStateOf("home")
    var mensaje by mutableStateOf("")
    var ultimoMensaje by mutableStateOf("")
    var bateria by mutableIntStateOf(-1)

    // SharedPreferences locales
    private val prefs = context.getSharedPreferences("dino_settings", Context.MODE_PRIVATE)

    // Estados de iluminación persistentes
    var brillo by mutableStateOf(prefs.getFloat("brillo", 80f))
    var brilloModos by mutableStateOf(prefs.getFloat("brillo_modos", 60f))
    var modoActual by mutableStateOf(prefs.getInt("modo_actual", 0))
    var dinoEncendido by mutableStateOf(prefs.getBoolean("dino_encendido", false))

    var currentColor by mutableStateOf(Color(prefs.getInt("current_color", Color.Red.toArgb())))
    var favoritos by mutableStateOf(cargarFavoritos Iniciales())

    // Estado de la lista de escenas dinámicas
    var escenas = mutableStateOf(SceneManager.cargarTodasLasEscenas(context))

    init {
        // Vinculamos la escucha de mensajes del hardware
        bluetoothManager.onMessageReceived = { message ->
            procesarMensaje(message)
        }

        // Conexión inicial automática
        conectarDino()
    }

    fun conectarDino() {
        viewModelScope.launch {
            bluetoothManager.connect()
        }
    }

    fun desconectarDino() {
        bluetoothManager.disconnect()
    }

    private fun procesarMensaje(message: String) {
        ultimoMensaje = message
        when {
            message.startsWith(DinoProtocol.ACK) -> Log.d("ESP32", message)
            message.startsWith(DinoProtocol.HELLO_RESPONSE) -> {
                Log.d("ESP32", "Firmware iniciado")
                bluetoothManager.send(DinoProtocol.BATTERY)
            }
            message.startsWith(DinoProtocol.BATTERY_RESPONSE) -> {
                message.substringAfter("|").toIntOrNull()?.let { bateria = it }
            }
            message.startsWith(DinoProtocol.INFO) -> Log.d("ESP32", message)
            message.startsWith(DinoProtocol.ERROR) -> Log.e("ESP32", message)
            else -> Log.d("ESP32", message)
        }
    }

    // --- MÉTODOS DE ENVÍO BLUETOOTH ---
    fun sendBT(message: String): Boolean = bluetoothManager.send(message)

    fun sendMode(mode: Int) {
        modoActual = mode
        dinoEncendido = (mode != 0)
        prefs.edit().putInt("modo_actual", mode).putBoolean("dino_encendido", dinoEncendido).apply()
        bluetoothManager.send(mode.toString())
    }

    fun sendColor(color: Color) {
        currentColor = color
        prefs.edit().putInt("current_color", color.toArgb()).apply()

        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        bluetoothManager.send("$r,$g,$b")
    }

    fun sendBrightness(value: Int, campoPref: String) {
        if (campoPref == "brillo") brillo = value.toFloat() else brilloModos = value.toFloat()
        prefs.edit().putFloat(campoPref, value.toFloat()).apply()
        bluetoothManager.send("${DinoProtocol.BRIGHTNESS}|$value")
    }

    fun aplicarEscena(escena: Escena) {
        val velocidad = 6
        val cantColores = escena.colores.size
        val stringEscena = StringBuilder("${DinoProtocol.SCENE}|${escena.efecto.codigo}|$velocidad|$cantColores")

        escena.colores.forEach { colorArgb ->
            val color = Color(colorArgb)
            stringEscena.append("|${(color.red * 255).toInt()}|${(color.green * 255).toInt()}|${(color.blue * 255).toInt()}")
        }
        sendBrightness(escena.brillo, "brillo_modos")
        sendBT(stringEscena.toString())
    }

    // --- GESTIÓN DE FAVORITOS Y ESCENAS ---
    private fun cargarFavoritosIniciales(): List<Color?> {
        return List(5) { index ->
            if (prefs.contains("favorito_$index")) {
                Color(prefs.getInt("favorito_$index", Color.Transparent.toArgb()))
            } else null
        }
    }

    fun guardarColorFavorito(index: Int, color: Color) {
        favoritos = favoritos.toMutableList().also { it[index] = color }
        prefs.edit().putInt("favorito_$index", color.toArgb()).apply()
    }

    fun agregarNuevaEscena(escena: Escena) {
        SceneManager.agregarEscena(context, escena)
        actualizarListaEscenas()
    }

    fun modificarEscena(escena: Escena) {
        SceneManager.actualizarEscena(context, escena)
        actualizarListaEscenas()
    }

    fun eliminarEscena(id: Long) {
        SceneManager.eliminarEscena(context, id)
        actualizarListaEscenas()
    }

    private fun actualizarListaEscenas() {
        escenas.value = SceneManager.cargarTodasLasEscenas(context)
    }
}
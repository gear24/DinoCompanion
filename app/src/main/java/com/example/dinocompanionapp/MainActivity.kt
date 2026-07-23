package com.example.dinocompanionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.material3.*
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.dinocompanionapp.ui.theme.DinoCompanionAppTheme

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import android.bluetooth.BluetoothAdapter

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll



import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.rememberCoroutineScope
import com.example.dinocompanionapp.bluetooth.BluetoothManager
import com.example.dinocompanionapp.data.BtState
import com.example.dinocompanionapp.data.DinoProtocol
import kotlinx.coroutines.launch

import com.example.dinocompanionapp.data.*
import com.example.dinocompanionapp.ui.screens.*
import com.example.dinocompanionapp.ui.components.*
import com.example.dinocompanionapp.viewmodel.DinoViewModel
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.dinocompanionapp.ui.theme.*
import kotlinx.coroutines.Dispatchers

/*
val SPP_UUID: UUID = UUID.fromString(
    "00001101-0000-1000-8000-00805F9B34FB"
)
 */

class MainActivity : ComponentActivity() {

    private val dinoViewModel: DinoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            DinoCompanionAppTheme {
                var pantalla by remember { mutableStateOf("home") }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val modifierConPadding = Modifier.padding(innerPadding)

                    when (pantalla) {
                        "home" -> {
                            HomeScreen(
                                viewModel = dinoViewModel,
                                bluetoothManager = dinoViewModel.bluetoothManager,
                                onNavigateToColors = { pantalla = "colors" },
                                onNavigateToModes = { pantalla = "modes" },
                                onNavigateToScenes = { pantalla = "scenes" },
                                modifier = modifierConPadding
                            )
                        }
                        "colors" -> {
                            ColorsScreen(
                                currentColor = dinoViewModel.currentColor,
                                brillo = dinoViewModel.brillo,
                                favoritos = dinoViewModel.favoritos,
                                onColorChangedInPicker = { color -> dinoViewModel.updateCurrentColor(color) },
                                onColorStream = { color -> dinoViewModel.streamColorLive(color) },
                                onBrilloChanged = { nBrillo -> dinoViewModel.updateBrillo(nBrillo) },
                                onFavoritoClick = { colorFav -> dinoViewModel.sendColor(colorFav) },
                                onFavoritoLongClick = { index, color -> dinoViewModel.saveOrClearFavorite(index, color) },
                                onSendColor = { r, g, b -> dinoViewModel.sendColorRGB(r, g, b) },
                                onReactivarColor = {
                                    if (!dinoViewModel.dinoEncendido) {
                                        dinoViewModel.sendCurrentColor()
                                    }
                                },
                                onBackToHome = { pantalla = "home" },
                                modifier = modifierConPadding
                            )
                        }
                        "modes" -> {
                            ModesScreen(
                                animState = dinoViewModel.animState,
                                speed = dinoViewModel.speed,
                                onSpeedChanged = { nSpeed -> dinoViewModel.updateSpeed(nSpeed) },
                                onStartLava = { dinoViewModel.startLava() },
                                onStartArcoiris = { dinoViewModel.startArcoiris() },
                                onStartRespirar = { dinoViewModel.startRespirar() },
                                onStartOcean = { dinoViewModel.startOcean() },
                                onStartForest = { dinoViewModel.startForest() },
                                onStartParty = { dinoViewModel.startParty() },
                                onStopAnimation = { dinoViewModel.stopAnimation() },
                                onReactivarModo = {
                                    if (!dinoViewModel.dinoEncendido) {
                                        dinoViewModel.reactivarUltimoModo()
                                    }
                                },
                                onBackToHome = { pantalla = "home" },
                                modifier = modifierConPadding
                            )
                        }
                        "scenes" -> {
                            ScenesScreen(
                                listaEscenas = dinoViewModel.listaEscenas,
                                viewModel = dinoViewModel,
                                onAplicarEscena = { escena -> dinoViewModel.aplicarEscena(escena) },
                                onBorrarEscena = { id -> dinoViewModel.borrarEscena(id) },
                                onGuardarNuevaEscena = { nuevaEscena ->
                                    dinoViewModel.guardarNuevaEscena(nuevaEscena, esEdicion = false)
                                },
                                onActualizarEscena = { escenaEditada ->
                                    dinoViewModel.guardarNuevaEscena(escenaEditada, esEdicion = true)
                                },
                                onSendColorRGB = { r, g, b -> dinoViewModel.sendColorRGB(r, g, b) },
                                onReactivarEscena = {
                                    if (!dinoViewModel.dinoEncendido) {
                                        dinoViewModel.reactivarUltimaEscena()
                                    }
                                },
                                onBackToHome = { pantalla = "home" },
                                modifier = modifierConPadding
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HomeScreen(
        viewModel: DinoViewModel,
        bluetoothManager: BluetoothManager,
        onNavigateToColors: () -> Unit,
        onNavigateToModes: () -> Unit,
        onNavigateToScenes: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        var mensaje by remember { mutableStateOf("") }
        var bateria by remember { mutableIntStateOf(-1) }
        val scope = rememberCoroutineScope()
        val context = androidx.compose.ui.platform.LocalContext.current

        fun procesarMensaje(message: String) {
            when {
                message.startsWith(DinoProtocol.ACK) -> Log.d("ESP32", message)

                mensaje.startsWith(DinoProtocol.HELLO_RESPONSE) -> {
                    Log.d("ESP32", "Firmware iniciado")
                    // 🟢 Envolver en corrutina
                    lifecycleScope.launch {
                        bluetoothManager.send(DinoProtocol.BATTERY)
                    }
                }

                message.startsWith(DinoProtocol.BATTERY_RESPONSE) -> {
                    val porcentaje = message.substringAfter("|").toIntOrNull()
                    if (porcentaje != null) bateria = porcentaje
                }
                message.startsWith(DinoProtocol.INFO) -> Log.d("ESP32", message)
                message.startsWith(DinoProtocol.ERROR) -> Log.e("ESP32", message)
                else -> Log.d("ESP32", message)
            }
        }

        LaunchedEffect(bluetoothManager) {
            bluetoothManager.onMessageReceived = { message -> procesarMensaje(message) }
        }

        val textoConexion = when (bluetoothManager.state) {
            BtState.DISCONNECTED -> "🔴 Conectar Dino"
            BtState.CONNECTING -> "🟡 Conectando..."
            BtState.CONNECTED -> "🟢 Desconectar Dino"
            BtState.RECONNECTING -> "🟠 Reconectando..."
            BtState.ERROR -> "🔴 Conectar Dino"
        }

        // Launcher ejecutando la conexión en hilo secundario IO
        val enableBtLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) {
                scope.launch(Dispatchers.IO) { bluetoothManager.connect() }
            }
        }

        val prefs = remember { context.getSharedPreferences("dino_settings", Context.MODE_PRIVATE) }
        var dinoEncendido by remember { mutableStateOf(prefs.getBoolean("dino_encendido", false)) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Pink)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Dino Companion", style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(24.dp))

            // Botón de conexión con ejecución en Dispatchers.IO
            DinoButton(textoConexion) {
                when (bluetoothManager.state) {
                    BtState.DISCONNECTED, BtState.ERROR -> {
                        if (BluetoothAdapter.getDefaultAdapter()?.isEnabled != true) {
                            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        } else {
                            scope.launch(Dispatchers.IO) { bluetoothManager.connect() }
                        }
                    }
                    BtState.CONNECTED -> { bluetoothManager.disconnect() }
                    else -> {}
                }
            }

            Spacer(Modifier.height(8.dp))

            // Texto de estado principal
            Text(
                text = bluetoothManager.statusText,
                color = Color.White
            )

            // Detalle del error en blanco
            bluetoothManager.lastError?.let { errorText ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = errorText,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (bateria >= 0) {
                Spacer(Modifier.height(4.dp))
                Text("🔋 Batería: $bateria%", color = Color.White)
            }

            Spacer(Modifier.height(24.dp))
            DinoCard("🏠 Home del Dino")
            Spacer(Modifier.height(16.dp))

            DinoButton("Colores Favoritos") { onNavigateToColors() }
            DinoButton("💡 Modos de Luz") { onNavigateToModes() }
            DinoButton("Creador de Escenas") { onNavigateToScenes() }

            DinoButton("⛔ Apagar") {
                viewModel.turnOffDino() // 🟢 Llama al ViewModel, que usará el BluetoothManager que SÍ está conectado
            }

            if (mensaje.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                Text(mensaje, color = Color.White)
            }
        }
    }
}
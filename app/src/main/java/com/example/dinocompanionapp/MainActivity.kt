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
import com.example.dinocompanionapp.ui.theme.DinoCompanionAppTheme

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import android.bluetooth.BluetoothAdapter

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

import com.example.dinocompanionapp.ui.screens.*
import com.example.dinocompanionapp.ui.components.*
import androidx.activity.viewModels
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.lifecycleScope
import com.example.dinocompanionapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import com.example.dinocompanionapp.viewmodel.DinoViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.Manifest
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import androidx.compose.ui.platform.LocalContext


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
                                onFavoritoClick = { colorFav ->
                                    dinoViewModel.sendColorFinal(
                                        (colorFav.red * 255).toInt(),
                                        (colorFav.green * 255).toInt(),
                                        (colorFav.blue * 255).toInt()
                                    )
                                },                                onFavoritoLongClick = { index, color -> dinoViewModel.saveOrClearFavorite(index, color) },
                                onSendColor = { r, g, b -> dinoViewModel.sendColorFinal(r, g, b) },
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
                                brightness = dinoViewModel.brillo, // 👈 Usa el brillo unificado
                                onBrightnessChanged = { nuevoBrillo -> dinoViewModel.updateBrillo(nuevoBrillo) }, // 👈 Usa la función única
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
                                onSendColorRGB = { r, g, b -> dinoViewModel.sendColorFinal(r, g, b) },
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
        var audioPermission by remember {
            mutableStateOf(dinoViewModel.hasAudioPermission())
        }
        val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            bluetoothManager.checkBluetoothStatus()
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        var batteryOptimization by remember {
            mutableStateOf(true)
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->

                if (event == Lifecycle.Event.ON_RESUME) {
                    audioPermission = dinoViewModel.hasAudioPermission()
                    val powerManager =
                        context.getSystemService(Context.POWER_SERVICE) as PowerManager

                    batteryOptimization =
                        powerManager.isIgnoringBatteryOptimizations(
                            context.packageName
                        )
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        val media = dinoViewModel.mediaState
        var mostrarNombreDialog by remember {
            mutableStateOf(false)
        }

        var nuevoNombre by remember {
            mutableStateOf("")
        }



        fun procesarMensaje(message: String) {
            when {
                message.startsWith(DinoProtocol.ACK) -> {
                    Log.d("DINO_ESP32", message)

                    if (message.contains("HELLO")) {
                        bluetoothManager.updateDeviceName(dinoViewModel.dinoName)
                        Log.d("DINO_BT", "Nombre sincronizado: ${dinoViewModel.dinoName}")

                        lifecycleScope.launch {
                            bluetoothManager.send(DinoProtocol.BATTERY)
                        }
                    }
                }
                message.startsWith(DinoProtocol.HELLO_RESPONSE) -> {
                    Log.d("DINO_ESP32", "Firmware iniciado")
                    // 🟢 Envolver en corrutina
                    lifecycleScope.launch {
                        bluetoothManager.send(DinoProtocol.BATTERY)
                    }
                }


                message.startsWith(DinoProtocol.BATTERY_RESPONSE) -> {
                    val porcentaje = message.substringAfter("|").toIntOrNull()

                    if (porcentaje != null) {
                        bateria = porcentaje
                        Log.d("DINO_ESP32", "Batería recibida: $porcentaje%")
                    }
                }

                message.startsWith(DinoProtocol.INFO) -> Log.d("DINO_ESP32", message)
                message.startsWith(DinoProtocol.ERROR) -> Log.e("DINO_ESP32", message)
                else -> Log.d("DINO_ESP32", message)
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

        LaunchedEffect(Unit) {
            bluetoothManager.checkBluetoothStatus()

            if (!dinoViewModel.hasBtPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bluetoothPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    )
                }
            }
        }


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
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (bateria >= 0) {
                Spacer(Modifier.height(4.dp))
                Text("🔋 Batería: $bateria%", color = Color.White)
            }

            Spacer(Modifier.height(24.dp))
            DinoCard("🏠 Home de ${viewModel.dinoName}")
            Spacer(Modifier.height(8.dp))

            DinoButton("✏️ Cambiar nombre") {

                nuevoNombre = viewModel.dinoName
                mostrarNombreDialog = true

            }
            Spacer(Modifier.height(16.dp))

            DinoButton("Colores Favoritos") { onNavigateToColors() }
            DinoButton("💡 Modos de Luz") { onNavigateToModes() }
            DinoButton("Creador de Escenas") { onNavigateToScenes() }
            if (!audioPermission) {

                DinoButton("🎵 Activar música") {
                    dinoViewModel.requestAudioPermission()
                }

            } else {

                DinoCard {

                    Text(
                        "🎵 Música",
                        color = Dark
                    )

                    if (media.title.isNotBlank()) {

                        Text("Titulo ", color=Dark)
                        Text(
                            media.title,
                            color = Color.Blue
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Artista ", color=Dark)
                        Text(
                            media.artist,
                            color = Color.Blue
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Album ", color=Dark)
                        Text(
                            media.album,
                            color = Color.Blue
                        )

                    } else {

                        Text(
                            "Esperando música...",
                            color = Dark
                        )
                    }
                }
            }


            if (!batteryOptimization) {

                DinoCard {

                    Text(
                        "🔋 Optimización de batería",
                        color = Dark
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Para mantener la conexión con Dino y actualizar la música en segundo plano, se recomienda usar \"Sin restricciones\".",
                        color = Dark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    DinoButton("⚙️ Configurar batería") {

                        val intent = Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = Uri.parse("package:${context.packageName}")
                        }

                        context.startActivity(intent)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
            DinoButton("⛔ Apagar") {
                viewModel.turnOffDino() // 🟢 Llama al ViewModel, que usará el BluetoothManager que SÍ está conectado
            }
            if (mostrarNombreDialog) {

                AlertDialog(

                    onDismissRequest = {
                        mostrarNombreDialog = false
                    },

                    title = {
                        Text("Cambiar nombre del Dino")
                    },

                    text = {

                        TextField(
                            value = nuevoNombre,
                            onValueChange = {
                                nuevoNombre = it
                            },
                            placeholder = {
                                Text(viewModel.dinoName)
                            }
                        )

                    },

                    confirmButton = {

                        TextButton(

                            onClick = {

                                viewModel.cambiarNombreDesdeUI(nuevoNombre)

                                mostrarNombreDialog = false

                            }

                        ) {

                            Text("Aceptar y reiniciar")

                        }

                    },

                    dismissButton = {

                        TextButton(

                            onClick = {
                                mostrarNombreDialog = false
                            }

                        ) {

                            Text("Cancelar")

                        }

                    }
                )
            }

            if (mensaje.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                Text(mensaje, color = Color.White)
            }
        }
    }
}
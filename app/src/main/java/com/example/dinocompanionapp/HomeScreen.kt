package com.example.dinocompanionapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.dinocompanionapp.bluetooth.BluetoothManager
import com.example.dinocompanionapp.data.BtState
import com.example.dinocompanionapp.ui.components.DinoButton
import com.example.dinocompanionapp.ui.components.DinoCard
import com.example.dinocompanionapp.ui.theme.Dark
import com.example.dinocompanionapp.ui.theme.Pink
import com.example.dinocompanionapp.viewmodel.DinoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


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
    val scope = rememberCoroutineScope()
    var audioPermission by remember {
        mutableStateOf(viewModel.hasAudioPermission())
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
                audioPermission = viewModel.hasAudioPermission()
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

    val media = viewModel.mediaState
    var mostrarNombreDialog by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf("") }

    val textoConexion = when (bluetoothManager.state) {
        BtState.DISCONNECTED -> "🔴 Conectar Dino"
        BtState.CONNECTING -> "🟡 Conectando..."
        BtState.CONNECTED -> "🟢 Desconectar Dino"
        BtState.RECONNECTING -> "🟠 Reconectando..."
        BtState.ERROR -> "🔴 Conectar Dino"
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) {
            scope.launch(Dispatchers.IO) { bluetoothManager.connect() }
        }
    }

    LaunchedEffect(Unit) {
        bluetoothManager.checkBluetoothStatus()

        if (!viewModel.hasBtPermission()) {
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

        Text(
            text = bluetoothManager.statusText,
            color = Color.White
        )

        bluetoothManager.lastError?.let { errorText ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = errorText,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 🟢 AHORA LEEMOS LA BATERÍA DIRECTO DESDE EL VIEWMODEL:
        if (viewModel.bateria >= 0) {
            Spacer(Modifier.height(4.dp))
            Text("🔋 Batería: ${viewModel.bateria}% (${viewModel.estadoBateria})", color = Color.White)
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
                viewModel.requestAudioPermission()
            }
        } else {
            DinoCard {
                Text(
                    "🎵 Música",
                    color = Dark
                )

                if (media.title.isNotBlank()) {
                    Text("Titulo ", color=Dark)
                    Text(media.title, color = Color.Blue)
                    Spacer(Modifier.height(6.dp))
                    Text("Artista ", color=Dark)
                    Text(media.artist, color = Color.Blue)
                    Spacer(Modifier.height(6.dp))
                    Text("Album ", color=Dark)
                    Text(media.album, color = Color.Blue)
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
            viewModel.turnOffDino()
        }

        if (mostrarNombreDialog) {
            AlertDialog(
                onDismissRequest = { mostrarNombreDialog = false },
                title = { Text("Cambiar nombre del Dino") },
                text = {
                    TextField(
                        value = nuevoNombre,
                        onValueChange = { nuevoNombre = it },
                        placeholder = { Text(viewModel.dinoName) }
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
                        onClick = { mostrarNombreDialog = false }
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


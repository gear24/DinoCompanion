package com.example.dinocompanionapp.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dinocompanionapp.data.BtState
import com.example.dinocompanionapp.ui.components.DinoButton
import com.example.dinocompanionapp.ui.components.DinoCard
import com.example.dinocompanionapp.ui.theme.Pink
import com.example.dinocompanionapp.viewmodel.DinoViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: DinoViewModel,
    onNavigateToColors: () -> Unit,
    onNavigateToModes: () -> Unit,
    onNavigateToScenes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bluetoothManager = viewModel.bluetoothManager

    // Configuración del Launcher para solicitar activar Bluetooth si está apagado
    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) {
            scope.launch {
                bluetoothManager.connect()
            }
        }
    }

    // Mapeo exacto de los estados de conexión para el texto del botón
    val textoConexion = when (bluetoothManager.state) {
        BtState.DISCONNECTED -> "🔴 Conectar Dino"
        BtState.CONNECTING -> "🟡 Conectando..."
        BtState.CONNECTED -> "🟢 Desconectar Dino"
        BtState.RECONNECTING -> "🟠 Reconectando..."
        BtState.ERROR -> "🔴 Conectar Dino"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Pink)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dino Companion")

        Spacer(Modifier.height(24.dp))

        // Botón del estado de conexión original
        DinoButton(textoConexion) {
            when (bluetoothManager.state) {
                BtState.DISCONNECTED,
                BtState.ERROR -> {
                    if (BluetoothAdapter.getDefaultAdapter()?.isEnabled != true) {
                        enableBtLauncher.launch(
                            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        )
                    } else {
                        scope.launch {
                            bluetoothManager.connect()
                        }
                    }
                }
                BtState.CONNECTED -> {
                    bluetoothManager.disconnect()
                }
                else -> {}
            }
        }

        Spacer(Modifier.height(8.dp))

        // Textos informativos de estado del BluetoothManager
        Text(bluetoothManager.statusText)

        bluetoothManager.lastError?.let { error ->
            Spacer(Modifier.height(4.dp))
            Text(error)
        }

        // Lectura de la batería centralizada en el ViewModel
        if (viewModel.bateria >= 0) {
            Text("🔋 ${viewModel.bateria}%")
        }

        Spacer(Modifier.height(16.dp))

        DinoCard("🏠 Home del Dino")

        Spacer(Modifier.height(16.dp))

        // Botones de navegación hacia las demás pantallas
        DinoButton("Colores Favoritos") {
            onNavigateToColors()
        }

        DinoButton("💡 Modos de Luz") {
            onNavigateToModes()
        }

        DinoButton("Creador de Escenas") {
            onNavigateToScenes()
        }

        DinoButton("⛔ Apagar") {
            viewModel.turnOffDino()
        }
    }
}
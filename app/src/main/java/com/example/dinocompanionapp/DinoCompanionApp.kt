
package com.example.dinocompanionapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.dinocompanionapp.ui.theme.DinoCompanionAppTheme
import com.example.dinocompanionapp.viewmodel.DinoColorsViewModel
import com.example.dinocompanionapp.viewmodel.DinoColorsViewModelFactory
import com.example.dinocompanionapp.viewmodel.DinoModesViewModel
import com.example.dinocompanionapp.viewmodel.DinoModesViewModelFactory
import com.example.dinocompanionapp.viewmodel.DinoScenesViewModel
import com.example.dinocompanionapp.viewmodel.DinoScenesViewModelFactory
import com.example.dinocompanionapp.viewmodel.DinoViewModel
import android.app.Application



@Composable
fun DinoCompanionApp(
    application: Application,
    dinoViewModel: DinoViewModel
) {
    val colorsViewModel: DinoColorsViewModel = viewModel(
        factory = DinoColorsViewModelFactory(
            application = dinoViewModel.getApplication(),
            bluetoothManager = dinoViewModel.bluetoothManager,
            onDinoEncendidoChanged = { encendido ->
                dinoViewModel.actualizarDinoEncendido(encendido)
            },
            onModoActualChanged = { modo ->
                dinoViewModel.actualizarModoActual(modo)
            }
        )
    )

    val modesViewModel: DinoModesViewModel = viewModel(
        factory = DinoModesViewModelFactory(
            application = dinoViewModel.getApplication(),
            modeManager = dinoViewModel.modeManager,
            getModoActual = {
                dinoViewModel.modoActual
            },
            onBrilloChanged = { brillo ->
                dinoViewModel.sendBrightness(brillo.toInt())
            }
        )
    )

    val scenesViewModel: DinoScenesViewModel = viewModel(
        factory = DinoScenesViewModelFactory(
            application = dinoViewModel.getApplication(),
            sceneManager = dinoViewModel.sceneManager,
            getModoActual = { dinoViewModel.modoActual },
            getUltimaEscenaId = { dinoViewModel.ultimaEscenaId },
            onRestoreColor = {
                dinoViewModel.reactivarColorActual()
            },
            onRestoreMode = { modo ->
                dinoViewModel.modeManager.ejecutarModo(modo)
            }
        )
    )

    DinoCompanionAppTheme {
        var pantalla by remember { mutableStateOf("home") }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val modifierConPadding = Modifier.padding(innerPadding)

            AppNavigation(
                dinoViewModel = dinoViewModel,
                colorsViewModel = colorsViewModel,
                modesViewModel = modesViewModel,
                scenesViewModel = scenesViewModel,
                pantalla = pantalla,
                onNavigate = { pantalla = it },
                modifier = modifierConPadding
            )
        }
    }


}

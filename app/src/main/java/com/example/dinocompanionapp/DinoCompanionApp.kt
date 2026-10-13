
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
import com.example.dinocompanionapp.ui.screens.ColorsScreen
import com.example.dinocompanionapp.ui.screens.ModesScreen
import com.example.dinocompanionapp.ui.screens.ScenesScreen
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
                        currentColor = colorsViewModel.currentColor,
                        brilloColor = colorsViewModel.brilloColor,
                        favoritos = colorsViewModel.favoritos,
                        favoritoActivo = colorsViewModel.favoritoActivo,
                        favoritoModificado = colorsViewModel.favoritoModificado,
                        onColorChangedInPicker = {
                            colorsViewModel.updateCurrentColor(it)
                        },
                        onColorStream = {
                            colorsViewModel.streamColorLive(it)
                        },
                        onBrilloChanged = {
                            colorsViewModel.updateBrilloColor(it)
                        },
                        onFavoritoClick = { favorito ->
                            val index = colorsViewModel.favoritos.indexOf(favorito)
                            if (index != -1) {
                                colorsViewModel.activarFavorito(index)
                            }
                        },
                        onFavoritoLongClick = { index, color, brillo ->
                            colorsViewModel.saveOrClearFavorite(
                                index,
                                color,
                                brillo
                            )
                        },
                        onSendColor = { r, g, b ->
                            colorsViewModel.sendColorFinal(r, g, b)
                        },
                        onReactivarColor = {
                            if (!dinoViewModel.dinoEncendido) {
                                colorsViewModel.sendCurrentColor()
                            }
                        },
                        onBackToHome = { pantalla = "home" },
                        modifier = modifierConPadding
                    )
                }

                "modes" -> {
                    ModesScreen(
                        animState = dinoViewModel.animState,
                        brightness = modesViewModel.brilloModo(
                            dinoViewModel.modoActual
                        ),
                        onBrightnessChanged = { nuevoBrillo ->
                            modesViewModel.updateBrilloModo(
                                dinoViewModel.modoActual,
                                nuevoBrillo
                            )
                        },
                        onStartLava = { modesViewModel.startLava() },
                        onStartArcoiris = { modesViewModel.startArcoiris() },
                        onStartRespirar = { modesViewModel.startRespirar() },
                        onStartOcean = { modesViewModel.startOcean() },
                        onStartForest = { modesViewModel.startForest() },
                        onStartParty = { modesViewModel.startParty() },
                        onStopAnimation = { dinoViewModel.stopAnimation() },
                        onReactivarModo = {
                            if (!dinoViewModel.dinoEncendido) {
                                modesViewModel.reactivarUltimoModo(
                                    dinoViewModel.ultimoModoId
                                )
                            }
                        },
                        onBackToHome = { pantalla = "home" },
                        modifier = modifierConPadding
                    )
                }

                "scenes" -> {
                    ScenesScreen(
                        listaEscenas = scenesViewModel.listaEscenas,
                        dinoEncendido = dinoViewModel.dinoEncendido,
                        viewModel = scenesViewModel,
                        onAplicarEscena = { escena ->
                            scenesViewModel.aplicarEscena(escena)
                        },
                        onBorrarEscena = { id ->
                            scenesViewModel.borrarEscena(id)
                        },
                        onGuardarNuevaEscena = { nuevaEscena ->
                            scenesViewModel.guardarNuevaEscena(
                                nuevaEscena,
                                esEdicion = false
                            )
                        },
                        onActualizarEscena = { escenaEditada ->
                            scenesViewModel.guardarNuevaEscena(
                                escenaEditada,
                                esEdicion = true
                            )
                        },
                        onSendColorRGB = { r, g, b ->
                            colorsViewModel.sendColorFinal(r, g, b)
                        },
                        onReactivarEscena = {
                            if (!dinoViewModel.dinoEncendido) {
                                scenesViewModel.reactivarUltimaEscena()
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

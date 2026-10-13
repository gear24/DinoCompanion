package com.example.dinocompanionapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.dinocompanionapp.ui.screens.ColorsScreen
import com.example.dinocompanionapp.ui.screens.ModesScreen
import com.example.dinocompanionapp.ui.screens.ScenesScreen
import com.example.dinocompanionapp.viewmodel.DinoColorsViewModel
import com.example.dinocompanionapp.viewmodel.DinoModesViewModel
import com.example.dinocompanionapp.viewmodel.DinoScenesViewModel
import com.example.dinocompanionapp.viewmodel.DinoViewModel

@Composable
fun AppNavigation(
    dinoViewModel: DinoViewModel,
    colorsViewModel: DinoColorsViewModel,
    modesViewModel: DinoModesViewModel,
    scenesViewModel: DinoScenesViewModel,
    pantalla: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
 )
 {
    when (pantalla) {
        "home" -> {
            HomeScreen(
                viewModel = dinoViewModel,
                bluetoothManager = dinoViewModel.bluetoothManager,
                onNavigateToColors = { onNavigate("colors") },
                onNavigateToModes = { onNavigate("modes") },
                onNavigateToScenes = { onNavigate("scenes") },
                modifier = modifier
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
                onBackToHome = { onNavigate("home") },
                modifier = modifier
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
                onBackToHome = { onNavigate("home") },
                modifier = modifier
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
                onBackToHome = { onNavigate("home") },
                modifier = modifier
            )
        }
    }
}
package com.example.dinocompanionapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.example.dinocompanionapp.data.Escena
import com.example.dinocompanionapp.ui.components.DinoButton
import com.example.dinocompanionapp.ui.components.DinoCard
import com.example.dinocompanionapp.ui.components.SceneForm
import com.example.dinocompanionapp.ui.theme.Dark
import com.example.dinocompanionapp.ui.theme.Pink
import com.example.dinocompanionapp.ui.theme.SoftPink
import com.example.dinocompanionapp.viewmodel.DinoViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScenesScreen(
    listaEscenas: List<Escena>,
    viewModel: DinoViewModel,
    onAplicarEscena: (Escena) -> Unit,
    onBorrarEscena: (Long) -> Unit,
    onGuardarNuevaEscena: (Escena) -> Unit,
    onActualizarEscena: (Escena) -> Unit,
    onSendColorRGB: (Int, Int, Int) -> Unit,
    onBackToHome: () -> Unit,
    onReactivarEscena: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 🟢 Si entra a escenas y está apagado, aplica la última escena
// 🟢 Solo reactivar si la lámpara estaba en estado apagado
    LaunchedEffect(Unit) {
        if (!viewModel.dinoEncendido) {
            onReactivarEscena()
        }
    }

    // Estados para controlar el Overlay del Formulario
    var mostrandoFormulario by remember { mutableStateOf(false) }
    var escenaSeleccionadaParaEditar by remember { mutableStateOf<Escena?>(null) }

    // Almacena el ID de la tarjeta que tiene los botones de acción activos
    var idEscenaAccionesActivas by remember { mutableStateOf<Long?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        // --- CONTENIDO PRINCIPAL DE LA PANTALLA ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Pink)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DinoButton("⬅️ Home") { onBackToHome() }
                DinoButton("➕ Nueva") {
                    escenaSeleccionadaParaEditar = null
                    mostrandoFormulario = true
                }
            }

            Spacer(Modifier.height(16.dp))
            DinoCard("Mis Escenas")
            Spacer(Modifier.height(16.dp))

            if (listaEscenas.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay escenas creadas. ¡Toca '➕ Nueva' para empezar!")
                }
            } else {
                // Diseño Bento Grid de 2 columnas
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listaEscenas, key = { it.id }) { escena ->
                        val mostrarAcciones = idEscenaAccionesActivas == escena.id

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SoftPink)
                                .combinedClickable(
                                    onClick = {
                                        if (mostrarAcciones) {
                                            idEscenaAccionesActivas = null
                                        } else {
                                            // 🟢 CORREGIDO: Registra la escena en el ViewModel como el modo activo actual
                                            viewModel.previewEscenaEnVivo(escena)
                                            onAplicarEscena(escena)
                                        }
                                    },
                                    onLongClick = {
                                        idEscenaAccionesActivas = escena.id
                                    }
                                )
                                .padding(12.dp)
                        ) {
                            // Info normal de la tarjeta
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(escena.nombre)
                                Text("💡 ${escena.brillo}%")
                                Spacer(Modifier.weight(1f))

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    escena.colores.forEach { colorArgb ->
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(colorArgb))
                                        )
                                    }
                                }
                            }

                            if (mostrarAcciones) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Dark.copy(alpha = 0.9f))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DinoButton("✏️") {
                                        escenaSeleccionadaParaEditar = escena
                                        mostrandoFormulario = true
                                        idEscenaAccionesActivas = null
                                    }
                                    DinoButton("🗑️") {
                                        onBorrarEscena(escena.id)
                                        idEscenaAccionesActivas = null
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- OVERLAY OSCURO GENERAL + FORMULARIO INFERIOR ---
        if (mostrandoFormulario) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = true, onClick = { /* Bloquea clics traseros */ })
            )

            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                SceneForm(
                    escenaAEditar = escenaSeleccionadaParaEditar,
                    viewModel = viewModel,
                    onSendColor = { r, g, b ->
                        onSendColorRGB(r, g, b)
                    },
                    onCancelar = { mostrandoFormulario = false },
                    onGuardar = { escenaConstruida ->
                        if (escenaSeleccionadaParaEditar == null) {
                            onGuardarNuevaEscena(escenaConstruida)
                        } else {
                            onActualizarEscena(escenaConstruida)
                        }
                        mostrandoFormulario = false
                    }
                )
            }
        }
    }
}
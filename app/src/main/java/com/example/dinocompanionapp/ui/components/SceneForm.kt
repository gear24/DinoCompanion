package com.example.dinocompanionapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

import com.example.dinocompanionapp.data.*
import com.example.dinocompanionapp.ui.theme.Cream
import com.example.dinocompanionapp.ui.theme.Dark
import com.example.dinocompanionapp.viewmodel.DinoViewModel




@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SceneForm(
    escenaAEditar: Escena? = null,
    viewModel: DinoViewModel,
    onSendColor: (Int, Int, Int) -> Unit,
    onGuardar: (Escena) -> Unit,
    onCancelar: () -> Unit
) {
    // Iniciar la escena previa en vivo al montar el formulario
    LaunchedEffect(Unit) {
        viewModel.iniciarLiveScene(escenaAEditar)
    }

    var nombre by remember(escenaAEditar) {
        mutableStateOf(escenaAEditar?.nombre ?: "")
    }

    var brilloEscena by remember(escenaAEditar) {
        mutableStateOf(escenaAEditar?.brillo?.toFloat() ?: 50f)
    }

    var velocidadEscena by remember(escenaAEditar) {
        mutableStateOf(escenaAEditar?.velocidad?.toFloat() ?: 50f)
    }

    var efecto by remember(escenaAEditar) {
        mutableStateOf(escenaAEditar?.efecto ?: EfectoEscena.ESTATICO)
    }

    val coloresEscena = remember(escenaAEditar) {
        if (escenaAEditar != null) {
            val listaExistente = escenaAEditar.colores
            val lista = List(5) { index ->
                if (index < listaExistente.size) listaExistente[index] else Color.Red.toArgb()
            }
            mutableStateListOf(*lista.toTypedArray())
        } else {
            mutableStateListOf(
                Color.Red.toArgb(),     // 0
                Color.Green.toArgb(),   // 1
                Color.Blue.toArgb(),    // 2
                Color.Yellow.toArgb(),  // 3
                Color.Magenta.toArgb()  // 4
            )
        }
    }

    var colorSeleccionado by remember { mutableIntStateOf(0) }

    val cantidadColores = when (efecto) {
        EfectoEscena.ESTATICO -> 1
        EfectoEscena.RESPIRAR -> 1
        EfectoEscena.PARPADEO -> 2
        EfectoEscena.MEZCLA -> 5
    }

    var ultimoEnvioLive by remember { mutableLongStateOf(0L) }

    // 🚀 Notifica los cambios enviando la escena animada con un filtro de 120ms para fluidez
    fun notificarCambiosLive(forzar: Boolean = false) {
        val ahora = System.currentTimeMillis()
        if (forzar || (ahora - ultimoEnvioLive > 120L)) {
            ultimoEnvioLive = ahora
            val escenaTemp = Escena(
                id = escenaAEditar?.id ?: System.currentTimeMillis(),
                nombre = nombre,
                efecto = efecto,
                colores = coloresEscena.take(cantidadColores),
                brillo = brilloEscena.toInt(),
                velocidad = velocidadEscena.toInt()
            )
            viewModel.previewEscenaEnVivo(escenaTemp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Dark)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (escenaAEditar == null) "✨ Nueva Escena" else "✏️ Editar Escena",
            color = Cream
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre de la escena") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        Text("Efecto", color = Cream)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EfectoEscena.entries.forEach { item ->
                Box(modifier = Modifier.weight(1f)) {
                    DinoButton(item.name) {
                        efecto = item
                        colorSeleccionado = 0
                        notificarCambiosLive(forzar = true)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Colores", color = Cream)
        Spacer(Modifier.height(8.dp))

        // CAJITAS DE COLORES
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            coloresEscena.take(cantidadColores).forEachIndexed { index, color ->
                val esElSeleccionado = (index == colorSeleccionado)
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (color == Color.Transparent.toArgb())
                                Color.Gray.copy(alpha = 0.3f)
                            else
                                Color(color)
                        )
                        .border(
                            width = if (esElSeleccionado) 3.dp else 0.dp,
                            color = if (esElSeleccionado) Cream else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .combinedClickable(onClick = { colorSeleccionado = index })
                )
            }
        }

        Spacer(Modifier.height(16.dp))

// 🟢 CORREGIDO: Refresca la escena en vivo sin romper los efectos/mezclas con colores planos
        ColorPicker(
            currentColor = Color(coloresEscena.getOrElse(colorSeleccionado) { Color.Red.toArgb() }),
            onColorChanged = { nuevoColor ->
                if (colorSeleccionado in coloresEscena.indices) {
                    coloresEscena[colorSeleccionado] = nuevoColor.toArgb()
                    notificarCambiosLive(forzar = true)
                }
            },
            onColorStream = { colorStream ->
                if (colorSeleccionado in coloresEscena.indices) {
                    coloresEscena[colorSeleccionado] = colorStream.toArgb()
                    notificarCambiosLive()
                }
            },
            sendColorFinal = { _, _, _ ->
                notificarCambiosLive(forzar = true)
            }
        )

        Spacer(Modifier.height(16.dp))

        // --- SLIDER DE BRILLO ---
        Text("Brillo: ${brilloEscena.toInt()}%", color = Cream)

        DinoSlider(
            label = "Brillo",
            value = brilloEscena,
            onValueChange = {
                brilloEscena = it
                notificarCambiosLive()
            },
            valueRange = 0f..100f,
            activeTrackColor = Color(0xFFFFC107),  // 🌟 Ámbar/Dorado cálido para el brillo
            inactiveTrackColor = Dark.copy(alpha = 0.2f), // Pista inactiva sutil
            thumbColor = Cream,                    // Botón en tono crema
            labelColor = Cream,
            valueFormatter = { "${it.toInt()}%" },
            warningMessage = if (efecto == EfectoEscena.RESPIRAR && brilloEscena < 20f) {
                "En modo Respirar, un brillo menor al 20% puede apagar temporalmente los LEDs en el punto más bajo."
            } else null
        )

        // --- SLIDER DE VELOCIDAD DINO ---
        if (efecto != EfectoEscena.ESTATICO) {
            Spacer(Modifier.height(16.dp))
            val etiquetaVelocidad = when (efecto) {
                EfectoEscena.MEZCLA -> "Suavidad de mezcla"
                EfectoEscena.RESPIRAR -> "Frecuencia de respiración"
                EfectoEscena.PARPADEO -> "Velocidad de destello"
                EfectoEscena.ESTATICO -> "Velocidad"
            }

            Text("$etiquetaVelocidad: ${velocidadEscena.toInt()}%", color = Cream)
            DinoSlider(
                label = "",
                value = velocidadEscena,
                onValueChange = {
                    velocidadEscena = it
                    notificarCambiosLive()
                },
                valueRange = 1f..100f,
                activeTrackColor = Color(0xFF4DB6AC),  // 🌊 Verde turquesa / menta suave
                inactiveTrackColor = Dark.copy(alpha = 0.2f),
                thumbColor = Cream,
                labelColor = Cream,
                valueFormatter = { "${it.toInt()}%" }
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                DinoButton("Cancelar") {
                    viewModel.cancelarEdicionEscena()
                    onCancelar()
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                DinoButton("💾 Guardar") {
                    if (nombre.isNotBlank()) {
                        val escenaFinal = Escena(
                            id = escenaAEditar?.id ?: System.currentTimeMillis(),
                            nombre = nombre,
                            efecto = efecto,
                            colores = coloresEscena.take(cantidadColores),
                            brillo = brilloEscena.toInt(),
                            velocidad = velocidadEscena.toInt()
                        )
                        onGuardar(escenaFinal)
                    }
                }
            }
        }
    }
}
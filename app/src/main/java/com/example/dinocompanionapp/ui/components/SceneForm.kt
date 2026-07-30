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

    fun obtenerColoresPorDefecto(tipoEfecto: EfectoEscena): List<Int> {
        return when (tipoEfecto) {
            EfectoEscena.ESTATICO -> listOf(Color.Red.toArgb())
            EfectoEscena.RESPIRAR -> listOf(Color.Cyan.toArgb())
            EfectoEscena.PARPADEO -> listOf(Color.Red.toArgb(), Color.Blue.toArgb())
            EfectoEscena.MEZCLA -> listOf(
                Color.Red.toArgb(),
                Color.Green.toArgb(),
                Color.Blue.toArgb(),
                Color.Yellow.toArgb(),
                Color.Magenta.toArgb()
            )
        }
    }

// 🟢 FIX: Si es nueva, cargar la paleta inicial del efecto por defecto (ESTATICO = 1 color)
    val coloresEscena = remember(escenaAEditar) {
        if (escenaAEditar != null) {
            mutableStateListOf(*escenaAEditar.colores.toTypedArray())
        } else {
            // Cargar los colores del efecto inicial real (ESTATICO), no de MEZCLA
            mutableStateListOf(*obtenerColoresPorDefecto(efecto).toTypedArray())
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

    // 🚀 Notifica los cambios enviando la escena animada
    fun notificarCambiosLive(forzar: Boolean = false) {
        val ahora = System.currentTimeMillis()
        if (forzar || (ahora - ultimoEnvioLive > 120L)) {
            ultimoEnvioLive = ahora
            val escenaTemp = Escena(
                id = escenaAEditar?.id ?: System.currentTimeMillis(),
                nombre = nombre,
                efecto = efecto,
                // 🟢 Usamos la lista directa porque ya está limpia y garantizada
                colores = coloresEscena.toList(),
                brillo = brilloEscena.toInt(),
                velocidad = velocidadEscena.toInt()
            )
            viewModel.previewEscenaEnVivo(escenaTemp)
        }
    }

    // 🟢 1. Iniciar escena previa Y FORZAR el envío inmediato al montar la pantalla
    LaunchedEffect(Unit) {
        viewModel.iniciarLiveScene(escenaAEditar)
        // Forzar actualización inmediata para que el ESP32 reaccione de inmediato al entrar
        notificarCambiosLive(forzar = true)
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
                        if (efecto != item) {
                            efecto = item
                            colorSeleccionado = 0 // Resetear siempre al primer color

                            // 🟢 FIX: Obtener la paleta limpia para el nuevo efecto
                            val coloresNuevos = obtenerColoresPorDefecto(item)

                            // Reemplazar el contenido completo de la lista para no dejar basura de otros modos
                            coloresEscena.clear()
                            coloresEscena.addAll(coloresNuevos)

                            // Notificar inmediatamente la nueva estructura limpia al ESP32
                            notificarCambiosLive(forzar = true)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Colores", color = Cream)
        Spacer(Modifier.height(8.dp))

        // CAJITAS DE COLORES
        // CAJITAS DE COLORES
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // 🟢 Iterar directamente sobre la lista (ya no requiere .take(cantidadColores))
            coloresEscena.forEachIndexed { index, color ->
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

        ColorPicker(
            currentColor = Color(coloresEscena.getOrElse(colorSeleccionado) { Color.Red.toArgb() }),
            onColorChanged = { nuevoColor ->
                if (colorSeleccionado in coloresEscena.indices) {
                    coloresEscena[colorSeleccionado] = nuevoColor.toArgb()
                }
            },
            onColorStream = { colorStream ->
                if (colorSeleccionado in coloresEscena.indices) {
                    coloresEscena[colorSeleccionado] = colorStream.toArgb()
                    notificarCambiosLive(forzar = false)
                }
            },
            sendColorFinal = { r, g, b ->
                if (colorSeleccionado in coloresEscena.indices) {
                    // Actualizar el valor final exacto producido por el picker
                    coloresEscena[colorSeleccionado] = android.graphics.Color.rgb(r, g, b)
                    notificarCambiosLive(forzar = true)
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        // --- SLIDER DE BRILLO ---
        Text("Brillo: ${brilloEscena.toInt()}%", color = Cream)

        DinoSlider(
            label = "Brillo",
            value = brilloEscena,
            onValueChange = { nuevoBrillo ->
                brilloEscena = nuevoBrillo
                viewModel.previewBrilloEscena(nuevoBrillo.toInt())
                notificarCambiosLive()
            },
            valueRange = 0f..100f,
            activeTrackColor = Color(0xFFFFC107),
            inactiveTrackColor = Dark.copy(alpha = 0.2f),
            thumbColor = Cream,
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
                activeTrackColor = Color(0xFF4DB6AC),
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
package com.example.dinocompanionapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.dinocompanionapp.ui.components.ColorPicker
import com.example.dinocompanionapp.ui.components.DinoButton
import com.example.dinocompanionapp.ui.components.DinoCard
import com.example.dinocompanionapp.ui.components.DinoSlider
import com.example.dinocompanionapp.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColorsScreen(
    currentColor: Color,
    brillo: Float,
    favoritos: List<Color?>,
    onColorChangedInPicker: (Color) -> Unit,
    onColorStream: (Color) -> Unit, // 👈 1. AGREGADO AQUÍ
    onBrilloChanged: (Float) -> Unit,
    onFavoritoClick: (Color) -> Unit,
    onFavoritoLongClick: (Int, Color) -> Unit,
    onSendColor: (Int, Int, Int) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
    onReactivarColor: () -> Unit
) {
    LaunchedEffect(Unit) {
        onReactivarColor()
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Pink)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DinoButton("⬅️ Volver al Home") {
            onBackToHome()
        }

        Spacer(Modifier.height(16.dp))

        DinoCard("Colores Favoritos")

        Spacer(Modifier.height(20.dp))

        Text("Brillo: ${brillo.toInt()}")

        DinoSlider(
            label = "Brillo General",
            value = brillo,
            onValueChange = onBrilloChanged,
            valueRange = 0f..100f,
            activeTrackColor = Color(0xFFFFD54F),        // ☀️ Ámbar cálido para representar intensidad de luz
            inactiveTrackColor = SoftPink.copy(alpha = 0.4f), // Tu fondo rosa suave integrado
            thumbColor = Cream,                          // Botón crema característico de tu UI
            labelColor = Cream,
            valueFormatter = { "${it.toInt()}%" }
        )

        Spacer(Modifier.height(12.dp))

        // Caja de previsualización del color actual
        /*
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(currentColor)
        )

        Spacer(Modifier.height(20.dp))
*/
        Text("Favoritos")
        Text("Selecciona un color del espectro y luego mantén presionado un cuadro de color por unos segundos. ¡Disfruta tu nuevo color!")

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            favoritos.forEachIndexed { index, favorito ->
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(favorito ?: Color.Gray.copy(alpha = 0.3f))
                        .combinedClickable(
                            onClick = {
                                favorito?.let { onFavoritoClick(it) }
                            },
                            onLongClick = {
                                onFavoritoLongClick(index, currentColor)
                            }
                        )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 👈 2. SE PASA AL COLORPICKER
        ColorPicker(
            currentColor = currentColor,
            onColorChanged = onColorChangedInPicker,
            onColorStream = onColorStream,
            sendColorFinal = onSendColor
        )

        Spacer(Modifier.height(12.dp))

        // --- PRESETS DE COLOR ---
        DinoButton("Wine") {
            onSendColor(255, 33, 19)
            onColorChangedInPicker(Color(255, 33, 19))
        }

        DinoButton("Champagne") {
            onSendColor(252, 228, 216)
            onColorChangedInPicker(Color(252, 228, 216))
        }

        DinoButton("French Rose") {
            onSendColor(247, 85, 144)
            onColorChangedInPicker(Color(247, 85, 144))
        }

        DinoButton("Crimson Silk") {
            onSendColor(215, 38, 56)
            onColorChangedInPicker(Color(215, 38, 56))
        }

        DinoButton("Deep Bordeaux") {
            onSendColor(189, 45, 54)
            onColorChangedInPicker(Color(189, 45, 54))
        }

        DinoButton("Deep Purple") {
            onSendColor(148, 36, 184)
            onColorChangedInPicker(Color(148, 36, 184))
        }
    }
}
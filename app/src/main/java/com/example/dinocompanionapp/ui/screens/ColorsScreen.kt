package com.example.dinocompanionapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dinocompanionapp.ui.components.ColorPicker
import com.example.dinocompanionapp.ui.components.DinoButton
import com.example.dinocompanionapp.ui.components.DinoCard
import com.example.dinocompanionapp.ui.components.DinoSlider
import com.example.dinocompanionapp.ui.theme.*
import com.example.dinocompanionapp.viewmodel.DinoViewModel





@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColorsScreen(
    currentColor: Color,
    brilloColor: Float,
    favoritos: List<DinoViewModel.Favorito>,
    onColorChangedInPicker: (Color) -> Unit,
    onColorStream: (Color) -> Unit,
    onBrilloChanged: (Float) -> Unit,
    onFavoritoClick: (DinoViewModel.Favorito) -> Unit,
    onFavoritoLongClick: (Int, Color, Float) -> Unit,
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

        Text("Brillo: ${brilloColor.toInt()}")

        DinoSlider(
            label = "Brillo",
            value = brilloColor,
            onValueChange = onBrilloChanged,
            valueRange = 0f..100f,
            activeTrackColor = Color(0xFFFFD54F),
            inactiveTrackColor = SoftPink.copy(alpha = 0.4f),
            thumbColor = Cream,
            labelColor = Cream,
            valueFormatter = { "${it.toInt()}%" }
        )

        Spacer(Modifier.height(12.dp))

        Text("Favoritos")
        Text(
            "Selecciona un color del espectro y luego mantén presionado " +
                    "un cuadro de color por unos segundos. ¡Disfruta tu nuevo color!"
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = "💡 Sugerencia: Si actualizas el brillo de un color guardado, vuelve a mantener presionado su respectivo cuadro para guardar el nuevo brillo.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            favoritos.forEachIndexed { index, favorito ->

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            favorito.color
                                ?: Color.Gray.copy(alpha = 0.3f)
                        )
                        .combinedClickable(
                            onClick = {
                                if (favorito.color != null) {
                                    onFavoritoClick(favorito)
                                }
                            },
                            onLongClick = {
                                onFavoritoLongClick(
                                    index,
                                    currentColor,
                                    brilloColor
                                )
                            }
                        )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        ColorPicker(
            currentColor = currentColor,
            onColorChanged = onColorChangedInPicker,
            onColorStream = onColorStream,
            sendColorFinal = onSendColor
        )

        Spacer(Modifier.height(12.dp))

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


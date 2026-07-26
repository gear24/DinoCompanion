package com.example.dinocompanionapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dinocompanionapp.ui.theme.*


@Composable
fun DinoSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    // 🎨 Nuevos parámetros opcionales
    valueFormatter: (Float) -> String = { "${it.toInt()}" }, // Formato por defecto (solo el número)
    warningMessage: String? = null,
    onValueChangeFinished: (() -> Unit)? = null, // Para cuando el usuario suelta el slider
    thumbColor: Color = Dark,
    activeTrackColor: Color = Cream,
    inactiveTrackColor: Color = SoftPink,
    labelColor: Color = Cream
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "$label: ${valueFormatter(value)}", // 👈 Usa el formateador personalizado
            style = MaterialTheme.typography.titleMedium,
            color = labelColor
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished, // 👈 Callback opcional al soltar
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = thumbColor,
                activeTrackColor = activeTrackColor,
                inactiveTrackColor = inactiveTrackColor
            )
        )

        if (!warningMessage.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "💡 $warningMessage",
                color = Color(0xFFFFB74D), // Naranja de advertencia
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
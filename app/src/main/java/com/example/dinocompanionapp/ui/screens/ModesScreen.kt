package com.example.dinocompanionapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dinocompanionapp.ui.components.DinoButton
import com.example.dinocompanionapp.ui.components.DinoCard
import com.example.dinocompanionapp.ui.theme.*
import androidx.compose.ui.graphics.Color
import com.example.dinocompanionapp.ui.components.DinoSlider

@Composable
fun ModesScreen(
    animState: Boolean,       // Indica si hay alguna animación corriendo
    brightness: Float,             // Velocidad actual del slider (ej. 1f..100f)
    onBrightnessChanged: (Float) -> Unit,
    onStartLava: () -> Unit,
    onStartArcoiris: () -> Unit,
    onStartRespirar: () -> Unit,
    onStartForest: ()-> Unit,
    onStartParty: ()-> Unit,
    onStartOcean: ()-> Unit,
    onStopAnimation: () -> Unit,
    onBackToHome: () -> Unit,
    onReactivarModo: () -> Unit, // 👈 Nuevo callback
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onReactivarModo()
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

        DinoCard("Efectos Animados")

        Spacer(Modifier.height(20.dp))

        Text("Velocidad de Efecto: ${brightness.toInt()}")


        DinoSlider(
            label = "Brillo",
            value = brightness,
            onValueChange = onBrightnessChanged,
            valueRange = 1f..100f,
            activeTrackColor = Color(0xFFFFD54F),        // ☀️ Ámbar cálido (símbolo de luz encendida)
            inactiveTrackColor = SoftPink.copy(alpha = 0.4f), // Pista inactiva suave con la paleta de tu app
            thumbColor = Cream,                          // BotónCrema para mantener tu identidad visual
            labelColor = Cream,
            valueFormatter = { "${it.toInt()}%" }
        )


        Spacer(Modifier.height(24.dp))

        // Botón de encendido/apagado general del efecto
        if (animState) {
            DinoButton("🛑 Apagar Efecto") {
                onStopAnimation()
            }
        } else {
            Text("No hay ningún efecto activo en este momento.")
        }

        Spacer(Modifier.height(24.dp))

        Text("Selecciona una animación:")
        Spacer(Modifier.height(12.dp))

        DinoButton("🌋 Efecto Lava") {
            onStartLava()
        }

        Spacer(Modifier.height(12.dp))

        DinoButton("🌈 Arcoíris") {
            onStartArcoiris()
        }

        Spacer(Modifier.height(12.dp))

        DinoButton("💨 Efecto Respirar") {
            onStartRespirar()
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "💡 Sugerencia: En el modo Respirar, asegúrate de tener el brillo en la app mayor al 20% para evitar que los LEDs se apaguen por completo en el ciclo bajo.",
            color = Color(0xFFFFB74D), // Naranja suave
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(12.dp))

        DinoButton(" Efecto Oceano") {
            onStartOcean()
        }

        Spacer(Modifier.height(12.dp))

        DinoButton(" Efecto Bosque") {
            onStartForest()
        }

        Spacer(Modifier.height(12.dp))

        DinoButton(" Efecto Fiesta") {
            onStartParty()
        }

    }
}
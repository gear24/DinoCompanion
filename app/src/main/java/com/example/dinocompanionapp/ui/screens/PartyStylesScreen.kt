package com.example.dinocompanionapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dinocompanionapp.ui.components.DinoButton
import com.example.dinocompanionapp.ui.components.DinoCard
import com.example.dinocompanionapp.ui.theme.Dark
import com.example.dinocompanionapp.ui.theme.Pink

@Composable
fun PartyStylesScreen(
    fiestaStyle: Int,
    autoFiesta: Boolean,
    panzaOn: Boolean,
    onFiestaStyleChange: (Int) -> Unit,
    onAutoFiestaChange: (Boolean) -> Unit,
    onPanzaChange: (Boolean) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Pink)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "🎉 Estilos de Fiesta",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        // --- Estilo de fiesta ---
        DinoCard("Estilo de música") {
            Spacer(Modifier.height(8.dp))
            DinoButton(
                texto = "Vúmetro",
                isSelected = fiestaStyle == 0
            ) { onFiestaStyleChange(0) }

            Spacer(Modifier.height(6.dp))
            DinoButton(
                texto = "Beat Flash",
                isSelected = fiestaStyle == 1
            ) { onFiestaStyleChange(1) }

            Spacer(Modifier.height(6.dp))
            DinoButton(
                texto = "Olas",
                isSelected = fiestaStyle == 2
            ) { onFiestaStyleChange(2) }
        }

        Spacer(Modifier.height(16.dp))

        // --- Auto-fiesta ---
        DinoCard("Auto-fiesta") {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Activar automáticamente con audio",
                    color = Dark,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = autoFiesta,
                    onCheckedChange = onAutoFiestaChange
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- Panza ---
        DinoCard("Panza") {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Modo panza",
                    color = Dark,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = panzaOn,
                    onCheckedChange = onPanzaChange
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        DinoButton("⬅️ Volver") { onBackToHome() }
    }
}
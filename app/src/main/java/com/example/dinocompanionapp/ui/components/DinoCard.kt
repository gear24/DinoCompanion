package com.example.dinocompanionapp.ui.components


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// Importa tus colores personalizados desde el paquete de tu tema
import com.example.dinocompanionapp.ui.theme.Beige
import com.example.dinocompanionapp.ui.theme.Dark



@Composable
fun DinoCard(
    text: String? = null,
    content: (@Composable () -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Beige
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            if (text != null) {
                Text(
                    text = text,
                    color = Dark
                )
            }

            content?.invoke()
        }
    }
}
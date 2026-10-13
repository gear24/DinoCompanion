package com.example.dinocompanionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.dinocompanionapp.viewmodel.DinoViewModel

class MainActivity : ComponentActivity() {

    private val dinoViewModel: DinoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DinoCompanionApp(
                application = application,
                dinoViewModel = dinoViewModel
            )
        }
    }
}

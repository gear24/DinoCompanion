package com.example.dinocompanionapp.repository

import android.content.Context
import androidx.core.content.edit

class DinoRepository(context: Context) {

    private val prefs = context.getSharedPreferences(
        "dino_settings",
        Context.MODE_PRIVATE
    )

    // --- MODO ---

    fun getUltimoModoId(): Int =
        prefs.getInt("ultimo_modo_id", 3)

    fun saveUltimoModoId(idModo: Int) {
        prefs.edit {
            putInt("ultimo_modo_id", idModo)
        }
    }

    fun getModoActual(): Int =
        prefs.getInt("modo_actual", 0)

    fun saveModoActual(idModo: Int) {
        prefs.edit {
            putInt("modo_actual", idModo)
        }
    }

    // --- ESTADO DEL DINO ---

    fun isDinoEncendido(): Boolean =
        prefs.getBoolean("dino_encendido", false)

    fun saveDinoEncendido(encendido: Boolean) {
        prefs.edit {
            putBoolean("dino_encendido", encendido)
        }
    }

    // --- ESCENA ---

    fun getUltimaEscenaId(): Long =
        prefs.getLong("ultima_escena_id", -1L)

    fun saveUltimaEscenaId(id: Long) {
        prefs.edit {
            putLong("ultima_escena_id", id)
        }
    }

    // --- NOMBRE ---

    fun getDinoName(): String =
        prefs.getString("dino_name", "Dino") ?: "Dino"

    fun saveDinoName(name: String) {
        prefs.edit {
            putString("dino_name", name)
        }
    }

    // --- COLOR ---

    fun getCurrentColor(defaultColor: Int): Int =
        prefs.getInt("current_color", defaultColor)

    fun saveCurrentColor(color: Int) {
        prefs.edit {
            putInt("current_color", color)
        }
    }


    // --- BRILLO DEL COLOR ---

    fun getBrilloColor(): Float =
        prefs.getFloat("brillo_color", 80f)

    fun saveBrilloColor(brillo: Float) {
        prefs.edit {
            putFloat("brillo_color", brillo)
        }
    }

    // --- BRILLO POR MODO ---

    fun getBrilloModo(idModo: Int): Float =
        prefs.getFloat("brillo_modo_$idModo", 80f)

    fun saveBrilloModo(idModo: Int, brillo: Float) {
        prefs.edit {
            putFloat("brillo_modo_$idModo", brillo)
        }
    }

    // --- FAVORITOS ---

    fun hasFavorito(index: Int): Boolean =
        prefs.contains("favorito_${index}_color")

    fun getFavoritoColor(index: Int, defaultColor: Int): Int =
        prefs.getInt(
            "favorito_${index}_color",
            defaultColor
        )

    fun getFavoritoBrillo(index: Int): Float =
        prefs.getFloat(
            "favorito_${index}_brillo",
            80f
        )

    fun saveFavorito(
        index: Int,
        color: Int,
        brillo: Float
    ) {
        prefs.edit {
            putInt("favorito_${index}_color", color)
            putFloat("favorito_${index}_brillo", brillo)
        }
    }
}
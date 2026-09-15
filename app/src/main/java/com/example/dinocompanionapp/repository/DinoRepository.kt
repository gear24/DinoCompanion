package com.example.dinocompanionapp.repository

import android.content.Context
import androidx.core.content.edit
import com.example.dinocompanionapp.data.Escena
import com.example.dinocompanionapp.data.SceneManager
import com.example.dinocompanionapp.data.EfectoEscena





class DinoRepository(context: Context) {

    private val prefs = context.getSharedPreferences(
        "dino_settings",
        Context.MODE_PRIVATE
    )
    private val scenePrefs = context.getSharedPreferences(
        "dino_scenes",
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

    // --- ESCENAS ---

    private fun guardarEscena(
        indice: Int,
        escena: Escena
    ) {
        scenePrefs.edit {
            putLong("escena_${indice}_id", escena.id)
            putString("escena_${indice}_nombre", escena.nombre)
            putString("escena_${indice}_efecto", escena.efecto.name)
            putInt("escena_${indice}_brillo", escena.brillo)
            putInt("escena_${indice}_velocidad", escena.velocidad)
            putInt("escena_${indice}_total_colores", escena.colores.size)

            escena.colores.forEachIndexed { index, color ->
                putInt("escena_${indice}_color_$index", color)
            }
        }
    }

    private fun cargarEscena(indice: Int): Escena? {
        val nombre = scenePrefs.getString(
            "escena_${indice}_nombre",
            null
        ) ?: return null

        val id = scenePrefs.getLong(
            "escena_${indice}_id",
            -1L
        )

        val efecto = EfectoEscena.valueOf(
            scenePrefs.getString(
                "escena_${indice}_efecto",
                EfectoEscena.ESTATICO.name
            )!!
        )

        val brillo = scenePrefs.getInt(
            "escena_${indice}_brillo",
            50
        )

        val velocidad = scenePrefs.getInt(
            "escena_${indice}_velocidad",
            50
        )

        val totalColores = scenePrefs.getInt(
            "escena_${indice}_total_colores",
            0
        )

        val colores = mutableListOf<Int>()

        repeat(totalColores) { index ->
            colores.add(
                scenePrefs.getInt(
                    "escena_${indice}_color_$index",
                    0
                )
            )
        }

        return Escena(
            id = id,
            nombre = nombre,
            efecto = efecto,
            colores = colores,
            brillo = brillo,
            velocidad = velocidad
        )
    }

    private fun obtenerCantidadEscenas(): Int =
        scenePrefs.getInt("cantidad_escenas", 0)

    private fun guardarCantidadEscenas(cantidad: Int) {
        scenePrefs.edit {
            putInt("cantidad_escenas", cantidad)
        }
    }

    fun cargarTodasLasEscenas(): List<Escena> {
        val escenas = mutableListOf<Escena>()
        val cantidad = obtenerCantidadEscenas()

        repeat(cantidad) { index ->
            cargarEscena(index)?.let {
                escenas.add(it)
            }
        }

        return escenas
    }

    fun agregarEscena(escena: Escena) {
        val indice = obtenerCantidadEscenas()

        guardarEscena(indice, escena)
        guardarCantidadEscenas(indice + 1)
    }

    fun actualizarEscena(escena: Escena) {
        val escenas = cargarTodasLasEscenas().toMutableList()
        val index = escenas.indexOfFirst { it.id == escena.id }

        if (index != -1) {
            escenas[index] = escena
        }

        guardarTodasLasEscenas(escenas)
    }

    private fun guardarTodasLasEscenas(
        escenas: List<Escena>
    ) {
        guardarCantidadEscenas(escenas.size)

        escenas.forEachIndexed { index, escena ->
            guardarEscena(index, escena)
        }
    }

    fun eliminarEscena(id: Long) {
        val escenas = cargarTodasLasEscenas()
            .filter { it.id != id }

        guardarTodasLasEscenas(escenas)
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
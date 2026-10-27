package com.example.therapp.data.routines.remote.payload.res

import com.google.gson.annotations.SerializedName

// Todos los campos son opcionales: Gson ignora la nulabilidad de Kotlin y el
// backend puede mandar null (o no mandar el campo). Los mapeadores ponen los
// valores por defecto.
data class DifficultyRes(
    val id: Int? = null,
    @SerializedName("exercise_id")
    val exerciseId: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val ranges: List<RangeRes>? = null
)

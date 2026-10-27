package com.example.therapp.data.routines.remote.payload.res

import com.google.gson.annotations.SerializedName

// Todos los campos son opcionales: Gson ignora la nulabilidad de Kotlin y el
// backend puede mandar null (o no mandar el campo). Los mapeadores ponen los
// valores por defecto.
data class TherapyRes(
    val id: Int? = null,
    val name: String? = null,
    val description: String? = null,
    @SerializedName("is_model")
    val isModel: Boolean? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null,
    val routines: List<String>? = null
)

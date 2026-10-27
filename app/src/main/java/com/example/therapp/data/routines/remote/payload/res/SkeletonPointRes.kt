package com.example.therapp.data.routines.remote.payload.res

import com.google.gson.annotations.SerializedName

// Todos los campos son opcionales: Gson ignora la nulabilidad de Kotlin y el
// backend puede mandar null (o no mandar el campo). Los mapeadores ponen los
// valores por defecto.
data class SkeletonPointRes(
    val id: Int? = null,
    val codename: String? = null,
    val verbose: String? = null,
    @SerializedName("left_point")
    val leftPoint: Int? = null,
    @SerializedName("right_point")
    val rightPoint: Int? = null
)

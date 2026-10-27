package com.example.therapp.data.routines.remote.payload.res

import com.google.gson.annotations.SerializedName

// Todos los campos son opcionales: Gson ignora la nulabilidad de Kotlin y el
// backend puede mandar null (o no mandar el campo). Los mapeadores ponen los
// valores por defecto.
data class RangeRes(
    @SerializedName("point_tracked_id")
    val pointTrackedId: Int? = null,
    @SerializedName("point_tracked")
    val pointTracked: PointTrackedRes? = null,
    @SerializedName("difficulty_id")
    val difficultyId: Int? = null,
    @SerializedName("max_angle")
    val maxAngle: Double? = null,
    @SerializedName("min_angle")
    val minAngle: Double? = null
)

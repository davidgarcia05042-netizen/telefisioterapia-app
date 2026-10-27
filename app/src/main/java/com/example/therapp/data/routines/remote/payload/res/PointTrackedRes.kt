package com.example.therapp.data.routines.remote.payload.res

import com.google.gson.annotations.SerializedName

// Todos los campos son opcionales: Gson ignora la nulabilidad de Kotlin y el
// backend puede mandar null (o no mandar el campo). Los mapeadores ponen los
// valores por defecto.
data class PointTrackedRes(
    @SerializedName("skeleton_point")
    val skeletonPoint: SkeletonPointRes? = null,
    @SerializedName("skeleton_point_id")
    val skeletonPointId: Int? = null,
    @SerializedName("exercise_id")
    val exerciseId: Int? = null,
    @SerializedName("max_angle")
    val maxAngle: Double? = null,
    @SerializedName("min_angle")
    val minAngle: Double? = null
)

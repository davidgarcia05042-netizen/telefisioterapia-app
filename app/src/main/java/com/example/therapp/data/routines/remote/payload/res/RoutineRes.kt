package com.example.therapp.data.routines.remote.payload.res

import com.google.gson.annotations.SerializedName

// Todos los campos son opcionales: Gson ignora la nulabilidad de Kotlin y el
// backend puede mandar null (o no mandar el campo). Los mapeadores ponen los
// valores por defecto.
// is_model, is_active y therapy_id llegaban sin @SerializedName (con el nombre
// en camelCase) y nunca se llenaban.
data class RoutineRes(
    val id: Int? = null,
    @SerializedName("therapy_id")
    val therapyId: Int? = null,
    val therapy: TherapyRes? = null,
    val name: String? = null,
    val description: String? = null,
    val exercises: List<ExercisesRes>? = null,
    @SerializedName("is_model")
    val isModeled: Boolean? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null
)

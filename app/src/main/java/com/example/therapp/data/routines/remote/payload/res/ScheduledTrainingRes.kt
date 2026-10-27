package com.example.therapp.data.routines.remote.payload.res

import com.google.gson.annotations.SerializedName

// Todos los campos son opcionales: Gson ignora la nulabilidad de Kotlin y el
// backend puede mandar null (o no mandar el campo). Los mapeadores ponen los
// valores por defecto.
data class ScheduledTrainingRes(
    val id: Int? = null,
    val routine: RoutineRes? = null,
    @SerializedName("start_time")
    val startTime: String? = null,
    val status: String? = null,
)

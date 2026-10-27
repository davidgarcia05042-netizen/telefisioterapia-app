package com.example.therapp.data.results.remote

import com.example.therapp.data.routines.remote.payload.res.PointTrackedRes
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Estado y resultados de la sesión (flujo del paciente en docs/DECISIONES_TECNICAS.md, pasos 9 y 10). */
interface ResultsApi {
    /** Resultados de la sesión; 404 si la sesión no es del paciente. */
    @GET("/exercise_results")
    suspend fun getResults(@Query("training_id") trainingId: Int): Response<List<ExerciseResultRes>?>

    /** Cierra la sesión y deja la rutina programada como "Finalizado". */
    @POST("/training/end")
    suspend fun endTraining(@Query("training_id") trainingId: Int): Response<Unit>
}

// Solo los campos que usa la app; Gson ignora el resto (la serie de ángulos
// por frame, el video, el concepto del terapeuta).
data class ExerciseResultRes(
    val id: Int? = null,
    val exercise: ResultExerciseRes? = null,
    val status: String? = null,
    val points: List<ResultPointRes>? = null,
)

data class ResultExerciseRes(
    val id: Int? = null,
)

data class ResultPointRes(
    @SerializedName("point_tracked")
    val pointTracked: PointTrackedRes? = null,
    @SerializedName("max_angle")
    val maxAngle: Double? = null,
    @SerializedName("min_angle")
    val minAngle: Double? = null,
)

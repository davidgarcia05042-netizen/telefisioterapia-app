package com.example.therapp.data.exercise.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Envío del video de un ejercicio (flujo del paciente en docs/DECISIONES_TECNICAS.md, pasos 5 a 8).
 * El PUT a Supabase no va por aquí: ver VideoUploader.
 */
interface SendApi {
    /** Crea la sesión de la rutina programada o devuelve la que ya existe (201 {id}). */
    @POST("/training")
    suspend fun startTraining(@Body body: StartTrainingReq): Response<IdRes>

    /** URL firmada para subir el video del paciente (kind=result). */
    @POST("/storage/upload_url")
    suspend fun uploadUrl(@Body body: UploadUrlReq): Response<UploadUrlRes>

    /** Registra el resultado; el backend lo manda al servicio (201 {id}). */
    @POST("/exercise_results")
    suspend fun createResult(@Body body: ExerciseResultReq): Response<IdRes>
}

data class StartTrainingReq(
    @SerializedName("schedule_training_id")
    val scheduleTrainingId: Int,
)

data class UploadUrlReq(
    val kind: String,
    @SerializedName("content_type")
    val contentType: String,
    // El backend solo usa la extensión; el nombre nunca llega a Supabase.
    val filename: String,
)

data class UploadUrlRes(
    @SerializedName("upload_url")
    val uploadUrl: String? = null,
    @SerializedName("public_url")
    val publicUrl: String? = null,
    val path: String? = null,
)

data class ExerciseResultReq(
    @SerializedName("training_id")
    val trainingId: Int,
    @SerializedName("exercise_id")
    val exerciseId: Int,
    val video: String,
)

data class IdRes(
    val id: Int? = null,
)

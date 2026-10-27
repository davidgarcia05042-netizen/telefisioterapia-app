package com.example.therapp.data.exercise

import com.example.therapp.common.api.ErrorMessages
import com.example.therapp.data.exercise.remote.ExerciseResultReq
import com.example.therapp.data.exercise.remote.SendApi
import com.example.therapp.data.exercise.remote.StartTrainingReq
import com.example.therapp.data.exercise.remote.UploadUrlReq
import kotlinx.coroutines.delay
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import javax.inject.Inject

/** Pasos del envío, para mostrar en pantalla. */
enum class SendStep(val label: String) {
    STARTING("Preparando el envío..."),
    UPLOADING("Subiendo el video..."),
    REGISTERING("Avisando a la plataforma..."),
}

/**
 * Un envío en curso. Guarda lo ya logrado para que "Reintentar" siga desde el
 * paso que falló: no se crea otra sesión ni se vuelve a subir un video que ya
 * subió.
 */
class SendJob(
    val scheduledTrainingId: Int,
    val exerciseId: Int,
    val video: VideoSource,
) {
    var trainingId: Int? = null
        internal set
    var publicUrl: String? = null
        internal set
}

sealed class SendOutcome {
    data class Sent(val resultId: Int) : SendOutcome()

    /** [retryable]: tiene sentido tocar "Reintentar" (red, servidor ocupado). */
    data class Failed(val message: String, val retryable: Boolean) : SendOutcome()
}

/**
 * Envío del video de un ejercicio (flujo del paciente en docs/DECISIONES_TECNICAS.md, pasos 5 a 8):
 * POST /training → POST /storage/upload_url → PUT a Supabase →
 * POST /exercise_results.
 *
 * Reintentos automáticos solo ante fallos de red:
 * - la subida pide una URL firmada nueva en cada intento, porque la de
 *   Supabase no permite sobrescribir: si un PUT llegó pero su respuesta se
 *   perdió, repetirlo con la misma URL fallaría;
 * - el aviso final (POST /exercise_results) solo se repite si la petición no
 *   llegó a salir (sin conexión). Si se cortó esperando la respuesta, pudo
 *   quedar registrado y repetirlo duplicaría el resultado.
 */
class ExerciseVideoSender @Inject constructor(
    private val api: SendApi,
    private val uploader: VideoUploader,
) {
    /** Espera antes de cada reintento automático (las pruebas usan 0). */
    var retryDelaysMillis: List<Long> = listOf(2_000L, 5_000L)

    suspend fun send(job: SendJob, onStep: (SendStep, Float) -> Unit): SendOutcome {
        val size = job.video.sizeBytes
        if (size <= 0) return SendOutcome.Failed("El video está vacío. Grábalo de nuevo.", retryable = false)
        if (size > MAX_VIDEO_BYTES) return SendOutcome.Failed(tooBig(size), retryable = false)

        return try {
            val trainingId = job.trainingId ?: run {
                onStep(SendStep.STARTING, 0f)
                val response = withRetry { api.startTraining(StartTrainingReq(job.scheduledTrainingId)) }
                val id = response.body()?.id
                if (!response.isSuccessful || id == null) return failure(Stage.START, response)
                job.trainingId = id
                id
            }

            val publicUrl = job.publicUrl ?: run {
                onStep(SendStep.UPLOADING, 0f)
                val uploaded = withRetry { upload(job.video) { onStep(SendStep.UPLOADING, it) } }
                when (uploaded) {
                    is Upload.Done -> uploaded.publicUrl.also { job.publicUrl = it }
                    is Upload.Rejected -> return uploaded.outcome
                }
            }

            onStep(SendStep.REGISTERING, 1f)
            val response = withRetry(onlyIfNotSent = true) {
                api.createResult(ExerciseResultReq(trainingId, job.exerciseId, publicUrl))
            }
            val id = response.body()?.id
            if (!response.isSuccessful || id == null) return failure(Stage.REGISTER, response)
            SendOutcome.Sent(id)
        } catch (e: IOException) {
            SendOutcome.Failed(NO_NETWORK, retryable = true)
        }
    }

    private sealed class Upload {
        data class Done(val publicUrl: String) : Upload()
        data class Rejected(val outcome: SendOutcome.Failed) : Upload()
    }

    /** URL firmada + PUT, como una unidad: cada intento usa una URL nueva. */
    private suspend fun upload(video: VideoSource, onProgress: (Float) -> Unit): Upload {
        val response = api.uploadUrl(UploadUrlReq(RESULT_KIND, video.contentType, video.fileName))
        val body = response.body()
        val uploadUrl = body?.uploadUrl
        val publicUrl = body?.publicUrl
        if (!response.isSuccessful || uploadUrl == null || publicUrl == null) {
            return Upload.Rejected(failure(Stage.SIGN, response))
        }
        val code = uploader.put(uploadUrl, video, onProgress)
        if (code !in 200..299) return Upload.Rejected(putFailure(code))
        return Upload.Done(publicUrl)
    }

    private suspend fun <T> withRetry(onlyIfNotSent: Boolean = false, block: suspend () -> T): T {
        for (wait in retryDelaysMillis) {
            try {
                return block()
            } catch (e: IOException) {
                if (onlyIfNotSent && !neverSent(e)) throw e
                delay(wait)
            }
        }
        return block()
    }

    /** La petición no salió del teléfono: repetirla no duplica nada. */
    private fun neverSent(e: IOException) = e is ConnectException || e is UnknownHostException

    private enum class Stage { START, SIGN, REGISTER }

    private fun failure(stage: Stage, response: Response<*>): SendOutcome.Failed {
        val code = response.code()
        val detail = ErrorMessages.fromBody(response.errorBody()?.string())
        return when {
            code == 401 -> SendOutcome.Failed(SESSION_EXPIRED, retryable = false)
            code == 404 && stage != Stage.SIGN ->
                SendOutcome.Failed("Esta rutina ya no está disponible. Actualiza tus rutinas.", retryable = false)
            // 503 = falta configurar Supabase o el servicio en el servidor.
            code == 503 && stage == Stage.SIGN -> SendOutcome.Failed(
                "El almacenamiento de videos no está disponible en el servidor. Avísale a tu terapeuta.",
                retryable = false
            )
            code == 503 && stage == Stage.REGISTER -> SendOutcome.Failed(
                "El servicio de medición no está disponible en el servidor. Avísale a tu terapeuta.",
                retryable = false
            )
            code == 502 && stage == Stage.SIGN -> SendOutcome.Failed(
                "El almacenamiento de videos no respondió. Inténtalo de nuevo.", retryable = true
            )
            code >= 500 -> SendOutcome.Failed(
                "El servidor tuvo un problema (código $code). Inténtalo de nuevo.", retryable = true
            )
            response.isSuccessful -> SendOutcome.Failed(ErrorMessages.UNEXPECTED, retryable = true)
            else -> SendOutcome.Failed(
                detail ?: "No se pudo enviar el video (código $code).", retryable = false
            )
        }
    }

    private fun putFailure(code: Int): SendOutcome.Failed = when (code) {
        413 -> SendOutcome.Failed("El almacenamiento rechazó el video por su tamaño (máximo 50 MB).", retryable = false)
        415 -> SendOutcome.Failed("El almacenamiento rechazó el formato del video.", retryable = false)
        else -> SendOutcome.Failed("No se pudo subir el video (código $code). Inténtalo de nuevo.", retryable = true)
    }

    companion object {
        /** Igual al tope del servicio, del bucket y del backend. */
        const val MAX_VIDEO_BYTES = 50L * 1024 * 1024
        private const val RESULT_KIND = "result"
        const val NO_NETWORK = "No hay conexión a internet. Revisa la red y toca Reintentar."
        const val SESSION_EXPIRED = "Tu sesión expiró. Inicia sesión de nuevo."

        fun tooBig(bytes: Long): String {
            val mb = String.format(java.util.Locale("es", "CO"), "%.1f", bytes / (1024.0 * 1024.0))
            return "El video pesa $mb MB y el máximo es 50 MB. Grábalo de nuevo, más corto."
        }
    }
}

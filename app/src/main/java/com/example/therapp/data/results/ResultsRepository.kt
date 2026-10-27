package com.example.therapp.data.results

import com.example.therapp.common.api.ApiResponse
import com.example.therapp.common.api.ErrorMessages
import com.example.therapp.data.exercise.remote.SendApi
import com.example.therapp.data.exercise.remote.StartTrainingReq
import com.example.therapp.data.results.remote.ExerciseResultRes
import com.example.therapp.data.results.remote.ResultsApi
import com.example.therapp.domain.model.ExerciseOutcome
import com.example.therapp.domain.model.MeasuredJoint
import com.example.therapp.domain.model.resultStatusOf
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/** Estado de los envíos de una rutina y su cierre. */
@Singleton
class ResultsRepository @Inject constructor(
    private val api: ResultsApi,
    private val sendApi: SendApi,
) {

    /**
     * Id de la sesión de una rutina ya iniciada. El backend no tiene otra
     * forma de pedírsela al paciente: POST /training devuelve la existente y,
     * con la rutina iniciada, no cambia nada. Por eso solo se llama si la
     * rutina está iniciada o finalizada (ver scheduledStatusHasSession).
     */
    suspend fun findTraining(scheduledTrainingId: Int): ApiResponse<Int> =
        call({ sendApi.startTraining(StartTrainingReq(scheduledTrainingId)) }) { it?.id }

    suspend fun getOutcomes(trainingId: Int): ApiResponse<List<ExerciseOutcome>> =
        call({ api.getResults(trainingId) }) { results -> results.orEmpty().mapNotNull { it.toOutcome() } }

    suspend fun endTraining(trainingId: Int): ApiResponse<Unit> =
        call({ api.endTraining(trainingId) }) { }

    private suspend fun <T, R> call(request: suspend () -> Response<T>, read: (T?) -> R?): ApiResponse<R> {
        return try {
            val response = request()
            val value = if (response.isSuccessful) read(response.body()) else null
            if (value != null) {
                ApiResponse.Success(value)
            } else {
                // El 404 del backend ("No encontrado.") no le dice nada al paciente.
                val message = if (response.code() == 404) NOT_FOUND
                else ErrorMessages.fromResponse(response.code(), response.errorBody()?.string())
                ApiResponse.Failure(message, response.code())
            }
        } catch (e: SocketTimeoutException) {
            ApiResponse.Failure(ErrorMessages.TIMEOUT, ErrorMessages.CODE_TIMEOUT)
        } catch (e: IOException) {
            ApiResponse.Failure(ErrorMessages.NO_CONNECTION, ErrorMessages.CODE_NO_CONNECTION)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResponse.Failure(ErrorMessages.UNEXPECTED, ErrorMessages.CODE_UNEXPECTED)
        }
    }

    companion object {
        const val NOT_FOUND = "Esta rutina ya no está disponible. Actualiza tus rutinas."
    }
}

private fun ExerciseResultRes.toOutcome(): ExerciseOutcome? {
    val resultId = id ?: return null
    val exerciseId = exercise?.id ?: return null
    return ExerciseOutcome(
        resultId = resultId,
        exerciseId = exerciseId,
        status = resultStatusOf(status),
        joints = points.orEmpty().map { point ->
            val skeletonPoint = point.pointTracked?.skeletonPoint
            MeasuredJoint(
                skeletonPointId = point.pointTracked?.skeletonPointId ?: skeletonPoint?.id,
                name = skeletonPoint?.verbose?.takeIf { it.isNotBlank() } ?: "Articulación",
                minAngle = point.minAngle,
                maxAngle = point.maxAngle,
            )
        }
    )
}

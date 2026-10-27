package com.example.therapp.data.routines

import com.example.therapp.common.api.ApiResponse
import com.example.therapp.common.api.ErrorMessages
import com.example.therapp.data.routines.local.RoutineDao
import com.example.therapp.data.routines.remote.RoutinesApi
import com.example.therapp.data.routines.remote.payload.mapper.RoutinesSnapshot
import com.example.therapp.data.routines.remote.payload.mapper.toDomain
import com.example.therapp.data.routines.remote.payload.mapper.toExpectedRange
import com.example.therapp.data.routines.remote.payload.mapper.toSnapshot
import com.example.therapp.domain.model.ExpectedRange
import com.example.therapp.domain.model.Routine
import com.example.therapp.domain.model.ScheduledTraining
import com.example.therapp.domain.repository.RoutinesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 9/19/2025
 * @version 1.0
 */
@Singleton
class RoutinesRepositoryImpl @Inject constructor(
    private val api: RoutinesApi,
    private val dao: RoutineDao
) : RoutinesRepository {

    // Antes emitía Loading mientras la tabla estuviera vacía: un paciente sin
    // rutinas veía "Cargando…" para siempre.
    override fun getScheduledTraining(): Flow<ApiResponse<List<ScheduledTraining>>> {
        return dao.getScheduledTrainingsWithRoutines().map { entities ->
            ApiResponse.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun refreshScheduledTraining(): ApiResponse<Unit> {
        return try {
            val response = api.getScheduledTraining()
            when {
                response.isSuccessful -> {
                    save(response.body().orEmpty().toSnapshot())
                    ApiResponse.Success(Unit)
                }
                // El backend responde 404 cuando el paciente no tiene rutinas.
                response.code() == 404 -> {
                    save(RoutinesSnapshot())
                    ApiResponse.Success(Unit)
                }
                else -> ApiResponse.Failure(
                    ErrorMessages.fromResponse(response.code(), response.errorBody()?.string()),
                    response.code()
                )
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

    private suspend fun save(snapshot: RoutinesSnapshot) {
        dao.updateAllData(
            snapshot.trainings,
            snapshot.therapies,
            snapshot.routines,
            snapshot.exercises,
            snapshot.difficulties,
            snapshot.ranges
        )
    }

    override fun getRoutineById(id: Int): Flow<Routine?> {
        return dao.getRoutineWithExercisesById(id).map { it?.toDomain() }
    }

    override fun getScheduledTrainingById(id: Int): Flow<ScheduledTraining?> {
        return dao.getScheduledTrainingById(id).map { it?.toDomain() }
    }

    override suspend fun getExpectedRange(scheduledTrainingId: Int, exerciseId: Int): ApiResponse<ExpectedRange?> {
        return try {
            val response = api.getScheduledDiff(scheduledTrainingId, exerciseId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ApiResponse.Success(body.toExpectedRange())
            } else {
                ApiResponse.Failure(
                    ErrorMessages.fromResponse(response.code(), response.errorBody()?.string()),
                    response.code()
                )
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
}
package com.example.therapp.domain.repository

import com.example.therapp.common.api.ApiResponse
import com.example.therapp.domain.model.ExpectedRange
import com.example.therapp.domain.model.Routine
import com.example.therapp.domain.model.ScheduledTraining
import kotlinx.coroutines.flow.Flow

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 9/19/2025
 * @version 1.0
 */
interface RoutinesRepository {
    fun getScheduledTraining(): Flow<ApiResponse<List<ScheduledTraining>>>
    suspend fun refreshScheduledTraining(): ApiResponse<Unit>
    fun getRoutineById(id: Int): Flow<Routine?>
    fun getScheduledTrainingById(id: Int): Flow<ScheduledTraining?>

    /** Success(null) si el ejercicio no tiene dificultad asignada. */
    suspend fun getExpectedRange(scheduledTrainingId: Int, exerciseId: Int): ApiResponse<ExpectedRange?>
}
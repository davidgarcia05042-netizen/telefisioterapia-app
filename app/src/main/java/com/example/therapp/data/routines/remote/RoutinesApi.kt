package com.example.therapp.data.routines.remote

import com.example.therapp.data.routines.remote.payload.res.ScheduledDiffRes
import com.example.therapp.data.routines.remote.payload.res.ScheduledTrainingRes
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 9/19/2025
 * @version 1.0
 */
interface RoutinesApi {
    @GET("/scheduled_training")
    suspend fun getScheduledTraining(): Response<List<ScheduledTrainingRes>?>

    @GET("/scheduled_training/diffs")
    suspend fun getScheduledDiff(
        @Query("scheduled_training_id") scheduledTrainingId: Int,
        @Query("exercise_id") exerciseId: Int
    ): Response<ScheduledDiffRes>
}
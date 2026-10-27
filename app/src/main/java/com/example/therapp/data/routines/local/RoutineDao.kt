package com.example.therapp.data.routines.local

import androidx.room.*
import com.example.therapp.data.local.BaseDao
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao : BaseDao<RoutineEntity> {

    @Transaction
    @Query("SELECT * FROM scheduled_trainings")
    fun getScheduledTrainingsWithRoutines(): Flow<List<ScheduledTrainingWithRoutine>>

    @Transaction
    @Query("SELECT * FROM scheduled_trainings WHERE id = :id")
    fun getScheduledTrainingById(id: Int): Flow<ScheduledTrainingWithRoutine?>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    fun getRoutineWithExercisesById(id: Int): Flow<RoutineWithExercises?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledTrainings(trainings: List<ScheduledTrainingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTherapies(therapies: List<TherapyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutines(routines: List<RoutineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDifficulties(difficulties: List<DifficultyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRanges(ranges: List<RangeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkeletonPoints(points: List<SkeletonPointEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPointsTracked(points: List<PointTrackedEntity>)

    @Query("DELETE FROM scheduled_trainings")
    suspend fun clearScheduledTrainings()

    @Query("DELETE FROM therapies")
    suspend fun clearTherapies()

    @Query("DELETE FROM routines")
    suspend fun clearRoutines()

    @Query("DELETE FROM exercises")
    suspend fun clearExercises()

    @Query("DELETE FROM difficulties")
    suspend fun clearDifficulties()

    @Query("DELETE FROM ranges")
    suspend fun clearRanges()
    
    /** Al cerrar sesión: la copia local de las rutinas es del paciente que sale. */
    @Transaction
    suspend fun clearAll() {
        clearRanges()
        clearDifficulties()
        clearExercises()
        clearScheduledTrainings()
        clearRoutines()
        clearTherapies()
    }

    @Transaction
    suspend fun updateAllData(
        trainings: List<ScheduledTrainingEntity>,
        therapies: List<TherapyEntity>,
        routines: List<RoutineEntity>,
        exercises: List<ExerciseEntity>,
        difficulties: List<DifficultyEntity>,
        ranges: List<RangeEntity>
    ) {
        clearScheduledTrainings()
        clearRoutines()
        clearTherapies()
        clearExercises()
        clearDifficulties()
        clearRanges()

        insertTherapies(therapies)
        insertRoutines(routines)
        insertScheduledTrainings(trainings)
        insertExercises(exercises)
        insertDifficulties(difficulties)
        insertRanges(ranges)
    }
}

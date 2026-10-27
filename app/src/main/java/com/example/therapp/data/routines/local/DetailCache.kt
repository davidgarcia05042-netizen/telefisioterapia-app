package com.example.therapp.data.routines.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction

/**
 * Lo último que se vio en el detalle de una rutina programada, para mostrarlo
 * al abrirla mientras se actualiza en segundo plano. No se borra al refrescar
 * la lista de rutinas (RoutineDao.updateAllData), solo al cerrar sesión.
 */

/**
 * Rango esperado de un ejercicio en una rutina programada (la dificultad
 * asignada). [rangeJson] nulo = sin dificultad asignada. Guardarlo es seguro:
 * el backend no deja cambiar los rangos de una dificultad ya asignada (409).
 */
@Entity(tableName = "expected_range_cache", primaryKeys = ["scheduledTrainingId", "exerciseId"])
data class ExpectedRangeCacheEntity(
    val scheduledTrainingId: Int,
    val exerciseId: Int,
    val rangeJson: String?
)

/** Sesión de la rutina y último envío de cada ejercicio. */
@Entity(tableName = "routine_progress_cache")
data class RoutineProgressCacheEntity(
    @PrimaryKey val scheduledTrainingId: Int,
    val trainingId: Int?,
    val outcomesJson: String
)

@Dao
interface DetailCacheDao {

    @Query("SELECT * FROM expected_range_cache WHERE scheduledTrainingId = :scheduledTrainingId")
    suspend fun getRanges(scheduledTrainingId: Int): List<ExpectedRangeCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRange(range: ExpectedRangeCacheEntity)

    @Query("SELECT * FROM routine_progress_cache WHERE scheduledTrainingId = :scheduledTrainingId")
    suspend fun getProgress(scheduledTrainingId: Int): RoutineProgressCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: RoutineProgressCacheEntity)

    @Query("DELETE FROM expected_range_cache")
    suspend fun clearRanges()

    @Query("DELETE FROM routine_progress_cache")
    suspend fun clearProgress()

    /** Al cerrar sesión, con el resto de la copia local. */
    @Transaction
    suspend fun clearAll() {
        clearRanges()
        clearProgress()
    }
}

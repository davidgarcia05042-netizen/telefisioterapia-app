package com.example.therapp.data.routines

import com.example.therapp.data.routines.local.DetailCacheDao
import com.example.therapp.data.routines.local.ExpectedRangeCacheEntity
import com.example.therapp.data.routines.local.RoutineProgressCacheEntity
import com.example.therapp.domain.model.ExerciseOutcome
import com.example.therapp.domain.model.ExpectedRange
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

/** Lo guardado de una rutina programada: su sesión y el último envío de cada ejercicio. */
data class CachedProgress(val trainingId: Int?, val outcomes: List<ExerciseOutcome>)

/**
 * Copia local del detalle de una rutina (rangos esperados y último estado),
 * para mostrarla al abrir sin pantalla de carga mientras se actualiza.
 */
@Singleton
class RoutineDetailCache @Inject constructor(private val dao: DetailCacheDao) {

    /** Por id de ejercicio; un valor nulo = sin dificultad asignada. */
    suspend fun ranges(scheduledTrainingId: Int): Map<Int, ExpectedRange?> =
        dao.getRanges(scheduledTrainingId)
            .mapNotNull { row ->
                val range = DetailCacheCodec.decodeRange(row.rangeJson) ?: return@mapNotNull null
                row.exerciseId to range.value
            }
            .toMap()

    suspend fun saveRange(scheduledTrainingId: Int, exerciseId: Int, range: ExpectedRange?) {
        dao.saveRange(ExpectedRangeCacheEntity(scheduledTrainingId, exerciseId, DetailCacheCodec.encodeRange(range)))
    }

    suspend fun progress(scheduledTrainingId: Int): CachedProgress? {
        val row = dao.getProgress(scheduledTrainingId) ?: return null
        val outcomes = DetailCacheCodec.decodeOutcomes(row.outcomesJson) ?: return null
        return CachedProgress(row.trainingId, outcomes)
    }

    suspend fun saveProgress(scheduledTrainingId: Int, trainingId: Int?, outcomes: Collection<ExerciseOutcome>) {
        dao.saveProgress(
            RoutineProgressCacheEntity(scheduledTrainingId, trainingId, DetailCacheCodec.encodeOutcomes(outcomes))
        )
    }
}

/** Un rango leído de la copia; [value] nulo = sin dificultad asignada. */
data class CachedRange(val value: ExpectedRange?)

/**
 * JSON de lo guardado. Algo que no se pueda leer (por ejemplo, de una versión
 * anterior de la app) se ignora: la pantalla lo vuelve a pedir al backend.
 */
object DetailCacheCodec {
    private val gson = Gson()
    private const val NO_RANGE = "null"
    private val outcomesType = object : TypeToken<List<ExerciseOutcome>>() {}.type

    fun encodeRange(range: ExpectedRange?): String = if (range == null) NO_RANGE else gson.toJson(range)

    /** null si no se puede leer; CachedRange(null) si no hay dificultad asignada. */
    fun decodeRange(json: String?): CachedRange? {
        if (json == null) return null
        if (json == NO_RANGE) return CachedRange(null)
        val range = runCatching { gson.fromJson(json, ExpectedRange::class.java) }.getOrNull() ?: return null
        // Gson no respeta la nulabilidad de Kotlin: un campo faltante llega nulo.
        @Suppress("SENSELESS_COMPARISON")
        val complete = range.difficultyName != null && range.joints != null && range.joints.all { it.name != null }
        return if (complete) CachedRange(range) else null
    }

    fun encodeOutcomes(outcomes: Collection<ExerciseOutcome>): String = gson.toJson(outcomes.toList())

    fun decodeOutcomes(json: String): List<ExerciseOutcome>? {
        val outcomes = runCatching { gson.fromJson<List<ExerciseOutcome>>(json, outcomesType) }.getOrNull()
            ?: return null
        @Suppress("SENSELESS_COMPARISON")
        val complete = outcomes.all { outcome ->
            outcome != null && outcome.status != null && outcome.joints != null && outcome.joints.all { it.name != null }
        }
        return if (complete) outcomes else null
    }
}

package com.example.therapp.data.routines.remote.payload.mapper

import com.example.therapp.data.routines.local.DifficultyEntity
import com.example.therapp.data.routines.local.DifficultyWithRanges
import com.example.therapp.data.routines.local.ExerciseEntity
import com.example.therapp.data.routines.local.ExerciseWithDifficulties
import com.example.therapp.data.routines.local.RangeEntity
import com.example.therapp.data.routines.local.RoutineEntity
import com.example.therapp.data.routines.local.RoutineWithExercises
import com.example.therapp.data.routines.local.ScheduledTrainingEntity
import com.example.therapp.data.routines.local.ScheduledTrainingWithRoutine
import com.example.therapp.data.routines.local.TherapyEntity
import com.example.therapp.data.routines.remote.payload.res.DifficultyRes
import com.example.therapp.data.routines.remote.payload.res.ExercisesRes
import com.example.therapp.data.routines.remote.payload.res.RangeRes
import com.example.therapp.data.routines.remote.payload.res.RoutineRes
import com.example.therapp.data.routines.remote.payload.res.ScheduledDiffRes
import com.example.therapp.data.routines.remote.payload.res.ScheduledTrainingRes
import com.example.therapp.data.routines.remote.payload.res.TherapyRes
import com.example.therapp.domain.model.Difficulty
import com.example.therapp.domain.model.Exercise
import com.example.therapp.domain.model.ExpectedRange
import com.example.therapp.domain.model.JointRange
import com.example.therapp.domain.model.Range
import com.example.therapp.domain.model.Routine
import com.example.therapp.domain.model.ScheduledTraining
import com.example.therapp.domain.model.Therapy

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 3/5/2026
 * @version 1.0
 */
/**
 * Lo que se guarda en Room a partir de GET /scheduled_training.
 */
data class RoutinesSnapshot(
    val trainings: List<ScheduledTrainingEntity> = emptyList(),
    val therapies: List<TherapyEntity> = emptyList(),
    val routines: List<RoutineEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val difficulties: List<DifficultyEntity> = emptyList(),
    val ranges: List<RangeEntity> = emptyList(),
)

/**
 * Tolerante a nulos: lo que no tiene id se descarta y el resto toma valores
 * por defecto. Antes un campo nulo (una rutina programada sin rutina, una
 * descripción vacía) hacía fallar toda la actualización.
 *
 * Las relaciones salen del anidamiento y no de los *_id de cada objeto: así
 * cada fila apunta a una que también se guarda (Room exige las claves
 * foráneas).
 */
fun List<ScheduledTrainingRes>.toSnapshot(): RoutinesSnapshot {
    val usable = filter { it.id != null && it.routine?.id != null }
    val routines = usable.mapNotNull { it.routine }.distinctBy { it.id }
    val exercises = routines.flatMap { routine ->
        routine.exercises.orEmpty().filter { it.id != null }.map { routine.id!! to it }
    }.distinctBy { it.second.id }
    val difficulties = exercises.flatMap { (_, exercise) ->
        exercise.difficulties.orEmpty().filter { it.id != null }.map { exercise.id!! to it }
    }.distinctBy { it.second.id }

    return RoutinesSnapshot(
        trainings = usable.map {
            ScheduledTrainingEntity(
                id = it.id!!,
                routineId = it.routine!!.id!!,
                startTime = it.startTime.orEmpty(),
                status = it.status.orEmpty()
            )
        },
        therapies = routines.mapNotNull { it.therapy?.toEntity() }.distinctBy { it.id },
        routines = routines.map { it.toEntity() },
        exercises = exercises.map { (routineId, exercise) -> exercise.toEntity(routineId) },
        difficulties = difficulties.map { (exerciseId, difficulty) -> difficulty.toEntity(exerciseId) },
        ranges = difficulties.flatMap { (_, difficulty) ->
            difficulty.ranges.orEmpty().map { it.toEntity(difficulty.id!!) }
        }
    )
}

private fun TherapyRes.toEntity(): TherapyEntity? = id?.let {
    TherapyEntity(
        id = it,
        name = name.orEmpty(),
        description = description.orEmpty(),
        isModel = isModel ?: false,
        isActive = isActive ?: true
    )
}

private fun RoutineRes.toEntity() = RoutineEntity(
    id = id!!,
    therapyId = therapy?.id,
    name = name.orEmpty(),
    description = description.orEmpty(),
    isModeled = isModeled ?: false,
    isActive = isActive ?: true
)

private fun ExercisesRes.toEntity(routineId: Int) = ExerciseEntity(
    id = id!!,
    routineId = routineId,
    name = name.orEmpty(),
    description = description.orEmpty(),
    order = order ?: 0,
    status = status.orEmpty(),
    video = video?.takeIf { it.isNotBlank() },
    isModel = isModel ?: false,
    isActive = isActive ?: true
)

private fun DifficultyRes.toEntity(exerciseId: Int) = DifficultyEntity(
    id = id!!,
    exerciseId = exerciseId,
    name = name.orEmpty(),
    description = description.orEmpty()
)

private fun RangeRes.toEntity(difficultyId: Int) = RangeEntity(
    difficultyId = difficultyId,
    pointTrackedId = pointTrackedId,
    maxAngle = maxAngle,
    minAngle = minAngle
)

/**
 * Rango esperado a partir de la dificultad asignada. El nombre de cada
 * articulación es el `verbose` del punto del esqueleto ("Rodilla izquierda").
 */
fun ScheduledDiffRes.toExpectedRange(): ExpectedRange? = difficulty?.let { assigned ->
    ExpectedRange(
        difficultyName = assigned.name.orEmpty(),
        joints = assigned.ranges.orEmpty().map { range ->
            JointRange(
                name = range.pointTracked?.skeletonPoint?.verbose?.takeIf { it.isNotBlank() } ?: "Articulación",
                minAngle = range.minAngle,
                maxAngle = range.maxAngle,
                skeletonPointId = range.pointTracked?.skeletonPointId ?: range.pointTracked?.skeletonPoint?.id
            )
        }
    )
}

// Domain Mappers
fun ScheduledTrainingWithRoutine.toDomain() = ScheduledTraining(
    id = scheduledTraining.id,
    routine = routine.toDomain(),
    startTime = scheduledTraining.startTime,
    status = scheduledTraining.status
)

fun RoutineWithExercises.toDomain() = Routine(
    id = routine.id,
    therapyId = routine.therapyId,
    therapy = therapy?.toDomain(),
    name = routine.name,
    description = routine.description,
    exercises = exercises.map { it.toDomain() },
    isModeled = routine.isModeled,
    isActive = routine.isActive
)

fun TherapyEntity.toDomain() = Therapy(
    id = id,
    name = name,
    description = description,
    isModel = isModel,
    isActive = isActive
)

fun ExerciseWithDifficulties.toDomain() = Exercise(
    id = exercise.id,
    name = exercise.name,
    description = exercise.description,
    order = exercise.order,
    routineId = exercise.routineId,
    status = exercise.status,
    video = exercise.video,
    isModel = exercise.isModel,
    isActive = exercise.isActive,
    difficulties = difficulties.map { it.toDomain() }
)

fun DifficultyWithRanges.toDomain() = Difficulty(
    id = difficulty.id,
    exerciseId = difficulty.exerciseId,
    name = difficulty.name,
    description = difficulty.description,
    ranges = ranges.map { it.toDomain() }
)

fun RangeEntity.toDomain() = Range(
    id = id,
    pointTrackedId = pointTrackedId,
    pointTracked = null, // Can be fetched if needed
    difficultyId = difficultyId,
    maxAngle = maxAngle,
    minAngle = minAngle
)

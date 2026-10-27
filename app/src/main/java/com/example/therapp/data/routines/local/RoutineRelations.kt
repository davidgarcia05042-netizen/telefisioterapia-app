package com.example.therapp.data.routines.local

import androidx.room.Embedded
import androidx.room.Relation

data class ScheduledTrainingWithRoutine(
    @Embedded val scheduledTraining: ScheduledTrainingEntity,
    @Relation(
        entity = RoutineEntity::class,
        parentColumn = "routineId",
        entityColumn = "id"
    )
    val routine: RoutineWithExercises
)

data class RoutineWithExercises(
    @Embedded val routine: RoutineEntity,
    @Relation(
        entity = TherapyEntity::class,
        parentColumn = "therapyId",
        entityColumn = "id"
    )
    val therapy: TherapyEntity?,
    @Relation(
        entity = ExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val exercises: List<ExerciseWithDifficulties>
)

data class ExerciseWithDifficulties(
    @Embedded val exercise: ExerciseEntity,
    @Relation(
        entity = DifficultyEntity::class,
        parentColumn = "id",
        entityColumn = "exerciseId"
    )
    val difficulties: List<DifficultyWithRanges>
)

data class DifficultyWithRanges(
    @Embedded val difficulty: DifficultyEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "difficultyId"
    )
    val ranges: List<RangeEntity>
)

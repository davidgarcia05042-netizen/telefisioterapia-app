package com.example.therapp.data.routines.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.therapp.domain.model.*

@Entity(tableName = "scheduled_trainings")
data class ScheduledTrainingEntity(
    @PrimaryKey val id: Int,
    val routineId: Int,
    val startTime: String,
    val status: String
)

@Entity(tableName = "therapies")
data class TherapyEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val isModel: Boolean,
    val isActive: Boolean
)

@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(
            entity = TherapyEntity::class,
            parentColumns = ["id"],
            childColumns = ["therapyId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoutineEntity(
    @PrimaryKey val id: Int,
    // Nulo si la rutina quedó sin terapia (el backend usa SET_NULL).
    val therapyId: Int?,
    val name: String,
    val description: String,
    val isModeled: Boolean,
    val isActive: Boolean
)

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseEntity(
    @PrimaryKey val id: Int,
    val routineId: Int,
    val name: String,
    val description: String,
    val order: Int,
    val status: String,
    val video: String?,
    val isModel: Boolean,
    val isActive: Boolean
)

@Entity(
    tableName = "difficulties",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DifficultyEntity(
    @PrimaryKey val id: Int,
    val exerciseId: Int,
    val name: String,
    val description: String
)

@Entity(
    tableName = "ranges",
    foreignKeys = [
        ForeignKey(
            entity = DifficultyEntity::class,
            parentColumns = ["id"],
            childColumns = ["difficultyId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val difficultyId: Int,
    val pointTrackedId: Int?,
    // El backend permite rangos sin ángulo.
    val maxAngle: Double?,
    val minAngle: Double?
)

@Entity(tableName = "skeleton_points")
data class SkeletonPointEntity(
    @PrimaryKey val id: Int,
    val codename: String,
    val verbose: String,
    val leftPoint: Int,
    val rightPoint: Int
)

@Entity(
    tableName = "points_tracked",
    foreignKeys = [
        ForeignKey(
            entity = SkeletonPointEntity::class,
            parentColumns = ["id"],
            childColumns = ["skeletonPointId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PointTrackedEntity(
    @PrimaryKey val id: Int,
    val skeletonPointId: Int,
    val exerciseId: Int,
    val maxAngle: Double,
    val minAngle: Double
)

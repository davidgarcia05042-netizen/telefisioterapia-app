package com.example.therapp.domain.model

data class ScheduledTraining(
    val id: Int,
    val routine: Routine,
    val startTime: String,
    val status: String
)

data class Routine(
    val id: Int,
    val therapyId: Int?,
    val therapy: Therapy?,
    val name: String,
    val description: String,
    val exercises: List<Exercise>,
    val isModeled: Boolean,
    val isActive: Boolean
)

data class Therapy(
    val id: Int,
    val name: String,
    val description: String,
    val isModel: Boolean,
    val isActive: Boolean
)

data class Exercise(
    val id: Int,
    val name: String,
    val description: String,
    val order: Int,
    val routineId: Int,
    val status: String,
    val video: String?,
    val isModel: Boolean,
    val isActive: Boolean,
    val difficulties: List<Difficulty>
)

data class Difficulty(
    val id: Int,
    val exerciseId: Int,
    val name: String,
    val description: String,
    val ranges: List<Range>
)

data class Range(
    val id: Int = 0, // Room auto-gen or from server if available
    val pointTrackedId: Int?,
    val pointTracked: PointTracked?,
    val difficultyId: Int,
    val maxAngle: Double?,
    val minAngle: Double?
)

data class PointTracked(
    val skeletonPoint: SkeletonPoint?,
    val skeletonPointId: Int,
    val exerciseId: Int,
    val maxAngle: Double,
    val minAngle: Double
)

data class SkeletonPoint(
    val id: Int,
    val codename: String,
    val verbose: String,
    val leftPoint: Int,
    val rightPoint: Int
)

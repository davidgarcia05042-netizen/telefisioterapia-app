package com.example.therapp.data.routines.remote.payload.res

/**
 * GET /scheduled_training/diffs?scheduled_training_id=&exercise_id=: la
 * dificultad asignada a ese ejercicio en esa rutina programada, o
 * {"difficulty": null} si no tiene. Solo se lee la dificultad; los demás
 * campos (exercise, scheduled_training) la app ya los tiene.
 */
data class ScheduledDiffRes(
    val difficulty: DifficultyRes? = null,
)

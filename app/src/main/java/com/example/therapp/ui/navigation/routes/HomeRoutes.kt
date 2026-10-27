package com.example.therapp.ui.navigation.routes

import kotlinx.serialization.Serializable


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 9/4/2025
 * @version 1.0
 */
@Serializable
sealed class HomeRoutes {
    @Serializable
    object RoutinesScreen : HomeRoutes()

    @Serializable
    object CameraScreen : HomeRoutes()

    @Serializable
    // Por rutina programada, no por rutina: el rango esperado depende de la
    // dificultad asignada en esa programación y el envío necesita su id.
    data class RoutineDetailScreen(val scheduledTrainingId: Int) : HomeRoutes()

    /** Cámara para hacer y enviar un ejercicio de una rutina programada. */
    @Serializable
    data class ExerciseCamera(
        val scheduledTrainingId: Int,
        val exerciseId: Int,
        val exerciseName: String
    ) : HomeRoutes()
}
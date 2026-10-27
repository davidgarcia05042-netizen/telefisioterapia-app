package com.example.therapp.domain.model

/** Rango esperado de un ejercicio en una rutina programada (la dificultad asignada). */
data class ExpectedRange(
    val difficultyName: String,
    val joints: List<JointRange>,
)

/**
 * Una articulación con su rango en grados; los ángulos pueden faltar.
 * [skeletonPointId] la une con lo que midió el servicio (MeasuredJoint).
 */
data class JointRange(
    val name: String,
    val minAngle: Double?,
    val maxAngle: Double?,
    val skeletonPointId: Int? = null,
)

/** "30° a 120°", o lo que haya si falta un extremo. */
fun formatRange(min: Double?, max: Double?): String {
    return when {
        min != null && max != null -> "${formatDegrees(min)} a ${formatDegrees(max)}"
        min != null -> "desde ${formatDegrees(min)}"
        max != null -> "hasta ${formatDegrees(max)}"
        else -> "sin rango definido"
    }
}

/** "118°": grados enteros, como se muestran en toda la app. */
fun formatDegrees(value: Double): String = "${Math.round(value)}°"

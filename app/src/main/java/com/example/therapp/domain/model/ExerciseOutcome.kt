package com.example.therapp.domain.model

/** Estado del envío del paciente para un ejercicio de la rutina. */
enum class ResultStatus(val label: String) {
    PENDING("Pendiente"),
    PROCESSING("Procesando"),
    PROCESSED("Procesado"),
    ERROR("Error"),
}

/** Lo que midió el servicio en una articulación. */
data class MeasuredJoint(
    val skeletonPointId: Int?,
    val name: String,
    val minAngle: Double?,
    val maxAngle: Double?,
)

/** El último resultado enviado de un ejercicio (GET /exercise_results?training_id=). */
data class ExerciseOutcome(
    val resultId: Int,
    val exerciseId: Int,
    val status: ResultStatus,
    val joints: List<MeasuredJoint>,
)

/**
 * El backend manda el estado con su texto ("Video procesado"); también se
 * aceptan los códigos. "Sin video" o un valor desconocido cuentan como
 * pendiente: el paciente puede grabar de nuevo.
 */
fun resultStatusOf(raw: String?): ResultStatus = when (raw?.trim()?.lowercase()) {
    "video en procesamiento", "2" -> ResultStatus.PROCESSING
    "video procesado", "3" -> ResultStatus.PROCESSED
    "error al procesar video", "4" -> ResultStatus.ERROR
    else -> ResultStatus.PENDING
}

/** Si el paciente envió varias veces un ejercicio, vale el último envío. */
fun latestByExercise(outcomes: List<ExerciseOutcome>): Map<Int, ExerciseOutcome> =
    outcomes.groupBy { it.exerciseId }.mapValues { (_, sent) -> sent.maxBy { it.resultId } }

/**
 * "Terminar rutina" cuando cada ejercicio tiene un resultado: procesado o en
 * error. Uno que no se pudo medir no bloquea la rutina; el terapeuta lo ve en
 * el panel. Mientras algo se procesa, no.
 */
fun canFinishRoutine(exerciseIds: Collection<Int>, outcomes: Map<Int, ExerciseOutcome>): Boolean =
    exerciseIds.isNotEmpty() && exerciseIds.all {
        outcomes[it]?.status == ResultStatus.PROCESSED || outcomes[it]?.status == ResultStatus.ERROR
    }

/**
 * Aviso de la confirmación de "Terminar rutina" con los ejercicios que
 * quedaron en error, o null si no hay ninguno.
 */
fun erroredExercisesNotice(names: List<String>): String? = when (names.size) {
    0 -> null
    1 -> "Quedó en error: ${names[0]}. Tu terapeuta lo verá."
    else -> "Quedaron en error: ${names.dropLast(1).joinToString(", ")} y ${names.last()}. Tu terapeuta lo verá."
}

/**
 * Estados de la rutina programada ("Sin iniciar", "Iniciado", "Finalizado").
 * Solo una rutina iniciada o finalizada tiene sesión: POST /training la crea
 * al enviar el primer video.
 */
fun scheduledStatusHasSession(status: String): Boolean =
    status.trim().lowercase() in setOf("iniciado", "finalizado", "2", "3")

fun scheduledStatusIsFinished(status: String): Boolean =
    status.trim().lowercase() in setOf("finalizado", "3")

/** Mínimo y máximo medidos frente a las metas de la articulación. */
sealed class RangeCheck {
    /** El ejercicio no tiene rango para esta articulación. */
    object NoRange : RangeCheck()

    /** El servicio no devolvió ángulos. */
    object Unmeasured : RangeCheck()

    object Reached : RangeCheck()

    /** [gaps]: "Llegó a 54°, meta 37° (faltaron 17°)", primero el mínimo y después el máximo. */
    data class NotReached(val gaps: List<String>) : RangeCheck()
}

/** Umbral de error aceptable de la medición (tesis): 5°. */
const val RANGE_TOLERANCE_DEGREES = 5

/** El rango esperado de [joint], por su punto del esqueleto o, si falta, por nombre. */
fun ExpectedRange.rangeFor(joint: MeasuredJoint): JointRange? =
    joints.firstOrNull { joint.skeletonPointId != null && it.skeletonPointId == joint.skeletonPointId }
        ?: joints.firstOrNull { joint.skeletonPointId == null && it.name.equals(joint.name, ignoreCase = true) }

/**
 * Los extremos de una dificultad son METAS, no una zona permitida: como en el
 * RepetitionCounter, una repetición es llegar al máximo y volver al mínimo.
 * Alcanzó el rango si bajó hasta el mínimo y subió hasta el máximo, con 5° de
 * tolerancia: min medido <= min esperado + 5 y max medido >= max esperado - 5.
 * Hacer más movimiento también lo alcanza.
 *
 * Se compara en grados enteros, como se muestran, y lo que faltó se cuenta
 * hasta la meta, no hasta la tolerancia.
 */
fun checkRange(joint: MeasuredJoint, expected: JointRange?): RangeCheck {
    if (joint.minAngle == null && joint.maxAngle == null) return RangeCheck.Unmeasured
    if (expected == null || (expected.minAngle == null && expected.maxAngle == null)) return RangeCheck.NoRange

    val gaps = mutableListOf<String>()
    expected.minAngle?.let { Math.round(it) }?.let { goal ->
        val reached = joint.minAngle?.let { Math.round(it) }
        when {
            reached == null -> gaps += "Sin medición del mínimo, meta $goal°"
            reached > goal + RANGE_TOLERANCE_DEGREES ->
                gaps += "Llegó a $reached°, meta $goal° (faltaron ${reached - goal}°)"
        }
    }
    expected.maxAngle?.let { Math.round(it) }?.let { goal ->
        val reached = joint.maxAngle?.let { Math.round(it) }
        when {
            reached == null -> gaps += "Sin medición del máximo, meta $goal°"
            reached < goal - RANGE_TOLERANCE_DEGREES ->
                gaps += "Llegó a $reached°, meta $goal° (faltaron ${goal - reached}°)"
        }
    }
    return if (gaps.isEmpty()) RangeCheck.Reached else RangeCheck.NotReached(gaps)
}

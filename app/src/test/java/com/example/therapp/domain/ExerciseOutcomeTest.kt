package com.example.therapp.domain

import com.example.therapp.domain.model.ExerciseOutcome
import com.example.therapp.domain.model.ExpectedRange
import com.example.therapp.domain.model.JointRange
import com.example.therapp.domain.model.MeasuredJoint
import com.example.therapp.domain.model.RangeCheck
import com.example.therapp.domain.model.ResultStatus
import com.example.therapp.domain.model.canFinishRoutine
import com.example.therapp.domain.model.checkRange
import com.example.therapp.domain.model.erroredExercisesNotice
import com.example.therapp.domain.model.latestByExercise
import com.example.therapp.domain.model.rangeFor
import com.example.therapp.domain.model.resultStatusOf
import com.example.therapp.domain.model.scheduledStatusHasSession
import com.example.therapp.domain.model.scheduledStatusIsFinished
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseOutcomeTest {

    private val knee = JointRange("Rodilla izquierda", minAngle = 30.0, maxAngle = 120.0, skeletonPointId = 25)

    private fun measured(min: Double?, max: Double?, id: Int? = 25, name: String = "Rodilla izquierda") =
        MeasuredJoint(skeletonPointId = id, name = name, minAngle = min, maxAngle = max)

    private fun outcome(resultId: Int, exerciseId: Int, status: ResultStatus) =
        ExerciseOutcome(resultId, exerciseId, status, emptyList())

    @Test
    fun statusFromBackendLabelsAndCodes() {
        assertEquals(ResultStatus.PROCESSING, resultStatusOf("Video en procesamiento"))
        assertEquals(ResultStatus.PROCESSED, resultStatusOf("Video procesado"))
        assertEquals(ResultStatus.ERROR, resultStatusOf("Error al procesar video"))
        assertEquals(ResultStatus.PENDING, resultStatusOf("Sin video"))
        assertEquals(ResultStatus.PROCESSED, resultStatusOf("3"))
        assertEquals(ResultStatus.PENDING, resultStatusOf(null))
        assertEquals(ResultStatus.PENDING, resultStatusOf("otro"))
    }

    @Test
    fun lastSubmissionWins() {
        // Un error y después un reenvío procesado: vale el reenvío.
        val latest = latestByExercise(
            listOf(outcome(7, 1, ResultStatus.PROCESSED), outcome(4, 1, ResultStatus.ERROR), outcome(5, 2, ResultStatus.PROCESSING))
        )
        assertEquals(ResultStatus.PROCESSED, latest[1]!!.status)
        assertEquals(ResultStatus.PROCESSING, latest[2]!!.status)
    }

    @Test
    fun finishWhenEveryExerciseHasAResult() {
        val done = mapOf(1 to outcome(1, 1, ResultStatus.PROCESSED), 2 to outcome(2, 2, ResultStatus.PROCESSED))
        assertTrue(canFinishRoutine(listOf(1, 2), done))
        // Un error es un resultado: no bloquea la rutina.
        assertTrue(canFinishRoutine(listOf(1, 2), done + (2 to outcome(3, 2, ResultStatus.ERROR))))
        // Sin envío o todavía procesando, no.
        assertFalse(canFinishRoutine(listOf(1, 2, 3), done))
        assertFalse(canFinishRoutine(listOf(1, 2), done + (2 to outcome(3, 2, ResultStatus.PROCESSING))))
        assertFalse(canFinishRoutine(listOf(1, 2), done + (2 to outcome(3, 2, ResultStatus.PENDING))))
        assertFalse(canFinishRoutine(emptyList(), done))
    }

    @Test
    fun finishNoticeNamesTheExercisesInError() {
        assertNull(erroredExercisesNotice(emptyList()))
        assertEquals(
            "Quedó en error: Extensión de rodilla. Tu terapeuta lo verá.",
            erroredExercisesNotice(listOf("Extensión de rodilla"))
        )
        assertEquals(
            "Quedaron en error: Flexión, Extensión y Cadera. Tu terapeuta lo verá.",
            erroredExercisesNotice(listOf("Flexión", "Extensión", "Cadera"))
        )
    }

    @Test
    fun scheduledStatus() {
        assertFalse(scheduledStatusHasSession("Sin iniciar"))
        assertTrue(scheduledStatusHasSession("Iniciado"))
        assertTrue(scheduledStatusHasSession("Finalizado"))
        assertTrue(scheduledStatusIsFinished("Finalizado"))
        assertFalse(scheduledStatusIsFinished("Iniciado"))
    }

    @Test
    fun reachedTheGoals() {
        // Bajó a 30° y subió a 120°: exacto.
        assertEquals(RangeCheck.Reached, checkRange(measured(30.0, 120.0), knee))
        // Más movimiento que la meta también la alcanza.
        assertEquals(RangeCheck.Reached, checkRange(measured(12.0, 140.0), knee))
        // Dentro de la tolerancia de 5°: 35° y 115°.
        assertEquals(RangeCheck.Reached, checkRange(measured(35.0, 115.0), knee))
        // Se compara como se muestra: 35,4° es "35°".
        assertEquals(RangeCheck.Reached, checkRange(measured(35.4, 114.6), knee))
    }

    @Test
    fun lessMovementDoesNotReachTheRange() {
        // Antes (contención) esto salía "dentro del rango".
        assertEquals(
            RangeCheck.NotReached(listOf("Llegó a 54°, meta 30° (faltaron 24°)", "Llegó a 100°, meta 120° (faltaron 20°)")),
            checkRange(measured(54.0, 100.0), knee)
        )
        assertEquals(
            RangeCheck.NotReached(listOf("Llegó a 54°, meta 37° (faltaron 17°)")),
            checkRange(measured(54.0, 120.0), knee.copy(minAngle = 37.0))
        )
        // Justo fuera de la tolerancia: 36° y 114°.
        assertEquals(
            RangeCheck.NotReached(listOf("Llegó a 36°, meta 30° (faltaron 6°)", "Llegó a 114°, meta 120° (faltaron 6°)")),
            checkRange(measured(36.0, 114.0), knee)
        )
    }

    @Test
    fun withoutRangeOrMeasurement() {
        assertEquals(RangeCheck.NoRange, checkRange(measured(35.0, 118.0), null))
        assertEquals(RangeCheck.NoRange, checkRange(measured(35.0, 118.0), knee.copy(minAngle = null, maxAngle = null)))
        assertEquals(RangeCheck.Unmeasured, checkRange(measured(null, null), knee))
        // Con una sola meta se compara esa.
        assertEquals(RangeCheck.Reached, checkRange(measured(80.0, 118.0), knee.copy(minAngle = null)))
        assertEquals(
            RangeCheck.NotReached(listOf("Llegó a 90°, meta 100° (faltaron 10°)")),
            checkRange(measured(35.0, 90.0), knee.copy(minAngle = null, maxAngle = 100.0))
        )
        // Falta un extremo medido: no se da por alcanzado.
        assertEquals(
            RangeCheck.NotReached(listOf("Sin medición del máximo, meta 120°")),
            checkRange(measured(30.0, null), knee)
        )
    }

    @Test
    fun jointsMatchBySkeletonPointThenByName() {
        val expected = ExpectedRange(
            "Fácil",
            listOf(knee, JointRange("Cadera izquierda", 10.0, 80.0, skeletonPointId = 23))
        )
        assertEquals(23, expected.rangeFor(measured(1.0, 2.0, id = 23, name = "Otro nombre"))!!.skeletonPointId)
        assertEquals(25, expected.rangeFor(measured(1.0, 2.0, id = null, name = "rodilla izquierda"))!!.skeletonPointId)
        assertNull(expected.rangeFor(measured(1.0, 2.0, id = 11, name = "Rodilla izquierda")))
    }
}

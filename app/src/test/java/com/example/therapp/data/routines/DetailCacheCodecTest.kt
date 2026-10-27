package com.example.therapp.data.routines

import com.example.therapp.domain.model.ExerciseOutcome
import com.example.therapp.domain.model.ExpectedRange
import com.example.therapp.domain.model.JointRange
import com.example.therapp.domain.model.MeasuredJoint
import com.example.therapp.domain.model.ResultStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Lo que se guarda del detalle de la rutina se lee igual; lo ilegible se ignora. */
class DetailCacheCodecTest {

    private val range = ExpectedRange(
        difficultyName = "Media",
        joints = listOf(
            JointRange("Rodilla izquierda", 37.0, 120.0, skeletonPointId = 25),
            // El backend permite rangos sin ángulo.
            JointRange("Cadera izquierda", null, 90.0, skeletonPointId = null),
        )
    )

    private val outcomes = listOf(
        ExerciseOutcome(6, 11, ResultStatus.PROCESSED, listOf(MeasuredJoint(25, "Rodilla izquierda", 34.2, 118.5))),
        ExerciseOutcome(7, 12, ResultStatus.PROCESSING, emptyList()),
        ExerciseOutcome(8, 13, ResultStatus.ERROR, listOf(MeasuredJoint(null, "Articulación", null, null))),
    )

    @Test
    fun rangeRoundTrip() {
        assertEquals(CachedRange(range), DetailCacheCodec.decodeRange(DetailCacheCodec.encodeRange(range)))
    }

    @Test
    fun noDifficultyAssignedIsKeptApartFromUnreadable() {
        assertEquals(CachedRange(null), DetailCacheCodec.decodeRange(DetailCacheCodec.encodeRange(null)))
        assertNull(DetailCacheCodec.decodeRange(null))
    }

    @Test
    fun outcomesRoundTrip() {
        assertEquals(outcomes, DetailCacheCodec.decodeOutcomes(DetailCacheCodec.encodeOutcomes(outcomes)))
        assertEquals(emptyList<ExerciseOutcome>(), DetailCacheCodec.decodeOutcomes(DetailCacheCodec.encodeOutcomes(emptyList())))
    }

    @Test
    fun unreadableDataIsIgnored() {
        assertNull(DetailCacheCodec.decodeRange("no es json"))
        // Sin los campos que la pantalla necesita (Gson los dejaría nulos).
        assertNull(DetailCacheCodec.decodeRange("""{"joints": []}"""))
        assertNull(DetailCacheCodec.decodeRange("""{"difficultyName": "Media", "joints": [{"minAngle": 3.0}]}"""))
        assertNull(DetailCacheCodec.decodeOutcomes("{"))
        assertNull(DetailCacheCodec.decodeOutcomes("""[{"resultId": 1, "exerciseId": 2, "joints": []}]"""))
        assertNull(DetailCacheCodec.decodeOutcomes("""[{"resultId": 1, "exerciseId": 2, "status": "OTRO", "joints": []}]"""))
    }
}

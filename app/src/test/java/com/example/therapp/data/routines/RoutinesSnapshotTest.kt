package com.example.therapp.data.routines

import com.example.therapp.data.routines.remote.payload.mapper.toSnapshot
import com.example.therapp.data.routines.remote.payload.res.ScheduledTrainingRes
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GET /scheduled_training con la forma del backend (RoutineSerializer y
 * compañía), incluidos los nulos que permite el modelo.
 */
class RoutinesSnapshotTest {

    private fun parse(json: String): List<ScheduledTrainingRes> =
        Gson().fromJson(json, Array<ScheduledTrainingRes>::class.java).toList()

    private val full = """
        [{
          "id": 7,
          "start_time": "2026-10-20T08:00:00-05:00",
          "status": "NOT_STARTED",
          "routine": {
            "id": 3, "therapy_id": 2, "name": "Rodilla", "description": "Fase 1",
            "is_model": false, "is_active": true,
            "therapy": {"id": 2, "name": "Rehabilitación", "description": "d",
                        "is_model": false, "is_active": true, "routines": ["http://x/routines/3/"]},
            "exercises": [{
              "id": 11, "name": "Flexión", "description": null, "order": 1,
              "routine": "http://x/routines/3/", "routine_id": 3, "status": "Sin video",
              "video": null, "is_model": false, "is_active": true,
              "difficulties": [{
                "id": 5, "exercise_id": 11, "name": "Fácil", "description": "",
                "ranges": [
                  {"point_tracked_id": 9, "point_tracked": null, "difficulty_id": 5,
                   "max_angle": 120.0, "min_angle": 30.0},
                  {"point_tracked_id": null, "point_tracked": null, "difficulty_id": null,
                   "max_angle": null, "min_angle": null}
                ]
              }]
            }]
          }
        }]
    """.trimIndent()

    @Test
    fun readsTheBackendFieldNames() {
        val snapshot = parse(full).toSnapshot()
        val routine = snapshot.routines.single()
        assertEquals(2, routine.therapyId)
        assertEquals(false, routine.isModeled)
        assertEquals(true, routine.isActive)
        assertEquals("Rehabilitación", snapshot.therapies.single().name)
        assertEquals(7, snapshot.trainings.single().id)
        assertEquals(3, snapshot.trainings.single().routineId)
    }

    @Test
    fun nullFieldsGetDefaults() {
        val snapshot = parse(full).toSnapshot()
        val exercise = snapshot.exercises.single()
        assertEquals("", exercise.description)
        assertNull(exercise.video)
        assertEquals(3, exercise.routineId)
        assertEquals(11, snapshot.difficulties.single().exerciseId)

        val ranges = snapshot.ranges
        assertEquals(2, ranges.size)
        assertEquals(120.0, ranges[0].maxAngle!!, 0.0)
        // Rango sin ángulos ni difficulty_id: queda colgado de su dificultad.
        assertNull(ranges[1].maxAngle)
        assertEquals(5, ranges[1].difficultyId)
    }

    @Test
    fun skipsWhatHasNoIdAndToleratesMissingTherapy() {
        val json = """
            [
              {"id": 1, "routine": null, "start_time": null, "status": null},
              {"id": null, "routine": {"id": 4}},
              {"id": 2, "routine": {"id": 4, "therapy": null, "exercises": null,
                                    "name": null, "description": null}},
              {"id": 3, "routine": {"id": 4, "exercises": [{"id": null, "name": "x"}]}}
            ]
        """.trimIndent()
        val snapshot = parse(json).toSnapshot()
        assertEquals(listOf(2, 3), snapshot.trainings.map { it.id })
        assertEquals("", snapshot.trainings.first().startTime)
        val routine = snapshot.routines.single()
        assertNull(routine.therapyId)
        assertEquals("", routine.name)
        assertTrue(snapshot.therapies.isEmpty())
        assertTrue(snapshot.exercises.isEmpty())
    }

    @Test
    fun aRoutineScheduledTwiceIsStoredOnce() {
        val twice = full.trim().removePrefix("[").removeSuffix("]")
        val json = "[$twice, ${twice.replaceFirst("\"id\": 7", "\"id\": 8")}]"
        val snapshot = parse(json).toSnapshot()
        assertEquals(listOf(7, 8), snapshot.trainings.map { it.id })
        assertEquals(1, snapshot.routines.size)
        assertEquals(1, snapshot.exercises.size)
        assertEquals(2, snapshot.ranges.size)
    }
}

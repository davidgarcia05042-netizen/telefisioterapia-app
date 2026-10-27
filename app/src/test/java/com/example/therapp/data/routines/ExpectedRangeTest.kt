package com.example.therapp.data.routines

import com.example.therapp.data.routines.remote.payload.mapper.toExpectedRange
import com.example.therapp.data.routines.remote.payload.res.ScheduledDiffRes
import com.example.therapp.domain.model.formatRange
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** GET /scheduled_training/diffs con la forma del backend (ScheduledTrainingDiffSerializer). */
class ExpectedRangeTest {

    private fun parse(json: String) = Gson().fromJson(json, ScheduledDiffRes::class.java)

    @Test
    fun assignedDifficultyWithJointNames() {
        val range = parse(
            """
            {"difficulty": {"id": 5, "exercise_id": 11, "name": "Fácil", "description": "",
              "ranges": [
                {"point_tracked_id": 9, "difficulty_id": 5, "max_angle": 120.0, "min_angle": 30.0,
                 "point_tracked": {"skeleton_point": {"id": 25, "codename": "LEFT_KNEE",
                   "verbose": "Rodilla izquierda", "left_point": 23, "right_point": 27},
                   "skeleton_point_id": 25, "exercise_id": 11}},
                {"point_tracked_id": null, "point_tracked": null, "difficulty_id": 5,
                 "max_angle": null, "min_angle": null}
              ]},
             "exercise": {"id": 11}, "scheduled_training": {"id": 7}, "scheduled_training_id": 7}
            """.trimIndent()
        ).toExpectedRange()!!
        assertEquals("Fácil", range.difficultyName)
        assertEquals("Rodilla izquierda", range.joints[0].name)
        assertEquals(30.0, range.joints[0].minAngle!!, 0.0)
        // Sin punto rastreado ni ángulos: nombre genérico, sin romper.
        assertEquals("Articulación", range.joints[1].name)
        assertNull(range.joints[1].maxAngle)
    }

    @Test
    fun withoutDifficultyIsNull() {
        assertNull(parse("""{"difficulty": null}""").toExpectedRange())
        assertNull(parse("{}").toExpectedRange())
    }

    @Test
    fun rangeText() {
        assertEquals("30° a 120°", formatRange(30.0, 120.0))
        assertEquals("35° a 90°", formatRange(34.6, 90.2))
        assertEquals("desde 10°", formatRange(10.0, null))
        assertEquals("hasta 80°", formatRange(null, 80.0))
        assertEquals("sin rango definido", formatRange(null, null))
    }
}

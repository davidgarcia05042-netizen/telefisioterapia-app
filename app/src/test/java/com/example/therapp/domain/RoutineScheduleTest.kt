package com.example.therapp.domain

import com.example.therapp.domain.model.availableFrom
import com.example.therapp.domain.model.availableFromNotice
import com.example.therapp.domain.model.scheduledDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** "Hacer ejercicio" solo hoy o en vencidas, con el día en la zona del teléfono. */
class RoutineScheduleTest {

    private val bogota = ZoneId.of("America/Bogota")
    private val today = LocalDate.of(2026, 10, 19)

    @Test
    fun theDayIsTakenInThePhoneZone() {
        // El mismo instante escrito en UTC o con la hora de Colombia (como lo manda el backend).
        assertEquals(LocalDate.of(2026, 10, 19), scheduledDate("2026-10-20T01:00:00Z", bogota))
        assertEquals(LocalDate.of(2026, 10, 19), scheduledDate("2026-10-19T08:00:00-05:00", bogota))
        assertNull(scheduledDate("no es una fecha", bogota))
    }

    @Test
    fun todayAndOverdueCanBeDone() {
        assertNull(availableFrom("2026-10-19T13:00:00Z", today, bogota))
        assertNull(availableFrom("2026-10-20T01:00:00Z", today, bogota))
        assertNull(availableFrom("2026-10-02T13:00:00Z", today, bogota))
    }

    @Test
    fun futureRoutinesSayFromWhen() {
        assertEquals(LocalDate.of(2026, 10, 20), availableFrom("2026-10-20T13:00:00Z", today, bogota))
        assertEquals(LocalDate.of(2026, 11, 3), availableFrom("2026-11-03T15:30:00Z", today, bogota))
    }

    @Test
    fun anUnreadableDateDoesNotBlockThePatient() {
        assertNull(availableFrom("", today, bogota))
    }

    @Test
    fun notice() {
        assertEquals("Disponible desde el martes 20 de octubre.", availableFromNotice(LocalDate.of(2026, 10, 20), today))
        assertEquals(
            "Disponible desde el lunes 4 de enero de 2027.",
            availableFromNotice(LocalDate.of(2027, 1, 4), today)
        )
    }
}

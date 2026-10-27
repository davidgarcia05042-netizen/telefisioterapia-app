package com.example.therapp.domain.model

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Día de una rutina programada en la zona del teléfono, o null si la fecha no
 * se puede leer. El backend manda la hora de Colombia ("...T20:30:00-05:00"):
 * en un teléfono en Colombia da el mismo día que el texto; en otra zona, el
 * día local. La lista de rutinas y "Disponible desde" usan este mismo cálculo.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun scheduledDate(startTime: String, zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
    runCatching { OffsetDateTime.parse(startTime).atZoneSameInstant(zone).toLocalDate() }.getOrNull()

/**
 * "Hacer ejercicio" solo en las rutinas de hoy o vencidas (que no estén
 * terminadas lo revisa la pantalla). Devuelve null si ya se puede hacer o,
 * si es futura, el día desde el que se puede. Una fecha que no se puede leer
 * no bloquea al paciente.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun availableFrom(startTime: String, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
    scheduledDate(startTime, zone)?.takeIf { it.isAfter(today) }

/** "Disponible desde el martes 21 de octubre."; con el año si no es el de hoy. */
@RequiresApi(Build.VERSION_CODES.O)
fun availableFromNotice(day: LocalDate, today: LocalDate): String {
    val pattern = if (day.year == today.year) "EEEE d 'de' MMMM" else "EEEE d 'de' MMMM 'de' yyyy"
    return "Disponible desde el ${day.format(DateTimeFormatter.ofPattern(pattern, Locale("es", "CO")))}."
}

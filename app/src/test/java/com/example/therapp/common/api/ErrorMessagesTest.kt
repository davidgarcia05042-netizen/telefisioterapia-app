package com.example.therapp.common.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cuerpos de error con la forma que devuelve el backend (DRF). */
class ErrorMessagesTest {

    @Test
    fun detailComesFirst() {
        assertEquals(
            "La contraseña actual no es correcta.",
            ErrorMessages.fromBody("""{"detail": "La contraseña actual no es correcta."}""")
        )
    }

    @Test
    fun nonFieldErrorsFromTheLoginSerializer() {
        assertEquals(
            "The credentials are not valid",
            ErrorMessages.fromBody("""{"non_field_errors": ["The credentials are not valid"]}""")
        )
    }

    @Test
    fun firstFieldError() {
        assertEquals(
            "Ensure this field has at least 8 characters.",
            ErrorMessages.fromBody("""{"password": ["Ensure this field has at least 8 characters."]}""")
        )
    }

    @Test
    fun emptyOrNonJsonBodiesGiveNull() {
        assertNull(ErrorMessages.fromBody(null))
        assertNull(ErrorMessages.fromBody(""))
        assertNull(ErrorMessages.fromBody("<html>Bad Gateway</html>"))
        assertNull(ErrorMessages.fromBody("{}"))
        assertNull(ErrorMessages.fromBody("""{"detail": ""}"""))
    }

    // M5: Render respondió 502 con su página HTML mientras el backend se reiniciaba.
    @Test
    fun serverDownIsNotAnUnexpectedError() {
        assertEquals(ErrorMessages.SERVER_UNAVAILABLE, ErrorMessages.fromResponse(502, "<html>Bad Gateway</html>"))
        assertEquals(ErrorMessages.SERVER_UNAVAILABLE, ErrorMessages.fromResponse(503, null))
        // Con mensaje propio del backend, vale ese.
        assertEquals("Sin servicio.", ErrorMessages.fromResponse(503, """{"detail": "Sin servicio."}"""))
        assertEquals(ErrorMessages.UNEXPECTED, ErrorMessages.fromResponse(400, "{}"))
    }

    @Test
    fun transientFailures() {
        assertTrue(ErrorMessages.isTransient(502))
        assertTrue(ErrorMessages.isTransient(500))
        assertTrue(ErrorMessages.isTransient(ErrorMessages.CODE_NO_CONNECTION))
        assertTrue(ErrorMessages.isTransient(ErrorMessages.CODE_TIMEOUT))
        assertFalse(ErrorMessages.isTransient(404))
        assertFalse(ErrorMessages.isTransient(401))
        assertFalse(ErrorMessages.isTransient(ErrorMessages.CODE_UNEXPECTED))
    }
}

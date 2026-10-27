package com.example.therapp.security

import com.example.therapp.data.auth.remote.payload.res.SignInRes
import com.google.gson.Gson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Respuestas de POST /auth/users/login/ con la forma del backend. */
class PatientAccessTest {

    private fun userWithGroups(groups: String) = Gson().fromJson(
        """{"access_token": "t", "user": {"id": 1, "username": "u", "groups": $groups}}""",
        SignInRes::class.java
    ).user

    @Test
    fun patientCanEnter() {
        assertTrue(PatientAccess.allows(userWithGroups("""[{"name": "Patient", "permissions": []}]""")))
    }

    @Test
    fun therapistAndAdminCannot() {
        assertFalse(PatientAccess.allows(userWithGroups("""[{"name": "Therapist", "permissions": []}]""")))
        assertFalse(PatientAccess.allows(userWithGroups("""[{"name": "Admin", "permissions": []}]""")))
    }

    @Test
    fun withoutGroupsCannot() {
        assertFalse(PatientAccess.allows(userWithGroups("[]")))
        assertFalse(PatientAccess.allows(userWithGroups("null")))
        assertFalse(PatientAccess.allows(null))
    }
}

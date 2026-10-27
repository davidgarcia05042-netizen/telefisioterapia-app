package com.example.therapp.security

import com.example.therapp.data.auth.remote.payload.res.UserRes

/**
 * La app es solo para pacientes (DM5): los terapeutas y administradores usan
 * el panel. El backend deja iniciar sesión a cualquier usuario, así que la
 * app revisa el grupo en la respuesta del login.
 */
object PatientAccess {
    const val PATIENT_GROUP = "Patient"
    const val ONLY_PATIENTS =
        "Esta aplicación es para pacientes. Si eres terapeuta o administrador, usa el panel web."

    fun allows(user: UserRes?): Boolean =
        user?.groups.orEmpty().any { it.name == PATIENT_GROUP }
}

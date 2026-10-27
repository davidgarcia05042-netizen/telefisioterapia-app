package com.example.therapp.security

import com.example.therapp.data.routines.local.DetailCacheDao
import com.example.therapp.data.routines.local.RoutineDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lo que necesita el interceptor de la sesión (permite probarlo sin DataStore).
 */
interface Session {
    suspend fun accessToken(): String?

    /** El backend rechazó [token] con 401: la sesión venció o la revocaron. */
    suspend fun expire(token: String)
}

/**
 * Único lugar donde vive la sesión del paciente: el token, el usuario y la
 * copia local de sus rutinas.
 *
 * Antes el token se guardaba con la clave "access_token" pero el interceptor
 * lo leía de "jwt_token" (TokenManager), así que cada petición salía sin token,
 * recibía 401 y AuthAuthenticator la repetía con el bueno. Ahora se lee y se
 * escribe solo aquí.
 */
@Singleton
class SessionManager @Inject constructor(
    private val storage: AsyncStorage,
    private val routineDao: RoutineDao,
    private val detailCacheDao: DetailCacheDao,
) : Session {

    private val _notice = MutableStateFlow<String?>(null)

    /** Aviso para la pantalla de login (por ejemplo, que la sesión expiró). */
    val notice: StateFlow<String?> = _notice.asStateFlow()

    override suspend fun accessToken(): String? =
        storage.getItem(ACCESS_TOKEN)?.takeIf { it.isNotEmpty() }

    fun isSignedIn(): Flow<Boolean> =
        storage.observeItem(ACCESS_TOKEN).map { !it.isNullOrEmpty() }

    fun user(): Flow<String?> = storage.observeItem(USER)

    /** El usuario va primero: al guardar el token la app pasa al inicio y ya lo encuentra. */
    suspend fun start(token: String, userJson: String) {
        _notice.value = null
        storage.setItem(USER, userJson)
        storage.setItem(ACCESS_TOKEN, token)
    }

    /**
     * Borra la sesión y la copia local de las rutinas (si otro paciente entra
     * en el mismo teléfono, no debe ver las del anterior). Al desaparecer el
     * token, la navegación vuelve al login.
     */
    suspend fun clear() {
        storage.removeItem(ACCESS_TOKEN)
        storage.removeItem(USER)
        routineDao.clearAll()
        detailCacheDao.clearAll()
    }

    override suspend fun expire(token: String) {
        // Una respuesta atrasada de una sesión anterior no cierra la actual.
        if (accessToken() != token) return
        _notice.value = SESSION_EXPIRED
        clear()
    }

    fun dismissNotice() {
        _notice.value = null
    }

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val USER = "user"
        const val SESSION_EXPIRED = "Tu sesión expiró. Inicia sesión de nuevo."
    }
}

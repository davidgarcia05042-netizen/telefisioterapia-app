package com.example.therapp.data.auth

import com.example.therapp.common.api.ErrorMessages
import com.example.therapp.common.api.apiRequestFlow
import com.example.therapp.data.auth.remote.AuthApi
import com.example.therapp.data.auth.remote.payload.req.ChangePasswordReq
import com.example.therapp.data.auth.remote.payload.req.SignInReq
import com.example.therapp.data.auth.remote.payload.res.SignInRes
import com.example.therapp.data.auth.remote.payload.res.UserRes
import com.example.therapp.di.modules.ApplicationScope
import com.example.therapp.domain.repository.AuthRepository
import com.example.therapp.security.PatientAccess
import com.example.therapp.security.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/15/2025
 * @version 1.0
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val session: SessionManager,
    private val api: AuthApi,
    private val gson: Gson,
    @ApplicationScope private val appScope: CoroutineScope
) : AuthRepository {
    override fun signIn(req: SignInReq) = apiRequestFlow {
        val response = api.signIn(req)
        val body = response.body()
        if (!response.isSuccessful || body == null) return@apiRequestFlow response
        val token = body.accessToken
        when {
            // Un terapeuta o administrador: no se guarda la sesión. Tampoco se
            // revoca el token, porque es el mismo de su sesión en el panel.
            !PatientAccess.allows(body.user) -> rejected(403, PatientAccess.ONLY_PATIENTS)
            token.isNullOrEmpty() -> rejected(500, ErrorMessages.UNEXPECTED)
            else -> {
                session.start(token, gson.toJson(body.user))
                response
            }
        }
    }

    /** Respuesta de error con el mismo formato del backend ({"detail": …}). */
    private fun rejected(code: Int, message: String): Response<SignInRes> =
        Response.error(
            code,
            gson.toJson(mapOf("detail" to message)).toResponseBody("application/json".toMediaType())
        )

    /**
     * Borra la sesión local de inmediato (la app vuelve al login sin esperar
     * al servidor) y revoca el token en el backend con POST
     * /auth/users/logout/. Antes solo borraba el token del teléfono y seguía
     * vigente en el servidor. Si no hay red, el token vence solo a las 12 h.
     */
    override suspend fun signOut() {
        val token = session.accessToken()
        session.clear()
        if (token == null) return
        appScope.launch {
            try {
                api.logout("Token $token")
            } catch (e: Exception) {
                // Sin red o servidor dormido: la sesión local ya se borró.
            }
        }
    }

    override fun isSignedIn(): Flow<Boolean> = session.isSignedIn()

    // El token sigue vigente después del cambio: la sesión no se cierra.
    override fun changePassword(req: ChangePasswordReq) = apiRequestFlow {
        api.changePassword(req)
    }

    override fun getUser(): Flow<UserRes?> {
        return session.user().map { userJson ->
            if (userJson.isNullOrEmpty()) {
                null
            } else {
                try {
                    gson.fromJson(userJson, UserRes::class.java)
                } catch (e: Exception) {
                    // JSON malformado: sin usuario. Sin log: el JSON trae datos personales.
                    null
                }
            }
        }
    }
}

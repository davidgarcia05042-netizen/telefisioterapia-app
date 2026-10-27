package com.example.therapp.security

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Agrega "Authorization: Token <token>" si hay sesión y, si el backend
 * responde 401, cierra la sesión: la app vuelve al login con "Tu sesión
 * expiró". El token vence a las 12 h (ExpiringTokenAuthentication).
 *
 * Reemplaza a AuthAuthenticator, que ante un 401 repetía la petición con el
 * mismo token hasta que OkHttp cortaba. Nada de esto escribe el token en el log.
 */
class AuthInterceptor
@Inject constructor(
    private val session: Session
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = runBlocking { session.accessToken() }
        if (token == null || request.header(AUTHORIZATION) != null) {
            return chain.proceed(request)
        }

        val response = chain.proceed(
            request.newBuilder().header(AUTHORIZATION, "Token $token").build()
        )
        // Al cerrar sesión a mano, un token ya vencido no es "tu sesión expiró".
        if (response.code == 401 && !request.url.encodedPath.endsWith(LOGOUT_PATH)) {
            runBlocking { session.expire(token) }
        }
        return response
    }

    companion object {
        private const val AUTHORIZATION = "Authorization"
        const val LOGOUT_PATH = "/auth/users/logout/"
    }
}

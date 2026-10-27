package com.example.therapp.security

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** El backend se simula con MockWebServer: nunca se llama al real. */
class AuthInterceptorTest {

    private class FakeSession(var token: String?) : Session {
        val expired = mutableListOf<String>()
        override suspend fun accessToken() = token
        override suspend fun expire(token: String) {
            expired += token
        }
    }

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun call(session: Session, path: String, code: Int): Int {
        server.enqueue(MockResponse().setResponseCode(code))
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(session)).build()
        client.newCall(Request.Builder().url(server.url(path)).build()).execute().use { return it.code }
    }

    @Test
    fun withoutSessionSendsNoToken() {
        call(FakeSession(null), "/scheduled_training", 200)
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun withSessionSendsTheStoredToken() {
        call(FakeSession("abc123"), "/scheduled_training", 200)
        assertEquals("Token abc123", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun unauthorizedExpiresTheSessionOnceWithoutRetrying() {
        val session = FakeSession("abc123")
        assertEquals(401, call(session, "/scheduled_training", 401))
        assertEquals(listOf("abc123"), session.expired)
        // Sin reintentos: el original repetía la petición con el mismo token.
        assertEquals(1, server.requestCount)
    }

    @Test
    fun successDoesNotExpireTheSession() {
        val session = FakeSession("abc123")
        call(session, "/scheduled_training", 200)
        assertEquals(emptyList<String>(), session.expired)
    }

    @Test
    fun forbiddenDoesNotExpireTheSession() {
        val session = FakeSession("abc123")
        call(session, "/scheduled_training", 403)
        assertEquals(emptyList<String>(), session.expired)
    }

    @Test
    fun explicitAuthorizationIsKept() {
        // Al cerrar sesión la sesión local ya se borró y el logout lleva su token.
        server.enqueue(MockResponse().setResponseCode(204))
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(FakeSession(null))).build()
        val request = Request.Builder()
            .url(server.url("/auth/users/logout/"))
            .header("Authorization", "Token old")
            .post(ByteArray(0).toRequestBody())
            .build()
        client.newCall(request).execute().close()
        assertEquals("Token old", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun unauthorizedOnLogoutIsNotSessionExpired() {
        val session = FakeSession("abc123")
        call(session, "/auth/users/logout/", 401)
        assertEquals(emptyList<String>(), session.expired)
    }
}

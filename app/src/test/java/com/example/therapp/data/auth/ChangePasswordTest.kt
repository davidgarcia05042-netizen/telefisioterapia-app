package com.example.therapp.data.auth

import com.example.therapp.common.api.ApiResponse
import com.example.therapp.common.api.apiRequestFlow
import com.example.therapp.data.auth.remote.AuthApi
import com.example.therapp.data.auth.remote.payload.req.ChangePasswordReq
import com.example.therapp.ui.presenter.change_password.ChangePasswordViewModel
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** POST /auth/users/change_password/ contra un backend simulado (MockWebServer). */
class ChangePasswordTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AuthApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun change(): ApiResponse<Unit> = runBlocking {
        apiRequestFlow { api.changePassword(ChangePasswordReq("Temporal-1234", "MiClaveNueva-77")) }.last()
    }

    @Test
    fun sendsCurrentAndNewWithTheBackendNames() {
        server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))
        assertTrue(change() is ApiResponse.Success)
        val request = server.takeRequest()
        assertEquals("/auth/users/change_password/", request.path)
        assertEquals(
            """{"password":"Temporal-1234","new_password":"MiClaveNueva-77"}""",
            request.body.readUtf8()
        )
    }

    @Test
    fun showsTheBackendReason() {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"detail": "La contraseña actual no es correcta."}""")
        )
        val result = change() as ApiResponse.Failure
        assertEquals("La contraseña actual no es correcta.", result.errorMessage)
        assertEquals(400, result.code)
    }

    @Test
    fun localValidation() {
        assertEquals("Completa los tres campos.", ChangePasswordViewModel.validate("", "a", "a"))
        assertEquals(
            "La contraseña nueva y su confirmación no coinciden.",
            ChangePasswordViewModel.validate("actual", "nueva-1", "nueva-2")
        )
        assertEquals(
            "La contraseña nueva debe ser distinta de la actual.",
            ChangePasswordViewModel.validate("igual", "igual", "igual")
        )
        assertNull(ChangePasswordViewModel.validate("actual", "nueva", "nueva"))
    }
}

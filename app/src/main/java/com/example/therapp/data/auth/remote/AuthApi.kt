package com.example.therapp.data.auth.remote

import com.example.therapp.data.auth.remote.payload.req.ChangePasswordReq
import com.example.therapp.data.auth.remote.payload.req.SignInReq
import com.example.therapp.data.auth.remote.payload.res.SignInRes
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/15/2025
 * @version 1.0
 */
interface AuthApi {
    @POST("/auth/users/login/")
    suspend fun signIn(@Body signInReq: SignInReq): Response<SignInRes>

    /** Revoca el token en el backend (204). El encabezado va explícito: la sesión local ya se borró. */
    @POST("/auth/users/logout/")
    suspend fun logout(@Header("Authorization") authorization: String): Response<Unit>

    /** 201 con {} si cambió; 400 con {"detail": "..."} en español si no. */
    @POST("/auth/users/change_password/")
    suspend fun changePassword(@Body req: ChangePasswordReq): Response<Unit>
}
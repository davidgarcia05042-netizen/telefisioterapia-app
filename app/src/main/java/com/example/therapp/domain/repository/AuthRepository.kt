package com.example.therapp.domain.repository

import com.example.therapp.common.api.ApiResponse
import com.example.therapp.data.auth.remote.payload.req.ChangePasswordReq
import com.example.therapp.data.auth.remote.payload.req.SignInReq
import com.example.therapp.data.auth.remote.payload.res.SignInRes
import com.example.therapp.data.auth.remote.payload.res.UserRes
import kotlinx.coroutines.flow.Flow

/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 7/29/2025
 * @version 1.0
 */
interface AuthRepository {
    fun signIn(req: SignInReq): Flow<ApiResponse<SignInRes>>
    suspend fun signOut()
    fun isSignedIn(): Flow<Boolean>
    fun getUser() : Flow<UserRes?>
    fun changePassword(req: ChangePasswordReq): Flow<ApiResponse<Unit>>

}
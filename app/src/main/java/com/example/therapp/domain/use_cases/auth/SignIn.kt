package com.example.therapp.domain.use_cases.auth

import com.example.therapp.data.auth.remote.payload.req.SignInReq
import com.example.therapp.domain.repository.AuthRepository


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/31/2025
 * @version 1.0
 */
class SignIn(
    private val repository: AuthRepository
) {
    operator fun invoke(username: String, password: String) =
        repository.signIn(SignInReq(username, password))
}
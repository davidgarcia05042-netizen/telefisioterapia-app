package com.example.therapp.domain.use_cases.auth

import com.example.therapp.data.auth.remote.payload.req.ChangePasswordReq
import com.example.therapp.domain.repository.AuthRepository

class ChangePassword(
    private val repository: AuthRepository
) {
    operator fun invoke(current: String, new: String) =
        repository.changePassword(ChangePasswordReq(current, new))
}

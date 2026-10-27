package com.example.therapp.domain.use_cases.auth

import com.example.therapp.domain.repository.AuthRepository

class SignOut(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.signOut()
    }
}

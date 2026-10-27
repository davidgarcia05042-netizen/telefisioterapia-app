package com.example.therapp.domain.use_cases.auth

import com.example.therapp.domain.repository.AuthRepository

class IsSignedIn(
    private val repository: AuthRepository
) {
    operator fun invoke() =
        repository.isSignedIn()
}
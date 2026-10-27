package com.example.therapp.ui.presenter.sign_in

data class SignInState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,

    val isError: Boolean = false,
    val errorMessage: String = "",

    val usernameError: Boolean = false,
    val passwordError: Boolean = false
)

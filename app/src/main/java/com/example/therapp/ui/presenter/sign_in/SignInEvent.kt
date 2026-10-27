package com.example.therapp.ui.presenter.sign_in

sealed class SignInEvent {
    data class UsernameChanged(val username: String) : SignInEvent()
    data class PasswordChanged(val password: String) : SignInEvent()
    object LoginButtonClicked : SignInEvent()
    object ForgotPasswordButtonClicked : SignInEvent()
    object ErrorHandled : SignInEvent()
}

package com.example.therapp.ui.presenter.sign_in

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.therapp.common.api.ApiResponse
import com.example.therapp.domain.use_cases.auth.AuthUseCases
import com.example.therapp.security.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/31/2025
 * @version 1.0
 */
@HiltViewModel
class SignInViewModel
@Inject constructor(
    private val authUseCases: AuthUseCases,
    private val session: SessionManager
) : ViewModel() {

    val signIn = authUseCases.signIn
    val isSignedIn = authUseCases.isSignedIn

    var state by mutableStateOf(SignInState())

    /** "Tu sesión expiró…" cuando el backend respondió 401. */
    val notice: StateFlow<String?> = session.notice

    init {
        viewModelScope.launch {
            isSignedIn().collect { signedIn ->
                state = state.copy(isSignedIn = signedIn)
            }
        }
    }


    fun onEvent(event: SignInEvent) {
        when (event) {
            is SignInEvent.UsernameChanged -> {
                state = state.copy(username = event.username, isError = false, errorMessage = "")
            }

            is SignInEvent.PasswordChanged -> {
                state = state.copy(password = event.password, isError = false, errorMessage = "")
            }

            SignInEvent.ForgotPasswordButtonClicked -> {

            }

            SignInEvent.LoginButtonClicked -> {
                signIn()
            }

            SignInEvent.ErrorHandled -> {
                state = state.copy(errorMessage = "", isError = false)
            }
        }
    }

    private fun signIn() {
        val username = state.username.trim()
        if (username.isEmpty() || state.password.isEmpty()) {
            state = state.copy(isError = true, errorMessage = EMPTY_FIELDS)
            return
        }
        viewModelScope.launch {
            signIn(username, state.password).collect {
                when (it) {
                    is ApiResponse.Loading -> {
                        state = state.copy(isLoading = true)
                    }

                    is ApiResponse.Error -> {
                        state = state.copy(isLoading = false, isError = true, errorMessage = it.errorMessage)
                    }

                    is ApiResponse.Failure -> {
                        // 400 = credenciales inválidas (o una contraseña de menos
                        // de 8 caracteres, que el backend rechaza igual).
                        val message = if (it.code == 400) INVALID_CREDENTIALS else it.errorMessage
                        state = state.copy(isLoading = false, isError = true, errorMessage = message)
                    }

                    is ApiResponse.Success -> {
                        // La contraseña no se queda en memoria después de entrar.
                        state = state.copy(isLoading = false, isError = false, password = "")
                    }
                }
            }
        }
    }

    companion object {
        const val EMPTY_FIELDS = "Escribe tu usuario y tu contraseña."
        const val INVALID_CREDENTIALS = "Usuario o contraseña incorrectos."
    }
}

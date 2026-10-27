package com.example.therapp.ui.presenter.change_password

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.therapp.common.api.ApiResponse
import com.example.therapp.domain.use_cases.auth.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordState(
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : ViewModel() {

    var state by mutableStateOf(ChangePasswordState())
        private set

    fun onCurrentChanged(value: String) {
        state = state.copy(current = value, errorMessage = null, successMessage = null)
    }

    fun onNewChanged(value: String) {
        state = state.copy(new = value, errorMessage = null, successMessage = null)
    }

    fun onConfirmChanged(value: String) {
        state = state.copy(confirm = value, errorMessage = null, successMessage = null)
    }

    fun submit() {
        val problem = validate(state.current, state.new, state.confirm)
        if (problem != null) {
            state = state.copy(errorMessage = problem)
            return
        }
        viewModelScope.launch {
            authUseCases.changePassword(state.current, state.new).collect {
                state = when (it) {
                    is ApiResponse.Loading -> state.copy(isLoading = true, errorMessage = null)
                    // Las contraseñas no se quedan en el estado después de cambiarlas.
                    is ApiResponse.Success -> ChangePasswordState(successMessage = SUCCESS)
                    // El backend explica el motivo en español (actual incorrecta,
                    // nueva demasiado corta, común, solo números…).
                    is ApiResponse.Failure -> state.copy(isLoading = false, errorMessage = it.errorMessage)
                    is ApiResponse.Error -> state.copy(isLoading = false, errorMessage = it.errorMessage)
                }
            }
        }
    }

    companion object {
        const val SUCCESS = "Tu contraseña se cambió. La próxima vez entra con la nueva."

        /** Lo que se puede revisar sin el servidor; el resto lo valida el backend. */
        fun validate(current: String, new: String, confirm: String): String? = when {
            current.isEmpty() || new.isEmpty() || confirm.isEmpty() -> "Completa los tres campos."
            new != confirm -> "La contraseña nueva y su confirmación no coinciden."
            new == current -> "La contraseña nueva debe ser distinta de la actual."
            else -> null
        }
    }
}

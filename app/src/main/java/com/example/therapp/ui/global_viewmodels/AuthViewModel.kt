package com.example.therapp.ui.global_viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.therapp.common.UserAuthState
import com.example.therapp.domain.use_cases.auth.AuthUseCases
import com.example.therapp.ui.presenter.splash.SplashEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthUseCases
) : ViewModel() {
    private val _isAuthenticated = MutableStateFlow<UserAuthState>(UserAuthState.UNKNOWN)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    private var authJob: Job? = null
    fun onEvent(event: SplashEvent) {
        when (event) {
            is SplashEvent.CheckAuthentication -> {
                // Se llama en cada cambio de estado: sin cancelar, se acumulaban
                // recolectores del mismo flujo.
                authJob?.cancel()
                authJob = viewModelScope.launch(Dispatchers.IO) {
                    val result = authUseCases.isSignedIn()

                    withContext(Dispatchers.Main) {
                        result.collect {
                            if (it) {
                                _isAuthenticated.emit(UserAuthState.AUTHENTICATED)
                            } else {
                                _isAuthenticated.emit(UserAuthState.UNAUTHENTICATED)
                            }
                        }
                    }

                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        authJob?.let {
            if (it.isActive) {
                it.cancel()
            }
        }
    }
}
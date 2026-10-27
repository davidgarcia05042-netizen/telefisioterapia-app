package com.example.therapp.ui.presenter.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.therapp.data.auth.remote.payload.res.UserRes
import com.example.therapp.domain.repository.AuthRepository
import com.example.therapp.domain.use_cases.auth.AuthUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 9/19/2025
 * @version 1.0
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authUseCases: AuthUseCases,
    private val authRepository: AuthRepository

) : ViewModel() {

    val user: StateFlow<UserRes?> = authRepository.getUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null // El valor inicial es null mientras se carga
        )


    fun signOut() {
        viewModelScope.launch {
            authUseCases.signOut()
        }
    }

}
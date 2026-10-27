package com.example.therapp.di.modules

import com.example.therapp.domain.repository.AuthRepository
import com.example.therapp.domain.repository.RoutinesRepository
import com.example.therapp.domain.use_cases.auth.AuthUseCases
import com.example.therapp.domain.use_cases.auth.ChangePassword
import com.example.therapp.domain.use_cases.auth.IsSignedIn
import com.example.therapp.domain.use_cases.auth.SignIn
import com.example.therapp.domain.use_cases.auth.SignOut
import com.example.therapp.domain.use_cases.routines.GetScheduledTraining
import com.example.therapp.domain.use_cases.routines.RefreshScheduledTraining
import com.example.therapp.domain.use_cases.routines.RoutinesUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCasesModule {
    @Provides
    @Singleton
    fun provideAuthUseCases(authRepository: AuthRepository): AuthUseCases =
        AuthUseCases(
            signIn = SignIn(authRepository),
            isSignedIn = IsSignedIn(authRepository),
            signOut = SignOut(authRepository),
            changePassword = ChangePassword(authRepository)
        )

    @Provides
    @Singleton
    fun provideRoutinesUseCases(routinesRepository: RoutinesRepository): RoutinesUseCases =
        RoutinesUseCases(
            getScheduledTraining = GetScheduledTraining(routinesRepository),
            refreshScheduledTraining = RefreshScheduledTraining(routinesRepository)
        )
}
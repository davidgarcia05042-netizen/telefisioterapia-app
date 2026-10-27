package com.example.therapp.di.modules

import com.example.therapp.data.auth.AuthRepositoryImpl
import com.example.therapp.data.routines.RoutinesRepositoryImpl
import com.example.therapp.domain.repository.AuthRepository
import com.example.therapp.domain.repository.RoutinesRepository
import com.example.therapp.security.Session
import com.example.therapp.security.SessionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

//    @Provides
//    @Singleton
//    fun provideAuthRepository(
//        asyncStorage: AsyncStorage,
//        authApi: AuthApi,
//        gson: Gson
//    ): AuthRepository = AuthRepositoryImpl(asyncStorage, authApi, gson)

    @Binds
    @Singleton
    abstract fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository


//    @Provides
//    @Singleton
//    fun provideRoutinesRepository(
//        routinesApi: RoutinesApi
//    ): RoutinesRepository = RoutinesRepositoryImpl(routinesApi)

    @Binds
    @Singleton
    abstract fun bindRoutinesRepository(routinesRepositoryImpl: RoutinesRepositoryImpl): RoutinesRepository

    @Binds
    @Singleton
    abstract fun bindSession(sessionManager: SessionManager): Session
}
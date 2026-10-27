package com.example.therapp.di.modules

import com.example.therapp.data.local.AppDatabase
import com.example.therapp.data.routines.local.DetailCacheDao
import com.example.therapp.data.routines.local.RoutineDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides
    @Singleton
    fun provideRoutineDao(database: AppDatabase): RoutineDao = database.routineDao

    @Provides
    @Singleton
    fun provideDetailCacheDao(database: AppDatabase): DetailCacheDao = database.detailCacheDao
}
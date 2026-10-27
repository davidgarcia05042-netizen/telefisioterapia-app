package com.example.therapp.di.modules

import android.app.Application
import androidx.room.Room
import com.example.therapp.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DbModule {
    @Provides
    @Singleton
    fun provideClientDb(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "app_db.db"
        )
            // Copia local de lo que manda el backend: sin migraciones, se
            // vuelve a descargar.
            .fallbackToDestructiveMigration()
            .build()
    }
}
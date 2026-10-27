package com.example.therapp.di.modules

import com.example.therapp.BuildConfig
import com.example.therapp.common.HOST_URL
import com.example.therapp.data.auth.remote.AuthApi
import com.example.therapp.data.exercise.UploadClient
import com.example.therapp.data.exercise.remote.SendApi
import com.example.therapp.data.results.remote.ResultsApi
import com.example.therapp.data.routines.remote.RoutinesApi
import com.example.therapp.security.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApiModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
    ): OkHttpClient {
        // Antes el nivel era BODY: el log mostraba la contraseña del login y
        // el token. BASIC solo deja método, URL, código y tamaño, y únicamente
        // en la versión de depuración.
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE

        // Un solo cliente para todo (el login usaba el de Retrofit por defecto,
        // con 10 s, que no alcanza para el arranque en frío de Render).
        return OkHttpClient
            .Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(HOST_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAuthAPI(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideRoutinesApi(retrofit: Retrofit): RoutinesApi =
        retrofit.create(RoutinesApi::class.java)

    @Provides
    @Singleton
    fun provideSendApi(retrofit: Retrofit): SendApi =
        retrofit.create(SendApi::class.java)

    @Provides
    @Singleton
    fun provideResultsApi(retrofit: Retrofit): ResultsApi =
        retrofit.create(ResultsApi::class.java)

    /**
     * Subida a Supabase: sin el interceptor (el token de la plataforma no va a
     * un tercero) y sin log (la URL firmada es una credencial). Tiempos largos
     * para un video de hasta 50 MB en una red móvil.
     */
    @Provides
    @Singleton
    @UploadClient
    fun provideUploadClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
}
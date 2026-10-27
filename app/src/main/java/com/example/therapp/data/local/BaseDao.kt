package com.example.therapp.data.local

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz genérica para operaciones básicas de base de datos (escritura).
 * @param T La clase de la entidad (Entity).
 */
interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<T>)

    @Update
    suspend fun update(entity: T)

    @Delete
    suspend fun delete(entity: T)
}

/**
 * Interfaz genérica para operaciones de lectura y caché.
 * @param T La clase de la entidad (Entity).
 * @param ID El tipo del identificador de la entidad.
 */
interface ReadableDao<T, ID> {
    suspend fun getAll(): List<T>
    suspend fun getById(id: ID): T?
    fun observe(): Flow<List<T>>
    suspend fun clearAll()
    suspend fun replaceAll(entities: List<T>)
}

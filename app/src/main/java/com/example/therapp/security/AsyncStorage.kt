package com.example.therapp.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/31/2025
 * @version 1.0
 */
class AsyncStorage @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun getItem(key: String): String? {
        val prefKey = stringPreferencesKey(key)
        return dataStore.data.first()[prefKey]
    }

    fun observeItem(key: String): Flow<String?> {
        val prefKey = stringPreferencesKey(key)
        return dataStore.data.map { it[prefKey] }
    }


    suspend fun setItem(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        dataStore.edit { it[prefKey] = value }
    }

    suspend fun removeItem(key: String) {
        val prefKey = stringPreferencesKey(key)
        dataStore.edit { it.remove(prefKey) }
    }
}
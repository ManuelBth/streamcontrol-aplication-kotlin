package com.example.streamcontrol.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.streamcontrol.domain.model.ConnectionConfig
import com.example.streamcontrol.domain.model.ControlConfig
import com.example.streamcontrol.domain.model.Controllers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stream_control_config")

class GlobalConfigStorage(
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private object PreferencesKeys {
        val CONTROL_CONFIG = stringPreferencesKey("control_config")
        val CONNECTION_CONFIG = stringPreferencesKey("connection_config")
    }

    val controlConfigFlow: Flow<ControlConfig> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CONTROL_CONFIG]?.let { jsonStr ->
            try {
                json.decodeFromString<ControlConfig>(jsonStr)
            } catch (e: Exception) {
                ControlConfig.default()
            }
        } ?: ControlConfig.default()
    }

    val connectionConfigFlow: Flow<ConnectionConfig> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CONNECTION_CONFIG]?.let { jsonStr ->
            try {
                json.decodeFromString<ConnectionConfig>(jsonStr)
            } catch (e: Exception) {
                ConnectionConfig.default()
            }
        } ?: ConnectionConfig.default()
    }

    suspend fun saveControlConfig(config: ControlConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONTROL_CONFIG] = json.encodeToString(config)
        }
    }

    suspend fun saveConnectionConfig(config: ConnectionConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONNECTION_CONFIG] = json.encodeToString(config)
        }
    }

    suspend fun saveControllers(controllers: Controllers) {
        context.dataStore.edit { preferences ->
            val currentConfig = preferences[PreferencesKeys.CONTROL_CONFIG]?.let {
                try {
                    json.decodeFromString<ControlConfig>(it)
                } catch (e: Exception) {
                    ControlConfig.default()
                }
            } ?: ControlConfig.default()

            val updatedConfig = currentConfig.copy(controllers = controllers)
            preferences[PreferencesKeys.CONTROL_CONFIG] = json.encodeToString(updatedConfig)
        }
    }
}
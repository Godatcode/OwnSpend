package com.ownspend.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * DataStore-based settings repository
 */
class SettingsRepository(private val context: Context) {
    
    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val API_KEY = stringPreferencesKey("api_key")
        val SMS_CAPTURE_ENABLED = booleanPreferencesKey("sms_capture_enabled")
        val NOTIFICATION_CAPTURE_ENABLED = booleanPreferencesKey("notification_capture_enabled")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    }
    
    val serverUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SERVER_URL] ?: ""
    }
    
    val apiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[API_KEY] ?: ""
    }
    
    val smsCaptureEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SMS_CAPTURE_ENABLED] ?: true
    }
    
    val notificationCaptureEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_CAPTURE_ENABLED] ?: true
    }
    
    val autoSyncEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_SYNC_ENABLED] ?: true
    }
    
    val lastSyncTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LAST_SYNC_TIME] ?: 0L
    }
    
    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL] = url
        }
    }
    
    suspend fun setApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = key
        }
    }
    
    suspend fun setSmsCaptureEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SMS_CAPTURE_ENABLED] = enabled
        }
    }
    
    suspend fun setNotificationCaptureEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_CAPTURE_ENABLED] = enabled
        }
    }
    
    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SYNC_ENABLED] = enabled
        }
    }
    
    suspend fun updateLastSyncTime() {
        context.dataStore.edit { preferences ->
            preferences[LAST_SYNC_TIME] = System.currentTimeMillis()
        }
    }
    
    suspend fun getApiKeySync(): String {
        return context.dataStore.data.map { it[API_KEY] ?: "" }.toString()
    }
}

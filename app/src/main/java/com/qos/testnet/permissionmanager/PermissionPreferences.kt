package com.qos.testnet.permissionmanager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PermissionPreferences private constructor() {

    private val Context.permissionDataStore: DataStore<Preferences> by preferencesDataStore(name = "permission_preferences")

    object PermissionPreferencesKeys {
        val DONT_ASK_AGAIN_LOCATION_PERMISSION =
            booleanPreferencesKey("dont_ask_again_location_permission")
        val ASK_OPEN_PERMISSION = booleanPreferencesKey("ask_open_permission")
        val DONT_ASK_AGAIN_PHONE_PERMISSION =
            booleanPreferencesKey("dont_ask_again_phone_permission")
        val USER_ID = stringPreferencesKey("User ID")
    }

    suspend fun savePermissionPreference(
        context: Context,
        key: Preferences.Key<Boolean>,
        value: Boolean
    ) {
        context.permissionDataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun saveUserId(context: Context, key: Preferences.Key<String>, value: String) {
        context.permissionDataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun getPermissionPreference(context: Context, key: Preferences.Key<Boolean>): Boolean {
        return context.permissionDataStore.data.map { preferences ->
            preferences[key] ?: false
        }.first()
    }

    suspend fun getUserId(context: Context, key: Preferences.Key<String>): String {
        return context.permissionDataStore.data.map { preferences ->
            preferences[key] ?: "User not found"
        }.first()
    }

    companion object {
        @Volatile
        private var INSTANCE: PermissionPreferences? = null

        fun getInstance(): PermissionPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PermissionPreferences().also { INSTANCE = it }
            }
        }
    }
}
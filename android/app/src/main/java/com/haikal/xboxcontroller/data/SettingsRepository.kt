package com.haikal.xboxcontroller.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "xbox_controller_settings")

data class AppSettings(
    val selectedConsole: String = "XBOX_ONE",
    val leftStickSensitivity: Float = 1.0f,
    val rightStickSensitivity: Float = 1.0f,
    val triggerSensitivity: Float = 1.0f,
    val hapticsEnabled: Boolean = true,
    val bridgeHost: String = "127.0.0.1",
    val bridgePort: Int = 8765,
    val localTestingMode: Boolean = false,
)

/**
 * DataStore-backed settings repository: selected console, analog
 * sensitivities, haptics toggle, bridge address & port, testing mode.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val SELECTED_CONSOLE = stringPreferencesKey("selected_console")
        val LEFT_STICK_SENSITIVITY = floatPreferencesKey("left_stick_sensitivity")
        val RIGHT_STICK_SENSITIVITY = floatPreferencesKey("right_stick_sensitivity")
        val TRIGGER_SENSITIVITY = floatPreferencesKey("trigger_sensitivity")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val BRIDGE_HOST = stringPreferencesKey("bridge_host")
        val BRIDGE_PORT = intPreferencesKey("bridge_port")
        val LOCAL_TESTING_MODE = booleanPreferencesKey("local_testing_mode")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            selectedConsole = prefs[Keys.SELECTED_CONSOLE] ?: "XBOX_ONE",
            leftStickSensitivity = prefs[Keys.LEFT_STICK_SENSITIVITY] ?: 1.0f,
            rightStickSensitivity = prefs[Keys.RIGHT_STICK_SENSITIVITY] ?: 1.0f,
            triggerSensitivity = prefs[Keys.TRIGGER_SENSITIVITY] ?: 1.0f,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            bridgeHost = prefs[Keys.BRIDGE_HOST] ?: "127.0.0.1",
            bridgePort = prefs[Keys.BRIDGE_PORT] ?: 8765,
            localTestingMode = prefs[Keys.LOCAL_TESTING_MODE] ?: false,
        )
    }

    suspend fun setConsole(wireName: String) {
        context.dataStore.edit { it[Keys.SELECTED_CONSOLE] = wireName }
    }

    suspend fun setStickSensitivities(left: Float, right: Float) {
        context.dataStore.edit {
            it[Keys.LEFT_STICK_SENSITIVITY] = left.coerceIn(0.5f, 2.0f)
            it[Keys.RIGHT_STICK_SENSITIVITY] = right.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun setTriggerSensitivity(value: Float) {
        context.dataStore.edit {
            it[Keys.TRIGGER_SENSITIVITY] = value.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setBridgeAddress(host: String, port: Int) {
        context.dataStore.edit {
            it[Keys.BRIDGE_HOST] = host.ifBlank { "127.0.0.1" }
            it[Keys.BRIDGE_PORT] = port.coerceIn(1, 65535)
        }
    }

    suspend fun setLocalTestingMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCAL_TESTING_MODE] = enabled }
    }
}

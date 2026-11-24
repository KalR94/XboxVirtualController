package com.haikal.xboxcontroller.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haikal.xboxcontroller.connection.ConnectionManager
import com.haikal.xboxcontroller.console.ConsoleProfile
import com.haikal.xboxcontroller.data.AppSettings
import com.haikal.xboxcontroller.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Home screen state: selected console, bridge settings, local-testing flag.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val selectedProfile: StateFlow<ConsoleProfile?> = settingsRepository.settings
        .map { s ->
            ConsoleProfile.entries.firstOrNull { it.wireName == s.selectedConsole }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setConsole(profile: ConsoleProfile) {
        viewModelScope.launch {
            settingsRepository.setConsole(profile.wireName)
        }
    }

    fun setLocalTestingMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLocalTestingMode(enabled)
        }
    }

    fun setStickSensitivities(left: Float, right: Float) {
        viewModelScope.launch {
            settingsRepository.setStickSensitivities(left, right)
        }
    }

    fun setTriggerSensitivity(value: Float) {
        viewModelScope.launch {
            settingsRepository.setTriggerSensitivity(value)
        }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHapticsEnabled(enabled)
        }
    }

    fun saveBridgeAddress(host: String, port: Int) {
        viewModelScope.launch {
            settingsRepository.setBridgeAddress(host, port)
        }
    }

    companion object {
        const val DEFAULT_HOST = ConnectionManager.DEFAULT_HOST
        const val DEFAULT_PORT = ConnectionManager.DEFAULT_PORT
    }
}

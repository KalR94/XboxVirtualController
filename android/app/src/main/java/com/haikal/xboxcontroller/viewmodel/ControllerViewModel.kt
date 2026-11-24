package com.haikal.xboxcontroller.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haikal.xboxcontroller.connection.ConnectionManager
import com.haikal.xboxcontroller.connection.ConnectionStatus
import com.haikal.xboxcontroller.connection.ControllerInputState
import com.haikal.xboxcontroller.console.ConsoleProfile
import com.haikal.xboxcontroller.data.AppSettings
import com.haikal.xboxcontroller.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the live virtual controller input state and drives
 * [ConnectionManager] (or the local testing mode when enabled).
 */
class ControllerViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val connectionManager = ConnectionManager(
        scope = viewModelScope,
        consoleNameProvider = { selectedConsole.value },
    )

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val connectionStatus: StateFlow<ConnectionStatus> = connectionManager.status

    private val _inputState = MutableStateFlow(ControllerInputState())
    val inputState: StateFlow<ControllerInputState> = _inputState.asStateFlow()

    private val _debugLog = MutableStateFlow<List<String>>(emptyList())
    val debugLog: StateFlow<List<String>> = _debugLog.asStateFlow()

    private val selectedConsoleWire = MutableStateFlow("XBOX_ONE")
    val selectedConsole: StateFlow<String> = selectedConsoleWire.asStateFlow()

    private val _localTestingMode = MutableStateFlow(false)
    val localTestingMode: StateFlow<Boolean> = _localTestingMode.asStateFlow()

    init {
        viewModelScope.launch {
            settings.collect { s ->
                selectedConsoleWire.value = s.selectedConsole
                _localTestingMode.value = s.localTestingMode
                connectionManager.configure(s.bridgeHost, s.bridgePort)
            }
        }
    }

    fun startConnection() {
        if (_localTestingMode.value) return
        connectionManager.configure(
            settings.value.bridgeHost,
            settings.value.bridgePort,
        )
        connectionManager.connect()
    }

    fun stopConnection() {
        connectionManager.disconnect()
    }

    /** Pushes a new state from the controller UI, sends/log as appropriate. */
    fun updateInput(state: ControllerInputState) {
        _inputState.value = state
        if (_localTestingMode.value) {
            if (state != _lastLoggedState) {
                appendDebugLog(formatState(state))
                _lastLoggedState = state
            }
        } else {
            connectionManager.sendInput(state)
        }
    }

    private var _lastLoggedState: ControllerInputState? = null

    fun appendDebugLog(line: String) {
        _debugLog.update { current ->
            (current + line).takeLast(100)
        }
    }

    fun clearDebugLog() {
        _debugLog.value = emptyList()
    }

    private fun formatState(state: ControllerInputState): String {
        val buttons = if (state.buttons.isEmpty()) {
            "-"
        } else {
            state.buttons.joinToString(",") { it.wireName }
        }
        return "BTN[$buttons] LS(%.2f,%.2f) RS(%.2f,%.2f) LT=%.2f RT=%.2f".format(
            state.leftStickX, state.leftStickY,
            state.rightStickX, state.rightStickY,
            state.leftTrigger, state.rightTrigger,
        )
    }

    fun consoleProfile(): ConsoleProfile =
        ConsoleProfile.entries.firstOrNull { it.wireName == selectedConsoleWire.value }
            ?: ConsoleProfile.XBOX_ONE

    override fun onCleared() {
        connectionManager.disconnect()
        super.onCleared()
    }
}

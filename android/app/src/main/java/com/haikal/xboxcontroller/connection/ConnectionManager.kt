package com.haikal.xboxcontroller.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Manages the TCP connection to the controller bridge (localhost:8765 via
 * adb reverse by default), with:
 *  - connect timeout and send error handling,
 *  - automatic reconnection with exponential backoff + jitter,
 *  - input send rate limiting (~60Hz) and change-based suppression:
 *    frames are only sent when the input state actually differs from the
 *    last sent state, and at most once per rate-limit window.
 */
class ConnectionManager(
    private val scope: CoroutineScope,
    private val consoleNameProvider: () -> String = { "XBOX_ONE" },
) {
    companion object {
        const val DEFAULT_HOST = "127.0.0.1"
        const val DEFAULT_PORT = 8765
        const val RATE_LIMIT_HZ = 60
        const val RATE_LIMIT_INTERVAL_MS = 1000L / RATE_LIMIT_HZ
        const val MAX_BACKOFF_MS = 8_000L
    }

    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var client: UsbClient? = null
    private var host: String = DEFAULT_HOST
    private var port: Int = DEFAULT_PORT

    private val sendMutex = Mutex()
    private var lastSentState: ControllerInputState? = null
    private var lastSendTimeMs: Long = 0L

    private var connectJob: Job? = null
    private var reconnectAttempt = 0
    private var manuallyDisconnected = false

    fun configure(newHost: String, newPort: Int) {
        host = newHost.ifBlank { DEFAULT_HOST }
        port = newPort
    }

    fun connect() {
        manuallyDisconnected = false
        reconnectAttempt = 0
        startConnectLoop()
    }

    fun disconnect() {
        manuallyDisconnected = true
        connectJob?.cancel()
        connectJob = null
        scope.launch {
            client?.close()
            client = null
            lastSentState = null
            _status.value = ConnectionStatus.Disconnected
        }
    }

    private fun startConnectLoop() {
        connectJob?.cancel()
        connectJob = scope.launch {
            while (!manuallyDisconnected) {
                _status.value = ConnectionStatus.Connecting
                val c = UsbClient(host, port)
                try {
                    c.connect()
                    client = c
                    reconnectAttempt = 0
                    lastSentState = null
                    _status.value = ConnectionStatus.Connected(host, port)
                    return@launch
                } catch (e: Exception) {
                    runCatching { c.close() }
                    client = null
                    _lastError.value = e.message ?: e.javaClass.simpleName
                    if (manuallyDisconnected) return@launch
                    reconnectAttempt++
                    val backoff = backoffDelayMs(reconnectAttempt)
                    _status.value = ConnectionStatus.Retrying(reconnectAttempt, backoff)
                    delay(backoff)
                }
            }
        }
    }

    private fun backoffDelayMs(attempt: Int): Long {
        val base = 500L * (1L shl (attempt - 1).coerceAtMost(5)) // 0.5s..8s
        val jitter = (0..250).random()
        return (base + jitter).coerceAtMost(MAX_BACKOFF_MS)
    }

    /**
     * Sends the input state if it changed and the rate-limit window allows.
     * Called at UI frequency (per-frame); network traffic stays <= 60Hz.
     */
    fun sendInput(state: ControllerInputState) {
        scope.launch(Dispatchers.IO) {
            val c = client ?: return@launch
            if (!c.isConnected) return@launch
            if (state.isNeutral && lastSentState?.isNeutral == true) return@launch
            if (state == lastSentState) return@launch

            val now = System.currentTimeMillis()
            val elapsed = now - lastSendTimeMs
            if (elapsed < RATE_LIMIT_INTERVAL_MS && !state.isNeutral) return@launch

            sendMutex.withLock {
                val nowLocked = System.currentTimeMillis()
                val elapsedLocked = nowLocked - lastSendTimeMs
                if (elapsedLocked < RATE_LIMIT_INTERVAL_MS && !state.isNeutral) return@launch
                try {
                    c.sendLine(InputSerializer.serialize(state, consoleNameProvider()))
                    lastSentState = state
                    lastSendTimeMs = System.currentTimeMillis()
                } catch (e: Exception) {
                    _lastError.value = "Send failed: ${e.message ?: e.javaClass.simpleName}"
                    // Connection is broken: trigger reconnect.
                    scope.launch {
                        client?.close()
                        client = null
                        if (!manuallyDisconnected) startConnectLoop()
                    }
                }
            }
        }
    }
}
